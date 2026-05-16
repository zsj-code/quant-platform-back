package com.quant.platform.business.kline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("kline_bar")
public class KlineBarEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("symbol")
    private String symbol;

    @TableField("interval_type")
    private String intervalType;

    @TableField("bar_time")
    private LocalDateTime barTime;

    @TableField("open")
    private BigDecimal open;

    @TableField("high")
    private BigDecimal high;

    @TableField("low")
    private BigDecimal low;

    @TableField("close")
    private BigDecimal close;

    @TableField("volume")
    private Long volume;

    /** 成交额（东财 f57） */
    @TableField("amount")
    private BigDecimal amount;

    /** 振幅 %（东财 f58） */
    @TableField("amplitude")
    private BigDecimal amplitude;

    /** 涨跌幅 %（东财 f59） */
    @TableField("change_pct")
    private BigDecimal changePct;

    /** 涨跌额（东财 f60） */
    @TableField("change_amount")
    private BigDecimal changeAmount;

    /** 换手率 %（东财 f61） */
    @TableField("turnover_rate")
    private BigDecimal turnoverRate;
}
