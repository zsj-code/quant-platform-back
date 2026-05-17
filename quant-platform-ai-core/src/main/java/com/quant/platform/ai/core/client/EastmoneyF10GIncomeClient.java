package com.quant.platform.ai.core.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalancePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalanceRowDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GCashflowPageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GCashflowRowDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomeRowDTO;
import com.quant.platform.common.config.endpoints.EastmoneySecuritiesEndpoints;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 东方财富 F10 财务报表：利润表（{@code RPT_F10_FINANCE_GINCOME}）、现金流量表（{@code RPT_F10_FINANCE_GCASHFLOW}）、
 * 资产负债表（{@code RPT_F10_FINANCE_GBALANCE}）。
 *
 * @see <a href="https://datacenter.eastmoney.com/securities/api/data/get">securities/api/data/get</a>
 */
@Component
public class EastmoneyF10GIncomeClient {

    public static final int DEFAULT_PAGE_SIZE = 5;

    private static final DateTimeFormatter REPORT_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyF10GIncomeClient(@Qualifier("eastmoneySecuritiesDatacenterWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 查询指定证券、报告期的 F10 利润表，默认第 1 页、每页 {@link #DEFAULT_PAGE_SIZE} 条。
     */
    public EastmoneyF10GIncomePageDTO fetchF10GIncome(String symbolOrSecucode, List<LocalDate> reportDates) {
        return fetchF10GIncome(symbolOrSecucode, reportDates, 1, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param symbolOrSecucode 带后缀代码或 6 位代码，如 {@code 001979.SZ}
     * @param reportDates      报告期列表（可为空表示不按报告期过滤）
     * @param pageNumber       页码 {@code p}，从 1 开始
     * @param pageSize         每页条数 {@code ps}
     */
    public EastmoneyF10GIncomePageDTO fetchF10GIncome(String symbolOrSecucode, List<LocalDate> reportDates,
                                                      int pageNumber, int pageSize) {
        String secucode = normalizeSecucode(symbolOrSecucode);
        validatePaging(secucode, symbolOrSecucode, pageNumber, pageSize);
        List<String> dateFilters = formatReportDates(reportDates);
        String pathAndQuery = buildQueryPath(
                EastmoneySecuritiesEndpoints.TYPE_RPT_F10_FINANCE_GINCOME,
                EastmoneySecuritiesEndpoints.STY_APP_F10_GINCOME,
                secucode, dateFilters, pageNumber, pageSize);
        return fetchPage(pathAndQuery, secucode, pageNumber, pageSize, dateFilters,
                EastmoneyF10GIncomeRowDTO.class,
                (sc, pn, ps, total, tp, df, rows) ->
                        new EastmoneyF10GIncomePageDTO(sc, pn, ps, total, tp, df, rows),
                "GINCOME");
    }

    /**
     * 查询指定证券、报告期的 F10 现金流量表，默认第 1 页、每页 {@link #DEFAULT_PAGE_SIZE} 条。
     */
    public EastmoneyF10GCashflowPageDTO fetchF10GCashflow(String symbolOrSecucode, List<LocalDate> reportDates) {
        return fetchF10GCashflow(symbolOrSecucode, reportDates, 1, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param symbolOrSecucode 带后缀代码或 6 位代码，如 {@code 001979.SZ}
     * @param reportDates      报告期列表（可为空表示不按报告期过滤）
     * @param pageNumber       页码 {@code p}，从 1 开始
     * @param pageSize         每页条数 {@code ps}
     */
    public EastmoneyF10GCashflowPageDTO fetchF10GCashflow(String symbolOrSecucode, List<LocalDate> reportDates,
                                                          int pageNumber, int pageSize) {
        String secucode = normalizeSecucode(symbolOrSecucode);
        validatePaging(secucode, symbolOrSecucode, pageNumber, pageSize);
        List<String> dateFilters = formatReportDates(reportDates);
        String pathAndQuery = buildQueryPath(
                EastmoneySecuritiesEndpoints.TYPE_RPT_F10_FINANCE_GCASHFLOW,
                EastmoneySecuritiesEndpoints.STY_APP_F10_GCASHFLOW,
                secucode, dateFilters, pageNumber, pageSize);
        return fetchPage(pathAndQuery, secucode, pageNumber, pageSize, dateFilters,
                EastmoneyF10GCashflowRowDTO.class,
                (sc, pn, ps, total, tp, df, rows) ->
                        new EastmoneyF10GCashflowPageDTO(sc, pn, ps, total, tp, df, rows),
                "GCASHFLOW");
    }

    /**
     * 查询指定证券、报告期的 F10 资产负债表，默认第 1 页、每页 {@link #DEFAULT_PAGE_SIZE} 条。
     */
    public EastmoneyF10GBalancePageDTO fetchF10GBalance(String symbolOrSecucode, List<LocalDate> reportDates) {
        return fetchF10GBalance(symbolOrSecucode, reportDates, 1, DEFAULT_PAGE_SIZE);
    }

    /**
     * @param symbolOrSecucode 带后缀代码或 6 位代码，如 {@code 001979.SZ}
     * @param reportDates      报告期列表（可为空表示不按报告期过滤）
     * @param pageNumber       页码 {@code p}，从 1 开始
     * @param pageSize         每页条数 {@code ps}
     */
    public EastmoneyF10GBalancePageDTO fetchF10GBalance(String symbolOrSecucode, List<LocalDate> reportDates,
                                                        int pageNumber, int pageSize) {
        String secucode = normalizeSecucode(symbolOrSecucode);
        validatePaging(secucode, symbolOrSecucode, pageNumber, pageSize);
        List<String> dateFilters = formatReportDates(reportDates);
        String pathAndQuery = buildQueryPath(
                EastmoneySecuritiesEndpoints.TYPE_RPT_F10_FINANCE_GBALANCE,
                EastmoneySecuritiesEndpoints.STY_F10_FINANCE_GBALANCE,
                secucode, dateFilters, pageNumber, pageSize);
        return fetchPage(pathAndQuery, secucode, pageNumber, pageSize, dateFilters,
                EastmoneyF10GBalanceRowDTO.class,
                (sc, pn, ps, total, tp, df, rows) ->
                        new EastmoneyF10GBalancePageDTO(sc, pn, ps, total, tp, df, rows),
                "GBALANCE");
    }

    /**
     * 解析逗号分隔报告期，如 {@code 2026-03-31,2025-12-31}。
     */
    public static List<LocalDate> parseReportDatesParam(String reportDatesCsv) {
        if (reportDatesCsv == null || reportDatesCsv.isBlank()) {
            return List.of();
        }
        List<LocalDate> out = new ArrayList<>();
        for (String part : reportDatesCsv.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (s.length() >= 10) {
                s = s.substring(0, 10);
            }
            try {
                out.add(LocalDate.parse(s, REPORT_DATE_FMT));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("invalid report date: " + part.trim(), e);
            }
        }
        return List.copyOf(out);
    }

    public static String normalizeSecucode(String symbolOrSecucode) {
        if (symbolOrSecucode == null || symbolOrSecucode.isBlank()) {
            return null;
        }
        String s = symbolOrSecucode.trim().toUpperCase(Locale.ROOT);
        if (s.contains(".")) {
            return s;
        }
        String code = CommonUtil.normalizeSixDigitCode(s);
        return code == null ? null : CommonUtil.toSymbol(code);
    }

    static List<String> formatReportDates(List<LocalDate> reportDates) {
        if (reportDates == null || reportDates.isEmpty()) {
            return List.of();
        }
        return reportDates.stream()
                .map(d -> d.format(REPORT_DATE_FMT))
                .collect(Collectors.toList());
    }

    /**
     * {@code filter=(SECUCODE="001979.SZ")(REPORT_DATE in ('2026-03-31',...))}
     */
    static String filterForQuery(String secucode, List<String> reportDatesYmd) {
        StringBuilder sb = new StringBuilder();
        sb.append("(SECUCODE%3D%22").append(secucode).append("%22)");
        if (reportDatesYmd != null && !reportDatesYmd.isEmpty()) {
            sb.append("(REPORT_DATE%20in%20(");
            for (int i = 0; i < reportDatesYmd.size(); i++) {
                if (i > 0) {
                    sb.append("%2C");
                }
                sb.append("%27").append(reportDatesYmd.get(i)).append("%27");
            }
            sb.append("))");
        }
        return sb.toString();
    }

    private static void validatePaging(String secucode, String symbolOrSecucode, int pageNumber, int pageSize) {
        if (secucode == null || secucode.isEmpty()) {
            throw new IllegalArgumentException("secucode required, got: " + symbolOrSecucode);
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
    }

    private static String buildQueryPath(String type, String sty, String secucode, List<String> reportDatesYmd,
                                       int pageNumber, int pageSize) {
        var cs = StandardCharsets.UTF_8;
        String p = EastmoneySecuritiesEndpoints.SECURITIES_DATA_GET_PATH;
        String filter = filterForQuery(secucode, reportDatesYmd);
        return p + "?type=" + UriUtils.encodeQueryParam(type, cs)
                + "&sty=" + UriUtils.encodeQueryParam(sty, cs)
                + "&filter=" + filter
                + "&p=" + UriUtils.encodeQueryParam(String.valueOf(pageNumber), cs)
                + "&ps=" + UriUtils.encodeQueryParam(String.valueOf(pageSize), cs)
                + "&sr=" + UriUtils.encodeQueryParam("-1", cs)
                + "&st=" + UriUtils.encodeQueryParam("REPORT_DATE", cs)
                + "&source=" + UriUtils.encodeQueryParam(EastmoneySecuritiesEndpoints.SOURCE_HSF10, cs)
                + "&client=" + UriUtils.encodeQueryParam(EastmoneySecuritiesEndpoints.CLIENT_PC, cs);
    }

    private <T, P> P fetchPage(String pathAndQuery, String secucode, int pageNumber, int pageSize,
                               List<String> dateFilters, Class<T> rowClass,
                               PageFactory<T, P> pageFactory, String sheetLabel) {
        String body = requestBody(pathAndQuery);
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("success").asBoolean(false)) {
                String msg = root.path("message").asText("unknown");
                throw new IllegalStateException("Eastmoney securities datacenter returned success=false: " + msg);
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
            List<T> rows = parseDataRows(result.path("data"), rowClass);
            return pageFactory.build(secucode, pageNumber, pageSize, total, totalPages, dateFilters, rows);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Eastmoney F10 " + sheetLabel + " response", e);
        }
    }

    private <T> List<T> parseDataRows(JsonNode data, Class<T> rowClass) {
        List<T> rows = new ArrayList<>();
        if (data == null || data.isNull()) {
            return rows;
        }
        if (data.isArray()) {
            for (JsonNode n : data) {
                rows.add(objectMapper.convertValue(n, rowClass));
            }
            return rows;
        }
        if (data.isObject()) {
            Iterator<String> it = data.fieldNames();
            while (it.hasNext()) {
                JsonNode n = data.get(it.next());
                if (n != null && n.isObject()) {
                    rows.add(objectMapper.convertValue(n, rowClass));
                }
            }
        }
        return rows;
    }

    private String requestBody(String pathAndQuery) {
        try {
            URI requestUri = URI.create(EastmoneySecuritiesEndpoints.BASE_DATACENTER_SECURITIES + pathAndQuery);
            return webClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.REFERER, "https://emweb.securities.eastmoney.com/")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(WebClientResponseException.class,
                            e -> Mono.error(new IllegalStateException("Eastmoney securities datacenter HTTP error: "
                                    + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(), e)))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney securities datacenter request failed: " + pathAndQuery, e);
        }
    }

    @FunctionalInterface
    private interface PageFactory<T, P> {
        P build(String secucode, int pageNumber, int pageSize, long total, Integer totalPages,
                List<String> dateFilters, List<T> rows);
    }
}
