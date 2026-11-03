package com.example.interview.application.auth;

import com.example.interview.api.common.ApiException;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for Admin user management (Task 04, M1 — FR-34).
 *
 * <p>Encapsulates listing, fetching and status updates for users. The actual
 * refresh-token revocation on lock is delegated to {@link AuthService} which
 * already exposes {@link AuthService#revokeAllTokensForUser(UUID)} (AC-USER-01).
 *
 * <p>Security rules enforced here:
 * <ul>
 *   <li>Invalid status value → 400.</li>
 *   <li>Unknown user id → 404.</li>
 *   <li>AC-USER-02: an Admin locking their own account → 400
 *       ("Không thể khóa tài khoản của chính mình").</li>
 *   <li>AC-USER-01: changing status to LOCKED revokes every refresh token for
 *       that user immediately, so active sessions cannot be refreshed.</li>
 * </ul>
 */
@Service
public class UserManagementService {

    static final String SELF_LOCK = "Không thể khóa tài khoản của chính mình";
    static final String INVALID_STATUS = "Trạng thái không hợp lệ, chỉ chấp nhận ACTIVE hoặc LOCKED";
    static final String USER_NOT_FOUND = "Không tìm thấy người dùng";

    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "LOCKED");

    private final UserJpaRepository userRepository;
    private final AuthService authService;

    public UserManagementService(UserJpaRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Lists users with optional role/status filters (FR-34: "filter theo role và status").
     * When both filters are blank/null, every user is returned.
     */
    @Transactional(readOnly = true)
    public List<UserJpaEntity> listUsers(String roleFilter, String statusFilter) {
        boolean hasRole = roleFilter != null && !roleFilter.isBlank();
        boolean hasStatus = statusFilter != null && !statusFilter.isBlank();
        if (hasRole && hasStatus) {
            return userRepository.findByRoleIgnoreCaseAndStatusIgnoreCase(roleFilter, statusFilter);
        }
        if (hasRole) {
            return userRepository.findByRoleIgnoreCase(roleFilter);
        }
        if (hasStatus) {
            return userRepository.findByStatusIgnoreCase(statusFilter);
        }
        return userRepository.findAll();
    }

    /** GET /api/v1/admin/users/{id} → 404 when the user does not exist. */
    @Transactional(readOnly = true)
    public UserJpaEntity getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
    }

    /**
     * PATCH /api/v1/admin/users/{id}/status.
     *
     * @param targetUserId   the user whose status is being changed.
     * @param newStatus      the requested status ("ACTIVE" or "LOCKED").
     * @param currentUserId  the authenticated admin's own user id (for self-lock check).
     * @return the updated user entity.
     * @throws ApiException 400 on invalid status, 400 on self-lock, 404 on unknown user.
     */
    @Transactional
    public UserJpaEntity updateUserStatus(UUID targetUserId, String newStatus, UUID currentUserId) {
        // 1. Validate the status value (only ACTIVE / LOCKED allowed).
        if (newStatus == null || !VALID_STATUSES.contains(newStatus.trim().toUpperCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_STATUS);
        }
        String normalizedStatus = newStatus.trim().toUpperCase();

        // 2. Ensure the target user exists.
        UserJpaEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));

        // 3. AC-USER-02: an Admin cannot lock their own account.
        if (targetUserId.equals(currentUserId) && "LOCKED".equals(normalizedStatus)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, SELF_LOCK);
        }

        // 4. AC-USER-01: revoke all refresh tokens when the account is locked.
        if ("LOCKED".equals(normalizedStatus)) {
            authService.revokeAllTokensForUser(targetUserId);
        }

        user.setStatus(normalizedStatus);
        return userRepository.save(user);
    }
}
