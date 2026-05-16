package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.client.EastmoneyStockMarginTradingClient;
import com.quant.platform.ai.core.client.dto.EastmoneyMarginTradingStockPageDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EastmoneyStockMarginTradingController {

    @Autowired
    private EastmoneyStockMarginTradingClient eastmoneyStockMarginTradingClient;

    @GetMapping("/fetchStockMarginTrading")
    public Result<EastmoneyMarginTradingStockPageDTO> fetchStockMarginTrading(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        EastmoneyMarginTradingStockPageDTO dto = pageSize == null
                ? eastmoneyStockMarginTradingClient.fetchStockMarginTrading(symbol, pageNumber)
                : eastmoneyStockMarginTradingClient.fetchStockMarginTrading(symbol, pageNumber, pageSize);
        return Result.ok(dto);
    }
}
