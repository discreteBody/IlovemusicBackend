package com.ilovemusic.ilovemusic_backend.service;

import com.ilovemusic.ilovemusic_backend.common.exception.BusinessException;
import com.ilovemusic.ilovemusic_backend.common.exception.ResourceNotFoundException;
import com.ilovemusic.ilovemusic_backend.dto.PlaylistDTO;
import com.ilovemusic.ilovemusic_backend.dto.TrackDTO;
import com.ilovemusic.ilovemusic_backend.entity.Playlist;
import com.ilovemusic.ilovemusic_backend.entity.Track;
import com.ilovemusic.ilovemusic_backend.entity.User;
import com.ilovemusic.ilovemusic_backend.repository.PlaylistRepository;
import com.ilovemusic.ilovemusic_backend.repository.TrackRepository;
import com.ilovemusic.ilovemusic_backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;

    public PlaylistService(PlaylistRepository playlistRepository,
                          TrackRepository trackRepository,
                          UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new playlist for a user
     */
    public PlaylistDTO createPlaylist(Long userId, PlaylistDTO playlistDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (playlistDTO.getName() == null || playlistDTO.getName().isEmpty()) {
            throw new BusinessException("Playlist name cannot be empty");
        }

        Playlist playlist = new Playlist();
        playlist.setUser(user);
        playlist.setName(playlistDTO.getName());
        playlist.setDescription(playlistDTO.getDescription());
        playlist.setCoverImageUrl(playlistDTO.getCoverImageUrl());

        Playlist savedPlaylist = playlistRepository.save(playlist);
        log.info("Created playlist {} for user {}", savedPlaylist.getId(), userId);
        return mapToDTO(savedPlaylist);
    }

    /**
     * Get all playlists for a user with Stream API
     */
    public List<PlaylistDTO> getUserPlaylists(Long userId) {
        return playlistRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific playlist
     */
    public PlaylistDTO getPlaylist(Long playlistId, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId));
        return mapToDTO(playlist);
    }

    /**
     * Update a playlist
     */
    public PlaylistDTO updatePlaylist(Long playlistId, Long userId, PlaylistDTO playlistDTO) {
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId));

        if (playlistDTO.getName() != null && !playlistDTO.getName().isEmpty()) {
            playlist.setName(playlistDTO.getName());
        }
        if (playlistDTO.getDescription() != null) {
            playlist.setDescription(playlistDTO.getDescription());
        }
        if (playlistDTO.getCoverImageUrl() != null) {
            playlist.setCoverImageUrl(playlistDTO.getCoverImageUrl());
        }

        Playlist updatedPlaylist = playlistRepository.save(playlist);
        log.info("Updated playlist {}", playlistId);
        return mapToDTO(updatedPlaylist);
    }

    /**
     * Delete a playlist
     */
    public void deletePlaylist(Long playlistId, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId));
        playlistRepository.delete(playlist);
        log.info("Deleted playlist {}", playlistId);
    }

    /**
     * Add a track to a playlist
     */
    public TrackDTO addTrackToPlaylist(Long playlistId, Long userId, TrackDTO trackDTO) {
        if (trackDTO.getTitle() == null || trackDTO.getTitle().isEmpty()) {
            throw new BusinessException("Track title cannot be empty");
        }

        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId));

        Track track = new Track();
        track.setPlaylist(playlist);
        track.setTitle(trackDTO.getTitle());
        track.setArtist(trackDTO.getArtist());
        track.setAlbum(trackDTO.getAlbum());
        track.setSpotifyId(trackDTO.getSpotifyId());
        track.setYoutubeId(trackDTO.getYoutubeId());
        track.setCoverImageUrl(trackDTO.getCoverImageUrl());
        track.setDuration(trackDTO.getDuration());
        track.setSource(trackDTO.getSource());

        Track savedTrack = trackRepository.save(track);
        log.info("Added track {} to playlist {}", savedTrack.getId(), playlistId);
        return mapTrackToDTO(savedTrack);
    }

    /**
     * Remove a track from a playlist
     */
    public void removeTrackFromPlaylist(Long playlistId, Long trackId, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId));

        Track track = trackRepository.findById(trackId)
                .orElseThrow(() -> new ResourceNotFoundException("Track", "id", trackId));

        if (!track.getPlaylist().getId().equals(playlistId)) {
            throw new BusinessException("Track does not belong to this playlist");
        }

        trackRepository.delete(track);
        log.info("Removed track {} from playlist {}", trackId, playlistId);
    }

    /**
     * Get all tracks in a playlist with Stream API
     */
    public List<TrackDTO> getPlaylistTracks(Long playlistId, Long userId) {
        Playlist playlist = playlistRepository.findByIdAndUserId(playlistId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist", "id", playlistId));

        return playlist.getTracks().stream()
                .map(this::mapTrackToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Filter playlists by platform (based on track sources) with Stream API
     */
    public List<PlaylistDTO> filterPlaylistsByPlatform(List<PlaylistDTO> playlists, String platform) {
        if (platform == null || platform.isEmpty()) {
            return playlists;
        }

        return playlists.stream()
                .filter(playlist -> {
                    if (playlist.getTracks() == null || playlist.getTracks().isEmpty()) {
                        return true;
                    }
                    return playlist.getTracks()
                            .stream()
                            .anyMatch(track -> platform.equalsIgnoreCase(track.getSource()));
                })
                .collect(Collectors.toList());
    }

    private PlaylistDTO mapToDTO(Playlist playlist) {
        PlaylistDTO dto = new PlaylistDTO();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        dto.setDescription(playlist.getDescription());
        dto.setCoverImageUrl(playlist.getCoverImageUrl());
        dto.setCreatedAt(playlist.getCreatedAt());
        dto.setUpdatedAt(playlist.getUpdatedAt());
        if (playlist.getTracks() != null) {
            dto.setTracks(playlist.getTracks().stream()
                    .map(this::mapTrackToDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private TrackDTO mapTrackToDTO(Track track) {
        TrackDTO dto = new TrackDTO();
        dto.setId(track.getId());
        dto.setTitle(track.getTitle());
        dto.setArtist(track.getArtist());
        dto.setAlbum(track.getAlbum());
        dto.setSpotifyId(track.getSpotifyId());
        dto.setYoutubeId(track.getYoutubeId());
        dto.setCoverImageUrl(track.getCoverImageUrl());
        dto.setDuration(track.getDuration());
        dto.setSource(track.getSource());
        dto.setCreatedAt(track.getCreatedAt());
        return dto;
    }
}
