package com.quant.platform.business.research.service;


import com.quant.platform.business.research.entity.ResearchReportIndustryEntity;

import java.time.LocalDate;
import java.util.List;

public interface ResearchReportIndustryAdminService {

    void upsertBatch(List<ResearchReportIndustryEntity> entities);

    /**
     * 某行业代码下近 {@code since}（含）以来落库的行业研报（按发布日倒序）。
     */
    List<ResearchReportIndustryEntity> listByIndustryCodeSince(String industryCode, LocalDate since);
}
