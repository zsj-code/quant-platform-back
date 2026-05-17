package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 换手率警戒：基于日 K {@code turnover_rate}（换手率%，东财 f61）。
 * <ul>
 *     <li>&lt; 1%：僵尸股，技术信号可信度低</li>
 *     <li>1% ~ 5%：正常</li>
 *     <li>5% ~ 10%：活跃</li>
 *     <li>&gt; 10% 且近 60 日高位：死亡换手</li>
 *     <li>&gt; 10% 且近 60 日低位：启动换手</li>
 * </ul>
 */
public class TurnoverRateAlertFactor implements TechnicalFactor {

    static final BigDecimal TH_ZOMBIE_BELOW = new BigDecimal("1");
    static final BigDecimal TH_NORMAL_HIGH = new BigDecimal("5");
    static final BigDecimal TH_ACTIVE_HIGH = new BigDecimal("10");
    /** 收盘距 60 日最高/最低在该比例以内视为高/低位（与 {@link RelativeVolumeFactor} 一致） */
    static final double POSITION_BAND = 0.05;

    @Override
    public String factorKey() {
        return "volume.turnover_rate_alert";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.VOLUME_FLOW;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.size() < 60) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要60根日线用于换手率与60日高低位判断");
        }

        KlineBarDTO latest = bars.get(bars.size() - 1);
        BigDecimal turnover = latest.getTurnoverRate();
        if (turnover == null) {
            return FactorResult.unavailable(factorKey(), "最新日K缺少换手率(turnover_rate)");
        }

        BigDecimal close = latest.getClose();
        if (close == null) {
            return FactorResult.unavailable(factorKey(), "最新日K缺少收盘价");
        }

        Position60d position = positionIn60dWindow(bars, close);
        return buildResult(turnover, latest, position);
    }

    static FactorResult buildResult(BigDecimal turnover, KlineBarDTO latest, Position60d position) {
        FactorSignalLevel level = FactorSignalLevel.NEUTRAL;
        String summary = "换手率正常(1%~5%)";
        String bucket = "normal";

        if (turnover.compareTo(TH_ZOMBIE_BELOW) < 0) {
            level = FactorSignalLevel.WARNING;
            summary = "换手率<1%：流动性不足(僵尸股)，技术信号可信度低";
            bucket = "zombie";
        } else if (turnover.compareTo(TH_NORMAL_HIGH) <= 0) {
            level = FactorSignalLevel.NEUTRAL;
            summary = "换手率1%~5%：正常区间";
            bucket = "normal";
        } else if (turnover.compareTo(TH_ACTIVE_HIGH) <= 0) {
            level = FactorSignalLevel.INFO;
            summary = "换手率5%~10%：交投活跃，短线资金关注";
            bucket = "active";
        } else if (position.nearHigh60()) {
            level = FactorSignalLevel.WARNING;
            summary = "换手率>10%且股价近60日高位：死亡换手，筹码分散风险高";
            bucket = "death_turnover_high";
        } else if (position.nearLow60()) {
            level = FactorSignalLevel.BULLISH;
            summary = "换手率>10%且股价近60日低位：启动换手，可积极关注";
            bucket = "launch_turnover_low";
        } else {
            level = FactorSignalLevel.WARNING;
            summary = "换手率>10%：异常放量换手，需结合价位结构判断";
            bucket = "high_turnover_mid";
        }

        return FactorResult.builder("volume.turnover_rate_alert")
                .level(level)
                .summary(summary)
                .metric("bar_time", latest.getBarTime())
                .metric("turnover_rate_pct", turnover)
                .metric("close", latest.getClose())
                .metric("change_pct", latest.getChangePct())
                .metric("close_min60", position.min60())
                .metric("close_max60", position.max60())
                .metric("near_low60", position.nearLow60())
                .metric("near_high60", position.nearHigh60())
                .metric("bucket", bucket)
                .notes(List.of(
                        "换手率来源：日K turnover_rate(%)",
                        "60日高低位为近似：收盘距60日最高/最低约5%以内"))
                .build();
    }

    static Position60d positionIn60dWindow(List<KlineBarDTO> bars, BigDecimal close) {
        int end = bars.size() - 1;
        int start = Math.max(0, end - 59);
        BigDecimal min60 = null;
        BigDecimal max60 = null;
        for (int i = start; i <= end; i++) {
            BigDecimal c = bars.get(i).getClose();
            if (c == null) {
                continue;
            }
            min60 = min60 == null ? c : min60.min(c);
            max60 = max60 == null ? c : max60.max(c);
        }
        if (min60 == null || max60 == null || max60.compareTo(BigDecimal.ZERO) == 0) {
            return new Position60d(min60, max60, false, false);
        }
        BigDecimal span = max60.subtract(min60);
        if (span.compareTo(BigDecimal.ZERO) == 0) {
            return new Position60d(min60, max60, true, true);
        }
        BigDecimal distLow = close.subtract(min60);
        BigDecimal distHigh = max60.subtract(close);
        BigDecimal band = span.multiply(BigDecimal.valueOf(POSITION_BAND));
        boolean nearLow = distLow.compareTo(band) <= 0;
        boolean nearHigh = distHigh.compareTo(band) <= 0;
        return new Position60d(
                min60.setScale(4, RoundingMode.HALF_UP),
                max60.setScale(4, RoundingMode.HALF_UP),
                nearLow,
                nearHigh);
    }

    record Position60d(BigDecimal min60, BigDecimal max60, boolean nearLow60, boolean nearHigh60) {
    }
}
