package com.quant.platform.business.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.stock.dto.StockBasicDTO;
import com.quant.platform.common.config.endpoints.EastmoneyEndpoints;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class EastmoneyStockClient {
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public EastmoneyStockClient(@Qualifier("eastmoneyWebClient") WebClient webClient) {
        this.objectMapper = new ObjectMapper();
        this.webClient = webClient;
    }

    /**
     * A股股票列表（东财）。
     * <p>
     * 对应示例：
     * https://push2.eastmoney.com/api/qt/clist/get?pn=1&pz=1000&fs=m:0%20t:6,m:0%20t:80,m:1%20t:2,m:1%20t:23&fields=f12,f14
     */
    public List<StockBasicDTO> fetchAStocks(int pn, int pz) {
        String pathAndQuery = UriComponentsBuilder.fromPath(EastmoneyEndpoints.STOCKS_CLIST_PATH).queryParam("pn", pn)
                .queryParam("pz", pz).queryParam("fs", EastmoneyEndpoints.A_STOCKS_FS)
                .queryParam("ut", EastmoneyEndpoints.QT_UT).queryParam("fields", EastmoneyEndpoints.STOCKS_FIELDS)
                .build(true).toUriString();

        String body = getBody(pathAndQuery);
        if (body == null || body.trim().isEmpty()) {
            return List.of();
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode diff = root.path("data").path("diff");

            List<StockBasicDTO> result = new ArrayList<>();

            // diff 既可能是数组，也可能是 {"0": {...}, "1": {...}} 这种对象结构
            if (diff.isArray()) {
                for (JsonNode n : diff) {
                    addIfValid(result, n);
                }
                return result;
            }

            if (diff.isObject()) {
                diff.fields().forEachRemaining(e -> addIfValid(result, e.getValue()));
                return result;
            }

            return List.of();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse eastmoney response", e);
        }
    }

    private static void addIfValid(List<StockBasicDTO> out, JsonNode node) {
        String code = node.path("f12").asText(null);
        String name = node.path("f14").asText(null);
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            return;
        }
        out.add(new StockBasicDTO(code.trim(), name.trim()));
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
}
