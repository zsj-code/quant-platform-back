package com.quant.platform.business.financial.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("financial_statement")
public class FinancialStatementEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("code")
    private String code;

    @TableField("symbol")
    private String symbol;

    @TableField("report_type")
    private String reportType;

    @TableField("report_date")
    private LocalDate reportDate;

    @TableField("raw_json")
    private String rawJson;

    @TableField("source_report_name")
    private String sourceReportName;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;
}
