package com.quant.platform.business.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_industry_valuation")
public class StockIndustryValuationEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("symbol")
    private String symbol;

    @TableField("sec_code")
    private String secCode;

    @TableField("stock_name")
    private String stockName;

    @TableField("industry_name_from_quote")
    private String industryNameFromQuote;

    @TableField("industry_board_code")
    private String industryBoardCode;

    @TableField("industry_board_name")
    private String industryBoardName;

    @TableField("board_change_pct")
    private BigDecimal boardChangePct;

    @TableField("board_pe_ttm")
    private BigDecimal boardPeTtm;

    @TableField("board_pb")
    private BigDecimal boardPb;

    @TableField("board_ps_ttm")
    private BigDecimal boardPsTtm;

    @TableField("board_total_market_cap_yuan")
    private BigDecimal boardTotalMarketCapYuan;

    @TableField("board_circ_market_cap_yuan")
    private BigDecimal boardCircMarketCapYuan;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;
}
