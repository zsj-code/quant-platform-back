package com.quant.platform.ai.core.factor.sentiment.marketwide;

import com.quant.platform.ai.core.client.EastmoneyMarketMarginTradingClient;
import com.quant.platform.ai.core.client.ThsFuyaoMarketChartClient;
import com.quant.platform.ai.core.client.dto.EastmoneyMarketMarginHistoryPageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyMarketMarginHistoryRowDTO;
import com.quant.platform.ai.core.client.dto.ThsMarketTurnoverMinuteChartDTO;
import com.quant.platform.ai.core.factor.sentiment.SentimentContext;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactor;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactorGroup;
import com.quant.platform.ai.core.factor.sentiment.SentimentMdThresholds;
import com.quant.platform.ai.core.factor.technical.FactorResult;
import com.quant.platform.ai.core.factor.technical.FactorSignalLevel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * S4 全市场融资买入情绪：融资买入额相对全市场成交额占比（%），阈值见 {@link SentimentMdThresholds} 与 {@code md/情绪面.md}。
 * <p>
 * 实现口径（在仅有东财全市场历史 + 同花顺分时两路数据的前提下）：
 * <ul>
 *     <li><b>分子</b>：{@link EastmoneyMarketMarginTradingClient} {@code RPTA_RZRQ_LSHJ}，取最近 5 个交易日 {@code RZMRE}（融资买入额）算术平均；</li>
 *     <li><b>分母</b>：{@link ThsFuyaoMarketChartClient} {@code turnover_minute}，取汇总区 {@code header} 中 {@code key=turnover} 的当日全市场成交额（累计）。</li>
 * </ul>
 * 与文档「双端均为 5 日均」的理想口径存在差异，{@code metrics.s4DataNote} 中已注明，后续若有 A 股日成交额序列可对齐分母。
 */
public final class S4MarginFinancingEmotionFactor implements SentimentFactor {

    private static final int MIN_MARGIN_DAYS = 5;

    @Override
    public String factorKey() {
        return "S4_MARGIN_TO_TURNOVER";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.MARKET_WIDE;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        EastmoneyMarketMarginTradingClient marketMargin = ctx.getEastmoneyMarketMarginTradingClient();
        ThsFuyaoMarketChartClient thsFuyao = ctx.getThsFuyaoMarketChartClient();
        if (marketMargin == null || thsFuyao == null) {
            return missing(ctx, "缺少东财全市场融资融券客户端或同花顺扶摇客户端");
        }
        EastmoneyMarketMarginHistoryPageDTO page;
        ThsMarketTurnoverMinuteChartDTO chart;
        try {
            page = marketMargin.fetchMarketMarginHistory(1, EastmoneyMarketMarginTradingClient.DEFAULT_PAGE_SIZE);
            chart = thsFuyao.fetchMarketTurnoverMinute();
        } catch (Exception e) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder(factorKey())
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("融资买入情绪：东财或同花顺接口拉取失败")
                    .metrics(m)
                    .notes(List.of("symbol=" + ctx.getSymbol(), e.getClass().getSimpleName()))
                    .build();
        }
        List<EastmoneyMarketMarginHistoryRowDTO> rows = page == null ? null : page.rows();
        if (rows == null || rows.size() < MIN_MARGIN_DAYS) {
            return missing(ctx, "东财全市场融资融券历史不足 5 个交易日");
        }
        double sumRzmre = 0;
        for (int i = 0; i < MIN_MARGIN_DAYS; i++) {
            Long v = rows.get(i).rzmre();
            if (v == null) {
                return missing(ctx, "近 5 个交易日融资买入额 RZMRE 存在空值");
            }
            sumRzmre += v.doubleValue();
        }
        double avgRzmre5 = sumRzmre / MIN_MARGIN_DAYS;

        Long turnover = extractThsTodayTurnoverYuan(chart);
        if (turnover == null || turnover <= 0) {
            return missing(ctx, "同花顺分时图未返回当日全市场成交额（header.turnover）");
        }
        double marginBuyToMarketTurnoverPct = avgRzmre5 / turnover.doubleValue() * 100.0;

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("avgRzmre5MarketYuan", avgRzmre5);
        extra.put("thsMarketTurnoverYuan", turnover);
        extra.put("s4DataNote", "分子=东财RPTA_RZRQ_LSHJ近5日RZMRE均值；分母=同花顺turnover_minute.header当日成交额");
        extra.put("marginHistoryLatestDimDate", rows.get(0).dimDate());

        return classify(marginBuyToMarketTurnoverPct, extra);
    }

    /**
     * 同花顺「市场成交额分时」汇总：当日全市场成交额（{@code turnover}）。
     */
    static Long extractThsTodayTurnoverYuan(ThsMarketTurnoverMinuteChartDTO chart) {
        if (chart == null || chart.header() == null) {
            return null;
        }
        return chart.header()
                .stream()
                .filter(Objects::nonNull)
                .filter(h -> "turnover".equals(h.key()))
                .map(h -> h.val())
                .findFirst()
                .orElse(null);
    }

    private static FactorResult missing(SentimentContext ctx, String reason) {
        Map<String, Object> m = thresholdMap();
        return FactorResult.builder("S4_MARGIN_TO_TURNOVER")
                .level(FactorSignalLevel.UNAVAILABLE)
                .summary("融资买入情绪：" + reason)
                .metrics(m)
                .notes(List.of("symbol=" + ctx.getSymbol()))
                .build();
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("iceColdBelow", SentimentMdThresholds.S4_ICE_COLD_BELOW);
        m.put("coldNeutralHigh", SentimentMdThresholds.S4_COLD_NEUTRAL_HIGH);
        m.put("warmHigh", SentimentMdThresholds.S4_WARM_HIGH);
        m.put("euphoricAbove", SentimentMdThresholds.S4_EUPHORIC_ABOVE);
        return m;
    }

    public static FactorResult classify(double marginBuyToMarketTurnoverPct) {
        return classify(marginBuyToMarketTurnoverPct, null);
    }

    /**
     * @param extraMetrics 附加诊断字段（写入 metrics，置于阈值常量之后）
     */
    public static FactorResult classify(double marginBuyToMarketTurnoverPct, Map<String, Object> extraMetrics) {
        Map<String, Object> m = thresholdMap();
        if (extraMetrics != null) {
            m.putAll(extraMetrics);
        }
        m.put("marginBuyToTurnoverPct5d", marginBuyToMarketTurnoverPct);
        if (marginBuyToMarketTurnoverPct < SentimentMdThresholds.S4_ICE_COLD_BELOW) {
            return FactorResult.builder("S4_MARGIN_TO_TURNOVER")
                    .level(FactorSignalLevel.INFO)
                    .summary("情绪冰点：杠杆资金不愿入场，接近阶段底部区域")
                    .metrics(m)
                    .build();
        }
        if (marginBuyToMarketTurnoverPct < SentimentMdThresholds.S4_COLD_NEUTRAL_HIGH) {
            return FactorResult.builder("S4_MARGIN_TO_TURNOVER")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("中性偏冷")
                    .metrics(m)
                    .build();
        }
        if (marginBuyToMarketTurnoverPct < SentimentMdThresholds.S4_WARM_HIGH) {
            return FactorResult.builder("S4_MARGIN_TO_TURNOVER")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("温和活跃")
                    .metrics(m)
                    .build();
        }
        if (marginBuyToMarketTurnoverPct <= SentimentMdThresholds.S4_EUPHORIC_ABOVE) {
            return FactorResult.builder("S4_MARGIN_TO_TURNOVER")
                    .level(FactorSignalLevel.WARNING)
                    .summary("亢奋：注意过热")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("S4_MARGIN_TO_TURNOVER")
                .level(FactorSignalLevel.WARNING)
                .summary("极度亢奋：杠杆过重，易引发踩踏式下跌")
                .metrics(m)
                .build();
    }
}
