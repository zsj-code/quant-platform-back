package com.quant.platform.ai.core.factor.fundamental.hardfilter;

import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalDecision;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;
import com.quant.platform.ai.core.port.RegulatoryAnnouncementPort;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * H2 监管处罚：近两年存在“证监会立案调查”或“交易所公开谴责” → 直接踢出。
 *
 * 数据缺口：
 * - 当前项目的公告表 `stock_announcement` 主要是东财公告抓取，尚未见对“立案调查/公开谴责”做结构化分类；
 * - 需要：监管事件结构化表（事件类型、发生时间、来源链接），或对公告标题/正文做 NLP/规则提取并落库。
 */
public class RegulatoryPunishmentHardFilterFactor implements FundamentalFactor {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

    private final RegulatoryAnnouncementPort regulatoryAnnouncementPort;

    public RegulatoryPunishmentHardFilterFactor(@Nullable RegulatoryAnnouncementPort regulatoryAnnouncementPort) {
        this.regulatoryAnnouncementPort = regulatoryAnnouncementPort;
    }

    @Override
    public String factorKey() {
        return "fund.hard.regulatory_punishment";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.HARD_FILTER;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        if (regulatoryAnnouncementPort == null) {
            return FundamentalResult.unavailable(factorKey(), group(), "未配置 RegulatoryAnnouncementPort，无法查询公告表");
        }
        String code = ctx.getSecCode();
        if (code == null || code.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少证券代码，无法查询监管相关公告");
        }

        LocalDate since = LocalDate.now(CN).minusYears(2);

        final RegulatoryAnnouncementPort.RegulatoryPunishmentScanResult scan;
        try {
            scan = regulatoryAnnouncementPort.scanRegulatoryKeywords(code.trim(), since);
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "查询监管相关公告失败：" + e.getMessage());
        }

        long hits = scan.matchCount();
        List<String> samples = scan.sampleTitles();

        if (hits > 0) {
            return FundamentalResult.builder(factorKey(), group())
                    .decision(FundamentalDecision.HARD_EXCLUDE)
                    .summary("近两年公告命中监管敏感关键字（立案调查/公开谴责/违法违规），踢出")
                    .metric("notice_date_since", since.toString())
                    .metric("match_count", hits)
                    .metric("sample_titles", samples)
                    .metric("hit", true)
                    .build();
        }

        return FundamentalResult.builder(factorKey(), group())
                .decision(FundamentalDecision.PASS)
                .summary("近两年公告未命中立案调查/公开谴责/违法违规关键字")
                .metric("notice_date_since", since.toString())
                .metric("match_count", 0L)
                .metric("sample_titles", samples)
                .metric("hit", false)
                .build();
    }
}
