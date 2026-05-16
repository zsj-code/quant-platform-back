package com.quant.platform.ai.core.port;

import com.quant.platform.common.dto.KlineBarDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface KlineBarPort {

    /**
     * 返回按时间升序排列的K线（最老 -> 最新）。
     */
    List<KlineBarDTO> listLatestBarsAsc(String symbol, String intervalType, int limit);

    /**
     * 返回按时间升序排列的K线（最老 -> 最新）。
     */
    List<KlineBarDTO> listBarsAscBetween(String symbol, String intervalType, LocalDateTime startInclusive, LocalDateTime endInclusive);

    LocalDateTime findLatestBarTime(String symbol, String intervalType);
}

