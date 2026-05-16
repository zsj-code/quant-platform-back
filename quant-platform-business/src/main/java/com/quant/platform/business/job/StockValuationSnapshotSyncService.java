package com.quant.platform.business.job;


import com.quant.platform.business.client.EastmoneyStockValuationClient;
import com.quant.platform.business.stock.dto.EastmoneyStockValuationDTO;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.entity.StockValuationSnapshotEntity;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.business.stock.service.StockValuationSnapshotAdminService;
import com.quant.platform.common.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 东财个股快照（估值/行情）同步至 {@code stock_valuation_snapshot}，数据源 {@link EastmoneyStockValuationClient}。
 */
@Service
public class StockValuationSnapshotSyncService {

    private static final Logger log = LoggerFactory.getLogger(StockValuationSnapshotSyncService.class);

    private static final int UPSERT_FLUSH_SIZE = 200;

    private final StockAdminService stockAdminService;
    private final EastmoneyStockValuationClient eastmoneyStockValuationClient;
    private final StockValuationSnapshotAdminService stockValuationSnapshotAdminService;

    public StockValuationSnapshotSyncService(StockAdminService stockAdminService,
            EastmoneyStockValuationClient eastmoneyStockValuationClient,
            StockValuationSnapshotAdminService stockValuationSnapshotAdminService) {
        this.stockAdminService = stockAdminService;
        this.eastmoneyStockValuationClient = eastmoneyStockValuationClient;
        this.stockValuationSnapshotAdminService = stockValuationSnapshotAdminService;
    }

    /**
     * 按本地 {@link StockEntity} 列表逐只拉取东财快照并 upsert。
     *
     * @param sleepMsPerStock
     *            每只请求后的休眠（毫秒），减轻对东财接口压力；≤0 则不睡
     * @param maxStocks
     *            最多处理只数，≤0 表示不限制
     * @return 成功写入缓冲并触发 upsert 的条数（含 null 报价跳过）
     */
    public long syncAll(int sleepMsPerStock, int maxStocks) {
        List<StockEntity> stocks = stockAdminService.listNonDelisted();
        if (stocks == null || stocks.isEmpty()) {
            log.warn("stock valuation sync skipped: no stocks in local table");
            return 0L;
        }
        int limit = maxStocks <= 0 ? Integer.MAX_VALUE : maxStocks;
        List<StockValuationSnapshotEntity> batch = new ArrayList<>(UPSERT_FLUSH_SIZE);
        long success = 0L;
        int processed = 0;
        for (StockEntity stock : stocks) {
            if (stock == null) {
                continue;
            }
            if (processed >= limit) {
                break;
            }
            String code = stock.getCode();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            processed++;
            try {
                EastmoneyStockValuationDTO dto = eastmoneyStockValuationClient.fetchValuationSnapshot(code.trim());
                StockValuationSnapshotEntity row = toEntity(stock, dto);
                if (row != null) {
                    batch.add(row);
                    success++;
                    if (batch.size() >= UPSERT_FLUSH_SIZE) {
                        stockValuationSnapshotAdminService.upsertBatch(batch);
                        batch.clear();
                    }
                }
            } catch (Exception e) {
                log.warn("stock valuation sync failed code={}: {}", code, e.toString());
            }
            if (sleepMsPerStock > 0) {
                try {
                    Thread.sleep(sleepMsPerStock);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    flushBatch(batch);
                    return success;
                }
            }
        }
        flushBatch(batch);
        log.info("stock valuation sync done, processedStocks={}, rowsUpserted={}", processed, success);
        return success;
    }

    private void flushBatch(List<StockValuationSnapshotEntity> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        stockValuationSnapshotAdminService.upsertBatch(batch);
        batch.clear();
    }

    private static StockValuationSnapshotEntity toEntity(StockEntity stock, EastmoneyStockValuationDTO dto) {
        if (dto == null) {
            return null;
        }
        String secCode = CommonUtil.normalizeSixDigitCode(stock.getCode());
        if (secCode == null || secCode.isEmpty()) {
            return null;
        }
        String symbol = CommonUtil.toSymbol(secCode);
        if (symbol == null) {
            return null;
        }
        StockValuationSnapshotEntity e = new StockValuationSnapshotEntity();
        e.setSymbol(symbol);
        e.setSecCode(secCode);
        String name = dto.getName();
        if (name == null || name.isBlank()) {
            name = stock.getName();
        }
        e.setStockName(name == null || name.isBlank() ? null : name.trim());
        e.setLatestPrice(dto.getLatestPrice());
        e.setChangePct(deriveChangePct(dto.getLatestPrice(), dto.getPrevClose()));
        e.setVolume(dto.getVolume());
        e.setAmount(dto.getAmount());
        e.setTurnoverRate(dto.getTurnoverRate());
        e.setVolumeRatio(dto.getVolumeRatio());
        e.setPrevClose(dto.getPrevClose());
        e.setOpenPrice(dto.getOpenPrice());
        e.setHighPrice(dto.getHighPrice());
        e.setLowPrice(dto.getLowPrice());
        e.setAvgPrice(dto.getAvgPrice());
        e.setLimitUp(dto.getLimitUp());
        e.setLimitDown(dto.getLimitDown());
        e.setAmplitude(dto.getAmplitude());
        e.setTotalMarketCapYuan(dto.getTotalMarketCap());
        e.setCircMarketCapYuan(dto.getFloatMarketCap());
        e.setTotalShares(dto.getTotalShares());
        e.setFloatShares(dto.getFloatShares());
        e.setPeDynamic(dto.getPeDynamic());
        e.setPc(dto.getPc());
        e.setPeStatic(dto.getPeStatic());
        e.setPs(dto.getPs());
        e.setPb(dto.getPb());
        e.setRawQuoteJson(dto.getRawQuoteJson());
        e.setFetchedAt(LocalDateTime.now());
        return e;
    }

    private static BigDecimal deriveChangePct(BigDecimal latestPrice, BigDecimal prevClose) {
        if (latestPrice == null || prevClose == null) {
            return null;
        }
        if (prevClose.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return latestPrice.subtract(prevClose).divide(prevClose, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(6, RoundingMode.HALF_UP);
    }
}
