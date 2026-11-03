package com.example.interview.api.admin;

import com.example.interview.infrastructure.persistence.TopicAliasJpaEntity;
import com.example.interview.infrastructure.persistence.TopicJpaEntity;
import com.example.interview.infrastructure.persistence.TopicTranslationJpaEntity;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

/**
 * Response DTOs for the Admin topic endpoints (design §3.2, FR-37).
 *
 * <p>Mirrors the structure of {@link com.example.interview.api.auth.AuthResponse}:
 * a private constructor and public records nested inside.
 *
 * <ul>
 *   <li>{@link TopicDto} — full topic projection with translations and aliases.</li>
 *   <li>{@link TranslationDto} — also used as input in create/update/patch requests.</li>
 *   <li>{@link AliasDto} — alias projection.</li>
 * </ul>
 */
public final class TopicResponse {

    private TopicResponse() {
    }

    /** Full topic response returned by GET/POST/PUT/PATCH. */
    public record TopicDto(
            UUID id,
            String code,
            UUID parentId,
            boolean isActive,
            List<TranslationDto> translations,
            List<AliasDto> aliases
    ) {
        /** Projects a JPA entity (with initialized lazy collections) into the DTO. */
        public static TopicDto from(TopicJpaEntity topic) {
            UUID parentId = topic.getParent() != null ? topic.getParent().getId() : null;
            List<TranslationDto> translations = topic.getTranslations().stream()
                    .map(TranslationDto::from)
                    .toList();
            List<AliasDto> aliases = topic.getAliases().stream()
                    .map(AliasDto::from)
                    .toList();
            return new TopicDto(topic.getId(), topic.getCode(), parentId,
                    topic.isActive(), translations, aliases);
        }
    }

    /**
     * Translation projection — also used as input in create/update/patch requests
     * (referenced by {@link TopicRequest}). Validation annotations apply when
     * the list is annotated with {@code @Valid} on the request side.
     * NFR-27: language is an attribute of content, not hard-coded.
     */
    public record TranslationDto(
            @NotBlank(message = "Ngôn ngữ không được để trống")
            String language,
            @NotBlank(message = "Tên không được để trống")
            String name,
            String description
    ) {
        public static TranslationDto from(TopicTranslationJpaEntity t) {
            return new TranslationDto(t.getLanguage(), t.getName(), t.getDescription());
        }
    }

    /** Alias projection — keyword synonym for JD mapping (FR-37, BR-07). */
    public record AliasDto(UUID id, String alias) {
        public static AliasDto from(TopicAliasJpaEntity a) {
            return new AliasDto(a.getId(), a.getAlias());
        }
    }
}
