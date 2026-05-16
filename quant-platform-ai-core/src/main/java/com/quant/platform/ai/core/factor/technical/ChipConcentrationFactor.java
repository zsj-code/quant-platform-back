package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 筹码集中度（基于日线的 OHLCV 近似）：
 * <p>
 * 在滑动窗口内，将每日成交量在当日 [Low, High] 区间上均匀摊薄到价格直方图，合成近似筹码分布；
 * 再计算价格维度上的归一化 HHI（Herfindahl）以及 5%–95% 累积量对应的成本带宽度（相对收盘价）。
 * </p>
 * <p>
 * 该实现<strong>不是</strong>交易所或行情商提供的真实分价/逐笔筹码，仅作技术因子可用的统计近似。
 * </p>
 */
public class ChipConcentrationFactor implements TechnicalFactor {

    /**
     * 参与合成筹码分布的日线根数（含当日）。
     */
    private static final int WINDOW = 90;
    /**
     * 最少需要的日线根数（略小于 WINDOW 时仍可用较短窗口计算）。
     */
    private static final int MIN_BARS = 60;
    /**
     * 价格直方图桶数。
     */
    private static final int BINS = 96;

    @Override
    public String factorKey() {
        return "chip.concentration_lorenz_inverse";
    }

    @Override
    public TechnicalFactorGroup group() {
        return TechnicalFactorGroup.CHIP_DISTRIBUTION;
    }

    @Override
    public FactorResult evaluate(List<KlineBarDTO> bars) {
        if (bars == null || bars.isEmpty()) {
            return FactorResult.unavailable(factorKey(), "K线为空");
        }
        if (bars.size() < MIN_BARS) {
            return FactorResult.unavailable(factorKey(), "K线数量不足，至少需要约 " + MIN_BARS + " 根日线用于筹码集中度近似");
        }

        int n = bars.size();
        int start = Math.max(0, n - WINDOW);
        List<KlineBarDTO> window = bars.subList(start, n);

        double priceMin = Double.POSITIVE_INFINITY;
        double priceMax = Double.NEGATIVE_INFINITY;
        for (KlineBarDTO b : window) {
            if (b == null || b.getLow() == null || b.getHigh() == null) {
                continue;
            }
            double lo = b.getLow().doubleValue();
            double hi = b.getHigh().doubleValue();
            if (Double.isNaN(lo) || Double.isNaN(hi)) {
                continue;
            }
            priceMin = Math.min(priceMin, Math.min(lo, hi));
            priceMax = Math.max(priceMax, Math.max(lo, hi));
        }
        if (priceMin == Double.POSITIVE_INFINITY || priceMax <= priceMin) {
            return FactorResult.unavailable(factorKey(), "窗口内价格区间无效，无法分桶");
        }

        double[] chip = new double[BINS];
        for (KlineBarDTO b : window) {
            if (b == null || b.getLow() == null || b.getHigh() == null) {
                continue;
            }
            double lo = b.getLow().doubleValue();
            double hi = b.getHigh().doubleValue();
            long vol = b.getVolume() == null ? 0L : b.getVolume();
            if (vol <= 0L || Double.isNaN(lo) || Double.isNaN(hi)) {
                continue;
            }
            if (hi < lo) {
                double t = lo;
                lo = hi;
                hi = t;
            }
            addUniformVolume(chip, priceMin, priceMax, lo, hi, vol);
        }

        double total = 0d;
        for (double v : chip) {
            total += v;
        }
        if (total <= 0d) {
            return FactorResult.unavailable(factorKey(), "窗口内有效成交量为0，无法估计筹码分布");
        }

        double hhiNorm = normalizedHhi(chip, total);
        double p5 = priceAtVolumeQuantile(chip, priceMin, priceMax, total, 0.05d);
        double p95 = priceAtVolumeQuantile(chip, priceMin, priceMax, total, 0.95d);
        if (Double.isNaN(p5) || Double.isNaN(p95) || p95 < p5) {
            return FactorResult.unavailable(factorKey(), "分位成本计算失败");
        }

        KlineBarDTO last = bars.get(n - 1);
        BigDecimal closeBd = last != null ? last.getClose() : null;
        double close = closeBd == null ? Double.NaN : closeBd.doubleValue();
        double bandPctOfClose = (close > 0d && !Double.isNaN(close))
                ? (p95 - p5) / close * 100.0d
                : Double.NaN;

        // 以桶中心价为“财富”、筹码量为权重，计算价格加权基尼；成本越聚拢则基尼越小，洛伦兹补数越大
        double range = priceMax - priceMin;
        double giniPrice = giniPriceWeighted(chip, total, priceMin, range, BINS);
        double lorenzInverse = Double.isNaN(giniPrice) ? Double.NaN : (1.0d - giniPrice);

        FactorSignalLevel level;
        String summary;
        if (hhiNorm >= 0.48d) {
            level = FactorSignalLevel.INFO;
            summary = "筹码在少数价位显著堆积（近似高度集中），关注后续方向选择";
        } else if (hhiNorm <= 0.14d) {
            level = FactorSignalLevel.NEUTRAL;
            summary = "筹码沿成本轴相对分散，成本分歧较大";
        } else if (!Double.isNaN(bandPctOfClose) && bandPctOfClose <= 12d) {
            level = FactorSignalLevel.INFO;
            summary = "90% 量级成本带相对较窄（相对现价），筹码成本较为聚拢";
        } else if (!Double.isNaN(bandPctOfClose) && bandPctOfClose >= 45d) {
            level = FactorSignalLevel.WARNING;
            summary = "成本带相对现价很宽，筹码成本分散，波动与分歧风险偏高";
        } else {
            level = FactorSignalLevel.NEUTRAL;
            summary = "筹码集中度处于中等区间";
        }

        List<String> notes = new ArrayList<>(2);
        notes.add("基于日线：成交量在当日高低价区间均匀摊薄，窗口 " + window.size() + " 日，分桶 " + BINS + "。");
        notes.add("指标为统计近似，不等同于真实分价筹码分布。");

        FactorResult.Builder b = FactorResult.builder(factorKey())
                .level(level)
                .summary(summary)
                .metric("window_days", window.size())
                .metric("bins", BINS)
                .metric("price_min_window", priceMin)
                .metric("price_max_window", priceMax)
                .metric("chip_hhi_normalized", hhiNorm)
                .metric("cost_band_p5", p5)
                .metric("cost_band_p95", p95)
                .metric("chip_gini_price_weighted", Double.isNaN(giniPrice) ? null : giniPrice)
                .metric("lorenz_inverse_gini_complement", Double.isNaN(lorenzInverse) ? null : lorenzInverse)
                .notes(notes);

        if (!Double.isNaN(bandPctOfClose)) {
            b.metric("cost_band_width_pct_of_close", bandPctOfClose);
        }
        if (last != null && last.getBarTime() != null) {
            b.metric("as_of_bar_time", last.getBarTime().toString());
        }
        return b.build();
    }

    /**
     * 将成交量均匀加到与 [barLow, barHigh] 相交的桶上。
     */
    private static void addUniformVolume(
            double[] chip,
            double axisMin,
            double axisMax,
            double barLow,
            double barHigh,
            long volume
    ) {
        double range = axisMax - axisMin;
        int i0 = priceToBin(axisMin, range, barLow, chip.length);
        int i1 = priceToBin(axisMin, range, barHigh, chip.length);
        if (i0 > i1) {
            int t = i0;
            i0 = i1;
            i1 = t;
        }
        int span = i1 - i0 + 1;
        double per = volume / (double) span;
        for (int i = i0; i <= i1; i++) {
            chip[i] += per;
        }
    }

    private static int priceToBin(double axisMin, double axisRange, double price, int bins) {
        if (axisRange <= 0d || bins <= 1) {
            return 0;
        }
        int idx = (int) Math.floor((price - axisMin) / axisRange * bins);
        if (idx < 0) {
            return 0;
        }
        if (idx >= bins) {
            return bins - 1;
        }
        return idx;
    }

    /**
     * 归一化 HHI：((HHI - 1/n) / (1 - 1/n))，n 为桶数；越接近 1 表示筹码越集中在少数价位。
     */
    private static double normalizedHhi(double[] w, double total) {
        int n = w.length;
        if (n <= 1 || total <= 0d) {
            return 0d;
        }
        double sumSq = 0d;
        for (double v : w) {
            double p = v / total;
            sumSq += p * p;
        }
        double hhiMin = 1.0d / n;
        double denom = 1.0d - hhiMin;
        if (denom <= 0d) {
            return 0d;
        }
        double raw = (sumSq - hhiMin) / denom;
        if (raw < 0d) {
            return 0d;
        }
        if (raw > 1d) {
            return 1d;
        }
        return raw;
    }

    /**
     * 按价格从低到高累积量，在线性插值下得到累积成交量比例 q 对应的价格（q∈[0,1]）。
     */
    private static double priceAtVolumeQuantile(
            double[] chip,
            double axisMin,
            double axisMax,
            double total,
            double q
    ) {
        int n = chip.length;
        if (n <= 0 || total <= 0d || q < 0d || q > 1d) {
            return Double.NaN;
        }
        double range = axisMax - axisMin;
        double target = q * total;
        double cum = 0d;
        for (int i = 0; i < n; i++) {
            double prevCum = cum;
            cum += chip[i];
            if (target <= cum + 1e-12d) {
                double binLo = axisMin + range * i / n;
                double binHi = axisMin + range * (i + 1) / n;
                if (chip[i] <= 1e-15d) {
                    return binLo;
                }
                double frac = (target - prevCum) / chip[i];
                if (frac < 0d) {
                    frac = 0d;
                } else if (frac > 1d) {
                    frac = 1d;
                }
                return binLo + frac * (binHi - binLo);
            }
        }
        return axisMax;
    }

    /**
     * 以桶中心价为取值、筹码占比为权重，计算离散分布的基尼系数（与洛伦兹曲线一致）。
     */
    private static double giniPriceWeighted(
            double[] w,
            double total,
            double axisMin,
            double axisRange,
            int n
    ) {
        if (n <= 0 || total <= 0d || axisRange <= 0d) {
            return Double.NaN;
        }
        double mu = 0d;
        for (int i = 0; i < n; i++) {
            double xi = axisMin + axisRange * (i + 0.5d) / n;
            mu += w[i] * xi;
        }
        mu /= total;
        if (mu <= 1e-15d) {
            return Double.NaN;
        }
        double sumPair = 0d;
        for (int i = 0; i < n; i++) {
            if (w[i] <= 0d) {
                continue;
            }
            double xi = axisMin + axisRange * (i + 0.5d) / n;
            double pi = w[i] / total;
            for (int j = 0; j < n; j++) {
                if (w[j] <= 0d) {
                    continue;
                }
                double xj = axisMin + axisRange * (j + 0.5d) / n;
                double pj = w[j] / total;
                sumPair += pi * pj * Math.abs(xi - xj);
            }
        }
        double g = sumPair / (2.0d * mu);
        if (g < 0d) {
            return 0d;
        }
        if (g > 1d) {
            return 1d;
        }
        return g;
    }
}
