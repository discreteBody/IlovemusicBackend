package com.ilovemusic.ilovemusic_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Conditional OAuth2 Configuration
 * 
 * This configuration allows OAuth2 to be conditionally enabled/disabled.
 * By default, OAuth2ClientAutoConfiguration is excluded to prevent bean initialization errors
 * when OAuth2 credentials are not provided.
 * 
 * To enable OAuth2:
 * 1. Set environment variables: SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET
 * 2. Set OAUTH2_EXCLUDE to an empty string or remove it
 * 3. Uncomment the OAuth2 configuration in application.yml
 */
@Slf4j
public class OAuth2Config {
    
    public OAuth2Config() {
        log.info("OAuth2 auto-configuration has been disabled. Set OAUTH2_EXCLUDE environment variable to enable it.");
    }
}

