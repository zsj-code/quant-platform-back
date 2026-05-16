package com.quant.platform.business.research.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("research_report_stock")
public class ResearchReportStockEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("source")
    private String source;

    @TableField("external_id")
    private String externalId;

    @TableField("sec_code")
    private String secCode;

    @TableField("symbol")
    private String symbol;

    @TableField("stock_name")
    private String stockName;

    @TableField("industry_name")
    private String industryName;

    @TableField("title")
    private String title;

    @TableField("org_s_name")
    private String orgSName;

    @TableField("publish_date")
    private LocalDate publishDate;

    @TableField("rating_name")
    private String ratingName;

    @TableField("report_type")
    private String reportType;

    @TableField("column_code")
    private String columnCode;

    @TableField("info_code")
    private String infoCode;

    @TableField("encode_url")
    private String encodeUrl;

    @TableField("detail_url")
    private String detailUrl;

    @TableField("raw_json")
    private String rawJson;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;
}
