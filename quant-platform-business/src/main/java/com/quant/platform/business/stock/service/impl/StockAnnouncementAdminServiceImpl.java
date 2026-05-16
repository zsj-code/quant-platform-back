package com.quant.platform.business.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.stock.entity.StockAnnouncementEntity;
import com.quant.platform.business.stock.mapper.StockAnnouncementMapper;
import com.quant.platform.business.stock.service.StockAnnouncementAdminService;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockAnnouncementAdminServiceImpl implements StockAnnouncementAdminService {

    /** 单次 INSERT 行数上限，避免超过 max_allowed_packet */
    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final StockAnnouncementMapper stockAnnouncementMapper;

    public StockAnnouncementAdminServiceImpl(StockAnnouncementMapper stockAnnouncementMapper) {
        this.stockAnnouncementMapper = stockAnnouncementMapper;
    }

    @Override
    public StockAnnouncementEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return stockAnnouncementMapper.selectById(id);
    }

    @Override
    public List<StockAnnouncementEntity> listByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return List.of();
        }
        return stockAnnouncementMapper.selectList(new LambdaQueryWrapper<StockAnnouncementEntity>()
                .eq(StockAnnouncementEntity::getCode, code.trim())
                .orderByDesc(StockAnnouncementEntity::getAnnounceTime));
    }

    @Override
    public PageResult<StockAnnouncementEntity> pageByCode(String code, Long current, Long size) {
        if (code == null || code.trim().isEmpty()) {
            return PageResult.of(1L, 20L, 0L, List.of());
        }
        long c = current == null ? 1L : current;
        long s = size == null ? 20L : size;
        if (c < 1) {
            c = 1L;
        }
        if (s < 1) {
            s = 20L;
        }
        if (s > 500) {
            s = 500L;
        }
        var wrapper = new LambdaQueryWrapper<StockAnnouncementEntity>()
                .eq(StockAnnouncementEntity::getCode, code.trim())
                .orderByDesc(StockAnnouncementEntity::getAnnounceTime);
        var page = stockAnnouncementMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public void upsert(StockAnnouncementEntity entity) {
        upsertOne(entity);
    }

    @Override
    public void upsertBatch(List<StockAnnouncementEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, StockAnnouncementEntity> byKey = new LinkedHashMap<>();
        for (StockAnnouncementEntity entity : entities) {
            if (entity == null) {
                continue;
            }
            String symbol = entity.getSymbol();
            if (symbol == null || symbol.trim().isEmpty()) {
                continue;
            }
            String sym = symbol.trim();
            entity.setSymbol(sym);
            String existingCode = entity.getCode();
            if (existingCode == null || existingCode.trim().isEmpty()) {
                String co = CommonUtil.normalizeSixDigitCode(sym);
                if (co != null && !co.isEmpty()) {
                    entity.setCode(co);
                }
            }
            String source = entity.getSource() != null && !entity.getSource().trim().isEmpty()
                    ? entity.getSource().trim()
                    : "EASTMONEY";
            entity.setSource(source);
            String ext = entity.getExternalId();
            if (ext == null || ext.trim().isEmpty()) {
                continue;
            }
            String extTrim = ext.trim();
            entity.setExternalId(extTrim);
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(IdWorker.getIdStr());
            }
            String key = source + "|" + extTrim;
            byKey.put(key, entity);
        }
        if (byKey.isEmpty()) {
            return;
        }
        List<StockAnnouncementEntity> rows = new ArrayList<>(byKey.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            stockAnnouncementMapper.upsertBatch(rows.subList(i, end));
        }
    }

    private void upsertOne(StockAnnouncementEntity entity) {
        if (entity == null) {
            return;
        }
        String symbol = entity.getSymbol();
        if (symbol == null || symbol.trim().isEmpty()) {
            return;
        }
        String sym = symbol.trim();
        String existingCode = entity.getCode();
        if (existingCode == null || existingCode.trim().isEmpty()) {
            String co = CommonUtil.normalizeSixDigitCode(sym);
            if (co != null && !co.isEmpty()) {
                entity.setCode(co);
            }
        }
        String source = entity.getSource() != null && !entity.getSource().trim().isEmpty()
                ? entity.getSource().trim()
                : "EASTMONEY";
        entity.setSource(source);
        String ext = entity.getExternalId();
        if (ext == null || ext.trim().isEmpty()) {
            return;
        }
        StockAnnouncementEntity existing = stockAnnouncementMapper.selectOne(
                new LambdaQueryWrapper<StockAnnouncementEntity>().eq(StockAnnouncementEntity::getSource, source)
                        .eq(StockAnnouncementEntity::getExternalId, ext.trim()).last("limit 1"));
        if (existing != null && existing.getId() != null) {
            entity.setId(existing.getId());
            stockAnnouncementMapper.updateById(entity);
        } else {
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(IdWorker.getIdStr());
            }
            stockAnnouncementMapper.insert(entity);
        }
    }
}
