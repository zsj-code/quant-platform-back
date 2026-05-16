package com.quant.platform.business.financial.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.financial.entity.FinancialStatementEntity;
import com.quant.platform.business.financial.mapper.FinancialStatementMapper;
import com.quant.platform.business.financial.service.FinancialStatementAdminService;
import com.quant.platform.common.api.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinancialStatementAdminServiceImpl implements FinancialStatementAdminService {

    /** 单次 INSERT 行数上限，避免超过 max_allowed_packet */
    private static final int UPSERT_BATCH_CHUNK_SIZE = 500;

    private final FinancialStatementMapper financialStatementMapper;

    public FinancialStatementAdminServiceImpl(FinancialStatementMapper financialStatementMapper) {
        this.financialStatementMapper = financialStatementMapper;
    }

    @Override
    public FinancialStatementEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return financialStatementMapper.selectById(id);
    }

    @Override
    public PageResult<FinancialStatementEntity> page(String code, String reportType, LocalDate startDate,
            LocalDate endDate, Long current, Long size) {
        long c = current == null ? 1L : current;
        long s = size == null ? 50L : size;
        if (c < 1) {
            c = 1L;
        }
        if (s < 1) {
            s = 50L;
        }
        if (s > 2000) {
            s = 2000L;
        }

        var wrapper = baseWrapper(code, reportType, startDate, endDate)
                .orderByDesc(FinancialStatementEntity::getReportDate)
                .orderByDesc(FinancialStatementEntity::getFetchedAt);
        var page = financialStatementMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public List<FinancialStatementEntity> list(String code, String reportType, LocalDate startDate, LocalDate endDate) {
        var wrapper = baseWrapper(code, reportType, startDate, endDate)
                .orderByDesc(FinancialStatementEntity::getReportDate)
                .orderByDesc(FinancialStatementEntity::getFetchedAt);
        return financialStatementMapper.selectList(wrapper);
    }

    @Override
    public List<LocalDate> listDistinctReportDatesByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return List.of();
        }
        List<LocalDate> rows = financialStatementMapper.selectDistinctReportDatesByCode(code.trim());
        return rows == null ? List.of() : rows;
    }

    @Override
    public List<FinancialStatementEntity> listReportDimensionsInWindow(String code, LocalDate startInclusive,
            LocalDate endInclusive) {
        if (code == null || code.trim().isEmpty()) {
            return List.of();
        }
        return financialStatementMapper.selectList(new LambdaQueryWrapper<FinancialStatementEntity>()
                .select(FinancialStatementEntity::getReportType, FinancialStatementEntity::getReportDate)
                .eq(FinancialStatementEntity::getCode, code.trim())
                .ge(startInclusive != null, FinancialStatementEntity::getReportDate, startInclusive)
                .le(endInclusive != null, FinancialStatementEntity::getReportDate, endInclusive)
                .orderByDesc(FinancialStatementEntity::getReportDate));
    }

    @Override
    public void upsert(FinancialStatementEntity entity) {
        upsertOne(entity);
    }

    @Override
    public void upsertBatch(List<FinancialStatementEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        Map<String, FinancialStatementEntity> byNaturalKey = new LinkedHashMap<>();
        for (FinancialStatementEntity entity : entities) {
            if (entity == null) {
                continue;
            }
            String code = entity.getCode();
            String reportType = entity.getReportType();
            LocalDate reportDate = entity.getReportDate();
            if (code == null || code.trim().isEmpty() || reportType == null || reportType.trim().isEmpty()
                    || reportDate == null) {
                continue;
            }
            code = code.trim();
            reportType = reportType.trim();
            entity.setCode(code);
            entity.setReportType(reportType);
            if (entity.getId() == null || entity.getId().isEmpty()) {
                entity.setId(IdWorker.getIdStr());
            }
            String key = code + "|" + reportType + "|" + reportDate;
            byNaturalKey.put(key, entity);
        }
        if (byNaturalKey.isEmpty()) {
            return;
        }
        List<FinancialStatementEntity> rows = new ArrayList<>(byNaturalKey.values());
        for (int i = 0; i < rows.size(); i += UPSERT_BATCH_CHUNK_SIZE) {
            int end = Math.min(i + UPSERT_BATCH_CHUNK_SIZE, rows.size());
            financialStatementMapper.upsertBatch(rows.subList(i, end));
        }
    }

    private void upsertOne(FinancialStatementEntity entity) {
        if (entity == null) {
            return;
        }
        String code = entity.getCode();
        String reportType = entity.getReportType();
        LocalDate reportDate = entity.getReportDate();
        if (code == null || code.trim().isEmpty() || reportType == null || reportType.trim().isEmpty()
                || reportDate == null) {
            return;
        }
        FinancialStatementEntity existing = financialStatementMapper.selectOne(
                new LambdaQueryWrapper<FinancialStatementEntity>().eq(FinancialStatementEntity::getCode, code.trim())
                        .eq(FinancialStatementEntity::getReportType, reportType.trim())
                        .eq(FinancialStatementEntity::getReportDate, reportDate).last("limit 1"));
        if (existing != null && existing.getId() != null) {
            entity.setId(existing.getId());
            financialStatementMapper.updateById(entity);
        } else {
            entity.setId(IdWorker.getIdStr());
            financialStatementMapper.insert(entity);
        }
    }

    private static LambdaQueryWrapper<FinancialStatementEntity> baseWrapper(String code, String reportType,
            LocalDate startDate, LocalDate endDate) {
        String c = code == null ? null : code.trim();
        String t = reportType == null ? null : reportType.trim();

        return new LambdaQueryWrapper<FinancialStatementEntity>()
                .eq(c != null && !c.isEmpty(), FinancialStatementEntity::getCode, c)
                .eq(t != null && !t.isEmpty(), FinancialStatementEntity::getReportType, t)
                .ge(startDate != null, FinancialStatementEntity::getReportDate, startDate)
                .le(endDate != null, FinancialStatementEntity::getReportDate, endDate);
    }
}
