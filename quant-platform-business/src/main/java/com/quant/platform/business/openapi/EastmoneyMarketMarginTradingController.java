package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.client.EastmoneyMarketMarginTradingClient;
import com.quant.platform.ai.core.client.dto.EastmoneyMarketMarginHistoryPageDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EastmoneyMarketMarginTradingController {

    @Autowired
    private EastmoneyMarketMarginTradingClient eastmoneyMarketMarginTradingClient;

    @GetMapping("/fetchMarketMarginTrading")
    public Result<EastmoneyMarketMarginHistoryPageDTO> fetchMarketMarginTrading(
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        EastmoneyMarketMarginHistoryPageDTO dto = pageSize == null
                ? eastmoneyMarketMarginTradingClient.fetchMarketMarginHistory(pageNumber)
                : eastmoneyMarketMarginTradingClient.fetchMarketMarginHistory(pageNumber, pageSize);
        return Result.ok(dto);
    }
}
