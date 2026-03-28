package com.ilovemusic.ilovemusic_backend.service;

import com.ilovemusic.ilovemusic_backend.dto.TrackDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
public class SpotifyService {

    @Value("${spotify.api.base-url:https://api.spotify.com/v1}")
    private String spotifyBaseUrl;

    private final RestTemplate restTemplate;

    public SpotifyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetch track information from Spotify API
     */
    public TrackDTO getTrackInfo(String spotifyTrackId, String accessToken) {
        try {
            String url = spotifyBaseUrl + "/tracks/" + spotifyTrackId;
            // Implementation would call Spotify API with accessToken
            log.info("Fetching track info from Spotify: {}", spotifyTrackId);
            // Return TrackDTO
            return new TrackDTO();
        } catch (Exception e) {
            log.error("Error fetching track from Spotify", e);
            throw new RuntimeException("Failed to fetch track from Spotify", e);
        }
    }

    /**
     * Search tracks on Spotify
     */
    public List<TrackDTO> searchTracks(String query, String accessToken) {
        try {
            String url = spotifyBaseUrl + "/search?q=" + query + "&type=track";
            // Implementation would call Spotify API
            log.info("Searching Spotify for: {}", query);
            // Return list of TrackDTOs
            return List.of();
        } catch (Exception e) {
            log.error("Error searching Spotify", e);
            throw new RuntimeException("Failed to search Spotify", e);
        }
    }

    /**
     * Get user's Spotify playlists
     */
    public List<com.ilovemusic.ilovemusic_backend.dto.PlaylistDTO> getUserPlaylists(String accessToken) {
        try {
            String url = spotifyBaseUrl + "/me/playlists";
            log.info("Fetching user playlists from Spotify");
            // Implementation would call Spotify API
            return List.of();
        } catch (Exception e) {
            log.error("Error fetching user playlists from Spotify", e);
            throw new RuntimeException("Failed to fetch playlists from Spotify", e);
        }
    }
}

