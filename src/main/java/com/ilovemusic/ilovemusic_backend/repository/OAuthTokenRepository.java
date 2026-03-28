package com.ilovemusic.ilovemusic_backend.repository;

import com.ilovemusic.ilovemusic_backend.entity.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
    Optional<OAuthToken> findByUserIdAndProvider(Long userId, String provider);
}

