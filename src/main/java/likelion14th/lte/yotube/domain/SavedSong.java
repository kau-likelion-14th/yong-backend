package likelion14th.lte.yotube.domain;

import jakarta.persistence.*;
import likelion14th.lte.user.entity.User;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)


public class SavedSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name= "user_id", nullable = false)
    private User user;

    @Column(name = "song_id", nullable = false, length = 64)
    private String songId;

    @Column (nullable = false, length = 300)
    private String title;

    @Column (nullable = false, length = 200)
    private String artist;

    @Column(length = 500)
    private String imgUrl;

    @Column(nullable = false)
    private LocalDateTime savedAt;

    private Long durationMs;

    @Builder
    public SavedSong(User user, String songId, String title, String artist,
    String imgUrl, Long durationMs){
        this.user = user;
        this.songId = songId;
        this.title = title;
        this.artist = artist;
        this.imgUrl = imgUrl;
        this.durationMs = durationMs;
        this.savedAt = LocalDateTime.now();
    }
}
