package com.quant.platform.business.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区帖子（股吧/财富号等）：用于舆情与研究辅助，不保证结构长期稳定。
 *
 * 唯一键：({@code source}, {@code external_id})
 */
@Data
@TableName("stock_post")
public class StockPostEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 数据来源：如 EASTMONEY_GUBA、EASTMONEY_CAIFUHAO、TAO_GUBA */
    @TableField("source")
    private String source;

    /** 来源侧唯一 ID（建议：股吧 postId；财富号用 cfh_{id}） */
    @TableField("external_id")
    private String externalId;

    @TableField("sec_code")
    private String secCode;

    @TableField("symbol")
    private String symbol;

    @TableField("title")
    private String title;

    @TableField("author")
    private String author;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("read_count")
    private Integer readCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("url")
    private String url;

    /** 正文纯文本（尽力清洗） */
    @TableField("content_text")
    private String contentText;

    /** 原始 HTML（可选，用于回放与纠错） */
    @TableField("raw_html")
    private String rawHtml;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;
}

