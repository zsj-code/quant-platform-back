package com.quant.platform.ai.core.factor.fundamental.hardfilter;

import com.quant.platform.common.dto.FinancialStatementDTO;

import com.quant.platform.ai.core.factor.fundamental.*;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * H5 商誉堰塞湖：商誉 / 归母净资产 > 0.5 → 踢出（取最近一期定期报告）。
 *
 * 说明：
 * - 依赖资产负债表 raw_json 字段名；若未命中则返回 UNAVAILABLE。
 * - 行业修正阈值（如医药/软件/地产）后续通过“行业修正配置表”接入；此处先按通用默认 0.5。
 */
public class GoodwillDamHardFilterFactor implements FundamentalFactor {
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.50");

    @Override
    public String factorKey() {
        return "fund.hard.goodwill_to_parent_equity";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.HARD_FILTER;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        List<FinancialStatementDTO> balance = ctx.getStatementsByReportType().getOrDefault(
                EastmoneyFinancialStatementReportTypeEnum.BALANCE.getReportName(), List.of());
        if (balance.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少资产负债表数据（financial_statement.raw_json）");
        }

        FinancialStatementDTO latest = balance.get(0); // 预期已按 report_date desc
        Map<String, Object> m = FundamentalJsonExtractors.parse(latest.getRawJson());

        BigDecimal goodwill = FundamentalJsonExtractors.pickDecimal(m, "GOODWILL");
        BigDecimal parentEquity = FundamentalJsonExtractors.pickDecimal(m, "TOTAL_EQUITY");

        if (goodwill == null || parentEquity == null || parentEquity.compareTo(BigDecimal.ZERO) == 0) {
            return FundamentalResult.unavailable(factorKey(), group(), "raw_json字段缺失：无法提取商誉或归母净资产");
        }

        BigDecimal ratio = goodwill.divide(parentEquity.abs(), 6, BigDecimal.ROUND_HALF_UP);
        boolean hit = ratio.compareTo(DEFAULT_THRESHOLD) > 0;

        return FundamentalResult.builder(factorKey(), group())
                .decision(hit ? FundamentalDecision.HARD_EXCLUDE : FundamentalDecision.PASS)
                .summary(hit ? "商誉/归母净资产>0.5：商誉堰塞湖，踢出" : "未触发商誉堰塞湖阈值")
                .metric("report_date", latest.getReportDate())
                .metric("goodwill", goodwill)
                .metric("parent_equity", parentEquity)
                .metric("ratio", ratio)
                .metric("threshold", DEFAULT_THRESHOLD)
                .metric("hit", hit)
                .build();
    }
}

