package com.quant.platform.business.stock.controller;

import com.quant.platform.business.stock.convert.StockValuationSnapshotAdminConvert;
import com.quant.platform.business.stock.dto.StockValuationSnapshotDTO;
import com.quant.platform.business.stock.service.StockValuationSnapshotAdminService;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.api.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/stock-valuation-snapshots")
public class StockValuationSnapshotController {

    private final StockValuationSnapshotAdminService stockValuationSnapshotAdminService;

    public StockValuationSnapshotController(StockValuationSnapshotAdminService stockValuationSnapshotAdminService) {
        this.stockValuationSnapshotAdminService = stockValuationSnapshotAdminService;
    }

    @GetMapping("/detail/{id}")
    public Result<StockValuationSnapshotDTO> getById(
            @PathVariable(name = "id") @NotBlank(message = "id 不能为空") String id) {
        var entity = stockValuationSnapshotAdminService.getById(id);
        return Result.ok(StockValuationSnapshotAdminConvert.INSTANCE.toDTO(entity));
    }

    @GetMapping("/by-symbol")
    public Result<StockValuationSnapshotDTO> getBySymbol(
            @RequestParam(name = "symbol") @NotBlank(message = "symbol 不能为空") String symbol) {
        var entity = stockValuationSnapshotAdminService.getBySymbol(symbol);
        return Result.ok(StockValuationSnapshotAdminConvert.INSTANCE.toDTO(entity));
    }

    @GetMapping("/by-sec-code")
    public Result<StockValuationSnapshotDTO> getBySecCode(
            @RequestParam(name = "secCode") @NotBlank(message = "secCode 不能为空") String secCode) {
        var entity = stockValuationSnapshotAdminService.getBySecCode(secCode);
        return Result.ok(StockValuationSnapshotAdminConvert.INSTANCE.toDTO(entity));
    }

    @GetMapping("/page")
    public Result<PageResult<StockValuationSnapshotDTO>> page(
            @RequestParam(name = "secCode", required = false) String secCode,
            @RequestParam(name = "symbol", required = false) String symbol,
            @RequestParam(name = "current", defaultValue = "1") @Min(value = 1, message = "current 不能小于 1") Long current,
            @RequestParam(name = "size", defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 2000,
                    message = "size 过大") Long size) {
        var p = stockValuationSnapshotAdminService.page(secCode, symbol, current, size);
        return Result.ok(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                StockValuationSnapshotAdminConvert.INSTANCE.toDTOList(p.getRecords())));
    }
}
