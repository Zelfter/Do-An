-- =============================================================================
-- Verification script: V008 seed data integrity for default topics
-- Spec 01, Task 02 DoD: "8 chủ đề seed tồn tại với aliases"
-- SRS §9.4 / requirements.md §7
--
-- Run after `mvn flyway:migrate` against a database with the `app` schema.
-- Usage (local-dev):
--   psql -U postgres -d interview_dev -f backend/src/test/resources/verify-topics-seed.sql
-- Or via Docker:
--   docker exec pg-seed psql -U postgres -d interview_dev -f - < backend/src/test/resources/verify-topics-seed.sql
--
-- Expected results (every query should return the value noted in the comment).
-- All 8 topics: oop, core_java, collections, java_8_plus, concurrency,
--               jvm_memory, spring_boot, database_jpa
-- =============================================================================

\echo '========================================='
\echo '1) Topic count — expect 8'
\echo '========================================='
SELECT count(*) AS topic_count
FROM app.topics;
-- Expect: 8

\echo ''
\echo '========================================='
\echo '2) All topics active — expect 8'
\echo '========================================='
SELECT count(*) AS active_topic_count
FROM app.topics
WHERE is_active = TRUE;
-- Expect: 8

\echo ''
\echo '========================================='
\echo '3) Topic codes (ordered) — expect 8 rows'
\echo '    collections, concurrency, core_java, database_jpa,'
\echo '    java_8_plus, jvm_memory, oop, spring_boot'
\echo '========================================='
SELECT code, is_active
FROM app.topics
ORDER BY code;
-- Expect: 8 rows, all is_active = t

\echo ''
\echo '========================================='
\echo '4) Translations count — expect 16 (8 topics x 2 languages)'
\echo '========================================='
SELECT count(*) AS translation_count
FROM app.topic_translations;
-- Expect: 16

\echo ''
\echo '========================================='
\echo '5) Each topic has BOTH vi and en — expect 0 rows (missing translations)'
\echo '========================================='
SELECT t.code
FROM app.topics t
WHERE NOT EXISTS (
    SELECT 1 FROM app.topic_translations tt
    WHERE tt.topic_id = t.id AND tt.language = 'vi'
)
   OR NOT EXISTS (
    SELECT 1 FROM app.topic_translations tt
    WHERE tt.topic_id = t.id AND tt.language = 'en'
)
ORDER BY t.code;
-- Expect: 0 rows (all topics have both languages)

\echo ''
\echo '========================================='
\echo '6) All translations (vi + en names) — expect 16 rows'
\echo '========================================='
SELECT t.code, tt.language, tt.name
FROM app.topics t
JOIN app.topic_translations tt ON t.id = tt.topic_id
ORDER BY t.code, tt.language;
-- Expect: 16 rows

\echo ''
\echo '========================================='
\echo '7) Total aliases — expect 43'
\echo '========================================='
SELECT count(*) AS total_aliases
FROM app.topic_aliases;
-- Expect: 43

\echo ''
\echo '========================================='
\echo '8) Aliases per topic — expect 8 rows'
\echo '    oop=5, core_java=5, collections=6, java_8_plus=5,'
\echo '    concurrency=5, jvm_memory=5, spring_boot=6, database_jpa=6'
\echo '========================================='
SELECT t.code, count(*) AS alias_count
FROM app.topics t
JOIN app.topic_aliases a ON t.id = a.topic_id
GROUP BY t.code
ORDER BY t.code;
-- Expect: 8 rows, total 43

\echo ''
\echo '========================================='
\echo '9) Every topic has at least one alias — expect 0 rows (orphans)'
\echo '========================================='
SELECT t.code
FROM app.topics t
WHERE NOT EXISTS (
    SELECT 1 FROM app.topic_aliases a WHERE a.topic_id = t.id
)
ORDER BY t.code;
-- Expect: 0 rows

\echo ''
\echo '========================================='
\echo '10) Parent-child relationship — expect java_8_plus -> core_java'
\echo '========================================='
SELECT child.code AS child_code, parent.code AS parent_code
FROM app.topics child
LEFT JOIN app.topics parent ON child.parent_id = parent.id
WHERE child.parent_id IS NOT NULL;
-- Expect: 1 row: java_8_plus | core_java

\echo ''
\echo '========================================='
\echo '11) Full seed summary — expect 8 rows'
\echo '========================================='
SELECT
    t.code,
    t.is_active,
    parent.code AS parent_code,
    (SELECT count(*) FROM app.topic_translations tt WHERE tt.topic_id = t.id) AS translations,
    (SELECT count(*) FROM app.topic_aliases a WHERE a.topic_id = t.id) AS aliases
FROM app.topics t
LEFT JOIN app.topics parent ON t.parent_id = parent.id
ORDER BY t.code;
-- Expect: 8 rows, translations=2 each, aliases as noted above

\echo ''
\echo '========================================='
\echo 'Done. Review output against expected values above.'
\echo '========================================='
