package com.quant.platform.ai.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.ai.core.client.dto.EastmoneyPledgeRatioLatestDTO;
import com.quant.platform.common.config.endpoints.EastmoneyFinancialStatementEndpoints;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * 东方财富数据中心 {@link EastmoneyFinancialStatementEndpoints#REPORT_RPT_CSDC_LIST}：
 * 按 {@code TRADE_DATE} 倒序取最新一条质押比例 {@code PLEDGE_RATIO}。
 *
 * @see <a href="https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>
 */
@Component
public class EastmoneyPledgeRatioClient {

    /**
     * 与示例 URL 一致的下界日期；默认与 {@link #fetchLatestPledgeRatio(String)} 组合为
     * {@code TRADE_DATE > '2026-01-01'}（不含当日）。若需含当日请用 {@code inclusive=true}。
     */
    public static final LocalDate DEFAULT_TRADE_DATE_BOUNDARY = LocalDate.of(2026, 1, 1);

    private static final int PAGE_SIZE = 50;
    private static final DateTimeFormatter TRADE_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public EastmoneyPledgeRatioClient(@Qualifier("eastmoneyDatacenterWebClient") WebClient webClient) {
        this.objectMapper = new ObjectMapper();
        this.webClient = webClient;
    }

    /**
     * 等价于 {@link #fetchLatestPledgeRatio(String, LocalDate, boolean)}：
     * {@link #DEFAULT_TRADE_DATE_BOUNDARY} + {@code TRADE_DATE > boundary}（与常见浏览器请求一致）。
     */
    public Optional<EastmoneyPledgeRatioLatestDTO> fetchLatestPledgeRatio(String sixDigitOrSymbol) {
        return fetchLatestPledgeRatio(sixDigitOrSymbol, DEFAULT_TRADE_DATE_BOUNDARY, false);
    }

    /**
     * 查询指定证券在日期下界之后的质押记录，按 {@code TRADE_DATE} 倒序取第一条。
     *
     * @param sixDigitOrSymbol   6 位代码或带后缀代码，如 002456 / 002456.SZ
     * @param tradeDateBoundary  比较用日期（如 2026-01-01）
     * @param inclusive          {@code true}：{@code TRADE_DATE >= boundary}；{@code false}：{@code TRADE_DATE > boundary}（与示例 URL 一致）
     */
    public Optional<EastmoneyPledgeRatioLatestDTO> fetchLatestPledgeRatio(String sixDigitOrSymbol,
                                                                          LocalDate tradeDateBoundary,
                                                                          boolean inclusive) {
        String code = CommonUtil.normalizeSixDigitCode(sixDigitOrSymbol);
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("stockCode required, got: " + sixDigitOrSymbol);
        }
        if (tradeDateBoundary == null) {
            throw new IllegalArgumentException("tradeDateBoundary required");
        }

        String pathAndQuery = buildDatacenterQueryPath(code, tradeDateBoundary, inclusive);
        String body = requestBody(pathAndQuery);
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("success").asBoolean(false)) {
                String msg = root.path("message").asText("unknown");
                throw new IllegalStateException("Eastmoney datacenter returned success=false: " + msg);
            }
            JsonNode data = root.path("result").path("data");
            if (!data.isArray() || data.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = data.get(0);
            return Optional.of(parseRow(first, code));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Eastmoney pledge ratio response", e);
        }
    }

    /**
     * 构造与浏览器一致的 query：{@code filter} 仅对 {@code =}、{@code "}、日期引号做编码，勿对整段 URL 编码括号。
     *
     * @see com.quant.platform.business.client.EastmoneyFinancialStatementClient#buildDatacenterQueryPath
     */
    private static String buildDatacenterQueryPath(String sixDigitCode, LocalDate boundary, boolean inclusive) {
        var cs = StandardCharsets.UTF_8;
        String p = EastmoneyFinancialStatementEndpoints.DATA_V1_GET_PATH;
        String filter = pledgeFilterForQuery(boundary, sixDigitCode, inclusive);
        return p + "?sortColumns=" + UriUtils.encodeQueryParam("TRADE_DATE", cs) + "&sortTypes="
                + UriUtils.encodeQueryParam("-1", cs) + "&pageSize="
                + UriUtils.encodeQueryParam(String.valueOf(PAGE_SIZE), cs) + "&pageNumber="
                + UriUtils.encodeQueryParam("1", cs) + "&reportName="
                + UriUtils.encodeQueryParam(EastmoneyFinancialStatementEndpoints.REPORT_RPT_CSDC_LIST, cs)
                + "&columns=" + UriUtils.encodeQueryParam("ALL", cs) + "&quoteColumns=&source="
                + UriUtils.encodeQueryParam("WEB", cs) + "&client=" + UriUtils.encodeQueryParam("WEB", cs)
                + "&filter=" + filter;
    }

    /**
     * 生成 filter 片段（已分段编码）。{@code inclusive=false} 时与示例
     * {@code (TRADE_DATE>'2026-01-01')(SECURITY_CODE="002456")} 一致。
     */
    static String pledgeFilterForQuery(LocalDate boundary, String sixDigitCode, boolean inclusive) {
        String d = boundary.toString();
        String op = inclusive ? "%3E%3D" : "%3E";
        return "(TRADE_DATE" + op + "%27" + d + "%27)(SECURITY_CODE%3D%22" + sixDigitCode + "%22)";
    }

    private String requestBody(String pathAndQuery) {
        try {
            URI requestUri = URI.create(EastmoneyFinancialStatementEndpoints.BASE_DATACENTER_WEB + pathAndQuery);
            return webClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.REFERER, "https://data.eastmoney.com/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(WebClientResponseException.class,
                            e -> Mono.error(new IllegalStateException("Eastmoney datacenter HTTP error: "
                                    + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(), e)))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney datacenter request failed: " + pathAndQuery, e);
        }
    }

    private static EastmoneyPledgeRatioLatestDTO parseRow(JsonNode n, String fallbackCode) {
        String secCode = n.path("SECURITY_CODE").asText(fallbackCode);
        String name = n.path("SECURITY_NAME_ABBR").asText("");
        LocalDate tradeDate = parseTradeDate(n.path("TRADE_DATE").asText(null));
        BigDecimal ratio = null;
        if (n.path("PLEDGE_RATIO").isNumber()) {
            ratio = n.path("PLEDGE_RATIO").decimalValue();
        } else if (!n.path("PLEDGE_RATIO").asText("").isBlank()) {
            ratio = new BigDecimal(n.path("PLEDGE_RATIO").asText().trim());
        }
        return new EastmoneyPledgeRatioLatestDTO(secCode, name, tradeDate, ratio);
    }

    private static LocalDate parseTradeDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        try {
            if (t.length() >= 10 && t.charAt(4) == '-' && t.charAt(7) == '-') {
                if (t.length() == 10) {
                    return LocalDate.parse(t.substring(0, 10));
                }
                return LocalDateTime.parse(t, TRADE_DATE_FMT).toLocalDate();
            }
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(t.substring(0, Math.min(10, t.length())));
        } catch (Exception e) {
            return null;
        }
    }
}
