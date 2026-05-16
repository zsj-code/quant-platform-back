package com.quant.platform.ai.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.ai.core.client.dto.EastmoneyMarginTradingStockPageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyMarginTradingStockRowDTO;
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
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 东方财富数据中心 {@code RPTA_WEB_RZRQ_GGMX}：某只个股融资融券明细（按 {@code date} 倒序分页）。
 * <p>
 * 与浏览器一致：{@code filter=(scode="6位代码")} 仅对 {@code =}、{@code "} 编码，勿对整段 URL 编码括号。
 *
 * @see <a href="https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>
 */
@Component
public class EastmoneyStockMarginTradingClient {

    public static final int DEFAULT_PAGE_SIZE = 50;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyStockMarginTradingClient(@Qualifier("eastmoneyDatacenterWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分页查询指定证券融资融券明细，每页 {@link #DEFAULT_PAGE_SIZE} 条，从第 1 页起。
     */
    public EastmoneyMarginTradingStockPageDTO fetchStockMarginTrading(String sixDigitOrSymbol, int pageNumber) {
        return fetchStockMarginTrading(sixDigitOrSymbol, pageNumber, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param sixDigitOrSymbol 6 位代码或带后缀，如 002456 / 002456.SZ
     * @param pageNumber       页码，从 1 开始
     * @param pageSize         每页条数，建议与东财常见值一致（如 50）
     */
    public EastmoneyMarginTradingStockPageDTO fetchStockMarginTrading(String sixDigitOrSymbol, int pageNumber,
                                                                       int pageSize) {
        String code = CommonUtil.normalizeSixDigitCode(sixDigitOrSymbol);
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("stockCode required, got: " + sixDigitOrSymbol);
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        String pathAndQuery = buildDatacenterQueryPath(code, pageNumber, pageSize);
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
            List<EastmoneyMarginTradingStockRowDTO> rows = parseDataRows(data);
            return new EastmoneyMarginTradingStockPageDTO(code, pageNumber, pageSize, total, totalPages, rows);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Eastmoney margin trading (RZRQ) response", e);
        }
    }

    /**
     * 等价于浏览器 query：{@code filter=(scode="code")} → {@code (scode%3D%22code%22)}。
     *
     * @see com.quant.platform.business.client.EastmoneyFinancialStatementClient#eastmoneyFilterForQuery
     */
    static String marginStockFilterForQuery(String sixDigitCode) {
        return "(scode%3D%22" + sixDigitCode + "%22)";
    }

    private static String buildDatacenterQueryPath(String sixDigitCode, int pageNumber, int pageSize) {
        var cs = StandardCharsets.UTF_8;
        String p = EastmoneyFinancialStatementEndpoints.DATA_V1_GET_PATH;
        String filter = marginStockFilterForQuery(sixDigitCode);
        return p + "?reportName=" + UriUtils.encodeQueryParam(EastmoneyFinancialStatementEndpoints.REPORT_RPTA_WEB_RZRQ_GGMX, cs)
                + "&columns=" + UriUtils.encodeQueryParam("ALL", cs) + "&source="
                + UriUtils.encodeQueryParam("WEB", cs) + "&sortColumns="
                + UriUtils.encodeQueryParam("date", cs) + "&sortTypes="
                + UriUtils.encodeQueryParam("-1", cs) + "&pageNumber="
                + UriUtils.encodeQueryParam(String.valueOf(pageNumber), cs) + "&pageSize="
                + UriUtils.encodeQueryParam(String.valueOf(pageSize), cs) + "&filter=" + filter + "&client="
                + UriUtils.encodeQueryParam("WEB", cs);
    }

    private List<EastmoneyMarginTradingStockRowDTO> parseDataRows(JsonNode data) {
        List<EastmoneyMarginTradingStockRowDTO> rows = new ArrayList<>();
        if (data == null || data.isNull()) {
            return rows;
        }
        if (data.isArray()) {
            for (JsonNode n : data) {
                rows.add(objectMapper.convertValue(n, EastmoneyMarginTradingStockRowDTO.class));
            }
            return rows;
        }
        if (data.isObject()) {
            Iterator<String> it = data.fieldNames();
            while (it.hasNext()) {
                JsonNode n = data.get(it.next());
                if (n != null && n.isObject()) {
                    rows.add(objectMapper.convertValue(n, EastmoneyMarginTradingStockRowDTO.class));
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
