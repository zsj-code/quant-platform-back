package com.quant.platform.business.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 社区帖子评论/跟帖（股吧/财富号等）。
 *
 * 唯一键：({@code source}, {@code external_id})
 */
@Data
@TableName("stock_post_comment")
public class StockPostCommentEntity {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /** 数据来源：如 EASTMONEY_GUBA、EASTMONEY_CAIFUHAO、TAO_GUBA */
    @TableField("source")
    private String source;

    /** 来源侧唯一 ID（若无则可用 hash；注意稳定性） */
    @TableField("external_id")
    private String externalId;

    /** 所属帖子 external_id（方便 join / 过滤） */
    @TableField("post_external_id")
    private String postExternalId;

    @TableField("sec_code")
    private String secCode;

    @TableField("author")
    private String author;

    @TableField("publish_time")
    private LocalDateTime publishTime;

    @TableField("floor_no")
    private Integer floorNo;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("content_text")
    private String contentText;

    @TableField("raw_html")
    private String rawHtml;

    @TableField("fetched_at")
    private LocalDateTime fetchedAt;
}

