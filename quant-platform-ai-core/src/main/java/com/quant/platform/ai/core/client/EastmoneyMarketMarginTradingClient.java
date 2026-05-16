package com.quant.platform.ai.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.ai.core.client.dto.EastmoneyMarketMarginHistoryPageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyMarketMarginHistoryRowDTO;
import com.quant.platform.common.config.endpoints.EastmoneyFinancialStatementEndpoints;
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
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 东方财富数据中心 {@code RPTA_RZRQ_LSHJ}：全市场融资融券历史汇总（按 {@code dim_date} 倒序分页）。
 *
 * @see <a href="https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>
 */
@Component
public class EastmoneyMarketMarginTradingClient {

    public static final int DEFAULT_PAGE_SIZE = 50;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyMarketMarginTradingClient(@Qualifier("eastmoneyDatacenterWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分页查询全市场融资融券汇总，每页 {@link #DEFAULT_PAGE_SIZE} 条，从第 1 页起。
     */
    public EastmoneyMarketMarginHistoryPageDTO fetchMarketMarginHistory(int pageNumber) {
        return fetchMarketMarginHistory(pageNumber, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param pageNumber 页码，从 1 开始
     * @param pageSize   每页条数
     */
    public EastmoneyMarketMarginHistoryPageDTO fetchMarketMarginHistory(int pageNumber, int pageSize) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        String pathAndQuery = buildDatacenterQueryPath(pageNumber, pageSize);
        String body = requestBody(pathAndQuery);
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("success").asBoolean(false)) {
                String msg = root.path("message").asText("unknown");
                throw new IllegalStateException("Eastmoney datacenter returned success=false: " + msg);
            }
            JsonNode result = root.path("result");
            long total = result.path("count").asLong(0);
            int apiPages = result.path("pages").asInt(0);
            Integer totalPages = apiPages > 0
                    ? apiPages
                    : (pageSize > 0 && total > 0
                            ? BigDecimal.valueOf(total).divide(BigDecimal.valueOf(pageSize), 0, RoundingMode.CEILING)
                                    .intValue()
                            : null);
            JsonNode data = result.path("data");
            List<EastmoneyMarketMarginHistoryRowDTO> rows = parseDataRows(data);
            return new EastmoneyMarketMarginHistoryPageDTO(pageNumber, pageSize, total, totalPages, rows);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Eastmoney market margin history (RPTA_RZRQ_LSHJ)", e);
        }
    }

    private static String buildDatacenterQueryPath(int pageNumber, int pageSize) {
        var cs = StandardCharsets.UTF_8;
        String p = EastmoneyFinancialStatementEndpoints.DATA_V1_GET_PATH;
        String filter = UriUtils.encodeQueryParam("", cs);
        return p + "?reportName=" + UriUtils.encodeQueryParam(EastmoneyFinancialStatementEndpoints.REPORT_RPTA_RZRQ_LSHJ, cs)
                + "&columns=" + UriUtils.encodeQueryParam("ALL", cs) + "&source="
                + UriUtils.encodeQueryParam("WEB", cs) + "&sortColumns="
                + UriUtils.encodeQueryParam("dim_date", cs) + "&sortTypes="
                + UriUtils.encodeQueryParam("-1", cs) + "&pageNumber="
                + UriUtils.encodeQueryParam(String.valueOf(pageNumber), cs) + "&pageSize="
                + UriUtils.encodeQueryParam(String.valueOf(pageSize), cs) + "&filter=" + filter + "&client="
                + UriUtils.encodeQueryParam("WEB", cs);
    }

    private List<EastmoneyMarketMarginHistoryRowDTO> parseDataRows(JsonNode data) {
        List<EastmoneyMarketMarginHistoryRowDTO> rows = new ArrayList<>();
        if (data == null || data.isNull()) {
            return rows;
        }
        if (data.isArray()) {
            for (JsonNode n : data) {
                rows.add(objectMapper.convertValue(n, EastmoneyMarketMarginHistoryRowDTO.class));
            }
            return rows;
        }
        if (data.isObject()) {
            Iterator<String> it = data.fieldNames();
            while (it.hasNext()) {
                JsonNode n = data.get(it.next());
                if (n != null && n.isObject()) {
                    rows.add(objectMapper.convertValue(n, EastmoneyMarketMarginHistoryRowDTO.class));
                }
            }
        }
        return rows;
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
}
