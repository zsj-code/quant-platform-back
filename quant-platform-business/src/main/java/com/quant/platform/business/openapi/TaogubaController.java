package com.quant.platform.business.openapi;

import com.quant.platform.business.client.TaogubaClient;
import com.quant.platform.business.client.dto.TaogubaBarItemDTO;
import com.quant.platform.business.client.dto.TaogubaTopicReplyDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/taoguba")
public class TaogubaController {

    @Autowired
    private TaogubaClient taogubaClient;

    @GetMapping("/listStockBarCool")
    public Result<List<TaogubaBarItemDTO>> listStockBarCool() {
        return Result.ok(taogubaClient.listStockBarCool("sz000858"));
    }

    @GetMapping("/listTopicRepliesPage")
    public Result<List<TaogubaTopicReplyDTO>> listTopicRepliesPage() {
        return Result.ok(taogubaClient.listTopicRepliesPage("2rqVGwRTQZt", 1));
    }


}
