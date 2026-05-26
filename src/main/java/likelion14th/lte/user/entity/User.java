package likelion14th.lte.user.entity;

import jakarta.persistence.*;
import likelion14th.lte.Entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
// [Q1. @NoArgsConstructor는 매개변수가 없는 기본 생성자를 만듭니다.
// 그런데 왜 누구나 쓸 수 있게 PUBLIC으로 열어두지 않고, 굳이 PROTECTED로 막아두었을까요? (객체 생성의 안전성과 JPA 관점)]
// 답변: JPA는 프록시 생성 및 조회 시 리플렉션을 사용하므로 기본 생성자가 필수적이지만,
// PUBLIC으로 열어두면 외부에서 필수 필드가 빠진 빈 객체를 무분별하게 생성할 수 있는 허점이 생깁니다.
// 따라서 JPA 프록시가 접근 가능한 최소 범위인 PROTECTED로 제한함으로써,
// 개발자가 의도한 생성자나 빌더를 통해서만 객체를 생성할 수 있습니다.

@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [Q2. @Column(nullable = false) 어노테이션이 DB와 자바 코드 사이에서 하는 역할은 무엇인가요?]
    // 답변: 데이터베이스 스키마를 자동 생성할 때, 해당 컬럼에 'NOT NULL' 제약조건을 추가하는 역할을 합니다.
    // 데이터베이스 엔진 수준에서 null 값이 삽입되는 것을 차단해서 시스템 전체의 데이터 무결성을 보장합니다.
    @Column(nullable = false)
    private String username;

    @Column(length = 16, nullable = false, unique = true)
    private String userTag;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(columnDefinition = "TEXT")
    private String profileImage;

    @Column(columnDefinition = "TEXT")
    private String s3ImageKey;

    @Builder(access = AccessLevel.PUBLIC)
    private User(String username, String userTag, String introduction) {
        this.username = username;
        this.userTag = userTag;
        this.introduction = introduction;
    }

    // [Q3. @Setter를 위 @Getter 처럼 사용하면 모든 맴버들에 setIntruduction() 같은 setter 메서드가 생성됩니다. 하지만 왜 @Setter를 쓰지않고 updateIntroduction() 이라는 명확한 메서드를 만든 객체지향적인 이유는 무엇인가요?]
    // 답변: @Setter를 클래스 레벨에 열어두면 누구나 엔티티의 모든 상태를 변경할 수 있게 되어 객체의 캡슐화가 깨집니다.
    // updateIntroduction()처럼 변경이 필요한 필드에 대해서만 메서드를 만들면,
    // 코드를 읽을 때 상태 변경의 목적을 분명히 알 수 있고 유지보수와 추적이 훨씬 쉬워집니다.

    public void updateIntroduction(String introduction) {
        this.introduction = introduction;
    }
}