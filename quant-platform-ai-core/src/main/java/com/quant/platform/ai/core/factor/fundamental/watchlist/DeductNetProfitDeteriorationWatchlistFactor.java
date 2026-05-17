package com.quant.platform.ai.core.factor.fundamental.watchlist;

import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalDecision;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalJsonExtractors;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;
import com.quant.platform.common.dto.FinancialStatementDTO;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * W3 扣非净利润趋势恶化（观察池）：
 * <ul>
 *   <li>从本地 {@code financial_statement} 利润表 {@code raw_json} 读取 {@code DEDUCT_PARENT_NETPROFIT}</li>
 *   <li>取最近三个完整财年，扣非净利润整体趋势恶化则标记观察（内部按复合增速判定，对外不输出 CAGR）</li>
 * </ul>
 * 行业分位比较（申万二级后 20%）暂未实现。
 */
public class DeductNetProfitDeteriorationWatchlistFactor implements FundamentalFactor {
    private static final int REQUIRED_YEAR_REPORTS = 3;
    private static final MathContext CAGR_MATH = new MathContext(12, RoundingMode.HALF_UP);

    @Override
    public String factorKey() {
        return "fund.watch.deduct_net_profit_deterioration";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.WATCHLIST;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        List<FinancialStatementDTO> income = ctx.getStatementsByReportType().getOrDefault(
                EastmoneyFinancialStatementReportTypeEnum.INCOME.getReportName(), List.of());
        if (income.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少利润表数据（financial_statement.raw_json）");
        }

        List<FinancialStatementDTO> years = pickLatestYearReports(income, REQUIRED_YEAR_REPORTS);
        if (years.size() < REQUIRED_YEAR_REPORTS) {
            return FundamentalResult.unavailable(factorKey(), group(),
                    "财年数据不足三期（需要最近三年年报口径的扣非净利润）");
        }

        List<BigDecimal> deductSeries = new ArrayList<>(REQUIRED_YEAR_REPORTS);
        List<String> reportDates = new ArrayList<>(REQUIRED_YEAR_REPORTS);
        for (FinancialStatementDTO e : years) {
            Map<String, Object> m = FundamentalJsonExtractors.parse(e.getRawJson());
            BigDecimal deduct = FundamentalJsonExtractors.pickDecimal(m, "DEDUCT_PARENT_NETPROFIT");
            if (deduct == null) {
                return FundamentalResult.unavailable(factorKey(), group(),
                        "raw_json字段缺失：无法提取 DEDUCT_PARENT_NETPROFIT（扣非净利润）");
            }
            deductSeries.add(deduct);
            reportDates.add(e.getReportDate() != null ? e.getReportDate().toString() : "unknown");
        }

        // years 为 report_date 倒序：index0 最新，index2 最早
        BigDecimal newest = deductSeries.get(0);
        BigDecimal oldest = deductSeries.get(REQUIRED_YEAR_REPORTS - 1);
        int spanYears = REQUIRED_YEAR_REPORTS - 1;

        BigDecimal cagr = compoundAnnualGrowthRate(newest, oldest, spanYears);
        if (cagr == null) {
            return FundamentalResult.unavailable(factorKey(), group(),
                    "无法判定近三年扣非净利润趋势（基期非正且趋势无法判定）");
        }

        boolean hit = cagr.compareTo(BigDecimal.ZERO) < 0;
        String summary = hit
                ? "近三年扣非净利润趋势恶化：标记观察"
                : "近三年扣非净利润趋势未恶化：未触发观察";

        return FundamentalResult.builder(factorKey(), group())
                .decision(hit ? FundamentalDecision.WATCH : FundamentalDecision.PASS)
                .summary(summary)
                .metric("deduct_parent_netprofit_latest", newest)
                .metric("deduct_parent_netprofit_oldest_3y", oldest)
                .metric("deduct_parent_netprofit_mid", deductSeries.get(1))
                .metric("three_year_trend_deteriorating", hit)
                .metric("report_dates_desc", reportDates)
                .metric("hit", hit)
                .notes(List.of(
                        "字段：DEDUCT_PARENT_NETPROFIT；财年优先取 12-31 年报",
                        "行业增速分位比较暂未实现"))
                .build();
    }

    /**
     * CAGR = (期末/期初)^(1/年数) - 1。期初 &lt;= 0 时：期末 &lt; 期初 视为负增长（恶化），否则无法计算。
     */
    static BigDecimal compoundAnnualGrowthRate(BigDecimal end, BigDecimal begin, int years) {
        if (end == null || begin == null || years <= 0) {
            return null;
        }
        if (begin.compareTo(BigDecimal.ZERO) <= 0) {
            if (end.compareTo(begin) < 0) {
                return BigDecimal.ONE.negate();
            }
            return null;
        }
        BigDecimal ratio = end.divide(begin, CAGR_MATH);
        double root = Math.pow(ratio.doubleValue(), 1.0 / years);
        return BigDecimal.valueOf(root - 1.0).setScale(6, RoundingMode.HALF_UP);
    }

    private static List<FinancialStatementDTO> pickLatestYearReports(List<FinancialStatementDTO> rowsDesc, int n) {
        List<FinancialStatementDTO> year = new ArrayList<>();
        for (FinancialStatementDTO e : rowsDesc) {
            if (e == null || e.getReportDate() == null) {
                continue;
            }
            if (e.getReportDate().getMonthValue() == 12 && e.getReportDate().getDayOfMonth() == 31) {
                year.add(e);
            }
            if (year.size() >= n) {
                break;
            }
        }
        if (year.size() >= n) {
            return year;
        }
        List<FinancialStatementDTO> out = new ArrayList<>();
        for (FinancialStatementDTO e : rowsDesc) {
            if (e == null) {
                continue;
            }
            out.add(e);
            if (out.size() >= n) {
                break;
            }
        }
        return out;
    }
}
