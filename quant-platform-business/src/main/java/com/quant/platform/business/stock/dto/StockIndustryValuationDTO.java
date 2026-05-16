package com.quant.platform.business.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个股所属行业板块估值（表 {@code stock_industry_valuation}）。
 */
@Data
public class StockIndustryValuationDTO {
    private String id;
    private String symbol;
    private String secCode;
    private String stockName;
    private String industryNameFromQuote;
    private String industryBoardCode;
    private String industryBoardName;
    private BigDecimal boardChangePct;
    private BigDecimal boardPeTtm;
    private BigDecimal boardPb;
    private BigDecimal boardPsTtm;
    private BigDecimal boardTotalMarketCapYuan;
    private BigDecimal boardCircMarketCapYuan;
    private LocalDateTime fetchedAt;
}
