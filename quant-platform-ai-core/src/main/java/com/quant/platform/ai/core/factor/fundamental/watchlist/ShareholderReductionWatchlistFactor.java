package com.quant.platform.ai.core.factor.fundamental.watchlist;


import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;

/**
 * W5 股东减持密集度：
 * - 近三个月内，重要股东减持计划公告涉及股份 > 总股本的2%
 * - 或 近一个月实际减持 > 1%
 *
 * 数据缺口：
 * - 需要解析“减持预披露公告”和“实际变动公告”，抽取减持数量/比例，并与总股本对齐；
 * - 当前公告库表未见对应的结构化字段与解析逻辑，因此先占位。
 */
public class ShareholderReductionWatchlistFactor implements FundamentalFactor {
    @Override
    public String factorKey() {
        return "fund.watch.shareholder_reduction_density";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.WATCHLIST;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        // TODO: 接入公告解析与总股本字段后实现
        return FundamentalResult.unavailable(factorKey(), group(), "缺少重要股东减持公告的结构化解析数据");
    }
}

