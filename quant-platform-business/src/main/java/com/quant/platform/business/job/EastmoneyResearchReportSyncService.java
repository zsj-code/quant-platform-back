package com.quant.platform.business.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.client.EastmoneyResearchReportClient;
import com.quant.platform.business.research.dto.ResearchReportItemDTO;
import com.quant.platform.business.research.dto.ResearchReportPageDTO;
import com.quant.platform.business.research.entity.ResearchReportIndustryEntity;
import com.quant.platform.business.research.entity.ResearchReportStockEntity;
import com.quant.platform.business.research.service.ResearchReportIndustryAdminService;
import com.quant.platform.business.research.service.ResearchReportStockAdminService;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 东财研报同步：个股 {@code /report/list2}、行业 {@code /report/list}，时间范围默认 <b>近 1 年</b>。
 */
@Service
public class EastmoneyResearchReportSyncService {

    private static final Logger log = LoggerFactory.getLogger(EastmoneyResearchReportSyncService.class);

    private static final String SOURCE = "EASTMONEY";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StockAdminService stockAdminService;
    private final EastmoneyResearchReportClient eastmoneyResearchReportClient;
    private final ResearchReportStockAdminService researchReportStockAdminService;
    private final ResearchReportIndustryAdminService researchReportIndustryAdminService;
    private final ObjectMapper objectMapper;

    public EastmoneyResearchReportSyncService(StockAdminService stockAdminService,
            EastmoneyResearchReportClient eastmoneyResearchReportClient,
            ResearchReportStockAdminService researchReportStockAdminService,
            ResearchReportIndustryAdminService researchReportIndustryAdminService, ObjectMapper objectMapper) {
        this.stockAdminService = stockAdminService;
        this.eastmoneyResearchReportClient = eastmoneyResearchReportClient;
        this.researchReportStockAdminService = researchReportStockAdminService;
        this.researchReportIndustryAdminService = researchReportIndustryAdminService;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步个股研报（全市场股票 × 分页），时间窗为近 1 年。
     *
     * @return 成功写入条数（按批次 upsert 计数）
     */
    public long syncStockResearchReports(int pageSize, int maxPagesPerStock, int sleepMsPerStock) {
        LocalDate end = LocalDate.now();
        LocalDate beg = end.minusYears(1);
        int ps = pageSize > 0 ? pageSize : EastmoneyResearchReportClient.DEFAULT_PAGE_SIZE;
        int maxPages = maxPagesPerStock > 0 ? maxPagesPerStock : 50;
        long affected = 0L;
        List<StockEntity> stocks = stockAdminService.listNonDelisted();
        if (stocks == null || stocks.isEmpty()) {
            log.info("syncStockResearchReports: no stocks");
            return 0L;
        }
        for (StockEntity stock : stocks) {
            if (stock == null) {
                continue;
            }
            String code = CommonUtil.normalizeSixDigitCode(stock.getCode());
            if (code == null || code.isEmpty()) {
                continue;
            }
            String symbol = CommonUtil.toSymbol(code);
            try {
                affected += syncOneStockResearch(code, symbol, ps, maxPages, beg, end);
            } catch (Exception e) {
                log.warn("syncStockResearchReports skip code={} err={}", code, e.toString());
            }
            if (sleepMsPerStock > 0) {
                try {
                    Thread.sleep(sleepMsPerStock);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return affected;
                }
            }
        }
        log.info("syncStockResearchReports done, affected={}", affected);
        return affected;
    }

    private long syncOneStockResearch(String code, String symbol, int pageSize, int maxPages, LocalDate beg,
            LocalDate end) {
        long n = 0L;
        for (int page = 1; page <= maxPages; page++) {
            ResearchReportPageDTO dto = eastmoneyResearchReportClient.fetchStockResearchReports(code, page, pageSize,
                    beg, end);
            List<ResearchReportItemDTO> items = dto.getList();
            if (items == null || items.isEmpty()) {
                break;
            }
            List<ResearchReportStockEntity> batch = new ArrayList<>();
            for (ResearchReportItemDTO item : items) {
                ResearchReportStockEntity e = toStockEntity(item, code, symbol, beg, end);
                if (e != null) {
                    batch.add(e);
                }
            }
            researchReportStockAdminService.upsertBatch(batch);
            n += batch.size();
            if (items.size() < pageSize) {
                break;
            }
        }
        return n;
    }

    /**
     * 同步行业研报（{@code industryCode=*} 全市场列表分页），时间窗为近 1 年。
     */
    public long syncIndustryResearchReports(int pageSize, int maxPages, int sleepMsPerPage) {
        LocalDate end = LocalDate.now();
        LocalDate beg = end.minusYears(1);
        int ps = pageSize > 0 ? pageSize : EastmoneyResearchReportClient.DEFAULT_PAGE_SIZE;
        int cap = maxPages > 0 ? maxPages : 200;
        long affected = 0L;
        for (int page = 1; page <= cap; page++) {
            ResearchReportPageDTO dto;
            try {
                dto = eastmoneyResearchReportClient.fetchIndustryResearchReports("*", page, ps, beg, end);
            } catch (Exception e) {
                log.warn("syncIndustryResearchReports page={} err={}", page, e.toString());
                break;
            }
            List<ResearchReportItemDTO> items = dto.getList();
            if (items == null || items.isEmpty()) {
                break;
            }
            List<ResearchReportIndustryEntity> batch = new ArrayList<>();
            for (ResearchReportItemDTO item : items) {
                ResearchReportIndustryEntity e = toIndustryEntity(item, beg, end);
                if (e != null) {
                    batch.add(e);
                }
            }
            researchReportIndustryAdminService.upsertBatch(batch);
            affected += batch.size();
            if (items.size() < pageSize) {
                break;
            }
            if (sleepMsPerPage > 0) {
                try {
                    Thread.sleep(sleepMsPerPage);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return affected;
                }
            }
        }
        log.info("syncIndustryResearchReports done, affected={}", affected);
        return affected;
    }

    /**
     * 先个股后行业；参数与单类同步一致。
     */
    public long syncAll(int pageSize, int maxPagesPerStock, int maxIndustryPages, int sleepMsPerStock,
            int sleepMsPerIndustryPage) {
        long n = syncStockResearchReports(pageSize, maxPagesPerStock, sleepMsPerStock);
        n += syncIndustryResearchReports(pageSize, maxIndustryPages, sleepMsPerIndustryPage);
        log.info("syncAll research reports done, totalAffected={}", n);
        return n;
    }

    private ResearchReportStockEntity toStockEntity(ResearchReportItemDTO item, String secCode, String symbol,
            LocalDate beg, LocalDate end) {
        if (item == null) {
            return null;
        }
        LocalDate pd = parsePublishDate(item.getPublishDate());
        if (pd != null && (pd.isBefore(beg) || pd.isAfter(end))) {
            return null;
        }
        String ext = externalId(item);
        String title = item.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "（无标题）";
        }
        if (title.length() > 512) {
            title = title.substring(0, 512);
        }
        ResearchReportStockEntity e = new ResearchReportStockEntity();
        e.setSource(SOURCE);
        e.setExternalId(ext);
        e.setSecCode(secCode);
        e.setSymbol(symbol);
        e.setStockName(trimToNull(item.getStockName()));
        e.setIndustryName(trimToNull(item.getIndustryName()));
        e.setTitle(title);
        e.setOrgSName(trimToNull(item.getOrgSName()));
        e.setPublishDate(pd);
        e.setRatingName(trimToNull(item.getRatingName()));
        e.setReportType(trimToNull(item.getReportType()));
        e.setColumnCode(trimToNull(item.getColumn()));
        e.setInfoCode(trimToNull(item.getInfoCode()));
        e.setEncodeUrl(trimToNull(item.getEncodeUrl()));
        e.setDetailUrl(null);
        e.setRawJson(toRawJson(item));
        e.setFetchedAt(LocalDateTime.now());
        return e;
    }

    private ResearchReportIndustryEntity toIndustryEntity(ResearchReportItemDTO item, LocalDate beg, LocalDate end) {
        if (item == null) {
            return null;
        }
        LocalDate pd = parsePublishDate(item.getPublishDate());
        if (pd != null && (pd.isBefore(beg) || pd.isAfter(end))) {
            return null;
        }
        String ext = externalId(item);
        String title = item.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = "（无标题）";
        }
        if (title.length() > 512) {
            title = title.substring(0, 512);
        }
        String indCode = trimToNull(item.getIndustryCode());
        if (indCode == null) {
            indCode = "*";
        }
        ResearchReportIndustryEntity e = new ResearchReportIndustryEntity();
        e.setSource(SOURCE);
        e.setExternalId(ext);
        e.setIndustryCode(indCode);
        e.setIndustryName(trimToNull(item.getIndustryName()));
        e.setTitle(title);
        e.setOrgSName(trimToNull(item.getOrgSName()));
        e.setPublishDate(pd);
        e.setRatingName(trimToNull(item.getRatingName()));
        e.setReportType(trimToNull(item.getReportType()));
        e.setColumnCode(trimToNull(item.getColumn()));
        e.setInfoCode(trimToNull(item.getInfoCode()));
        e.setEncodeUrl(trimToNull(item.getEncodeUrl()));
        e.setDetailUrl(null);
        e.setRawJson(toRawJson(item));
        e.setFetchedAt(LocalDateTime.now());
        return e;
    }

    private String toRawJson(ResearchReportItemDTO item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static LocalDate parsePublishDate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String v = s.trim();
        if (v.length() >= 10 && v.charAt(4) == '-') {
            try {
                return LocalDate.parse(v.substring(0, 10), ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static String externalId(ResearchReportItemDTO item) {
        String info = item.getInfoCode();
        if (info != null && !info.trim().isEmpty()) {
            return info.trim();
        }
        String base = String.valueOf(item.getTitle()) + "|" + String.valueOf(item.getPublishDate()) + "|"
                + String.valueOf(item.getOrgSName()) + "|" + String.valueOf(item.getStockCode()) + "|"
                + String.valueOf(item.getIndustryName());
        return "m_" + md5Hex(base).substring(0, 32);
    }

    private static String md5Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
