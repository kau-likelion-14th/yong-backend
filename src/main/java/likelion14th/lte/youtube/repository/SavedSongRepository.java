package likelion14th.lte.youtube.repository;

import likelion14th.lte.user.entity.User;
import likelion14th.lte.youtube.domain.SavedSong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedSongRepository extends JpaRepository<SavedSong,Long> {

    boolean existsByUserAndSongId(User user, String songId);

    List<SavedSong> findAllByUserAndSongIdOrderBySavedAtDescIdDesc(User user, String songId);

    List<SavedSong> findAllByUserOrderBySavedAtDesc(User user);
}
