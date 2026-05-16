package com.quant.platform.ai.core.factor.fundamental.watchlist;

import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;

/**
 * W4 大额异常减值：
 * - 最近一个财年 资产减值损失 / 营业利润 > 0.50
 * - 营业利润为负时：减值损失 > |净利润| * 0.50 也触发
 *
 * 数据缺口：
 * - 需要利润表中：资产减值损失、营业利润、净利润字段（从 raw_json 提取，字段名需确认）；
 * - 当前实现先占位，后续确认字段名后再补齐。
 */
public class LargeImpairmentWatchlistFactor implements FundamentalFactor {
    @Override
    public String factorKey() {
        return "fund.watch.large_impairment";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.WATCHLIST;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        // TODO: 从利润表 raw_json 提取字段后实现
        return FundamentalResult.unavailable(factorKey(), group(), "缺少减值损失/营业利润/净利润字段映射（需确认东财 raw_json 字段名）");
    }
}

