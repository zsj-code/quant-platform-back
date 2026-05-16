package com.quant.platform.business.job;


import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.quant.platform.business.client.EastmoneyStockClient;
import com.quant.platform.business.stock.dto.StockBasicDTO;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.enums.StockDelistStatus;
import com.quant.platform.business.stock.service.StockAdminService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class StockSyncService {

    private static final Logger log = LoggerFactory.getLogger(StockSyncService.class);

    @Autowired
    private StockAdminService stockAdminService;

    @Autowired
    private EastmoneyStockClient eastmoneyStockClient;

    /**
     * @param sleepMsBetweenPages 翻页请求之间的休眠毫秒数；≤0 表示不休眠
     */
    public void syncAllStock(int sleepMsBetweenPages) {
        List<StockBasicDTO> stockBasicDTOList = eastmoneyStockClient.fetchAStocks(1, 10);
        log.info("stock开始同步, sleepMsBetweenPages={}", sleepMsBetweenPages);
        if (stockBasicDTOList != null && !stockBasicDTOList.isEmpty()) {
            addStock(stockBasicDTOList);
            for (int i = 2; i <= 600; i++) {
                sleepBetweenPages(sleepMsBetweenPages);
                stockBasicDTOList = eastmoneyStockClient.fetchAStocks(i, 10);
                log.info("stockBasicDTOList={}", stockBasicDTOList);
                if (stockBasicDTOList == null || stockBasicDTOList.isEmpty()) {
                    break;
                }
                addStock(stockBasicDTOList);
            }
        }
        log.info("stocks同步结束");
    }

    private static void sleepBetweenPages(int sleepMsBetweenPages) {
        if (sleepMsBetweenPages <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(sleepMsBetweenPages);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void addStock(List<StockBasicDTO> stockBasicDTOList) {
        if (stockBasicDTOList == null || stockBasicDTOList.isEmpty()) {
            return;
        }
        var uniqueByCode = new LinkedHashMap<String, StockBasicDTO>();
        for (StockBasicDTO dto : stockBasicDTOList) {
            if (dto.getCode() == null || dto.getCode().isBlank()) {
                continue;
            }
            String code = dto.getCode().trim();
            uniqueByCode.putIfAbsent(code, dto);
        }
        if (uniqueByCode.isEmpty()) {
            return;
        }
        Set<String> existing = stockAdminService.queryByStockCodeList(new ArrayList<>(uniqueByCode.keySet()));
        LocalDateTime now = LocalDateTime.now();
        List<StockEntity> toInsert = new ArrayList<>();
        for (StockBasicDTO dto : uniqueByCode.values()) {
            String code = dto.getCode().trim();
            if (existing.contains(code)) {
                continue;
            }
            StockEntity e = new StockEntity();
            e.setId(IdWorker.getIdStr());
            e.setCode(code);
            e.setName(dto.getName() == null || dto.getName().isBlank() ? "" : dto.getName().trim());
            e.setIsDelisted(StockDelistStatus.LISTED);
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            toInsert.add(e);
        }
        if (!toInsert.isEmpty()) {
            stockAdminService.addStockBatch(toInsert);
        }
    }

}
