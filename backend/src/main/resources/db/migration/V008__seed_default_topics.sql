-- Migration V008: Seed Default Topics
-- SRS §9.4: ~8 chủ đề mặc định với aliases tiếng Việt + tiếng Anh
-- FR-37: topic.code immutable; translations in topic_translations; aliases for JD mapping

-- 1. OOP (Object-Oriented Programming)
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('11111111-1111-1111-1111-111111111111', 'oop', NULL, TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'vi', 'Lập trình hướng đối tượng (OOP)', 'Các khái niệm OOP: class, object, encapsulation, inheritance, polymorphism, abstraction.'),
    ('11111111-1111-1111-1111-111111111111', 'en', 'Object-Oriented Programming', 'OOP concepts: class, object, encapsulation, inheritance, polymorphism, abstraction.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('11111111-1111-1111-1111-111111111111', 'OOP'),
    ('11111111-1111-1111-1111-111111111111', 'Lập trình hướng đối tượng'),
    ('11111111-1111-1111-1111-111111111111', 'Object-Oriented'),
    ('11111111-1111-1111-1111-111111111111', 'HĐDT'),
    ('11111111-1111-1111-1111-111111111111', 'Đối tượng');

-- 2. Core Java
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('22222222-2222-2222-2222-222222222222', 'core_java', NULL, TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('22222222-2222-2222-2222-222222222222', 'vi', 'Core Java', 'Cú pháp Java cơ bản, kiểu dữ liệu, String, Exception, Wrapper classes.'),
    ('22222222-2222-2222-2222-222222222222', 'en', 'Core Java', 'Basic Java syntax, data types, String, Exception handling, Wrapper classes.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('22222222-2222-2222-2222-222222222222', 'Core Java'),
    ('22222222-2222-2222-2222-222222222222', 'Java cơ bản'),
    ('22222222-2222-2222-2222-222222222222', 'Java basics'),
    ('22222222-2222-2222-2222-222222222222', 'Exception handling'),
    ('22222222-2222-2222-2222-222222222222', 'Java fundamental');

-- 3. Collections Framework
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('33333333-3333-3333-3333-333333333333', 'collections', NULL, TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('33333333-3333-3333-3333-333333333333', 'vi', 'Collections Framework', 'List, Set, Map, Queue, Iterator và các implementation (ArrayList, HashSet, HashMap, LinkedList...).'),
    ('33333333-3333-3333-3333-333333333333', 'en', 'Collections Framework', 'List, Set, Map, Queue, Iterator and their implementations.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('33333333-3333-3333-3333-333333333333', 'Collections'),
    ('33333333-3333-3333-3333-333333333333', 'Java Collections'),
    ('33333333-3333-3333-3333-333333333333', 'ArrayList'),
    ('33333333-3333-3333-3333-333333333333', 'HashMap'),
    ('33333333-3333-3333-3333-333333333333', 'HashSet'),
    ('33333333-3333-3333-3333-333333333333', 'LinkedList');

-- 4. Java 8+ (Lambda, Stream API, Functional Programming)
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('44444444-4444-4444-4444-444444444444', 'java_8_plus', '22222222-2222-2222-2222-222222222222', TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('44444444-4444-4444-4444-444444444444', 'vi', 'Java 8+ Lambda & Stream', 'Lambda expression, Stream API, Optional, Functional interfaces, Method reference.'),
    ('44444444-4444-4444-4444-444444444444', 'en', 'Java 8+ Lambda & Stream', 'Lambda expressions, Stream API, Optional, Functional interfaces, Method references.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('44444444-4444-4444-4444-444444444444', 'Java 8'),
    ('44444444-4444-4444-4444-444444444444', 'Lambda'),
    ('44444444-4444-4444-4444-444444444444', 'Stream API'),
    ('44444444-4444-4444-4444-444444444444', 'Functional programming'),
    ('44444444-4444-4444-4444-444444444444', 'Optional');

-- 5. Concurrency & Multithreading
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('55555555-5555-5555-5555-555555555555', 'concurrency', NULL, TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('55555555-5555-5555-5555-555555555555', 'vi', 'Concurrency & Multithreading', 'Thread, Runnable, Executor, synchronized, volatile, deadlock, java.util.concurrent.'),
    ('55555555-5555-5555-5555-555555555555', 'en', 'Concurrency & Multithreading', 'Thread, Runnable, Executor, synchronized, volatile, deadlock, java.util.concurrent.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('55555555-5555-5555-5555-555555555555', 'Multithreading'),
    ('55555555-5555-5555-5555-555555555555', 'Concurrency'),
    ('55555555-5555-5555-5555-555555555555', 'Thread'),
    ('55555555-5555-5555-5555-555555555555', 'Đa luồng'),
    ('55555555-5555-5555-5555-555555555555', 'java.util.concurrent');

-- 6. JVM & Memory Management
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('66666666-6666-6666-6666-666666666666', 'jvm_memory', NULL, TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('66666666-6666-6666-6666-666666666666', 'vi', 'JVM & Memory Management', 'JVM architecture, Heap, Stack, Garbage Collection, Memory leak, Class loading.'),
    ('66666666-6666-6666-6666-666666666666', 'en', 'JVM & Memory Management', 'JVM architecture, Heap, Stack, GC algorithms, Memory leak detection, Class loading.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('66666666-6666-6666-6666-666666666666', 'JVM'),
    ('66666666-6666-6666-6666-666666666666', 'Garbage Collection'),
    ('66666666-6666-6666-6666-666666666666', 'Memory Management'),
    ('66666666-6666-6666-6666-666666666666', 'Heap'),
    ('66666666-6666-6666-6666-666666666666', 'GC');

-- 7. Spring Boot
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('77777777-7777-7777-7777-777777777777', 'spring_boot', NULL, TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('77777777-7777-7777-7777-777777777777', 'vi', 'Spring Boot', 'Spring Boot fundamentals, Dependency Injection, REST API, Spring Data JPA, Spring Security.'),
    ('77777777-7777-7777-7777-777777777777', 'en', 'Spring Boot', 'Spring Boot fundamentals, DI, REST API development, Spring Data JPA, Spring Security.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('77777777-7777-7777-7777-777777777777', 'Spring Boot'),
    ('77777777-7777-7777-7777-777777777777', 'Spring Framework'),
    ('77777777-7777-7777-7777-777777777777', 'Spring'),
    ('77777777-7777-7777-7777-777777777777', 'Dependency Injection'),
    ('77777777-7777-7777-7777-777777777777', 'Spring Security'),
    ('77777777-7777-7777-7777-777777777777', 'Spring Data JPA');

-- 8. Database & JPA
INSERT INTO topics (id, code, parent_id, is_active) VALUES
    ('88888888-8888-8888-8888-888888888888', 'database_jpa', NULL, TRUE);

INSERT INTO topic_translations (topic_id, language, name, description) VALUES
    ('88888888-8888-8888-8888-888888888888', 'vi', 'Database & JPA', 'SQL, JDBC, JPA/Hibernate, Entity relationships, Transactions, Query optimization.'),
    ('88888888-8888-8888-8888-888888888888', 'en', 'Database & JPA', 'SQL, JDBC, JPA/Hibernate, Entity relationships, Transactions, Query optimization.');

INSERT INTO topic_aliases (topic_id, alias) VALUES
    ('88888888-8888-8888-8888-888888888888', 'Database'),
    ('88888888-8888-8888-8888-888888888888', 'JPA'),
    ('88888888-8888-8888-8888-888888888888', 'Hibernate'),
    ('88888888-8888-8888-8888-888888888888', 'SQL'),
    ('88888888-8888-8888-8888-888888888888', 'JDBC'),
    ('88888888-8888-8888-8888-888888888888', 'Cơ sở dữ liệu');

-- Confirmation comment
-- Total: 8 topics seeded with Vietnamese + English translations and aliases
-- AC: 8 chủ đề seed tồn tại với aliases ✅
