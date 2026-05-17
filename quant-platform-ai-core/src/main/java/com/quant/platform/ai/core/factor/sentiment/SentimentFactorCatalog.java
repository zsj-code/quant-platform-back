package com.quant.platform.ai.core.factor.sentiment;

import com.quant.platform.ai.core.factor.sentiment.derivatives.D1StockOptionIvRankFactor;
import com.quant.platform.ai.core.factor.sentiment.derivatives.D2ShortBalanceGrowthFactor;
import com.quant.platform.ai.core.factor.sentiment.derivatives.D3BrokerReportTitleSentimentFactor;
import com.quant.platform.ai.core.factor.sentiment.marketwide.S1MarketPanicGreedFactor;
import com.quant.platform.ai.core.factor.sentiment.marketwide.S2OptionPcr50EtfFactor;
import com.quant.platform.ai.core.factor.sentiment.marketwide.S4MarginFinancingEmotionFactor;
import com.quant.platform.ai.core.factor.sentiment.stock.G1StockSocialHeatFactor;
import com.quant.platform.ai.core.factor.sentiment.stock.G2StockSocialSentimentFactor;
import com.quant.platform.ai.core.factor.sentiment.stock.G3PostDeleteRatioFactor;
import com.quant.platform.ai.core.factor.sentiment.style.F2TickFlowDivergenceFactor;
import com.quant.platform.ai.core.factor.sentiment.style.F3SectorRotationRetentionFactor;
import com.quant.platform.ai.core.factor.sentiment.style.F4LimitUpPromotionFactor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 情绪面因子注册表：对应 {@code md/情绪面.md} 四组共 13 项（S1、S2、S4；F2～F4；G1～G3；D1～D3）。
 * <p>
 * 北向相关项（原 S3、F1、G4）已移除：东财口径无法稳定区分净买入/净卖出。
 * 各因子在输入数据未接入时返回 {@link com.quant.platform.ai.core.factor.technical.FactorSignalLevel#UNAVAILABLE}，
 * 阈值与分档逻辑见 {@link SentimentMdThresholds} 及各因子类中的 {@code classify} 静态方法。
 */
public final class SentimentFactorCatalog {
    private SentimentFactorCatalog() {
    }

    public static Map<SentimentFactorGroup, List<SentimentFactor>> allGrouped() {
        Map<SentimentFactorGroup, List<SentimentFactor>> m = new LinkedHashMap<>();
        m.put(SentimentFactorGroup.MARKET_WIDE, List.of(
                new S1MarketPanicGreedFactor(),
                new S2OptionPcr50EtfFactor(),
                new S4MarginFinancingEmotionFactor()
        ));
        m.put(SentimentFactorGroup.STYLE_AND_FLOW, List.of(
                new F2TickFlowDivergenceFactor(),
                new F3SectorRotationRetentionFactor(),
                new F4LimitUpPromotionFactor()
        ));
        m.put(SentimentFactorGroup.STOCK_SPECIFIC, List.of(
                new G1StockSocialHeatFactor(),
                new G2StockSocialSentimentFactor(),
                new G3PostDeleteRatioFactor()
        ));
        m.put(SentimentFactorGroup.DERIVATIVES_AND_SHADOW, List.of(
                new D1StockOptionIvRankFactor(),
                new D2ShortBalanceGrowthFactor(),
                new D3BrokerReportTitleSentimentFactor()
        ));
        return m;
    }
}
