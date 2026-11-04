package com.example.interview.api.signup;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/v1/auth/signup (isolated register endpoint).
 *
 * <p>Maps <b>exactly</b> to the existing {@code users} table columns — only
 * {@code email} and {@code password} are accepted. The existing schema
 * (Flyway V001__create_users_and_refresh_tokens.sql) has NO "name" column, and
 * per the append-only / no-schema-change constraint none is introduced here.
 *
 * <p>Password is received only here and is immediately bcrypt-hashed in the
 * service layer (NFR-17); it is never stored as plaintext or returned in any
 * response.
 */
public record RegisterRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 128, message = "Mật khẩu phải có ít nhất 8 ký tự")
        String password
) {
}
