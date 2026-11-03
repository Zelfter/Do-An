package com.example.interview;

import com.example.interview.infrastructure.persistence.RefreshTokenJpaEntity;
import com.example.interview.infrastructure.persistence.RefreshTokenJpaRepository;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Task 04 — Admin user management (FR-34).
 *
 * <p>Covers AC-USER-01 (lock → cannot login + tokens revoked) and AC-USER-02
 * (self-lock → 400), plus supporting RBAC and listing scenarios. Runs through
 * the full Spring MVC + Security chain backed by an H2 in-memory DB, with real
 * bcrypt hashing, JWT signing and token persistence — no business-logic mocks.
 *
 * <p>{@code @Transactional} rolls back after each test to keep the DB clean.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;
    @Autowired
    private EntityManager entityManager;

    private static final String PASSWORD = "Password123!";

    // ---- helpers -----------------------------------------------------------

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    /** Registers a candidate user and returns the parsed response body. */
    private JsonNode register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Logs in and returns the parsed response body (access + refresh tokens). */
    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /**
     * Promotes a registered candidate to ADMIN directly in the DB (there is no
     * self-registration endpoint for admins — FR-RBAC). Returns the admin's
     * email so the caller can log in to obtain an admin JWT.
     */
    private String promoteToAdmin(String email) {
        UserJpaEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setRole("ADMIN");
        userRepository.save(user);
        return email;
    }

    private UUID userIdByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow().getId();
    }

    // ---- AC-USER-01: lock a user → 403 on login + refresh tokens revoked -----

    @Test
    @org.junit.jupiter.api.DisplayName("AC-USER-01: Admin locks a user → login 403, refresh token revoked")
    void adminLocksUser_userCannotLoginAndTokensRevoked() throws Exception {
        // 1. Register + login a candidate to obtain a refresh token.
        String candidateEmail = uniqueEmail();
        register(candidateEmail, PASSWORD);
        JsonNode candidateLogin = login(candidateEmail, PASSWORD);
        String candidateRefreshToken = candidateLogin.get("refreshToken").asText();
        UUID candidateId = userIdByEmail(candidateEmail);

        // 2. Create an admin user and log in as admin.
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        // 3. Admin locks the candidate.
        MvcResult lockResult = mockMvc.perform(patch("/api/v1/admin/users/" + candidateId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "LOCKED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andReturn();

        // Response must never contain password fields (NFR-17).
        String lockBody = lockResult.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(lockBody.contains(PASSWORD),
                "Password must not appear in any response");
        org.junit.jupiter.api.Assertions.assertFalse(lockBody.toLowerCase().contains("password"),
                "No password field may appear in the response body");

        // 4. The candidate's existing refresh token must now be revoked → 401.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", candidateRefreshToken))))
                .andExpect(status().isUnauthorized());

        // 5. The candidate can no longer log in → 403.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", candidateEmail, "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    // ---- AC-USER-01 (multi-token): multiple refresh tokens → all revoked on lock -----

    @Test
    @org.junit.jupiter.api.DisplayName("AC-USER-01: Lock user with multiple refresh tokens → all revoked, all refresh 401")
    void adminLocksUser_withMultipleRefreshTokens_allRevoked() throws Exception {
        // 1. Register a candidate.
        String candidateEmail = uniqueEmail();
        register(candidateEmail, PASSWORD);
        UUID candidateId = userIdByEmail(candidateEmail);

        // 2. Login 3 times → 3 distinct refresh tokens persisted in the DB.
        JsonNode login1 = login(candidateEmail, PASSWORD);
        JsonNode login2 = login(candidateEmail, PASSWORD);
        JsonNode login3 = login(candidateEmail, PASSWORD);
        String refreshToken1 = login1.get("refreshToken").asText();
        String refreshToken2 = login2.get("refreshToken").asText();
        String refreshToken3 = login3.get("refreshToken").asText();

        // 3. Verify the DB holds exactly 3 non-revoked refresh tokens for this user.
        var tokens = refreshTokenRepository.findByUser_Id(candidateId);
        org.junit.jupiter.api.Assertions.assertEquals(3, tokens.size(),
                "Candidate should have exactly 3 refresh tokens after 3 logins");
        org.junit.jupiter.api.Assertions.assertTrue(
                tokens.stream().noneMatch(RefreshTokenJpaEntity::isRevoked),
                "All 3 refresh tokens should be non-revoked before lock");

        // 4. Create an admin and log in as admin.
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        // 5. Admin locks the candidate.
        mockMvc.perform(patch("/api/v1/admin/users/" + candidateId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "LOCKED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));

        // 6. Verify ALL refresh tokens are now revoked in the DB (FR-34 / AC-USER-01).
        //    The bulk UPDATE (revokeAllByUserId) bypasses the persistence context, so
        //    flush pending changes and clear the 1st-level cache before re-querying to
        //    read the post-lock state fresh from the DB.
        entityManager.flush();
        entityManager.clear();
        var revokedTokens = refreshTokenRepository.findByUser_Id(candidateId);
        org.junit.jupiter.api.Assertions.assertEquals(3, revokedTokens.size(),
                "There should still be 3 refresh tokens for the user");
        org.junit.jupiter.api.Assertions.assertTrue(
                revokedTokens.stream().allMatch(RefreshTokenJpaEntity::isRevoked),
                "Every refresh token for the locked user must be revoked");

        // 7. Using any of the 3 refresh tokens → 401 (all revoked).
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken1))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken2))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken3))))
                .andExpect(status().isUnauthorized());
    }

    // ---- AC-USER-02: self-lock prevention → 400 -----------------------------

    @Test
    @org.junit.jupiter.api.DisplayName("AC-USER-02: Admin locking own account → 400")
    void adminLocksOwnAccount_returns400() throws Exception {
        // Create an admin and log in.
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();
        UUID adminId = userIdByEmail(adminEmail);

        // Admin tries to lock their own account.
        MvcResult result = mockMvc.perform(patch("/api/v1/admin/users/" + adminId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "LOCKED"))))
                .andExpect(status().isBadRequest())
                .andReturn();

        String message = objectMapper.readTree(result.getResponse().getContentAsString()).get("message").asText();
        org.junit.jupiter.api.Assertions.assertFalse(message.isBlank(),
                "400 message must be clear and non-empty");

        // The admin's status must remain ACTIVE (lock was rejected).
        UserJpaEntity admin = userRepository.findByEmailIgnoreCase(adminEmail).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ACTIVE", admin.getStatus(),
                "Admin's own account must not be locked after self-lock rejection");
    }

    // ---- Unlock flow: lock then unlock → user can login again -------------

    @Test
    @org.junit.jupiter.api.DisplayName("Unlock: lock then unlock a user → login succeeds (200)")
    void lockThenUnlock_userCanLoginAgain() throws Exception {
        String candidateEmail = uniqueEmail();
        register(candidateEmail, PASSWORD);
        UUID candidateId = userIdByEmail(candidateEmail);

        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        // Lock the candidate.
        mockMvc.perform(patch("/api/v1/admin/users/" + candidateId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "LOCKED"))))
                .andExpect(status().isOk());

        // Verify locked → 403 on login.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", candidateEmail, "password", PASSWORD))))
                .andExpect(status().isForbidden());

        // Unlock the candidate.
        mockMvc.perform(patch("/api/v1/admin/users/" + candidateId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Verify unlocked → login succeeds (200).
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", candidateEmail, "password", PASSWORD))))
                .andExpect(status().isOk());
    }

    // ---- GET /api/v1/admin/users with admin JWT → 200 list ----------------

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/users with admin JWT → 200 list, no password fields")
    void listUsers_withAdminJwt_returns200() throws Exception {
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        // Register an extra candidate so the list isn't just the admin.
        register(uniqueEmail(), PASSWORD);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode users = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(users.isArray(), "Response must be a JSON array");
        org.junit.jupiter.api.Assertions.assertTrue(users.size() >= 2, "List should contain at least 2 users");

        // No element should expose password fields (NFR-17).
        String body = result.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(body.toLowerCase().contains("password"),
                "No password field may appear in the user list");
        org.junit.jupiter.api.Assertions.assertFalse(body.contains(PASSWORD),
                "Raw password must not appear in the user list");
    }

    // ---- GET /api/v1/admin/users/{id} with admin JWT → 200 ----------------

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/users/{id} with admin JWT → 200 user detail")
    void getUser_withAdminJwt_returns200() throws Exception {
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        String candidateEmail = uniqueEmail();
        register(candidateEmail, PASSWORD);
        UUID candidateId = userIdByEmail(candidateEmail);

        mockMvc.perform(get("/api/v1/admin/users/" + candidateId)
                        .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidateId.toString()))
                .andExpect(jsonPath("$.email").value(candidateEmail))
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // ---- GET /admin/users without JWT → 401 --------------------------------

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/users without JWT → 401")
    void listUsers_withoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET /admin/users with candidate JWT → 403 -------------------------

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/users with candidate JWT → 403")
    void listUsers_withCandidateJwt_returns403() throws Exception {
        String candidateEmail = uniqueEmail();
        register(candidateEmail, PASSWORD);
        JsonNode candidateLogin = login(candidateEmail, PASSWORD);
        String candidateAccessToken = candidateLogin.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + candidateAccessToken))
                .andExpect(status().isForbidden());
    }

    // ---- PATCH with invalid status value → 400 ----------------------------

    @Test
    @org.junit.jupiter.api.DisplayName("PATCH /admin/users/{id}/status with invalid status → 400")
    void updateStatus_invalidValue_returns400() throws Exception {
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        String candidateEmail = uniqueEmail();
        register(candidateEmail, PASSWORD);
        UUID candidateId = userIdByEmail(candidateEmail);

        mockMvc.perform(patch("/api/v1/admin/users/" + candidateId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PENDING"))))
                .andExpect(status().isBadRequest());
    }

    // ---- PATCH non-existent user → 404 -------------------------------------

    @Test
    @org.junit.jupiter.api.DisplayName("PATCH /admin/users/{nonexistent}/status → 404")
    void updateStatus_nonexistentUser_returns404() throws Exception {
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        mockMvc.perform(patch("/api/v1/admin/users/" + UUID.randomUUID() + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "LOCKED"))))
                .andExpect(status().isNotFound());
    }

    // ---- PATCH with blank status → 400 (Bean Validation) -------------------

    @Test
    @org.junit.jupiter.api.DisplayName("PATCH /admin/users/{id}/status with blank status → 400")
    void updateStatus_blankStatus_returns400() throws Exception {
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        JsonNode adminLogin = login(adminEmail, PASSWORD);
        String adminAccessToken = adminLogin.get("accessToken").asText();

        String candidateEmail = uniqueEmail();
        register(candidateEmail, PASSWORD);
        UUID candidateId = userIdByEmail(candidateEmail);

        mockMvc.perform(patch("/api/v1/admin/users/" + candidateId + "/status")
                        .header("Authorization", "Bearer " + adminAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", ""))))
                .andExpect(status().isBadRequest());
    }
}
