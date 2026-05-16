package com.quant.platform.ai.core.factor.fundamental.watchlist;


import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;

/**
 * W6 一致预期剧烈下调：
 * - 近一个月，未来一年一致预期净利润下调超过15%
 * - 且覆盖机构数 >= 3
 *
 * 数据缺口：
 * - 当前项目未见一致预期（覆盖机构数、预测净利润、变动幅度）落库表；
 * - 需要接入外部一致预期数据源（如东财/同花顺等）并做日度快照/变更记录。
 */
public class ConsensusEstimateCutWatchlistFactor implements FundamentalFactor {
    @Override
    public String factorKey() {
        return "fund.watch.consensus_estimate_cut";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.WATCHLIST;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        // TODO: 接入一致预期数据后实现
        return FundamentalResult.unavailable(factorKey(), group(), "缺少一致预期净利润与覆盖机构数数据");
    }
}

