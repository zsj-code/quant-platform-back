package com.quant.platform.business.research.dto;

import lombok.Value;

import java.util.List;

/**
 * 东方财富研报分页结果。
 */
@Value
public class ResearchReportPageDTO {
    long pageNo;
    long pageSize;
    /** 总条数（接口字段名可能为 total / totalCount，解析时兼容） */
    long total;
    List<ResearchReportItemDTO> list;
}
