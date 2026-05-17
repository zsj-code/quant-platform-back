package com.quant.platform.ai.core.client.dto;

import java.util.List;

/**
 * 东财 {@code RPT_SHARE_HOLDER_INCREASE} 股东增减持分页（按 {@code END_DATE} 倒序）。
 */
public record EastmoneyShareHolderIncreasePageDTO(
        String stockCode,
        int pageNumber,
        int pageSize,
        long total,
        Integer totalPages,
        List<EastmoneyShareHolderIncreaseRowDTO> rows) {
}
