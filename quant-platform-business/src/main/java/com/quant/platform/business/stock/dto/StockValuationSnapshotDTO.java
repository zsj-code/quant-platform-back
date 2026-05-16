package com.quant.platform.business.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个股估值快照（表 {@code stock_valuation_snapshot}）对外传输对象，与 {@link EastmoneyStockValuationDTO} 口径一致。
 */
@Data
public class StockValuationSnapshotDTO {
    private String id;
    private String symbol;
    private String secCode;
    private String stockName;
    private BigDecimal latestPrice;
    private BigDecimal changePct;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal turnoverRate;
    private BigDecimal volumeRatio;
    private BigDecimal prevClose;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal avgPrice;
    private BigDecimal limitUp;
    private BigDecimal limitDown;
    private BigDecimal amplitude;
    private BigDecimal totalMarketCapYuan;
    private BigDecimal circMarketCapYuan;
    private BigDecimal totalShares;
    private BigDecimal floatShares;
    private BigDecimal peDynamic;
    private BigDecimal pc;
    private BigDecimal peStatic;
    private BigDecimal ps;
    private BigDecimal pb;
    private String rawQuoteJson;
    private LocalDateTime fetchedAt;
}
