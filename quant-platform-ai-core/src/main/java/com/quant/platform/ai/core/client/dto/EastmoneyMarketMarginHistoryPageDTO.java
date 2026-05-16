package com.quant.platform.ai.core.client.dto;

import java.util.List;

/**
 * 东财 {@code RPTA_RZRQ_LSHJ} 全市场融资融券历史汇总分页（按 {@code dim_date} 倒序）。
 *
 * @see <a href="https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>
 */
public record EastmoneyMarketMarginHistoryPageDTO(
        int pageNumber,
        int pageSize,
        long total,
        Integer totalPages,
        List<EastmoneyMarketMarginHistoryRowDTO> rows) {
}
