package com.example.interview.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request bodies for the auth endpoints (design §2.2).
 *
 * <p>Bean Validation is enforced with {@code @Valid} on the controller. Password is
 * only ever received here and immediately hashed — it is never stored or returned.
 */
public final class AuthRequest {

    private AuthRequest() {
    }

    /** POST /auth/register — AC-AUTH-01/02. Email RFC 5322, password ≥8 chars. */
    public record RegisterRequest(
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không đúng định dạng")
            String email,
            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 128, message = "Mật khẩu phải có ít nhất 8 ký tự")
            String password
    ) {
    }

    /** POST /auth/login — AC-AUTH-03/04/07. */
    public record LoginRequest(
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không đúng định dạng")
            String email,
            @NotBlank(message = "Mật khẩu không được để trống")
            String password
    ) {
    }

    /** POST /auth/refresh — AC-AUTH-05/06. */
    public record RefreshRequest(
            @NotBlank(message = "Refresh token không được để trống")
            String refreshToken
    ) {
    }

    /** POST /auth/logout — 204. */
    public record LogoutRequest(
            @NotBlank(message = "Refresh token không được để trống")
            String refreshToken
    ) {
    }
}
