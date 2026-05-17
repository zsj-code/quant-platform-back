package com.quant.platform.ai.core.factor.fundamental.watchlist;

import com.quant.platform.ai.core.client.EastmoneyF10GIncomeClient;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalancePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalanceRowDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomeRowDTO;
import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalDecision;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;
import com.quant.platform.ai.core.factor.fundamental.f10.F10FinanceReportUtil;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * W2 研发过度资本化（东财 F10）：
 * <ul>
 *     <li>利润表 {@code RESEARCH_EXPENSE}：费用化研发支出</li>
 *     <li>资产负债表 {@code DEVELOP_EXPENSE} 期初/期末差额：近似当期资本化研发增量</li>
 *     <li>资本化率 = 资本化增量 / (费用化研发 + 资本化增量)；连续两个财年均 &gt; 0.50 则观察</li>
 * </ul>
 */
public class RndOverCapitalizationWatchlistFactor implements FundamentalFactor {

    static final BigDecimal DEFAULT_TH = new BigDecimal("0.50");
    private static final int FETCH_PAGE_SIZE = 10;

    private final EastmoneyF10GIncomeClient f10FinanceClient;

    public RndOverCapitalizationWatchlistFactor(@Nullable EastmoneyF10GIncomeClient f10FinanceClient) {
        this.f10FinanceClient = f10FinanceClient;
    }

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
        if (f10FinanceClient == null) {
            return FundamentalResult.unavailable(factorKey(), group(),
                    "未配置 EastmoneyF10GIncomeClient，无法查询 F10 财务报表");
        }
        String code = resolveStockCode(ctx);
        if (code == null || code.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少证券代码");
        }

        try {
            return evaluateFromF10(code.trim());
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "拉取 F10 财务报表失败：" + e.getMessage());
        }
    }

    private FundamentalResult evaluateFromF10(String code) {
        EastmoneyF10GIncomePageDTO incomePage = f10FinanceClient.fetchF10GIncome(code, List.of(), 1, FETCH_PAGE_SIZE);
        EastmoneyF10GBalancePageDTO balancePage = f10FinanceClient.fetchF10GBalance(code, List.of(), 1, FETCH_PAGE_SIZE);

        List<EastmoneyF10GIncomeRowDTO> incomeYears =
                F10FinanceReportUtil.pickLatestTwoYearIncome(incomePage == null ? null : incomePage.rows());
        List<EastmoneyF10GBalanceRowDTO> balanceRows = balancePage == null ? null : balancePage.rows();

        if (incomeYears.size() < 2) {
            return FundamentalResult.unavailable(factorKey(), group(), "财年利润表数据不足两期（需要最近两年年报）");
        }
        if (balanceRows == null || balanceRows.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少 F10 资产负债表数据");
        }

        List<BigDecimal> ratios = new ArrayList<>(2);
        List<String> reportDates = new ArrayList<>(2);
        for (int i = 0; i < 2; i++) {
            EastmoneyF10GIncomeRowDTO inc = incomeYears.get(i);
            EastmoneyF10GBalanceRowDTO bal = F10FinanceReportUtil.findBalanceByReportDate(balanceRows, inc.reportDate());
            if (bal == null) {
                return FundamentalResult.unavailable(factorKey(), group(),
                        "利润表与资产负债表报告期无法对齐：" + inc.reportDate());
            }
            EastmoneyF10GBalanceRowDTO priorBal = i + 1 < incomeYears.size()
                    ? F10FinanceReportUtil.findBalanceByReportDate(balanceRows, incomeYears.get(i + 1).reportDate())
                    : null;

            RndCapitalizationMetrics m = computeCapitalizationRatio(inc, bal, priorBal);
            if (m == null) {
                return FundamentalResult.unavailable(factorKey(), group(),
                        "无法计算研发资本化率（缺少 RESEARCH_EXPENSE 或研发总支出非正）");
            }
            ratios.add(m.ratio());
            reportDates.add(inc.reportDate());
        }

        boolean hit = ratios.get(0).compareTo(DEFAULT_TH) > 0 && ratios.get(1).compareTo(DEFAULT_TH) > 0;
        String summary = hit ? "连续两财年研发资本化率>0.50：标记观察" : "未触发研发过度资本化(连续两年)";

        return FundamentalResult.builder(factorKey(), group())
                .decision(hit ? FundamentalDecision.WATCH : FundamentalDecision.PASS)
                .summary(summary)
                .metric("ratio_year1", ratios.get(0))
                .metric("ratio_year2", ratios.get(1))
                .metric("report_date_year1", reportDates.get(0))
                .metric("report_date_year2", reportDates.get(1))
                .metric("threshold", DEFAULT_TH)
                .metric("hit", hit)
                .notes(List.of(
                        "费用化：GINCOME.RESEARCH_EXPENSE",
                        "资本化增量：GBALANCE.DEVELOP_EXPENSE 同比期差额（非负）",
                        "F10 利润表无单独资本化科目，采用资产负债表开发支出变动近似"))
                .build();
    }

    /**
     * @param priorBalance 上一报告期资产负债表（用于开发支出期初），可为 null 则资本化增量取 0
     */
    static RndCapitalizationMetrics computeCapitalizationRatio(
            EastmoneyF10GIncomeRowDTO income,
            EastmoneyF10GBalanceRowDTO balance,
            EastmoneyF10GBalanceRowDTO priorBalance) {
        if (income == null || balance == null) {
            return null;
        }
        BigDecimal expensed = income.researchExpense();
        if (expensed == null) {
            return null;
        }

        BigDecimal developEnd = balance.developExpense == null ? BigDecimal.ZERO : balance.developExpense;
        BigDecimal developBegin = priorBalance == null || priorBalance.developExpense == null
                ? BigDecimal.ZERO
                : priorBalance.developExpense;

        BigDecimal capitalized = developEnd.subtract(developBegin);
        if (capitalized.compareTo(BigDecimal.ZERO) < 0) {
            capitalized = BigDecimal.ZERO;
        }

        BigDecimal total = expensed.add(capitalized);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal ratio = capitalized.divide(total, 6, RoundingMode.HALF_UP);
        return new RndCapitalizationMetrics(expensed, capitalized, total, ratio);
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

    record RndCapitalizationMetrics(
            BigDecimal researchExpense,
            BigDecimal capitalizedIncrement,
            BigDecimal totalRnd,
            BigDecimal ratio) {
    }
}
