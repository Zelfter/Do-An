package com.example.interview.infrastructure.persistence;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for topic_translations table (composite PK: topic_id + language).
 * NFR-27: language is an attribute of content — no hard-coded strings.
 */
@Entity
@Table(name = "topic_translations")
@IdClass(TopicTranslationJpaEntity.TopicTranslationId.class)
public class TopicTranslationJpaEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, updatable = false)
    private TopicJpaEntity topic;

    /** BCP-47 language tag: 'vi' (primary) or 'en' (future NFR-27). */
    @Id
    @Column(name = "language", nullable = false, length = 8, updatable = false)
    private String language;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // ---- Getters / Setters ----

    public TopicJpaEntity getTopic() { return topic; }
    public void setTopic(TopicJpaEntity topic) { this.topic = topic; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    /** Composite PK class. */
    public static class TopicTranslationId implements Serializable {
        private UUID topic;
        private String language;

        public TopicTranslationId() {}
        public TopicTranslationId(UUID topic, String language) {
            this.topic = topic;
            this.language = language;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TopicTranslationId that)) return false;
            return Objects.equals(topic, that.topic) && Objects.equals(language, that.language);
        }

        @Override
        public int hashCode() { return Objects.hash(topic, language); }
    }
}
