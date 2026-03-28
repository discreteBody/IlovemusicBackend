package com.ilovemusic.ilovemusic_backend.service;

import com.ilovemusic.ilovemusic_backend.dto.TrackDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class YouTubeService {

    @Value("${youtube.api.base-url:https://www.googleapis.com/youtube/v3}")
    private String youtubeBaseUrl;

    @Value("${youtube.api.key:}")
    private String youtubeApiKey;

    private final RestTemplate restTemplate;

    public YouTubeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch video information from YouTube API
     */
    public TrackDTO getVideoInfo(String youtubeVideoId) {
        try {
            String url = youtubeBaseUrl + "/videos?id=" + youtubeVideoId + "&key=" + youtubeApiKey
                    + "&part=snippet,contentDetails";
            log.info("Fetching video info from YouTube: {}", youtubeVideoId);
            // Implementation would call YouTube API
            return new TrackDTO();
        } catch (Exception e) {
            log.error("Error fetching video from YouTube", e);
            throw new RuntimeException("Failed to fetch video from YouTube", e);
        }
    }

    /**
     * Search videos on YouTube
     */
    public List<TrackDTO> searchVideos(String query) {
        try {
            String url = youtubeBaseUrl + "/search?q=" + query + "&key=" + youtubeApiKey
                    + "&part=snippet&type=video";
            log.info("Searching YouTube for: {}", query);
            // Implementation would call YouTube API
            return List.of();
        } catch (Exception e) {
            log.error("Error searching YouTube", e);
            throw new RuntimeException("Failed to search YouTube", e);
        }
    }

    /**
     * Get user's YouTube playlists
     */
    public List<com.ilovemusic.ilovemusic_backend.dto.PlaylistDTO> getUserPlaylists(String accessToken) {
        try {
            String url = youtubeBaseUrl + "/playlists?mine=true&part=snippet&access_token=" + accessToken;
            log.info("Fetching user playlists from YouTube");
            // Implementation would call YouTube API
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching user playlists from YouTube", e);
            throw new RuntimeException("Failed to fetch playlists from YouTube", e);
        }
    }
}

