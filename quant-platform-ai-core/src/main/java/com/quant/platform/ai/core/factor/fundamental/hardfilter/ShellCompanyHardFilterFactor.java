package com.quant.platform.ai.core.factor.fundamental.hardfilter;

import com.quant.platform.common.dto.FinancialStatementDTO;
import com.quant.platform.common.dto.StockValuationSnapshotDTO;

import com.quant.platform.ai.core.factor.fundamental.*;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * H6 壳公司特征：
 * - 近12个月营业总收入 < 1亿元
 * - 总市值 < 20亿元
 * 两者同时满足 → 踢出
 *
 * 说明：
 * - 总市值使用 {@link StockValuationSnapshotDTO#getTotalMarketCapYuan()}（元）
 * - 营业总收入（TTM）依赖利润表 raw_json 字段名；若无 TTM 字段，则用最新一期报告的营业总收入近似。
 */
public class ShellCompanyHardFilterFactor implements FundamentalFactor {
    private static final BigDecimal REV_THRESHOLD = new BigDecimal("100000000"); // 1亿
    private static final BigDecimal MCAP_THRESHOLD = new BigDecimal("2000000000"); // 20亿

    @Override
    public String factorKey() {
        return "fund.hard.shell_company";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.HARD_FILTER;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        StockValuationSnapshotDTO snap = ctx.getSnapshot();
        if (snap == null || snap.getTotalMarketCapYuan() == null) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少市值快照 total_market_cap_yuan");
        }
        BigDecimal mcap = snap.getTotalMarketCapYuan();

        List<FinancialStatementDTO> income = ctx.getStatementsByReportType().getOrDefault(
                EastmoneyFinancialStatementReportTypeEnum.INCOME.getReportName(), List.of());
        if (income.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少利润表数据（无法提取近12个月营业总收入）");
        }
        FinancialStatementDTO latest = income.get(0);
        Map<String, Object> m = FundamentalJsonExtractors.parse(latest.getRawJson());

        BigDecimal rev = FundamentalJsonExtractors.pickDecimal(m, "TOTAL_OPERATE_INCOME");
        if (rev == null) {
            return FundamentalResult.unavailable(factorKey(), group(), "raw_json字段缺失：无法提取营业总收入(TOTAL_OPERATE_INCOME)");
        }

        boolean hit = rev.compareTo(REV_THRESHOLD) < 0 && mcap.compareTo(MCAP_THRESHOLD) < 0;
        String summary = hit ? "近12个月营收<1亿且总市值<20亿：壳公司特征，踢出" : "未触发壳公司特征阈值";

        return FundamentalResult.builder(factorKey(), group())
                .decision(hit ? FundamentalDecision.HARD_EXCLUDE : FundamentalDecision.PASS)
                .summary(summary)
                .metric("report_date", latest.getReportDate())
                .metric("revenue_ttm_or_latest", rev)
                .metric("revenue_threshold", REV_THRESHOLD)
                .metric("total_market_cap_yuan", mcap)
                .metric("market_cap_threshold", MCAP_THRESHOLD)
                .metric("hit", hit)
                .notes(List.of("使用最新一期利润表字段 TOTAL_OPERATE_INCOME 作为近12个月营收近似"))
                .build();
    }
}

