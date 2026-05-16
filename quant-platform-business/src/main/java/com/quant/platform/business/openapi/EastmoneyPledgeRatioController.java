package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.client.EastmoneyPledgeRatioClient;
import com.quant.platform.ai.core.client.dto.EastmoneyPledgeRatioLatestDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class EastmoneyPledgeRatioController {

    @Autowired
    private EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient;


    @GetMapping("/fetchLatestPledgeRatio")
    public Result<EastmoneyPledgeRatioLatestDTO> fetchLatestPledgeRatio() {
        return Result.ok(eastmoneyPledgeRatioClient.fetchLatestPledgeRatio("002456").get());
    }


}
