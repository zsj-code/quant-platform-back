package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.service.FundamentalFactorOrchestrationService;
import com.quant.platform.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/fundamental")
public class FundamentalController {
    private final FundamentalFactorOrchestrationService orchestrationService;

    public FundamentalController(FundamentalFactorOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @GetMapping("/evaluate")
    public Result<Map<String, Object>> evaluate(
            @RequestParam("symbol") String symbol
    ) {
        return Result.ok(orchestrationService.evaluate(symbol));
    }
}

