package com.quant.platform.business.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.stock.entity.StockIndustryValuationEntity;
import com.quant.platform.business.stock.mapper.StockIndustryValuationMapper;
import com.quant.platform.business.stock.service.StockIndustryValuationAdminService;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockIndustryValuationAdminServiceImpl implements StockIndustryValuationAdminService {

    /** 单次 INSERT 行数上限，避免超过 max_allowed_packet */
    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final StockIndustryValuationMapper stockIndustryValuationMapper;

    public StockIndustryValuationAdminServiceImpl(StockIndustryValuationMapper stockIndustryValuationMapper) {
        this.stockIndustryValuationMapper = stockIndustryValuationMapper;
    }

    @Override
    public StockIndustryValuationEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return stockIndustryValuationMapper.selectById(id);
    }

    @Override
    public StockIndustryValuationEntity getBySecCode(String secCode) {
        if (secCode == null || secCode.trim().isEmpty()) {
            return null;
        }
        return stockIndustryValuationMapper.selectOne(new LambdaQueryWrapper<StockIndustryValuationEntity>()
                .eq(StockIndustryValuationEntity::getSecCode, secCode.trim()).last("limit 1"));
    }

    @Override
    public PageResult<StockIndustryValuationEntity> page(String secCode, String symbol, Long current, Long size) {
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

        var wrapper = new LambdaQueryWrapper<StockIndustryValuationEntity>()
                .eq(sc != null && !sc.isEmpty(), StockIndustryValuationEntity::getSecCode, sc)
                .eq(sy != null && !sy.isEmpty(), StockIndustryValuationEntity::getSymbol, sy)
                .orderByDesc(StockIndustryValuationEntity::getFetchedAt);

        var page = stockIndustryValuationMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public void upsert(StockIndustryValuationEntity entity) {
        upsertOne(entity);
    }

    @Override
    public void upsertBatch(List<StockIndustryValuationEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, StockIndustryValuationEntity> bySymbol = new LinkedHashMap<>();
        for (StockIndustryValuationEntity entity : entities) {
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
                String co = CommonUtil.normalizeSixDigitCode(sym);
                if (co != null && !co.isEmpty()) {
                    entity.setSecCode(co);
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
        List<StockIndustryValuationEntity> rows = new ArrayList<>(bySymbol.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            stockIndustryValuationMapper.upsertBatch(rows.subList(i, end));
        }
    }

    private void upsertOne(StockIndustryValuationEntity entity) {
        if (entity == null) {
            return;
        }
        String symbol = entity.getSymbol();
        if (symbol == null || symbol.trim().isEmpty()) {
            return;
        }
        String sym = symbol.trim();
        String existingSec = entity.getSecCode();
        if (existingSec == null || existingSec.trim().isEmpty()) {
            String co = CommonUtil.normalizeSixDigitCode(sym);
            if (co != null && !co.isEmpty()) {
                entity.setSecCode(co);
            }
        }
        StockIndustryValuationEntity existing = stockIndustryValuationMapper.selectOne(
                new LambdaQueryWrapper<StockIndustryValuationEntity>().eq(StockIndustryValuationEntity::getSymbol, sym)
                        .last("limit 1"));
        if (existing != null && existing.getId() != null) {
            entity.setId(existing.getId());
            stockIndustryValuationMapper.updateById(entity);
        } else {
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(IdWorker.getIdStr());
            }
            stockIndustryValuationMapper.insert(entity);
        }
    }
}
