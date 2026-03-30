package com.ilovemusic.ilovemusic_backend.controller;

import com.ilovemusic.ilovemusic_backend.common.exception.UnauthorizedException;
import com.ilovemusic.ilovemusic_backend.common.response.ApiResponse;
import com.ilovemusic.ilovemusic_backend.entity.User;
import com.ilovemusic.ilovemusic_backend.repository.UserRepository;
import com.ilovemusic.ilovemusic_backend.service.SpotifyService;
import com.ilovemusic.ilovemusic_backend.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SpotifyService spotifyService;

    // ✅ With these
    @Value("${spotify.client.id:}")
    private String spotifyClientId;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthController(JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          SpotifyService spotifyService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.spotifyService = spotifyService;
    }

    // ─── Register ─────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String email    = request.get("email");
        String password = request.get("password");

        if (username == null || email == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("username, email and password are required",
                            "VALIDATION_ERROR", 400));
        }

        if (password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 8 characters",
                            "WEAK_PASSWORD", 400));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username already taken", "USERNAME_TAKEN", 400));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email already registered", "EMAIL_TAKEN", 400));
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        String token = jwtUtil.generateToken(username);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("email", email);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Registration successful", 201));
    }

    // ─── Login ────────────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("username and password are required",
                            "VALIDATION_ERROR", 400));
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getPassword() == null ||
                !passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(username);
        Map<String, String> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("email", user.getEmail());

        return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
    }

    // ─── Refresh Token ────────────────────────────────────────────────────────
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token is invalid or expired", "INVALID_TOKEN", 401));
        }

        String username = jwtUtil.extractUsername(token);
        String newToken = jwtUtil.generateToken(username);

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("token", newToken), "Token refreshed"));
    }

    // ─── Logout ───────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    // ─── Check Connections ────────────────────────────────────────────────────
    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkConnections(
            @RequestHeader("Authorization") String authHeader) {

        String username = jwtUtil.extractUsername(authHeader.replace("Bearer ", ""));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("spotify_connected", spotifyService.isConnected(user.getId()));
        data.put("youtube_connected", user.getYoutubeId() != null);

        return ResponseEntity.ok(ApiResponse.success(data, "Connections retrieved"));
    }

    // ─── Spotify: Generate OAuth URL ─────────────────────────────────────────
    // The JWT token is passed as state so we can identify the user in the callback
    @GetMapping("/spotify")
    public ResponseEntity<ApiResponse<Map<String, String>>> spotifyRedirect(
            @RequestHeader("Authorization") String authHeader) {

        String jwtToken = authHeader.replace("Bearer ", "");

        // Use 127.0.0.1 instead of localhost to match Spotify app configuration
        String baseUrlForCallback = appBaseUrl.replace("localhost", "127.0.0.1");
        String redirectUrl = "https://accounts.spotify.com/authorize"
                + "?client_id=" + spotifyClientId
                + "&response_type=code"
                + "&redirect_uri=" + baseUrlForCallback + "/ilovemusic/api/auth/spotify/callback"
                + "&scope=playlist-read-private%20playlist-read-collaborative"
                + "%20playlist-modify-public%20playlist-modify-private"
                + "&state=" + jwtToken;  // ✅ pass JWT as state

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("redirect_url", redirectUrl),
                        "Spotify OAuth URL generated"));
    }

    // ─── Spotify: Real OAuth Callback ────────────────────────────────────────
    @GetMapping("/spotify/callback")
    public ResponseEntity<Void> spotifyCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        log.info("Spotify OAuth callback received");

        if (state == null || !jwtUtil.validateToken(state)) {
            log.error("Invalid state/JWT in Spotify callback");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/dashboard?error=spotify_auth_failed"))
                    .build();
        }

        try {
            // Identify the user from JWT state
            String username = jwtUtil.extractUsername(state);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Exchange the code for tokens and save to DB
            spotifyService.exchangeCodeForTokens(user, code);

            // Save Spotify user ID to the user record
            String spotifyUserId = spotifyService.fetchSpotifyUserId(user);
            user.setSpotifyId(spotifyUserId);
            userRepository.save(user);

            log.info("Spotify connected successfully for user {}", username);

            // Redirect back to frontend with success
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/dashboard/playlists?connected=spotify"))
                    .build();

        } catch (Exception e) {
            log.error("Spotify callback error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/dashboard?error=spotify_auth_failed"))
                    .build();
        }
    }

    // ─── YouTube: Generate OAuth URL ─────────────────────────────────────────
    @GetMapping("/youtube")
    public ResponseEntity<ApiResponse<Map<String, String>>> youtubeRedirect(
            @RequestHeader("Authorization") String authHeader) {

        String jwtToken = authHeader.replace("Bearer ", "");

        // Use 127.0.0.1 instead of localhost to match OAuth app configuration
        String baseUrlForCallback = appBaseUrl.replace("localhost", "127.0.0.1");
        String redirectUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleClientId
                + "&response_type=code"
                + "&redirect_uri=" + baseUrlForCallback + "/ilovemusic/api/auth/youtube/callback"
                + "&scope=https://www.googleapis.com/auth/youtube.readonly"
                + "%20https://www.googleapis.com/auth/youtube"
                + "&access_type=offline&prompt=consent"
                + "&state=" + jwtToken;

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("redirect_url", redirectUrl),
                        "YouTube OAuth URL generated"));
    }

    // ─── YouTube: Callback (stub — next phase) ────────────────────────────────
    @GetMapping("/youtube/callback")
    public ResponseEntity<Void> youtubeCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        log.info("YouTube OAuth callback received — implementation coming next");
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/dashboard/playlists?connected=youtube"))
                .build();
    }
}