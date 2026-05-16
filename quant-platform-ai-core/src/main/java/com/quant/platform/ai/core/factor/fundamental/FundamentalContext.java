package com.quant.platform.ai.core.factor.fundamental;

import com.quant.platform.common.dto.FinancialStatementDTO;
import com.quant.platform.common.dto.StockValuationSnapshotDTO;

import java.util.List;
import java.util.Map;

public final class FundamentalContext {
    private final String symbol;
    private final String secCode;
    private final StockValuationSnapshotDTO snapshot;
    private final Map<String, List<FinancialStatementDTO>> statementsByReportType;

    public FundamentalContext(String symbol,
                              String secCode,
                              StockValuationSnapshotDTO snapshot,
                              Map<String, List<FinancialStatementDTO>> statementsByReportType) {
        this.symbol = symbol;
        this.secCode = secCode;
        this.snapshot = snapshot;
        this.statementsByReportType = statementsByReportType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSecCode() {
        return secCode;
    }

    public StockValuationSnapshotDTO getSnapshot() {
        return snapshot;
    }

    public Map<String, List<FinancialStatementDTO>> getStatementsByReportType() {
        return statementsByReportType;
    }
}

