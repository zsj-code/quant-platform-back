package com.quant.platform.ai.core.factor.fundamental.hardfilter;

import com.quant.platform.ai.core.client.EastmoneyF10GIncomeClient;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalancePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalanceRowDTO;
import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalDecision;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;
import com.quant.platform.ai.core.factor.fundamental.f10.F10FinanceReportUtil;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * H5 商誉堰塞湖（东财 F10 资产负债表 {@code RPT_F10_FINANCE_GBALANCE}）：
 * 商誉 / 归母净资产 &gt; 0.5 → 踢出（最近一期完整财年）。
 */
public class GoodwillDamHardFilterFactor implements FundamentalFactor {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.50");
    private static final int FETCH_PAGE_SIZE = 6;

    private final EastmoneyF10GIncomeClient f10FinanceClient;

    public GoodwillDamHardFilterFactor(@Nullable EastmoneyF10GIncomeClient f10FinanceClient) {
        this.f10FinanceClient = f10FinanceClient;
    }

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
        if (f10FinanceClient == null) {
            return FundamentalResult.unavailable(factorKey(), group(),
                    "未配置 EastmoneyF10GIncomeClient，无法查询 F10 资产负债表");
        }
        String code = resolveStockCode(ctx);
        if (code == null || code.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少证券代码");
        }

        EastmoneyF10GBalanceRowDTO latest;
        try {
            latest = loadLatestYearBalanceRow(code.trim());
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "拉取 F10 资产负债表失败：" + e.getMessage());
        }
        if (latest == null) {
            return FundamentalResult.unavailable(factorKey(), group(), "无可用资产负债表数据（最近财年）");
        }

        return buildResult(latest);
    }

    static FundamentalResult buildResult(EastmoneyF10GBalanceRowDTO row) {
        BigDecimal goodwill = row.goodwill;
        BigDecimal parentEquity = row.totalParentEquity;
        if (parentEquity == null) {
            parentEquity = row.totalEquity;
        }

        if (goodwill == null || parentEquity == null || parentEquity.compareTo(BigDecimal.ZERO) == 0) {
            return FundamentalResult.unavailable("fund.hard.goodwill_to_parent_equity", FundamentalFactorGroup.HARD_FILTER,
                    "资产负债表缺少 GOODWILL 或 TOTAL_PARENT_EQUITY（归母净资产）");
        }

        BigDecimal ratio = goodwill.divide(parentEquity.abs(), 6, RoundingMode.HALF_UP);
        boolean hit = ratio.compareTo(DEFAULT_THRESHOLD) > 0;

        return FundamentalResult.builder("fund.hard.goodwill_to_parent_equity", FundamentalFactorGroup.HARD_FILTER)
                .decision(hit ? FundamentalDecision.HARD_EXCLUDE : FundamentalDecision.PASS)
                .summary(hit ? "商誉/归母净资产>0.5：商誉堰塞湖，踢出" : "未触发商誉堰塞湖阈值")
                .metric("report_date", row.reportDate)
                .metric("report_type", row.reportType)
                .metric("goodwill", goodwill)
                .metric("parent_equity", parentEquity)
                .metric("ratio", ratio)
                .metric("threshold", DEFAULT_THRESHOLD)
                .metric("hit", hit)
                .notes(List.of("数据源：Eastmoney F10 GBALANCE；归母净资产优先 TOTAL_PARENT_EQUITY"))
                .build();
    }

    private EastmoneyF10GBalanceRowDTO loadLatestYearBalanceRow(String code) {
        EastmoneyF10GBalancePageDTO page = f10FinanceClient.fetchF10GBalance(code, List.of(), 1, FETCH_PAGE_SIZE);
        List<EastmoneyF10GBalanceRowDTO> rows = page == null ? null : page.rows();
        return F10FinanceReportUtil.pickLatestYearBalance(rows);
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
