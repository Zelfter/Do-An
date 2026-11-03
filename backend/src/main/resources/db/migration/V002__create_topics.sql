-- Migration V002: Topics, Translations, Aliases
-- FR-37: Admin manages topic catalog and aliases
-- NFR-27: multi-language support via topic_translations
-- BR-07: question scope follows JD topics; aliases used for JD mapping

CREATE TABLE topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(64) NOT NULL UNIQUE,
    parent_id UUID REFERENCES topics(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_topics_code ON topics(code);
CREATE INDEX idx_topics_parent_id ON topics(parent_id);
CREATE INDEX idx_topics_is_active ON topics(is_active);

COMMENT ON TABLE topics IS 'FR-37: Topic catalog — code is immutable after creation (design.md §3.1)';
COMMENT ON COLUMN topics.code IS 'Unique snake_case identifier, immutable — used as stable foreign reference';
COMMENT ON COLUMN topics.parent_id IS 'Support 1-level tree (FR-37) — e.g. java_8 is child of core_java';
COMMENT ON COLUMN topics.is_active IS 'Soft-delete: deactivate instead of DELETE when topic has linked questions (FR-37, AC-TOPIC-02)';

-- Topic translations for i18n (NFR-27: no hard-coded strings)
CREATE TABLE topic_translations (
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    language VARCHAR(8) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    PRIMARY KEY (topic_id, language)
);

COMMENT ON TABLE topic_translations IS 'NFR-27: language is a property of content, not hard-coded — supports vi and en';
COMMENT ON COLUMN topic_translations.language IS 'BCP-47 tag: ''vi'' (primary MVP) or ''en'' (future NFR-27)';

-- Topic aliases for JD mapping (FR-37, BR-07)
CREATE TABLE topic_aliases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    alias VARCHAR(200) NOT NULL,
    UNIQUE (topic_id, alias)
);

CREATE INDEX idx_topic_aliases_topic_id ON topic_aliases(topic_id);
CREATE INDEX idx_topic_aliases_alias ON topic_aliases(alias);

COMMENT ON TABLE topic_aliases IS 'FR-37, BR-07: keyword synonyms used by JD analysis to map raw JD terms to topic codes';
COMMENT ON COLUMN topic_aliases.alias IS 'Case-insensitive match expected at query level, e.g. ''OOP'', ''Lập trình hướng đối tượng''';
