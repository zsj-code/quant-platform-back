package com.quant.platform.business.kline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.kline.entity.KlineBarEntity;
import com.quant.platform.business.kline.mapper.KlineBarMapper;
import com.quant.platform.business.kline.service.KlineBarAdminService;
import com.quant.platform.common.api.PageResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KlineBarAdminServiceImpl implements KlineBarAdminService {
    private final KlineBarMapper klineBarMapper;

    public KlineBarAdminServiceImpl(KlineBarMapper klineBarMapper) {
        this.klineBarMapper = klineBarMapper;
    }

    @Override
    public KlineBarEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return klineBarMapper.selectById(id);
    }

    @Override
    public PageResult<KlineBarEntity> page(String symbol, String intervalType, LocalDateTime startTime,
            LocalDateTime endTime, Long current, Long size) {
        if (symbol == null || symbol.trim().isEmpty() || intervalType == null || intervalType.trim().isEmpty()) {
            return PageResult.of(1, 0, 0, List.of());
        }

        long c = current == null ? 1L : current;
        long s = size == null ? 500L : size;
        if (c < 1) {
            c = 1L;
        }
        if (s < 1) {
            s = 500L;
        }
        if (s > 2000) {
            s = 2000L;
        }

        var wrapper = baseWrapper(symbol, intervalType, startTime, endTime).orderByAsc(KlineBarEntity::getBarTime);
        var page = klineBarMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public List<KlineBarEntity> list(String symbol, String intervalType, LocalDateTime startTime,
            LocalDateTime endTime) {
        if (symbol == null || symbol.trim().isEmpty() || intervalType == null || intervalType.trim().isEmpty()) {
            return List.of();
        }

        var wrapper = baseWrapper(symbol, intervalType, startTime, endTime).orderByAsc(KlineBarEntity::getBarTime);
        return klineBarMapper.selectList(wrapper);
    }

    private static LambdaQueryWrapper<KlineBarEntity> baseWrapper(String symbol, String intervalType,
            LocalDateTime startTime, LocalDateTime endTime) {
        return new LambdaQueryWrapper<KlineBarEntity>().eq(KlineBarEntity::getSymbol, symbol.trim())
                .eq(KlineBarEntity::getIntervalType, intervalType.trim())
                .ge(startTime != null, KlineBarEntity::getBarTime, startTime)
                .le(endTime != null, KlineBarEntity::getBarTime, endTime);
    }
}
