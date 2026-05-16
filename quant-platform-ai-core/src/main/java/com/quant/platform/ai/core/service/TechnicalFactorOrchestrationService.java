package com.quant.platform.ai.core.service;

import com.quant.platform.ai.core.factor.technical.*;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import com.quant.platform.common.dto.KlineBarDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TechnicalFactorOrchestrationService {
    private final KlineBarPort klineBarPort;

    public TechnicalFactorOrchestrationService(KlineBarPort klineBarPort) {
        this.klineBarPort = klineBarPort;
    }

    public Map<String, Object> evaluate(String symbol, int minuteLimit) {
        // minuteLimit：按调用方传入执行，不做 safe 上限裁剪（避免影响分钟因子验证）
        int m1Limit = minuteLimit;

        // 日线：固定取最近3年内的数据，不使用 dayLimit
        List<KlineBarDTO> d1Bars = loadD1BarsLatest3Years(symbol);
        List<KlineBarDTO> m1Bars = m1Limit > 0 ? klineBarPort.listLatestBarsAsc(symbol, KlineIntervalTypeEnum.M1.getCode(), m1Limit) : List.of();

        // 若没有日线，但有分钟线，则尝试从分钟线聚合出日线，保证大部分因子可跑
        List<KlineBarDTO> effectiveD1 = d1Bars.isEmpty() && !m1Bars.isEmpty()
                ? KlineAggregationUtils.aggregateM1ToD1(m1Bars)
                : d1Bars;

        Map<TechnicalFactorGroup, List<TechnicalFactor>> grouped = TechnicalFactorCatalog.allGrouped();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("symbol", symbol);
        out.put("dayBarCount", effectiveD1.size());
        out.put("minuteBarCount", m1Bars.size());
        out.put("dayBarsSource", d1Bars.isEmpty() && !m1Bars.isEmpty() ? "AGG_FROM_M1" : KlineIntervalTypeEnum.D.getCode());
        out.put("dayLookback", "3Y");

        Map<TechnicalFactorGroup, List<FactorResult>> results = new LinkedHashMap<>();
        for (Map.Entry<TechnicalFactorGroup, List<TechnicalFactor>> e : grouped.entrySet()) {
            List<FactorResult> list = new ArrayList<>();
            for (TechnicalFactor f : e.getValue()) {
                String req = f.requiredIntervalType();
                List<KlineBarDTO> input = KlineIntervalTypeEnum.M1.getCode().equalsIgnoreCase(req) ? m1Bars : effectiveD1;
                list.add(f.evaluate(input));
            }
            results.put(e.getKey(), list);
        }
        out.put("groupedResults", results);
        return out;
    }

    /**
     * 兼容旧接口：传 intervalType+limit 时，仍然可用。
     * - intervalType=D -> dayLimit=limit
     * - intervalType=M1 -> minuteLimit=limit
     */
    public Map<String, Object> evaluate(String symbol, String intervalType, int limit) {
        if (KlineIntervalTypeEnum.M1.getCode().equalsIgnoreCase(intervalType)) {
            return evaluate(symbol, limit);
        }
        return evaluate(symbol, 2000);
    }

    private List<KlineBarDTO> loadD1BarsLatest3Years(String symbol) {
        // 找到最新一根D1的bar_time，再向前推3年作为起点
        LocalDateTime latest = klineBarPort.findLatestBarTime(symbol, KlineIntervalTypeEnum.D.getCode());
        if (latest == null) {
            return List.of();
        }
        LocalDateTime start = latest.minusYears(3);
        return klineBarPort.listBarsAscBetween(symbol, KlineIntervalTypeEnum.D.getCode(), start, latest);
    }
}

