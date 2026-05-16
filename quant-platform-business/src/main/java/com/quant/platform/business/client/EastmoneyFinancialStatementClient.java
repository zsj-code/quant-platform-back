package com.quant.platform.business.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.financial.dto.EastmoneyFinancialStatementPageDTO;
import com.quant.platform.common.config.endpoints.EastmoneyFinancialStatementEndpoints;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 东方财富数据中心 <a href=
 * "https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>：
 * 拉取 A 股财务报表（利润表 / 资产负债表 / 现金流量表），按报告期倒序分页；数据可写入表 {@code financial_statement}。
 */
@Component
public class EastmoneyFinancialStatementClient {

    public static final int PAGE_SIZE = EastmoneyFinancialStatementEndpoints.DEFAULT_PAGE_SIZE;

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyFinancialStatementClient(@Qualifier("eastmoneyDatacenterWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * @param stockCode
     *            6 位代码，如 600000
     * @param type
     *            报表类型
     * @param pageNumber
     *            页码，从 1 开始
     */
    public EastmoneyFinancialStatementPageDTO fetchPage(String stockCode, EastmoneyFinancialStatementReportTypeEnum type,
                                                        int pageNumber) throws IOException {

        String code = CommonUtil.normalizeSixDigitCode(stockCode);
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("stockCode required, got: " + stockCode);
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1");
        }
        // 与浏览器一致：filter=(SECURITY_CODE%3D%226位%22) —— 仅编码 = 与 "，括号保持明文；整段
        // encodeQueryParam 会把 () 编成 %28%29 导致服务端 ANTLR 解析失败
        String pathAndQuery = buildDatacenterQueryPath(type, pageNumber, code);

        String body = requestBody(pathAndQuery);
        JsonNode root = objectMapper.readTree(body);
        if (!root.path("success").asBoolean(false)) {
            String msg = root.path("message").asText("unknown");
            throw new RuntimeException("Eastmoney datacenter returned success=false: " + msg);
        }
        JsonNode result = root.path("result");
        long total = result.path("count").asLong(0);
        int apiPages = result.path("pages").asInt(0);
        Integer totalPages = apiPages > 0
                ? apiPages
                : (PAGE_SIZE > 0 && total > 0
                        ? BigDecimal.valueOf(total).divide(BigDecimal.valueOf(PAGE_SIZE), 0, RoundingMode.CEILING).intValue()
                        : null);

        JsonNode data = result.path("data");
        List<Map<String, Object>> rows = parseDataRows(data);

        return new EastmoneyFinancialStatementPageDTO(type.getReportName(), code, pageNumber, PAGE_SIZE, total,
                totalPages, rows);
    }

    /**
     * 手工拼接 query；filter 按东财规则只对 {@code =}、{@code "} 编码，勿对整段做
     * {@link UriUtils#encodeQueryParam}（否则会编码括号，触发 9501 / ANTLR）。
     */
    private static String buildDatacenterQueryPath(EastmoneyFinancialStatementReportTypeEnum type, int pageNumber,
                                                   String sixDigitCode) {
        var cs = StandardCharsets.UTF_8;
        String p = EastmoneyFinancialStatementEndpoints.DATA_V1_GET_PATH;
        String filterParam = eastmoneyFilterForQuery(sixDigitCode);
        return p + "?sortColumns=" + UriUtils.encodeQueryParam("REPORT_DATE", cs) + "&sortTypes="
                + UriUtils.encodeQueryParam("-1", cs) + "&pageSize="
                + UriUtils.encodeQueryParam(String.valueOf(PAGE_SIZE), cs) + "&pageNumber="
                + UriUtils.encodeQueryParam(String.valueOf(pageNumber), cs) + "&reportName="
                + UriUtils.encodeQueryParam(type.getReportName(), cs) + "&columns="
                + UriUtils.encodeQueryParam("ALL", cs) + "&filter=" + filterParam + "&source="
                + UriUtils.encodeQueryParam("WEB", cs) + "&client=" + UriUtils.encodeQueryParam("WEB", cs);
    }

    /**
     * 等价于浏览器中 {@code (SECURITY_CODE="code")} 在 query
     * 里的写法：{@code (SECURITY_CODE%3D%22code%22)}。
     */
    static String eastmoneyFilterForQuery(String sixDigitCode) {
        return "(SECURITY_CODE%3D%22" + sixDigitCode + "%22)";
    }

    private List<Map<String, Object>> parseDataRows(JsonNode data) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (data == null || data.isNull()) {
            return rows;
        }
        if (data.isArray()) {
            for (JsonNode n : data) {
                rows.add(objectMapper.convertValue(n, MAP_TYPE));
            }
            return rows;
        }
        if (data.isObject()) {
            Iterator<String> it = data.fieldNames();
            while (it.hasNext()) {
                JsonNode n = data.get(it.next());
                if (n != null && n.isObject()) {
                    rows.add(objectMapper.convertValue(n, MAP_TYPE));
                }
            }
        }
        return rows;
    }

    private String requestBody(String pathAndQuery) {
        try {
            // 使用绝对 URI + uri(URI)：避免 uri(String) 对 query 二次编码；相对路径 + baseUrl 时部分版本仍会改写已编码的
            // filter
            URI requestUri = URI.create(EastmoneyFinancialStatementEndpoints.BASE_DATACENTER_WEB + pathAndQuery);
            return webClient.get().uri(requestUri).retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(15))
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
