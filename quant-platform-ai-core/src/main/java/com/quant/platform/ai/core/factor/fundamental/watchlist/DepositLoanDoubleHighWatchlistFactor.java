package com.quant.platform.ai.core.factor.fundamental.watchlist;

import com.quant.platform.common.dto.FinancialStatementDTO;

import com.quant.platform.ai.core.factor.fundamental.*;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * W1 存贷双高：
 * - (货币资金 + 交易性金融资产) / 总资产 > 0.20
 * - 有息负债 / 总资产 > 0.30
 *
 * 有息负债 = 短期借款 + 一年内到期非流动负债 + 长期借款 + 应付债券
 *
 * 说明：
 * - 仅在资产负债表 raw_json 能提取到相应科目时可计算，否则 UNAVAILABLE。
 * - 行业修正阈值后续通过配置表接入，此处先用默认 0.20/0.30。
 */
public class DepositLoanDoubleHighWatchlistFactor implements FundamentalFactor {
    private static final BigDecimal CASH_RATIO_TH = new BigDecimal("0.20");
    private static final BigDecimal DEBT_RATIO_TH = new BigDecimal("0.30");

    @Override
    public String factorKey() {
        return "fund.watch.deposit_loan_double_high";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.WATCHLIST;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        List<FinancialStatementDTO> balance = ctx.getStatementsByReportType().getOrDefault(
                EastmoneyFinancialStatementReportTypeEnum.BALANCE.getReportName(), List.of());
        if (balance.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少资产负债表数据（financial_statement.raw_json）");
        }
        FinancialStatementDTO latest = balance.get(0);
        Map<String, Object> m = FundamentalJsonExtractors.parse(latest.getRawJson());

        BigDecimal totalAssets = FundamentalJsonExtractors.pickDecimal(m, "TOTAL_ASSETS");
        BigDecimal cash = FundamentalJsonExtractors.pickDecimal(m, "MONETARYFUNDS");
        BigDecimal tradingFin = FundamentalJsonExtractors.pickDecimal(m, "TRADING_FINASSET");

        BigDecimal stLoan = FundamentalJsonExtractors.pickDecimal(m, "SHORT_LOAN");
        BigDecimal oneYearDue = FundamentalJsonExtractors.pickDecimal(m, "NONCURRENT_LIAB_1YEAR");
        BigDecimal ltLoan = FundamentalJsonExtractors.pickDecimal(m, "LONG_LOAN");
        BigDecimal bonds = FundamentalJsonExtractors.pickDecimal(m, "BOND_PAYABLE");

        if (totalAssets == null || totalAssets.compareTo(BigDecimal.ZERO) == 0) {
            return FundamentalResult.unavailable(factorKey(), group(), "raw_json字段缺失：无法提取总资产");
        }

        List<String> notes = new ArrayList<>();
        if (cash == null) {
            notes.add("未命中货币资金字段");
            cash = BigDecimal.ZERO;
        }
        if (tradingFin == null) {
            notes.add("未命中交易性金融资产字段");
            tradingFin = BigDecimal.ZERO;
        }

        BigDecimal cashPlus = cash.add(tradingFin);
        BigDecimal cashRatio = cashPlus.divide(totalAssets, 6, BigDecimal.ROUND_HALF_UP);

        BigDecimal interestDebt = nz(stLoan).add(nz(oneYearDue)).add(nz(ltLoan)).add(nz(bonds));
        BigDecimal debtRatio = interestDebt.divide(totalAssets, 6, BigDecimal.ROUND_HALF_UP);

        boolean hit = cashRatio.compareTo(CASH_RATIO_TH) > 0 && debtRatio.compareTo(DEBT_RATIO_TH) > 0;
        String summary = hit ? "触发存贷双高：标记观察" : "未触发存贷双高";

        return FundamentalResult.builder(factorKey(), group())
                .decision(hit ? FundamentalDecision.WATCH : FundamentalDecision.PASS)
                .summary(summary)
                .metric("report_date", latest.getReportDate())
                .metric("total_assets", totalAssets)
                .metric("cash_plus_trading_fin_assets", cashPlus)
                .metric("cash_ratio", cashRatio)
                .metric("interest_bearing_debt", interestDebt)
                .metric("debt_ratio", debtRatio)
                .metric("cash_ratio_threshold", CASH_RATIO_TH)
                .metric("debt_ratio_threshold", DEBT_RATIO_TH)
                .metric("hit", hit)
                .notes(notes)
                .build();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

