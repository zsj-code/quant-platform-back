package com.quant.platform.business.stock.dto;

import lombok.Value;

import java.util.List;

@Value
public class StockAnnouncementPageDTO {
    long pageIndex;
    long pageSize;
    long totalHits;
    List<StockAnnouncementItemDTO> list;
}
