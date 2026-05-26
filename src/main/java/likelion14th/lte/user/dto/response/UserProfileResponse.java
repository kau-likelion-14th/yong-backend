package likelion14th.lte.user.dto.response;

import likelion14th.lte.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)

public class UserProfileResponse {
    private String username;
    private String profileImageUrl;
    private String introduction;

    // [Q4. Controller가 DB에서 꺼낸 원본 Entity(User)를 클라이언트 화면에 그대로 반환하지 않고,
    // 굳이 from() 메서드를 통해 DTO로 한번 변환해서 내보내는 핵심적인 이유 2가지는 무엇인가요?]
    // 답변:
    // 1. Entity에는 비밀번호, 이메일과 같이 클라이언트에 노출되면 안 되는 민감한 정보나
    // 내부 로직에만 필요한 필드가 있을 수 있습니다.
    // DTO를 사용하면 화면에 필요한 데이터만 골라 내보내서 내부 구조를 안전하게 보호할 수 있습니다.

    // 2. 만약 Entity를 직접 반환하면 DB 테이블의 컬럼명 하나만 바꿔도 API를 사용하는 모든 클라이언트의 코드가 깨지게 됩니다.
    // DTO를 중간에 두면 DB 구조가 바뀌더라도 DTO의 변환 로직만 수정하면 되므로 유지보수성을 높일 수 있습니다.

    public static UserProfileResponse from (User user) {
        return new UserProfileResponse(
                user.getUsername() + "#" + user.getUserTag(),
                user.getProfileImage(),
                user.getIntroduction()
        );
    }
}