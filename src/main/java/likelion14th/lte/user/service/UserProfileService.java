package likelion14th.lte.user.service;

import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.user.dto.request.CreateTestUserRequest;
import likelion14th.lte.user.dto.response.UserProfileResponse;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfileService {

    // [Q5. Service 안에서 new UserRepository() 로 객체를 직접 생성하지 않고,
    // 외부에서 의존성 주입(DI)을 받는 이유는 무엇인가요? (결합도와 단위 테스트 관점)]
    // 답변:
    // new UserRepository()를 사용해 객체를 생성하면 특정 클래스에 강하게 의존하게 되어서 코드 변경이 어려워집니다.
    // 하지만 외부에서 주입(DI)받는 방식을 사용하면 Service는 인터페이스의 명세에만 의존하게 되어,
    // 나중에 DB 접근 로직이 바뀌거나 테스트를 위해 Mock 객체로 교체해야 할 때
    // 코드 수정도 없이 주입되는 객체만 바꿔 끼울 수 있어서 유지보수성과 효율이 극대화됩니다.

    private final UserRepository userRepository;

    // [Q6. (코딩 문제) 만약 클래스 위의 @RequiredArgsConstructor를 지운다면,
    // 우리가 직접 작성해야 할 의존성 주입용 자바 '생성자' 코드는 어떤 모습일까요? 아래에 직접 코딩해 보세요.]
    /*
       여기에 생성자 코드 작성:
       protected UserProfileService(UserRepository userRepository) {
           this.userRepository = userRepository;
       }
    */

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorCode.USER_NOT_FOUND));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse createTestUser(CreateTestUserRequest request) {

        // [Q7. 일반적인 생성자 new User(name, intro, tag) 방식을 쓰지 않고,
        // User.builder()...build() 라는 '빌더 패턴'을 사용하여 객체를 조립했을 때 얻는 장점은 무엇인가요?]
        // 답변: 가독성을 높여서 실수를 방지하고 안정성을 확보할 수 있습니다.
        // 생성자 방식은 매개변수가 많아질수록 각 인자가 어떤 필드인지 알기 어렵고,
        // 특히 타입이 같은 인자가 연속되면 런타임 오류가 발생할 수 있습니다.
        // 빌더 패턴을 사용하면 각 필드명을 메서드 형태로 명시하므로 코드의 가독성이 높아지고,
        // 인자의 순서에 상관없이 필요한 데이터만 선택적으로 지정하여 유연하게 객체를 생성할 수 있습니다.

        User newUser = User.builder()
                .username(request.getUsername())
                .introduction(request.getIntroduction())
                .userTag(request.getUserTag())
                .build();

        User savedUser;
        try {
            // [Q8. 데이터를 저장하는 이 메서드 위에 @Transactional이 반드시 붙어야 하는 이유는 무엇인가요?
            // (저장 도중 DB 서버가 끊겼을 때의 상황을 가정해서 설명하세요)]
            // 답변: 로직 수행 중 DB 서버가 끊겼는데 @Transactional이 없으면
            // 일부 데이터만 DB에 반영되는 데이터 파편화 현상이 발생합니다.
            // 이 어노테이션은 메서드 내의 모든 작업을 하나의 작업 단위로 묶어서
            // 성공하면 모두 Commit하고 실패하면 Rollback 해서 데이터의 무결성을 보장합니다.
            savedUser = userRepository.save(newUser);
        } catch (Exception e) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }
        return UserProfileResponse.from(savedUser);
    }
}