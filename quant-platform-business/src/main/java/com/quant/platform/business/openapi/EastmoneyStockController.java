package com.quant.platform.business.openapi;

import com.quant.platform.business.client.EastmoneyStockClient;
import com.quant.platform.business.stock.dto.StockBasicDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EastmoneyStockController {

    @Autowired
    private EastmoneyStockClient eastmoneyStockClient;

    @GetMapping("/fetchAStocks")
    public Result<List<StockBasicDTO>> fetchAStocks() {
        return Result.ok(eastmoneyStockClient.fetchAStocks(100, 10));
    }

}
