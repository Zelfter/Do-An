package com.example.interview.api.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for PATCH /api/v1/admin/users/{id}/status (design §2.3).
 *
 * <p>The {@code status} value is validated by the service layer to be either
 * {@code ACTIVE} or {@code LOCKED} (FR-34). {@code @NotBlank} guards against an
 * empty/missing field here so the controller-level {@code @Valid} produces a
 * clear 400 before the service is reached.
 */
public record UserStatusRequest(
        @NotBlank(message = "Trạng thái không được để trống")
        String status
) {
}
