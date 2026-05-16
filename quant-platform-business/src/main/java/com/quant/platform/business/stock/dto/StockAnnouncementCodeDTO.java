package com.quant.platform.business.stock.dto;

import lombok.Value;

@Value
public class StockAnnouncementCodeDTO {
    String annType;
    String innerCode;
    String marketCode;
    String shortName;
    String stockCode;
}
