package com.quant.platform.business.research.dto;

import lombok.Value;

/**
 * 东方财富研报列表单条（个股/行业等接口字段兼容）。
 */
@Value
public class ResearchReportItemDTO {
    /** 研报标题 */
    String title;
    /** 机构简称 */
    String orgSName;
    /** 发布日期（原文字符串，多为 yyyy-MM-dd） */
    String publishDate;
    /** 行业名称 */
    String industryName;
    /** 东财行业代码（部分接口字段为 industryCode / industry_code） */
    String industryCode;
    /** 证券简称 */
    String stockName;
    /** 证券代码（多为 6 位） */
    String stockCode;
    /** 研报详情页 infoCode */
    String infoCode;
    /** 部分类型使用 encodeUrl 跳转 */
    String encodeUrl;
    /** 投资评级名称 */
    String ratingName;
    /** 栏目/分类编码 */
    String column;
    /** 研报类型描述 */
    String reportType;
}
