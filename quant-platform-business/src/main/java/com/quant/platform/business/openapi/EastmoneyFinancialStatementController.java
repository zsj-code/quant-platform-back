package com.quant.platform.business.openapi;

import com.quant.platform.business.client.EastmoneyFinancialStatementClient;
import com.quant.platform.business.financial.dto.EastmoneyFinancialStatementPageDTO;
import com.quant.platform.common.api.Result;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class EastmoneyFinancialStatementController {

    @Autowired
    private EastmoneyFinancialStatementClient eastmoneyFinancialStatementClient;

    @PostMapping("/fetchPage")
    public Result<EastmoneyFinancialStatementPageDTO> fetchPage() throws IOException {
        return Result.ok(eastmoneyFinancialStatementClient.fetchPage("000001", EastmoneyFinancialStatementReportTypeEnum.BALANCE, 1));
    }

}
