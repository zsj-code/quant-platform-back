package com.quant.platform.business.client.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 股吧帖子摘要（来自列表页或详情页解析）。
 */
@Value
@Builder
public class GubaPostDTO {
    /** 6 位证券代码 */
    String secCode;
    /** 帖子 ID（从 URL 中抽取） */
    String postId;
    /** 帖子标题 */
    String title;
    /** 帖子详情页 URL（绝对地址） */
    String url;

    /** 作者（若解析不到则为空） */
    String author;
    /** 发布时间文本（若解析不到则为空） */
    String publishTimeText;

    /** 阅读数（若解析不到则为 null） */
    Integer readCount;
    /** 评论数（若解析不到则为 null） */
    Integer commentCount;
}

