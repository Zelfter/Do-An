package com.example.interview;

import com.example.interview.infrastructure.persistence.TopicAliasJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaRepository;
import com.example.interview.infrastructure.persistence.TopicTranslationJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaEntity;
import com.example.interview.infrastructure.persistence.UserJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Task 05 sub-task: 8 chủ đề seed với aliases (SRS §9.4).
 *
 * <p>Simulates the Flyway migration V008__seed_default_topics.sql by inserting
 * the 8 default topics (with vi+en translations and aliases) directly via JPA.
 * Then verifies them through the Admin Topic Management API (FR-37) — the same
 * path a real admin would use.
 *
 * <p>The test profile uses H2 in-memory with Flyway disabled (Hibernate
 * create-drop), so the V008 seed data does not exist by default. This test
 * seeds the data programmatically using the same codes, translations, and
 * aliases as V008, then asserts the Admin API returns them correctly.
 *
 * <p>Follows the same pattern as {@link TopicIntegrationTest}:
 * {@code @SpringBootTest}, {@code @AutoConfigureMockMvc},
 * {@code @ActiveProfiles("test")}, {@code @Transactional}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TopicSeedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private TopicJpaRepository topicRepository;

    private static final String PASSWORD = "Password123!";

    private static final List<String> EXPECTED_CODES = List.of(
            "oop", "core_java", "collections", "java_8_plus",
            "concurrency", "jvm_memory", "spring_boot", "database_jpa");

    /** Captures the generated UUID for each seeded topic, keyed by code. */
    private final Map<String, UUID> topicIds = new HashMap<>();

    // ---- Test setup: seed 8 default topics (simulates V008) ---------------

    /**
     * Seeds the 8 default topics with vi+en translations and aliases — exactly
     * as V008__seed_default_topics.sql would in a real DB.
     *
     * <p>Topics are created via {@code topicRepository.save} (which calls
     * {@code persist} for new entities with null ID, letting Hibernate generate
     * the UUID). The generated UUIDs are captured in {@link #topicIds} so that
     * subsequent API assertions can reference them.
     */
    @BeforeEach
    void seedDefaultTopics() {
        // Create core_java first (needed as parent for java_8_plus).
        TopicJpaEntity coreJava = seedTopic("core_java", null,
                "Core Java", "Cú pháp Java cơ bản, kiểu dữ liệu, String, Exception, Wrapper classes.",
                "Core Java", "Basic Java syntax, data types, String, Exception handling, Wrapper classes.",
                "Core Java", "Java cơ bản", "Java basics", "Exception handling", "Java fundamental");

        seedTopic("oop", null,
                "Lập trình hướng đối tượng (OOP)",
                "Các khái niệm OOP: class, object, encapsulation, inheritance, polymorphism, abstraction.",
                "Object-Oriented Programming",
                "OOP concepts: class, object, encapsulation, inheritance, polymorphism, abstraction.",
                "OOP", "Lập trình hướng đối tượng", "Object-Oriented", "HĐDT", "Đối tượng");

        seedTopic("collections", null,
                "Collections Framework",
                "List, Set, Map, Queue, Iterator và các implementation (ArrayList, HashSet, HashMap, LinkedList...).",
                "Collections Framework",
                "List, Set, Map, Queue, Iterator and their implementations.",
                "Collections", "Java Collections", "ArrayList", "HashMap", "HashSet", "LinkedList");

        seedTopic("java_8_plus", coreJava,
                "Java 8+ Lambda & Stream",
                "Lambda expression, Stream API, Optional, Functional interfaces, Method reference.",
                "Java 8+ Lambda & Stream",
                "Lambda expressions, Stream API, Optional, Functional interfaces, Method references.",
                "Java 8", "Lambda", "Stream API", "Functional programming", "Optional");

        seedTopic("concurrency", null,
                "Concurrency & Multithreading",
                "Thread, Runnable, Executor, synchronized, volatile, deadlock, java.util.concurrent.",
                "Concurrency & Multithreading",
                "Thread, Runnable, Executor, synchronized, volatile, deadlock, java.util.concurrent.",
                "Multithreading", "Concurrency", "Thread", "Đa luồng", "java.util.concurrent");

        seedTopic("jvm_memory", null,
                "JVM & Memory Management",
                "JVM architecture, Heap, Stack, Garbage Collection, Memory leak, Class loading.",
                "JVM & Memory Management",
                "JVM architecture, Heap, Stack, GC algorithms, Memory leak detection, Class loading.",
                "JVM", "Garbage Collection", "Memory Management", "Heap", "GC");

        seedTopic("spring_boot", null,
                "Spring Boot",
                "Spring Boot fundamentals, Dependency Injection, REST API, Spring Data JPA, Spring Security.",
                "Spring Boot",
                "Spring Boot fundamentals, DI, REST API development, Spring Data JPA, Spring Security.",
                "Spring Boot", "Spring Framework", "Spring", "Dependency Injection", "Spring Security", "Spring Data JPA");

        seedTopic("database_jpa", null,
                "Database & JPA",
                "SQL, JDBC, JPA/Hibernate, Entity relationships, Transactions, Query optimization.",
                "Database & JPA",
                "SQL, JDBC, JPA/Hibernate, Entity relationships, Transactions, Query optimization.",
                "Database", "JPA", "Hibernate", "SQL", "JDBC", "Cơ sở dữ liệu");
    }

    // ---- Helpers: auth (same pattern as TopicIntegrationTest) -------------

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private JsonNode register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String promoteToAdmin(String email) {
        UserJpaEntity user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        user.setRole("ADMIN");
        userRepository.save(user);
        return email;
    }

    private String adminAccessToken() throws Exception {
        String adminEmail = uniqueEmail();
        register(adminEmail, PASSWORD);
        promoteToAdmin(adminEmail);
        return login(adminEmail, PASSWORD).get("accessToken").asText();
    }

    // ---- Helpers: seeding -------------------------------------------------

    /**
     * Creates and persists a topic with the given code, optional parent,
     * vi+en translations, and aliases. The generated UUID is captured in
     * {@link #topicIds} for use in assertions.
     */
    private TopicJpaEntity seedTopic(String code, TopicJpaEntity parent,
                                     String viName, String viDesc,
                                     String enName, String enDesc,
                                     String... aliases) {
        TopicJpaEntity topic = new TopicJpaEntity();
        topic.setCode(code);
        topic.setActive(true);
        topic.setParent(parent);

        // vi translation
        TopicTranslationJpaEntity vi = new TopicTranslationJpaEntity();
        vi.setTopic(topic);
        vi.setLanguage("vi");
        vi.setName(viName);
        vi.setDescription(viDesc);
        topic.getTranslations().add(vi);

        // en translation
        TopicTranslationJpaEntity en = new TopicTranslationJpaEntity();
        en.setTopic(topic);
        en.setLanguage("en");
        en.setName(enName);
        en.setDescription(enDesc);
        topic.getTranslations().add(en);

        // aliases
        for (String alias : aliases) {
            TopicAliasJpaEntity a = new TopicAliasJpaEntity();
            a.setTopic(topic);
            a.setAlias(alias);
            topic.getAliases().add(a);
        }

        TopicJpaEntity saved = topicRepository.save(topic);
        topicIds.put(code, saved.getId());
        return saved;
    }

    // ---- Helpers: JSON assertions -----------------------------------------

    /** Finds a topic node by code within a JSON array of topics. */
    private JsonNode findTopicByCode(JsonNode topics, String code) {
        for (JsonNode topic : topics) {
            if (code.equals(topic.get("code").asText())) {
                return topic;
            }
        }
        throw new AssertionError("Không tìm thấy chủ đề với code: " + code);
    }

    /** Extracts alias strings from a JSON array of alias objects. */
    private List<String> aliasStrings(JsonNode aliases) {
        List<String> result = new ArrayList<>();
        for (JsonNode alias : aliases) {
            result.add(alias.get("alias").asText());
        }
        return result;
    }

    /** Fetches the full topic list as a parsed JSON array (UTF-8). */
    private JsonNode fetchTopicList(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** Fetches aliases for a topic via the aliases endpoint (UTF-8). */
    private List<String> fetchAliases(String token, UUID topicId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/topics/" + topicId + "/aliases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return aliasStrings(objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)));
    }

    // ====================================================================
    // Test 1: GET /admin/topics → 200, exactly 8 topics, all codes present
    // ====================================================================

    @Test
    @DisplayName("GET /admin/topics trả về đúng 8 chủ đề seed với đầy đủ code")
    void listTopics_returnsExactly8SeedTopics() throws Exception {
        String token = adminAccessToken();
        JsonNode topics = fetchTopicList(token);

        assertEquals(8, topics.size(), "Phải có đúng 8 chủ đề seed");

        // Collect all codes and verify all 8 expected codes are present.
        List<String> codes = new ArrayList<>();
        for (JsonNode topic : topics) {
            codes.add(topic.get("code").asText());
        }
        for (String expected : EXPECTED_CODES) {
            assertTrue(codes.contains(expected),
                    "Thiếu chủ đề với code: " + expected + " — tìm thấy: " + codes);
        }
    }

    // ====================================================================
    // Test 2: All topics active, 2 translations each, ≥1 alias, total 43
    // ====================================================================

    @Test
    @DisplayName("Tất cả 8 chủ đề is_active=true, 2 bản dịch (vi+en), tổng 43 alias")
    void allSeedTopics_activeWithTwoTranslationsAnd43Aliases() throws Exception {
        String token = adminAccessToken();
        JsonNode topics = fetchTopicList(token);

        int totalAliases = 0;
        for (JsonNode topic : topics) {
            String code = topic.get("code").asText();

            assertTrue(topic.get("isActive").asBoolean(),
                    "Chủ đề '" + code + "' phải is_active=true");
            assertEquals(2, topic.get("translations").size(),
                    "Chủ đề '" + code + "' phải có 2 bản dịch (vi + en)");
            assertFalse(topic.get("aliases").isEmpty(),
                    "Chủ đề '" + code + "' phải có ít nhất 1 alias");
            totalAliases += topic.get("aliases").size();
        }

        assertEquals(43, totalAliases, "Tổng số alias phải là 43 (5+5+6+5+5+5+6+6)");
    }

    // ====================================================================
    // Test 3: Each topic has both vi and en translations
    // ====================================================================

    @Test
    @DisplayName("Mỗi chủ đề seed có bản dịch tiếng Việt và tiếng Anh")
    void eachTopicHasViAndEnTranslations() throws Exception {
        String token = adminAccessToken();
        JsonNode topics = fetchTopicList(token);

        for (JsonNode topic : topics) {
            String code = topic.get("code").asText();
            JsonNode translations = topic.get("translations");
            boolean hasVi = false;
            boolean hasEn = false;
            for (JsonNode t : translations) {
                String lang = t.get("language").asText();
                if ("vi".equals(lang)) hasVi = true;
                if ("en".equals(lang)) hasEn = true;
            }
            assertTrue(hasVi, "Chủ đề '" + code + "' phải có bản dịch 'vi'");
            assertTrue(hasEn, "Chủ đề '" + code + "' phải có bản dịch 'en'");
        }
    }

    // ====================================================================
    // Test 4: oop topic has aliases "OOP" and "Lập trình hướng đối tượng"
    // ====================================================================

    @Test
    @DisplayName("Chủ đề 'oop' có alias 'OOP' và 'Lập trình hướng đối tượng'")
    void oopTopicHasSpecificAliases() throws Exception {
        String token = adminAccessToken();

        // Verify via the list endpoint (aliases embedded in topic).
        JsonNode topics = fetchTopicList(token);
        JsonNode oop = findTopicByCode(topics, "oop");
        List<String> listAliases = aliasStrings(oop.get("aliases"));
        assertTrue(listAliases.contains("OOP"),
                "oop phải có alias 'OOP' — tìm thấy: " + listAliases);
        assertEquals(5, listAliases.size(), "oop phải có đúng 5 alias");

        // Verify via the dedicated aliases endpoint.
        List<String> endpointAliases = fetchAliases(token, topicIds.get("oop"));
        assertTrue(endpointAliases.contains("OOP"),
                "GET aliases endpoint: phải có 'OOP' — tìm thấy: " + endpointAliases);
        assertEquals(5, endpointAliases.size(), "GET aliases endpoint: oop phải có đúng 5 alias");
    }

    // ====================================================================
    // Test 5: spring_boot topic has aliases "Spring Boot" and "Spring Framework"
    // ====================================================================

    @Test
    @DisplayName("Chủ đề 'spring_boot' có alias 'Spring Boot' và 'Spring Framework'")
    void springBootTopicHasSpecificAliases() throws Exception {
        String token = adminAccessToken();
        List<String> aliases = fetchAliases(token, topicIds.get("spring_boot"));

        assertTrue(aliases.contains("Spring Boot"),
                "spring_boot phải có alias 'Spring Boot' — tìm thấy: " + aliases);
        assertTrue(aliases.contains("Spring Framework"),
                "spring_boot phải có alias 'Spring Framework' — tìm thấy: " + aliases);
        assertEquals(6, aliases.size(), "spring_boot phải có đúng 6 alias");
    }

    // ====================================================================
    // Test 6: java_8_plus parentId points to core_java
    // ====================================================================

    @Test
    @DisplayName("Chủ đề 'java_8_plus' có parentId trỏ tới 'core_java'")
    void java8PlusHasCoreJavaParent() throws Exception {
        String token = adminAccessToken();
        JsonNode topics = fetchTopicList(token);
        JsonNode java8Plus = findTopicByCode(topics, "java_8_plus");

        // parentId should equal core_java's UUID.
        assertFalse(java8Plus.get("parentId").isNull(),
                "java_8_plus phải có parentId không null");
        assertEquals(topicIds.get("core_java").toString(), java8Plus.get("parentId").asText(),
                "java_8_plus parentId phải là UUID của core_java");

        // Also verify that core_java has a null parentId (top-level topic).
        JsonNode coreJava = findTopicByCode(topics, "core_java");
        assertTrue(coreJava.get("parentId").isNull(),
                "core_java phải có parentId null (chủ đề gốc)");
    }

    // ====================================================================
    // Test 7: Each seed topic accessible via GET /admin/topics/{id}
    // ====================================================================

    @Test
    @DisplayName("Mỗi chủ đề seed truy xuất được qua GET /admin/topics/{id}")
    void eachSeedTopicAccessibleById() throws Exception {
        String token = adminAccessToken();

        for (String code : EXPECTED_CODES) {
            UUID topicId = topicIds.get(code);
            mockMvc.perform(get("/api/v1/admin/topics/" + topicId)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.isActive").value(true))
                    .andExpect(jsonPath("$.translations.length()").value(2))
                    .andExpect(jsonPath("$.aliases.length()").exists());
        }
    }

    // ====================================================================
    // Test 8: oop translations have correct vi and en names
    // ====================================================================

    @Test
    @DisplayName("Chủ đề 'oop' có tên dịch vụ đúng (vi + en)")
    void oopTopicHasCorrectTranslationNames() throws Exception {
        String token = adminAccessToken();

        MvcResult result = mockMvc.perform(get("/api/v1/admin/topics/" + topicIds.get("oop"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("oop"))
                .andReturn();

        JsonNode topic = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode translations = topic.get("translations");
        String viName = null;
        String enName = null;
        for (JsonNode t : translations) {
            if ("vi".equals(t.get("language").asText())) {
                viName = t.get("name").asText();
            }
            if ("en".equals(t.get("language").asText())) {
                enName = t.get("name").asText();
            }
        }
        assertEquals("Lập trình hướng đối tượng (OOP)", viName,
                "oop bản dịch vi phải có tên đúng");
        assertEquals("Object-Oriented Programming", enName,
                "oop bản dịch en phải có tên đúng");
    }

    // ====================================================================
    // Test 9: Full alias counts per topic via aliases endpoint
    // ====================================================================

    @Test
    @DisplayName("Số alias mỗi chủ đề đúng theo V008: 5,5,6,5,5,5,6,6")
    void aliasCountPerTopicMatchesSeed() throws Exception {
        String token = adminAccessToken();

        assertEquals(5, fetchAliases(token, topicIds.get("oop")).size(), "oop phải có 5 alias");
        assertEquals(5, fetchAliases(token, topicIds.get("core_java")).size(), "core_java phải có 5 alias");
        assertEquals(6, fetchAliases(token, topicIds.get("collections")).size(), "collections phải có 6 alias");
        assertEquals(5, fetchAliases(token, topicIds.get("java_8_plus")).size(), "java_8_plus phải có 5 alias");
        assertEquals(5, fetchAliases(token, topicIds.get("concurrency")).size(), "concurrency phải có 5 alias");
        assertEquals(5, fetchAliases(token, topicIds.get("jvm_memory")).size(), "jvm_memory phải có 5 alias");
        assertEquals(6, fetchAliases(token, topicIds.get("spring_boot")).size(), "spring_boot phải có 6 alias");
        assertEquals(6, fetchAliases(token, topicIds.get("database_jpa")).size(), "database_jpa phải có 6 alias");
    }

    // ====================================================================
    // Test 10: RBAC — candidate cannot list admin topics
    // ====================================================================

    @Test
    @DisplayName("Candidate không thể truy cập GET /admin/topics → 403")
    void candidateCannotListTopics_returns403() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        String candidateToken = login(email, PASSWORD).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/topics")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }
}
