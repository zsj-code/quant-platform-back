package com.quant.platform.business.stock.dto;

import lombok.Value;

import java.util.List;

@Value
public class StockAnnouncementItemDTO {
    String artCode;
    List<StockAnnouncementCodeDTO> codes;
    String title;
    String titleCh;
    String titleEn;
    String noticeDate;
    String displayTime;
    String eiTime;
    String language;
    String listingState;
    String sourceType;
    String productCode;
    String sortDate;
    List<StockAnnouncementColumnDTO> columns;
}
