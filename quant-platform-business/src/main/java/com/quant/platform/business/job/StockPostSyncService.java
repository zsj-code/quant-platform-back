package com.quant.platform.business.job;


import com.quant.platform.business.client.EastmoneyGubaClient;
import com.quant.platform.business.client.dto.GubaCommentDTO;
import com.quant.platform.business.client.dto.GubaPostDTO;
import com.quant.platform.business.client.dto.GubaPostDetailDTO;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.entity.StockPostCommentEntity;
import com.quant.platform.business.stock.entity.StockPostEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.business.stock.service.StockPostAdminService;
import com.quant.platform.business.stock.service.StockPostCommentAdminService;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 股吧/财富号帖子与评论同步（落库到 {@code stock_post} / {@code stock_post_comment}）。
 * <p>
 * 说明：当前 {@link EastmoneyGubaClient} 对评论解析为 HTML「尽力策略」；如需稳定全量评论，建议后续接入评论 XHR JSON 接口。
 */
@Service
public class StockPostSyncService {

    private static final Logger log = LoggerFactory.getLogger(StockPostSyncService.class);

    private static final String SOURCE_EASTMONEY_GUBA = "EASTMONEY_GUBA";
    private static final String SOURCE_EASTMONEY_CAIFUHAO = "EASTMONEY_CAIFUHAO";

    private static final int DEFAULT_SLEEP_MS_PER_PAGE = 900;

    private static final DateTimeFormatter DT1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DT2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DT3 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    private static final DateTimeFormatter DT4 = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    private static final DateTimeFormatter DT5 = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final EastmoneyGubaClient eastmoneyGubaClient;
    private final StockPostAdminService stockPostAdminService;
    private final StockPostCommentAdminService stockPostCommentAdminService;
    private final StockAdminService stockAdminService;

    public StockPostSyncService(EastmoneyGubaClient eastmoneyGubaClient, StockPostAdminService stockPostAdminService,
                                StockPostCommentAdminService stockPostCommentAdminService, StockAdminService stockAdminService) {
        this.eastmoneyGubaClient = eastmoneyGubaClient;
        this.stockPostAdminService = stockPostAdminService;
        this.stockPostCommentAdminService = stockPostCommentAdminService;
        this.stockAdminService = stockAdminService;
    }

    public List<SyncResult> syncAll(int maxPages, int sleepMsPerPage) {
        List<StockEntity> stockEntityList = stockAdminService.listNonDelisted();
        List<SyncResult>  syncResultList = new ArrayList<>();
        for (StockEntity stock : stockEntityList) {
            SyncResult syncResult = syncOne(stock.getCode(), maxPages, sleepMsPerPage);
            syncResultList.add(syncResult);
        }
        return syncResultList;
    }

    /**
     * 同步单个标的最近若干页帖子；对股吧帖额外拉取详情与评论。
     *
     * @param secCode
     *            6 位证券代码
     * @param maxPages
     *            从 1 开始，建议小于 30，避免触发站点限制
     * @param sleepMsPerPage
     *            每页抓取后的 sleep（限速）
     * @return 本次写入帖子数（去重后）与评论数
     */
    public SyncResult syncOne(String secCode, int maxPages, int sleepMsPerPage) {
        String c = CommonUtil.normalizeSixDigitCode(secCode);
        if (c == null || c.isEmpty()) {
            return new SyncResult(0, 0);
        }
        int pages = Math.max(1, maxPages);
        int sleep = sleepMsPerPage <= 0 ? DEFAULT_SLEEP_MS_PER_PAGE : sleepMsPerPage;

        Map<String, StockPostEntity> posts = new LinkedHashMap<>();
        List<StockPostCommentEntity> comments = new ArrayList<>();

        for (int page = 1; page <= pages; page++) {
            List<GubaPostDTO> list = eastmoneyGubaClient.listPosts(c, page);
            if (list == null || list.isEmpty()) {
                break;
            }

            for (GubaPostDTO p : list) {
                // 列表解析异常时可能混入空 id；此处 break 结束本页循环（与「跳过单条」不同，遇脏数据即停止该页）。
                if (p == null || p.getPostId() == null || p.getPostId().isBlank()) {
                    break;
                }
                String postExternalId = p.getPostId().trim();
                boolean isCaifuhao = postExternalId.startsWith("cfh_");
                String source = isCaifuhao ? SOURCE_EASTMONEY_CAIFUHAO : SOURCE_EASTMONEY_GUBA;

                StockPostEntity e = new StockPostEntity();
                e.setSource(source);
                e.setExternalId(postExternalId);
                e.setSecCode(c);
                e.setSymbol(CommonUtil.toSymbol(c));
                e.setTitle(p.getTitle());
                e.setUrl(p.getUrl());
                e.setAuthor(p.getAuthor());
                e.setPublishTime(parsePostTime(p.getPublishTimeText()));
                e.setReadCount(p.getReadCount());
                e.setCommentCount(p.getCommentCount());
                e.setFetchedAt(LocalDateTime.now());

                // 对股吧帖尽力补正文+评论
                if (!isCaifuhao) {
                    try {
                        GubaPostDetailDTO detail = eastmoneyGubaClient.fetchPostDetail(c, postExternalId);
                        if (detail != null) {
                            if (detail.getPost() != null) {
                                // 标题可能更准
                                if (detail.getPost().getTitle() != null && !detail.getPost().getTitle().isBlank()) {
                                    e.setTitle(detail.getPost().getTitle());
                                }
                                if (detail.getPost().getUrl() != null && !detail.getPost().getUrl().isBlank()) {
                                    e.setUrl(detail.getPost().getUrl());
                                }
                            }
                            if (detail.getContentText() != null && !detail.getContentText().isBlank()) {
                                e.setContentText(detail.getContentText());
                            }
                            List<GubaCommentDTO> cs = detail.getComments();
                            if (cs != null && !cs.isEmpty()) {
                                for (GubaCommentDTO cmt : cs) {
                                    StockPostCommentEntity ce = mapComment(c, postExternalId, source, cmt);
                                    if (ce != null) {
                                        comments.add(ce);
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // 尽力同步：单帖失败不影响整页
                        log.warn("post detail parse failed secCode={} postId={} err={}", c, postExternalId,
                                ex.toString());
                    }
                }

                posts.put(source + "|" + postExternalId, e);
            }

            if (sleep > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(sleep);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!posts.isEmpty()) {
            stockPostAdminService.upsertBatch(new ArrayList<>(posts.values()));
        }
        if (!comments.isEmpty()) {
            stockPostCommentAdminService.upsertBatch(comments);
        }
        return new SyncResult(posts.size(), comments.size());
    }

    private static StockPostCommentEntity mapComment(String secCode, String postExternalId, String source,
            GubaCommentDTO cmt) {
        if (cmt == null) {
            return null;
        }
        String text = cmt.getContentText();
        if (text == null || text.isBlank()) {
            return null;
        }
        StockPostCommentEntity e = new StockPostCommentEntity();
        e.setSource(source);
        e.setPostExternalId(postExternalId);
        e.setSecCode(secCode);
        e.setAuthor(cmt.getAuthor());
        e.setPublishTime(parsePostTime(cmt.getPublishTimeText()));
        e.setFloorNo(cmt.getFloorNo());
        e.setLikeCount(cmt.getLikeCount());
        e.setContentText(text);
        e.setFetchedAt(LocalDateTime.now());

        String ext = cmt.getCommentId();
        if (ext == null || ext.isBlank()) {
            // 无 commentId 时生成稳定键：post + floor + author + time + content hash
            String rawKey = postExternalId + "|" + (cmt.getFloorNo() == null ? "" : cmt.getFloorNo()) + "|"
                    + safe(cmt.getAuthor()) + "|" + safe(cmt.getPublishTimeText()) + "|" + text;
            ext = "gen_" + sha1Hex(rawKey);
        }
        e.setExternalId(ext.trim());
        return e;
    }

    private static LocalDateTime parsePostTime(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty() || "—".equals(v)) {
            return null;
        }
        // 常见：04-13 05:34（无年）
        if (v.length() == 11 && v.charAt(2) == '-' && v.charAt(5) == ' ') {
            try {
                int y = Year.now().getValue();
                return LocalDateTime.parse(y + "-" + v, DT2.withLocale(Locale.ROOT));
            } catch (DateTimeParseException ignored) {
            }
        }
        // 常见：今天 10:26 / 昨天 23:10（无日期）
        if (v.startsWith("今天")) {
            LocalTime t = tryParseLocalTime(v.substring(2).trim());
            if (t != null) {
                return LocalDateTime.of(LocalDate.now(), t);
            }
        }
        if (v.startsWith("昨天")) {
            LocalTime t = tryParseLocalTime(v.substring(2).trim());
            if (t != null) {
                return LocalDateTime.of(LocalDate.now().minusDays(1), t);
            }
        }
        // 常见：10:26（仅时间）
        if (v.length() == 5 && v.charAt(2) == ':') {
            LocalTime t = tryParseLocalTime(v);
            if (t != null) {
                return LocalDateTime.of(LocalDate.now(), t);
            }
        }
        // 常见：2026年04月25日 10:26 / 04月25日 10:26
        LocalDateTime cn = tryParseChineseDateTime(v);
        if (cn != null) {
            return cn;
        }
        for (DateTimeFormatter f : List.of(DT1, DT2, DT3, DT4)) {
            try {
                return LocalDateTime.parse(v, f);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static LocalTime tryParseLocalTime(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.length() != 5 || v.charAt(2) != ':') {
            return null;
        }
        try {
            return LocalTime.parse(v, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDateTime tryParseChineseDateTime(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty()) {
            return null;
        }
        try {
            // 2026年04月25日 10:26(:30 可选)
            if (v.contains("年") && v.contains("月") && v.contains("日")) {
                DateTimeFormatter f = v.contains(":") && v.length() >= 19
                        ? DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")
                        : DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
                return LocalDateTime.parse(v, f);
            }
            // 04月25日 10:26
            if (!v.contains("年") && v.contains("月") && v.contains("日")) {
                int y = Year.now().getValue();
                String norm = y + "年" + v;
                DateTimeFormatter f = norm.contains(":") && norm.length() >= 19
                        ? DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")
                        : DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
                return LocalDateTime.parse(norm, f);
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String sha1Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dig = md.digest((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 理论不可能
            throw new IllegalStateException(e);
        }
    }

    public static final class SyncResult {
        private final int upsertPosts;
        private final int upsertComments;

        public SyncResult(int upsertPosts, int upsertComments) {
            this.upsertPosts = upsertPosts;
            this.upsertComments = upsertComments;
        }

        public int getUpsertPosts() {
            return upsertPosts;
        }

        public int getUpsertComments() {
            return upsertComments;
        }
    }
}

