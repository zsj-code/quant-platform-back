package com.quant.platform.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery {
    private long current;

    private long size;

    public static PageQuery of(long current, long size) {
        return new PageQuery(current, size);
    }
}
