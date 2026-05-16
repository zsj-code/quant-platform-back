package com.quant.platform.ai.core.factor.sentiment;

import com.quant.platform.ai.core.client.EastmoneyMarketMarginTradingClient;
import com.quant.platform.ai.core.client.EastmoneyStockMarginTradingClient;
import com.quant.platform.ai.core.client.ThsFuyaoMarketChartClient;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.common.dto.StockValuationSnapshotDTO;

/**
 * 单标的情绪面计算上下文（预留与基本面一致的 symbol / secCode / 快照解析）。
 */
public final class SentimentContext {
    private final String symbol;
    private final String secCode;
    private final StockValuationSnapshotDTO snapshot;
    private final EastmoneyStockMarginTradingClient eastmoneyStockMarginTradingClient;
    private final KlineBarPort klineBarPort;
    private final EastmoneyMarketMarginTradingClient eastmoneyMarketMarginTradingClient;
    private final ThsFuyaoMarketChartClient thsFuyaoMarketChartClient;

    public SentimentContext(String symbol, String secCode, StockValuationSnapshotDTO snapshot,
                            EastmoneyStockMarginTradingClient eastmoneyStockMarginTradingClient,
                            KlineBarPort klineBarPort,
                            EastmoneyMarketMarginTradingClient eastmoneyMarketMarginTradingClient,
                            ThsFuyaoMarketChartClient thsFuyaoMarketChartClient) {
        this.symbol = symbol;
        this.secCode = secCode;
        this.snapshot = snapshot;
        this.eastmoneyStockMarginTradingClient = eastmoneyStockMarginTradingClient;
        this.klineBarPort = klineBarPort;
        this.eastmoneyMarketMarginTradingClient = eastmoneyMarketMarginTradingClient;
        this.thsFuyaoMarketChartClient = thsFuyaoMarketChartClient;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSecCode() {
        return secCode;
    }

    public StockValuationSnapshotDTO getSnapshot() {
        return snapshot;
    }

    /**
     * 东财个股融资融券日明细客户端；未注入时依赖该客户端的因子返回 {@code UNAVAILABLE}。
     */
    public EastmoneyStockMarginTradingClient getEastmoneyStockMarginTradingClient() {
        return eastmoneyStockMarginTradingClient;
    }

    /**
     * 日 K 等 K 线查询端口；未注入时依赖日 K 的因子可回退或返回 {@code UNAVAILABLE}。
     */
    public KlineBarPort getKlineBarPort() {
        return klineBarPort;
    }

    /**
     * 东财全市场融资融券历史（{@code RPTA_RZRQ_LSHJ}）；未注入时 {@link com.quant.platform.ai.core.factor.sentiment.marketwide.S4MarginFinancingEmotionFactor} 等返回 {@code UNAVAILABLE}。
     */
    public EastmoneyMarketMarginTradingClient getEastmoneyMarketMarginTradingClient() {
        return eastmoneyMarketMarginTradingClient;
    }

    /**
     * 同花顺 dq「扶摇」市场图表（如全市场成交额分时）；未注入时依赖该端的因子返回 {@code UNAVAILABLE}。
     */
    public ThsFuyaoMarketChartClient getThsFuyaoMarketChartClient() {
        return thsFuyaoMarketChartClient;
    }
}
