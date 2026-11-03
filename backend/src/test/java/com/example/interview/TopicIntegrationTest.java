package com.example.interview;

import com.example.interview.infrastructure.persistence.QuestionJpaEntity;
import com.example.interview.infrastructure.persistence.QuestionJpaRepository;
import com.example.interview.infrastructure.persistence.StudyDocumentJpaEntity;
import com.example.interview.infrastructure.persistence.StudyDocumentJpaRepository;
import com.example.interview.infrastructure.persistence.TopicAliasJpaEntity;
import com.example.interview.infrastructure.persistence.TopicAliasJpaRepository;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaRepository;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Task 05 — Admin topic management (FR-37).
 *
 * <p>Covers AC-TOPIC-01 (create → 201), AC-TOPIC-02 (delete blocked → 409),
 * and AC-TOPIC-03 (deactivate → 200), plus supporting CRUD, alias, and RBAC
 * scenarios. Runs through the full Spring MVC + Security chain backed by an
 * H2 in-memory DB — no business-logic mocks.
 *
 * <p>Follows the same pattern as {@link AdminUserIntegrationTest}:
 * {@code @SpringBootTest}, {@code @AutoConfigureMockMvc},
 * {@code @ActiveProfiles("test")}, {@code @Transactional}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TopicIntegrationTest {

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
    private StudyDocumentJpaRepository documentRepository;
    @Autowired
    private TopicAliasJpaRepository aliasRepository;

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

    /** Builds a topic-create request body. */
    private Map<String, Object> topicBody(String code, String viName, String... aliases) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("translations", List.of(
                Map.of("language", "vi", "name", viName, "description", "Mô tả test")));
        if (aliases.length > 0) {
            body.put("aliases", Arrays.asList(aliases));
        }
        return body;
    }

    /** Creates a topic via POST and returns the parsed response body. */
    private JsonNode createTopic(String token, String code, String viName, String... aliases) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(topicBody(code, viName, aliases))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Persists a question linked to a topic directly via the repository. */
    private void createQuestionForTopic(TopicJpaEntity topic) {
        QuestionJpaEntity question = new QuestionJpaEntity();
        question.setTopic(topic);
        question.setLevel("JUNIOR");
        question.setContent("Câu hỏi test cho topic");
        questionRepository.save(question);
    }

    /** Persists a study document linked to a topic directly via the repository. */
    private void createDocumentForTopic(TopicJpaEntity topic) {
        StudyDocumentJpaEntity doc = new StudyDocumentJpaEntity();
        doc.setTopic(topic);
        doc.setTitle("Tài liệu test");
        doc.setContentMd("# Nội dung test");
        documentRepository.save(doc);
    }

    // ====================================================================
    // AC-TOPIC-01: POST /admin/topics → 201 + topic exists in DB
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-01: Admin creates topic → 201, topic exists in DB")
    void adminCreatesTopic_returns201_andTopicExistsInDb() throws Exception {
        String token = adminAccessToken();

        MvcResult result = mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(topicBody("spring_data_jpa", "Spring Data JPA", "JPA", "Hibernate"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").value("spring_data_jpa"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.parentId").isEmpty())
                .andExpect(jsonPath("$.translations[0].language").value("vi"))
                .andExpect(jsonPath("$.translations[0].name").value("Spring Data JPA"))
                .andExpect(jsonPath("$.aliases[0].alias").value("JPA"))
                .andExpect(jsonPath("$.aliases[1].alias").value("Hibernate"))
                .andReturn();

        // Verify topic exists in DB.
        UUID topicId = UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        org.junit.jupiter.api.Assertions.assertTrue(topicRepository.existsById(topicId),
                "Topic must exist in DB after creation");
        TopicJpaEntity dbTopic = topicRepository.findById(topicId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("spring_data_jpa", dbTopic.getCode());
        org.junit.jupiter.api.Assertions.assertTrue(dbTopic.isActive());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-01: POST with duplicate code → 409")
    void createTopic_duplicateCode_returns409() throws Exception {
        String token = adminAccessToken();
        createTopic(token, "design_patterns", "Design Patterns");

        mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(topicBody("design_patterns", "Duplicate"))))
                .andExpect(status().isConflict());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-01: POST without JWT → 401")
    void createTopic_withoutJwt_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(topicBody("microservices", "Microservices"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-01: POST with candidate JWT → 403")
    void createTopic_withCandidateJwt_returns403() throws Exception {
        String candidateToken = candidateAccessToken();

        mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(topicBody("microservices", "Microservices"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("POST with invalid snake_case code → 400")
    void createTopic_invalidCodeFormat_returns400() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(topicBody("InvalidCode", "Invalid"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("POST without 'vi' translation → 400")
    void createTopic_noViTranslation_returns400() throws Exception {
        String token = adminAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("code", "no_vi_topic");
        body.put("translations", List.of(
                Map.of("language", "en", "name", "English only", "description", "No Vietnamese")));

        mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest());
    }

    // ====================================================================
    // AC-TOPIC-02: DELETE topic with linked questions/docs → 409
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-02: DELETE topic with linked question → 409")
    void deleteTopic_withLinkedQuestion_returns409() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "react_basics", "React Basics");
        UUID topicId = UUID.fromString(created.get("id").asText());

        TopicJpaEntity topic = topicRepository.findById(topicId).orElseThrow();
        createQuestionForTopic(topic);

        mockMvc.perform(delete("/api/v1/admin/topics/" + topicId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-02: DELETE topic with linked document → 409")
    void deleteTopic_withLinkedDocument_returns409() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "devops_ci", "DevOps & CI/CD");
        UUID topicId = UUID.fromString(created.get("id").asText());

        TopicJpaEntity topic = topicRepository.findById(topicId).orElseThrow();
        createDocumentForTopic(topic);

        mockMvc.perform(delete("/api/v1/admin/topics/" + topicId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-02: DELETE topic with no linked questions/docs → 204")
    void deleteTopic_noLinks_returns204() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "clean_code", "Clean Code");
        UUID topicId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(delete("/api/v1/admin/topics/" + topicId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(topicRepository.existsById(topicId),
                "Topic should be deleted from DB");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("DELETE non-existent topic → 404")
    void deleteTopic_notFound_returns404() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(delete("/api/v1/admin/topics/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ====================================================================
    // AC-TOPIC-03: PATCH isActive=false → 200, topic inactive in DB
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-03: PATCH isActive=false → 200, topic inactive in DB")
    void patchTopic_deactivate_returns200_andInactiveInDb() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "kafka", "Apache Kafka");
        UUID topicId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(patch("/api/v1/admin/topics/" + topicId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("isActive", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topicId.toString()))
                .andExpect(jsonPath("$.isActive").value(false));

        // Verify in DB.
        TopicJpaEntity dbTopic = topicRepository.findById(topicId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(dbTopic.isActive(),
                "Topic must be inactive in DB after PATCH");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-03: PATCH with candidate JWT → 403")
    void patchTopic_withCandidateJwt_returns403() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode created = createTopic(adminToken, "rabbitmq", "RabbitMQ");
        UUID topicId = UUID.fromString(created.get("id").asText());
        String candidateToken = candidateAccessToken();

        mockMvc.perform(patch("/api/v1/admin/topics/" + topicId)
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("isActive", false))))
                .andExpect(status().isForbidden());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("AC-TOPIC-03: PATCH without JWT → 401")
    void patchTopic_withoutJwt_returns401() throws Exception {
        String adminToken = adminAccessToken();
        JsonNode created = createTopic(adminToken, "redis_cache", "Redis Cache");
        UUID topicId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(patch("/api/v1/admin/topics/" + topicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("isActive", false))))
                .andExpect(status().isUnauthorized());
    }

    // ====================================================================
    // Additional CRUD tests
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/topics with admin JWT → 200 list")
    void listTopics_withAdminJwt_returns200() throws Exception {
        String token = adminAccessToken();
        createTopic(token, "graphql_api", "GraphQL API");

        MvcResult result = mockMvc.perform(get("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode topics = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(topics.isArray(), "Response must be a JSON array");
        org.junit.jupiter.api.Assertions.assertTrue(topics.size() >= 1, "List should contain at least 1 topic");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/topics/{id} with admin JWT → 200")
    void getTopic_withAdminJwt_returns200() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "docker_k8s", "Docker & Kubernetes", "Container");
        UUID topicId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(get("/api/v1/admin/topics/" + topicId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(topicId.toString()))
                .andExpect(jsonPath("$.code").value("docker_k8s"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.translations[0].language").value("vi"))
                .andExpect(jsonPath("$.aliases[0].alias").value("Container"));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/topics/{nonexistent} → 404")
    void getTopic_notFound_returns404() throws Exception {
        String token = adminAccessToken();

        mockMvc.perform(get("/api/v1/admin/topics/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("PUT /admin/topics/{id} → 200 (full update, code immutable)")
    void updateTopic_returns200_andCodeImmutable() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "testing_junit", "Testing & JUnit", "JUnit");
        UUID topicId = UUID.fromString(created.get("id").asText());

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("translations", List.of(
                Map.of("language", "vi", "name", "Kiểm thử & JUnit (đã cập nhật)", "description", "Mô tả mới")));
        updateBody.put("aliases", Arrays.asList("JUnit 5", "Mockito"));
        // Include "code" in the body to verify immutability (Jackson ignores unknown fields,
        // and the JPA entity has updatable=false on code).
        updateBody.put("code", "should_not_change");

        mockMvc.perform(put("/api/v1/admin/topics/" + topicId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("testing_junit"))
                .andExpect(jsonPath("$.translations[0].name").value("Kiểm thử & JUnit (đã cập nhật)"))
                .andExpect(jsonPath("$.aliases[0].alias").value("JUnit 5"))
                .andExpect(jsonPath("$.aliases[1].alias").value("Mockito"));

        // Verify code unchanged in DB.
        TopicJpaEntity dbTopic = topicRepository.findById(topicId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("testing_junit", dbTopic.getCode(),
                "Code must remain immutable after PUT");
    }

    // ====================================================================
    // Alias tests
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("POST /admin/topics/{id}/aliases → 201")
    void addAlias_returns201() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "microservices_arch", "Microservices Architecture");
        UUID topicId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(post("/api/v1/admin/topics/" + topicId + "/aliases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alias", "Microservice"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.alias").value("Microservice"));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("POST duplicate alias → 409")
    void addAlias_duplicate_returns409() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "event_driven", "Event Driven", "EDA");
        UUID topicId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(post("/api/v1/admin/topics/" + topicId + "/aliases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("alias", "EDA"))))
                .andExpect(status().isConflict());
    }

    @Test
    @org.junit.jupiter.api.DisplayName("GET /admin/topics/{id}/aliases → 200 list")
    void listAliases_returns200() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "rest_api", "REST API", "REST", "RESTful");
        UUID topicId = UUID.fromString(created.get("id").asText());

        mockMvc.perform(get("/api/v1/admin/topics/" + topicId + "/aliases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alias").value("REST"))
                .andExpect(jsonPath("$[1].alias").value("RESTful"));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("DELETE /admin/topics/{id}/aliases/{aliasId} → 204")
    void deleteAlias_returns204() throws Exception {
        String token = adminAccessToken();
        JsonNode created = createTopic(token, "graphql_query", "GraphQL Query", "GQL");
        UUID topicId = UUID.fromString(created.get("id").asText());

        // Find the alias ID.
        MvcResult aliasResult = mockMvc.perform(get("/api/v1/admin/topics/" + topicId + "/aliases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode aliases = objectMapper.readTree(aliasResult.getResponse().getContentAsString());
        UUID aliasId = UUID.fromString(aliases.get(0).get("id").asText());

        mockMvc.perform(delete("/api/v1/admin/topics/" + topicId + "/aliases/" + aliasId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify alias removed.
        org.junit.jupiter.api.Assertions.assertFalse(aliasRepository.existsById(aliasId),
                "Alias should be deleted from DB");
    }

    // ====================================================================
    // Parent hierarchy test
    // ====================================================================

    @Test
    @org.junit.jupiter.api.DisplayName("POST topic with parent → 201, parentId set")
    void createTopic_withParent_returns201() throws Exception {
        String token = adminAccessToken();
        JsonNode parent = createTopic(token, "java_ecosystem", "Java Ecosystem");
        UUID parentId = UUID.fromString(parent.get("id").asText());

        Map<String, Object> childBody = topicBody("java_spring", "Spring Framework");
        childBody.put("parentId", parentId.toString());

        mockMvc.perform(post("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(childBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentId").value(parentId.toString()))
                .andExpect(jsonPath("$.code").value("java_spring"));
    }
}
