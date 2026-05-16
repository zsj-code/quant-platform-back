package com.quant.platform.ai.core.factor.fundamental.watchlist;


import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;

/**
 * W3 扣非净利润趋势恶化：
 * - 近三年扣非净利润 CAGR 为负
 * - 且本年度增长率低于行业后20%分位（申万二级）
 *
 * 数据缺口：
 * - 需要至少3年“扣非净利润”时间序列（可从利润表 raw_json 提取，但字段名需确认）；
 * - 行业分类（申万二级）与行业分位需要预计算并落库（或离线计算服务），当前项目未见相关表/字段。
 */
public class DeductNetProfitDeteriorationWatchlistFactor implements FundamentalFactor {
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
        // TODO: 接入扣非净利润3年序列 + 行业分位数据后实现
        return FundamentalResult.unavailable(factorKey(), group(), "缺少行业分位与三年扣非净利润序列（需新增行业分类与离线分位数据）");
    }
}

