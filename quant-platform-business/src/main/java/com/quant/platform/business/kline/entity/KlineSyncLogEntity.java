package com.quant.platform.business.kline.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("kline_sync_log")
public class KlineSyncLogEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("stock_code")
    private String stockCode;

    @TableField("symbol")
    private String symbol;

    @TableField("interval_type")
    private String intervalType;

    @TableField("sync_date")
    private LocalDate syncDate;

    @TableField("bar_count")
    private Integer barCount;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
