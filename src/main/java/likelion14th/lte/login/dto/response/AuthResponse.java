package likelion14th.lte.login.dto.response;


import likelion14th.lte.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonIgnore;


@Getter
@Builder
public class AuthResponse {

    private Long id;
    private String username;
    private String userTag;
    private String introduction;
    private String profileImages;
    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    public static AuthResponse from (User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .id(user.getId())
                .userTag(user.getUserTag())
                .username(user.getUsername())
                .introduction(user.getIntroduction())
                .profileImages(user.getProfileImage())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
