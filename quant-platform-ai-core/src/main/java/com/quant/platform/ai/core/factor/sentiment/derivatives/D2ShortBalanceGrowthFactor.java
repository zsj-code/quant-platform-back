package com.quant.platform.ai.core.factor.sentiment.derivatives;

import com.quant.platform.ai.core.client.EastmoneyStockMarginTradingClient;
import com.quant.platform.ai.core.client.dto.EastmoneyMarginTradingStockPageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyMarginTradingStockRowDTO;
import com.quant.platform.ai.core.factor.sentiment.SentimentContext;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactor;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactorGroup;
import com.quant.platform.ai.core.factor.sentiment.SentimentMdThresholds;
import com.quant.platform.ai.core.factor.technical.FactorResult;
import com.quant.platform.ai.core.factor.technical.FactorSignalLevel;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.common.dto.KlineBarDTO;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * D2 融券余额增长（{@code md/情绪面.md}）：核心量为<strong>近 5 个交易日融券余额增长比例</strong>（相对 5 日前融券余额的百分比变化）。
 * <p>
 * 「股价处于高位 / 已暴跌」<strong>仅</strong>依据<strong>落库日 K（{@code D}）</strong>的收盘价序列与最新一根涨跌幅（{@link KlineBarDTO#getChangePct}）；
 * 不使用融资融券明细中的 {@code SPJ}/{@code ZDF}。若无日 K 端口或无日 K 数据，则价位标签为未知（{@code priceAtHigh == null}）。
 * <p>
 * 文档分档（阈值见 {@link SentimentMdThresholds#D2_SHORT_BALANCE_SURGE_PCT}）：
 * <ul>
 *     <li><strong>大于 30% 且股价处于高位</strong>：有人在大量借券做空，看跌信号极强；</li>
 *     <li><strong>大于 30% 且股价已暴跌</strong>：可能是跌后对未来进一步分歧加大，也可能是做空力量即将衰竭（物极必反），需结合价量。</li>
 * </ul>
 */
public final class D2ShortBalanceGrowthFactor implements SentimentFactor {

    private static final int PAGE_NUMBER = 1;
    private static final int PAGE_SIZE = 50;
    /** 近 5 日增速：最新日与往前第 5 个交易日对比，至少需 6 条有效日频融券余额 */
    private static final int MIN_ROWS_FOR_5D_GROWTH = 6;
    /** 拉取日 K 条数（升序最老→最新），需覆盖 {@link #PRICE_WINDOW_MAX_DAYS} 且有冗余 */
    private static final int D1_DAILY_BAR_LIMIT = 30;
    /** 用最近若干日收盘价判断「高位 / 暴跌」的窗口上限 */
    private static final int PRICE_WINDOW_MAX_DAYS = 20;
    /** 相对窗口内最高价回撤达到该比例（%）视为「已暴跌 / 深跌」一侧 */
    private static final BigDecimal DRAWDOWN_DEEP_PCT = BigDecimal.valueOf(10);
    /** 收盘价落在近窗区间下沿的比例以下视为偏「暴跌」一侧 */
    private static final BigDecimal RANGE_LOW_BAND = new BigDecimal("0.20");
    /** 收盘价落在近窗区间上沿的比例以上且回撤不大视为「高位」一侧 */
    private static final BigDecimal RANGE_HIGH_BAND = new BigDecimal("0.75");
    /** 视为高位时，允许相对窗口最高价的最大回撤（%） */
    private static final BigDecimal HIGH_POSITION_MAX_DRAWDOWN_PCT = BigDecimal.valueOf(5);
    /** 最新一日涨跌幅（%）低于该值时强化「暴跌」判定（仅日 K {@code changePct}） */
    private static final BigDecimal CRASH_DAY_CHANGE_PCT_THRESHOLD = new BigDecimal("-8");
    /** 窗口高低价过窄（相对最高价）时不判位置 */
    private static final BigDecimal MIN_RANGE_TO_HIGH_RATIO = new BigDecimal("0.005");

    /** 价位窗口：收盘价按日期新→旧；涨跌幅为最新一日 */
    private record PriceWindowInput(List<BigDecimal> closesNewestFirst, BigDecimal latestDayChangePct, String source) {
    }

    @Override
    public String factorKey() {
        return "D2_SHORT_BALANCE_GROWTH";
    }

    @Override
    public SentimentFactorGroup group() {
        return SentimentFactorGroup.DERIVATIVES_AND_SHADOW;
    }

    @Override
    public FactorResult evaluate(SentimentContext ctx) {
        EastmoneyStockMarginTradingClient client = ctx.getEastmoneyStockMarginTradingClient();
        if (client == null) {
            return missing(ctx, "缺少东财融资融券客户端依赖");
        }
        String symbol = ctx.getSymbol();
        if (symbol == null || symbol.isBlank()) {
            return missing(ctx, "缺少 symbol");
        }
        EastmoneyMarginTradingStockPageDTO page;
        try {
            page = client.fetchStockMarginTrading(symbol, PAGE_NUMBER, PAGE_SIZE);
        } catch (Exception e) {
            Map<String, Object> m = thresholdMap();
            return FactorResult.builder("D2_SHORT_BALANCE_GROWTH")
                    .level(FactorSignalLevel.UNAVAILABLE)
                    .summary("近5日融券余额增长比例：东财个股融资融券明细拉取失败")
                    .metrics(m)
                    .notes(List.of("symbol=" + symbol, e.getClass().getSimpleName()))
                    .build();
        }
        List<EastmoneyMarginTradingStockRowDTO> raw = page.rows();
        if (raw == null || raw.isEmpty()) {
            return missing(ctx, "接口无融券日频数据");
        }
        List<EastmoneyMarginTradingStockRowDTO> rows = raw.stream()
                .filter(r -> r.rqye() != null && r.rqye() > 0)
                .toList();
        if (rows.size() < MIN_ROWS_FOR_5D_GROWTH) {
            return missing(ctx, "有效融券余额日频序列不足 6 日，无法计算近 5 日增速");
        }
        long rqyeLatest = rows.get(0).rqye();
        long rqye5dAgo = rows.get(5).rqye();
        if (rqye5dAgo <= 0) {
            return missing(ctx, "第 6 个交易日融券余额无效，无法计算增速");
        }
        // 近 5 个交易日融券余额增长比例（%）= (最新日 rqye − 5 交易日前 rqye) / 5 交易日前 rqye × 100
        double growth5dPct = (rqyeLatest - rqye5dAgo) * 100.0 / rqye5dAgo;

        PriceWindowInput pw = resolvePriceWindowInput(ctx);
        Boolean priceAtHigh = inferPricePosition(pw.latestDayChangePct(), pw.closesNewestFirst());

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("rqyeLatest", rqyeLatest);
        extra.put("rqye5TradingDaysAgo", rqye5dAgo);
        extra.put("latestTradeDate", rows.get(0).date());
        extra.put("ref5TradeDate", rows.get(5).date());
        extra.put("rawRowCount", raw.size());
        extra.put("validRqyeRowCount", rows.size());
        extra.put("pricePositionSource", pw.source());
        putPriceWindowDiagnostics(extra, pw.latestDayChangePct(), pw.closesNewestFirst());

        return classify(growth5dPct, priceAtHigh, extra);
    }

    private static PriceWindowInput resolvePriceWindowInput(SentimentContext ctx) {
        KlineBarPort klinePort = ctx.getKlineBarPort();
        if (klinePort == null) {
            return new PriceWindowInput(List.of(), null, "NO_KLINE_PORT");
        }
        if (ctx.getSymbol() == null || ctx.getSymbol().isBlank()) {
            return new PriceWindowInput(List.of(), null, "NO_SYMBOL");
        }
        List<KlineBarDTO> d1 = klinePort.listLatestBarsAsc(
                ctx.getSymbol().trim(), KlineIntervalTypeEnum.D.getCode(), D1_DAILY_BAR_LIMIT);
        if (d1 == null || d1.isEmpty()) {
            return new PriceWindowInput(List.of(), null, "NO_D1_BARS");
        }
        List<BigDecimal> closes = closesNewestFirstFromDailyAsc(d1);
        KlineBarDTO last = d1.get(d1.size() - 1);
        BigDecimal chg = last == null ? null : last.getChangePct();
        return new PriceWindowInput(closes, chg, "D1_KLINE");
    }

    /**
     * {@code listLatestBarsAsc} 为时间升序（最老→最新），转为收盘价新→旧，最多 {@link #PRICE_WINDOW_MAX_DAYS} 根。
     */
    static List<BigDecimal> closesNewestFirstFromDailyAsc(List<KlineBarDTO> asc) {
        List<BigDecimal> out = new ArrayList<>();
        if (asc == null) {
            return out;
        }
        for (int i = asc.size() - 1; i >= 0; i--) {
            KlineBarDTO b = asc.get(i);
            if (b != null && b.getClose() != null && b.getClose().signum() > 0) {
                out.add(b.getClose());
                if (out.size() >= PRICE_WINDOW_MAX_DAYS) {
                    break;
                }
            }
        }
        return out;
    }

    private static FactorResult missing(SentimentContext ctx, String reason) {
        Map<String, Object> m = thresholdMap();
        return FactorResult.builder("D2_SHORT_BALANCE_GROWTH")
                .level(FactorSignalLevel.UNAVAILABLE)
                .summary("近5日融券余额增长比例：" + reason)
                .metrics(m)
                .notes(List.of("symbol=" + ctx.getSymbol()))
                .build();
    }

    private static Map<String, Object> thresholdMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shortBalanceSurgePct", SentimentMdThresholds.D2_SHORT_BALANCE_SURGE_PCT);
        return m;
    }

    private static void putPriceWindowDiagnostics(
            Map<String, Object> extra, BigDecimal latestDayChangePct, List<BigDecimal> closes) {
        extra.put("priceWindowDayCount", closes.size());
        if (latestDayChangePct != null) {
            extra.put("latestDayChangePct", latestDayChangePct);
        }
        if (closes.size() < 5) {
            return;
        }
        BigDecimal lastClose = closes.get(0);
        BigDecimal high = closes.stream().max(BigDecimal::compareTo).orElse(lastClose);
        BigDecimal low = closes.stream().min(BigDecimal::compareTo).orElse(lastClose);
        extra.put("priceWindowHigh", high);
        extra.put("priceWindowLow", low);
        if (high.compareTo(low) <= 0) {
            return;
        }
        BigDecimal range = high.subtract(low);
        BigDecimal fromHighPct = high.subtract(lastClose)
                .divide(high, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        BigDecimal posInRange = lastClose.subtract(low).divide(range, 6, RoundingMode.HALF_UP);
        extra.put("drawdownFromWindowHighPct", fromHighPct);
        extra.put("closePositionInWindowRange", posInRange);
    }

    /**
     * 用日 K 最近 {@link #PRICE_WINDOW_MAX_DAYS} 日<strong>收盘价</strong>（新→旧）及最新一日<strong>涨跌幅</strong>（{@code changePct}）推断「高位 / 暴跌」：
     * <ul>
     *     <li>相对窗口最高价显著回撤或贴近区间下沿 → {@code false}（偏暴跌/深跌）</li>
     *     <li>贴近区间上沿且回撤不大 → {@code true}（偏高位）</li>
     *     <li>其余或波动过窄 → {@code null}</li>
     * </ul>
     */
    static Boolean inferPricePosition(BigDecimal latestDayChangePct, List<BigDecimal> closesNewestFirst) {
        if (latestDayChangePct != null
                && latestDayChangePct.compareTo(CRASH_DAY_CHANGE_PCT_THRESHOLD) <= 0) {
            return Boolean.FALSE;
        }
        if (closesNewestFirst == null || closesNewestFirst.size() < 5) {
            return null;
        }
        BigDecimal lastClose = closesNewestFirst.get(0);
        BigDecimal high = closesNewestFirst.stream().max(BigDecimal::compareTo).orElse(lastClose);
        BigDecimal low = closesNewestFirst.stream().min(BigDecimal::compareTo).orElse(lastClose);
        if (high.signum() <= 0 || high.compareTo(low) <= 0) {
            return null;
        }
        BigDecimal rangeToHigh = high.subtract(low).divide(high, 8, RoundingMode.HALF_UP);
        if (rangeToHigh.compareTo(MIN_RANGE_TO_HIGH_RATIO) < 0) {
            return null;
        }
        BigDecimal range = high.subtract(low);
        BigDecimal fromHighPct = high.subtract(lastClose)
                .divide(high, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        BigDecimal posInRange = lastClose.subtract(low).divide(range, 8, RoundingMode.HALF_UP);

        if (fromHighPct.compareTo(DRAWDOWN_DEEP_PCT) >= 0 || posInRange.compareTo(RANGE_LOW_BAND) <= 0) {
            return Boolean.FALSE;
        }
        if (posInRange.compareTo(RANGE_HIGH_BAND) >= 0
                && fromHighPct.compareTo(HIGH_POSITION_MAX_DRAWDOWN_PCT) < 0) {
            return Boolean.TRUE;
        }
        return null;
    }

    /**
     * @param shortBalanceGrowth5dPct 近 5 个交易日融券余额增长比例（%，相对 5 交易日前余额）
     * @param priceAtHigh             是否处于股价高位；{@code false} 表示偏「已暴跌/深跌」一侧；{@code null} 表示未知
     */
    public static FactorResult classify(double shortBalanceGrowth5dPct, Boolean priceAtHigh) {
        return classify(shortBalanceGrowth5dPct, priceAtHigh, null);
    }

    /**
     * @param extraMetrics 附加诊断字段（写入 metrics）
     */
    public static FactorResult classify(double shortBalanceGrowth5dPct, Boolean priceAtHigh,
                                        Map<String, Object> extraMetrics) {
        Map<String, Object> m = thresholdMap();
        if (extraMetrics != null) {
            m.putAll(extraMetrics);
        }
        m.put("shortBalanceGrowth5dPct", shortBalanceGrowth5dPct);
        m.put("priceAtHigh", priceAtHigh);
        // 文档：仅当增长比例「大于」阈值时进入高位/暴跌两类解读
        if (!(shortBalanceGrowth5dPct > SentimentMdThresholds.D2_SHORT_BALANCE_SURGE_PCT)) {
            return FactorResult.builder("D2_SHORT_BALANCE_GROWTH")
                    .level(FactorSignalLevel.NEUTRAL)
                    .summary("近5日融券余额增长比例未大于 " + SentimentMdThresholds.D2_SHORT_BALANCE_SURGE_PCT
                            + "%，未触发文档警戒档")
                    .metrics(m)
                    .build();
        }
        if (Boolean.TRUE.equals(priceAtHigh)) {
            return FactorResult.builder("D2_SHORT_BALANCE_GROWTH")
                    .level(FactorSignalLevel.BEARISH)
                    .summary("大于 " + SentimentMdThresholds.D2_SHORT_BALANCE_SURGE_PCT
                            + "% 且股价处于高位：有人在大量借券做空，看跌信号极强")
                    .metrics(m)
                    .build();
        }
        if (Boolean.FALSE.equals(priceAtHigh)) {
            return FactorResult.builder("D2_SHORT_BALANCE_GROWTH")
                    .level(FactorSignalLevel.INFO)
                    .summary("大于 " + SentimentMdThresholds.D2_SHORT_BALANCE_SURGE_PCT
                            + "% 且股价已暴跌：可能是跌后对未来进一步分歧加大，也可能是做空力量即将衰竭（物极必反）")
                    .metrics(m)
                    .build();
        }
        return FactorResult.builder("D2_SHORT_BALANCE_GROWTH")
                .level(FactorSignalLevel.WARNING)
                .summary("近5日融券余额增长比例大于 " + SentimentMdThresholds.D2_SHORT_BALANCE_SURGE_PCT
                        + "%，但缺少「高位/已暴跌」位置标签，无法区分文档两类情形")
                .metrics(m)
                .build();
    }
}
