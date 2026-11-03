package com.example.interview;

import com.example.interview.infrastructure.persistence.RefreshTokenJpaEntity;
import com.example.interview.infrastructure.persistence.RefreshTokenJpaRepository;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for M1 — Auth + RBAC (Task 03).
 *
 * <p>Covers all nine acceptance criteria (AC-AUTH-01..07, AC-RBAC-01..02) end to end
 * through the Spring MVC + Security filter chain, backed by an H2 in-memory DB
 * (application-test.yml). No mocks of business logic are used — real bcrypt hashing,
 * real JWT signing and real DB persistence are exercised.
 *
 * <p>{@code @Transactional} rolls back after each test, keeping the in-memory DB clean
 * so the unique-email constraint never collides across tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    private static final String PASSWORD = "Password123!";

    // ---- helpers -----------------------------------------------------------

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    /** Registers a user and returns the parsed response body. */
    private JsonNode register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Logs in and returns the parsed response body (access + refresh tokens). */
    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Decodes the JWT payload (no signature verification) to inspect claims. */
    private JsonNode decodeJwtPayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        return objectMapper.readTree(payload);
    }

    private UUID userIdByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    // ---- AC-AUTH-01: register new user → 201, ACTIVE, no password in response

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-01: register new user → 201, ACTIVE, password not in response")
    void register_newUser_returns201ActiveAndHidesPassword() throws Exception {
        String email = uniqueEmail();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // NFR-17 / AC-AUTH-01: password must never appear in any response.
        org.junit.jupiter.api.Assertions.assertFalse(body.contains(PASSWORD),
                "Raw password must not appear in the response body");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("password"),
                "No field named password may appear in the response body");

        // User persisted as ACTIVE in the DB.
        UserJpaEntity saved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", saved.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("CANDIDATE", saved.getRole());
        org.junit.jupiter.api.Assertions.assertNotEquals(PASSWORD, saved.getPasswordHash(),
                "DB must store a hash, not the raw password");
    }

    // ---- AC-AUTH-02: duplicate email → 409 with a clear message

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-02: duplicate email → 409 with clear message")
    void register_duplicateEmail_returns409() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        String message = objectMapper.readTree(result.getResponse().getContentAsString()).get("message").asText();
        org.junit.jupiter.api.Assertions.assertFalse(message.isBlank(), "409 message must be clear and non-empty");
    }

    // ---- AC-AUTH-03: valid credentials → 200 with access_token (JWT) + refresh_token

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-03: valid login → 200 with JWT access_token and refresh_token")
    void login_validCredentials_returns200WithTokens() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        JsonNode body = login(email, PASSWORD);
        mockMvc.perform(post("/api/v1/auth/login") // re-assert status here
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk());

        String accessToken = body.get("accessToken").asText();
        String refreshToken = body.get("refreshToken").asText();
        long expiresIn = body.get("expiresIn").asLong();

        org.junit.jupiter.api.Assertions.assertFalse(accessToken.isBlank(), "access_token must be present");
        org.junit.jupiter.api.Assertions.assertEquals(3, accessToken.split("\\.").length,
                "access_token must be a JWT (header.payload.signature)");
        org.junit.jupiter.api.Assertions.assertFalse(refreshToken.isBlank(), "refresh_token must be present");
        org.junit.jupiter.api.Assertions.assertTrue(expiresIn > 0, "expires_in must be positive");

        // JWT payload carries the user id and role.
        JsonNode payload = decodeJwtPayload(accessToken);
        org.junit.jupiter.api.Assertions.assertEquals("CANDIDATE", payload.get("role").asText());
        org.junit.jupiter.api.Assertions.assertEquals(userIdByEmail(email).toString(), payload.get("sub").asText());
    }

    // ---- AC-AUTH-04: wrong password → 401

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-04: wrong credentials → 401")
    void login_wrongPassword_returns401() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", "WrongPassword!"))))
                .andExpect(status().isUnauthorized());
    }

    // ---- AC-AUTH-05: valid refresh token → 200 with a new access_token

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-05: valid refresh token → 200 new access_token")
    void refresh_validToken_returns200NewAccessToken() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        JsonNode loginBody = login(email, PASSWORD);
        String refreshToken = loginBody.get("refreshToken").asText();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists())
                .andReturn();

        String newAccessToken = objectMapper
                .readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        org.junit.jupiter.api.Assertions.assertEquals(3, newAccessToken.split("\\.").length,
                "new access_token must be a JWT");
        // The new access token must reference the same user.
        JsonNode payload = decodeJwtPayload(newAccessToken);
        org.junit.jupiter.api.Assertions.assertEquals(userIdByEmail(email).toString(), payload.get("sub").asText());
        org.junit.jupiter.api.Assertions.assertEquals("CANDIDATE", payload.get("role").asText());
    }

    // ---- AC-AUTH-06: invalid refresh token → 401

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-06: invalid refresh token → 401")
    void refresh_invalidToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("refreshToken", "not-a-real-token"))))
                .andExpect(status().isUnauthorized());
    }

    // ---- AC-AUTH-06 (expired): expired refresh token → 401

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-06: expired refresh token → 401")
    void refresh_expiredToken_returns401() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        JsonNode loginBody = login(email, PASSWORD);
        String rawRefreshToken = loginBody.get("refreshToken").asText();

        // Force the stored token's expiry into the past to simulate expiry.
        UUID userId = userIdByEmail(email);
        RefreshTokenJpaEntity token = refreshTokenRepository.findByUser_Id(userId).get(0);
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.SECONDS));
        refreshTokenRepository.save(token);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("refreshToken", rawRefreshToken))))
                .andExpect(status().isUnauthorized());
    }

    // ---- AC-AUTH-07: locked account login → 403

    @Test
    @org.junit.jupiter.api.DisplayName("AC-AUTH-07: locked account login → 403")
    void login_lockedAccount_returns403() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);

        // Lock the account directly (as an Admin would in Task 04).
        UserJpaEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setStatus("LOCKED");
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    // ---- AC-RBAC-01: candidate JWT → GET /api/v1/admin/topics → 403

    @Test
    @org.junit.jupiter.api.DisplayName("AC-RBAC-01: candidate JWT accessing admin route → 403")
    void adminRoute_withCandidateJwt_returns403() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        JsonNode loginBody = login(email, PASSWORD);
        String accessToken = loginBody.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    // ---- AC-RBAC-02: no JWT → GET /api/v1/topics → 401

    @Test
    @org.junit.jupiter.api.DisplayName("AC-RBAC-02: no JWT accessing protected route → 401")
    void protectedRoute_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/topics"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Supporting: GET /api/v1/me with a JWT → 200 user profile (no password)

    @Test
    @org.junit.jupiter.api.DisplayName("GET /api/v1/me with JWT → 200 profile without password")
    void me_withJwt_returnsProfile() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        JsonNode loginBody = login(email, PASSWORD);
        String accessToken = loginBody.get("accessToken").asText();

        MvcResult result = mockMvc.perform(get("/api/v1/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.contains(PASSWORD),
                "Password must never appear in /me response");
    }

    // ---- Supporting: logout revokes the refresh token (204)

    @Test
    @org.junit.jupiter.api.DisplayName("logout → 204 and refresh token revoked")
    void logout_revokesRefreshToken() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        JsonNode loginBody = login(email, PASSWORD);
        String refreshToken = loginBody.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        // The same refresh token must no longer be usable.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(java.util.Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }
}
