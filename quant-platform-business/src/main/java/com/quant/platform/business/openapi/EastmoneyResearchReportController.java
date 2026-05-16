package com.quant.platform.business.openapi;

import com.quant.platform.business.client.EastmoneyResearchReportClient;
import com.quant.platform.business.research.dto.ResearchReportPageDTO;
import com.quant.platform.common.api.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
public class EastmoneyResearchReportController {

    @Autowired
    private EastmoneyResearchReportClient eastmoneyResearchReportClient;

    @GetMapping("/fetchIndustryResearchReports")
    public Result<ResearchReportPageDTO> fetchIndustryResearchReports() {
        return Result.ok(eastmoneyResearchReportClient.fetchIndustryResearchReports("*", 1, 1000, LocalDate.of(2024, 1, 1), LocalDate.now()));
    }

    @GetMapping("/fetchStockResearchReports")
    public Result<ResearchReportPageDTO> fetchStockResearchReports(String stockCode) {
        return Result.ok(eastmoneyResearchReportClient.fetchStockResearchReports(stockCode, 1, 1000, LocalDate.of(2025, 1, 1), LocalDate.now()));
    }

}
