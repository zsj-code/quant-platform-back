package com.quant.platform.business.stock.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.platform.business.financial.entity.FinancialStatementEntity;
import com.quant.platform.business.financial.mapper.FinancialStatementMapper;
import com.quant.platform.business.stock.convert.StockAdminConvert;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.business.stock.vo.StockVO;
import com.quant.platform.common.api.PageResult;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.api.ResultCode;
import com.quant.platform.common.exception.BizException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockAdminService stockAdminService;
    private final FinancialStatementMapper financialStatementMapper;

    public StockController(StockAdminService stockAdminService, FinancialStatementMapper financialStatementMapper) {
        this.stockAdminService = stockAdminService;
        this.financialStatementMapper = financialStatementMapper;
    }

    @GetMapping("/{id}")
    public Result<StockVO> get(@PathVariable(name = "id") @NotBlank(message = "id 不能为空") String id) {
        var entity = stockAdminService.getById(id);
        if (entity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "股票不存在");
        }
        StockVO vo = StockAdminConvert.INSTANCE.toVO(entity);

        var latest = financialStatementMapper.selectOne(new LambdaQueryWrapper<FinancialStatementEntity>()
                .eq(FinancialStatementEntity::getCode, entity.getCode())
                .orderByDesc(FinancialStatementEntity::getReportDate).last("limit 1"));
        vo.setLatestReportDate(latest == null ? null : latest.getReportDate());

        return Result.ok(vo);
    }

    @GetMapping("/page")
    public Result<PageResult<StockVO>> page(@RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "current", defaultValue = "1") @Min(value = 1, message = "current 不能小于 1") Long current,
            @RequestParam(name = "size", defaultValue = "20") @Min(value = 1, message = "size 不能小于 1") @Max(value = 2000, message = "size 过大（最大 2000）") Long size) {
        var p = stockAdminService.page(keyword, current, size);
        return Result.ok(PageResult.of(p.getCurrent(), p.getSize(), p.getTotal(),
                StockAdminConvert.INSTANCE.toVOList(p.getRecords())));
    }
}
