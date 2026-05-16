package com.quant.platform.business.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.stock.dto.EastmoneyStockValuationDTO;
import com.quant.platform.common.config.endpoints.EastmoneyEndpoints;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class EastmoneyStockValuationClient {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public EastmoneyStockValuationClient(@Qualifier("eastmoneyWebClient") WebClient webClient) {
        this.objectMapper = new ObjectMapper();
        this.webClient = webClient;
    }

    /**
     * 个股快照（东财 stock/get，{@link EastmoneyEndpoints#STOCK_VALUATION_FIELDS}）。
     */
    public EastmoneyStockValuationDTO fetchValuationSnapshot(String code) {
        return fetchValuationSnapshotWithHint(CommonUtil.toSecId(code), EastmoneyEndpoints.STOCK_VALUATION_FIELDS,
            EastmoneyEndpoints.QT_INVT, EastmoneyEndpoints.QT_FLTT, "fa5fd1943c7b386f172d6893dbfba10b", code);
    }

    private EastmoneyStockValuationDTO fetchValuationSnapshotWithHint(String secid, String fields, String invt,
                                                                      String fltt, String ut, String codeHint) {
        String pathAndQuery = UriComponentsBuilder.fromPath(EastmoneyEndpoints.STOCK_GET_PATH)
            .queryParam("secid", secid).queryParam("fields", fields).queryParam("invt", invt)
            .queryParam("fltt", fltt).queryParam("ut", ut).build(true).toUriString();

        String body = getBody(pathAndQuery);
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return null;
            }

            // stock/get：f57=证券编码，f58=证券名称（f12/f14 等为档位量等，见东财快照字段）
            String stockCode = trimToNull(data.path("f57").asText(null));
            if (stockCode == null && codeHint != null) {
                stockCode = trimToNull(CommonUtil.normalizeSixDigitCode(codeHint));
            }
            String stockName = trimToNull(data.path("f58").asText(null));

            return new EastmoneyStockValuationDTO(stockCode, stockName, toDecimal(data, "f43"),
                toLong(data, "f47"), toDecimal(data, "f48"),
                toDecimal(data, "f168"), toDecimal(data, "f50"), toDecimal(data, "f60"), toDecimal(data, "f46"),
                toDecimal(data, "f44"), toDecimal(data, "f45"), toDecimal(data, "f71"), toDecimal(data, "f51"),
                toDecimal(data, "f52"), toDecimal(data, "f171"), toDecimal(data, "f116"), toDecimal(data, "f117"),
                toDecimal(data, "f84"), toDecimal(data, "f85"), toDecimal(data, "f162"), toDecimal(data, "f163"),
                toDecimal(data, "f164"), toDecimal(data, "f165"), toDecimal(data, "f167"), body);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse eastmoney stock/get response: " + body, e);
        }
    }

    private String getBody(String pathAndQuery) {
        try {
            return requestBodyWithRetry(webClient, pathAndQuery).block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney request failed: " + pathAndQuery, e);
        }
    }

    private static Mono<String> requestBodyWithRetry(WebClient client, String pathAndQuery) {
        return client
            .get().uri(pathAndQuery).retrieve().bodyToMono(String.class).timeout(
                Duration.ofSeconds(10))
            .onErrorResume(WebClientResponseException.class,
                e -> Mono.error(new IllegalStateException(
                    "Eastmoney HTTP error: " + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                    e)))
            .retryWhen(Retry.backoff(5, Duration.ofMillis(300)).maxBackoff(Duration.ofSeconds(3))
                .filter(ex -> ex instanceof WebClientRequestException));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static java.math.BigDecimal toDecimal(JsonNode data, String field) {
        JsonNode n = data.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        String v = n.asText(null);
        if (v == null) {
            return null;
        }
        v = v.trim();
        if (v.isEmpty() || "-".equals(v) || "null".equalsIgnoreCase(v)) {
            return null;
        }
        try {
            return new java.math.BigDecimal(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long toLong(JsonNode data, String field) {
        JsonNode n = data.get(field);
        if (n == null || n.isNull()) {
            return null;
        }
        String v = n.asText(null);
        if (v == null) {
            return null;
        }
        v = v.trim();
        if (v.isEmpty() || "-".equals(v) || "null".equalsIgnoreCase(v)) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }


}
