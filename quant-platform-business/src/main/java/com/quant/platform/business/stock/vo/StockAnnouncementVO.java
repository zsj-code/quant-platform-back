package com.quant.platform.business.stock.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StockAnnouncementVO {
    private String id;
    private String code;
    private String symbol;
    private String source;
    private String externalId;
    private String title;
    private LocalDateTime announceTime;
    private LocalDate noticeDate;
    private String categories;
    private LocalDateTime fetchedAt;
}
