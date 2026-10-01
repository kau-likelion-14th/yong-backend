package likelion14th.lte.user.service;

import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.user.dto.request.UserIntroRequest;
import likelion14th.lte.user.dto.response.UserProfileResponse;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import likelion14th.lte.utils.Image.ImageUtil;
import likelion14th.lte.utils.S3.S3Utils;
import likelion14th.lte.utils.exception.UtilException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserProfileServiceTest {

    @Test
    void deleteProfileImageDeletesStoredImage() {
        UserRepository userRepository = mock(UserRepository.class);
        S3Utils s3Utils = mock(S3Utils.class);
        ImageUtil imageUtil = mock(ImageUtil.class);
        UserProfileService service = new UserProfileService(userRepository, s3Utils, imageUtil);
        User user = User.builder()
                .username("홍길동")
                .userTag("abcd1234")
                .introduction("한줄 소개입니다")
                .profileImage("https://cdn.example.com/profile.png")
                .s3ImageKey("profile.png")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.deleteProfileImage(1L);

        verify(s3Utils).deleteFile("profile.png");
        assertEquals("홍길동#abcd1234", response.getUsername());
        assertNull(response.getProfileImageUrl());
        assertNull(user.getS3ImageKey());
    }

    @Test
    void deleteProfileImageSucceedsWithoutStoredImage() {
        UserRepository userRepository = mock(UserRepository.class);
        S3Utils s3Utils = mock(S3Utils.class);
        ImageUtil imageUtil = mock(ImageUtil.class);
        UserProfileService service = new UserProfileService(userRepository, s3Utils, imageUtil);
        User user = User.builder()
                .username("홍길동")
                .userTag("abcd1234")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserProfileResponse response = service.deleteProfileImage(1L);

        verifyNoInteractions(s3Utils);
        assertNull(response.getProfileImageUrl());
    }

    @Test
    void deleteProfileImageMapsS3Failure() {
        UserRepository userRepository = mock(UserRepository.class);
        S3Utils s3Utils = mock(S3Utils.class);
        ImageUtil imageUtil = mock(ImageUtil.class);
        UserProfileService service = new UserProfileService(userRepository, s3Utils, imageUtil);
        User user = User.builder()
                .username("홍길동")
                .userTag("abcd1234")
                .profileImage("https://cdn.example.com/profile.png")
                .s3ImageKey("profile.png")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doThrow(new UtilException(UtilException.Reason.S3_DELETE_FAILED))
                .when(s3Utils).deleteFile("profile.png");

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> service.deleteProfileImage(1L)
        );

        assertEquals(ErrorCode.S3_DELETE_FAILED, exception.getCode());
        assertEquals("https://cdn.example.com/profile.png", user.getProfileImage());
    }

    @Test
    void getToUserProfileReturnsTargetUser() {
        UserRepository userRepository = mock(UserRepository.class);
        S3Utils s3Utils = mock(S3Utils.class);
        ImageUtil imageUtil = mock(ImageUtil.class);
        UserProfileService service = new UserProfileService(userRepository, s3Utils, imageUtil);
        User targetUser = User.builder()
                .username("김철수")
                .userTag("efgh5678")
                .introduction("반갑습니다")
                .profileImage("https://cdn.example.com/profile/3.png")
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(targetUser));

        UserProfileResponse response = service.getToUserProfile(3L);

        assertEquals("김철수#efgh5678", response.getUsername());
        assertEquals("https://cdn.example.com/profile/3.png", response.getProfileImageUrl());
        assertEquals("반갑습니다", response.getIntroduction());
    }

    @Test
    void putUserIntroductionUpdatesIntroduction() {
        UserRepository userRepository = mock(UserRepository.class);
        S3Utils s3Utils = mock(S3Utils.class);
        ImageUtil imageUtil = mock(ImageUtil.class);
        UserProfileService service = new UserProfileService(userRepository, s3Utils, imageUtil);
        UserIntroRequest request = mock(UserIntroRequest.class);
        User user = User.builder()
                .username("홍길동")
                .userTag("abcd1234")
                .introduction("기존 소개")
                .profileImage("https://cdn.example.com/profile.png")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(request.getIntroduce()).thenReturn("한줄 소개입니다");

        UserProfileResponse response = service.putUserIntroduction(1L, request);

        assertEquals("한줄 소개입니다", response.getIntroduction());
        assertEquals("https://cdn.example.com/profile.png", response.getProfileImageUrl());
    }

    @Test
    void getToUserProfileRequiresExistingUser() {
        UserRepository userRepository = mock(UserRepository.class);
        S3Utils s3Utils = mock(S3Utils.class);
        ImageUtil imageUtil = mock(ImageUtil.class);
        UserProfileService service = new UserProfileService(userRepository, s3Utils, imageUtil);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        GeneralException exception = assertThrows(
                GeneralException.class,
                () -> service.getToUserProfile(999L)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getCode());
    }
}
