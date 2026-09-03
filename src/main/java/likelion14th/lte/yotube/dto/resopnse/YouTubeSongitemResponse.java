package likelion14th.lte.yotube.dto.resopnse;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class YouTubeSongitemResponse {

    private String songId;
    private String title;
    private String artist;
    private String ImgUrl;
}
