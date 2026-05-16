package com.quant.platform.business.kline.service;

import com.quant.platform.business.kline.entity.KlineBarEntity;
import com.quant.platform.common.api.PageResult;

import java.time.LocalDateTime;
import java.util.List;

public interface KlineBarAdminService {
    KlineBarEntity getById(String id);

    PageResult<KlineBarEntity> page(String symbol, String intervalType, LocalDateTime startTime, LocalDateTime endTime,
                                    Long current, Long size);

    List<KlineBarEntity> list(String symbol, String intervalType, LocalDateTime startTime, LocalDateTime endTime);
}
