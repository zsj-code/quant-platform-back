package com.quant.platform.business.agent;

import com.quant.platform.business.trader.service.TraderDecisionRunAdminService;
import com.quant.platform.business.trader.vo.TraderDecisionWorkflowRunVO;
import com.quant.platform.common.api.Result;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent/runs")
@ConditionalOnProperty(prefix = "quant.ai.langchain4j.openai", name = "api-key")
public class AgentRunController {
    private final TraderDecisionRunAdminService traderDecisionRunAdminService;

    public AgentRunController(TraderDecisionRunAdminService traderDecisionRunAdminService) {
        this.traderDecisionRunAdminService = traderDecisionRunAdminService;
    }

    @GetMapping("/{id}")
    public Result<TraderDecisionWorkflowRunVO> findById(@PathVariable("id") String id) {
        return traderDecisionRunAdminService.findById(id)
                .map(Result::ok)
                .orElseGet(() -> Result.fail(404, "运行记录不存在"));
    }
}

