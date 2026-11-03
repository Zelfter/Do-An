package com.example.interview.api.admin;

import com.example.interview.application.topic.TopicService;
import com.example.interview.infrastructure.persistence.TopicAliasJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Admin topic-management endpoints (design §3.2, FR-37).
 *
 * <p>All routes live under {@code /api/v1/admin/topics} and are therefore
 * protected by {@code hasRole(ADMIN)} in
 * {@link com.example.interview.infrastructure.security.SecurityConfig}
 * (FR-RBAC, NFR-19). A Candidate JWT yields 403; no JWT yields 401.
 *
 * <ul>
 *   <li>GET    /api/v1/admin/topics              → 200 [TopicDto]</li>
 *   <li>POST   /api/v1/admin/topics              → 201 TopicDto | 409 (code duplicate)</li>
 *   <li>GET    /api/v1/admin/topics/{id}         → 200 TopicDto | 404</li>
 *   <li>PUT    /api/v1/admin/topics/{id}         → 200 TopicDto</li>
 *   <li>PATCH  /api/v1/admin/topics/{id}         → 200 TopicDto (partial update incl. is_active)</li>
 *   <li>DELETE /api/v1/admin/topics/{id}         → 204 | 409 (has linked questions/docs)</li>
 *   <li>GET    /api/v1/admin/topics/{id}/aliases  → 200 [AliasDto]</li>
 *   <li>POST   /api/v1/admin/topics/{id}/aliases  → 201 AliasDto</li>
 *   <li>DELETE /api/v1/admin/topics/{id}/aliases/{aliasId} → 204</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/topics")
public class AdminTopicController {

    private final TopicService topicService;

    public AdminTopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    /** Lists all topics (FR-37). */
    @GetMapping
    public List<TopicResponse.TopicDto> listTopics() {
        return topicService.listTopics().stream()
                .map(TopicResponse.TopicDto::from)
                .toList();
    }

    /** AC-TOPIC-01: create a topic → 201 | 409 (duplicate code) | 400 (invalid). */
    @PostMapping
    public ResponseEntity<TopicResponse.TopicDto> createTopic(
            @Valid @RequestBody TopicRequest.TopicCreateRequest request) {
        TopicJpaEntity topic = topicService.createTopic(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TopicResponse.TopicDto.from(topic));
    }

    /** Fetches a single topic by id → 404 when not found. */
    @GetMapping("/{id}")
    public TopicResponse.TopicDto getTopic(@PathVariable UUID id) {
        TopicJpaEntity topic = topicService.getTopicById(id);
        return TopicResponse.TopicDto.from(topic);
    }

    /** Full update (code immutable). */
    @PutMapping("/{id}")
    public TopicResponse.TopicDto updateTopic(
            @PathVariable UUID id,
            @Valid @RequestBody TopicRequest.TopicUpdateRequest request) {
        TopicJpaEntity topic = topicService.updateTopic(id, request);
        return TopicResponse.TopicDto.from(topic);
    }

    /** AC-TOPIC-03: partial update (e.g. isActive = false to deactivate). */
    @PatchMapping("/{id}")
    public TopicResponse.TopicDto patchTopic(
            @PathVariable UUID id,
            @Valid @RequestBody TopicRequest.TopicPatchRequest request) {
        TopicJpaEntity topic = topicService.patchTopic(id, request);
        return TopicResponse.TopicDto.from(topic);
    }

    /** AC-TOPIC-02: delete → 204 | 409 (has linked questions/docs). */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopic(@PathVariable UUID id) {
        topicService.deleteTopic(id);
        return ResponseEntity.noContent().build();
    }

    /** Lists aliases for a topic (FR-37). */
    @GetMapping("/{id}/aliases")
    public List<TopicResponse.AliasDto> listAliases(@PathVariable UUID id) {
        return topicService.listAliases(id).stream()
                .map(TopicResponse.AliasDto::from)
                .toList();
    }

    /** Adds an alias to a topic → 201 | 409 (duplicate). */
    @PostMapping("/{id}/aliases")
    public ResponseEntity<TopicResponse.AliasDto> addAlias(
            @PathVariable UUID id,
            @Valid @RequestBody TopicRequest.AliasCreateRequest request) {
        TopicAliasJpaEntity alias = topicService.addAlias(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TopicResponse.AliasDto.from(alias));
    }

    /** Removes an alias from a topic → 204 | 404. */
    @DeleteMapping("/{id}/aliases/{aliasId}")
    public ResponseEntity<Void> deleteAlias(
            @PathVariable UUID id,
            @PathVariable UUID aliasId) {
        topicService.deleteAlias(id, aliasId);
        return ResponseEntity.noContent().build();
    }
}
