package com.quant.platform.business.trader.service;


import com.quant.platform.business.trader.dto.TraderDecisionRunPdfExport;
import com.quant.platform.business.trader.vo.TraderDecisionWorkflowRunVO;
import com.quant.platform.common.api.PageResult;

import java.util.Optional;

/**
 * 交易决策运行记录查询（落库表 {@code trader_decision_workflow_run}）。
 */
public interface TraderDecisionRunAdminService {

    /**
     * 按主键加载单次运行（含步骤列表）。
     */
    Optional<TraderDecisionWorkflowRunVO> findById(String id);

    /**
     * 将单次运行导出为 PDF：仅使用 {@code llm_summary_text}；文件名为 {@code 股票代码-股票名称-当前时间.pdf}。
     */
    TraderDecisionRunPdfExport buildRunPdfExport(String id);

    /**
     * 按规范化后的证券代码分页，按开始时间倒序。
     *
     * @param code
     *            股票编码（任意常见写法，内部规范为 6 位）
     */
    PageResult<TraderDecisionWorkflowRunVO> pageByCode(String code, Long current, Long size);
}
