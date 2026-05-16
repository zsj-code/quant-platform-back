package com.quant.platform.ai.core.factor.fundamental;

public interface FundamentalFactor {
    String factorKey();

    FundamentalFactorGroup group();

    FundamentalResult evaluate(FundamentalContext ctx);
}

