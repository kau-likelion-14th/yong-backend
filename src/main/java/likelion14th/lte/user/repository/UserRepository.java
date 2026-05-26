package likelion14th.lte.user.repository;

import likelion14th.lte.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// [추가문제] (필수 X) 이 코드는 인터페이스일 뿐이고 구현체(implements) 클래스가 없습니다.
// 그런데 어떻게 프로그램 실행 시 DB와 통신하는 객체로 동작할 수 있나요?
// 답변: 개발자가 인터페이스만 정의해두면 적절한 SQL을 생성하고 실행하는 객체를 만들어 의존성을 유지해줍니다.
// 덕분에 데이터 접근 로직에만 집중할 수 있습니다.
public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findById(Long id);
}