package com.example.interview.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;
import java.util.UUID;

/**
 * Request DTOs for the Admin topic endpoints (design §3.2, FR-37).
 *
 * <p>Mirrors the structure of {@link com.example.interview.api.auth.AuthRequest}:
 * a private constructor and public records nested inside.
 *
 * <ul>
 *   <li>{@link TopicCreateRequest} — POST /admin/topics (code immutable after creation).</li>
 *   <li>{@link TopicUpdateRequest} — PUT /admin/topics/{id} (full update, code immutable).</li>
 *   <li>{@link TopicPatchRequest} — PATCH /admin/topics/{id} (partial update incl. isActive).</li>
 *   <li>{@link AliasCreateRequest} — POST /admin/topics/{id}/aliases.</li>
 * </ul>
 */
public final class TopicRequest {

    private TopicRequest() {
    }

    /**
     * POST /api/v1/admin/topics.
     *
     * @param code         snake_case, immutable after creation (FR-37).
     * @param parentId     optional, for 1-level hierarchy.
     * @param translations at least one 'vi' translation required (service validates).
     * @param aliases      optional list of keyword synonyms.
     */
    public record TopicCreateRequest(
            @NotBlank(message = "Code không được để trống")
            @Pattern(regexp = "^[a-z][a-z0-9_]*$",
                    message = "Code phải là snake_case (chữ thường, số, gạch dưới)")
            String code,
            UUID parentId,
            @Valid List<TopicResponse.TranslationDto> translations,
            List<String> aliases
    ) {
    }

    /**
     * PUT /api/v1/admin/topics/{id} — full update.
     * Code is immutable (not included); {@code code} has {@code updatable = false}
     * in the JPA entity.
     */
    public record TopicUpdateRequest(
            UUID parentId,
            @Valid List<TopicResponse.TranslationDto> translations,
            List<String> aliases
    ) {
    }

    /**
     * PATCH /api/v1/admin/topics/{id} — partial update.
     * Only non-null fields are applied.
     * AC-TOPIC-03: {@code isActive = false} deactivates a topic.
     */
    public record TopicPatchRequest(
            UUID parentId,
            Boolean isActive,
            @Valid List<TopicResponse.TranslationDto> translations,
            List<String> aliases
    ) {
    }

    /** POST /api/v1/admin/topics/{id}/aliases — add a keyword alias. */
    public record AliasCreateRequest(
            @NotBlank(message = "Alias không được để trống")
            String alias
    ) {
    }
}
