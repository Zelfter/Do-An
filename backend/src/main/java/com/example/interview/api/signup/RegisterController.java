package com.example.interview.api.signup;

import com.example.interview.application.signup.RegisterService;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Isolated registration endpoint (append-only: brand-new file).
 *
 * <p>Mapped to {@code /api/v1/auth/signup} — a path distinct from the pre-existing
 * {@code /api/v1/auth/register} on {@code AuthController}, which is left
 * <b>untouched</b>. Using {@code /signup} avoids an ambiguous-handler-mapping
 * conflict at Spring startup (two methods mapping POST /api/v1/auth/register).
 *
 * <p>The path still lives under {@code /api/v1/auth/**}, so the existing security
 * chain's {@code permitAll} rule already covers it — <b>no change to
 * {@code SecurityConfig}</b> is required. This keeps the change strictly
 * append-only.
 *
 * <ul>
 *   <li>POST /api/v1/auth/signup → 201 {@link RegisterResponse}</li>
 *   <li>409 Conflict when the email is already registered (AC-AUTH-02)</li>
 *   <li>400 Bad Request on Bean Validation failure (handled by GlobalExceptionHandler)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    /** AC-AUTH-01 / AC-AUTH-02: create a new CANDIDATE account. */
    @PostMapping("/signup")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserJpaEntity user = registerService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse.from(user));
    }
}
