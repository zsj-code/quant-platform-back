package com.quant.platform.business.client.dto;

import java.time.LocalDateTime;

/**
 * 帖子页 {@code /a/{newTopicId}} 下单条回帖（由 HTML 解析而来）。
 * <p>
 * 注意：
 * - 该 DTO 来源于页面 HTML 解析，字段可能受站点样式/脚本变动影响；
 * - 时间字段为页面展示的本地时间文本解析结果，通常可按「上海时区」理解；
 * - 仅用于同步/落库与展示，不保证包含“回帖的全部元信息”（例如回复关系、@、图片等）。
 */
public class TaogubaTopicReplyDTO {

    /**
     * 回帖唯一标识：来自 DOM id（形如 {@code reply123456}）。
     */
    private long replyId;

    /**
     * 回帖作者昵称：页面展示名，可能为空或被脱敏。
     */
    private String userName;

    /**
     * 回帖正文纯文本：由 HTML 提取后的文本（不包含富文本结构）。
     */
    private String text;

    /**
     * 回帖发布时间：由页面时间文本解析得到（例如 {@code yyyy-MM-dd HH:mm}）。
     * <p>
     * 为空表示页面未解析到时间或格式不匹配；同步时应按需做空值保护与截止时间过滤。
     */
    private LocalDateTime publishTime;

    /**
     * 楼层号：从“第 N 楼”文本解析得到；解析失败则为 null。
     */
    private Integer floorNo;

    /**
     * 点赞/加油数：当前解析逻辑可能无法稳定获取，默认可能为 0。
     */
    private int likeCount;

    public long getReplyId() {
        return replyId;
    }

    public void setReplyId(long replyId) {
        this.replyId = replyId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(LocalDateTime publishTime) {
        this.publishTime = publishTime;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
}
