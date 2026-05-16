package com.quant.platform.ai.core.factor.fundamental.hardfilter;

import com.quant.platform.ai.core.client.EastmoneyPledgeRatioClient;
import com.quant.platform.ai.core.client.dto.EastmoneyPledgeRatioLatestDTO;
import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalDecision;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.common.dto.KlineBarDTO;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * H4 高危股权质押（与注释口径一致）：
 * <ul>
 *   <li>质押比例（东财 {@code RPT_CSDC_LIST} 最新 {@code PLEDGE_RATIO}）&gt; 70%</li>
 *   <li>当前股价（最新日 K 收盘价）&lt; 近两年（自最新一根日 K 向前约 2 年）内最高收盘价 × 0.5</li>
 * </ul>
 * 两条<strong>同时</strong>满足 → {@link FundamentalDecision#HARD_EXCLUDE}，否则 PASS。
 */
public class HighRiskPledgeHardFilterFactor implements FundamentalFactor {

    /** 累计质押比例阈值（%） */
    private static final BigDecimal PLEDGE_RATIO_PCT_THRESHOLD = new BigDecimal("70");

    /** 相对近两年最高收盘的折价阈值 */
    private static final BigDecimal MAX_CLOSE_HALF = new BigDecimal("0.5");

    private final EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient;
    private final KlineBarPort klineBarPort;

    public HighRiskPledgeHardFilterFactor(@Nullable EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient,
                                          @Nullable KlineBarPort klineBarPort) {
        this.eastmoneyPledgeRatioClient = eastmoneyPledgeRatioClient;
        this.klineBarPort = klineBarPort;
    }

    @Override
    public String factorKey() {
        return "fund.hard.high_risk_pledge";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.HARD_FILTER;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        if (eastmoneyPledgeRatioClient == null || klineBarPort == null) {
            return FundamentalResult.unavailable(factorKey(), group(),
                    "未配置 EastmoneyPledgeRatioClient 或 KlineBarPort，无法计算高危质押");
        }

        String secCode = ctx.getSecCode();
        String symbol = resolveSymbol(ctx);
        if (secCode == null || secCode.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少证券代码，无法查询质押比例");
        }
        if (symbol == null || symbol.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少 symbol，无法查询日 K");
        }

        final Optional<EastmoneyPledgeRatioLatestDTO> pledgeOpt;
        try {
            pledgeOpt = eastmoneyPledgeRatioClient.fetchLatestPledgeRatio(secCode.trim());
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "拉取质押比例失败：" + e.getMessage());
        }
        if (pledgeOpt.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "无质押比例数据（东财 RPT_CSDC_LIST）");
        }
        EastmoneyPledgeRatioLatestDTO pledge = pledgeOpt.get();
        BigDecimal pledgeRatio = pledge.pledgeRatio();
        if (pledgeRatio == null) {
            return FundamentalResult.unavailable(factorKey(), group(), "质押比例字段为空");
        }

        List<KlineBarDTO> d1;
        try {
            d1 = loadDailyBarsApproxTwoYears(symbol.trim());
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "拉取日 K 失败：" + e.getMessage());
        }
        if (d1 == null || d1.size() < 2) {
            return FundamentalResult.unavailable(factorKey(), group(), "日 K 数据不足，无法计算近两年最高收盘与现价");
        }

        BigDecimal maxClose2y = maxClose(d1);
        KlineBarDTO last = d1.get(d1.size() - 1);
        BigDecimal lastClose = last.getClose();
        if (maxClose2y == null || lastClose == null || maxClose2y.compareTo(BigDecimal.ZERO) <= 0) {
            return FundamentalResult.unavailable(factorKey(), group(), "日 K 收盘价缺失，无法比较折价");
        }

        BigDecimal halfOfMax = maxClose2y.multiply(MAX_CLOSE_HALF);
        boolean highPledge = pledgeRatio.compareTo(PLEDGE_RATIO_PCT_THRESHOLD) > 0;
        boolean priceCrash = lastClose.compareTo(halfOfMax) < 0;
        boolean hit = highPledge && priceCrash;

        String summary = hit
                ? "高质押(>" + PLEDGE_RATIO_PCT_THRESHOLD + "%)且现价低于近两年最高收盘×0.5，踢出"
                : "未同时满足高质押与深度折价条件";

        return FundamentalResult.builder(factorKey(), group())
                .decision(hit ? FundamentalDecision.HARD_EXCLUDE : FundamentalDecision.PASS)
                .summary(summary)
                .metric("pledge_ratio_pct", pledgeRatio)
                .metric("pledge_trade_date", pledge.tradeDate() != null ? pledge.tradeDate().toString() : null)
                .metric("max_close_2y", maxClose2y)
                .metric("last_close", lastClose)
                .metric("half_max_close", halfOfMax)
                .metric("high_pledge_over_threshold", highPledge)
                .metric("price_below_half_max_close", priceCrash)
                .metric("d1_bar_count", d1.size())
                .metric("hit", hit)
                .build();
    }

    private static String resolveSymbol(FundamentalContext ctx) {
        String s = ctx.getSymbol();
        if (s != null && !s.isBlank()) {
            return s.trim();
        }
        if (ctx.getSecCode() != null && !ctx.getSecCode().isBlank()) {
            return ctx.getSecCode().trim();
        }
        return null;
    }

    private List<KlineBarDTO> loadDailyBarsApproxTwoYears(String symbol) {
        String interval = KlineIntervalTypeEnum.D.getCode();
        LocalDateTime latest = klineBarPort.findLatestBarTime(symbol, interval);
        if (latest == null) {
            return List.of();
        }
        LocalDateTime start = latest.minusYears(2);
        return klineBarPort.listBarsAscBetween(symbol, interval, start, latest);
    }

    private static BigDecimal maxClose(List<KlineBarDTO> asc) {
        BigDecimal max = null;
        for (KlineBarDTO b : asc) {
            BigDecimal c = b.getClose();
            if (c == null) {
                continue;
            }
            if (max == null || c.compareTo(max) > 0) {
                max = c;
            }
        }
        return max;
    }
}
