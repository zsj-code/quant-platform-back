package com.quant.platform.ai.core.factor.fundamental.watchlist;

import com.quant.platform.ai.core.client.EastmoneyF10GIncomeClient;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomeRowDTO;
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
import java.util.List;

/**
 * W4 大额异常减值（东财 F10 利润表 {@code RPT_F10_FINANCE_GINCOME}）：
 * <ul>
 *     <li>最近一个财年：{@code ASSET_IMPAIRMENT_INCOME}（资产减值损失，新）/ {@code OPERATE_PROFIT} &gt; 0.50</li>
 *     <li>营业利润 ≤ 0 时：{@code |ASSET_IMPAIRMENT_INCOME|} &gt; {@code |NETPROFIT|} × 0.50 也触发</li>
 * </ul>
 */
public class LargeImpairmentWatchlistFactor implements FundamentalFactor {

    static final BigDecimal RATIO_THRESHOLD = new BigDecimal("0.50");
    private static final int FETCH_PAGE_SIZE = 6;
    private static final DateTimeFormatter REPORT_DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EastmoneyF10GIncomeClient incomeClient;

    public LargeImpairmentWatchlistFactor(@Nullable EastmoneyF10GIncomeClient incomeClient) {
        this.incomeClient = incomeClient;
    }

    @Override
    public String factorKey() {
        return "fund.watch.large_impairment";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.WATCHLIST;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        if (incomeClient == null) {
            return FundamentalResult.unavailable(factorKey(), group(),
                    "未配置 EastmoneyF10GIncomeClient，无法查询 F10 利润表");
        }
        String code = resolveStockCode(ctx);
        if (code == null || code.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少证券代码");
        }

        EastmoneyF10GIncomeRowDTO latest;
        try {
            latest = loadLatestYearIncomeRow(code.trim());
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "拉取 F10 利润表失败：" + e.getMessage());
        }
        if (latest == null) {
            return FundamentalResult.unavailable(factorKey(), group(), "无可用利润表数据（最近财年）");
        }

        return buildResult(latest);
    }

    static FundamentalResult buildResult(EastmoneyF10GIncomeRowDTO row) {
        BigDecimal impairmentRaw = resolveAssetImpairment(row);
        BigDecimal operateProfit = row.operateProfit();
        BigDecimal netProfit = row.netprofit();

        if (impairmentRaw == null) {
            return FundamentalResult.unavailable("fund.watch.large_impairment", FundamentalFactorGroup.WATCHLIST,
                    "利润表缺少 ASSET_IMPAIRMENT_INCOME（资产减值损失，新）");
        }
        BigDecimal impairmentAbs = impairmentRaw.abs();

        boolean hit;
        BigDecimal ratioToOperateProfit = null;
        BigDecimal netProfitThreshold = null;
        String rule;

        if (operateProfit != null && operateProfit.compareTo(BigDecimal.ZERO) > 0) {
            ratioToOperateProfit = impairmentAbs.divide(operateProfit, 6, RoundingMode.HALF_UP);
            hit = ratioToOperateProfit.compareTo(RATIO_THRESHOLD) > 0;
            rule = "impairment_to_operate_profit";
        } else {
            if (netProfit == null) {
                return FundamentalResult.unavailable("fund.watch.large_impairment", FundamentalFactorGroup.WATCHLIST,
                        "营业利润非正且缺少 NETPROFIT（净利润）");
            }
            netProfitThreshold = netProfit.abs().multiply(RATIO_THRESHOLD);
            hit = impairmentAbs.compareTo(netProfitThreshold) > 0;
            rule = "impairment_to_abs_net_profit";
        }

        String summary = hit
                ? "最近财年资产减值损失相对盈利指标超阈值，标记观察"
                : "最近财年未触发大额异常减值";

        return FundamentalResult.builder("fund.watch.large_impairment", FundamentalFactorGroup.WATCHLIST)
                .decision(hit ? FundamentalDecision.WATCH : FundamentalDecision.PASS)
                .summary(summary)
                .metric("report_date", row.reportDate())
                .metric("report_type", row.reportType())
                .metric("asset_impairment_income", impairmentRaw)
                .metric("asset_impairment_income_abs", impairmentAbs)
                .metric("operate_profit", operateProfit)
                .metric("netprofit", netProfit)
                .metric("impairment_to_operate_profit_ratio", ratioToOperateProfit)
                .metric("net_profit_half_threshold", netProfitThreshold)
                .metric("threshold", RATIO_THRESHOLD)
                .metric("rule_applied", rule)
                .metric("hit", hit)
                .notes(List.of("数据源：Eastmoney F10 GINCOME；减值优先 ASSET_IMPAIRMENT_INCOME（新），缺失时回退 ASSET_IMPAIRMENT_LOSS"))
                .build();
    }

    /**
     * 资产减值损失：优先新科目 {@code ASSET_IMPAIRMENT_INCOME}，历史报表回退 {@code ASSET_IMPAIRMENT_LOSS}。
     */
    static BigDecimal resolveAssetImpairment(EastmoneyF10GIncomeRowDTO row) {
        if (row == null) {
            return null;
        }
        if (row.assetImpairmentIncome() != null) {
            return row.assetImpairmentIncome();
        }
        return row.assetImpairmentLoss();
    }

    private EastmoneyF10GIncomeRowDTO loadLatestYearIncomeRow(String code) {
        EastmoneyF10GIncomePageDTO page = incomeClient.fetchF10GIncome(code, List.of(), 1, FETCH_PAGE_SIZE);
        List<EastmoneyF10GIncomeRowDTO> rows = page == null ? null : page.rows();
        return pickLatestYearReport(rows);
    }

    static EastmoneyF10GIncomeRowDTO pickLatestYearReport(List<EastmoneyF10GIncomeRowDTO> rowsDesc) {
        if (rowsDesc == null || rowsDesc.isEmpty()) {
            return null;
        }
        for (EastmoneyF10GIncomeRowDTO row : rowsDesc) {
            if (row == null) {
                continue;
            }
            LocalDate d = parseReportDate(row.reportDate());
            if (d != null && d.getMonthValue() == 12 && d.getDayOfMonth() == 31) {
                return row;
            }
        }
        return rowsDesc.stream().filter(r -> r != null).findFirst().orElse(null);
    }

    static LocalDate parseReportDate(String reportDate) {
        if (reportDate == null || reportDate.isBlank()) {
            return null;
        }
        String s = reportDate.trim();
        try {
            if (s.length() >= 19) {
                return LocalDateTime.parse(s.substring(0, 19), REPORT_DATE_TIME_FMT).toLocalDate();
            }
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private static String resolveStockCode(FundamentalContext ctx) {
        if (ctx.getSymbol() != null && !ctx.getSymbol().isBlank()) {
            return ctx.getSymbol().trim();
        }
        if (ctx.getSecCode() != null && !ctx.getSecCode().isBlank()) {
            return ctx.getSecCode().trim();
        }
        return null;
    }
}
