package com.quant.platform.ai.core.factor.fundamental;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FundamentalResult {
    private final String factorKey;
    private final FundamentalFactorGroup group;
    private final FundamentalDecision decision;
    private final Integer score; // QUALITY_SCORE 可填 0-100 或子项分
    private final String summary;
    private final Map<String, Object> metrics;
    private final List<String> notes;

    public FundamentalResult(String factorKey,
                             FundamentalFactorGroup group,
                             FundamentalDecision decision,
                             Integer score,
                             String summary,
                             Map<String, Object> metrics,
                             List<String> notes) {
        this.factorKey = factorKey;
        this.group = group;
        this.decision = decision;
        this.score = score;
        this.summary = summary;
        this.metrics = metrics;
        this.notes = notes;
    }

    public String getFactorKey() {
        return factorKey;
    }

    public FundamentalFactorGroup getGroup() {
        return group;
    }

    public FundamentalDecision getDecision() {
        return decision;
    }

    public Integer getScore() {
        return score;
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

    public static FundamentalResult unavailable(String factorKey, FundamentalFactorGroup group, String reason) {
        return new FundamentalResult(
                factorKey,
                group,
                FundamentalDecision.UNAVAILABLE,
                null,
                reason,
                Collections.emptyMap(),
                List.of(reason)
        );
    }

    public static Builder builder(String factorKey, FundamentalFactorGroup group) {
        return new Builder(factorKey, group);
    }

    public static final class Builder {
        private final String factorKey;
        private final FundamentalFactorGroup group;
        private FundamentalDecision decision = FundamentalDecision.PASS;
        private Integer score;
        private String summary = "";
        private final Map<String, Object> metrics = new LinkedHashMap<>();
        private List<String> notes = List.of();

        private Builder(String factorKey, FundamentalFactorGroup group) {
            this.factorKey = factorKey;
            this.group = group;
        }

        public Builder decision(FundamentalDecision decision) {
            this.decision = decision;
            return this;
        }

        public Builder score(Integer score) {
            this.score = score;
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

        public Builder notes(List<String> notes) {
            this.notes = notes == null ? List.of() : notes;
            return this;
        }

        public FundamentalResult build() {
            return new FundamentalResult(
                    factorKey,
                    group,
                    decision,
                    score,
                    summary,
                    Collections.unmodifiableMap(metrics),
                    notes
            );
        }
    }
}

