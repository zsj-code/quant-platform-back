package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.service.TechnicalFactorOrchestrationService;
import com.quant.platform.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/technical")
public class TechnicalController {
    private final TechnicalFactorOrchestrationService orchestrationService;

    public TechnicalController(TechnicalFactorOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @GetMapping("/evaluate")
    public Result<Map<String, Object>> evaluate(
            @RequestParam("symbol") String symbol,
            @RequestParam(name = "minuteLimit", defaultValue = "2000") int minuteLimit,
            @RequestParam(name = "intervalType", required = false) String intervalType
    ) {
        return Result.ok(orchestrationService.evaluate(symbol, intervalType, minuteLimit));
    }
}

