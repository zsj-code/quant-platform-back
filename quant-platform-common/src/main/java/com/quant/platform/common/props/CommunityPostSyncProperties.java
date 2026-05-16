package com.quant.platform.common.props;

/**
 * 淘股吧/社区帖与评论同步：时间窗、分页与限速。
 *
 * 说明：该类放在 common 供 business 依赖；Spring 的配置绑定在 app 层完成。
 */
public class CommunityPostSyncProperties {

    /**
     * 只拉取「最近 N 个自然日」内发布的帖与评论（按来源时间戳）。
     */
    private int lookbackDays = 1;

    private int maxListPagesPerStock = 10;

    private int listPageSize = 20;

    private int maxCommentPagesPerPost = 10;

    private int commentPageSize = 20;

    private int sleepMsBetweenPages = 500;

    public int getLookbackDays() {
        return lookbackDays;
    }

    public void setLookbackDays(int lookbackDays) {
        this.lookbackDays = lookbackDays;
    }

    public int getMaxListPagesPerStock() {
        return maxListPagesPerStock;
    }

    public void setMaxListPagesPerStock(int maxListPagesPerStock) {
        this.maxListPagesPerStock = maxListPagesPerStock;
    }

    public int getListPageSize() {
        return listPageSize;
    }

    public void setListPageSize(int listPageSize) {
        this.listPageSize = listPageSize;
    }

    public int getMaxCommentPagesPerPost() {
        return maxCommentPagesPerPost;
    }

    public void setMaxCommentPagesPerPost(int maxCommentPagesPerPost) {
        this.maxCommentPagesPerPost = maxCommentPagesPerPost;
    }

    public int getCommentPageSize() {
        return commentPageSize;
    }

    public void setCommentPageSize(int commentPageSize) {
        this.commentPageSize = commentPageSize;
    }

    public int getSleepMsBetweenPages() {
        return sleepMsBetweenPages;
    }

    public void setSleepMsBetweenPages(int sleepMsBetweenPages) {
        this.sleepMsBetweenPages = sleepMsBetweenPages;
    }
}

