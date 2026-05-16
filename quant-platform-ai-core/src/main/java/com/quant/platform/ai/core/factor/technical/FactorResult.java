package com.quant.platform.ai.core.factor.technical;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FactorResult {
    private final String factorKey;
    private final FactorSignalLevel level;
    private final String summary;
    private final Map<String, Object> metrics;
    private final List<String> notes;

    public FactorResult(
            String factorKey,
            FactorSignalLevel level,
            String summary,
            Map<String, Object> metrics,
            List<String> notes
    ) {
        this.factorKey = factorKey;
        this.level = level;
        this.summary = summary;
        this.metrics = metrics;
        this.notes = notes;
    }

    public String getFactorKey() {
        return factorKey;
    }

    public FactorSignalLevel getLevel() {
        return level;
    }

    public String getSummary() {
        return summary;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public List<String> getNotes() {
        return notes;
    }

    public static FactorResult unavailable(String factorKey, String reason) {
        return new FactorResult(factorKey, FactorSignalLevel.UNAVAILABLE, reason, Collections.emptyMap(), List.of(reason));
    }

    public static Builder builder(String factorKey) {
        return new Builder(factorKey);
    }

    public static final class Builder {
        private final String factorKey;
        private FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        private String summary = "";
        private final Map<String, Object> metrics = new LinkedHashMap<>();
        private List<String> notes = List.of();

        private Builder(String factorKey) {
            this.factorKey = factorKey;
        }

        public Builder level(FactorSignalLevel level) {
            this.level = level;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public Builder metric(String key, Object value) {
            this.metrics.put(key, value);
            return this;
        }

        public Builder metrics(Map<String, Object> toAdd) {
            if (toAdd != null) {
                this.metrics.putAll(toAdd);
            }
            return this;
        }

        public Builder notes(List<String> notes) {
            this.notes = notes == null ? List.of() : notes;
            return this;
        }

        public FactorResult build() {
            return new FactorResult(factorKey, level, summary, Collections.unmodifiableMap(metrics), notes);
        }
    }
}

