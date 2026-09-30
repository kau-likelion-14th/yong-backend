package likelion14th.lte.user.entity;

import jakarta.persistence.*;
import likelion14th.lte.Entity.BaseEntity;
import likelion14th.lte.login.domain.RefreshToken;
import likelion14th.lte.youtube.domain.SavedSong;
import likelion14th.lte.follow.entity.Follow;
import likelion14th.lte.statistic.entity.Statistic;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String providerId;

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

    @OneToMany(mappedBy = "toUser", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followers;

    @OneToMany(mappedBy = "fromUser", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followings;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<SavedSong> saveSongs;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken refreshToken;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "statistic_id", nullable = false, unique = true)
    private Statistic statistic;

    @Builder(access = AccessLevel.PUBLIC)
    private User(String providerId, String username, String userTag, String introduction, String s3ImageKey, String profileImage){
        this.providerId = providerId;
        this.username = username;
        this.userTag = userTag;
        this.introduction = introduction;
        this.s3ImageKey = s3ImageKey;
        this.profileImage = profileImage;
        this.followers = new ArrayList<>();
        this.followings = new ArrayList<>();
        this.statistic = Statistic.create();
        this.saveSongs = new ArrayList<>();
    }
    public void fixUserProfile(String s3ImageUrl, String s3ImageKey){
        this.s3ImageKey = s3ImageKey;
        this.profileImage = s3ImageUrl;
    }

    public void updateIntroduction(String introduction){
        this.introduction = introduction;
    }

    public void setFollowers(List<Follow> followers) {
        this.followers = followers;
    }
}
