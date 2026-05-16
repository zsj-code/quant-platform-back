package com.quant.platform.business.client.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 股吧评论/跟帖（来自详情页解析）。
 */
@Value
@Builder
public class GubaCommentDTO {
    /** 所属帖子 ID */
    String postId;

    /** 评论 ID（若页面未提供则为空） */
    String commentId;
    /** 楼层（若解析不到则为 null） */
    Integer floorNo;

    /** 作者（若解析不到则为空） */
    String author;
    /** 发布时间文本（若解析不到则为空） */
    String publishTimeText;
    /** 正文纯文本 */
    String contentText;

    /** 点赞数（若解析不到则为 null） */
    Integer likeCount;
}

