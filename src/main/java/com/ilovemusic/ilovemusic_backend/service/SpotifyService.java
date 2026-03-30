package com.ilovemusic.ilovemusic_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ilovemusic.ilovemusic_backend.common.exception.BusinessException;
import com.ilovemusic.ilovemusic_backend.common.exception.UnauthorizedException;
import com.ilovemusic.ilovemusic_backend.dto.PlaylistDTO;
import com.ilovemusic.ilovemusic_backend.dto.TrackDTO;
import com.ilovemusic.ilovemusic_backend.entity.OAuthToken;
import com.ilovemusic.ilovemusic_backend.entity.User;
import com.ilovemusic.ilovemusic_backend.repository.OAuthTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpotifyService {

    private final OAuthTokenRepository oauthTokenRepository;
    private final WebClient.Builder webClientBuilder;

    // ✅ With these
    @Value("${spotify.client.id:}")
    private String clientId;

    @Value("${spotify.client.secret:}")
    private String clientSecret;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private static final String SPOTIFY_API      = "https://api.spotify.com/v1";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String PROVIDER          = "spotify";

    // ─── OAuth: Exchange authorization code for tokens ────────────────────────
    public OAuthToken exchangeCodeForTokens(User user, String code) {
        log.info("Exchanging Spotify authorization code for tokens");

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri",
                appBaseUrl + "/ilovemusic/api/auth/spotify/callback");

        JsonNode response = webClientBuilder.build()
                .post()
                .uri(SPOTIFY_TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("access_token")) {
            throw new BusinessException("Failed to exchange Spotify authorization code");
        }

        // Save or update the token in DB
        OAuthToken token = oauthTokenRepository
                .findByUserIdAndProvider(user.getId(), PROVIDER)
                .orElse(new OAuthToken());

        token.setUser(user);
        token.setProvider(PROVIDER);
        token.setAccessToken(response.get("access_token").asText());
        token.setRefreshToken(response.get("refresh_token").asText());
        token.setExpiresIn(response.get("expires_in").asLong());

        OAuthToken saved = oauthTokenRepository.save(token);
        log.info("Spotify tokens saved for user {}", user.getId());
        return saved;
    }

    // ─── OAuth: Refresh an expired access token ───────────────────────────────
    public String refreshAccessToken(OAuthToken token) {
        log.info("Refreshing Spotify access token for user {}", token.getUser().getId());

        String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", token.getRefreshToken());

        JsonNode response = webClientBuilder.build()
                .post()
                .uri(SPOTIFY_TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("access_token")) {
            throw new BusinessException("Failed to refresh Spotify token");
        }

        String newAccessToken = response.get("access_token").asText();
        token.setAccessToken(newAccessToken);
        token.setExpiresIn(response.get("expires_in").asLong());

        // Spotify sometimes returns a new refresh token too
        if (response.has("refresh_token")) {
            token.setRefreshToken(response.get("refresh_token").asText());
        }

        oauthTokenRepository.save(token);
        log.info("Spotify access token refreshed");
        return newAccessToken;
    }

    // ─── Get a valid access token (auto-refreshes if expired) ────────────────
    public String getValidAccessToken(User user) {
        OAuthToken token = oauthTokenRepository
                .findByUserIdAndProvider(user.getId(), PROVIDER)
                .orElseThrow(() ->
                        new UnauthorizedException("Spotify account not connected. Please connect Spotify first."));

        // Check if token is expired (expiresIn is seconds, created_at tells us when)
        if (isTokenExpired(token)) {
            log.info("Spotify token expired, refreshing...");
            return refreshAccessToken(token);
        }

        return token.getAccessToken();
    }

    // ─── Check if Spotify is connected ───────────────────────────────────────
    public boolean isConnected(Long userId) {
        return oauthTokenRepository.findByUserIdAndProvider(userId, PROVIDER).isPresent();
    }

    // ─── Fetch user's Spotify playlists ───────────────────────────────────────
    public List<PlaylistDTO> fetchUserPlaylists(User user) {
        String accessToken = getValidAccessToken(user);
        List<PlaylistDTO> playlists = new ArrayList<>();
        String nextUrl = SPOTIFY_API + "/me/playlists?limit=50";

        while (nextUrl != null) {
            JsonNode response = callSpotifyApi(nextUrl, accessToken);
            if (response == null || !response.has("items")) break;

            for (JsonNode item : response.get("items")) {
                if (item.isNull()) continue;
                playlists.add(mapToPlaylistDTO(item));
            }

            JsonNode next = response.get("next");
            nextUrl = (next != null && !next.isNull()) ? next.asText() : null;
        }

        log.info("Fetched {} Spotify playlists for user {}", playlists.size(), user.getId());
        return playlists;
    }

    // ─── Fetch tracks for a specific Spotify playlist ─────────────────────────
    public List<TrackDTO> fetchPlaylistTracks(User user, String playlistId) {
        String accessToken = getValidAccessToken(user);
        List<TrackDTO> tracks = new ArrayList<>();
        String nextUrl = SPOTIFY_API + "/playlists/" + playlistId + "/tracks?limit=100";

        while (nextUrl != null) {
            JsonNode response = callSpotifyApi(nextUrl, accessToken);
            if (response == null || !response.has("items")) break;

            for (JsonNode item : response.get("items")) {
                JsonNode track = item.get("track");
                if (track == null || track.isNull()) continue;
                tracks.add(mapToTrackDTO(track));
            }

            JsonNode next = response.get("next");
            nextUrl = (next != null && !next.isNull()) ? next.asText() : null;
        }

        log.info("Fetched {} tracks from Spotify playlist {}", tracks.size(), playlistId);
        return tracks;
    }

    // ─── Get Spotify user profile (to save their Spotify ID) ─────────────────
    public String fetchSpotifyUserId(User user) {
        String accessToken = getValidAccessToken(user);
        JsonNode response = callSpotifyApi(SPOTIFY_API + "/me", accessToken);
        if (response == null || !response.has("id")) {
            throw new BusinessException("Failed to fetch Spotify user profile");
        }
        return response.get("id").asText();
    }

    // ─── Internal: Make an authenticated GET call to Spotify API ─────────────
    private JsonNode callSpotifyApi(String url, String accessToken) {
        try {
            return webClientBuilder.build()
                    .get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Spotify API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new UnauthorizedException("Spotify token is invalid. Please reconnect Spotify.");
            }
            throw new BusinessException("Spotify API error: " + e.getMessage());
        }
    }

    // ─── Internal: Check token expiry ────────────────────────────────────────
    private boolean isTokenExpired(OAuthToken token) {
        if (token.getUpdatedAt() == null || token.getExpiresIn() == null) return false;
        LocalDateTime expiresAt = token.getUpdatedAt().plusSeconds(token.getExpiresIn());
        // Refresh 5 minutes early to avoid edge cases
        return LocalDateTime.now().isAfter(expiresAt.minusMinutes(5));
    }

    // ─── Internal: Map Spotify playlist JSON to DTO ───────────────────────────
    private PlaylistDTO mapToPlaylistDTO(JsonNode item) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setExternalId(item.path("id").asText(null));
        dto.setName(item.path("name").asText("Unnamed Playlist"));
        dto.setDescription(item.path("description").asText(""));
        dto.setPlatform("spotify");
        dto.setTrackCount(item.path("tracks").path("total").asInt(0));

        // Get the best quality image
        JsonNode images = item.get("images");
        if (images != null && images.isArray() && images.size() > 0) {
            dto.setCoverImageUrl(images.get(0).path("url").asText(null));
        }

        return dto;
    }

    // ─── Internal: Map Spotify track JSON to DTO ─────────────────────────────
    private TrackDTO mapToTrackDTO(JsonNode track) {
        TrackDTO dto = new TrackDTO();
        dto.setSpotifyId(track.path("id").asText(null));
        dto.setTitle(track.path("name").asText("Unknown Title"));
        dto.setSource("spotify");

        // Artist — take first one
        JsonNode artists = track.get("artists");
        if (artists != null && artists.isArray() && artists.size() > 0) {
            dto.setArtist(artists.get(0).path("name").asText("Unknown Artist"));
        }

        // Album info
        JsonNode album = track.get("album");
        if (album != null) {
            dto.setAlbum(album.path("name").asText(null));
            JsonNode images = album.get("images");
            if (images != null && images.isArray() && images.size() > 0) {
                dto.setCoverImageUrl(images.get(0).path("url").asText(null));
            }
        }

        // Duration in seconds (Spotify gives milliseconds)
        int durationMs = track.path("duration_ms").asInt(0);
        dto.setDuration(durationMs / 1000);

        return dto;
    }
}