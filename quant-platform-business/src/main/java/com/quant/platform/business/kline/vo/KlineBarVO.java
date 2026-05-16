package com.quant.platform.business.kline.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KlineBarVO {
    private String id;
    private String symbol;
    private String intervalType;
    private LocalDateTime barTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal amplitude;
    private BigDecimal changePct;
    private BigDecimal changeAmount;
    private BigDecimal turnoverRate;
}
