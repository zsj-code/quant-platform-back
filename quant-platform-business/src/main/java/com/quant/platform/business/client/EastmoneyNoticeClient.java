package com.quant.platform.business.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.stock.dto.StockAnnouncementCodeDTO;
import com.quant.platform.business.stock.dto.StockAnnouncementColumnDTO;
import com.quant.platform.business.stock.dto.StockAnnouncementItemDTO;
import com.quant.platform.business.stock.dto.StockAnnouncementPageDTO;
import com.quant.platform.common.config.endpoints.EastmoneyNoticeEndpoints;
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
public class EastmoneyNoticeClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EastmoneyNoticeClient(@Qualifier("eastmoneyNoticeWebClient") WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper();
    }

    public StockAnnouncementPageDTO fetchAnnouncements(String stockCode, int pageIndex, int pageSize) {
        String pathAndQuery = UriComponentsBuilder.fromPath(EastmoneyNoticeEndpoints.ANN_PATH).queryParam("sr", -1)
                .queryParam("page_size", pageSize).queryParam("page_index", pageIndex).queryParam("ann_type", "A")
                .queryParam("client_source", "web").queryParam("f_node", 0).queryParam("s_node", 0)
                .queryParam("stock_list", stockCode).build(true).toUriString();

        String body = requestBody(pathAndQuery);
        if (body == null || body.trim().isEmpty()) {
            return new StockAnnouncementPageDTO(pageIndex, pageSize, 0, List.of());
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            JsonNode listNode = data.path("list");

            long totalHits = data.path("total_hits").asLong(0);
            long pi = data.path("page_index").asLong(pageIndex);
            long ps = data.path("page_size").asLong(pageSize);

            List<StockAnnouncementItemDTO> items = new ArrayList<>();
            if (listNode.isArray()) {
                for (JsonNode n : listNode) {
                    items.add(parseItem(n));
                }
            }

            return new StockAnnouncementPageDTO(pi, ps, totalHits, items);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse eastmoney notice response", e);
        }
    }

    private StockAnnouncementItemDTO parseItem(JsonNode n) {
        String artCode = n.path("art_code").asText(null);
        String title = n.path("title").asText(null);
        String titleCh = n.path("title_ch").asText(null);
        String titleEn = n.path("title_en").asText(null);
        String noticeDate = n.path("notice_date").asText(null);
        String displayTime = n.path("display_time").asText(null);
        String eiTime = n.path("eiTime").asText(null);
        String language = n.path("language").asText(null);
        String listingState = n.path("listing_state").asText(null);
        String sourceType = n.path("source_type").asText(null);
        String productCode = n.path("product_code").asText(null);
        String sortDate = n.path("sort_date").asText(null);

        List<StockAnnouncementCodeDTO> codes = new ArrayList<>();
        JsonNode codesNode = n.path("codes");
        if (codesNode.isArray()) {
            for (JsonNode c : codesNode) {
                codes.add(new StockAnnouncementCodeDTO(c.path("ann_type").asText(null),
                        c.path("inner_code").asText(null), c.path("market_code").asText(null),
                        c.path("short_name").asText(null), c.path("stock_code").asText(null)));
            }
        }

        List<StockAnnouncementColumnDTO> columns = new ArrayList<>();
        JsonNode cols = n.path("columns");
        if (cols.isArray()) {
            for (JsonNode c : cols) {
                columns.add(new StockAnnouncementColumnDTO(c.path("column_code").asText(null),
                        c.path("column_name").asText(null)));
            }
        }

        return new StockAnnouncementItemDTO(artCode, codes, title, titleCh, titleEn, noticeDate, displayTime, eiTime,
                language, listingState, sourceType, productCode, sortDate, columns);
    }

    private String requestBody(String pathAndQuery) {
        try {
            return webClient.get().uri(pathAndQuery).retrieve().bodyToMono(String.class).timeout(Duration.ofSeconds(10))
                    .onErrorResume(WebClientResponseException.class,
                            e -> Mono.error(new IllegalStateException("Eastmoney notice HTTP error: "
                                    + e.getStatusCode() + ", body=" + e.getResponseBodyAsString(), e)))
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(200)).maxBackoff(Duration.ofSeconds(2))
                            .filter(ex -> ex instanceof WebClientRequestException))
                    .block();
        } catch (Exception e) {
            throw new IllegalStateException("Eastmoney notice request failed: " + pathAndQuery, e);
        }
    }
}
