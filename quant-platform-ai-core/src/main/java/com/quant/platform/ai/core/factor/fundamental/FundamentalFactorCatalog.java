package com.quant.platform.ai.core.factor.fundamental;


import com.quant.platform.ai.core.client.EastmoneyF10GIncomeClient;
import com.quant.platform.ai.core.client.EastmoneyPledgeRatioClient;
import com.quant.platform.ai.core.client.EastmoneyShareHolderIncreaseClient;
import com.quant.platform.ai.core.client.ThsFinanceAnnounceClient;
import com.quant.platform.ai.core.factor.fundamental.hardfilter.*;
import com.quant.platform.ai.core.factor.fundamental.watchlist.*;
import com.quant.platform.ai.core.port.KlineBarPort;
import com.quant.platform.ai.core.port.RegulatoryAnnouncementPort;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

public final class FundamentalFactorCatalog {
    private FundamentalFactorCatalog() {
    }

    public static Map<FundamentalFactorGroup, List<FundamentalFactor>> allGrouped() {
        return allGrouped(null, null, null, null, null, null);
    }

    /**
     * @param thsFinanceAnnounceClient 为 null 时，{@link AuditOpinionHardFilterFactor} 对审计数据源不可用（返回 UNAVAILABLE）
     */
    public static Map<FundamentalFactorGroup, List<FundamentalFactor>> allGrouped(
            @Nullable ThsFinanceAnnounceClient thsFinanceAnnounceClient) {
        return allGrouped(thsFinanceAnnounceClient, null, null, null, null, null);
    }

    /**
     * @param thsFinanceAnnounceClient 为 null 时，{@link AuditOpinionHardFilterFactor} 不可用
     * @param regulatoryAnnouncementPort 为 null 时，{@link RegulatoryPunishmentHardFilterFactor} 不可用
     */
    public static Map<FundamentalFactorGroup, List<FundamentalFactor>> allGrouped(
            @Nullable ThsFinanceAnnounceClient thsFinanceAnnounceClient,
            @Nullable RegulatoryAnnouncementPort regulatoryAnnouncementPort) {
        return allGrouped(thsFinanceAnnounceClient, regulatoryAnnouncementPort, null, null, null, null);
    }

    /**
     * @param eastmoneyPledgeRatioClient 与 {@code klineBarPort} 任一为 null 时，{@link HighRiskPledgeHardFilterFactor} 不可用
     */
    public static Map<FundamentalFactorGroup, List<FundamentalFactor>> allGrouped(
            @Nullable ThsFinanceAnnounceClient thsFinanceAnnounceClient,
            @Nullable RegulatoryAnnouncementPort regulatoryAnnouncementPort,
            @Nullable EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient,
            @Nullable KlineBarPort klineBarPort) {
        return allGrouped(thsFinanceAnnounceClient, regulatoryAnnouncementPort,
                eastmoneyPledgeRatioClient, klineBarPort, null, null);
    }

    /**
     * @param shareHolderIncreaseClient 为 null 时，{@link ShareholderReductionWatchlistFactor} 不可用
     * @param eastmoneyF10GIncomeClient 为 null 时，依赖 F10 的观察/硬筛因子不可用
     */
    public static Map<FundamentalFactorGroup, List<FundamentalFactor>> allGrouped(
            @Nullable ThsFinanceAnnounceClient thsFinanceAnnounceClient,
            @Nullable RegulatoryAnnouncementPort regulatoryAnnouncementPort,
            @Nullable EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient,
            @Nullable KlineBarPort klineBarPort,
            @Nullable EastmoneyShareHolderIncreaseClient shareHolderIncreaseClient) {
        return allGrouped(thsFinanceAnnounceClient, regulatoryAnnouncementPort,
                eastmoneyPledgeRatioClient, klineBarPort, shareHolderIncreaseClient, null);
    }

    public static Map<FundamentalFactorGroup, List<FundamentalFactor>> allGrouped(
            @Nullable ThsFinanceAnnounceClient thsFinanceAnnounceClient,
            @Nullable RegulatoryAnnouncementPort regulatoryAnnouncementPort,
            @Nullable EastmoneyPledgeRatioClient eastmoneyPledgeRatioClient,
            @Nullable KlineBarPort klineBarPort,
            @Nullable EastmoneyShareHolderIncreaseClient shareHolderIncreaseClient,
            @Nullable EastmoneyF10GIncomeClient eastmoneyF10GIncomeClient) {
        return Map.of(
                FundamentalFactorGroup.HARD_FILTER, List.of(
                        new AuditOpinionHardFilterFactor(thsFinanceAnnounceClient),
                        new RegulatoryPunishmentHardFilterFactor(regulatoryAnnouncementPort),
                        new CashflowDeathDivergenceHardFilterFactor(),
                        new HighRiskPledgeHardFilterFactor(eastmoneyPledgeRatioClient, klineBarPort),
                        new GoodwillDamHardFilterFactor(eastmoneyF10GIncomeClient),
                        new ShellCompanyHardFilterFactor(),
                        new BadAuditFirmHardFilterFactor()
                ),
                FundamentalFactorGroup.WATCHLIST, List.of(
                        new DepositLoanDoubleHighWatchlistFactor(),
                        new RndOverCapitalizationWatchlistFactor(eastmoneyF10GIncomeClient),
                        new DeductNetProfitDeteriorationWatchlistFactor(),
                        new LargeImpairmentWatchlistFactor(eastmoneyF10GIncomeClient),
                        new ShareholderReductionWatchlistFactor(shareHolderIncreaseClient),
                        new ConsensusEstimateCutWatchlistFactor()
                ),
                FundamentalFactorGroup.QUALITY_SCORE, List.of(
                        // TODO: 质量评分因子较多，且依赖行业分位/一致预期等数据源；后续补齐
                )
        );
    }
}

