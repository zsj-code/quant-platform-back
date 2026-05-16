package com.quant.platform.business.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * {@code data/api/Data/GetIndexData} 返回：股吧情绪/指数时间序列。
 * <p>
 * 参考响应：
 * {@code {"re":[{"time":...,"value":...},...],"retime":...,"count":...,"rc":1,"me":"操作成功","rUrl":""}}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GubaIndexSeriesDTO {
    /** 指数序列点（注意：字段名就是 re） */
    private List<GubaIndexPointDTO> re;
}

