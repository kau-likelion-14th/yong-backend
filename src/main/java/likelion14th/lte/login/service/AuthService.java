package likelion14th.lte.login.service;

import tools.jackson.databind.JsonNode;
import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.login.client.KakaoClient;
import likelion14th.lte.login.domain.RefreshToken;
import likelion14th.lte.login.dto.response.AuthResponse;
import likelion14th.lte.login.repository.RefreshTokenRepository;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import likelion14th.lte.login.jwt.JwtProvider;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final KakaoClient kakaoClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    public AuthResponse handleKakaoCode(String code) {
        String kakaoAccessToken = kakaoClient.getAccessToken(code);
        JsonNode kakaoUserInfo = kakaoClient.getUserInfo(kakaoAccessToken);

        String providerId = kakaoUserInfo.path("id").asText(null);
        String username = kakaoUserInfo.path("kakao_account")
                .path("profile")
                .path("nickname")
                .asText("카카오 유저");

        User user = userRepository.findByProviderId(providerId)
            .orElseGet(() -> userRepository.save(
                    User.builder()
                            .providerId(providerId)
                            .username(username)
                            .userTag(createUniqueUserTag())
                            .build()
        ));

        return issuToken(user);
    }

    public AuthResponse issuToken(User user) {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        Long refreshTokenExpiration = jwtProvider.getRefreshTokenExpiration();

        saveOrUpdateRefreshToken(user, refreshToken, refreshTokenExpiration);
        return AuthResponse.from(user, accessToken, refreshToken);
    }

    public void saveOrUpdateRefreshToken(User user, String refreshToken, Long refreshTokenExpiration) {
        refreshTokenRepository.findByUser(user).ifPresentOrElse(
                exiting -> exiting.updateToken(refreshToken, refreshTokenExpiration),
                () -> refreshTokenRepository.save(
                        RefreshToken.builder()
                                .user(user)
                                .refreshToken(refreshToken)
                                .refreshTokenExpireTime(refreshTokenExpiration)
                                .build()
                )
        );
    }

    @Transactional(readOnly = true)
    public String reissueAccessToken(String refreshToken) {
        Long userId;
        try {
            userId = jwtProvider.validateRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        RefreshToken savedToken = refreshTokenRepository.findByUser(user)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_REFRESH_TOKEN));

        if(!savedToken.getRefreshToken().equals(refreshToken)) {
        throw new GeneralException(ErrorCode.TOKEN_INVALID);
        }

        return jwtProvider.createAccessToken(user.getId());
    }

    public void logout(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.findByUser(user).ifPresent(refreshTokenRepository::delete);
    }

    private String createUniqueUserTag() {
        String userTag;

        do {
            userTag = ("KAKAO" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8))
                    .toUpperCase();
        } while (userRepository.findByUserTag(userTag).isPresent());

        return userTag;
    }

    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
    }
}
