package com.quant.platform.ai.core.factor.fundamental.watchlist;

import com.quant.platform.ai.core.client.EastmoneyShareHolderIncreaseClient;
import com.quant.platform.ai.core.client.dto.EastmoneyShareHolderIncreasePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyShareHolderIncreaseRowDTO;
import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalDecision;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * W5 股东减持密集度（东财 {@code RPT_SHARE_HOLDER_INCREASE}）：
 * <ul>
 *     <li>近 {@value #PLAN_LOOKBACK_DAYS} 个自然日内，{@code DIRECTION=减持} 记录的占总股本变动比例累计 &gt; {@value #PLAN_TOTAL_PCT_THRESHOLD}%</li>
 *     <li>或近 {@value #ACTUAL_LOOKBACK_DAYS} 个自然日内，减持比例累计 &gt; {@value #ACTUAL_TOTAL_PCT_THRESHOLD}%</li>
 * </ul>
 * 比例优先取 {@code CHANGE_RATE}（%），缺失时用 {@code CHANGE_FREE_RATIO}；日期优先 {@code END_DATE}，其次 {@code NOTICE_DATE}。
 * <p>
 * 说明：接口为已披露增减持变动，无法单独区分「预披露计划」与「实施完成」，三口径以公告变动近似。
 */
public class ShareholderReductionWatchlistFactor implements FundamentalFactor {

    static final int PLAN_LOOKBACK_DAYS = 90;
    static final int ACTUAL_LOOKBACK_DAYS = 30;
    static final BigDecimal PLAN_TOTAL_PCT_THRESHOLD = new BigDecimal("2");
    static final BigDecimal ACTUAL_TOTAL_PCT_THRESHOLD = new BigDecimal("1");

    private static final int MAX_PAGES = 30;
    private static final DateTimeFormatter TRADE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DIRECTION_REDUCTION = "减持";

    private final EastmoneyShareHolderIncreaseClient shareHolderIncreaseClient;

    public ShareholderReductionWatchlistFactor(
            @Nullable EastmoneyShareHolderIncreaseClient shareHolderIncreaseClient) {
        this.shareHolderIncreaseClient = shareHolderIncreaseClient;
    }

    @Override
    public String factorKey() {
        return "fund.watch.shareholder_reduction_density";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.WATCHLIST;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        if (shareHolderIncreaseClient == null) {
            return FundamentalResult.unavailable(factorKey(), group(),
                    "未配置 EastmoneyShareHolderIncreaseClient，无法查询股东增减持");
        }
        String code = resolveStockCode(ctx);
        if (code == null || code.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少证券代码");
        }

        List<EastmoneyShareHolderIncreaseRowDTO> rows;
        try {
            rows = loadReductionRows(code.trim());
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "拉取股东增减持失败：" + e.getMessage());
        }

        LocalDate today = LocalDate.now();
        ReductionWindowStats planWindow = aggregateWindow(rows, today.minusDays(PLAN_LOOKBACK_DAYS));
        ReductionWindowStats actualWindow = aggregateWindow(rows, today.minusDays(ACTUAL_LOOKBACK_DAYS));

        return buildResult(planWindow, actualWindow);
    }

    static FundamentalResult buildResult(ReductionWindowStats planWindow, ReductionWindowStats actualWindow) {
        boolean hitPlan = planWindow.sumPct().compareTo(PLAN_TOTAL_PCT_THRESHOLD) > 0
                || planWindow.maxSinglePct().compareTo(PLAN_TOTAL_PCT_THRESHOLD) > 0;
        boolean hitActual = actualWindow.sumPct().compareTo(ACTUAL_TOTAL_PCT_THRESHOLD) > 0
                || actualWindow.maxSinglePct().compareTo(ACTUAL_TOTAL_PCT_THRESHOLD) > 0;
        boolean hit = hitPlan || hitActual;

        String summary;
        if (hitPlan && hitActual) {
            summary = "近90日与近30日减持比例均超阈值，标记观察";
        } else if (hitPlan) {
            summary = "近90日减持累计或单笔超" + PLAN_TOTAL_PCT_THRESHOLD + "%总股本，标记观察";
        } else if (hitActual) {
            summary = "近30日减持累计或单笔超" + ACTUAL_TOTAL_PCT_THRESHOLD + "%总股本，标记观察";
        } else {
            summary = "股东减持密集度未超观察阈值";
        }

        return FundamentalResult.builder("fund.watch.shareholder_reduction_density", FundamentalFactorGroup.WATCHLIST)
                .decision(hit ? FundamentalDecision.WATCH : FundamentalDecision.PASS)
                .summary(summary)
                .metric("plan_lookback_days", PLAN_LOOKBACK_DAYS)
                .metric("plan_threshold_pct", PLAN_TOTAL_PCT_THRESHOLD)
                .metric("plan_reduction_sum_pct", planWindow.sumPct())
                .metric("plan_reduction_max_single_pct", planWindow.maxSinglePct())
                .metric("plan_reduction_event_count", planWindow.eventCount())
                .metric("actual_lookback_days", ACTUAL_LOOKBACK_DAYS)
                .metric("actual_threshold_pct", ACTUAL_TOTAL_PCT_THRESHOLD)
                .metric("actual_reduction_sum_pct", actualWindow.sumPct())
                .metric("actual_reduction_max_single_pct", actualWindow.maxSinglePct())
                .metric("actual_reduction_event_count", actualWindow.eventCount())
                .metric("hit_plan_window", hitPlan)
                .metric("hit_actual_window", hitActual)
                .metric("hit", hit)
                .metric("data_note", "东财RPT_SHARE_HOLDER_INCREASE已披露减持变动，非减持计划专表")
                .build();
    }

    private List<EastmoneyShareHolderIncreaseRowDTO> loadReductionRows(String code) {
        List<EastmoneyShareHolderIncreaseRowDTO> all = new ArrayList<>();
        LocalDate oldestNeeded = LocalDate.now().minusDays(PLAN_LOOKBACK_DAYS);
        for (int page = 1; page <= MAX_PAGES; page++) {
            EastmoneyShareHolderIncreasePageDTO batch =
                    shareHolderIncreaseClient.fetchShareHolderIncrease(code, page,
                            EastmoneyShareHolderIncreaseClient.DEFAULT_PAGE_SIZE);
            List<EastmoneyShareHolderIncreaseRowDTO> rows = batch == null ? null : batch.rows();
            if (rows == null || rows.isEmpty()) {
                break;
            }
            LocalDate oldestOnPage = null;
            for (EastmoneyShareHolderIncreaseRowDTO row : rows) {
                if (row == null || !isReduction(row)) {
                    continue;
                }
                all.add(row);
                LocalDate d = eventDate(row);
                if (d != null && (oldestOnPage == null || d.isBefore(oldestOnPage))) {
                    oldestOnPage = d;
                }
            }
            if (oldestOnPage != null && oldestOnPage.isBefore(oldestNeeded)) {
                break;
            }
            Integer totalPages = batch.totalPages();
            if (totalPages != null && page >= totalPages) {
                break;
            }
        }
        return all;
    }

    static ReductionWindowStats aggregateWindow(List<EastmoneyShareHolderIncreaseRowDTO> rows, LocalDate windowStartInclusive) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal maxSingle = BigDecimal.ZERO;
        int count = 0;
        for (EastmoneyShareHolderIncreaseRowDTO row : rows) {
            if (row == null || !isReduction(row)) {
                continue;
            }
            LocalDate d = eventDate(row);
            if (d == null || d.isBefore(windowStartInclusive)) {
                continue;
            }
            BigDecimal pct = reductionPct(row);
            if (pct == null) {
                continue;
            }
            sum = sum.add(pct);
            if (pct.compareTo(maxSingle) > 0) {
                maxSingle = pct;
            }
            count++;
        }
        return new ReductionWindowStats(sum.setScale(4, RoundingMode.HALF_UP), maxSingle, count);
    }

    static boolean isReduction(EastmoneyShareHolderIncreaseRowDTO row) {
        return row.direction() != null && DIRECTION_REDUCTION.equals(row.direction().trim());
    }

    static BigDecimal reductionPct(EastmoneyShareHolderIncreaseRowDTO row) {
        BigDecimal rate = row.changeRate();
        if (rate != null) {
            return rate.abs();
        }
        BigDecimal free = row.changeFreeRatio();
        return free == null ? null : free.abs();
    }

    static LocalDate eventDate(EastmoneyShareHolderIncreaseRowDTO row) {
        LocalDate end = parseTradeDate(row.endDate());
        if (end != null) {
            return end;
        }
        return parseTradeDate(row.noticeDate());
    }

    static LocalDate parseTradeDate(String tradeDate) {
        if (tradeDate == null || tradeDate.isBlank()) {
            return null;
        }
        String s = tradeDate.trim();
        try {
            if (s.length() >= 19) {
                return LocalDateTime.parse(s.substring(0, 19), TRADE_DATE_FMT).toLocalDate();
            }
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveStockCode(FundamentalContext ctx) {
        if (ctx.getSecCode() != null && !ctx.getSecCode().isBlank()) {
            return ctx.getSecCode().trim();
        }
        if (ctx.getSymbol() != null && !ctx.getSymbol().isBlank()) {
            return ctx.getSymbol().trim();
        }
        return null;
    }

    record ReductionWindowStats(BigDecimal sumPct, BigDecimal maxSinglePct, int eventCount) {
    }
}
