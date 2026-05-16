package com.quant.platform.business.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.stock.entity.StockValuationSnapshotEntity;
import com.quant.platform.business.stock.mapper.StockValuationSnapshotMapper;
import com.quant.platform.business.stock.service.StockValuationSnapshotAdminService;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockValuationSnapshotAdminServiceImpl implements StockValuationSnapshotAdminService {

    /** 单次 INSERT 行数上限，避免超过 max_allowed_packet */
    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final StockValuationSnapshotMapper stockValuationSnapshotMapper;

    public StockValuationSnapshotAdminServiceImpl(StockValuationSnapshotMapper stockValuationSnapshotMapper) {
        this.stockValuationSnapshotMapper = stockValuationSnapshotMapper;
    }

    @Override
    public StockValuationSnapshotEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return stockValuationSnapshotMapper.selectById(id);
    }

    @Override
    public StockValuationSnapshotEntity getBySymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return null;
        }
        return stockValuationSnapshotMapper.selectOne(new LambdaQueryWrapper<StockValuationSnapshotEntity>()
                .eq(StockValuationSnapshotEntity::getSymbol, symbol.trim()).last("limit 1"));
    }

    @Override
    public StockValuationSnapshotEntity getBySecCode(String secCode) {
        if (secCode == null || secCode.trim().isEmpty()) {
            return null;
        }
        return stockValuationSnapshotMapper.selectOne(new LambdaQueryWrapper<StockValuationSnapshotEntity>()
                .eq(StockValuationSnapshotEntity::getSecCode, secCode.trim()).last("limit 1"));
    }

    @Override
    public PageResult<StockValuationSnapshotEntity> page(String secCode, String symbol, Long current, Long size) {
        long c = current == null ? 1L : current;
        long s = size == null ? 20L : size;
        if (c < 1) {
            c = 1L;
        }
        if (s < 1) {
            s = 20L;
        }
        if (s > 2000) {
            s = 2000L;
        }
        String sc = secCode == null ? null : secCode.trim();
        String sy = symbol == null ? null : symbol.trim();

        var wrapper = new LambdaQueryWrapper<StockValuationSnapshotEntity>()
                .eq(sc != null && !sc.isEmpty(), StockValuationSnapshotEntity::getSecCode, sc)
                .eq(sy != null && !sy.isEmpty(), StockValuationSnapshotEntity::getSymbol, sy)
                .orderByDesc(StockValuationSnapshotEntity::getFetchedAt);

        var page = stockValuationSnapshotMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public void upsert(StockValuationSnapshotEntity entity) {
        upsertOne(entity);
    }

    @Override
    public void upsertBatch(List<StockValuationSnapshotEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, StockValuationSnapshotEntity> bySymbol = new LinkedHashMap<>();
        for (StockValuationSnapshotEntity entity : entities) {
            if (entity == null) {
                continue;
            }
            String symbol = entity.getSymbol();
            if (symbol == null || symbol.trim().isEmpty()) {
                continue;
            }
            String sym = symbol.trim();
            entity.setSymbol(sym);
            if (entity.getSecCode() == null || entity.getSecCode().trim().isEmpty()) {
                String sc = CommonUtil.normalizeSixDigitCode(sym);
                if (sc != null && !sc.isEmpty()) {
                    entity.setSecCode(sc);
                }
            }
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(IdWorker.getIdStr());
            }
            bySymbol.put(sym, entity);
        }
        if (bySymbol.isEmpty()) {
            return;
        }
        List<StockValuationSnapshotEntity> rows = new ArrayList<>(bySymbol.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            stockValuationSnapshotMapper.upsertBatch(rows.subList(i, end));
        }
    }

    private void upsertOne(StockValuationSnapshotEntity entity) {
        if (entity == null) {
            return;
        }
        String symbol = entity.getSymbol();
        if (symbol == null || symbol.trim().isEmpty()) {
            return;
        }
        String sym = symbol.trim();
        if (entity.getSecCode() == null || entity.getSecCode().trim().isEmpty()) {
            String sc = CommonUtil.normalizeSixDigitCode(sym);
            if (sc != null && !sc.isEmpty()) {
                entity.setSecCode(sc);
            }
        }
        StockValuationSnapshotEntity existing = stockValuationSnapshotMapper.selectOne(
                new LambdaQueryWrapper<StockValuationSnapshotEntity>().eq(StockValuationSnapshotEntity::getSymbol, sym)
                        .last("limit 1"));
        if (existing != null && existing.getId() != null) {
            entity.setId(existing.getId());
            stockValuationSnapshotMapper.updateById(entity);
        } else {
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(IdWorker.getIdStr());
            }
            stockValuationSnapshotMapper.insert(entity);
        }
    }
}
