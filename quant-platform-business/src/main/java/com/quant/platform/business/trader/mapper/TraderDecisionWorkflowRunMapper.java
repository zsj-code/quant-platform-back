package com.quant.platform.business.trader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowRunEntity;
import org.apache.ibatis.annotations.Param;

public interface TraderDecisionWorkflowRunMapper extends BaseMapper<TraderDecisionWorkflowRunEntity> {

    int updateStatusAndFinished(@Param("workflowRunKey") String workflowRunKey, @Param("status") String status,
            @Param("errorMessage") String errorMessage);

    int updateDecisionJson(@Param("workflowRunKey") String workflowRunKey, @Param("decisionJson") String decisionJson);

    int updateLlmSummaryAndFinish(@Param("workflowRunKey") String workflowRunKey,
            @Param("llmSummaryText") String llmSummaryText);

    int markSucceeded(@Param("workflowRunKey") String workflowRunKey);
}
