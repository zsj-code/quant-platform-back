package com.quant.platform.business.trader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.quant.platform.business.trader.entity.TraderDecisionWorkflowStepEntity;
import org.apache.ibatis.annotations.Param;

public interface TraderDecisionWorkflowStepMapper extends BaseMapper<TraderDecisionWorkflowStepEntity> {

    /**
     * 依赖唯一键 (workflow_run_id, step_key)。
     */
    void upsertStep(@Param("e") TraderDecisionWorkflowStepEntity e);

    int patchLlmResponseByRunIdAndStepKey(@Param("workflowRunId") String workflowRunId, @Param("stepKey") String stepKey,
            @Param("llmResponseText") String llmResponseText);
}
