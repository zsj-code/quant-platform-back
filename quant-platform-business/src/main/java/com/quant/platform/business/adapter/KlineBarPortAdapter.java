package com.quant.platform.business.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.business.kline.entity.KlineBarEntity;
import com.quant.platform.business.kline.mapper.KlineBarMapper;
import com.quant.platform.common.dto.KlineBarDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KlineBarPortAdapter implements KlineBarPort {
    private final KlineBarMapper klineBarMapper;

    public KlineBarPortAdapter(KlineBarMapper klineBarMapper) {
        this.klineBarMapper = klineBarMapper;
    }

    @Override
    public List<KlineBarDTO> listLatestBarsAsc(String symbol, String intervalType, int limit) {
        if (symbol == null || symbol.isBlank() || intervalType == null || intervalType.isBlank() || limit <= 0) {
            return List.of();
        }
        LambdaQueryWrapper<KlineBarEntity> qw = new LambdaQueryWrapper<KlineBarEntity>()
                .eq(KlineBarEntity::getSymbol, symbol)
                .eq(KlineBarEntity::getIntervalType, intervalType)
                .orderByDesc(KlineBarEntity::getBarTime)
                .last("limit " + limit);
        List<KlineBarEntity> desc = klineBarMapper.selectList(qw);
        if (desc == null || desc.isEmpty()) {
            return List.of();
        }
        List<KlineBarDTO> asc = new ArrayList<>(desc.size());
        for (int i = desc.size() - 1; i >= 0; i--) {
            KlineBarEntity e = desc.get(i);
            if (e != null) {
                asc.add(toDto(e));
            }
        }
        return asc;
    }

    @Override
    public List<KlineBarDTO> listBarsAscBetween(String symbol, String intervalType, LocalDateTime startInclusive,
                                               LocalDateTime endInclusive) {
        if (symbol == null || symbol.isBlank() || intervalType == null || intervalType.isBlank()) {
            return List.of();
        }
        LambdaQueryWrapper<KlineBarEntity> qw = new LambdaQueryWrapper<KlineBarEntity>()
                .eq(KlineBarEntity::getSymbol, symbol)
                .eq(KlineBarEntity::getIntervalType, intervalType)
                .ge(startInclusive != null, KlineBarEntity::getBarTime, startInclusive)
                .le(endInclusive != null, KlineBarEntity::getBarTime, endInclusive)
                .orderByAsc(KlineBarEntity::getBarTime);
        List<KlineBarEntity> rows = klineBarMapper.selectList(qw);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<KlineBarDTO> out = new ArrayList<>(rows.size());
        for (KlineBarEntity e : rows) {
            if (e != null) {
                out.add(toDto(e));
            }
        }
        return out;
    }

    @Override
    public LocalDateTime findLatestBarTime(String symbol, String intervalType) {
        if (symbol == null || symbol.isBlank() || intervalType == null || intervalType.isBlank()) {
            return null;
        }
        LambdaQueryWrapper<KlineBarEntity> qw = new LambdaQueryWrapper<KlineBarEntity>()
                .eq(KlineBarEntity::getSymbol, symbol)
                .eq(KlineBarEntity::getIntervalType, intervalType)
                .orderByDesc(KlineBarEntity::getBarTime)
                .last("limit 1");
        List<KlineBarEntity> list = klineBarMapper.selectList(qw);
        if (list == null || list.isEmpty() || list.get(0) == null) {
            return null;
        }
        return list.get(0).getBarTime();
    }

    private static KlineBarDTO toDto(KlineBarEntity e) {
        KlineBarDTO dto = new KlineBarDTO();
        dto.setSymbol(e.getSymbol());
        dto.setIntervalType(e.getIntervalType());
        dto.setBarTime(e.getBarTime());
        dto.setOpen(e.getOpen());
        dto.setHigh(e.getHigh());
        dto.setLow(e.getLow());
        dto.setClose(e.getClose());
        dto.setVolume(e.getVolume());
        dto.setAmount(e.getAmount());
        dto.setAmplitude(e.getAmplitude());
        dto.setChangePct(e.getChangePct());
        dto.setChangeAmount(e.getChangeAmount());
        dto.setTurnoverRate(e.getTurnoverRate());
        return dto;
    }
}

