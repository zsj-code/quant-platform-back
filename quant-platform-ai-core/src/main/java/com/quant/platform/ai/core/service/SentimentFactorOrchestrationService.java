package com.quant.platform.ai.core.service;

import com.quant.platform.ai.core.client.EastmoneyMarketMarginTradingClient;
import com.quant.platform.ai.core.client.EastmoneyStockMarginTradingClient;
import com.quant.platform.ai.core.client.ThsFuyaoMarketChartClient;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.ai.core.factor.sentiment.SentimentContext;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactor;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactorCatalog;
import com.quant.platform.ai.core.factor.sentiment.SentimentFactorGroup;
import com.quant.platform.ai.core.factor.technical.FactorResult;
import com.quant.platform.ai.core.port.StockValuationSnapshotPort;
import com.quant.platform.common.dto.StockValuationSnapshotDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 情绪面因子编排：结构与 {@link FundamentalFactorOrchestrationService} /
 * {@link TechnicalFactorOrchestrationService} 对齐。
 * <p>
 * 情绪面因子由 {@link SentimentFactorCatalog} 注册；部分因子依赖上下文注入的客户端（东财个股/全市场融资融券、同花顺扶摇、日 K 等）。
 */
@Service
public class SentimentFactorOrchestrationService {
    private final StockValuationSnapshotPort snapshotPort;
    private final EastmoneyStockMarginTradingClient eastmoneyStockMarginTradingClient;
    private final KlineBarPort klineBarPort;
    private final EastmoneyMarketMarginTradingClient eastmoneyMarketMarginTradingClient;
    private final ThsFuyaoMarketChartClient thsFuyaoMarketChartClient;

    public SentimentFactorOrchestrationService(
            @Autowired StockValuationSnapshotPort stockValuationSnapshotPort,
            @Autowired(required = false) EastmoneyStockMarginTradingClient eastmoneyStockMarginTradingClient,
            @Autowired(required = false) KlineBarPort klineBarPort,
            @Autowired(required = false) EastmoneyMarketMarginTradingClient eastmoneyMarketMarginTradingClient,
            @Autowired(required = false) ThsFuyaoMarketChartClient thsFuyaoMarketChartClient) {
        this.snapshotPort = stockValuationSnapshotPort;
        this.eastmoneyStockMarginTradingClient = eastmoneyStockMarginTradingClient;
        this.klineBarPort = klineBarPort;
        this.eastmoneyMarketMarginTradingClient = eastmoneyMarketMarginTradingClient;
        this.thsFuyaoMarketChartClient = thsFuyaoMarketChartClient;
    }

    public Map<String, Object> evaluate(String symbol) {
        StockValuationSnapshotDTO snapshot = snapshotPort.findLatestBySymbol(symbol);
        String secCode = snapshot == null ? null : snapshot.getSecCode();
        SentimentContext ctx = new SentimentContext(symbol, secCode, snapshot, eastmoneyStockMarginTradingClient,
                klineBarPort, eastmoneyMarketMarginTradingClient, thsFuyaoMarketChartClient);

        Map<SentimentFactorGroup, List<SentimentFactor>> grouped = SentimentFactorCatalog.allGrouped();
        Map<SentimentFactorGroup, List<FactorResult>> results = new LinkedHashMap<>();
        for (Map.Entry<SentimentFactorGroup, List<SentimentFactor>> e : grouped.entrySet()) {
            List<FactorResult> list = new ArrayList<>();
            for (SentimentFactor f : e.getValue()) {
                list.add(f.evaluate(ctx));
            }
            results.put(e.getKey(), list);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("symbol", symbol);
        out.put("secCode", secCode);
        out.put("hasSnapshot", snapshot != null);
        out.put("groupedResults", results);
        return out;
    }
}
