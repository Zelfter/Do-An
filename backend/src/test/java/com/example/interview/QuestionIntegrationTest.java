package com.example.interview;

import com.example.interview.infrastructure.persistence.QuestionJpaEntity;
import com.example.interview.infrastructure.persistence.QuestionJpaRepository;
import com.example.interview.infrastructure.persistence.QuestionKeyPointJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Task 06 — Admin question-bank management (FR-32).
 *
 * <p>Covers:
 * <ul>
 *   <li>AC-Q-01 (≡ AC-18): publish with missing fields → 422 + missingFields list.</li>
 *   <li>AC-Q-02 (≡ AC-19): DRAFT questions excluded from RAG pool.</li>
 *   <li>AC-Q-03: publish with all fields → 200, status=PUBLISHED, indexing triggered.</li>
 * </ul>
 *
 * <p>Follows the same pattern as {@link TopicIntegrationTest}: {@code @SpringBootTest},
 * {@code @AutoConfigureMockMvc}, {@code @ActiveProfiles("test")}, {@code @Transactional}.
 * Backed by an H2 in-memory DB — no business-logic mocks (the MockAiServiceClient
 * is a no-op stub for the AI Service, not a test double).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private TopicJpaRepository topicRepository;
    @Autowired
    private QuestionJpaRepository questionRepository;
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

    /** Promotes a registered candidate to ADMIN directly in the DB (FR-RBAC). */
    private String promoteToAdmin(String email) {
        UserJpaEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setRole("ADMIN");
        userRepository.save(user);
        return email;
    }

    /** Creates an admin user and returns an admin access token. */
    private String adminAccessToken() throws Exception {
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        return login(adminEmail, PASSWORD).get("accessToken").asText();
    }

    /** Creates a candidate and returns a candidate access token. */
    private String candidateAccessToken() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        return login(email, PASSWORD).get("accessToken").asText();
    }

    /** Creates a topic via the admin API and returns its UUID. */
    private UUID createTopic(String token, String code) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("translations", List.of(
                Map.of("language", "vi", "name", "Topic " + code, "description", "Mô tả test")));
        MvcResult result = mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    /** Builds a question-create request body with full required fields. */
    private Map<String, Object> fullQuestionBody(UUID topicId) {
        Map<String, Object> body = new HashMap<>();
        body.put("topicId", topicId.toString());
        body.put("level", "JUNIOR");
        body.put("content", "Giải thích tính đóng gói (encapsulation) trong OOP?");
        body.put("referenceAnswer", "Encapsulation là ẩn chi tiết triển khai, chỉ lộ interface công khai.");
        body.put("keyPoints", List.of(
                Map.of("seq", 1, "content", "Ẩn trường dữ liệu (private)"),
                Map.of("seq", 2, "content", "Truy cập qua getter/setter")));
        return body;
    }

    /** Creates a fully-valid question via POST and returns its UUID. */
    private UUID createFullQuestion(String token, UUID topicId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(fullQuestionBody(topicId))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    /** Persists a DRAFT question directly in the DB (for RAG pool tests). */
    private UUID createDraftQuestionInDb(UUID topicId) {
        TopicJpaEntity topic = topicRepository.findById(topicId).orElseThrow();
        QuestionJpaEntity q = new QuestionJpaEntity();
        q.setTopic(topic);
        q.setLevel("JUNIOR");
        q.setContent("Câu hỏi DRAFT trực tiếp DB");
        q.setReferenceAnswer("Đáp án DRAFT");
        q.setStatus("DRAFT");
        q.setIndexStatus("NOT_INDEXED");
        QuestionJpaEntity saved = questionRepository.save(q);
        entityManager.flush();
        return saved.getId();
    }

    // ====================================================================
    // AC-Q-01: publish with missing fields → 422 + missingFields list
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-01: publish question missing reference_answer & key_points → 422")
    void publishQuestion_missingAnswerAndKeyPoints_returns422() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "oop_topic");

        // Create a question without reference_answer and without key_points.
        Map<String, Object> body = new HashMap<>();
        body.put("topicId", topicId.toString());
        body.put("level", "JUNIOR");
        body.put("content", "Câu hỏi thiếu đáp án và key points");

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID questionId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        // Publish → 422 with missingFields containing reference_answer and key_points.
        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Thiếu trường bắt buộc"))
                .andExpect(jsonPath("$.missingFields").isArray())
                .andExpect(jsonPath("$.missingFields").value(
                        org.hamcrest.Matchers.containsInAnyOrder("reference_answer", "key_points")));

        // Verify the question remains DRAFT in DB.
        QuestionJpaEntity dbQuestion = questionRepository.findById(questionId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("DRAFT", dbQuestion.getStatus(),
                "Question must remain DRAFT after failed publish");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-01: publish question with reference_answer but no key_points → 422")
    void publishQuestion_hasAnswerNoKeyPoints_returns422() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "spring_topic");

        Map<String, Object> body = new HashMap<>();
        body.put("topicId", topicId.toString());
        body.put("level", "MID");
        body.put("content", "Câu hỏi có đáp án nhưng thiếu key points");
        body.put("referenceAnswer", "Đây là đáp án đầy đủ");

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID questionId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.missingFields").value(
                        org.hamcrest.Matchers.contains("key_points")));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-01: publish with candidate JWT → 403")
    void publishQuestion_withCandidateJwt_returns403() throws Exception {
        String adminToken = adminAccessToken();
        UUID topicId = createTopic(adminToken, "react_state");
        UUID questionId = createFullQuestion(adminToken, topicId);
        String candidateToken = candidateAccessToken();

        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-01: publish without JWT → 401")
    void publishQuestion_withoutJwt_returns401() throws Exception {
        String adminToken = adminAccessToken();
        UUID topicId = createTopic(adminToken, "docker_topic");
        UUID questionId = createFullQuestion(adminToken, topicId);

        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish"))
                .andExpect(status().isUnauthorized());
    }

    // ====================================================================
    // AC-Q-03: publish with all fields → 200, status=PUBLISHED, indexing triggered
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-03: publish valid question → 200, status=PUBLISHED, indexingTriggered=true")
    void publishQuestion_allFieldsPresent_returns200_andPublished() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "java_oop");
        UUID questionId = createFullQuestion(token, topicId);

        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.indexingTriggered").value(true));

        // Verify in DB: status=PUBLISHED, indexStatus=INDEXED (mock succeeded).
        QuestionJpaEntity dbQuestion = questionRepository.findById(questionId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("PUBLISHED", dbQuestion.getStatus(),
                "Question status must be PUBLISHED after successful publish");
        org.junit.jupiter.api.Assertions.assertEquals("INDEXED", dbQuestion.getIndexStatus(),
                "indexStatus must be INDEXED after mock indexing success");
    }

    // ====================================================================
    // AC-Q-02: DRAFT questions excluded from RAG pool; PUBLISHED+INDEXED included
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-02: DRAFT question NOT in RAG pool")
    void draftQuestion_notInRagPool() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "algorithms");
        UUID draftId = createDraftQuestionInDb(topicId);

        // The DRAFT question must not be in the RAG pool.
        QuestionJpaEntity draft = questionRepository.findById(draftId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("DRAFT", draft.getStatus());

        List<QuestionJpaEntity> pool = questionRepository
                .findByStatusAndIndexStatus("PUBLISHED", "INDEXED");
        boolean inPool = pool.stream().anyMatch(q -> q.getId().equals(draftId));
        org.junit.jupiter.api.Assertions.assertFalse(inPool,
                "DRAFT question must not appear in the RAG pool (AC-Q-02)");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-02: published+indexed question IS in RAG pool")
    void publishedQuestion_inRagPool() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "design_patterns_q");
        UUID questionId = createFullQuestion(token, topicId);

        // Before publish: not in pool.
        org.junit.jupiter.api.Assertions.assertFalse(
                questionRepository.existsByIdAndStatusAndIndexStatus(questionId, "PUBLISHED", "INDEXED"),
                "Question should not be in pool before publish");

        // Publish → PUBLISHED + INDEXED.
        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        // After publish: must be in pool.
        org.junit.jupiter.api.Assertions.assertTrue(
                questionRepository.existsByIdAndStatusAndIndexStatus(questionId, "PUBLISHED", "INDEXED"),
                "Published+indexed question must appear in the RAG pool (AC-Q-02)");

        List<QuestionJpaEntity> pool = questionRepository
                .findByStatusAndIndexStatus("PUBLISHED", "INDEXED");
        boolean inPool = pool.stream().anyMatch(q -> q.getId().equals(questionId));
        org.junit.jupiter.api.Assertions.assertTrue(inPool,
                "Published question must be returned by the RAG pool query");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-Q-02: archived question NOT in RAG pool")
    void archivedQuestion_notInRagPool() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "kafka_q");
        UUID questionId = createFullQuestion(token, topicId);

        // Publish then archive.
        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/archive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        org.junit.jupiter.api.Assertions.assertFalse(
                questionRepository.existsByIdAndStatusAndIndexStatus(questionId, "PUBLISHED", "INDEXED"),
                "Archived question must not appear in the RAG pool");
    }

    // ====================================================================
    // CRUD tests
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("POST /admin/questions → 201 with DRAFT status")
    void createQuestion_returns201_andDraftStatus() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "microservices_q");

        MvcResult result = mockMvc.perform(post("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(fullQuestionBody(topicId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.indexStatus").value("NOT_INDEXED"))
                .andExpect(jsonPath("$.keyPoints[0].seq").value(1))
                .andExpect(jsonPath("$.keyPoints[1].seq").value(2))
                .andReturn();

        UUID questionId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        org.junit.jupiter.api.Assertions.assertTrue(questionRepository.existsById(questionId),
                "Question must exist in DB after creation");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("POST /admin/questions with non-existent topic → 404")
    void createQuestion_topicNotFound_returns404() throws Exception {
        String token = adminAccessToken();
        Map<String, Object> body = new HashMap<>();
        body.put("topicId", UUID.randomUUID().toString());
        body.put("level", "JUNIOR");
        body.put("content", "Câu hỏi với topic không tồn tại");

        mockMvc.perform(post("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("POST /admin/questions with invalid level → 400")
    void createQuestion_invalidLevel_returns400() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "invalid_level_topic");

        Map<String, Object> body = new HashMap<>();
        body.put("topicId", topicId.toString());
        body.put("level", "SUPERSENIOR"); // invalid
        body.put("content", "Câu hỏi với level không hợp lệ");

        mockMvc.perform(post("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/questions → 200 list")
    void listQuestions_returns200() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "list_questions_topic");
        createFullQuestion(token, topicId);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode questions = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(questions.isArray(),
                "Response must be a JSON array");
        org.junit.jupiter.api.Assertions.assertTrue(questions.size() >= 1,
                "List should contain at least 1 question");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/questions/{id} → 200 with key points")
    void getQuestion_returns200_withKeyPoints() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "get_question_topic");
        UUID questionId = createFullQuestion(token, topicId);

        mockMvc.perform(get("/api/v1/admin/questions/" + questionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.referenceAnswer").isNotEmpty())
                .andExpect(jsonPath("$.keyPoints").isArray())
                .andExpect(jsonPath("$.keyPoints.length()").value(2));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/questions/{nonexistent} → 404")
    void getQuestion_notFound_returns404() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(get("/api/v1/admin/questions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("PUT /admin/questions/{id} → 200 (update)")
    void updateQuestion_returns200() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "update_question_topic");
        UUID questionId = createFullQuestion(token, topicId);

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("topicId", topicId.toString());
        updateBody.put("level", "SENIOR");
        updateBody.put("content", "Câu hỏi đã cập nhật");
        updateBody.put("referenceAnswer", "Đáp án đã cập nhật");
        updateBody.put("keyPoints", List.of(
                Map.of("seq", 1, "content", "Key point mới")));

        mockMvc.perform(put("/api/v1/admin/questions/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("SENIOR"))
                .andExpect(jsonPath("$.content").value("Câu hỏi đã cập nhật"))
                .andExpect(jsonPath("$.referenceAnswer").value("Đáp án đã cập nhật"))
                .andExpect(jsonPath("$.keyPoints.length()").value(1));

        // Verify in DB.
        QuestionJpaEntity dbQuestion = questionRepository.findById(questionId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("SENIOR", dbQuestion.getLevel());
        org.junit.jupiter.api.Assertions.assertEquals(1, dbQuestion.getKeyPoints().size());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("DELETE /admin/questions/{id} → 204")
    void deleteQuestion_returns204() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "delete_question_topic");
        UUID questionId = createFullQuestion(token, topicId);

        mockMvc.perform(delete("/api/v1/admin/questions/" + questionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(questionRepository.existsById(questionId),
                "Question should be deleted from DB");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("POST /admin/questions/{id}/archive → 200, status=ARCHIVED")
    void archiveQuestion_returns200_andArchived() throws Exception {
        String token = adminAccessToken();
        UUID topicId = createTopic(token, "archive_question_topic");
        UUID questionId = createFullQuestion(token, topicId);

        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/archive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(questionId.toString()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        QuestionJpaEntity dbQuestion = questionRepository.findById(questionId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ARCHIVED", dbQuestion.getStatus(),
                "Question must be ARCHIVED in DB");
    }

    // ====================================================================
    // RBAC tests
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("POST /admin/questions with candidate JWT → 403")
    void createQuestion_withCandidateJwt_returns403() throws Exception {
        String adminToken = adminAccessToken();
        UUID topicId = createTopic(adminToken, "rbac_topic");
        String candidateToken = candidateAccessToken();

        mockMvc.perform(post("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(fullQuestionBody(topicId))))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("POST /admin/questions without JWT → 401")
    void createQuestion_withoutJwt_returns401() throws Exception {
        String adminToken = adminAccessToken();
        UUID topicId = createTopic(adminToken, "noauth_topic");

        mockMvc.perform(post("/api/v1/admin/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(fullQuestionBody(topicId))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/questions with candidate JWT → 403")
    void listQuestions_withCandidateJwt_returns403() throws Exception {
        String candidateToken = candidateAccessToken();

        mockMvc.perform(get("/api/v1/admin/questions")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }
}
