package com.ilovemusic.ilovemusic_backend.controller;

import com.ilovemusic.ilovemusic_backend.common.exception.UnauthorizedException;
import com.ilovemusic.ilovemusic_backend.common.response.ApiResponse;
import com.ilovemusic.ilovemusic_backend.entity.User;
import com.ilovemusic.ilovemusic_backend.repository.UserRepository;
import com.ilovemusic.ilovemusic_backend.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${spring.security.oauth2.client.registration.spotify.client-id:}")
    private String spotifyClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String googleClientId;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public AuthController(JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Username already taken",
                            "USERNAME_TAKEN", 400));
        }

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email already registered",
                            "EMAIL_TAKEN", 400));
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

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid Authorization header",
                            "INVALID_HEADER", 400));
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token is invalid or expired",
                            "INVALID_TOKEN", 401));
        }

        String username = jwtUtil.extractUsername(token);
        String newToken = jwtUtil.generateToken(username);

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("token", newToken), "Token refreshed"));
    }

    // ─── Logout ───────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // Stateless JWT — client just discards the token
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    // ─── Check Connections ────────────────────────────────────────────────────
    @GetMapping("/connections")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkConnections(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.extractUsername(token);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("spotify_connected", user.getSpotifyId() != null);
        data.put("youtube_connected", user.getYoutubeId() != null);

        return ResponseEntity.ok(ApiResponse.success(data, "Connections retrieved"));
    }

    // ─── Spotify OAuth redirect URL ───────────────────────────────────────────
    @GetMapping("/spotify")
    public ResponseEntity<ApiResponse<Map<String, String>>> spotifyRedirect() {
        String redirectUrl = "https://accounts.spotify.com/authorize"
                + "?client_id=" + spotifyClientId
                + "&response_type=code"
                + "&redirect_uri=" + appBaseUrl + "/ilovemusic/api/auth/spotify/callback"
                + "&scope=playlist-read-private%20playlist-read-collaborative"
                + "%20playlist-modify-public%20playlist-modify-private";

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("redirect_url", redirectUrl),
                        "Spotify OAuth URL generated"));
    }

    // ─── Spotify OAuth callback ───────────────────────────────────────────────
    @GetMapping("/spotify/callback")
    public ResponseEntity<ApiResponse<Map<String, String>>> spotifyCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        log.info("Spotify OAuth callback received");
        // Full token exchange will be implemented in SpotifyService
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of("message", "Spotify callback received — token exchange coming soon"),
                        "Callback received"));
    }

    // ─── YouTube OAuth redirect URL ───────────────────────────────────────────
    @GetMapping("/youtube")
    public ResponseEntity<ApiResponse<Map<String, String>>> youtubeRedirect() {
        String redirectUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + googleClientId
                + "&response_type=code"
                + "&redirect_uri=" + appBaseUrl + "/ilovemusic/api/auth/youtube/callback"
                + "&scope=https://www.googleapis.com/auth/youtube"
                + "&access_type=offline&prompt=consent";

        return ResponseEntity.ok(
                ApiResponse.success(Map.of("redirect_url", redirectUrl),
                        "YouTube OAuth URL generated"));
    }

    // ─── YouTube OAuth callback ───────────────────────────────────────────────
    @GetMapping("/youtube/callback")
    public ResponseEntity<ApiResponse<Map<String, String>>> youtubeCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state) {

        log.info("YouTube OAuth callback received");
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of("message", "YouTube callback received — token exchange coming soon"),
                        "Callback received"));
    }
}