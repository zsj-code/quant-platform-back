package com.quant.platform.ai.core.client.dto;

import java.util.List;

/**
 * 东财 {@code RPTA_WEB_RZRQ_GGMX} 个股融资融券明细分页（按 {@code date} 倒序）。
 *
 * @see <a href="https://datacenter-web.eastmoney.com/api/data/v1/get">api/data/v1/get</a>
 */
public record EastmoneyMarginTradingStockPageDTO(
        String stockCode,
        int pageNumber,
        int pageSize,
        long total,
        Integer totalPages,
        List<EastmoneyMarginTradingStockRowDTO> rows) {
}
