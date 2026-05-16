package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.client.ThsFuyaoMarketChartClient;
import com.quant.platform.ai.core.client.dto.ThsMarketTurnoverMinuteChartDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ThsMarketTurnoverMinuteController {

    @Autowired
    private ThsFuyaoMarketChartClient thsFuyaoMarketChartClient;

    @GetMapping("/fetchMarketTurnoverMinute")
    public Result<ThsMarketTurnoverMinuteChartDTO> fetchMarketTurnoverMinute() {
        return Result.ok(thsFuyaoMarketChartClient.fetchMarketTurnoverMinute());
    }
}
