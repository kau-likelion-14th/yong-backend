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
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/profile")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)

public class UserProfileController {
    public final UserProfileService userProfileService;

    // [Q9. Controller 내부에서 userRepository.findById()를 직접 호출해서 유저를 찾지 않고,
    // 반드시 userProfileService를 호출하여 작업을 위임해야 하는 이유는 무엇인가요? (단일 책임 원칙 관점)]
    // 답변: 단일 책임 원칙에 따라 각 계층의 역할을 명확히 구분하기 위해서입니다.
    // Controller는 클라이언트의 요청을 받고 응답을 반환하는 역할에만 집중해야 합니다.
    // 만약 Controller가 DB에 직접 접근하게 되면 로직이 뒤섞여 코드가 복잡해지고 재사용성이 떨어집니다.
    // 하지만 작업을 위임하면 코드의 유지보수성을 높이고 다른 컨트롤러에서도 해당 로직을 재사용할 수 있게 됩니다.

    @GetMapping
    @Operation(summary = "유저 프로필 조회", description = "유저아이디를 받아 유저 프로필을 반환하는 api 입니다")
    public ApiResponse<UserProfileResponse> getUserProfile(
            @RequestParam Long userId
    ){
        UserProfileResponse userProfileResponse = userProfileService.getUserProfile(userId);

        return ApiResponse.onSuccess(SuccessCode.OK, userProfileResponse);
    }

    @PostMapping
    @Operation(summary = "테스트 유저를 생성", description = "이름, 한줄소개, 유저 태그를 받아옵니다.")
    public ApiResponse<UserProfileResponse> createTestUserProfile(
            // [Q10. 클라이언트가 보낸 JSON 텍스트 데이터가 어떻게 자바 객체인 CreateTestUserRequest로
            // 변환 되는지앞의 어노테이션과 연관 지어 설명해 보세요.]
            // 답변: @RequestBody 어노테이션이 붙으면 스프링의 HttpMessageConverter가
            // HTTP 요청 본문에 담긴 JSON 텍스트를 읽어, 자바 객체의 필드명과 JSON의 키 값을 매칭하고
            // CreateTestUserRequest 객체의 필드에 데이터를 자동으로 채워줍니다.

            @RequestBody CreateTestUserRequest createTestUserRequest
    ){
        UserProfileResponse response = userProfileService.createTestUser(createTestUserRequest);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}