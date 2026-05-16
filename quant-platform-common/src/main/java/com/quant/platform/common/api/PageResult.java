package com.quant.platform.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private long current;
    private long size;
    private long total;
    private List<T> records;

    public static <T> PageResult<T> of(long current, long size, long total, List<T> records) {
        return new PageResult<>(current, size, total, records);
    }
}
