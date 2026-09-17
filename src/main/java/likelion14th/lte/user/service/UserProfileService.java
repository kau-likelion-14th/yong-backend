package likelion14th.lte.user.service;


import io.swagger.v3.oas.annotations.Operation;
import likelion14th.lte.global.api.ApiResponse;
import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.api.SuccessCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.user.dto.request.CreateTestUserRequest;
import likelion14th.lte.user.dto.response.UserProfileResponse;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import likelion14th.lte.utils.Image.ImageUtil;
import likelion14th.lte.utils.S3.S3Dto;
import likelion14th.lte.utils.S3.S3Utils;
import likelion14th.lte.utils.exception.UtilException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileService {
    private final UserRepository userRepository;
    private final S3Utils s3Utils;
    private final ImageUtil  imageUtil;

    @Transactional
    public UserProfileResponse createTestUser(CreateTestUserRequest request) {
        User newUser = User.builder()
                .username(request.getUsername())
                .userTag(request.getUserTag())
                .introduction(request.getIntroduction())
                .build();

        User savedUser;
        try {
            savedUser = userRepository.save(newUser);
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }
    return UserProfileResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserprofile(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponse.from(user);
    }
    @Transactional
    public UserProfileResponse putProfileImage(Long userId, MultipartFile file) {
        User  user = userRepository.findById(userId)
                .orElseThrow(()-> new GeneralException(ErrorCode.USER_NOT_FOUND));
        try{
            imageUtil.validateImage(file);
            ImageUtil.ResizedImage resizedImage =
                    imageUtil.resizeProfileToPngBytes(file, 256);
            String originalFilename = file.getOriginalFilename();
            String baseName = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : originalFilename;
            S3Dto result =
                    s3Utils.uploadBytes(resizedImage.bytes(), baseName+".png",resizedImage.contentType());
            if(user.getS3ImageKey()!=null){
                s3Utils.deleteFile(user.getS3ImageKey());
            }
            user.fixUserProfile(result.getUrl(), result.getKey());
            return UserProfileResponse.from(user);
        } catch (UtilException e){
            throw GeneralException.of(mapToErrorCode(e.getReason()));
        }

    }
    private ErrorCode mapToErrorCode(UtilException.Reason reason) {
        return switch (reason) {
            case FILE_EMPTY -> ErrorCode.IMAGE_FILE_EMPTY;
            case FILE_TOO_LARGE -> ErrorCode.IMAGE_TOO_LARGE;
            case TYPE_NOT_ALLOWED -> ErrorCode.IMAGE_TYPE_NOT_ALLOWED;

            case IMAGE_PROCESS_FAILED -> ErrorCode.IMAGE_PROCESS_FAILED;

            case S3_UPLOAD_FAILED -> ErrorCode.S3_UPLOAD_FAILED;
            case S3_DELETE_FAILED -> ErrorCode.S3_DELETE_FAILED;
        };
    }


}