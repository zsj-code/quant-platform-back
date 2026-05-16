package com.quant.platform.business.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.quant.platform.ai.core.port.RegulatoryAnnouncementPort;
import com.quant.platform.business.stock.entity.StockAnnouncementEntity;
import com.quant.platform.business.stock.mapper.StockAnnouncementMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于表 {@code stock_announcement}：{@code title} / {@code categories} 命中监管关键字即计命中。
 */
@Service
public class RegulatoryAnnouncementPortAdapter implements RegulatoryAnnouncementPort {

    private static final String[] REGULATORY_KEYWORDS = {"立案调查", "公开谴责", "违法违规"};

    private final StockAnnouncementMapper stockAnnouncementMapper;

    public RegulatoryAnnouncementPortAdapter(StockAnnouncementMapper stockAnnouncementMapper) {
        this.stockAnnouncementMapper = stockAnnouncementMapper;
    }

    @Override
    public RegulatoryPunishmentScanResult scanRegulatoryKeywords(String secCode, LocalDate noticeDateSinceInclusive) {
        if (secCode == null || secCode.isBlank() || noticeDateSinceInclusive == null) {
            return new RegulatoryAnnouncementPort.RegulatoryPunishmentScanResult(0L, List.of());
        }
        String code = secCode.trim();
        LambdaQueryWrapper<StockAnnouncementEntity> wrapper = buildKeywordWrapper(code, noticeDateSinceInclusive);

        Long cnt = stockAnnouncementMapper.selectCount(wrapper);
        long matchCount = cnt == null ? 0L : cnt;

        List<String> samples = List.of();
        if (matchCount > 0) {
            LambdaQueryWrapper<StockAnnouncementEntity> sampleW = buildKeywordWrapper(code, noticeDateSinceInclusive);
            sampleW.orderByDesc(StockAnnouncementEntity::getNoticeDate)
                    .orderByDesc(StockAnnouncementEntity::getAnnounceTime)
                    .last("LIMIT 5");
            List<StockAnnouncementEntity> rows = stockAnnouncementMapper.selectList(sampleW);
            List<String> titles = new ArrayList<>();
            for (StockAnnouncementEntity e : rows) {
                if (e.getTitle() != null && !e.getTitle().isBlank()) {
                    titles.add(e.getTitle().trim());
                }
            }
            samples = List.copyOf(titles);
        }
        return new RegulatoryAnnouncementPort.RegulatoryPunishmentScanResult(matchCount, samples);
    }

    private static LambdaQueryWrapper<StockAnnouncementEntity> buildKeywordWrapper(String code,
                                                                                     LocalDate sinceInclusive) {
        LambdaQueryWrapper<StockAnnouncementEntity> w = new LambdaQueryWrapper<StockAnnouncementEntity>()
                .eq(StockAnnouncementEntity::getCode, code)
                .apply("COALESCE(notice_date, DATE(announce_time)) >= {0}", sinceInclusive);
        w.and(q -> {
            for (int i = 0; i < REGULATORY_KEYWORDS.length; i++) {
                String kw = REGULATORY_KEYWORDS[i];
                if (i == 0) {
                    q.like(StockAnnouncementEntity::getTitle, kw).or().like(StockAnnouncementEntity::getCategories, kw);
                } else {
                    q.or().like(StockAnnouncementEntity::getTitle, kw).or().like(StockAnnouncementEntity::getCategories, kw);
                }
            }
        });
        return w;
    }
}
