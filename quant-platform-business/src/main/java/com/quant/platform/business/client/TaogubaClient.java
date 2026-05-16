package com.quant.platform.business.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.client.dto.TaogubaBarItemDTO;
import com.quant.platform.business.client.dto.TaogubaTopicReplyDTO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 淘股吧：个股页 {@code /quotes/{sh600000}} 内嵌 {@code var coolAttr = [ ... ]} 拉主帖/转发流；回帖自 {@code /a/{newTopicId}} 系列页 HTML 解析。
 * <p>
 * 注意：PC 端「翻页更多」由站点私有脚本请求，本实现仅取首屏流（页内约 20 条），与 {@code listPageSize} 配置一致时足够覆盖近几日活跃标的；若需更深翻页需后续对接内部或抓包接口。
 */
public class TaogubaClient {

    private static final Logger log = LoggerFactory.getLogger(TaogubaClient.class);
    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter REPLY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TOPIC_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern FLOOR = Pattern.compile("第(\\d+)楼");
    private static final String COOL_MARKER = "var coolAttr = ";
    private static final Pattern TOPIC_TIME_RE = Pattern.compile("\\b20\\d{2}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}\\b");
    private static final Pattern READ_RE = Pattern.compile("浏览\\s*(\\d+)");
    private static final Pattern COMMENT_RE = Pattern.compile("评论\\s*(\\d+)");
    // 帖子头部常见「加油 0/0」，这里取第一个数字作为点赞/加油数落库到 like_count
    private static final Pattern LIKE_RE = Pattern.compile("(?:点赞|加油)\\s*(\\d+)");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public TaogubaClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 行情页内嵌「最热」列表（单页）。
     *
     * @param tgbFullCode 小写全码，如 sh600000、sz000001
     */
    public List<TaogubaBarItemDTO> listStockBarCool(String tgbFullCode) {
        if (!StringUtils.hasText(tgbFullCode)) {
            return List.of();
        }
        String code = tgbFullCode.trim().toLowerCase();
        String path = "/quotes/" + code;
        String html = getHtml(path);
        if (html == null || html.isBlank()) {
            return List.of();
        }
        return parseCoolAttrArray(html);
    }

    /**
     * 某帖下回帖（一页），{@code page} 从 1 起：1 为 {@code /a/{id}}，2+ 为 {@code /a/{id}-{page}}，带 {@code type=X} 以与 PC「按时间」一致。
     */
    public List<TaogubaTopicReplyDTO> listTopicRepliesPage(String newTopicId, int page) {
        if (!StringUtils.hasText(newTopicId)) {
            return List.of();
        }
        int p = Math.max(1, page);
        String path = p == 1 ? "/a/" + newTopicId.trim() + "?type=X" : "/a/" + newTopicId.trim() + "-" + p + "?type=X";
        String html = getHtml(path);
        if (html == null || html.isBlank()) {
            return List.of();
        }
        return parseTopicRepliesHtml(html);
    }

    /**
     * 帖子详情页头部统计：浏览/评论/加油(点赞) 与发布时间。
     */
    public TopicStats fetchTopicStats(String newTopicId) {
        if (!StringUtils.hasText(newTopicId)) {
            return null;
        }
        String path = "/a/" + newTopicId.trim();
        String html = getHtml(path);
        if (html == null || html.isBlank()) {
            return null;
        }
        return parseTopicStats(html);
    }

    private String getHtml(String path) {
        try {
            return webClient.get().uri(path).header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,*/*;q=0.8")
                    .header(HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header(HttpHeaders.REFERER, "https://www.tgb.cn/quotes/").retrieve().bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(25))
                    .retryWhen(Retry.backoff(1, Duration.ofMillis(400))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .onErrorReturn("").block();
        } catch (Exception e) {
            log.debug("Taoguba GET {}: {}", path, e.getMessage());
            return "";
        }
    }

    private TopicStats parseTopicStats(String html) {
        Document doc = Jsoup.parse(html);
        String text = doc.text();
        if (text == null) {
            return null;
        }
        TopicStats s = new TopicStats();
        s.readCount = firstInt(text, READ_RE);
        s.commentCount = firstInt(text, COMMENT_RE);
        s.likeCount = firstInt(text, LIKE_RE);

        Matcher tm = TOPIC_TIME_RE.matcher(text);
        if (tm.find()) {
            try {
                // 帖子页时间不带 offset，按上海时区理解即可
                s.publishTime = LocalDateTime.parse(tm.group(), TOPIC_TIME);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (s.readCount == null && s.commentCount == null && s.likeCount == null && s.publishTime == null) {
            return null;
        }
        return s;
    }

    private static Integer firstInt(String text, Pattern p) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher m = p.matcher(text);
        if (!m.find()) {
            return null;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<TaogubaBarItemDTO> parseCoolAttrArray(String html) {
        int mark = html.indexOf(COOL_MARKER);
        if (mark < 0) {
            return List.of();
        }
        int start = html.indexOf('[', mark);
        if (start < 0) {
            return List.of();
        }
        int end = endIndexOfJsonArray(html, start);
        if (end < 0) {
            return List.of();
        }
        String json = html.substring(start, end + 1);
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            log.debug("parse coolAttr JSON: {}", e.getMessage());
            return List.of();
        }
        if (root == null || !root.isArray()) {
            return List.of();
        }
        List<TaogubaBarItemDTO> out = new ArrayList<>();
        for (JsonNode n : root) {
            if (n == null || !n.isObject()) {
                continue;
            }
            String rType = textOr(n, "rType", "rtype");
            if ("B".equalsIgnoreCase(rType) || "CLS".equalsIgnoreCase(rType)) {
                continue;
            }
            if (!"T".equalsIgnoreCase(rType) && !"R".equalsIgnoreCase(rType)) {
                continue;
            }
            String newTopic = n.path("newTopicID").asText("");
            if (!StringUtils.hasText(newTopic) || "null".equals(newTopic)) {
                continue;
            }
            OffsetDateTime actionTime = parseOffsetDateTime(n.path("actionDate").asText(null));
            if (actionTime == null) {
                continue;
            }
            TaogubaBarItemDTO d = new TaogubaBarItemDTO();
            d.setNewTopicId(newTopic);
            d.setRType(rType);
            d.setSubject(htmlToText(n.path("subject").asText("")));
            d.setBody(htmlToText(n.path("body").asText("")));
            d.setUserName(n.path("userName").asText(""));
            d.setActionTime(actionTime);
            d.setReplyNum(n.path("replyNum").asInt(0));
            d.setViewNum(n.path("viewNum").asInt(0));
            d.setLikeCount(n.path("usefulNum").asInt(0));
            out.add(d);
        }
        return out;
    }

    private static String textOr(JsonNode n, String a, String b) {
        String t = n.path(a).asText("");
        if (StringUtils.hasText(t)) {
            return t;
        }
        return n.path(b).asText("");
    }

    private static OffsetDateTime parseOffsetDateTime(String s) {
        if (!StringUtils.hasText(s) || "null".equals(s)) {
            return null;
        }
        try {
            // 站点常见格式：2026-04-21T08:40:39.000+08:00（带 offset，不是 ISO_INSTANT 的 Z 结尾）
            return OffsetDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static int endIndexOfJsonArray(String s, int arrayStart) {
        if (arrayStart < 0 || arrayStart >= s.length() || s.charAt(arrayStart) != '[') {
            return -1;
        }
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = arrayStart; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static String htmlToText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        if (!raw.contains("<")) {
            return raw;
        }
        return Jsoup.parseBodyFragment(raw).text();
    }

    private List<TaogubaTopicReplyDTO> parseTopicRepliesHtml(String html) {
        Document doc = Jsoup.parse(html);
        Elements blocks = doc.select("div.comment-data");
        List<TaogubaTopicReplyDTO> out = new ArrayList<>();
        for (Element block : blocks) {
            Element textEl = block.selectFirst("div.comment-data-text[id^=reply]");
            if (textEl == null) {
                continue;
            }
            String idAttr = textEl.id();
            if (!idAttr.startsWith("reply") || idAttr.length() <= 5) {
                continue;
            }
            long rid;
            try {
                rid = Long.parseLong(idAttr.substring("reply".length()));
            } catch (NumberFormatException e) {
                continue;
            }
            String text = textEl.text();
            if (text == null || text.isBlank()) {
                continue;
            }
            Element an = block.selectFirst("a.user-name");
            String uname = an == null ? "" : an.text().trim();
            LocalDateTime publishTime = null;
            Element span = block.selectFirst("span.pcyclspan");
            if (span != null) {
                publishTime = parseReplyTime(span.text().trim());
            }
            Integer floor = null;
            String btn = block.selectFirst("div.comment-data-button") == null ? ""
                    : block.selectFirst("div.comment-data-button").text();
            if (StringUtils.hasText(btn)) {
                Matcher m = FLOOR.matcher(btn);
                if (m.find()) {
                    try {
                        floor = Integer.parseInt(m.group(1));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            TaogubaTopicReplyDTO d = new TaogubaTopicReplyDTO();
            d.setReplyId(rid);
            d.setUserName(uname);
            d.setText(text);
            d.setPublishTime(publishTime);
            d.setFloorNo(floor);
            d.setLikeCount(0);
            out.add(d);
        }
        return out;
    }

    private LocalDateTime parseReplyTime(String t) {
        if (!StringUtils.hasText(t)) {
            return null;
        }
        try {
            return LocalDateTime.parse(t, REPLY_TIME);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String postUrl(String newTopicId) {
        if (!StringUtils.hasText(newTopicId)) {
            return "";
        }
        return "https://www.tgb.cn/a/" + newTopicId.trim();
    }

    public static final class TopicStats {
        private Integer readCount;
        private Integer commentCount;
        private Integer likeCount;
        private LocalDateTime publishTime;

        public Integer getReadCount() {
            return readCount;
        }

        public Integer getCommentCount() {
            return commentCount;
        }

        public Integer getLikeCount() {
            return likeCount;
        }

        public LocalDateTime getPublishTime() {
            return publishTime;
        }
    }
}
