package com.quant.platform.business.financial.controller;


import com.quant.platform.business.financial.convert.FinancialStatementAdminConvert;
import com.quant.platform.business.financial.service.FinancialStatementAdminService;
import com.quant.platform.business.financial.vo.FinancialStatementVO;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.api.ResultCode;
import com.quant.platform.common.exception.BizException;
import com.quant.platform.common.util.CommonUtil;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/fundamental/financial-statements")
public class FinancialStatementController {
    private final FinancialStatementAdminService financialStatementAdminService;

    public FinancialStatementController(FinancialStatementAdminService financialStatementAdminService) {
        this.financialStatementAdminService = financialStatementAdminService;
    }

    @GetMapping("/{id}")
    public Result<FinancialStatementVO> get(@PathVariable(name = "id") @NotBlank(message = "id 不能为空") String id) {
        var entity = financialStatementAdminService.getById(id);
        return Result.ok(FinancialStatementAdminConvert.INSTANCE.toVO(entity));
    }

    @GetMapping
    public Result<List<FinancialStatementVO>> listByCode(
            @RequestParam(name = "code") @NotBlank(message = "code 不能为空") String code) {
        var list = financialStatementAdminService.list(code, null, null, null);
        return Result.ok(FinancialStatementAdminConvert.INSTANCE.toVOList(list));
    }

    /**
     * 按股票编码查询已有财报的去重报告期列表（倒序）。支持 6 位代码或带后缀如 600000.SH。
     */
    @GetMapping("/codes/{code}/report-dates")
    public Result<List<LocalDate>> listReportDatesByCode(
            @PathVariable(name = "code") @NotBlank(message = "code 不能为空") String code) {
        String c = CommonUtil.normalizeSixDigitCode(code);
        if (c == null || c.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "股票编码无效");
        }
        return Result.ok(financialStatementAdminService.listDistinctReportDatesByCode(c));
    }
}
