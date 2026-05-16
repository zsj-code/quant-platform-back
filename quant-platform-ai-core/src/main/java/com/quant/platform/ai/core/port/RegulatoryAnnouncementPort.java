package com.quant.platform.ai.core.port;

import java.time.LocalDate;
import java.util.List;

/**
 * 基于公告库（如 {@code stock_announcement}）的监管类公告检索。
 */
public interface RegulatoryAnnouncementPort {

    /**
     * 统计指定证券在公告日下界（含）之后，标题或栏目命中监管关键字的公告条数，并返回若干条标题样例。
     *
     * @param secCode                   6 位证券代码
     * @param noticeDateSinceInclusive  公告日下界（含），通常取「近两年」起点
     */
    RegulatoryPunishmentScanResult scanRegulatoryKeywords(String secCode, LocalDate noticeDateSinceInclusive);

    /**
     * 监管处罚硬筛扫描结果。
     *
     * @param matchCount   命中条数
     * @param sampleTitles 按公告时间倒序截取的标题样例（至多若干条）
     */
    record RegulatoryPunishmentScanResult(long matchCount, List<String> sampleTitles) {
        public RegulatoryPunishmentScanResult {
            sampleTitles = sampleTitles == null ? List.of() : List.copyOf(sampleTitles);
        }
    }
}
