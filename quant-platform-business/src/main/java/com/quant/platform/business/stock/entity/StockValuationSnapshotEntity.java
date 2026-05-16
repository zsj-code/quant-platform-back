package com.quant.platform.business.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个股行情/估值快照，字段与 {@link EastmoneyStockValuationDTO}（东财 stock/get）对齐。
 * <p>
 * {@code change_pct} 在入库时由同步任务按 {@code (latest_price - prev_close) / prev_close} 派生（昨收不为 0）。
 */
@Data
@TableName("stock_valuation_snapshot")
public class StockValuationSnapshotEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 如 600000.SH，与 upsert 唯一键一致 */
    @TableField("symbol")
    private String symbol;

    @TableField("sec_code")
    private String secCode;

    @TableField("stock_name")
    private String stockName;

    @TableField("latest_price")
    private BigDecimal latestPrice;

    @TableField("change_pct")
    private BigDecimal changePct;

    @TableField("volume")
    private Long volume;

    @TableField("amount")
    private BigDecimal amount;

    @TableField("turnover_rate")
    private BigDecimal turnoverRate;

    @TableField("volume_ratio")
    private BigDecimal volumeRatio;

    @TableField("prev_close")
    private BigDecimal prevClose;

    @TableField("open_price")
    private BigDecimal openPrice;

    @TableField("high_price")
    private BigDecimal highPrice;

    @TableField("low_price")
    private BigDecimal lowPrice;

    @TableField("avg_price")
    private BigDecimal avgPrice;

    @TableField("limit_up")
    private BigDecimal limitUp;

    @TableField("limit_down")
    private BigDecimal limitDown;

    @TableField("amplitude")
    private BigDecimal amplitude;

    @TableField("total_market_cap_yuan")
    private BigDecimal totalMarketCapYuan;

    @TableField("circ_market_cap_yuan")
    private BigDecimal circMarketCapYuan;

    @TableField("total_shares")
    private BigDecimal totalShares;

    @TableField("float_shares")
    private BigDecimal floatShares;

    @TableField("pe_dynamic")
    private BigDecimal peDynamic;

    @TableField("pc")
    private BigDecimal pc;

    @TableField("pe_static")
    private BigDecimal peStatic;

    @TableField("ps")
    private BigDecimal ps;

    @TableField("pb")
    private BigDecimal pb;

    @TableField("raw_quote_json")
    private String rawQuoteJson;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;
}
