package com.example.interview.infrastructure.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration bound from {@code app.jwt.*} (application.yml).
 *
 * <p>The signing secret is sourced from the {@code JWT_SECRET} environment variable
 * (with a dev-only fallback in application.yml). It is NEVER hard-coded in Java.
 * HMAC-SHA256 (HS256) requires the secret to be at least 32 bytes — validated on
 * startup by {@link com.example.interview.application.auth.TokenService}.
 *
 * <p>NFR-17, FR-01-JWT, design.md §2.2 / §6.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HS256 signing secret, ≥32 bytes, from env {@code JWT_SECRET}. */
    @NotBlank
    private String secret;

    /** Access token TTL in minutes (default 60 — FR-01-JWT). */
    @Min(1)
    private long accessTtlMinutes = 60;

    /** Refresh token TTL in days (default 7 — FR-01-JWT). */
    @Min(1)
    private long refreshTtlDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTtlMinutes() {
        return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(long accessTtlMinutes) {
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public long getRefreshTtlDays() {
        return refreshTtlDays;
    }

    public void setRefreshTtlDays(long refreshTtlDays) {
        this.refreshTtlDays = refreshTtlDays;
    }

    /** Access token lifetime in seconds — returned to clients as {@code expires_in}. */
    public long getAccessTtlSeconds() {
        return accessTtlMinutes * 60;
    }

    /** Refresh token lifetime in seconds. */
    public long getRefreshTtlSeconds() {
        return refreshTtlDays * 24 * 60 * 60;
    }
}
