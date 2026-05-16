package com.quant.platform.business.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.research.entity.ResearchReportIndustryEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ResearchReportIndustryMapper extends BaseMapper<ResearchReportIndustryEntity> {

    void upsertBatch(@Param("list") List<ResearchReportIndustryEntity> list);
}
