package com.example.interview.application.auth;

import com.example.interview.api.common.ApiException;
import com.example.interview.domain.user.UserRole;
import com.example.interview.domain.user.UserStatus;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the auth flows (Task 03, M1).
 *
 * <p>Implements registration, login, token refresh and logout, plus a helper to
 * revoke a user's refresh tokens on lock (FR-34 / AC-USER-01 — used by Task 04).
 *
 * <p>Security rules enforced here:
 * <ul>
 *   <li>Email uniqueness → 409 (AC-AUTH-02).</li>
 *   <li>Bad credentials → 401 (AC-AUTH-04); the same message is used for
 *       "unknown email" and "wrong password" to avoid account enumeration.</li>
 *   <li>Locked account with valid credentials → 403 (AC-AUTH-07).</li>
 *   <li>Passwords are bcrypt-hashed (cost 12) and never returned in any response.</li>
 * </ul>
 */
@Service
public class AuthService {

    static final String BAD_CREDENTIALS = "Email hoặc mật khẩu không đúng";
    static final String ACCOUNT_LOCKED = "Tài khoản đã bị khóa";
    static final String EMAIL_EXISTS = "Email đã tồn tại trong hệ thống";

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserJpaRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /** AC-AUTH-01 / AC-AUTH-02. New users are CANDIDATE + ACTIVE. */
    @Transactional
    public UserJpaEntity register(String email, String password) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, EMAIL_EXISTS);
        }
        UserJpaEntity user = new UserJpaEntity();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.CANDIDATE.name());
        user.setStatus(UserStatus.ACTIVE.name());
        return userRepository.save(user);
    }

    /**
     * AC-AUTH-03 / AC-AUTH-04 / AC-AUTH-07.
     *
     * @return the authenticated user (with a freshly issued refresh token already
     *         persisted); the controller pairs it with a new access token.
     */
    @Transactional
    public LoginResult login(String email, String password) {
        UserJpaEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        // Verify password first so that only valid credentials can reveal a locked state.
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS);
        }
        if (UserStatus.LOCKED.name().equalsIgnoreCase(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, ACCOUNT_LOCKED);
        }
        String refreshToken = tokenService.issueRefreshToken(user);
        return new LoginResult(user, refreshToken);
    }

    /** AC-AUTH-05 / AC-AUTH-06: exchange a valid refresh token for a new access token. */
    @Transactional
    public UUID refresh(String rawRefreshToken) {
        // verifyRefreshToken throws 401 on invalid/expired/revoked/locked.
        UserJpaEntity user = tokenService.verifyRefreshToken(rawRefreshToken).getUser();
        return user.getId();
    }

    /** Logout: revoke the supplied refresh token (idempotent). */
    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revokeRefreshToken(rawRefreshToken);
    }

    /** GET /api/v1/me. */
    @Transactional(readOnly = true)
    public UserJpaEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    /** FR-34 / AC-USER-01: revoke all refresh tokens when an account is locked. */
    @Transactional
    public int revokeAllTokensForUser(UUID userId) {
        return tokenService.revokeAllForUser(userId);
    }

    /** Carries the authenticated user + the raw refresh token for the login response. */
    public record LoginResult(UserJpaEntity user, String rawRefreshToken) {
    }
}
