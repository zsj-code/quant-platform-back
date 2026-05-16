package com.quant.platform.business.stock.controller;


import com.quant.platform.business.stock.convert.StockIndustryValuationAdminConvert;
import com.quant.platform.business.stock.dto.StockIndustryValuationDTO;
import com.quant.platform.business.stock.service.StockIndustryValuationAdminService;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.api.ResultCode;
import com.quant.platform.common.exception.BizException;
import com.quant.platform.common.util.CommonUtil;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/stock-industry-valuations")
public class StockIndustryValuationController {

    private final StockIndustryValuationAdminService stockIndustryValuationAdminService;

    public StockIndustryValuationController(StockIndustryValuationAdminService stockIndustryValuationAdminService) {
        this.stockIndustryValuationAdminService = stockIndustryValuationAdminService;
    }

    @GetMapping("/detail/{id}")
    public Result<StockIndustryValuationDTO> getById(
            @PathVariable(name = "id") @NotBlank(message = "id 不能为空") String id) {
        var entity = stockIndustryValuationAdminService.getById(id);
        return Result.ok(StockIndustryValuationAdminConvert.INSTANCE.toDTO(entity));
    }

    /**
     * 按股票编码（6 位或带交易所后缀）查询行业板块估值；匹配表字段 {@code sec_code}。
     */
    @GetMapping("/by-code")
    public Result<StockIndustryValuationDTO> getByCode(
            @RequestParam(name = "code") @NotBlank(message = "code 不能为空") String code) {
        String c = CommonUtil.normalizeSixDigitCode(code);
        if (c == null || c.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "股票编码无效");
        }
        var entity = stockIndustryValuationAdminService.getBySecCode(c);
        return Result.ok(StockIndustryValuationAdminConvert.INSTANCE.toDTO(entity));
    }

}
