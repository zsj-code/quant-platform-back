package com.quant.platform.business.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.kline.dto.EastmoneyKlineBarDTO;
import com.quant.platform.common.config.endpoints.EastmoneyEndpoints;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class EastmoneyKlineClient {

    private static final DateTimeFormatter MINUTE_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MINUTE_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyKlineClient(@Qualifier("eastmoneyKlineWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * K线获取（东财）。
     * <p>
     * 示例：
     * https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=0.000001&fields1=...&fields2=...&klt=...&fqt=1&beg=20260101&end=20260401
     * （{@code klt} 取值见 {@link KlineIntervalTypeEnum#getEastmoneyKlt()}，如日 K 为 101）
     */
    public List<EastmoneyKlineBarDTO> fetchKline(String seid, int klt, int fqt, String beg, String end) {

        String pathAndQuery = UriComponentsBuilder.fromPath(EastmoneyEndpoints.STOCK_KLINE_GET_PATH)
                .queryParam("secid", seid).queryParam("fields1", EastmoneyEndpoints.STOCK_GET_FIELDS1)
                .queryParam("fields2", EastmoneyEndpoints.STOCK_GET_FIELDS2_KLINE).queryParam("klt", klt)
                .queryParam("fqt", fqt).queryParam("beg", beg).queryParam("end", end)
                .queryParam("ut", EastmoneyEndpoints.QT_UT).build(true).toUriString();

        String body = requestBody(pathAndQuery);
        if (body == null || body.trim().isEmpty()) {
            return List.of();
        }

        try {
            boolean minuteLine = klt < KlineIntervalTypeEnum.D.getEastmoneyKlt();
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            List<EastmoneyKlineBarDTO> out = new ArrayList<>();
            if (data.isMissingNode())
                return out;
            JsonNode klines = data.path("klines");
            if (klines.isMissingNode() || !klines.isArray())
                return out;
            for (JsonNode node : klines) {
                String line = node.asText();
                if (line == null || line.isEmpty())
                    continue;
                String[] parts = line.split(",");
                if (parts.length < 6)
                    continue;
                LocalDateTime barTime;
                if (minuteLine || parts[0].contains(" ")) {
                    barTime = parseEastmoneyDateTime(parts[0].trim());
                    if (barTime == null) {
                        continue;
                    }
                } else {
                    barTime = LocalDate.parse(parts[0].trim()).atStartOfDay();
                }
                EastmoneyKlineBarDTO dto = parseKlineLine(node.asText(null));
                out.add(dto);
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse eastmoney kline response", e);
        }
    }

    public List<EastmoneyKlineBarDTO> fetchKline(String secid, int klt, int fqt, LocalDate beg, LocalDate end) {
        String b = beg == null ? null : beg.format(DateTimeFormatter.BASIC_ISO_DATE);
        String e = end == null ? null : end.format(DateTimeFormatter.BASIC_ISO_DATE);
        return fetchKline(secid, klt, fqt, b, e);
    }

    private static LocalDateTime parseEastmoneyDateTime(String s) {
        try {
            return LocalDateTime.parse(s, MINUTE_SEC);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(s, MINUTE_MIN);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private EastmoneyKlineBarDTO parseKlineLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        // 期望格式（fields2）：date,open,close,high,low,volume,amount,amplitude%,涨跌幅%,涨跌额,换手率%
        String[] parts = line.split(",");
        if (parts.length < 8) {
            return null;
        }
        String date = parts[0];
        BigDecimal open = toDecimal(parts[1]);
        BigDecimal close = toDecimal(parts[2]);
        BigDecimal high = toDecimal(parts[3]);
        BigDecimal low = toDecimal(parts[4]);
        Long volume = toLong(parts[5]);
        BigDecimal amount = toDecimal(parts[6]);
        BigDecimal amplitude = toDecimal(parts[7]);
        BigDecimal changePct = parts.length > 8 ? toDecimal(parts[8]) : null;
        BigDecimal changeAmount = parts.length > 9 ? toDecimal(parts[9]) : null;
        BigDecimal turnoverRate = parts.length > 10 ? toDecimal(parts[10]) : null;
        return new EastmoneyKlineBarDTO(date, open, close, high, low, volume, amount, amplitude, changePct,
            changeAmount, turnoverRate);
    }

    private static BigDecimal toDecimal(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty() || "-".equals(v) || "null".equalsIgnoreCase(v)) {
            return null;
        }
        return new BigDecimal(v);
    }

    private static Long toLong(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        if (v.isEmpty() || "-".equals(v) || "null".equalsIgnoreCase(v)) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String requestBody(String pathAndQuery) {
        try {
            return webClient.get().uri(pathAndQuery).retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(10))
                    .onErrorResume(WebClientResponseException.class,
                            e -> Mono.error(new IllegalStateException("Eastmoney kline HTTP error: " + e.getStatusCode()
                                    + ", body=" + e.getResponseBodyAsString(), e)))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney kline request failed: " + pathAndQuery, e);
        }
    }
}
