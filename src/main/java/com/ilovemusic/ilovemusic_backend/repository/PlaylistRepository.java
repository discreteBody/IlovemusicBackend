package com.ilovemusic.ilovemusic_backend.repository;

import com.ilovemusic.ilovemusic_backend.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    // ✅ Fetch join — loads all tracks in one query instead of N+1
    @Query("SELECT p FROM Playlist p LEFT JOIN FETCH p.tracks WHERE p.user.id = :userId")
    List<Playlist> findByUserId(@Param("userId") Long userId);

    Optional<Playlist> findByIdAndUserId(Long id, Long userId);
}

