package com.quant.platform.business.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.stock.entity.StockPostEntity;
import com.quant.platform.business.stock.mapper.StockPostMapper;
import com.quant.platform.business.stock.service.StockPostAdminService;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockPostAdminServiceImpl implements StockPostAdminService {

    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final StockPostMapper stockPostMapper;

    public StockPostAdminServiceImpl(StockPostMapper stockPostMapper) {
        this.stockPostMapper = stockPostMapper;
    }

    @Override
    public StockPostEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return stockPostMapper.selectById(id.trim());
    }

    @Override
    public List<StockPostEntity> listBySecCode(String secCode) {
        String c = CommonUtil.normalizeSixDigitCode(secCode);
        if (c == null || c.isEmpty()) {
            return List.of();
        }
        return stockPostMapper.selectList(new LambdaQueryWrapper<StockPostEntity>().eq(StockPostEntity::getSecCode, c)
                .orderByDesc(StockPostEntity::getPublishTime));
    }

    @Override
    public PageResult<StockPostEntity> pageBySecCode(String secCode, Long current, Long size) {
        String c0 = CommonUtil.normalizeSixDigitCode(secCode);
        if (c0 == null || c0.isEmpty()) {
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
        var wrapper = new LambdaQueryWrapper<StockPostEntity>().eq(StockPostEntity::getSecCode, c0)
                .orderByDesc(StockPostEntity::getPublishTime);
        var page = stockPostMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public void upsert(StockPostEntity entity) {
        upsertOne(entity);
    }

    @Override
    public void upsertBatch(List<StockPostEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, StockPostEntity> byKey = new LinkedHashMap<>();
        for (StockPostEntity e : entities) {
            if (e == null) {
                continue;
            }
            String source = normalizeSource(e.getSource());
            String ext = trimToNull(e.getExternalId());
            if (ext == null) {
                continue;
            }
            e.setSource(source);
            e.setExternalId(ext);
            String sec = CommonUtil.normalizeSixDigitCode(e.getSecCode());
            if (sec != null && !sec.isEmpty()) {
                e.setSecCode(sec);
            }
            if (e.getFetchedAt() == null) {
                e.setFetchedAt(LocalDateTime.now());
            }
            if (e.getId() == null || e.getId().isEmpty()) {
                e.setId(IdWorker.getIdStr());
            }
            byKey.put(source + "|" + ext, e);
        }
        if (byKey.isEmpty()) {
            return;
        }
        List<StockPostEntity> rows = new ArrayList<>(byKey.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            stockPostMapper.upsertBatch(rows.subList(i, end));
        }
    }

    private void upsertOne(StockPostEntity e) {
        if (e == null) {
            return;
        }
        String source = normalizeSource(e.getSource());
        String ext = trimToNull(e.getExternalId());
        if (ext == null) {
            return;
        }
        e.setSource(source);
        e.setExternalId(ext);
        String sec = CommonUtil.normalizeSixDigitCode(e.getSecCode());
        if (sec != null && !sec.isEmpty()) {
            e.setSecCode(sec);
        }
        if (e.getFetchedAt() == null) {
            e.setFetchedAt(LocalDateTime.now());
        }

        StockPostEntity existing = stockPostMapper.selectOne(new LambdaQueryWrapper<StockPostEntity>()
                .eq(StockPostEntity::getSource, source).eq(StockPostEntity::getExternalId, ext).last("limit 1"));
        if (existing != null && existing.getId() != null && !existing.getId().isEmpty()) {
            // 避免将已存在的 publish_time 覆盖成 null（或缺失字段导致的空值）。
            if (e.getPublishTime() == null) {
                e.setPublishTime(existing.getPublishTime());
            }
            e.setId(existing.getId());
            stockPostMapper.updateById(e);
            return;
        }
        if (e.getId() == null || e.getId().isEmpty()) {
            e.setId(IdWorker.getIdStr());
        }
        stockPostMapper.insert(e);
    }

    private static String normalizeSource(String source) {
        String s = trimToNull(source);
        return s == null ? "EASTMONEY_GUBA" : s;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

