package com.quant.platform.ai.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.ai.core.client.dto.EastmoneyShareHolderIncreasePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyShareHolderIncreaseRowDTO;
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
 * 东方财富数据中心 {@code RPT_SHARE_HOLDER_INCREASE}：指定证券股东增减持记录。
 * <p>
 * 与浏览器一致：{@code filter=(SECURITY_CODE="6位代码")}，含行情联动字段 {@code NEWEST_PRICE}、
 * {@code CHANGE_RATE_QUOTES}。
 *
 * @see <a href="https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>
 */
@Component
public class EastmoneyShareHolderIncreaseClient {

    public static final int DEFAULT_PAGE_SIZE = 50;

    private static final String QUOTE_COLUMNS =
            "f2~01~SECURITY_CODE~NEWEST_PRICE,f3~01~SECURITY_CODE~CHANGE_RATE_QUOTES";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyShareHolderIncreaseClient(@Qualifier("eastmoneyDatacenterWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分页查询指定证券股东增减持，每页 {@link #DEFAULT_PAGE_SIZE} 条。
     */
    public EastmoneyShareHolderIncreasePageDTO fetchShareHolderIncrease(String sixDigitOrSymbol, int pageNumber) {
        return fetchShareHolderIncrease(sixDigitOrSymbol, pageNumber, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param sixDigitOrSymbol 6 位代码或带后缀，如 600176 / 600176.SH
     * @param pageNumber       页码，从 1 开始
     * @param pageSize         每页条数
     */
    public EastmoneyShareHolderIncreasePageDTO fetchShareHolderIncrease(String sixDigitOrSymbol, int pageNumber,
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
            List<EastmoneyShareHolderIncreaseRowDTO> rows = parseDataRows(result.path("data"));
            return new EastmoneyShareHolderIncreasePageDTO(code, pageNumber, pageSize, total, totalPages, rows);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Eastmoney share holder increase response", e);
        }
    }

    /**
     * 等价于浏览器 query：{@code filter=(SECURITY_CODE="code")} → {@code (SECURITY_CODE%3D%22code%22)}。
     */
    static String securityCodeFilterForQuery(String sixDigitCode) {
        return "(SECURITY_CODE%3D%22" + sixDigitCode + "%22)";
    }

    private static String buildDatacenterQueryPath(String sixDigitCode, int pageNumber, int pageSize) {
        var cs = StandardCharsets.UTF_8;
        String p = EastmoneyFinancialStatementEndpoints.DATA_V1_GET_PATH;
        String filter = securityCodeFilterForQuery(sixDigitCode);
        return p + "?sortColumns="
                + UriUtils.encodeQueryParam("END_DATE,SECURITY_CODE,EITIME", cs)
                + "&sortTypes=" + UriUtils.encodeQueryParam("-1,-1,-1", cs)
                + "&pageSize=" + UriUtils.encodeQueryParam(String.valueOf(pageSize), cs)
                + "&pageNumber=" + UriUtils.encodeQueryParam(String.valueOf(pageNumber), cs)
                + "&reportName="
                + UriUtils.encodeQueryParam(EastmoneyFinancialStatementEndpoints.REPORT_RPT_SHARE_HOLDER_INCREASE, cs)
                + "&quoteColumns=" + UriUtils.encodeQueryParam(QUOTE_COLUMNS, cs)
                + "&quoteType=" + UriUtils.encodeQueryParam("0", cs)
                + "&columns=" + UriUtils.encodeQueryParam("ALL", cs)
                + "&source=" + UriUtils.encodeQueryParam("WEB", cs)
                + "&client=" + UriUtils.encodeQueryParam("WEB", cs)
                + "&filter=" + filter;
    }

    private List<EastmoneyShareHolderIncreaseRowDTO> parseDataRows(JsonNode data) {
        List<EastmoneyShareHolderIncreaseRowDTO> rows = new ArrayList<>();
        if (data == null || data.isNull()) {
            return rows;
        }
        if (data.isArray()) {
            for (JsonNode n : data) {
                rows.add(objectMapper.convertValue(n, EastmoneyShareHolderIncreaseRowDTO.class));
            }
            return rows;
        }
        if (data.isObject()) {
            Iterator<String> it = data.fieldNames();
            while (it.hasNext()) {
                JsonNode n = data.get(it.next());
                if (n != null && n.isObject()) {
                    rows.add(objectMapper.convertValue(n, EastmoneyShareHolderIncreaseRowDTO.class));
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
