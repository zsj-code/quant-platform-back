package com.quant.platform.ai.core.factor.fundamental.watchlist;

import com.quant.platform.common.dto.FinancialStatementDTO;

import com.quant.platform.ai.core.factor.fundamental.*;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * W2 研发过度资本化：
 * - 研发支出资本化金额 / 当期研发总支出 > 0.50
 * - 连续两个会计年度均超阈值时触发
 *
 * 数据缺口/不确定性：
 * - 东财财报 raw_json 中“研发费用/研发支出/资本化金额”的字段命名可能随报告口径变化；
 * - 若无法命中字段，则返回 UNAVAILABLE（不强行估算）。
 */
public class RndOverCapitalizationWatchlistFactor implements FundamentalFactor {
    private static final BigDecimal DEFAULT_TH = new BigDecimal("0.50");

    @Override
    public String factorKey() {
        return "fund.watch.rnd_capitalization_ratio";
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

        List<FinancialStatementDTO> years = pickLatestTwoYearReports(income);
        if (years.size() < 2) {
            return FundamentalResult.unavailable(factorKey(), group(), "财年数据不足两期（需要最近两年年报口径）");
        }

        List<BigDecimal> ratios = new ArrayList<>();
        for (FinancialStatementDTO e : years) {
            Map<String, Object> m = FundamentalJsonExtractors.parse(e.getRawJson());
            BigDecimal totalRnd = FundamentalJsonExtractors.pickDecimal(m,
                    "RD_EXPENSE", "R_AND_D_EXPENSE", "RESEARCH_EXPENSE", "RND_TOTAL");
            BigDecimal capRnd = FundamentalJsonExtractors.pickDecimal(m,
                    "RD_CAPITALIZED", "RND_CAPITALIZED", "CAPITALIZED_RD");

            if (totalRnd == null || capRnd == null || totalRnd.compareTo(BigDecimal.ZERO) <= 0) {
                return FundamentalResult.unavailable(factorKey(), group(), "raw_json字段缺失：无法提取研发总支出/研发资本化金额");
            }
            ratios.add(capRnd.divide(totalRnd, 6, BigDecimal.ROUND_HALF_UP));
        }

        boolean hit = ratios.get(0).compareTo(DEFAULT_TH) > 0 && ratios.get(1).compareTo(DEFAULT_TH) > 0;
        String summary = hit ? "连续两财年研发资本化率>0.50：标记观察" : "未触发研发过度资本化(连续两年)";

        return FundamentalResult.builder(factorKey(), group())
                .decision(hit ? FundamentalDecision.WATCH : FundamentalDecision.PASS)
                .summary(summary)
                .metric("ratio_year1", ratios.get(0))
                .metric("ratio_year2", ratios.get(1))
                .metric("threshold", DEFAULT_TH)
                .metric("hit", hit)
                .notes(List.of("字段名为候选key；若命中失败需要根据东财 raw_json 实际字段补充映射"))
                .build();
    }

    private static List<FinancialStatementDTO> pickLatestTwoYearReports(List<FinancialStatementDTO> rowsDesc) {
        List<FinancialStatementDTO> year = new ArrayList<>();
        for (FinancialStatementDTO e : rowsDesc) {
            if (e == null || e.getReportDate() == null) {
                continue;
            }
            if (e.getReportDate().getMonthValue() == 12 && e.getReportDate().getDayOfMonth() == 31) {
                year.add(e);
            }
            if (year.size() >= 2) {
                break;
            }
        }
        if (year.size() >= 2) {
            return year;
        }
        List<FinancialStatementDTO> out = new ArrayList<>();
        for (FinancialStatementDTO e : rowsDesc) {
            if (e == null) {
                continue;
            }
            out.add(e);
            if (out.size() >= 2) {
                break;
            }
        }
        return out;
    }
}

