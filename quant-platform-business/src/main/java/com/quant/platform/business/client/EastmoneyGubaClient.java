package com.quant.platform.business.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.client.dto.GubaCommentDTO;
import com.quant.platform.business.client.dto.GubaIndexSeriesDTO;
import com.quant.platform.business.client.dto.GubaPostDTO;
import com.quant.platform.business.client.dto.GubaPostDetailDTO;
import com.quant.platform.common.config.endpoints.EastmoneyGubaEndpoints;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 东方财富股吧（guba.eastmoney.com）帖子与评论获取。
 * <p>
 * 股吧页面以 HTML 为主，DOM 结构可能变动；本客户端采用「尽力解析」策略：
 * - 帖子列表：通过 URL 规则匹配 news 链接提取 postId/title
 * - 评论：从详情页常见评论容器中提取（若结构变化则可能为空）
 */
@Component
public class EastmoneyGubaClient {

    private static final Pattern POST_URL_PATTERN = Pattern.compile("/news,(\\d{6}),(\\d+)\\.html");
    private static final Pattern CAIFUHAO_URL_PATTERN = Pattern.compile(
            "https?://caifuhao\\.eastmoney\\.com/news/(\\d+)(?:\\?.*)?$");
    private static final Pattern CAIFUHAO_ID_PATTERN = Pattern.compile("^cfh_(\\d+)$");
    private static final String CAIFUHAO_NEWS_PREFIX = "https://caifuhao.eastmoney.com/news/";
    private static final String GET_DATA_PATH = "/api/getData";
    private static final String REPLY_LIST_PATH = "reply/api/Reply/ArticleNewReplyList";
    private static final String INDEX_DATA_PATH = "data/api/Data/GetIndexData";
    private static final Pattern TIME_MM_DD_HH_MM = Pattern.compile("\\b\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}\\b");
    private static final Pattern TIME_YYYY_MM_DD = Pattern.compile("\\b\\d{4}[-/]\\d{2}[-/]\\d{2}\\b");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyGubaClient(@Qualifier("eastmoneyGubaWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 股吧指数/情绪等聚合指标（来自 {@code /api/getData?path=data/api/Data/GetIndexData}）。
     * <p>
     * 该接口的参数/字段可能随站点策略调整，本方法返回原始 JSON，供上层按需要取字段。
     *
     * @param params 传给前端网关的 query 串参数对象（会被拼成 {@code k=v&...} 并置于 form 的 {@code param=} 中）
     */
    public JsonNode fetchIndexDataRaw(Map<String, String> params) {
        // 站点前端请求该接口常见 env=2
        return postGetData(INDEX_DATA_PATH, params, 2);
    }

    /**
     * 获取股吧情绪/指数时间序列（常见入参：{@code day=1/7/30}）。
     * <p>
     * 注意：返回结构可能随站点变化；如需更强兼容性可用 {@link #fetchIndexDataRaw(Map)}。
     */
    public GubaIndexSeriesDTO fetchIndexSeries(int day) {
        int d = Math.max(1, day);
        JsonNode raw = fetchIndexDataRaw(Map.of("day", String.valueOf(d)));
        if (raw == null) {
            return null;
        }
        // 兼容返回形态差异：有的返回是 re:[{time,value}...]；有的返回是 re:true,result:[...]
        JsonNode reNode = raw.get("re");
        if (reNode == null || !reNode.isArray()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(raw, GubaIndexSeriesDTO.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 帖子列表页解析（尽力）。
     *
     * @param secCode
     *            6 位证券代码
     * @param page
     *            从 1 开始
     */
    public List<GubaPostDTO> listPosts(String secCode, int page) {
        String path = EastmoneyGubaEndpoints.listPagePath(secCode, page);
        String html = getHtml(path);
        if (html == null || html.isBlank()) {
            return List.of();
        }
        Document doc = Jsoup.parse(html, EastmoneyGubaEndpoints.BASE_GUBA);

        // 用 href 规则匹配帖子链接，并按出现顺序去重：
        // - 股吧帖子：/news,{secCode},{postId}.html
        // - 财富号： https://caifuhao.eastmoney.com/news/{id}?from=guba...
        Map<String, GubaPostDTO> out = new LinkedHashMap<>();
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            if (href == null || href.isBlank()) {
                continue;
            }
            String abs = a.absUrl("href");
            if (abs == null || abs.isBlank()) {
                // 兜底：少数 href 可能是相对路径但 absUrl 解析失败
                abs = href;
            }

            String code = secCode;
            String postId = null;

            Matcher m = POST_URL_PATTERN.matcher(href);
            if (m.find()) {
                code = m.group(1);
                postId = m.group(2);
            } else {
                Matcher cfh = CAIFUHAO_URL_PATTERN.matcher(abs);
                if (cfh.find()) {
                    // 与股吧 postId 取值域隔离，避免冲突
                    postId = "cfh_" + cfh.group(1);
                } else {
                    continue;
                }
            }

            String title = cleanText(a.attr("title"));
            if (title == null || title.isBlank()) {
                title = cleanText(a.text());
            }
            if (title == null || title.isBlank()) {
                continue;
            }
            PostMeta meta = extractPostMetaNearAnchor(a);
            // 同一 postId 在列表 HTML 中可能重复出现（多条 a 指向同一帖），保留首次解析结果。
            out.putIfAbsent(postId, GubaPostDTO.builder().secCode(code).postId(postId).title(title).url(abs)
                    .author(meta.author).publishTimeText(meta.publishTimeText).readCount(meta.readCount)
                    .commentCount(meta.commentCount).build());
        }
        return List.copyOf(out.values());
    }

    /**
     * 拉取帖子详情页并尽力解析正文与评论。
     */
    public GubaPostDetailDTO fetchPostDetail(String secCode, String postId) {
        Matcher cfhId = CAIFUHAO_ID_PATTERN.matcher(postId == null ? "" : postId.trim());
        if (cfhId.find()) {
            String id = cfhId.group(1);
            String url = CAIFUHAO_NEWS_PREFIX + id;
            String html = getHtml(url);
            if (html == null || html.isBlank()) {
                return GubaPostDetailDTO.builder()
                        .post(GubaPostDTO.builder().secCode(secCode).postId(postId).url(url).build())
                        .contentText("").comments(List.of()).build();
            }
            Document doc = Jsoup.parse(html, url);
            String title = cleanText(doc.selectFirst("h1") != null ? doc.selectFirst("h1").text() : null);
            if (title == null || title.isBlank()) {
                title = cleanText(doc.title());
            }
            PostMeta meta = extractPostMetaFromDetail(doc);
            String content = extractMainContent(doc);
            // 财富号评论多为异步/接口加载，此处先返回空（不阻断同步流程）
            return GubaPostDetailDTO.builder()
                    .post(GubaPostDTO.builder().secCode(secCode).postId(postId).title(title).url(url).author(meta.author)
                            .publishTimeText(meta.publishTimeText).build())
                    .contentText(content).comments(List.of()).build();
        }

        String path = EastmoneyGubaEndpoints.postDetailPath(secCode, postId);
        String html = getHtml(path);
        if (html == null || html.isBlank()) {
            return GubaPostDetailDTO.builder()
                    .post(GubaPostDTO.builder().secCode(secCode).postId(postId)
                            .url(EastmoneyGubaEndpoints.BASE_GUBA + path).build())
                    .contentText("").comments(List.of()).build();
        }
        Document doc = Jsoup.parse(html, EastmoneyGubaEndpoints.BASE_GUBA);

        String title = cleanText(doc.selectFirst("h1") != null ? doc.selectFirst("h1").text() : null);
        if (title == null || title.isBlank()) {
            title = cleanText(doc.title());
        }
        PostMeta meta = extractPostMetaFromDetail(doc);
        String content = extractMainContent(doc);
        // 评论优先走接口（HTML replylist 常为空，评论由前端异步加载）
        List<GubaCommentDTO> comments = fetchCommentsByApi(postId, 1, 30);
        if (comments.isEmpty()) {
            comments = extractComments(doc, postId);
        }

        GubaPostDTO post = GubaPostDTO.builder().secCode(secCode).postId(postId).title(title).author(meta.author)
                .publishTimeText(meta.publishTimeText)
                .url(EastmoneyGubaEndpoints.BASE_GUBA + path).build();
        return GubaPostDetailDTO.builder().post(post).contentText(content).comments(comments).build();
    }

    /**
     * 从列表页某个链接附近尽力抽取作者/时间/阅读/评论。
     * <p>
     * 股吧列表页 DOM 波动较大：既可能是 table/tr，也可能是 div 卡片；此处采用「就近上下文」启发式。
     */
    private static PostMeta extractPostMetaNearAnchor(Element a) {
        if (a == null) {
            return PostMeta.empty();
        }

        // 优先：在同一行/卡片内查找
        Element ctx = firstNonNull(a.closest("tr"), a.closest(".articleh"), a.closest(".article-item"),
                a.closest(".article"), a.parent());
        if (ctx == null) {
            return PostMeta.empty();
        }

        String author = null;
        Element authorEl = firstNonNull(ctx.selectFirst("a[href*=/user/]"), ctx.selectFirst("a.user"),
                ctx.selectFirst(".author a"), ctx.selectFirst(".l4 a"), ctx.selectFirst("td.author a"));
        if (authorEl != null) {
            author = cleanText(authorEl.text());
        } else {
            Element authorText = firstNonNull(ctx.selectFirst(".author"), ctx.selectFirst(".l4"), ctx.selectFirst("td.author"));
            if (authorText != null) {
                author = cleanText(authorText.text());
            }
        }

        String time = null;

        Integer read = null;
        Integer comment = null;
        // 若是 table/tr：通常前两列是阅读/评论
        if ("tr".equalsIgnoreCase(ctx.tagName())) {
            Elements tds = ctx.select("td");
            if (tds.size() >= 2) {
                read = tryParseInt(cleanText(tds.get(0).text()));
                comment = tryParseInt(cleanText(tds.get(1).text()));
            }
            // 列表表头一般为：阅读 / 评论 / 标题 / 作者 / 最后更新
            if (tds.size() >= 5) {
                String lastCol = cleanText(tds.get(tds.size() - 1).text());
                time = pickTimeText(lastCol);
            }
        } else {
            // div 卡片：尝试在上下文里找数字
            Element readEl = firstNonNull(ctx.selectFirst(".read"), ctx.selectFirst(".l1"), ctx.selectFirst(".l2"));
            if (readEl != null) {
                read = tryParseInt(cleanText(readEl.text()));
            }
            Element cEl = firstNonNull(ctx.selectFirst(".comment"), ctx.selectFirst(".l2"), ctx.selectFirst(".l3"));
            if (cEl != null) {
                comment = tryParseInt(cleanText(cEl.text()));
            }

            Element timeEl = firstNonNull(ctx.selectFirst(".l5"), ctx.selectFirst(".l6"), ctx.selectFirst(".update"),
                    ctx.selectFirst("span.time"));
            if (timeEl != null) {
                time = pickTimeText(cleanText(timeEl.text()));
            }
        }

        return new PostMeta(author, time, read, comment);
    }

    /**
     * 只接受可识别的时间片段，避免把整行/作者列误当时间。
     */
    private static String pickTimeText(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        // 先匹配常见的“04-13 05:34”
        Matcher m1 = TIME_MM_DD_HH_MM.matcher(s);
        if (m1.find()) {
            return m1.group(0);
        }
        // 再匹配“2026-04-13”或“2026/04/13”并返回整段（可能含时间）
        Matcher m2 = TIME_YYYY_MM_DD.matcher(s);
        if (m2.find()) {
            // 如果包含到分钟的时间，则截到分钟
            Matcher m3 = Pattern.compile("\\b\\d{4}[-/]\\d{2}[-/]\\d{2}\\s+\\d{2}:\\d{2}(?::\\d{2})?\\b")
                    .matcher(s);
            if (m3.find()) {
                return m3.group(0);
            }
            return m2.group(0);
        }
        return null;
    }

    /**
     * 从详情页尽力抽取作者/时间。
     */
    private static PostMeta extractPostMetaFromDetail(Document doc) {
        if (doc == null) {
            return PostMeta.empty();
        }
        String author = null;
        Element a1 = firstNonNull(doc.selectFirst(".zwname a"), doc.selectFirst(".author a"), doc.selectFirst(".user a"),
                doc.selectFirst("a[href*=/user/]"));
        if (a1 != null) {
            author = cleanText(a1.text());
        }

        String time = null;
        Element t1 = firstNonNull(doc.selectFirst(".zwfbtime"), doc.selectFirst(".time"), doc.selectFirst("span.time"),
                doc.selectFirst("div:matchesOwn(\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2})"));
        if (t1 != null) {
            String raw = cleanText(t1.text());
            // 财富号常见“修改于 2026年04月13日 20:02 ...”
            if (raw != null && raw.length() > 0 && raw.length() <= 64) {
                time = raw;
            }
        }
        return new PostMeta(author, time, null, null);
    }

    private static String extractMainContent(Document doc) {
        // 常见正文容器（股吧/资讯页可能不同）
        Element body = firstNonNull(doc.selectFirst("#zwcon"), doc.selectFirst("#post_content"),
                doc.selectFirst(".newstext"), doc.selectFirst(".article-content"), doc.selectFirst(".article-body"),
                doc.selectFirst(".articleBody"), doc.selectFirst(".xeditor"));
        if (body == null) {
            // 退化：抽取 meta 描述
            Element meta = doc.selectFirst("meta[name=description]");
            return cleanText(meta == null ? "" : meta.attr("content"));
        }
        return cleanText(body.text());
    }

    private static List<GubaCommentDTO> extractComments(Document doc, String postId) {
        List<GubaCommentDTO> out = new ArrayList<>();

        // 常见评论列表容器（结构可能变）
        Elements candidates = new Elements();
        candidates.addAll(doc.select("#replylist .reply_item"));
        candidates.addAll(doc.select(".replylist .reply_item"));
        candidates.addAll(doc.select("#reply_list .reply_item"));
        candidates.addAll(doc.select(".reply_list .reply_item"));
        candidates.addAll(doc.select(".reply_item"));

        if (candidates.isEmpty()) {
            return List.of();
        }

        int floor = 0;
        for (Element item : candidates) {
            String content = null;
            Element c1 = firstNonNull(item.selectFirst(".reply_content"), item.selectFirst(".short_text"),
                    item.selectFirst(".content"), item.selectFirst("p"));
            if (c1 != null) {
                content = cleanText(c1.text());
            }
            if (content == null || content.isBlank()) {
                continue;
            }
            floor++;

            String author = null;
            Element a1 = firstNonNull(item.selectFirst(".reply_user"), item.selectFirst(".author"),
                    item.selectFirst("a.user"), item.selectFirst("a"));
            if (a1 != null) {
                author = cleanText(a1.text());
            }

            String time = null;
            Element t1 = firstNonNull(item.selectFirst(".reply_time"), item.selectFirst(".time"),
                    item.selectFirst("span.time"));
            if (t1 != null) {
                time = cleanText(t1.text());
            }

            String commentId = null;
            if (item.hasAttr("data-replyid")) {
                commentId = cleanText(item.attr("data-replyid"));
            } else if (item.hasAttr("data-id")) {
                commentId = cleanText(item.attr("data-id"));
            }

            out.add(GubaCommentDTO.builder().postId(postId).commentId(commentId).floorNo(floor).author(author)
                    .publishTimeText(time).contentText(content).build());
        }
        return List.copyOf(out);
    }

    /**
     * 通过股吧统一网关 {@code /api/getData} 拉取评论列表（尽力）。
     * <p>
     * 注意：该接口可能受站点策略影响；失败时返回空列表并由调用方回退到 HTML 解析。
     */
    private List<GubaCommentDTO> fetchCommentsByApi(String postId, int page, int pageSize) {
        String pid = postId == null ? "" : postId.trim();
        if (pid.isEmpty() || pid.startsWith("cfh_")) {
            return List.of();
        }
        int p = Math.max(1, page);
        int ps = Math.max(1, Math.min(pageSize, 50));

        Map<String, String> args = new LinkedHashMap<>();
        args.put("postid", pid);
        args.put("sort", "1");
        args.put("sorttype", "1");
        args.put("p", String.valueOf(p));
        args.put("ps", String.valueOf(ps));

        JsonNode root = postGetData(REPLY_LIST_PATH, args, 1);
        if (root == null) {
            return List.of();
        }

        List<JsonNode> replyNodes = new ArrayList<>();
        collectObjectsWithField(root, "reply_text", replyNodes, 5000);
        if (replyNodes.isEmpty()) {
            return List.of();
        }

        List<GubaCommentDTO> out = new ArrayList<>();
        for (JsonNode n : replyNodes) {
            if (n == null || !n.isObject()) {
                continue;
            }
            String text = n.path("reply_text").asText(null);
            if (text == null || text.isBlank()) {
                continue;
            }
            String author = firstText(n, "user_nickname", "user_name", "userNickname", "nickname", "userNickName");
            if (author == null || author.isBlank()) {
                // 常见嵌套结构：user / post_user / reply_user
                author = firstText(n.path("user"), "user_nickname", "user_name", "nickname");
            }
            if (author == null || author.isBlank()) {
                author = firstText(n.path("post_user"), "user_nickname", "user_name", "nickname");
            }
            if (author == null || author.isBlank()) {
                author = firstText(n.path("reply_user"), "user_nickname", "user_name", "nickname");
            }
            if (author == null || author.isBlank()) {
                // 兜底：用 uid 作为作者占位（前端可再映射/补全）
                String uid = firstText(n, "user_id", "userid", "uid");
                author = uid == null ? null : ("uid_" + uid);
            }
            String time = n.path("reply_time").asText(null);
            if (time == null || time.isBlank()) {
                time = n.path("post_time").asText(null);
            }
            String cid = n.path("reply_id").asText(null);
            if (cid == null || cid.isBlank()) {
                cid = n.path("replyid").asText(null);
            }
            Integer like = null;
            if (n.has("like_count")) {
                like = n.path("like_count").isNumber() ? n.path("like_count").asInt() : null;
            } else if (n.has("like")) {
                like = n.path("like").isNumber() ? n.path("like").asInt() : null;
            }
            Integer floor = n.has("position") && n.path("position").isNumber() ? n.path("position").asInt() : null;
            out.add(GubaCommentDTO.builder().postId(pid).commentId(cid).floorNo(floor).author(author)
                    .publishTimeText(time).contentText(cleanText(text)).likeCount(like).build());
        }
        return List.copyOf(out);
    }

    private static String firstText(JsonNode obj, String... fields) {
        if (obj == null || fields == null) {
            return null;
        }
        for (String f : fields) {
            if (f == null || f.isBlank()) {
                continue;
            }
            JsonNode n = obj.get(f);
            if (n == null || n.isNull()) {
                continue;
            }
            String v = n.asText(null);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private JsonNode postGetData(String path, Map<String, String> paramObj) {
        return postGetData(path, paramObj, 1);
    }

    private JsonNode postGetData(String path, Map<String, String> paramObj, int env) {
        String p = path == null ? "" : path.trim();
        if (p.isEmpty()) {
            return null;
        }
        String param = buildQueryString(paramObj);
        String body = buildFormBody(p, param, env);
        String uri = GET_DATA_PATH + "?path=" + urlEncode(p);
        try {
            String raw = requestBodyWithRetry(webClient, uri, body).block();
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Mono<String> requestBodyWithRetry(WebClient client, String uri, String body) {
        return client.post().uri(uri).header("Content-Type", "application/x-www-form-urlencoded").bodyValue(body)
                .retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(15))
                .onErrorResume(WebClientResponseException.class,
                        e -> Mono.error(new IllegalStateException(
                                "Eastmoney guba HTTP error: " + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                                e)))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(400)).maxBackoff(Duration.ofSeconds(2))
                        .filter(ex -> ex instanceof WebClientRequestException));
    }

    private static String buildFormBody(String path, String paramQuery, int env) {
        // 对齐前端：param/plat/path/env/origin/version/product
        return "param=" + urlEncode(paramQuery == null ? "" : paramQuery) + "&plat=Web&path=" + urlEncode(path)
                + "&env=" + Math.max(1, env) + "&origin=&version=2022&product=Guba";
    }

    private static String buildQueryString(Map<String, String> m) {
        if (m == null || m.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (e == null) {
                continue;
            }
            String k = e.getKey();
            if (k == null || k.isBlank()) {
                continue;
            }
            String v = e.getValue() == null ? "" : e.getValue();
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(k.trim()).append('=').append(v);
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static void collectObjectsWithField(JsonNode node, String field, List<JsonNode> out, int max) {
        if (node == null || out.size() >= max) {
            return;
        }
        if (node.isObject()) {
            if (node.has(field)) {
                out.add(node);
                if (out.size() >= max) {
                    return;
                }
            }
            node.fields().forEachRemaining(e -> collectObjectsWithField(e.getValue(), field, out, max));
            return;
        }
        if (node.isArray()) {
            for (JsonNode n : node) {
                collectObjectsWithField(n, field, out, max);
                if (out.size() >= max) {
                    return;
                }
            }
        }
    }

    private String getHtml(String path) {
        try {
            return requestHtmlWithRetry(webClient, path).block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney guba request failed: " + path, e);
        }
    }

    private static Mono<String> requestHtmlWithRetry(WebClient client, String path) {
        return client.get().uri(path).retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(15))
                .onErrorResume(WebClientResponseException.class,
                        e -> Mono.error(new IllegalStateException(
                                "Eastmoney guba HTTP error: " + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                                e)))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(400)).maxBackoff(Duration.ofSeconds(2))
                        .filter(ex -> ex instanceof WebClientRequestException));
    }

    private static String cleanText(String s) {
        if (s == null) {
            return null;
        }
        String t = s.replace('\u00A0', ' ').trim();
        return t;
    }

    private static Integer tryParseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char ch = v.charAt(i);
            if (ch >= '0' && ch <= '9') {
                digits.append(ch);
            }
        }
        if (digits.length() == 0) {
            return null;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... items) {
        if (items == null) {
            return null;
        }
        for (T i : items) {
            if (i != null) {
                return i;
            }
        }
        return null;
    }

    private static final class PostMeta {
        final String author;
        final String publishTimeText;
        final Integer readCount;
        final Integer commentCount;

        PostMeta(String author, String publishTimeText, Integer readCount, Integer commentCount) {
            this.author = author;
            this.publishTimeText = publishTimeText;
            this.readCount = readCount;
            this.commentCount = commentCount;
        }

        static PostMeta empty() {
            return new PostMeta(null, null, null, null);
        }
    }
}

