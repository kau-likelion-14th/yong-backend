package likelion14th.lte.yotube.service;


import tools.jackson.databind.JsonNode;
import likelion14th.lte.global.api.ErrorCode;
import likelion14th.lte.global.exception.GeneralException;
import likelion14th.lte.user.entity.User;
import likelion14th.lte.user.repository.UserRepository;
import likelion14th.lte.yotube.client.YouTubeClient;
import likelion14th.lte.yotube.domain.SavedSong;
import likelion14th.lte.yotube.dto.resopnse.SavedSongResponse;
import likelion14th.lte.yotube.dto.resopnse.YouTubeSongitemResponse;
import likelion14th.lte.yotube.repository.SavedSongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class YouTubeService {

    private static final String DUMMY_LOGIN_ID = "dummy";
    private static final String DUMMY_USERNAME = "dummy";
    private static final String DUMMY_USER_TAG = "dummy";

    private final YouTubeClient  youTubeClient;
    private final SavedSongRepository  savedSongRepository;
    private final UserRepository  userRepository;

    private User getDummyUser(){
        return userRepository.findByUsername(DUMMY_USERNAME)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .username(DUMMY_USERNAME)
                                .userTag(DUMMY_USER_TAG)
                                .introduction("더미 데이터")
                                .build()
                ));
    }

    @Transactional(readOnly = true)
    public List<YouTubeSongitemResponse> searchSongs(String query, int limit) {
        JsonNode root = youTubeClient.searchVideoRaw(query, limit);
        JsonNode items = root.path("items");

        List<YouTubeSongitemResponse> result = new ArrayList<>();

        if (!items.isArray()) {
            return result;
        }

        for (JsonNode item : items) {
            String videoId = item.path("id").path("videoId")
                    .asText(null);
            if(videoId == null) {
                continue;
            }

            JsonNode snippet = item.path("snippet");

            result.add(YouTubeSongitemResponse.builder()
                    .songId(videoId)
                    .title(snippet.path("title").asText(""))
                    .artist(snippet.path("channelTitle").asText(""))
                    .ImgUrl(extractThumbnail(snippet))
                    .build());
        }

        return result;
    }

    public SavedSongResponse saveSong(String songId) {
        if (songId == null || songId.isBlank()) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }

        //TODO: 추후 로그인 기능 구현 후 수정예정
        User user = getDummyUser();

        if (savedSongRepository.existsByUserAndSongId(user, songId)) {
            throw new GeneralException(ErrorCode.SONG_ALREADY_SAVED);
        }

        JsonNode item = getFirstVideo(songId);
        JsonNode snippet = item.path("snippet");
        String duration = item.path("contentDetails")
                .path("duration").asText(null);

        SavedSong savedSong = SavedSong.builder()
                .user(user)
                .songId(songId)
                .title(snippet.path("title").asText(""))
                .artist(snippet.path("channelTitle").asText(""))
                .imgUrl(extractThumbnail (snippet))
                .durationMs(parseDurationMs(duration))
                .build();

        return SavedSongResponse.from(savedSongRepository.save(savedSong));
    }

    private JsonNode getFirstVideo(String videoId){
        JsonNode root = youTubeClient.getVideoRaw(videoId);
        JsonNode items = root.path("items");

        if(!items.isArray() || items.size() == 0) {
            throw new GeneralException(ErrorCode.SONG_NOT_FOUND);
        }

        return items.get(0);
    }

    public List<SavedSongResponse> mySavedSongs() {
        User user = getDummyUser();

        return savedSongRepository.findAllByUserOrderBySavedAtDesc(user)
                .stream()
                .map(SavedSongResponse::from)
                .toList();
    }

    public void deleteSavedSong(String songId) {
        User user = getDummyUser();

        SavedSong savedSong = savedSongRepository.findByUserAndSongId(user, songId)
                .orElseThrow(() -> new GeneralException(ErrorCode.SONG_NOT_FOUND));

        savedSongRepository.delete(savedSong);
    }

    private String extractThumbnail(JsonNode snippet) {
        String imageUrl = snippet.path("thumbnails").path("high")
                .path("url").asText(null);

        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = snippet.path("thumbnail").path("medium")
                    .path("url").asText(null);
        }

        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = snippet.path("thumbnail").path("default")
                    .path("url").asText(null);
        }

        return imageUrl;
    }

    private Long parseDurationMs(String duration) {
        if (duration == null || duration.isBlank()) {
            return null;
        }

        try {
            return Duration.parse(duration).toMillis();
        } catch (DateTimeParseException e) {
            return null;
        }
    }


}
