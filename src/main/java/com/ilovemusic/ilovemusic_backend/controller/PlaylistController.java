package com.ilovemusic.ilovemusic_backend.controller;

import com.ilovemusic.ilovemusic_backend.common.exception.UnauthorizedException;
import com.ilovemusic.ilovemusic_backend.common.response.ApiResponse;
import com.ilovemusic.ilovemusic_backend.dto.PlaylistDTO;
import com.ilovemusic.ilovemusic_backend.dto.TrackDTO;
import com.ilovemusic.ilovemusic_backend.repository.UserRepository;
import com.ilovemusic.ilovemusic_backend.service.PlaylistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.ilovemusic.ilovemusic_backend.service.SpotifyService;
import com.ilovemusic.ilovemusic_backend.entity.User;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/playlists")
@Slf4j
public class PlaylistController {

    private final SpotifyService spotifyService;
    private final PlaylistService playlistService;
    private final UserRepository userRepository;   // ✅ Added

    public PlaylistController(PlaylistService playlistService,
                              UserRepository userRepository,
                              SpotifyService spotifyService) {
        this.playlistService = playlistService;
        this.userRepository = userRepository;
        this.spotifyService = spotifyService;
    }

    // ─── NEW: Fetch live playlists from Spotify ───────────────────────────────
    @GetMapping("/spotify")
    public ResponseEntity<ApiResponse<List<PlaylistDTO>>> getSpotifyPlaylists(
            Authentication authentication) {
        User user = getUser(authentication);
        List<PlaylistDTO> playlists = spotifyService.fetchUserPlaylists(user);
        return ResponseEntity.ok(
                ApiResponse.success(playlists,
                        String.format("Found %d Spotify playlists", playlists.size()))
        );
    }

    // ─── NEW: Fetch live tracks from a Spotify playlist ──────────────────────
    @GetMapping("/spotify/{playlistId}/tracks")
    public ResponseEntity<ApiResponse<List<TrackDTO>>> getSpotifyPlaylistTracks(
            @PathVariable String playlistId,
            Authentication authentication) {
        User user = getUser(authentication);
        List<TrackDTO> tracks = spotifyService.fetchPlaylistTracks(user, playlistId);
        return ResponseEntity.ok(
                ApiResponse.success(tracks,
                        String.format("Found %d tracks", tracks.size()))
        );
    }


    @PostMapping
    public ResponseEntity<ApiResponse<PlaylistDTO>> createPlaylist(
            @RequestBody PlaylistDTO playlistDTO,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        PlaylistDTO created = playlistService.createPlaylist(userId, playlistDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Playlist created successfully", 201));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PlaylistDTO>>> getUserPlaylists(
            @RequestParam(required = false) String platform,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        List<PlaylistDTO> playlists = playlistService.getUserPlaylists(userId);
        if (platform != null && !platform.isEmpty()) {
            playlists = playlistService.filterPlaylistsByPlatform(playlists, platform);
        }
        return ResponseEntity.ok(
                ApiResponse.success(playlists, String.format("Found %d playlists", playlists.size()))
        );
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<ApiResponse<PlaylistDTO>> getPlaylist(
            @PathVariable Long playlistId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        PlaylistDTO playlist = playlistService.getPlaylist(playlistId, userId);
        return ResponseEntity.ok(ApiResponse.success(playlist, "Playlist retrieved successfully"));
    }

    @PutMapping("/{playlistId}")
    public ResponseEntity<ApiResponse<PlaylistDTO>> updatePlaylist(
            @PathVariable Long playlistId,
            @RequestBody PlaylistDTO playlistDTO,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        PlaylistDTO updated = playlistService.updatePlaylist(playlistId, userId, playlistDTO);
        return ResponseEntity.ok(ApiResponse.success(updated, "Playlist updated successfully"));
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<ApiResponse<Void>> deletePlaylist(
            @PathVariable Long playlistId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        playlistService.deletePlaylist(playlistId, userId);
        return ResponseEntity.ok(ApiResponse.success("Playlist deleted successfully"));
    }

    @PostMapping("/{playlistId}/tracks")
    public ResponseEntity<ApiResponse<TrackDTO>> addTrackToPlaylist(
            @PathVariable Long playlistId,
            @RequestBody TrackDTO trackDTO,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        TrackDTO created = playlistService.addTrackToPlaylist(playlistId, userId, trackDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Track added successfully", 201));
    }

    @GetMapping("/{playlistId}/tracks")
    public ResponseEntity<ApiResponse<List<TrackDTO>>> getPlaylistTracks(
            @PathVariable Long playlistId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        List<TrackDTO> tracks = playlistService.getPlaylistTracks(playlistId, userId);
        return ResponseEntity.ok(
                ApiResponse.success(tracks, String.format("Found %d tracks", tracks.size()))
        );
    }

    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<ApiResponse<Void>> removeTrackFromPlaylist(
            @PathVariable Long playlistId,
            @PathVariable Long trackId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        playlistService.removeTrackFromPlaylist(playlistId, trackId, userId);
        return ResponseEntity.ok(ApiResponse.success("Track removed successfully"));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportPlaylist(
            @RequestParam Long playlistId,
            @RequestParam String fromPlatform,
            @RequestParam String toPlatform,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        log.info("Exporting playlist {} from {} to {} for user {}",
                playlistId, fromPlatform, toPlatform, userId);

        Map<String, Object> result = Map.of(
                "status", "initiated",
                "playlist_id", playlistId,
                "from_platform", fromPlatform,
                "to_platform", toPlatform,
                "export_id", "export-" + System.currentTimeMillis()
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Export initiated successfully"));
    }

    // Update extractUserIdFromAuth to use getUser
    private Long extractUserIdFromAuth(Authentication authentication) {
        return getUser(authentication).getId();
    }



    // ─── Helper ───────────────────────────────────────────────────────────────
    private User getUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + username));
    }

}