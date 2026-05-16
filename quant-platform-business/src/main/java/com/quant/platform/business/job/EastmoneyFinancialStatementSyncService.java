package com.quant.platform.business.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quant.platform.business.client.EastmoneyFinancialStatementClient;
import com.quant.platform.business.financial.dto.EastmoneyFinancialStatementPageDTO;
import com.quant.platform.business.financial.entity.FinancialStatementEntity;
import com.quant.platform.business.financial.service.FinancialStatementAdminService;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 从东方财富数据中心同步财务报表至表 {@code financial_statement}（按 code + report_type +
 * report_date 幂等覆盖）。
 */
@Service
public class EastmoneyFinancialStatementSyncService {

    private static final Logger log = LoggerFactory.getLogger(EastmoneyFinancialStatementSyncService.class);

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /** 仅落库报告期在 2015-01-01（含）之后的数据 */
    private static final LocalDate REPORT_DATE_MIN_INCLUSIVE = LocalDate.of(2015, 1, 1);

    private final StockAdminService stockAdminService;
    private final FinancialStatementAdminService financialStatementAdminService;
    private final EastmoneyFinancialStatementClient eastmoneyFinancialStatementClient;
    private final ObjectMapper objectMapper;

    public EastmoneyFinancialStatementSyncService(StockAdminService stockAdminService,
            FinancialStatementAdminService financialStatementAdminService,
            EastmoneyFinancialStatementClient eastmoneyFinancialStatementClient, ObjectMapper objectMapper) {
        this.stockAdminService = stockAdminService;
        this.financialStatementAdminService = financialStatementAdminService;
        this.eastmoneyFinancialStatementClient = eastmoneyFinancialStatementClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 全市场同步三类报表全部分页。
     *
     * @param sleepMsPerStock
     *            每只股票处理完后休眠毫秒数，减轻源站压力；≤0 不休眠
     * @param maxPagesPerType
     *            每种报表最多拉取页数；≤0 表示不限制（直至接口无数据或达到总页数）
     */
    public long syncAll(int sleepMsPerStock, int maxPagesPerType) {
        log.info("syncAll start");
        List<StockEntity> stocks = stockAdminService.listNonDelisted();
        if (stocks == null || stocks.isEmpty()) {
            log.info("syncFinancialStatement: no stocks");
            return 0L;
        }
        LocalDate targetReportDate = previousQuarterEnd(LocalDate.now());
        int pageCap = maxPagesPerType > 0 ? maxPagesPerType : 20;
        long affected = 0L;
        for (StockEntity stock : stocks) {
            if (stock == null) {
                continue;
            }
            String code = CommonUtil.normalizeSixDigitCode(stock.getCode());
            if (code == null || code.isEmpty()) {
                continue;
            }
            try {
                // 如果最新报告期已存在，则跳过该股票，避免无意义的 HTTP 拉取
                LocalDate latestReportDate = null;
                List<LocalDate> reportDates = financialStatementAdminService.listDistinctReportDatesByCode(code);
                if (reportDates != null && !reportDates.isEmpty()) {
                    latestReportDate = reportDates.get(0);
                }
                if (latestReportDate != null && latestReportDate.equals(targetReportDate)) {
                    log.info("syncFinancialStatement skip http, latest report_date exists. code={} report_date={}", code,
                            targetReportDate);
                    continue;
                }
                for (EastmoneyFinancialStatementReportTypeEnum type : EastmoneyFinancialStatementReportTypeEnum.values()) {
                    affected += syncReportTypeForStock(code, type, pageCap);
                }
            } catch (Exception e) {
                log.warn("syncFinancialStatement skip stock code={} err={}", code, e.toString());
            }
            if (sleepMsPerStock > 0) {
                try {
                    Thread.sleep(sleepMsPerStock);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("syncFinancialStatement interrupted");
                    return affected;
                }
            }
        }
        log.info("syncFinancialStatement done, affected={}", affected);
        return affected;
    }

    /**
     * 取“当前日期所在季度”的上一个季度季末日期。
     * <p>
     * 例如：2026-04-27 → 2026-03-31；2026-01-01 → 2025-12-31。
     */
    static LocalDate previousQuarterEnd(LocalDate today) {
        if (today == null) {
            today = LocalDate.now();
        }
        int month = today.getMonthValue();
        int quarter = ((month - 1) / 3) + 1; // 1..4
        int firstMonthOfQuarter = ((quarter - 1) * 3) + 1;
        LocalDate firstDayOfQuarter = LocalDate.of(today.getYear(), firstMonthOfQuarter, 1);
        return firstDayOfQuarter.minusDays(1);
    }

    private long syncReportTypeForStock(String code, EastmoneyFinancialStatementReportTypeEnum type, int pageCap)
            throws IOException {
        long n = 0L;
        int page = 1;
        while (page <= pageCap) {
            EastmoneyFinancialStatementPageDTO dto = eastmoneyFinancialStatementClient.fetchPage(code, type, page);
            if (dto.getRows() == null || dto.getRows().isEmpty()) {
                break;
            }
            n += upsertRows(code, type, dto);
            Integer totalPages = dto.getTotalPages();
            if (totalPages != null && page >= totalPages) {
                break;
            }
            if (dto.getRows().size() < EastmoneyFinancialStatementClient.PAGE_SIZE) {
                break;
            }
            page++;
        }
        return n;
    }

    private long upsertRows(String code, EastmoneyFinancialStatementReportTypeEnum type,
            EastmoneyFinancialStatementPageDTO page) {
        String symbol = CommonUtil.toSymbol(code);
        String reportType = type.name();
        String sourceReportName = page.getReportName();
        LocalDateTime fetchedAt = LocalDateTime.now();
        List<FinancialStatementEntity> batch = new ArrayList<>();
        for (Map<String, Object> row : page.getRows()) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            LocalDate reportDate = parseReportDate(row.get("REPORT_DATE"));
            if (reportDate == null) {
                log.debug("skip row without REPORT_DATE code={} type={} keys={}", code, reportType, row.keySet());
                continue;
            }
            if (reportDate.isBefore(REPORT_DATE_MIN_INCLUSIVE)) {
                continue;
            }
            String rawJson;
            try {
                rawJson = objectMapper.writeValueAsString(row);
            } catch (JsonProcessingException e) {
                log.warn("skip row json error code={} type={} date={}", code, reportType, reportDate, e);
                continue;
            }

            FinancialStatementEntity entity = new FinancialStatementEntity();
            entity.setCode(code);
            entity.setSymbol(symbol);
            entity.setReportType(reportType);
            entity.setReportDate(reportDate);
            entity.setRawJson(rawJson);
            entity.setSourceReportName(sourceReportName);
            entity.setFetchedAt(fetchedAt);

            batch.add(entity);
        }
        financialStatementAdminService.upsertBatch(batch);
        return batch.size();
    }

    static LocalDate parseReportDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            try {
                return LocalDate.parse(s.substring(0, 10), ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        if (s.length() >= 8 && Character.isDigit(s.charAt(0))) {
            try {
                return LocalDate.parse(s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6, 8), ISO_DATE);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
