package com.quant.platform.business.research.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.quant.platform.business.research.entity.ResearchReportStockEntity;
import com.quant.platform.business.research.mapper.ResearchReportStockMapper;
import com.quant.platform.business.research.service.ResearchReportStockAdminService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResearchReportStockAdminServiceImpl implements ResearchReportStockAdminService {

    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final ResearchReportStockMapper researchReportStockMapper;

    public ResearchReportStockAdminServiceImpl(ResearchReportStockMapper researchReportStockMapper) {
        this.researchReportStockMapper = researchReportStockMapper;
    }

    @Override
    public List<ResearchReportStockEntity> listBySecCodeSince(String secCode, LocalDate since) {
        if (secCode == null || secCode.trim().isEmpty()) {
            return List.of();
        }
        return researchReportStockMapper.selectList(new LambdaQueryWrapper<ResearchReportStockEntity>()
                .eq(ResearchReportStockEntity::getSecCode, secCode.trim())
                .ge(since != null, ResearchReportStockEntity::getPublishDate, since)
                .orderByDesc(ResearchReportStockEntity::getPublishDate));
    }

    @Override
    public void upsertBatch(List<ResearchReportStockEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, ResearchReportStockEntity> byKey = new LinkedHashMap<>();
        for (ResearchReportStockEntity e : entities) {
            if (e == null || e.getExternalId() == null || e.getExternalId().trim().isEmpty()) {
                continue;
            }
            String src = e.getSource() != null && !e.getSource().trim().isEmpty() ? e.getSource().trim() : "EASTMONEY";
            e.setSource(src);
            e.setExternalId(e.getExternalId().trim());
            if (e.getId() == null || e.getId().isEmpty()) {
                e.setId(IdWorker.getIdStr());
            }
            byKey.put(src + "|" + e.getExternalId(), e);
        }
        if (byKey.isEmpty()) {
            return;
        }
        List<ResearchReportStockEntity> rows = new ArrayList<>(byKey.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            researchReportStockMapper.upsertBatch(rows.subList(i, end));
        }
    }
}
