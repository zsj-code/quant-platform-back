package com.quant.platform.handler;

import com.quant.platform.business.job.*;
import com.quant.platform.common.enums.KlineIntervalTypeEnum;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class DataSyncJobHandler {

    private static final Logger log = LoggerFactory.getLogger(DataSyncJobHandler.class);
    @Autowired
    private StockSyncService stockSyncService;

    @Autowired
    private EastmoneyNoticeSyncService eastmoneyNoticeSyncService;

    @Autowired
    private KlineSyncService klineSyncService;

    @Autowired
    private KlineDailyAggregateService klineDailyAggregateService;

    @Autowired
    private KlineMinuteAggregateService klineMinuteAggregateService;

    @Autowired
    private EastmoneyFinancialStatementSyncService eastmoneyFinancialStatementSyncService;

    @Autowired
    private EastmoneyResearchReportSyncService eastmoneyResearchReportSyncService;

    @Autowired
    private StockPostSyncService stockPostSyncService;

    @Autowired
    private TaogubaPostCommentSyncService taogubaPostCommentSyncService;

    @Autowired
    private StockValuationSnapshotSyncService stockValuationSnapshotSyncService;

    public DataSyncJobHandler() {
    }

    /**
     * 同步 stocks。param 示例：sleepMs=8000（翻页间隔毫秒，默认 8000；≤0 不休眠）
     */
    @XxlJob("syncStockBasics")
    public void syncStockBasics() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        int sleepMs = parseInt(kv.get("sleepMs"), 5000);
        stockSyncService.syncAllStock(sleepMs);
        XxlJobHelper.log("syncStockBasics done, sleepMs={0}", sleepMs);
    }

    /**
     * 同步个股估值/行情快照（东财 stock/get → stock_valuation_snapshot）。param 示例：sleepMs=80;maxStocks=0
     * （maxStocks≤0 表示全量 stocks 表）
     */
    @XxlJob("syncStockValuationSnapshot")
    public void syncStockValuationSnapshot() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        int sleepMs = parseInt(kv.get("sleepMs"), 8000);
        int maxStocks = parseInt(kv.get("maxStocks"), 0);
        long n = stockValuationSnapshotSyncService.syncAll(sleepMs, maxStocks);
        XxlJobHelper.log("syncStockValuationSnapshot done, sleepMs={0}, maxStocks={1}, rows={2}", sleepMs, maxStocks, n);
    }

    /**
     * 同步财报
     */
    @XxlJob("syncFinanceReports")
    public void syncFinanceReports() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        int sleepMs = parseInt(kv.get("sleepMs"), 80);
        int maxPages = parseInt(kv.get("maxPages"), 1);
        long affected = eastmoneyFinancialStatementSyncService.syncAll(sleepMs, maxPages);
        XxlJobHelper.log("syncFinanceReports done, sleepMs={0}, maxPages={1}, affected={2}", sleepMs, maxPages,
                affected);
    }

    /**
     * 同步公告
     */
    @XxlJob("syncEastmoneyNotice")
    public void syncEastmoneyNotice() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        // pageSize=30;maxPages=10;sleepMs=80
        int pageSize = parseInt(kv.get("pageSize"), 10);
        int maxPages = parseInt(kv.get("maxPages"), 1);
        int sleepMs = parseInt(kv.get("sleepMs"), 10);
        long affected = eastmoneyNoticeSyncService.syncAll(pageSize, maxPages, sleepMs);
        XxlJobHelper.log("syncEastmoneyNotice done, pageSize={0}, maxPages={1}, sleepMs={2}, affected={3}", pageSize,
                maxPages, sleepMs, affected);
    }

    /**
     * 研报同步（近 1 年由 Client 时间参数控制）。 param
     * 示例：mode=all;pageSize=20;maxPagesStock=30;maxPagesIndustry=100;sleepMsStock=80;sleepMsIndustry=0
     * mode=stock|industry|all
     */
    @XxlJob("syncResearchReports")
    public void syncResearchReports() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        String mode = kv.getOrDefault("mode", "all").trim().toLowerCase();
        int pageSize = parseInt(kv.get("pageSize"), 20);
        int maxPagesStock = parseInt(kv.get("maxPagesStock"), 1);
        int maxPagesIndustry = parseInt(kv.get("maxPagesIndustry"), 3);
        int sleepMsStock = parseInt(kv.get("sleepMsStock"), 10);
        int sleepMsIndustry = parseInt(kv.get("sleepMsIndustry"), 10);
        long affected;
        if ("stock".equals(mode)) {
            affected = eastmoneyResearchReportSyncService.syncStockResearchReports(pageSize, maxPagesStock,
                    sleepMsStock);
        } else if ("industry".equals(mode)) {
            affected = eastmoneyResearchReportSyncService.syncIndustryResearchReports(pageSize, maxPagesIndustry,
                    sleepMsIndustry);
        } else {
            affected = eastmoneyResearchReportSyncService.syncAll(pageSize, maxPagesStock, maxPagesIndustry,
                    sleepMsStock, sleepMsIndustry);
        }
        XxlJobHelper.log("syncResearchReports mode={0}, pageSize={1}, affected={2}", mode, pageSize, affected);
    }

    @XxlJob("syncStockPost")
    public void syncStockPost() {
        String param = XxlJobHelper.getJobParam();
        stockPostSyncService.syncAll(10, 100);
    }

    /**
     * 淘股吧：近 N 天帖与评论。时间窗等见 {@code quant.integration.community-post.*}
     */
    @XxlJob("syncTaoGubaPostComment")
    public void syncTaoGubaPostComment() {
        TaogubaPostCommentSyncService.SyncStats s = taogubaPostCommentSyncService.syncTaogubaForAllStocks();
        XxlJobHelper.log("syncTaoGubaPostComment posts={0} comments={1}", s.getPosts(), s.getComments());
    }

    /**
     * 同步日k交易数据
     */
    @XxlJob("syncKlineDay")
    public void syncKlineDay() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        LocalDate beg = KlineSyncService.parseDate(kv.get("beg"), LocalDate.now().minusDays(5));
        LocalDate end = KlineSyncService.parseDate(kv.get("end"), LocalDate.now());
        int fqt = parseInt(kv.get("fqt"), 1);
        int sleepMs = parseInt(kv.get("sleepMs"), 500);

        long inserted = klineSyncService.syncAll(KlineIntervalTypeEnum.D.getCode(), fqt, beg, end, sleepMs);
        XxlJobHelper.log("syncKline {0} done, beg={1}, end={2}, inserted={3}", KlineIntervalTypeEnum.D.getCode(), beg, end, inserted);
    }

    /**
     * 同步1分钟交易数据
     */
    @XxlJob("syncKlineSecond")
    public void syncKlineSecond() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        LocalDate beg = KlineSyncService.parseDate(kv.get("beg"), LocalDate.now().minusDays(3));
        LocalDate end = KlineSyncService.parseDate(kv.get("end"), LocalDate.now());
        int fqt = parseInt(kv.get("fqt"), 1);
        int sleepMs = parseInt(kv.get("sleepMs"), 500);

        Set<String> set = new HashSet<>();
        set.add(KlineIntervalTypeEnum.M1.getCode());
        long inserted = klineSyncService.syncAllMulti(set, fqt, beg, end, sleepMs);
        XxlJobHelper.log("syncKline {0} done, beg={1}, end={2}, inserted={3}", KlineIntervalTypeEnum.M1.getCode(), beg, end, inserted);
    }

    /**
     * 由日 K 聚合生成周 / 月 / 年 K 并写入 kline_bar（不请求东财）。param 示例：
     * beg=20200101;end=20260424;types=W,M,Y
     * 仅一只股票时加 code=600000（或 symbol=600000.SH）
     */
    @XxlJob("aggregateKlineFromDaily")
    public void aggregateKlineFromDaily() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        LocalDate beg = KlineSyncService.parseDate(kv.get("beg"), LocalDate.of(2026, 1, 1));
        LocalDate end = KlineSyncService.parseDate(kv.get("end"), LocalDate.now());
        Set<String> types = KlineSyncService.parseIntervalTypes(kv.getOrDefault("types", "W,M,Y"));
        String one = kv.get("code");
        if (one == null || one.trim().isEmpty()) {
            one = kv.get("symbol");
        }
        long n;
        if (one != null && !one.trim().isEmpty()) {
            n = klineDailyAggregateService.aggregateForStock(one.trim(), beg, end, types);
            XxlJobHelper.log(
                    "aggregateKlineFromDaily done (single), code/symbol={0}, beg={1}, end={2}, types={3}, affected={4}",
                    one.trim(), beg, end, String.join(",", types), n);
        } else {
            n = klineDailyAggregateService.aggregateAll(beg, end, types);
            XxlJobHelper.log("aggregateKlineFromDaily done, beg={0}, end={1}, types={2}, affected={3}", beg, end,
                    String.join(",", types), n);
        }
    }

    /**
     * 由 1 分钟 K 聚合生成更高分钟 K（不请求东财，基于已落库 {@code interval_type=M1}）。param 示例：
     * beg=20260401;end=20260424;types=M5,M15,M30,M60,M120
     * 仅一只股票时加 code=600000（或 symbol=600000.SH）
     */
    @XxlJob("aggregateKlineFromM1")
    public void aggregateKlineFromM1() {
        String param = XxlJobHelper.getJobParam();
        var kv = KlineSyncService.parseJobParam(param);
        LocalDate beg = KlineSyncService.parseDate(kv.get("beg"), LocalDate.now().minusDays(7));
        LocalDate end = KlineSyncService.parseDate(kv.get("end"), LocalDate.now());
        Set<String> types = KlineSyncService.parseIntervalTypes(kv.getOrDefault("types", "M5,M15,M30,M60,M120"));
        String one = kv.get("code");
        if (one == null || one.trim().isEmpty()) {
            one = kv.get("symbol");
        }
        long n;
        if (one != null && !one.trim().isEmpty()) {
            n = klineMinuteAggregateService.aggregateForStock(one.trim(), beg, end, types);
            XxlJobHelper.log(
                    "aggregateKlineFromM1 done (single), code/symbol={0}, beg={1}, end={2}, types={3}, affected={4}",
                    one.trim(), beg, end, String.join(",", types), n);
        } else {
            n = klineMinuteAggregateService.aggregateAll(beg, end, types);
            XxlJobHelper.log("aggregateKlineFromM1 done, beg={0}, end={1}, types={2}, affected={3}", beg, end,
                    String.join(",", types), n);
        }
    }

    private static int parseInt(String s, int defaultValue) {
        if (s == null || s.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
