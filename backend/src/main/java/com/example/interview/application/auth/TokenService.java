package com.example.interview.application.auth;

import com.example.interview.api.common.ApiException;
import com.example.interview.domain.user.UserRole;
import com.example.interview.infrastructure.persistence.RefreshTokenJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import com.example.interview.infrastructure.persistence.RefreshTokenJpaRepository;
import com.example.interview.infrastructure.security.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Issues and validates JWT access tokens and server-side refresh tokens.
 *
 * <ul>
 *   <li><b>Access token</b>: HS256 JWT, payload {@code {sub, role, iat, exp}} (design §2.2).
 *       Stateless — no DB lookup on each request.</li>
 *   <li><b>Refresh token</b>: opaque random value; only its SHA-256 hash is persisted
 *       (design §2.1). The raw token is handed to the client exactly once.</li>
 * </ul>
 *
 * <p>Signing key comes from {@code app.jwt.secret} (env {@code JWT_SECRET}); it is never
 * hard-coded. HS256 requires ≥32 bytes, validated on startup.
 */
@Service
public class TokenService {

    private final JwtProperties jwtProperties;
    private final RefreshTokenJpaRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKey signingKey;
    private long accessTtlSeconds;
    private long refreshTtlSeconds;

    public TokenService(JwtProperties jwtProperties, RefreshTokenJpaRepository refreshTokenRepository) {
        this.jwtProperties = jwtProperties;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        // design.md §6: HS256 secret must be ≥32 bytes — fail fast if misconfigured.
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET phải có ít nhất 32 byte để ký HS256 (hiện có " + keyBytes.length + " byte)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTtlSeconds = jwtProperties.getAccessTtlSeconds();
        this.refreshTtlSeconds = jwtProperties.getRefreshTtlSeconds();
    }

    /** Parsed access-token claims: subject (user id) + role. */
    public record AccessTokenClaims(UUID userId, UserRole role) {
    }

    // ---- Access token (JWT) -------------------------------------------------

    /** Builds a signed HS256 JWT for the given user. */
    public String generateAccessToken(UUID userId, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(accessTtlSeconds, ChronoUnit.SECONDS)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies signature + expiry and returns claims.
     *
     * @throws JwtException if the token is malformed, has a bad signature, or is expired.
     *         The caller (JwtAuthFilter) treats any JwtException as "no authentication".
     */
    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        UUID userId = UUID.fromString(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get("role", String.class));
        return new AccessTokenClaims(userId, role);
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    // ---- Refresh token (opaque, SHA-256 hashed in DB) ----------------------

    /**
     * Creates a new refresh token for {@code user}, persists its SHA-256 hash, and
     * returns the raw token (to be sent to the client once).
     */
    @Transactional
    public String issueRefreshToken(UserJpaEntity user) {
        String rawToken = generateOpaqueToken();
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setUser(user);
        entity.setTokenHash(sha256(rawToken));
        entity.setExpiresAt(Instant.now().plus(refreshTtlSeconds, ChronoUnit.SECONDS));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates a raw refresh token against the DB.
     *
     * @return the persisted token entity (with its owning user) when valid.
     * @throws ApiException (401) when the token is unknown, revoked, expired, or when
     *         the owning account is LOCKED (defense-in-depth — locking also revokes).
     */
    @Transactional
    public RefreshTokenJpaEntity verifyRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ");
        }
        RefreshTokenJpaEntity token = refreshTokenRepository.findByTokenHash(sha256(rawToken));
        if (token == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ");
        }
        if (token.isRevoked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã bị thu hồi");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token đã hết hạn");
        }
        UserJpaEntity user = token.getUser();
        if (user != null && "LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản đã bị khóa");
        }
        return token;
    }

    /** Logout: revoke the supplied refresh token if it exists (idempotent → 204). */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        RefreshTokenJpaEntity token = refreshTokenRepository.findByTokenHash(sha256(rawToken));
        if (token != null && !token.isRevoked()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
    }

    /** FR-34 / AC-USER-01: revoke every refresh token for a user (used on lock). */
    @Transactional
    public int revokeAllForUser(UUID userId) {
        return refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ---- helpers -----------------------------------------------------------

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
