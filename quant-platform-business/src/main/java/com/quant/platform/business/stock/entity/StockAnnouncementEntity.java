package com.quant.platform.business.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_announcement")
public class StockAnnouncementEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("code")
    private String code;

    @TableField("symbol")
    private String symbol;

    @TableField("source")
    private String source;

    @TableField("external_id")
    private String externalId;

    @TableField("title")
    private String title;

    @TableField("announce_time")
    private LocalDateTime announceTime;

    @TableField("notice_date")
    private LocalDate noticeDate;

    @TableField("categories")
    private String categories;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;
}
