package com.quant.platform.business.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.platform.ai.core.port.StockValuationSnapshotPort;
import com.quant.platform.business.stock.entity.StockValuationSnapshotEntity;
import com.quant.platform.business.stock.mapper.StockValuationSnapshotMapper;
import com.quant.platform.common.dto.StockValuationSnapshotDTO;
import org.springframework.stereotype.Service;

@Service
public class StockValuationSnapshotPortAdapter implements StockValuationSnapshotPort {
    private final StockValuationSnapshotMapper snapshotMapper;

    public StockValuationSnapshotPortAdapter(StockValuationSnapshotMapper snapshotMapper) {
        this.snapshotMapper = snapshotMapper;
    }

    @Override
    public StockValuationSnapshotDTO findLatestBySymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        StockValuationSnapshotEntity e = snapshotMapper.selectOne(new LambdaQueryWrapper<StockValuationSnapshotEntity>()
                .eq(StockValuationSnapshotEntity::getSymbol, symbol)
                .orderByDesc(StockValuationSnapshotEntity::getFetchedAt)
                .last("limit 1"));
        return e == null ? null : toDto(e);
    }

    private static StockValuationSnapshotDTO toDto(StockValuationSnapshotEntity e) {
        StockValuationSnapshotDTO dto = new StockValuationSnapshotDTO();
        dto.setSymbol(e.getSymbol());
        dto.setSecCode(e.getSecCode());
        dto.setTotalMarketCapYuan(e.getTotalMarketCapYuan());
        dto.setFetchedAt(e.getFetchedAt());
        return dto;
    }
}

