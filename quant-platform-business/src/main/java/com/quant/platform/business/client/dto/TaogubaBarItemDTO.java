package com.quant.platform.business.client.dto;

import java.time.OffsetDateTime;

/**
 * 淘股吧行情页内嵌「最热」流 {@code coolAttr} 单条（与页面字段对齐）。
 * <p>
 * 注意：
 * - 数据来自 {@code /quotes/{sh600000}} 页面内的脚本变量 {@code var coolAttr = [...]}，属于“页面内嵌 JSON”，字段可能随站点改版变化；
 * - 文本字段（subject/body）通常为 HTML 片段，客户端解析时会转为纯文本；
 * - 时间字段 {@link #actionTime} 为带 offset 的时间戳（一般为 +08:00），落库/比较时建议统一转换到上海时区的 {@code LocalDateTime}。
 */
public class TaogubaBarItemDTO {

    /**
     * 帖子唯一标识（淘股吧 newTopicID），用于拼接帖子 URL {@code https://www.tgb.cn/a/{newTopicId}}。
     */
    private String newTopicId;

    /**
     * 记录类型：常见为
     * <ul>
     *   <li>{@code T}：主帖</li>
     *   <li>{@code R}：转发/引用</li>
     * </ul>
     * 解析端通常会过滤 {@code B/CLS} 等非帖子流类型。
     */
    private String rType;

    /**
     * 标题：可能为空；为空时可降级用 {@link #body} 截断作为标题。
     */
    private String subject;

    /**
     * 内容摘要/正文：通常来自页面内嵌 HTML，解析后为纯文本。
     */
    private String body;

    /**
     * 作者昵称：页面展示名，可能为空或被脱敏。
     */
    private String userName;

    /**
     * 原字段 {@code actionDate}：带 offset 的时间，例如 {@code 2026-04-21T08:40:39.000+08:00}。
     * <p>
     * 用于时间线排序/过滤；建议在业务层转换为上海时区 {@code LocalDateTime} 再进行“按自然日”的统计或截止判断。
     */
    private OffsetDateTime actionTime;

    /**
     * 评论数（replyNum）：来自页面内嵌字段，可能为 0 或不准确（可用详情页补充校准）。
     */
    private int replyNum;

    /**
     * 浏览数（viewNum）：来自页面内嵌字段，可能为 0 或不准确（可用详情页补充校准）。
     */
    private int viewNum;

    /**
     * 点赞/加油数：对应站点字段 {@code usefulNum}，不少情况下可能为 0。
     */
    private int likeCount;

    public String getNewTopicId() {
        return newTopicId;
    }

    public void setNewTopicId(String newTopicId) {
        this.newTopicId = newTopicId;
    }

    public String getRType() {
        return rType;
    }

    public void setRType(String rType) {
        this.rType = rType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public OffsetDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(OffsetDateTime actionTime) {
        this.actionTime = actionTime;
    }

    public int getReplyNum() {
        return replyNum;
    }

    public void setReplyNum(int replyNum) {
        this.replyNum = replyNum;
    }

    public int getViewNum() {
        return viewNum;
    }

    public void setViewNum(int viewNum) {
        this.viewNum = viewNum;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
}
