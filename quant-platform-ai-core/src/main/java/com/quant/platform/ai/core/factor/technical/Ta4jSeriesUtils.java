package com.quant.platform.ai.core.factor.technical;

import com.quant.platform.common.dto.KlineBarDTO;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

final class Ta4jSeriesUtils {
    private Ta4jSeriesUtils() {
    }

    static BarSeries toDailySeries(List<KlineBarDTO> bars) {
        // 使用 DoubleNum，避免 BaseBar(double...) 与 Series(DecimalNum) 的类型不一致异常
        BarSeries series = new BaseBarSeriesBuilder().withName("kline").withNumTypeOf(DoubleNum.class).build();
        ZoneId zone = ZoneId.systemDefault();
        for (KlineBarDTO b : bars) {
            if (b == null || b.getBarTime() == null) {
                continue;
            }
            ZonedDateTime endTime = b.getBarTime().atZone(zone);
            // KlineBarEntity 没有 duration，这里按“日线”处理；分钟线需要单独映射
            Duration barDuration = Duration.ofDays(1);

            series.addBar(new BaseBar(
                    barDuration,
                    endTime,
                    b.getOpen() == null ? 0d : b.getOpen().doubleValue(),
                    b.getHigh() == null ? 0d : b.getHigh().doubleValue(),
                    b.getLow() == null ? 0d : b.getLow().doubleValue(),
                    b.getClose() == null ? 0d : b.getClose().doubleValue(),
                    b.getVolume() == null ? 0d : b.getVolume().doubleValue()
            ));
        }
        return series;
    }
}

