package likelion14th.lte.yotube.dto.resopnse;


import likelion14th.lte.yotube.domain.SavedSong;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SavedSongResponse {

    private Long id;
    private String songId;
    private String title;
    private String artist;
    private String imgUrl;
    private Long durationMs;
    private LocalDateTime savedAt;

    public static SavedSongResponse from(SavedSong song){
        return SavedSongResponse.builder()
                .id(song.getId())
                .songId(song.getSongId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .imgUrl(song.getImgUrl())
                .durationMs(song.getDurationMs())
                .savedAt(song.getSavedAt())
                .build();
    }

}
