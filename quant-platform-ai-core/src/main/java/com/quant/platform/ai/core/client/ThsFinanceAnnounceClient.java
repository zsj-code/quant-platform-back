package com.quant.platform.ai.core.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.ai.core.client.dto.ThsFinanceAnnounceDetailResponseDTO;
import com.quant.platform.ai.core.client.dto.ThsFinanceAnnounceYearDTO;
import com.quant.platform.common.config.endpoints.ThsBasicEndpoints;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * 同花顺 basic 财务页：报告期列表与历年审核意见（接口字段名 {@code opinion}，见 {@link ThsFinanceAnnounceYearDTO#auditOpinion()}）。
 * <p>
 * 对应浏览器请求示例：
 * {@code GET /basicapi/finance/announce/detail/?code=600396&market=17&page=1&size=10&type=stock}
 * <p>
 * 说明：当前 JSON 仅包含审核意见文案，不包含会计师事务所名称；若需审计机构需另接数据源。
 */
@Component
public class ThsFinanceAnnounceClient {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public ThsFinanceAnnounceClient(@Qualifier("thsBasicWebClient") WebClient webClient) {
        this.objectMapper = new ObjectMapper();
        this.webClient = webClient;
    }

    /**
     * 按 A 股常见规则推断 {@code market}：沪市主板/科创板（代码首位为 {@code 6}）→ {@link ThsBasicEndpoints#MARKET_SH_A}，否则 → {@link ThsBasicEndpoints#MARKET_SZ_A}。
     * <p>
     * 北交所等场景请自行传入正确的 {@code market}，勿依赖本方法。
     */
    public static int defaultMarketForSixDigitCode(String sixDigitCode) {
        if (sixDigitCode == null || sixDigitCode.length() != 6 || !sixDigitCode.chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("sixDigitCode must be 6 digits: " + sixDigitCode);
        }
        return sixDigitCode.charAt(0) == '6' ? ThsBasicEndpoints.MARKET_SH_A : ThsBasicEndpoints.MARKET_SZ_A;
    }

    /**
     * 拉取财务公告明细（含各自然年审核意见 {@code opinion} 与 {@code report_list}）。
     *
     * @param code   6 位股票代码，如 600396
     * @param market 同花顺 {@code market}，沪市 A 常用 {@link ThsBasicEndpoints#MARKET_SH_A}，深市 A 常用 {@link ThsBasicEndpoints#MARKET_SZ_A}
     * @param page   页码，从 1 起（与前端一致）
     * @param size   每页条数
     */
    public List<ThsFinanceAnnounceYearDTO> fetchFinanceAnnounceDetail(String code, int market, int page, int size) {
        String pathAndQuery = UriComponentsBuilder.fromPath(ThsBasicEndpoints.FINANCE_ANNOUNCE_DETAIL_PATH)
                .queryParam("code", code)
                .queryParam("market", market)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("type", "stock")
                .build(true)
                .toUriString();

        String referer = "https://basic.10jqka.com.cn/" + code + "/finance.html";
        String body = getBody(pathAndQuery, referer);
        if (body == null || body.isBlank()) {
            return List.of();
        }

        try {
            ThsFinanceAnnounceDetailResponseDTO root = objectMapper.readValue(body, ThsFinanceAnnounceDetailResponseDTO.class);
            if (root.statusCode() != 0) {
                throw new IllegalStateException(
                        "THS finance announce API status_code=" + root.statusCode() + ", msg=" + root.statusMsg());
            }
            return root.data();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse THS finance announce response", e);
        }
    }

    /**
     * 使用 {@link #defaultMarketForSixDigitCode(String)} 推断 {@code market}。
     */
    public List<ThsFinanceAnnounceYearDTO> fetchFinanceAnnounceDetail(String sixDigitCode, int page, int size) {
        return fetchFinanceAnnounceDetail(sixDigitCode, defaultMarketForSixDigitCode(sixDigitCode), page, size);
    }

    private String getBody(String pathAndQuery, String referer) {
        try {
            return requestBodyWithRetry(pathAndQuery, referer).block();
        } catch (Exception e) {
            throw new IllegalStateException("THS basic request failed: " + pathAndQuery, e);
        }
    }

    private Mono<String> requestBodyWithRetry(String pathAndQuery, String referer) {
        return webClient.get()
                .uri(pathAndQuery)
                .header(HttpHeaders.REFERER, referer)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(12))
                .onErrorResume(WebClientResponseException.class,
                        e -> Mono.error(new IllegalStateException(
                                "THS basic HTTP error: " + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(),
                                e)))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(400))
                        .maxBackoff(Duration.ofSeconds(2))
                        .filter(ex -> ex instanceof WebClientRequestException));
    }
}
