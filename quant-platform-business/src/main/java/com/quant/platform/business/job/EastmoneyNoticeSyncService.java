package com.quant.platform.business.job;

import com.quant.platform.business.client.EastmoneyNoticeClient;
import com.quant.platform.business.stock.dto.StockAnnouncementColumnDTO;
import com.quant.platform.business.stock.dto.StockAnnouncementItemDTO;
import com.quant.platform.business.stock.dto.StockAnnouncementPageDTO;
import com.quant.platform.business.stock.entity.StockAnnouncementEntity;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.business.stock.service.StockAnnouncementAdminService;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 东财公告列表同步至表 {@code stock_announcement}（按 source + external_id 幂等）。
 */
@Service
public class EastmoneyNoticeSyncService {

    private static final Logger log = LoggerFactory.getLogger(EastmoneyNoticeSyncService.class);

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 仅持久化公告日期晚于该日的记录（即 noticeDate &gt; 2025-01-01）。 */
    private static final LocalDate MIN_NOTICE_DATE_EXCLUSIVE = LocalDate.of(2025, 1, 1);

    private final EastmoneyNoticeClient eastmoneyNoticeClient;
    private final StockAnnouncementAdminService stockAnnouncementAdminService;
    private final StockAdminService stockAdminService;

    public EastmoneyNoticeSyncService(EastmoneyNoticeClient eastmoneyNoticeClient,
            StockAnnouncementAdminService stockAnnouncementAdminService, StockAdminService stockAdminService) {
        this.eastmoneyNoticeClient = eastmoneyNoticeClient;
        this.stockAnnouncementAdminService = stockAnnouncementAdminService;
        this.stockAdminService = stockAdminService;
    }

    /**
     * 兼容旧任务：默认分页与休眠。
     */
    public void syncEastmoneyNotice() {
        syncAll(30, 10, 80);
    }

    /**
     * 全市场同步公告。
     *
     * @param pageSize
     *            每页条数（东财 page_size）
     * @param maxPagesPerStock
     *            每只股票最多拉取页数
     * @param sleepMsPerStock
     *            每只股票处理完后休眠毫秒数，≤0 不休眠
     * @return 成功 upsert 条数（含更新）；仅 {@code noticeDate &gt; 2025-01-01} 的公告会写入
     */
    public long syncAll(int pageSize, int maxPagesPerStock, int sleepMsPerStock) {
        log.info("syncEastmoneyNotice start, pageSize={}, maxPagesPerStock={}, sleepMs={}", pageSize, maxPagesPerStock,
                sleepMsPerStock);
        List<StockEntity> stocks = stockAdminService.listNonDelisted();
        if (stocks == null || stocks.isEmpty()) {
            log.info("syncEastmoneyNotice: no stocks");
            return 0L;
        }
        int ps = pageSize > 0 ? pageSize : 30;
        int maxPages = maxPagesPerStock > 0 ? maxPagesPerStock : 10;
        long affected = 0L;
        for (StockEntity stock : stocks) {
            if (stock == null) {
                continue;
            }
            String code = CommonUtil.normalizeSixDigitCode(stock.getCode());
            if (code == null || code.isEmpty()) {
                continue;
            }
            String symbol = CommonUtil.toSymbol(code);
            if (symbol == null) {
                continue;
            }
            try {
                affected += syncStockAnnouncements(code, symbol, ps, maxPages);
            } catch (Exception e) {
                log.warn("syncEastmoneyNotice skip stock code={} err={}", code, e.toString());
            }
            // if (sleepMsPerStock > 0) {
            // try {
            // Thread.sleep(sleepMsPerStock);
            // } catch (InterruptedException ie) {
            // Thread.currentThread().interrupt();
            // log.warn("syncEastmoneyNotice interrupted");
            // return affected;
            // }
            // }
        }
        log.info("syncEastmoneyNotice done, affected={}", affected);
        return affected;
    }

    private long syncStockAnnouncements(String code, String symbol, int pageSize, int maxPages) throws Exception {
        long n = 0L;
        for (int page = 1; page <= maxPages; page++) {
            StockAnnouncementPageDTO dto = eastmoneyNoticeClient.fetchAnnouncements(code, page, pageSize);
            List<StockAnnouncementItemDTO> items = dto.getList();
            if (items == null || items.isEmpty()) {
                break;
            }
            List<StockAnnouncementEntity> batch = new ArrayList<>();
            for (StockAnnouncementItemDTO item : items) {
                StockAnnouncementEntity entity = toEntity(item, code, symbol);
                if (entity == null) {
                    continue;
                }
                batch.add(entity);
            }
            stockAnnouncementAdminService.upsertBatch(batch);
            n += batch.size();
            if (items.size() < pageSize) {
                break;
            }
        }
        return n;
    }

    /**
     * @return null 表示跳过（如无 art_code、noticeDate 不可解析或不大于 2025-01-01）
     */
    private static StockAnnouncementEntity toEntity(StockAnnouncementItemDTO item, String code, String symbol) {
        if (item == null) {
            return null;
        }
        String ext = item.getArtCode();
        if (ext == null || ext.trim().isEmpty()) {
            return null;
        }
        LocalDate noticeDate = parseLocalDate(item.getNoticeDate());
        if (noticeDate == null || !noticeDate.isAfter(MIN_NOTICE_DATE_EXCLUSIVE)) {
            return null;
        }
        String title = pickTitle(item);
        if (title == null || title.isEmpty()) {
            title = "（无标题）";
        }
        if (title.length() > 2048) {
            title = title.substring(0, 2048);
        }
        LocalDateTime announceTime = resolveAnnounceTime(item);

        StockAnnouncementEntity e = new StockAnnouncementEntity();
        e.setCode(code);
        e.setSymbol(symbol);
        e.setSource("EASTMONEY");
        e.setExternalId(ext.trim());
        e.setTitle(title);
        e.setAnnounceTime(announceTime);
        e.setNoticeDate(noticeDate);
        e.setCategories(joinCategories(item.getColumns()));
        e.setFetchedAt(LocalDateTime.now());
        return e;
    }

    private static String pickTitle(StockAnnouncementItemDTO item) {
        String t = firstNonBlank(item.getTitle(), item.getTitleCh(), item.getTitleEn());
        return t == null ? null : t.trim();
    }

    private static String firstNonBlank(String... ss) {
        if (ss == null) {
            return null;
        }
        for (String s : ss) {
            if (s != null && !s.trim().isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private static LocalDateTime resolveAnnounceTime(StockAnnouncementItemDTO item) {
        LocalDateTime t = firstNonNullTime(parseDateTimeLoose(item.getSortDate()), parseDateTimeLoose(item.getEiTime()),
                parseDateTimeLoose(item.getDisplayTime()));
        if (t != null) {
            return t;
        }
        LocalDate nd = parseLocalDate(item.getNoticeDate());
        if (nd != null) {
            return nd.atStartOfDay();
        }
        return LocalDateTime.now();
    }

    private static LocalDateTime firstNonNullTime(LocalDateTime... ts) {
        if (ts == null) {
            return null;
        }
        for (LocalDateTime t : ts) {
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    private static LocalDateTime parseDateTimeLoose(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        if (v.length() >= 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
            if (v.length() >= 19) {
                String sub = v.substring(0, 19).replace(' ', 'T');
                try {
                    return LocalDateTime.parse(sub, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException ignored) {
                }
            }
            if (v.length() >= 16 && v.charAt(10) == ' ') {
                try {
                    return LocalDateTime.parse(v.substring(0, 16).replace(' ', 'T') + ":00",
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException ignored) {
                }
            }
            try {
                return LocalDate.parse(v.substring(0, 10), ISO_DATE).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static LocalDate parseLocalDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        if (v.length() >= 10 && v.charAt(4) == '-' && v.charAt(7) == '-') {
            try {
                return LocalDate.parse(v.substring(0, 10), ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (v.length() >= 8 && Character.isDigit(v.charAt(0))) {
            try {
                return LocalDate.parse(v.substring(0, 4) + "-" + v.substring(4, 6) + "-" + v.substring(6, 8), ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static String joinCategories(List<StockAnnouncementColumnDTO> columns) {
        if (columns == null || columns.isEmpty()) {
            return null;
        }
        String joined = columns.stream().filter(Objects::nonNull).map(StockAnnouncementColumnDTO::getColumnName)
                .filter(n -> n != null && !n.trim().isEmpty()).map(String::trim).collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }
}
