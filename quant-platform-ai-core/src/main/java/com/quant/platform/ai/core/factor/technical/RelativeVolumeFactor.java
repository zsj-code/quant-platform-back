package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;

import java.util.List;

/**
 * 相对成交量：当日成交量 / 过去20日均量。
 *
 * 结合60日高低位（用“接近60日最高/最低”的近似判定）：
 * - > 3 且 60日低位：低位巨量，可能吸筹
 * - > 3 且 60日高位：高位巨量，可能出货
 * - > 1.5 且 突破关键颈线：突破确认（颈线定义需要上层提供，这里只给出“量能满足”）
 * - < 0.4 且 高位：极度缩量，上涨动能衰竭
 */
public class RelativeVolumeFactor implements TechnicalFactor {
    @Override
    public String factorKey() {
        return "volume.relative_volume_1d_vs20d";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.VOLUME_FLOW;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 70) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约70根日线用于 20日均量 与 60日高低位判断");
        }

        BarSeries series = Ta4jSeriesUtils.toDailySeries(bars);
        int end = series.getEndIndex();
        VolumeIndicator vol = new VolumeIndicator(series);
        SMAIndicator volMa20 = new SMAIndicator(vol, 20);

        double vToday = vol.getValue(end).doubleValue();
        double vAvg20 = volMa20.getValue(end).doubleValue();
        if (vAvg20 == 0d) {
            return FactorResult.unavailable(factorKey(), "20日均量为0，无法计算相对成交量");
        }
        double rv = vToday / vAvg20;

        // 60日高低位：以收盘价在60日窗口内是否“非常接近”最高/最低来近似
        double close = series.getBar(end).getClosePrice().doubleValue();
        double min60 = Double.POSITIVE_INFINITY;
        double max60 = Double.NEGATIVE_INFINITY;
        for (int i = end - 59; i <= end; i++) {
            double c = series.getBar(i).getClosePrice().doubleValue();
            min60 = Math.min(min60, c);
            max60 = Math.max(max60, c);
        }
        boolean nearLow60 = max60 == 0d ? false : (close - min60) / max60 <= 0.05;  // 近似：距离最低不超过约5%
        boolean nearHigh60 = max60 == 0d ? false : (max60 - close) / max60 <= 0.05; // 近似：距离最高不超过约5%

        FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        String summary = "相对成交量正常";

        if (rv > 3 && nearLow60) {
            level = FactorSignalLevel.BULLISH;
            summary = "低位巨量：强烈关注（可能吸筹）";
        } else if (rv > 3 && nearHigh60) {
            level = FactorSignalLevel.WARNING;
            summary = "高位巨量：禁止追入，持股者考虑减仓（可能出货）";
        } else if (rv < 0.4 && nearHigh60) {
            level = FactorSignalLevel.WARNING;
            summary = "高位极度缩量：动能衰竭，关注变盘";
        } else if (rv > 1.5) {
            level = FactorSignalLevel.INFO;
            summary = "量能偏强：若同时发生关键价位突破，可作为突破确认之一";
        }

        return FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("volume_today", vToday)
                .metric("volume_ma20", vAvg20)
                .metric("relative_volume", rv)
                .metric("close", close)
                .metric("close_min60", min60)
                .metric("close_max60", max60)
                .metric("near_low60", nearLow60)
                .metric("near_high60", nearHigh60)
                .notes(List.of("60日高低位为近似判断：当前收盘距60日最高/最低约5%以内视为高/低位"))
                .build();
    }
}

