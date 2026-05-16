package com.quant.platform.ai.core.factor.fundamental.hardfilter;

import com.quant.platform.ai.core.client.ThsFinanceAnnounceClient;
import com.quant.platform.ai.core.client.dto.ThsFinanceAnnounceYearDTO;
import com.quant.platform.ai.core.factor.fundamental.FundamentalContext;
import com.quant.platform.ai.core.factor.fundamental.FundamentalDecision;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactor;
import com.quant.platform.ai.core.factor.fundamental.FundamentalFactorGroup;
import com.quant.platform.ai.core.factor.fundamental.FundamentalResult;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

/**
 * H1 审计意见：近两个完整自然年（通常对应已披露年报的 FY-1、FY-2）中，任一年审核意见为非标准无保留（含强调事项段、保留、否定、无法表示意见等），则硬剔除。
 * <p>
 * 数据来源：同花顺 basic {@link ThsFinanceAnnounceClient}（各自然年 {@code opinion} 字段）。
 */
public class AuditOpinionHardFilterFactor implements FundamentalFactor {

    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

    /** 与「无保留意见」区分：匹配「保留意见」但前一字不为「无」 */
    private static final Pattern QUALIFIED_OPINION = Pattern.compile("(?<!无)保留意见");

    private final ThsFinanceAnnounceClient thsFinanceAnnounceClient;

    public AuditOpinionHardFilterFactor(@Nullable ThsFinanceAnnounceClient thsFinanceAnnounceClient) {
        this.thsFinanceAnnounceClient = thsFinanceAnnounceClient;
    }

    @Override
    public String factorKey() {
        return "fund.hard.audit_opinion_non_standard";
    }

    @Override
    public FundamentalFactorGroup group() {
        return FundamentalFactorGroup.HARD_FILTER;
    }

    @Override
    public FundamentalResult evaluate(FundamentalContext ctx) {
        if (thsFinanceAnnounceClient == null) {
            return FundamentalResult.unavailable(factorKey(), group(), "未配置 ThsFinanceAnnounceClient，无法拉取审计意见");
        }
        String code = ctx.getSecCode();
        if (code == null || code.isBlank()) {
            return FundamentalResult.unavailable(factorKey(), group(), "缺少证券代码，无法拉取审计意见");
        }

        final List<ThsFinanceAnnounceYearDTO> rows;
        try {
            rows = thsFinanceAnnounceClient.fetchFinanceAnnounceDetail(code.trim(), 1, 24);
        } catch (Exception e) {
            return FundamentalResult.unavailable(factorKey(), group(), "拉取审计意见失败：" + e.getMessage());
        }
        if (rows.isEmpty()) {
            return FundamentalResult.unavailable(factorKey(), group(), "未获取到财务年度审计意见数据");
        }

        int y1 = LocalDate.now(CN).getYear() - 1;
        int y2 = LocalDate.now(CN).getYear() - 2;

        YearOpinion yOp1 = resolveYear(rows, y1);
        YearOpinion yOp2 = resolveYear(rows, y2);

        if (yOp1.kind() == AuditOpinionKind.UNKNOWN || yOp2.kind() == AuditOpinionKind.UNKNOWN) {
            return FundamentalResult.builder(factorKey(), group())
                    .decision(FundamentalDecision.UNAVAILABLE)
                    .summary("近两个完整财年审计意见数据不全或无法解析（需已披露且接口返回非空意见）")
                    .metric("fiscal_year_n1", y1)
                    .metric("fiscal_year_n2", y2)
                    .metric("opinion_raw_n1", yOp1.raw())
                    .metric("opinion_raw_n2", yOp2.raw())
                    .metric("kind_n1", yOp1.kind().name())
                    .metric("kind_n2", yOp2.kind().name())
                    .build();
        }

        boolean hit = yOp1.kind() == AuditOpinionKind.NON_STANDARD || yOp2.kind() == AuditOpinionKind.NON_STANDARD;

        if (hit) {
            return FundamentalResult.builder(factorKey(), group())
                    .decision(FundamentalDecision.HARD_EXCLUDE)
                    .summary("近两个完整财年存在非标审计意见（含强调事项段/保留/否定/无法表示意见等），踢出")
                    .metric("fiscal_year_n1", y1)
                    .metric("fiscal_year_n2", y2)
                    .metric("opinion_raw_n1", yOp1.raw())
                    .metric("opinion_raw_n2", yOp2.raw())
                    .metric("kind_n1", yOp1.kind().name())
                    .metric("kind_n2", yOp2.kind().name())
                    .metric("hit", true)
                    .build();
        }

        return FundamentalResult.builder(factorKey(), group())
                .decision(FundamentalDecision.PASS)
                .summary("近两个完整财年均为标准无保留类审计意见")
                .metric("fiscal_year_n1", y1)
                .metric("fiscal_year_n2", y2)
                .metric("opinion_raw_n1", yOp1.raw())
                .metric("opinion_raw_n2", yOp2.raw())
                .metric("kind_n1", yOp1.kind().name())
                .metric("kind_n2", yOp2.kind().name())
                .metric("hit", false)
                .build();
    }

    private static YearOpinion resolveYear(List<ThsFinanceAnnounceYearDTO> rows, int year) {
        String key = String.valueOf(year);
        ThsFinanceAnnounceYearDTO row = rows.stream().filter(r -> key.equals(r.year())).findFirst().orElse(null);
        if (row == null) {
            return new YearOpinion(year, null, AuditOpinionKind.UNKNOWN);
        }
        String text = row.auditOpinion();
        if (text == null || text.isBlank()) {
            return new YearOpinion(year, "", AuditOpinionKind.UNKNOWN);
        }
        return new YearOpinion(year, text.trim(), classifyAuditOpinion(text.trim()));
    }

    private record YearOpinion(int year, String raw, AuditOpinionKind kind) {
    }

    enum AuditOpinionKind {
        STANDARD,
        NON_STANDARD,
        UNKNOWN
    }

    /**
     * 将同花顺返回的审核意见文案分为标准无保留 / 非标 / 无法判断。
     */
    static AuditOpinionKind classifyAuditOpinion(String t) {
        if (t.isEmpty()) {
            return AuditOpinionKind.UNKNOWN;
        }
        if (t.contains("无法表示意见")) {
            return AuditOpinionKind.NON_STANDARD;
        }
        if (t.contains("否定意见")) {
            return AuditOpinionKind.NON_STANDARD;
        }
        if (QUALIFIED_OPINION.matcher(t).find()) {
            return AuditOpinionKind.NON_STANDARD;
        }
        if (t.contains("强调事项段")) {
            return AuditOpinionKind.NON_STANDARD;
        }
        if (t.contains("其他事项段")) {
            return AuditOpinionKind.NON_STANDARD;
        }
        if (t.contains("持续经营") && (t.contains("重大不确定性") || t.contains("段落"))) {
            return AuditOpinionKind.NON_STANDARD;
        }
        if (t.contains("非无保留意见")) {
            return AuditOpinionKind.NON_STANDARD;
        }
        if (t.contains("标准无保留意见")) {
            return AuditOpinionKind.STANDARD;
        }
        if ("无保留意见".equals(t)) {
            return AuditOpinionKind.STANDARD;
        }
        return AuditOpinionKind.UNKNOWN;
    }
}
