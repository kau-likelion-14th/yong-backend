package likelion14th.lte.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import likelion14th.lte.global.api.ApiResponse;
import likelion14th.lte.global.api.SuccessCode;
import likelion14th.lte.user.dto.request.CreateTestUserRequest;
import likelion14th.lte.user.dto.response.UserProfileResponse;
import likelion14th.lte.user.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/api/profile")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)

public class UserProfileController {
    public final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "유저 프로필 조회", description = "유저아이디를 받아 유저 프로필을 반환하는 api 입니다")
    public ApiResponse<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = Long.valueOf(jwt.getSubject());
        UserProfileResponse userProfileResponse = userProfileService.getUserprofile(userId);

        return ApiResponse.onSuccess(SuccessCode.OK, userProfileResponse);
    }

    @PostMapping
    @Operation(summary = "테스트 유저를 생성", description = "이름, 한줄소개, 유저 태그를 받아옵니다.")
    public ApiResponse<UserProfileResponse> createTestUserProfile(
            @RequestBody CreateTestUserRequest createTestUserRequest
    ){
        UserProfileResponse response = userProfileService.createTestUser(createTestUserRequest);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "유저 프로필 추가 및 수정", description = "유저 프로필 이미지를 추가하거나 수정합니다.")
    public ApiResponse<UserProfileResponse> putUserProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("image") MultipartFile file
    ){
        Long userId = Long.valueOf(jwt.getSubject());

        UserProfileResponse response = userProfileService.putProfileImage(userId, file);
        return ApiResponse.onSuccess(SuccessCode.PROFILE_PUT_SUCCESS, response);
    }
}
