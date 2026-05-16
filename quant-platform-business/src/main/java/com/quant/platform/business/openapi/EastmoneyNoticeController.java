package com.quant.platform.business.openapi;

import com.quant.platform.business.client.EastmoneyNoticeClient;
import com.quant.platform.business.stock.dto.StockAnnouncementPageDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EastmoneyNoticeController {

    @Autowired
    private EastmoneyNoticeClient eastmoneyNoticeClient;

    @GetMapping("/fetchAnnouncements")
    public Result<StockAnnouncementPageDTO> fetchAnnouncements(String stockCode, int pageIndex, int pageSize) {
        return Result.ok(eastmoneyNoticeClient.fetchAnnouncements(stockCode, pageIndex, pageSize));
    }

}
