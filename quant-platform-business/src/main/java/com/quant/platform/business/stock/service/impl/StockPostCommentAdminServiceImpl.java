package com.quant.platform.business.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.stock.entity.StockPostCommentEntity;
import com.quant.platform.business.stock.mapper.StockPostCommentMapper;
import com.quant.platform.business.stock.service.StockPostCommentAdminService;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockPostCommentAdminServiceImpl implements StockPostCommentAdminService {

    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final StockPostCommentMapper stockPostCommentMapper;

    public StockPostCommentAdminServiceImpl(StockPostCommentMapper stockPostCommentMapper) {
        this.stockPostCommentMapper = stockPostCommentMapper;
    }

    @Override
    public StockPostCommentEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return stockPostCommentMapper.selectById(id.trim());
    }

    @Override
    public List<StockPostCommentEntity> listByPostExternalId(String postExternalId) {
        String p = trimToNull(postExternalId);
        if (p == null) {
            return List.of();
        }
        return stockPostCommentMapper.selectList(new LambdaQueryWrapper<StockPostCommentEntity>()
                .eq(StockPostCommentEntity::getPostExternalId, p).orderByAsc(StockPostCommentEntity::getPublishTime)
                .orderByAsc(StockPostCommentEntity::getFloorNo));
    }

    @Override
    public PageResult<StockPostCommentEntity> pageByPostExternalId(String postExternalId, Long current, Long size) {
        String p = trimToNull(postExternalId);
        if (p == null) {
            return PageResult.of(1L, 50L, 0L, List.of());
        }
        long c = current == null ? 1L : current;
        long s = size == null ? 50L : size;
        if (c < 1) {
            c = 1L;
        }
        if (s < 1) {
            s = 50L;
        }
        if (s > 500) {
            s = 500L;
        }
        var wrapper = new LambdaQueryWrapper<StockPostCommentEntity>()
                .eq(StockPostCommentEntity::getPostExternalId, p).orderByAsc(StockPostCommentEntity::getPublishTime)
                .orderByAsc(StockPostCommentEntity::getFloorNo);
        var page = stockPostCommentMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public void upsert(StockPostCommentEntity entity) {
        upsertOne(entity);
    }

    @Override
    public void upsertBatch(List<StockPostCommentEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, StockPostCommentEntity> byKey = new LinkedHashMap<>();
        for (StockPostCommentEntity e : entities) {
            if (e == null) {
                continue;
            }
            String source = normalizeSource(e.getSource());
            String ext = trimToNull(e.getExternalId());
            String postExt = trimToNull(e.getPostExternalId());
            if (ext == null || postExt == null) {
                continue;
            }
            e.setSource(source);
            e.setExternalId(ext);
            e.setPostExternalId(postExt);
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
        List<StockPostCommentEntity> rows = new ArrayList<>(byKey.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            stockPostCommentMapper.upsertBatch(rows.subList(i, end));
        }
    }

    private void upsertOne(StockPostCommentEntity e) {
        if (e == null) {
            return;
        }
        String source = normalizeSource(e.getSource());
        String ext = trimToNull(e.getExternalId());
        String postExt = trimToNull(e.getPostExternalId());
        if (ext == null || postExt == null) {
            return;
        }
        e.setSource(source);
        e.setExternalId(ext);
        e.setPostExternalId(postExt);
        String sec = CommonUtil.normalizeSixDigitCode(e.getSecCode());
        if (sec != null && !sec.isEmpty()) {
            e.setSecCode(sec);
        }
        if (e.getFetchedAt() == null) {
            e.setFetchedAt(LocalDateTime.now());
        }

        StockPostCommentEntity existing = stockPostCommentMapper.selectOne(new LambdaQueryWrapper<StockPostCommentEntity>()
                .eq(StockPostCommentEntity::getSource, source).eq(StockPostCommentEntity::getExternalId, ext)
                .last("limit 1"));
        if (existing != null && existing.getId() != null && !existing.getId().isEmpty()) {
            e.setId(existing.getId());
            stockPostCommentMapper.updateById(e);
            return;
        }
        if (e.getId() == null || e.getId().isEmpty()) {
            e.setId(IdWorker.getIdStr());
        }
        stockPostCommentMapper.insert(e);
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

