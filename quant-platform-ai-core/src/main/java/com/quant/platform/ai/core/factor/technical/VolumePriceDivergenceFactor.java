package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;

import java.util.List;

/**
 * 量价背离检测（连续5日）：
 * - 价涨量缩：连续5日收盘价创新高，但成交量逐日递减，且每天成交量低于其5日均量 → 顶部信号
 * - 价跌量增：连续5日收盘价创新低，但成交量逐日递增 → 接近底部（准备买入）
 */
public class VolumePriceDivergenceFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "volume.price_volume_divergence_5d";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.VOLUME_FLOW;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 30) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约30根日线用于 5日背离与均量参考");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        if (end < 10) {
            return FactorResult.unavailable(factorKey(), "序列长度不足");
        }

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator volMa5 = new SMAIndicator(volume, 5);

        boolean closeNewHigh5 = true;
        boolean closeNewLow5 = true;
        boolean volDecreasing5 = true;
        boolean volIncreasing5 = true;
        boolean volBelowMa5EachDay = true;

        // 检查最后5根（end-4..end），相对其之前的历史是否创新高/新低用滚动比较（简化：逐日比前一日更高/更低）
        for (int i = end - 4; i <= end; i++) {
            double c = close.getValue(i).doubleValue();
            double cPrev = close.getValue(i - 1).doubleValue();
            closeNewHigh5 &= c > cPrev;
            closeNewLow5 &= c < cPrev;

            double v = volume.getValue(i).doubleValue();
            double vPrev = volume.getValue(i - 1).doubleValue();
            volDecreasing5 &= v < vPrev;
            volIncreasing5 &= v > vPrev;

            double vMa = volMa5.getValue(i).doubleValue();
            volBelowMa5EachDay &= v < vMa;
        }

        FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        String summary = "未检测到典型的量价背离(5日)";

        boolean top = closeNewHigh5 && volDecreasing5 && volBelowMa5EachDay;
        boolean bottom = closeNewLow5 && volIncreasing5;

        if (top) {
            level = FactorSignalLevel.WARNING;
            summary = "价涨量缩(5日)：上涨虚脱，顶部信号";
        } else if (bottom) {
            level = FactorSignalLevel.INFO;
            summary = "价跌量增(5日)：恐慌盘涌出，接近底部（等待进一步确认）";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("close_up_5d_streak", closeNewHigh5)
                .metric("close_down_5d_streak", closeNewLow5)
                .metric("volume_decreasing_5d_streak", volDecreasing5)
                .metric("volume_increasing_5d_streak", volIncreasing5)
                .metric("volume_below_ma5_each_day", volBelowMa5EachDay)
                .notes(List.of("“创新高/新低”按连续5日收盘单调上涨/下跌近似；如需严格“突破过去N日极值”，可在后续增强"))
                .build();
    }
}

