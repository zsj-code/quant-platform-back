package com.quant.platform.ai.core.factor.fundamental.hardfilter;

import com.quant.platform.common.dto.FinancialStatementDTO;

import com.quant.platform.ai.core.factor.fundamental.*;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * H3 现金流死亡背离：
 * - 两个财年累计：扣非净利润之和 > 0 且 经营现金流净额之和 < 0 → 踢出
 *
 * 说明：
 * - 依赖东财财报 raw_json 的字段名；这里提供候选 key 列表，若无法命中则返回 UNAVAILABLE。
 */
public class CashflowDeathDivergenceHardFilterFactor implements FundamentalFactor {
    @Override
    public String factorKey() {
        return "fund.hard.cashflow_death_divergence_2y";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.HARD_FILTER;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        List<FinancialStatementDTO> income = ctx.getStatementsByReportType().getOrDefault(
                EastmoneyFinancialStatementReportTypeEnum.INCOME.getReportName(), List.of());
        List<FinancialStatementDTO> cash = ctx.getStatementsByReportType().getOrDefault(
                EastmoneyFinancialStatementReportTypeEnum.CASHFLOW.getReportName(), List.of());

        if (income.isEmpty() || cash.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少利润表/现金流量表数据（financial_statement.raw_json）");
        }

        // 取最近两个“完整财年”优先：一般是 12-31；若没有，则取最近两条记录作为近似
        List<FinancialStatementDTO> incomeYears = pickLatestTwoYearReports(income);
        List<FinancialStatementDTO> cashYears = pickLatestTwoYearReports(cash);
        if (incomeYears.size() < 2 || cashYears.size() < 2) {
            return FundamentalResult.unavailable(factorKey(), group(), "财年数据不足两期（需要最近两年年报口径）");
        }

        BigDecimal dedNpSum = BigDecimal.ZERO;
        BigDecimal ocfSum = BigDecimal.ZERO;
        List<String> notes = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            FinancialStatementDTO inc = incomeYears.get(i);
            FinancialStatementDTO cf = cashYears.get(i);
            Map<String, Object> incMap = FundamentalJsonExtractors.parse(inc.getRawJson());
            Map<String, Object> cfMap = FundamentalJsonExtractors.parse(cf.getRawJson());

            BigDecimal dedNp = FundamentalJsonExtractors.pickDecimal(incMap, "DEDUCT_PARENT_NETPROFIT");
            BigDecimal ocf = FundamentalJsonExtractors.pickDecimal(cfMap, "NETCASH_OPERATE");

            if (dedNp == null || ocf == null) {
                return FundamentalResult.unavailable(factorKey(), group(), "raw_json字段缺失：无法提取扣非净利润或经营现金流净额");
            }

            dedNpSum = dedNpSum.add(dedNp);
            ocfSum = ocfSum.add(ocf);
        }

        boolean hit = dedNpSum.compareTo(BigDecimal.ZERO) > 0 && ocfSum.compareTo(BigDecimal.ZERO) < 0;
        FundamentalDecision decision = hit ? FundamentalDecision.HARD_EXCLUDE : FundamentalDecision.PASS;
        String summary = hit ? "两财年累计扣非为正但经营现金流为负：现金流死亡背离，踢出"
                : "未触发现金流死亡背离(两财年累计)";

        return FundamentalResult.builder(factorKey(), group())
                .decision(decision)
                .summary(summary)
                .metric("deduct_net_profit_sum_2y", dedNpSum)
                .metric("ocf_sum_2y", ocfSum)
                .metric("hit", hit)
                .notes(notes)
                .build();
    }

    private static List<FinancialStatementDTO> pickLatestTwoYearReports(List<FinancialStatementDTO> rowsDesc) {
        // rowsDesc 通常是 report_date 倒序（现有 admin list 默认倒序），这里做稳健处理：直接按原顺序筛出 12-31
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
        // fallback：取前两条
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

