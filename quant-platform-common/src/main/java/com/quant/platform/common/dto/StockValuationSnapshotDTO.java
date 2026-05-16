package com.quant.platform.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockValuationSnapshotDTO {
    private String symbol;
    private String secCode;
    private BigDecimal totalMarketCapYuan;
    private LocalDateTime fetchedAt;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSecCode() {
        return secCode;
    }

    public void setSecCode(String secCode) {
        this.secCode = secCode;
    }

    public BigDecimal getTotalMarketCapYuan() {
        return totalMarketCapYuan;
    }

    public void setTotalMarketCapYuan(BigDecimal totalMarketCapYuan) {
        this.totalMarketCapYuan = totalMarketCapYuan;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}

