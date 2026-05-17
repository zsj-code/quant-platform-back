package com.quant.platform.business.openapi;

import com.quant.platform.ai.core.client.EastmoneyF10GIncomeClient;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalancePageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GCashflowPageDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomePageDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 东财 F10 财务报表对外查询：利润表、现金流量表、资产负债表。
 */
@RestController
public class EastmoneyF10GIncomeController {

    @Autowired
    private EastmoneyF10GIncomeClient eastmoneyF10GIncomeClient;

    /**
     * 按报告期倒序分页查询 F10 利润表。
     *
     * @param symbol       证券代码，如 {@code 001979.SZ} 或 {@code 001979}
     * @param reportDates  报告期，逗号分隔，如 {@code 2026-03-31,2025-12-31}；为空则不过滤报告期
     * @param pageNumber   页码，默认 1
     * @param pageSize     每页条数，默认 5
     */
    @GetMapping("/fetchF10GIncome")
    public Result<EastmoneyF10GIncomePageDTO> fetchF10GIncome(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "reportDates", required = false) String reportDates,
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<LocalDate> dates = EastmoneyF10GIncomeClient.parseReportDatesParam(reportDates);
        EastmoneyF10GIncomePageDTO dto = pageSize == null
                ? eastmoneyF10GIncomeClient.fetchF10GIncome(symbol, dates, pageNumber,
                        EastmoneyF10GIncomeClient.DEFAULT_PAGE_SIZE)
                : eastmoneyF10GIncomeClient.fetchF10GIncome(symbol, dates, pageNumber, pageSize);
        return Result.ok(dto);
    }

    /**
     * 按报告期倒序分页查询 F10 现金流量表。
     *
     * @param symbol       证券代码，如 {@code 001979.SZ} 或 {@code 001979}
     * @param reportDates  报告期，逗号分隔；为空则不过滤报告期
     * @param pageNumber   页码，默认 1
     * @param pageSize     每页条数，默认 5
     */
    @GetMapping("/fetchF10GCashflow")
    public Result<EastmoneyF10GCashflowPageDTO> fetchF10GCashflow(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "reportDates", required = false) String reportDates,
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<LocalDate> dates = EastmoneyF10GIncomeClient.parseReportDatesParam(reportDates);
        EastmoneyF10GCashflowPageDTO dto = pageSize == null
                ? eastmoneyF10GIncomeClient.fetchF10GCashflow(symbol, dates, pageNumber,
                        EastmoneyF10GIncomeClient.DEFAULT_PAGE_SIZE)
                : eastmoneyF10GIncomeClient.fetchF10GCashflow(symbol, dates, pageNumber, pageSize);
        return Result.ok(dto);
    }

    /**
     * 按报告期倒序分页查询 F10 资产负债表。
     *
     * @param symbol       证券代码，如 {@code 001979.SZ} 或 {@code 001979}
     * @param reportDates  报告期，逗号分隔；为空则不过滤报告期
     * @param pageNumber   页码，默认 1
     * @param pageSize     每页条数，默认 5
     */
    @GetMapping("/fetchF10GBalance")
    public Result<EastmoneyF10GBalancePageDTO> fetchF10GBalance(
            @RequestParam("symbol") String symbol,
            @RequestParam(value = "reportDates", required = false) String reportDates,
            @RequestParam(value = "pageNumber", defaultValue = "1") int pageNumber,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        List<LocalDate> dates = EastmoneyF10GIncomeClient.parseReportDatesParam(reportDates);
        EastmoneyF10GBalancePageDTO dto = pageSize == null
                ? eastmoneyF10GIncomeClient.fetchF10GBalance(symbol, dates, pageNumber,
                        EastmoneyF10GIncomeClient.DEFAULT_PAGE_SIZE)
                : eastmoneyF10GIncomeClient.fetchF10GBalance(symbol, dates, pageNumber, pageSize);
        return Result.ok(dto);
    }
}
