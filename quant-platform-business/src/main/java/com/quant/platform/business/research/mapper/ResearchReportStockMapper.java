package com.quant.platform.business.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.research.entity.ResearchReportStockEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ResearchReportStockMapper extends BaseMapper<ResearchReportStockEntity> {

    void upsertBatch(@Param("list") List<ResearchReportStockEntity> list);
}
