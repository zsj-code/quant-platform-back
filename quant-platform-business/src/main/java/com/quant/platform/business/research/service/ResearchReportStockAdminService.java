package com.quant.platform.business.research.service;


import com.quant.platform.business.research.entity.ResearchReportStockEntity;

import java.time.LocalDate;
import java.util.List;

public interface ResearchReportStockAdminService {

    void upsertBatch(List<ResearchReportStockEntity> entities);

    /**
     * 某证券近 {@code since}（含）以来落库的个股研报（按发布日倒序）。
     */
    List<ResearchReportStockEntity> listBySecCodeSince(String secCode, LocalDate since);
}
