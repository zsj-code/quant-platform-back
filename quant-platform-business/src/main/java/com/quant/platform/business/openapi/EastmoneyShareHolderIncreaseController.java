package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.client.EastmoneyShareHolderIncreaseClient;
import com.quant.platform.ai.core.client.dto.EastmoneyShareHolderIncreasePageDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 东财股东增减持（{@code RPT_SHARE_HOLDER_INCREASE}）对外查询接口。
 */
@RestController
public class EastmoneyShareHolderIncreaseController {

    @Autowired
    private EastmoneyShareHolderIncreaseClient eastmoneyShareHolderIncreaseClient;

    /**
     * 按 {@code END_DATE} 倒序分页查询指定证券股东增减持记录。
     *
     * @param symbol     证券代码，6 位或带后缀（如 600176、600176.SH）
     * @param pageNumber 页码，默认 1
     * @param pageSize   每页条数，默认 50
     */
    @GetMapping("/fetchShareHolderIncrease")
    public Result<EastmoneyShareHolderIncreasePageDTO> fetchShareHolderIncrease(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        EastmoneyShareHolderIncreasePageDTO dto = pageSize == null
                ? eastmoneyShareHolderIncreaseClient.fetchShareHolderIncrease(symbol, pageNumber)
                : eastmoneyShareHolderIncreaseClient.fetchShareHolderIncrease(symbol, pageNumber, pageSize);
        return Result.ok(dto);
    }
}
