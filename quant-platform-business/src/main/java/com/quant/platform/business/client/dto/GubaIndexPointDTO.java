package com.quant.platform.business.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 股吧指数点：time 为毫秒时间戳，value 为指数值（通常 0~1 之间的小数）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GubaIndexPointDTO {
    private long time;
    private double value;
}

