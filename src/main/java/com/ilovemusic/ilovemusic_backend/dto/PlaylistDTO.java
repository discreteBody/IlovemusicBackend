package com.ilovemusic.ilovemusic_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaylistDTO {

    private Long id;                  // internal DB id (null for external playlists)
    private String externalId;        // Spotify/YouTube playlist id
    private String name;
    private String description;
    private String coverImageUrl;
    private String platform;          // "spotify" or "youtube"
    private Integer trackCount;       // total track count from platform
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TrackDTO> tracks;
}