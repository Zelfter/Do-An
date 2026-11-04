package com.example.interview.application.interview;

import com.example.interview.infrastructure.persistence.ScoringSettingsJpaEntity;
import com.example.interview.infrastructure.persistence.ScoringSettingsJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads scoring configuration from the {@code scoring_settings} table (NFR-29:
 * ALL thresholds/weights are configurable, never hard-coded).
 *
 * <p>The table stores one JSON row per config group (rubric_weights,
 * score_thresholds, follow_up_config, weak_topic_config, score_weights,
 * total_score_classification, quota_config). This service merges them into a
 * single {@link ScoringConfig} with safe defaults, and can serialise the merged
 * map as a per-session snapshot (NFR-29) and re-parse a snapshot later.
 */
@Service
public class ScoringConfigService {

    private final ScoringSettingsJpaRepository repo;
    private final ObjectMapper objectMapper;

    public ScoringConfigService(ScoringSettingsJpaRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    /** Immutable scoring configuration (BR-01/BR-03/BR-05/BR-13/BR-14). */
    public record ScoringConfig(
            BigDecimal wAccuracy, BigDecimal wCompleteness, BigDecimal wClarity, BigDecimal wExample,
            int goodThreshold, int mediumThreshold,
            int followUpTriggerThreshold, int followUpMaxConsecutive,
            int weakTopicThreshold, int minWeakAnswers,
            BigDecimal requiredSkillMultiplier,
            int excellentThreshold, int goodClassThreshold, int averageThreshold
    ) {
        /** BR-01: weighted total (0–10) from per-criterion scores (each 0–10). */
        public BigDecimal total(BigDecimal a, BigDecimal c, BigDecimal cl, BigDecimal e) {
            return a.multiply(wAccuracy)
                    .add(c.multiply(wCompleteness))
                    .add(cl.multiply(wClarity))
                    .add(e.multiply(wExample))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        /** Percentage 0–100 (total is on a 0–10 scale). */
        public int pct(BigDecimal total) {
            if (total == null) return 0;
            return total.multiply(BigDecimal.TEN).setScale(0, RoundingMode.HALF_UP).intValue();
        }

        /** BR-13: classification label (Vietnamese) from a percentage. */
        public String classification(int pct) {
            if (pct >= excellentThreshold) return "Xuất sắc";
            if (pct >= goodClassThreshold) return "Khá tốt";
            if (pct >= averageThreshold) return "Trung bình";
            return "Cần cải thiện";
        }
    }

    /** Current configuration merged from the DB. */
    public ScoringConfig current() {
        return parse(loadMerged());
    }

    /** JSON snapshot captured at session creation (NFR-29). */
    public String snapshotJson() {
        try {
            return objectMapper.writeValueAsString(loadMerged());
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Re-parse a stored snapshot; falls back to defaults on any error. */
    public ScoringConfig fromSnapshot(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = objectMapper.readValue(json == null ? "{}" : json, Map.class);
            return parse(m);
        } catch (Exception e) {
            return defaults();
        }
    }

    // ---- internal ----

    private Map<String, Object> loadMerged() {
        Map<String, Object> merged = new HashMap<>();
        for (ScoringSettingsJpaEntity e : repo.findAll()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = objectMapper.readValue(e.getValue(), Map.class);
                merged.putAll(m);
            } catch (Exception ignored) {
                // Ignore a single malformed row; defaults cover missing keys.
            }
        }
        return merged;
    }

    private ScoringConfig defaults() {
        return parse(new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private ScoringConfig parse(Map<String, Object> m) {
        Map<String, Object> rw = sub(m, "rubric_weights");
        Map<String, Object> st = sub(m, "score_thresholds");
        Map<String, Object> fu = sub(m, "follow_up_config");
        Map<String, Object> wt = sub(m, "weak_topic_config");
        Map<String, Object> sw = sub(m, "score_weights");
        Map<String, Object> tc = sub(m, "total_score_classification");
        return new ScoringConfig(
                dec(rw, "accuracy", 0.50), dec(rw, "completeness", 0.30),
                dec(rw, "clarity", 0.10), dec(rw, "example", 0.10),
                intg(st, "good", 70), intg(st, "medium", 40),
                intg(fu, "trigger_threshold", 70), intg(fu, "max_consecutive", 2),
                intg(wt, "topic_score_threshold", 50), intg(wt, "min_weak_answers", 2),
                dec(sw, "required_skill_multiplier", 1.5),
                intg(tc, "excellent", 80), intg(tc, "good", 65), intg(tc, "average", 50)
        );
    }

    private Map<String, Object> sub(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return (o instanceof Map) ? (Map<String, Object>) o : new HashMap<>();
    }

    private BigDecimal dec(Map<String, Object> m, String key, double def) {
        Object o = m.get(key);
        if (o == null) return BigDecimal.valueOf(def);
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        try {
            return BigDecimal.valueOf(Double.parseDouble(o.toString())).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.valueOf(def);
        }
    }

    private int intg(Map<String, Object> m, String key, int def) {
        Object o = m.get(key);
        if (o == null) return def;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return def;
        }
    }
}
