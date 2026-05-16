package com.quant.platform.business.client.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * 帖子详情：正文 + 评论。
 */
@Value
@Builder
public class GubaPostDetailDTO {
    GubaPostDTO post;
    /** 正文纯文本（尽力解析） */
    String contentText;
    /** 评论列表（尽力解析；可能为空） */
    List<GubaCommentDTO> comments;
}

