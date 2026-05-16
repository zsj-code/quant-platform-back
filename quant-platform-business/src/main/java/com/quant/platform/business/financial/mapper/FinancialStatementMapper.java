package com.quant.platform.business.financial.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.financial.entity.FinancialStatementEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface FinancialStatementMapper extends BaseMapper<FinancialStatementEntity> {

    @Select("SELECT DISTINCT report_date FROM financial_statement WHERE code = #{code} ORDER BY report_date DESC")
    List<LocalDate> selectDistinctReportDatesByCode(@Param("code") String code);

    /**
     * 批量 upsert：依赖唯一键 (code, report_type, report_date)。
     */
    void upsertBatch(@Param("list") List<FinancialStatementEntity> list);
}
