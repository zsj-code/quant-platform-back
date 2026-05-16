package com.quant.platform.business.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.research.dto.ResearchReportItemDTO;
import com.quant.platform.business.research.dto.ResearchReportPageDTO;
import com.quant.platform.common.config.endpoints.EastmoneyResearchReportEndpoints;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 上市公司研报列表拉取（东财 reportapi），不下载 PDF。
 * <p>
 * 请求需适度限频，避免对源站造成压力。
 */
@Component
public class EastmoneyResearchReportClient {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyResearchReportClient(@Qualifier("eastmoneyReportWebClient") WebClient webClient) {
        this.objectMapper = new ObjectMapper();
        this.webClient = webClient;
    }

    /**
     * 个股研报分页（POST {@code /report/list2}）。
     *
     * @param stockCode
     *            6 位或带交易所后缀，内部会规范为 6 位
     * @param pageNo
     *            从 1 开始
     * @param pageSize
     *            每页条数
     * @param beginTime
     *            开始日期（含）
     * @param endTime
     *            结束日期（含）
     */
    public ResearchReportPageDTO fetchStockResearchReports(String stockCode, int pageNo, int pageSize,
                                                           LocalDate beginTime, LocalDate endTime) {
        String code = CommonUtil.normalizeSixDigitCode(stockCode);
        if (code == null || code.isEmpty()) {
            return new ResearchReportPageDTO(pageNo, pageSize, 0, List.of());
        }
        LocalDate beg = beginTime != null ? beginTime : LocalDate.now().minusYears(1);
        LocalDate end = endTime != null ? endTime : LocalDate.now();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("pageSize", pageSize);
        body.put("pageNo", pageNo);
        body.put("beginTime", beg.format(ISO_DATE));
        body.put("endTime", end.format(ISO_DATE));
        body.put("code", code);
        body.put("industryCode", "*");
        body.put("rating", "*");
        body.put("ratingChange", "*");
        body.put("orgCode", "");
        body.put("rcode", "");

        String raw = postJson(EastmoneyResearchReportEndpoints.PATH_LIST2, body);
        return parsePage(raw, pageNo, pageSize);
    }

    /**
     * 行业研报分页（GET {@code /report/list}），{@code industryCode} 为 {@code *} 表示不限行业。
     */
    public ResearchReportPageDTO fetchIndustryResearchReports(String industryCode, int pageNo, int pageSize,
            LocalDate beginTime, LocalDate endTime) {
        LocalDate beg = beginTime != null ? beginTime : LocalDate.now().minusYears(1);
        LocalDate end = endTime != null ? endTime : LocalDate.now();
        String ic = industryCode == null || industryCode.isBlank() ? "*" : industryCode.trim();
        String path = UriComponentsBuilder.fromPath(EastmoneyResearchReportEndpoints.PATH_LIST)
                .queryParam("pageSize", pageSize).queryParam("pageNo", pageNo)
                .queryParam("beginTime", beg.format(ISO_DATE)).queryParam("endTime", end.format(ISO_DATE))
                .queryParam("qType", "1").queryParam("industryCode", ic).queryParam("fields", "").build(true)
                .toUriString();

        String raw = get(path);
        return parsePage(raw, pageNo, pageSize);
    }

    private ResearchReportPageDTO parsePage(String raw, int pageNo, int pageSize) {
        if (raw == null || raw.isBlank()) {
            return new ResearchReportPageDTO(pageNo, pageSize, 0, List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode dataArr = root.path("data");
            long total = root.path("total").asLong(0);
            if (total == 0) {
                total = root.path("hits").asLong(0);
            }
            List<ResearchReportItemDTO> list = new ArrayList<>();
            if (dataArr.isArray()) {
                for (JsonNode n : dataArr) {
                    list.add(parseItem(n));
                }
            }
            return new ResearchReportPageDTO(pageNo, pageSize, total, list);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse eastmoney research report response", e);
        }
    }

    private static ResearchReportItemDTO parseItem(JsonNode n) {
        String indCode = firstNonBlank(text(n, "industryCode"), text(n, "industry_code"));
        String rating = firstNonBlank(text(n, "ratingName"), text(n, "emRatingName"), text(n, "sRatingName"),
                text(n, "lastEmRatingName"));
        return new ResearchReportItemDTO(text(n, "title"), text(n, "orgSName"), text(n, "publishDate"),
                text(n, "industryName"), indCode, text(n, "stockName"), text(n, "stockCode"), text(n, "infoCode"),
                text(n, "encodeUrl"), rating, text(n, "column"), text(n, "reportType"));
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String c : candidates) {
            if (c != null && !c.isBlank()) {
                return c;
            }
        }
        return null;
    }

    private static String text(JsonNode n, String field) {
        if (n == null || n.isMissingNode()) {
            return null;
        }
        String v = n.path(field).asText(null);
        return v == null || v.isEmpty() ? null : v;
    }

    private String postJson(String path, Map<String, Object> body) {
        try {
            return webClient.post().uri(path).contentType(MediaType.APPLICATION_JSON).bodyValue(body).retrieve()
                    .bodyToMono(String.class).timeout(Duration.ofSeconds(15))
                    .onErrorResume(WebClientResponseException.class,
                            e -> Mono.error(new IllegalStateException("Eastmoney report HTTP error: "
                                    + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(), e)))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney research report POST failed: " + path, e);
        }
    }

    private String get(String pathAndQuery) {
        try {
            return webClient.get().uri(pathAndQuery).retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(15))
                    .onErrorResume(WebClientResponseException.class,
                            e -> Mono.error(new IllegalStateException("Eastmoney report HTTP error: "
                                    + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(), e)))
                    .retryWhen(Retry.backoff(2, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney research report GET failed: " + pathAndQuery, e);
        }
    }
}
