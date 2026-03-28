package com.ilovemusic.ilovemusic_backend.repository;

import com.ilovemusic.ilovemusic_backend.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {
    List<Track> findByPlaylistId(Long playlistId);
}

