package likelion14th.lte.follow.service;

import likelion14th.lte.follow.repository.FollowRepository;
import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FollowServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final FollowRepository follows = mock(FollowRepository.class);
    private final FollowService service = new FollowService(follows, users);

    @ParameterizedTest
    @ValueSource(strings = {"abcd1234", "KAKAOA1B2C3D4"})
    void searchesByLegacyOrGeneratedKakaoTag(String tag) {
        User current = User.builder().username("나").userTag("SELF0001").build();
        User target = User.builder().username("친구").userTag(tag).build();
        ReflectionTestUtils.setField(current, "id", 1L);
        ReflectionTestUtils.setField(target, "id", 2L);
        when(users.findById(1L)).thenReturn(Optional.of(current));
        when(users.findByUserTag(tag)).thenReturn(Optional.of(target));

        var result = service.searchCanFollowers(1L, "친구#" + tag, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("친구#" + tag, result.getContent().getFirst().getUserName());
        verify(users).findByUserTag(tag);
    }

    @Test
    void rejectsMalformedTagBeforeSearching() {
        when(users.findById(1L)).thenReturn(Optional.of(mock(User.class)));

        var error = assertThrows(GeneralException.class,
                () -> service.searchCanFollowers(1L, "친구#KAKAO1234", PageRequest.of(0, 10)));

        assertEquals(ErrorCode.INVALID_HANDLE_FORMAT, error.getCode());
        verify(users, never()).findByUserTag(anyString());
    }
}
