package com.quant.platform.business.openapi;

import com.quant.platform.business.client.EastmoneyKlineClient;
import com.quant.platform.business.kline.dto.EastmoneyKlineBarDTO;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import com.quant.platform.common.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EastmoneyKlineController {


    @Autowired
    private EastmoneyKlineClient eastmoneyKlineClient;

    @GetMapping("/fetchKline")
    public Result<List<EastmoneyKlineBarDTO>> fetchKline() {
        return Result.ok(eastmoneyKlineClient.fetchKline(CommonUtil.toSecId("000001"),
                KlineIntervalTypeEnum.D.getEastmoneyKlt(), 1, "20260101", "20260411"));
    }

}
