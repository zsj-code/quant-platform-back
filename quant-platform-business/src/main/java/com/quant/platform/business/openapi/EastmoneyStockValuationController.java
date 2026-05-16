package com.quant.platform.business.openapi;

import com.quant.platform.business.client.EastmoneyStockValuationClient;
import com.quant.platform.business.stock.dto.EastmoneyStockValuationDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EastmoneyStockValuationController {

    @Autowired
    private EastmoneyStockValuationClient eastmoneyStockValuationClient;


    @GetMapping("/fetchValuationSnapshot")
    public Result<EastmoneyStockValuationDTO> fetchValuationSnapshot(String stockCode) {
        return Result.ok(eastmoneyStockValuationClient.fetchValuationSnapshot(stockCode));
    }

}
