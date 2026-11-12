package likelion14th.lte.youtube.service;

import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import likelion14th.lte.youtube.client.YouTubeClient;
import likelion14th.lte.youtube.domain.SavedSong;
import likelion14th.lte.youtube.dto.response.SavedSongResponse;
import likelion14th.lte.youtube.repository.SavedSongRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YouTubeServiceTest {
    @Test
    void selectingSavedSongMakesItRecentWithoutDeletingOrCreatingAnotherRecord() {
        YouTubeClient client = mock(YouTubeClient.class);
        SavedSongRepository songs = mock(SavedSongRepository.class);
        UserRepository users = mock(UserRepository.class);
        User user = User.builder().username("isolated-test").userTag("test").build();
        SavedSong song = SavedSong.builder().user(user).songId("old-song").title("Old song").artist("Artist").build();
        LocalDateTime oldDate = LocalDateTime.of(2020, 1, 1, 0, 0);
        ReflectionTestUtils.setField(song, "savedAt", oldDate);
        ReflectionTestUtils.setField(song, "id", 10L);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(songs.findByUserAndSongId(user, "old-song")).thenReturn(Optional.of(song));
        YouTubeService service = new YouTubeService(client, songs, users);

        SavedSongResponse first = service.saveSong(1L, "old-song");
        SavedSongResponse second = service.saveSong(1L, "old-song");

        assertEquals(10L, first.getId());
        assertEquals(first.getId(), second.getId());
        assertEquals("old-song", second.getSongId());
        assertTrue(second.getSavedAt().isAfter(oldDate));
        verifyNoInteractions(client);
        verify(songs, never()).save(any());
        verify(songs, never()).delete(any());
    }
}
