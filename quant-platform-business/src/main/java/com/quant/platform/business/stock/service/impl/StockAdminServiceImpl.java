package com.quant.platform.business.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.quant.platform.business.stock.entity.StockEntity;
import com.quant.platform.business.stock.enums.StockDelistStatus;
import com.quant.platform.business.stock.mapper.StockMapper;
import com.quant.platform.business.stock.service.StockAdminService;
import com.quant.platform.common.api.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StockAdminServiceImpl implements StockAdminService {

    private static final int INSERT_BATCH_SIZE = 500;

    private final StockMapper stockMapper;

    public StockAdminServiceImpl(StockMapper stockMapper) {
        this.stockMapper = stockMapper;
    }

    @Override
    public StockEntity getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        return stockMapper.selectById(id);
    }

    @Override
    public PageResult<StockEntity> page(String keyword, Long current, Long size) {
        long c = current == null ? 1L : current;
        long s = size == null ? 20L : size;
        if (c < 1) {
            c = 1L;
        }
        if (s < 1) {
            s = 20L;
        }
        if (s > 2000) {
            s = 2000L;
        }

        var wrapper = new LambdaQueryWrapper<StockEntity>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            wrapper.and(w -> w.like(StockEntity::getCode, k).or().like(StockEntity::getName, k));
        }
        wrapper.orderByAsc(StockEntity::getCode);

        var page = stockMapper.selectPage(Page.of(c, s), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public List<StockEntity> list() {
        return stockMapper.selectList(null);
    }

    @Override
    public List<StockEntity> listNonDelisted() {
        var w = new LambdaQueryWrapper<StockEntity>();
        w.and(q -> q.isNull(StockEntity::getIsDelisted).or().eq(StockEntity::getIsDelisted, StockDelistStatus.LISTED));
        return stockMapper.selectList(w);
    }

    @Override
    public boolean existByCode(String code) {
        return stockMapper.selectCount(new LambdaQueryWrapper<StockEntity>().eq(StockEntity::getCode, code)) > 0;
    }

    @Override
    public void addStock(StockEntity stockEntity) {
        stockMapper.insert(stockEntity);
    }

    @Override
    public void addStockBatch(List<StockEntity> stockEntityList) {
        if (CollectionUtils.isEmpty(stockEntityList)) {
            return;
        }
        for (int i = 0; i < stockEntityList.size(); i += INSERT_BATCH_SIZE) {
            int end = Math.min(i + INSERT_BATCH_SIZE, stockEntityList.size());
            stockMapper.insertBatch(stockEntityList.subList(i, end));
        }
    }

    @Override
    public Set<String> queryByStockCodeList(List<String> stockCodeList) {
        if (CollectionUtils.isEmpty(stockCodeList)) {
            return Set.of();
        }
        List<StockEntity> stockEntityList = stockMapper.selectList(new LambdaQueryWrapper<StockEntity>().in(StockEntity::getCode, stockCodeList));
        return stockEntityList.stream().map(StockEntity::getCode).collect(Collectors.toSet());
    }
}
