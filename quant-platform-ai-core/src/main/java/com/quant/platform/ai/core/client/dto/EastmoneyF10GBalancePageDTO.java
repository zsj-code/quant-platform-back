package com.quant.platform.ai.core.client.dto;

import java.util.List;

/**
 * 东财 F10 资产负债表（{@code RPT_F10_FINANCE_GBALANCE}）分页结果。
 */
public record EastmoneyF10GBalancePageDTO(
        String secucode,
        int pageNumber,
        int pageSize,
        long total,
        Integer totalPages,
        List<String> reportDatesFilter,
        List<EastmoneyF10GBalanceRowDTO> rows) {
}
