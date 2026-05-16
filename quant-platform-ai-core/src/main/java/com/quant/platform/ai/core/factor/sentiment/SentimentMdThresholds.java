package com.quant.platform.ai.core.factor.sentiment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code md/情绪面.md} 中的量化阈值（写死，与文档表格一致；后续因子实现直接引用本类常量）。
 */
public final class SentimentMdThresholds {
    private SentimentMdThresholds() {
    }

    // ----- S1 全市场恐慌-贪婪（综合指数分位，0-100）-----
    public static final double S1_EXTREME_FEAR_BELOW = 15;
    public static final double S1_FEAR_HIGH = 35;
    public static final double S1_NEUTRAL_HIGH = 65;
    public static final double S1_GREEDY_HIGH = 85;

    // ----- S2 50ETF 期权 PCR -----
    public static final double S2_EXTREME_PANIC_ABOVE = 0.95;
    public static final double S2_BEARISH_HIGH = 0.75;
    public static final double S2_NEUTRAL_HIGH = 0.55;
    public static final double S2_BULLISH_LOW = 0.40;

    // ----- S3 北向 20 日累计净买入（亿元人民币）-----
    public static final double S3_HUGE_OUTFLOW_BELOW = -300;
    public static final double S3_MID_OUTFLOW_BELOW = -100;
    public static final double S3_MID_INFLOW_ABOVE = 100;
    public static final double S3_HUGE_INFLOW_ABOVE = 300;

    // ----- S4 融资买入 / 全市场成交额（%）-----
    public static final double S4_ICE_COLD_BELOW = 6.5;
    public static final double S4_COLD_NEUTRAL_HIGH = 8.5;
    public static final double S4_WARM_HIGH = 10.5;
    public static final double S4_EUPHORIC_ABOVE = 12;

    // ----- F1 北向行业偏好（%）-----
    public static final double F1_HIGHLY_CONCENTRATED_ABOVE = 40;
    public static final double F1_DISPERSED_MAX_SHARE_BELOW = 20;

    // ----- F2 大单与散户背离（%）-----
    public static final double F2_STRONG_DIVERGENCE_ABS_ABOVE = 5;
    public static final double F2_FLAT_BAND_HALF_WIDTH = 3;

    // ----- F3 板块轮动保留率（%）-----
    public static final double F3_STRONG_PERSISTENCE_ABOVE = 60;
    public static final double F3_WEAK_PERSISTENCE_BELOW = 30;

    // ----- F4 涨停晋级率（%）-----
    public static final double F4_EXCELLENT_ABOVE = 25;
    public static final double F4_NORMAL_HIGH = 15;
    public static final double F4_WEAK_HIGH = 10;

    // ----- G1 舆情热度倍数 -----
    public static final double G1_ANOMALY_BURST_ABOVE = 5;
    public static final double G1_WARM_HIGH = 2;
    public static final double G1_COLD_BELOW = 0.5;

    // ----- G2 舆情情感比 -----
    public static final double G2_EUPHORIA_ABOVE = 4;
    public static final double G2_OPTIMISTIC_HIGH = 2;
    public static final double G2_DEBATE_LOW = 0.5;
    public static final double G2_PESSIMISTIC_LOW = 0.25;

    // ----- G3 删帖比例（%）-----
    public static final double G3_MANIPULATION_SUSPICION_ABOVE = 15;

    // ----- G4 北向个股 -----
    public static final int G4_STREAK_DAYS = 5;
    public static final double G4_CUMULATIVE_MV_PCT = 2;

    // ----- D1 IV 分位（%）-----
    public static final double D1_IV_HIGH_STRESS_PCT = 90;
    public static final double D1_IV_LOW_CALM_PCT = 10;

    // ----- D2 融券增速（%）-----
    public static final double D2_SHORT_BALANCE_SURGE_PCT = 30;

    // ----- D3 研报唱多词密度环比（%）-----
    public static final double D3_BULLISH_TITLE_DENSITY_MOM_SURGE_PCT = 50;

    /** 与编排层 JSON 输出结构一致，便于对外展示。 */
    public static Map<String, Object> summaryMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("S1", Map.of(
                "extremeFearBelow", S1_EXTREME_FEAR_BELOW,
                "fearHigh", S1_FEAR_HIGH,
                "neutralHigh", S1_NEUTRAL_HIGH,
                "greedyHigh", S1_GREEDY_HIGH));
        m.put("S2", Map.of(
                "extremePanicAbove", S2_EXTREME_PANIC_ABOVE,
                "bearishHigh", S2_BEARISH_HIGH,
                "neutralHigh", S2_NEUTRAL_HIGH,
                "bullishLow", S2_BULLISH_LOW));
        m.put("S3_cny100m", Map.of(
                "hugeOutflowBelow", S3_HUGE_OUTFLOW_BELOW,
                "midOutflowBelow", S3_MID_OUTFLOW_BELOW,
                "midInflowAbove", S3_MID_INFLOW_ABOVE,
                "hugeInflowAbove", S3_HUGE_INFLOW_ABOVE));
        m.put("S4_pct", Map.of(
                "iceColdBelow", S4_ICE_COLD_BELOW,
                "coldNeutralHigh", S4_COLD_NEUTRAL_HIGH,
                "warmHigh", S4_WARM_HIGH,
                "euphoricAbove", S4_EUPHORIC_ABOVE));
        m.put("F1_pct", Map.of(
                "highlyConcentratedAbove", F1_HIGHLY_CONCENTRATED_ABOVE,
                "dispersedMaxShareBelow", F1_DISPERSED_MAX_SHARE_BELOW));
        m.put("F2_pct", Map.of(
                "strongDivergenceAbsAbove", F2_STRONG_DIVERGENCE_ABS_ABOVE,
                "flatBandHalfWidth", F2_FLAT_BAND_HALF_WIDTH));
        m.put("F3_pct", Map.of(
                "strongPersistenceAbove", F3_STRONG_PERSISTENCE_ABOVE,
                "weakPersistenceBelow", F3_WEAK_PERSISTENCE_BELOW));
        m.put("F4_pct", Map.of(
                "excellentAbove", F4_EXCELLENT_ABOVE,
                "normalHigh", F4_NORMAL_HIGH,
                "weakHigh", F4_WEAK_HIGH));
        m.put("G1_ratio", Map.of(
                "anomalyBurstAbove", G1_ANOMALY_BURST_ABOVE,
                "warmHigh", G1_WARM_HIGH,
                "coldBelow", G1_COLD_BELOW));
        m.put("G2_ratio", Map.of(
                "euphoriaAbove", G2_EUPHORIA_ABOVE,
                "optimisticHigh", G2_OPTIMISTIC_HIGH,
                "debateLow", G2_DEBATE_LOW,
                "pessimisticLow", G2_PESSIMISTIC_LOW));
        m.put("G3_pct", Map.of("manipulationSuspicionAbove", G3_MANIPULATION_SUSPICION_ABOVE));
        m.put("G4", Map.of("streakDays", G4_STREAK_DAYS, "cumulativeMvPct", G4_CUMULATIVE_MV_PCT));
        m.put("D1_pct", Map.of("ivHighStressPct", D1_IV_HIGH_STRESS_PCT, "ivLowCalmPct", D1_IV_LOW_CALM_PCT));
        m.put("D2_pct", Map.of("shortBalanceSurgePct", D2_SHORT_BALANCE_SURGE_PCT));
        m.put("D3_pct", Map.of("bullishTitleDensityMomSurgePct", D3_BULLISH_TITLE_DENSITY_MOM_SURGE_PCT));
        return m;
    }
}
