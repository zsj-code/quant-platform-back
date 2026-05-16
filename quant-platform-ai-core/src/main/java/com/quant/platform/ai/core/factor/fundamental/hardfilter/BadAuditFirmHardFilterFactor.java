package com.quant.platform.ai.core.factor.fundamental.hardfilter;


import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;

/**
 * H7 非标审计关联信号：审计机构为“失能所” → 踢出。
 *
 * 数据缺口：
 * - 需要两部分：
 *   1) 标的对应的审计机构名称（最近年报）
 *   2) 失能会计师事务所名单（需维护配置/表，且可按时间生效）
 * - 当前项目未见审计机构落库字段，因此先占位。
 */
public class BadAuditFirmHardFilterFactor implements FundamentalFactor {
    @Override
    public String factorKey() {
        return "fund.hard.bad_audit_firm";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.HARD_FILTER;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        return FundamentalResult.unavailable(factorKey(), group(), "缺少审计机构字段与失能所名单配置（需新增数据源/配置表）");
    }
}

