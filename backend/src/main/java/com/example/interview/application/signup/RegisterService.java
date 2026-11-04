package com.example.interview.application.signup;

import com.example.interview.api.common.ApiException;
import com.example.interview.domain.user.UserRole;
import com.example.interview.domain.user.UserStatus;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isolated registration service (append-only: brand-new file, no existing file
 * modified).
 *
 * <p>Reuses the existing {@link UserJpaRepository} and the existing BCrypt
 * {@link PasswordEncoder} bean (cost 12, NFR-17) so newly created accounts are
 * hashed with exactly the same scheme as existing users — including the V010
 * admin seed (pgcrypto {@code crypt(.., gen_salt('bf', 12))}). This keeps the
 * password format compatible with the existing schema <b>without</b> altering
 * it. Writes only to existing {@code users} columns (email, password_hash,
 * role, status) — no new column, no new table.
 *
 * <p>Business rules enforced here:
 * <ul>
 *   <li>Duplicate email → 409 Conflict (AC-AUTH-02).</li>
 *   <li>New accounts are role=CANDIDATE, status=ACTIVE (same defaults as the
 *       existing {@code AuthService#register}).</li>
 *   <li>Password is bcrypt-hashed, never stored/returned as plaintext.</li>
 * </ul>
 */
@Service
public class RegisterService {

    static final String EMAIL_EXISTS = "Email đã tồn tại trong hệ thống";

    private final UserJpaRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterService(UserJpaRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a new CANDIDATE account.
     *
     * @param email    RFC 5322 email (validated upstream)
     * @param password raw password (≥8 chars, validated upstream)
     * @return the persisted user entity (with generated id)
     * @throws ApiException (409) if the email is already registered
     */
    @Transactional
    public UserJpaEntity register(String email, String password) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, EMAIL_EXISTS);
        }
        UserJpaEntity user = new UserJpaEntity();
        user.setEmail(email);
        // NFR-17: bcrypt cost 12 via the shared PasswordEncoder bean.
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(UserRole.CANDIDATE.name());
        user.setStatus(UserStatus.ACTIVE.name());
        return userRepository.save(user);
    }
}
