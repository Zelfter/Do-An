package com.example.interview.api.admin;

import com.example.interview.api.auth.AuthResponse;
import com.example.interview.application.auth.UserManagementService;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin user-management endpoints (design §2.3, FR-34).
 *
 * <p>All routes live under {@code /api/v1/admin/**} and are therefore protected
 * by {@code hasRole(ADMIN)} in {@link com.example.interview.infrastructure.security.SecurityConfig}
 * (FR-RBAC, NFR-19). A Candidate JWT yields 403; no JWT yields 401.
 *
 * <ul>
 *   <li>GET    /api/v1/admin/users                 → 200 [{id, email, role, status}]</li>
 *   <li>GET    /api/v1/admin/users/{id}            → 200 | 404</li>
 *   <li>PATCH  /api/v1/admin/users/{id}/status     → 200 | 400 (self-lock / invalid) | 404</li>
 * </ul>
 *
 * <p>None of the responses include {@code password_hash} — they all use
 * {@link AuthResponse.UserDto} which deliberately omits it (NFR-17).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * Lists users with optional role/status filters (FR-34).
     * Password hashes are never included in the projection.
     */
    @GetMapping
    public List<AuthResponse.UserDto> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        return userManagementService.listUsers(role, status).stream()
                .map(AuthResponse.UserDto::from)
                .toList();
    }

    /** Fetches a single user by id → 404 when not found. */
    @GetMapping("/{id}")
    public AuthResponse.UserDto getUser(@PathVariable UUID id) {
        UserJpaEntity user = userManagementService.getUserById(id);
        return AuthResponse.UserDto.from(user);
    }

    /**
     * Changes a user's status (ACTIVE ↔ LOCKED).
     *
     * <p>AC-USER-01: locking revokes all refresh tokens for the user.
     * <p>AC-USER-02: an Admin locking their own account → 400.
     */
    @PatchMapping("/{id}/status")
    public AuthResponse.UserDto updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusRequest request,
            @AuthenticationPrincipal UUID currentUserId) {
        UserJpaEntity updated = userManagementService.updateUserStatus(id, request.status(), currentUserId);
        return AuthResponse.UserDto.from(updated);
    }
}
