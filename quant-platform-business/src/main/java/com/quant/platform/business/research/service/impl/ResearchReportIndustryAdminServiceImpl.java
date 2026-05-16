package com.quant.platform.business.research.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.quant.platform.business.research.entity.ResearchReportIndustryEntity;
import com.quant.platform.business.research.mapper.ResearchReportIndustryMapper;
import com.quant.platform.business.research.service.ResearchReportIndustryAdminService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResearchReportIndustryAdminServiceImpl implements ResearchReportIndustryAdminService {

    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final ResearchReportIndustryMapper researchReportIndustryMapper;

    public ResearchReportIndustryAdminServiceImpl(ResearchReportIndustryMapper researchReportIndustryMapper) {
        this.researchReportIndustryMapper = researchReportIndustryMapper;
    }

    @Override
    public List<ResearchReportIndustryEntity> listByIndustryCodeSince(String industryCode, LocalDate since) {
        if (industryCode == null || industryCode.trim().isEmpty()) {
            return List.of();
        }
        return researchReportIndustryMapper.selectList(new LambdaQueryWrapper<ResearchReportIndustryEntity>()
                .eq(ResearchReportIndustryEntity::getIndustryCode, industryCode.trim())
                .ge(since != null, ResearchReportIndustryEntity::getPublishDate, since)
                .orderByDesc(ResearchReportIndustryEntity::getPublishDate));
    }

    @Override
    public void upsertBatch(List<ResearchReportIndustryEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, ResearchReportIndustryEntity> byKey = new LinkedHashMap<>();
        for (ResearchReportIndustryEntity e : entities) {
            if (e == null || e.getExternalId() == null || e.getExternalId().trim().isEmpty()) {
                continue;
            }
            String src = e.getSource() != null && !e.getSource().trim().isEmpty() ? e.getSource().trim() : "EASTMONEY";
            e.setSource(src);
            e.setExternalId(e.getExternalId().trim());
            if (e.getIndustryCode() == null || e.getIndustryCode().trim().isEmpty()) {
                e.setIndustryCode("*");
            } else {
                e.setIndustryCode(e.getIndustryCode().trim());
            }
            if (e.getId() == null || e.getId().isEmpty()) {
                e.setId(IdWorker.getIdStr());
            }
            byKey.put(src + "|" + e.getExternalId(), e);
        }
        if (byKey.isEmpty()) {
            return;
        }
        List<ResearchReportIndustryEntity> rows = new ArrayList<>(byKey.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            researchReportIndustryMapper.upsertBatch(rows.subList(i, end));
        }
    }
}
