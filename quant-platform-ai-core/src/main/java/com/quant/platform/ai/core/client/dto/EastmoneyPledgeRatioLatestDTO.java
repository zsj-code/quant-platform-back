package com.quant.platform.ai.core.client.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 东财 {@code RPT_CSDC_LIST} 中单条质押比例快照（通常取 TRADE_DATE 最新一条）。
 */
public record EastmoneyPledgeRatioLatestDTO(
        String securityCode,
        String securityNameAbbr,
        LocalDate tradeDate,
        /** 质押比例（%，与接口 {@code PLEDGE_RATIO} 一致） */
        BigDecimal pledgeRatio) {
}
