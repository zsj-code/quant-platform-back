package com.quant.platform.business.job;

import com.quant.platform.business.client.TaogubaClient;
import com.quant.platform.business.client.dto.TaogubaBarItemDTO;
import com.quant.platform.business.client.dto.TaogubaTopicReplyDTO;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.entity.StockPostCommentEntity;
import com.quant.platform.business.stock.entity.StockPostEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.business.stock.service.StockPostAdminService;
import com.quant.platform.business.stock.service.StockPostCommentAdminService;
import com.quant.platform.common.constant.CommunityPostSources;
import com.quant.platform.common.props.CommunityPostSyncProperties;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 淘股吧：近 N 天帖与评论落库 {@code stock_post}、{@code stock_post_comment}（来源 {@link CommunityPostSources#TAO_GUBA}）。
 */
@Service
public class TaogubaPostCommentSyncService {

    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    private final TaogubaClient taogubaClient;
    private final StockPostAdminService stockPostAdminService;
    private final StockPostCommentAdminService stockPostCommentAdminService;
    private final CommunityPostSyncProperties props;
    private final StockAdminService stockAdminService;

    public TaogubaPostCommentSyncService(TaogubaClient taogubaClient, StockPostAdminService stockPostAdminService,
            StockPostCommentAdminService stockPostCommentAdminService, CommunityPostSyncProperties props,
            StockAdminService stockAdminService) {
        this.taogubaClient = taogubaClient;
        this.stockPostAdminService = stockPostAdminService;
        this.stockPostCommentAdminService = stockPostCommentAdminService;
        this.props = props;
        this.stockAdminService = stockAdminService;
    }

    public SyncStats syncTaogubaForAllStocks() {
        LocalDateTime cutoff = cutoffTime();
        int posts = 0;
        int comments = 0;
        List<StockEntity> list = stockAdminService.listNonDelisted();
        for (StockEntity st : list) {
            if (st == null || st.getCode() == null) {
                continue;
            }
            SyncStats one = syncTaogubaForSecCode(st.getCode().trim(), cutoff);
            posts += one.getPosts();
            comments += one.getComments();
        }
        return new SyncStats(posts, comments);
    }

    public SyncStats syncTaogubaForSecCode(String secCode, LocalDateTime cutoff) {
        String c = CommonUtil.normalizeSixDigitCode(secCode);
        if (c == null || c.isEmpty()) {
            return new SyncStats(0, 0);
        }
        String tgb = CommonUtil.toTaogubaFullCode(c);
        if (tgb == null || tgb.isEmpty()) {
            return new SyncStats(0, 0);
        }
        int postN = 0;
        int cmtN = 0;
        int sleep = Math.max(0, props.getSleepMsBetweenPages());
        List<TaogubaBarItemDTO> bar = taogubaClient.listStockBarCool(tgb);
        if (bar == null) {
            return new SyncStats(0, 0);
        }
        for (TaogubaBarItemDTO b : bar) {
            if (b.getActionTime() == null) {
                continue;
            }
            LocalDateTime actionTime = b.getActionTime().atZoneSameInstant(SH).toLocalDateTime();
            if (cutoff != null && actionTime.isBefore(cutoff)) {
                continue;
            }
            StockPostEntity p = toTaoGubaPost(c, b);
            // 补充：详情页可解析到更准确的浏览/评论/加油(点赞)统计
            if ((p.getReadCount() == null || p.getReadCount() <= 0)
                    || (p.getCommentCount() == null || p.getCommentCount() <= 0)
                    || (p.getLikeCount() == null)) {
                TaogubaClient.TopicStats stats = taogubaClient.fetchTopicStats(b.getNewTopicId());
                if (stats != null) {
                    if (stats.getReadCount() != null && stats.getReadCount() >= 0) {
                        p.setReadCount(stats.getReadCount());
                    }
                    if (stats.getCommentCount() != null && stats.getCommentCount() >= 0) {
                        p.setCommentCount(stats.getCommentCount());
                    }
                    if (stats.getLikeCount() != null && stats.getLikeCount() >= 0) {
                        p.setLikeCount(stats.getLikeCount());
                    }
                    if (p.getPublishTime() == null && stats.getPublishTime() != null) {
                        p.setPublishTime(stats.getPublishTime());
                    }
                }
            }
            stockPostAdminService.upsert(p);
            postN++;
            String postExt = b.getNewTopicId();
            String ntid = b.getNewTopicId();
            List<StockPostCommentEntity> cmtBatch = new ArrayList<>();
            for (int cp = 1; cp <= props.getMaxCommentPagesPerPost(); cp++) {
                List<TaogubaTopicReplyDTO> cList = taogubaClient.listTopicRepliesPage(ntid, cp);
                if (cList == null || cList.isEmpty()) {
                    break;
                }
                int addedInPage = 0;
                for (TaogubaTopicReplyDTO d : cList) {
                    if (d.getPublishTime() == null || (cutoff != null && d.getPublishTime().isBefore(cutoff))) {
                        continue;
                    }
                    StockPostCommentEntity ce = toTaoGubaComment(c, postExt, d);
                    if (ce == null) {
                        continue;
                    }
                    cmtBatch.add(ce);
                    addedInPage++;
                }
                if (addedInPage == 0) {
                    break;
                }
                if (sleep > 0) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleep);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            if (!cmtBatch.isEmpty()) {
                stockPostCommentAdminService.upsertBatch(cmtBatch);
                cmtN += cmtBatch.size();
            }
        }
        return new SyncStats(postN, cmtN);
    }

    public SyncStats syncTaogubaForSecCode(String secCode) {
        return syncTaogubaForSecCode(secCode, cutoffTime());
    }

    private LocalDateTime cutoffTime() {
        // 淘股吧：仅拉取「从昨天 00:00 起」的时间线（上海时区）。
        return LocalDate.now(SH).minusDays(1).atStartOfDay();
    }

    private static StockPostEntity toTaoGubaPost(String secCode, TaogubaBarItemDTO b) {
        StockPostEntity e = new StockPostEntity();
        e.setSource(CommunityPostSources.TAO_GUBA);
        e.setExternalId(b.getNewTopicId());
        e.setSecCode(secCode);
        e.setSymbol(CommonUtil.toSymbol(secCode));
        String title = b.getSubject();
        if (title == null || title.isBlank()) {
            String t = b.getBody();
            if (t != null && t.length() > 200) {
                title = t.substring(0, 200);
            } else {
                title = t;
            }
        }
        e.setTitle(title);
        e.setContentText(b.getBody());
        e.setAuthor(b.getUserName());
        e.setUrl(TaogubaClient.postUrl(b.getNewTopicId()));
        e.setCommentCount(b.getReplyNum());
        e.setReadCount(b.getViewNum() > 0 ? b.getViewNum() : null);
        // 淘股吧 usefulNum 常为 0；为避免落库为 null 被误判为“没抓到”，这里保留 0 值。
        e.setLikeCount(b.getLikeCount());
        e.setPublishTime(b.getActionTime().atZoneSameInstant(SH).toLocalDateTime());
        e.setFetchedAt(LocalDateTime.now(SH));
        return e;
    }

    private static StockPostCommentEntity toTaoGubaComment(String secCode, String postExternalId, TaogubaTopicReplyDTO d) {
        if (d == null || d.getReplyId() <= 0) {
            return null;
        }
        String text = d.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        StockPostCommentEntity e = new StockPostCommentEntity();
        e.setSource(CommunityPostSources.TAO_GUBA);
        e.setExternalId(String.valueOf(d.getReplyId()));
        e.setPostExternalId(postExternalId);
        e.setSecCode(secCode);
        e.setAuthor(d.getUserName());
        e.setContentText(text);
        e.setLikeCount(d.getLikeCount() > 0 ? d.getLikeCount() : null);
        e.setFloorNo(d.getFloorNo());
        e.setPublishTime(d.getPublishTime());
        e.setFetchedAt(LocalDateTime.now(SH));
        return e;
    }

    public static final class SyncStats {
        private final int posts;
        private final int comments;

        public SyncStats(int posts, int comments) {
            this.posts = posts;
            this.comments = comments;
        }

        public int getPosts() {
            return posts;
        }

        public int getComments() {
            return comments;
        }
    }
}
