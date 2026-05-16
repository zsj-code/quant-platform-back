package com.quant.platform.ai.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.ai.core.client.dto.ThsMarketTurnoverMinuteApiResponseDTO;
import com.quant.platform.ai.core.client.dto.ThsMarketTurnoverMinuteChartDTO;
import com.quant.platform.common.config.endpoints.ThsDqFuyaoEndpoints;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * 同花顺 dq「扶摇」市场分析图表：全市场成交额分时等。
 *
 * @see <a href="https://dq.10jqka.com.cn/fuyao/market_analysis_api/chart/v1/get_chart_data">get_chart_data</a>
 */
@Component
public class ThsFuyaoMarketChartClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ThsFuyaoMarketChartClient(@Qualifier("thsDqFuyaoWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 全市场成交额分时（{@code chart_key=turnover_minute}），含 header 汇总与 point_list 序列。
     */
    public ThsMarketTurnoverMinuteChartDTO fetchMarketTurnoverMinute() {
        String body = requestBody();
        try {
            ThsMarketTurnoverMinuteApiResponseDTO root = objectMapper.readValue(body, ThsMarketTurnoverMinuteApiResponseDTO.class);
            if (root.statusCode() != 0) {
                throw new IllegalStateException("THS fuyao returned status_code=" + root.statusCode() + ", msg=" + root.statusMsg());
            }
            if (root.data() == null || root.data().charts() == null) {
                throw new IllegalStateException("THS fuyao response missing data.charts");
            }
            return root.data().charts();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse THS market turnover_minute chart", e);
        }
    }

    private String requestBody() {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(ThsDqFuyaoEndpoints.MARKET_ANALYSIS_CHART_GET_PATH)
                            .queryParam("chart_key", ThsDqFuyaoEndpoints.CHART_KEY_TURNOVER_MINUTE)
                            .build())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .onErrorResume(WebClientResponseException.class,
                            e -> Mono.error(new IllegalStateException("THS dq fuyao HTTP error: " + e.getStatusCode()
                                    + ", body=" + e.getResponseBodyAsString(), e)))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("THS dq fuyao request failed: " + ThsDqFuyaoEndpoints.MARKET_ANALYSIS_CHART_GET_PATH,
                    e);
        }
    }
}
