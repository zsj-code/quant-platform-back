package com.quant.platform.business.stock.vo;

import com.quant.platform.business.stock.enums.StockDelistStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockVO {
    private String id;
    private String code;
    private String name;
    private StockDelistStatus isDelisted;
    private LocalDate latestReportDate;
}
