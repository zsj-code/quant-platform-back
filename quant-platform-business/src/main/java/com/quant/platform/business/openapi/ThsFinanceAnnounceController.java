package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.client.ThsFinanceAnnounceClient;
import com.quant.platform.ai.core.client.dto.ThsFinanceAnnounceYearDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ThsFinanceAnnounceController {

    private final ThsFinanceAnnounceClient thsFinanceAnnounceClient;

    public ThsFinanceAnnounceController(@Autowired ThsFinanceAnnounceClient thsFinanceAnnounceClient) {
        this.thsFinanceAnnounceClient = thsFinanceAnnounceClient;
    }

    @GetMapping("/fetchFinanceAnnounceDetail")
    public Result<List<ThsFinanceAnnounceYearDTO>>  fetchFinanceAnnounceDetail() {
        return Result.ok(thsFinanceAnnounceClient.fetchFinanceAnnounceDetail("000001", 1, 10));
    }

}
