package com.ilovemusic.ilovemusic_backend.repository;

import com.ilovemusic.ilovemusic_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findBySpotifyId(String spotifyId);
    Optional<User> findByYoutubeId(String youtubeId);
}

