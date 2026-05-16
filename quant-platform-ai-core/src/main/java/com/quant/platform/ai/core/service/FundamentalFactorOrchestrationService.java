package com.quant.platform.ai.core.service;

import com.quant.platform.ai.core.client.EastmoneyPledgeRatioClient;
import com.quant.platform.ai.core.client.ThsFinanceAnnounceClient;
import com.quant.platform.ai.core.factor.fundamental.*;
import com.quant.platform.ai.core.port.FinancialStatementPort;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.ai.core.port.RegulatoryAnnouncementPort;
import com.quant.platform.ai.core.port.StockValuationSnapshotPort;
import com.quant.platform.common.enums.EastmoneyFinancialStatementReportTypeEnum;
import com.quant.platform.common.dto.FinancialStatementDTO;
import com.quant.platform.common.dto.StockValuationSnapshotDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FundamentalFactorOrchestrationService {
    private final StockValuationSnapshotPort snapshotPort;
    private final FinancialStatementPort financialStatementPort;
    private final ThsFinanceAnnounceClient thsFinanceAnnounceClient;
    private final RegulatoryAnnouncementPort regulatoryAnnouncementPort;
    private final EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient;
    private final KlineBarPort klineBarPort;

    public FundamentalFactorOrchestrationService(StockValuationSnapshotPort snapshotPort,
                                                 FinancialStatementPort financialStatementPort,
                                                 @Autowired(required = false) ThsFinanceAnnounceClient thsFinanceAnnounceClient,
                                                 @Autowired(required = false) RegulatoryAnnouncementPort regulatoryAnnouncementPort,
                                                 @Autowired(required = false) EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient,
                                                 @Autowired(required = false) KlineBarPort klineBarPort) {
        this.snapshotPort = snapshotPort;
        this.financialStatementPort = financialStatementPort;
        this.thsFinanceAnnounceClient = thsFinanceAnnounceClient;
        this.regulatoryAnnouncementPort = regulatoryAnnouncementPort;
        this.eastmoneyPledgeRatioClient = eastmoneyPledgeRatioClient;
        this.klineBarPort = klineBarPort;
    }

    public Map<String, Object> evaluate(String symbol) {

        StockValuationSnapshotDTO snapshot = snapshotPort.findLatestBySymbol(symbol);

        String secCode = snapshot == null ? null : snapshot.getSecCode();
        String code = secCode; // financial_statement.code 存的是6位证券代码

        Map<String, List<FinancialStatementDTO>> byType = new LinkedHashMap<>();
        if (code != null && !code.isBlank()) {
            // 注意：financial_statement.report_type 存的是 enum.name()（INCOME/BALANCE/CASHFLOW），
            // 而因子侧 Map key 使用的是 enum.getReportName()（RPT_DMSK_FN_*）。
            LocalDate begin = LocalDate.parse("2023-03-31");
            LocalDate end = LocalDate.parse("2026-03-31");
            byType.put(EastmoneyFinancialStatementReportTypeEnum.INCOME.getReportName(),
                    loadStatements(code, EastmoneyFinancialStatementReportTypeEnum.INCOME, begin, end));
            byType.put(EastmoneyFinancialStatementReportTypeEnum.BALANCE.getReportName(),
                    loadStatements(code, EastmoneyFinancialStatementReportTypeEnum.BALANCE, begin, end));
            byType.put(EastmoneyFinancialStatementReportTypeEnum.CASHFLOW.getReportName(),
                    loadStatements(code, EastmoneyFinancialStatementReportTypeEnum.CASHFLOW, begin, end));
        }

        FundamentalContext ctx = new FundamentalContext(symbol, code, snapshot, byType);

        Map<FundamentalFactorGroup, List<FundamentalFactor>> grouped = FundamentalFactorCatalog.allGrouped(
                thsFinanceAnnounceClient, regulatoryAnnouncementPort, eastmoneyPledgeRatioClient, klineBarPort);
        Map<FundamentalFactorGroup, List<FundamentalResult>> results = new LinkedHashMap<>();
        for (Map.Entry<FundamentalFactorGroup, List<FundamentalFactor>> e : grouped.entrySet()) {
            List<FundamentalResult> list = new ArrayList<>();
            for (FundamentalFactor f : e.getValue()) {
                list.add(f.evaluate(ctx));
            }
            results.put(e.getKey(), list);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("symbol", symbol);
        out.put("secCode", code);
        out.put("hasSnapshot", snapshot != null);
        out.put("statementsLoaded", Map.of(
                "INCOME", byType.getOrDefault(EastmoneyFinancialStatementReportTypeEnum.INCOME.getReportName(), List.of()).size(),
                "BALANCE", byType.getOrDefault(EastmoneyFinancialStatementReportTypeEnum.BALANCE.getReportName(), List.of()).size(),
                "CASHFLOW", byType.getOrDefault(EastmoneyFinancialStatementReportTypeEnum.CASHFLOW.getReportName(), List.of()).size()
        ));
        out.put("groupedResults", results);
        return out;
    }

    private List<FinancialStatementDTO> loadStatements(String code,
                                                          EastmoneyFinancialStatementReportTypeEnum type,
                                                          LocalDate reportDateBeginInclusive,
                                                          LocalDate reportDateEndInclusive) {
        return financialStatementPort.listStatementsDesc(code, type.name(), reportDateBeginInclusive, reportDateEndInclusive);
    }
}

