package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 东财 F10 资产负债表（{@code RPT_F10_FINANCE_GBALANCE}）单行，字段与接口 {@code data} 一致。
 * <p>
 * 金额类字段单位与报表币种一致（一般为元）；字段后缀 {@code _YOY} 为同比增长率（%）。
 * 使用 class 而非 record：字段数超过 JVM record 形参上限（255）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EastmoneyF10GBalanceRowDTO {
    /** 带市场后缀证券代码 */
    @JsonProperty("SECUCODE") public String secucode;
    /** 证券 6 位代码 */
    @JsonProperty("SECURITY_CODE") public String securityCode;
    /** 证券简称 */
    @JsonProperty("SECURITY_NAME_ABBR") public String securityNameAbbr;
    /** 机构编码 */
    @JsonProperty("ORG_CODE") public String orgCode;
    /** 机构类型，如「通用」 */
    @JsonProperty("ORG_TYPE") public String orgType;
    /** 报告期（接口原样） */
    @JsonProperty("REPORT_DATE") public String reportDate;
    /** 报告类型，如「一季报」 */
    @JsonProperty("REPORT_TYPE") public String reportType;
    /** 报告期展示名 */
    @JsonProperty("REPORT_DATE_NAME") public String reportDateName;
    /** 证券类型编码 */
    @JsonProperty("SECURITY_TYPE_CODE") public String securityTypeCode;
    /** 公告日期 */
    @JsonProperty("NOTICE_DATE") public String noticeDate;
    /** 数据更新日期 */
    @JsonProperty("UPDATE_DATE") public String updateDate;
    /** 币种，如 CNY */
    @JsonProperty("CURRENCY") public String currency;
    /** 吸收存款及同业存放 */
    @JsonProperty("ACCEPT_DEPOSIT_INTERBANK") public BigDecimal acceptDepositInterbank;
    /** 应付账款 */
    @JsonProperty("ACCOUNTS_PAYABLE") public BigDecimal accountsPayable;
    /** 应收账款 */
    @JsonProperty("ACCOUNTS_RECE") public BigDecimal accountsRece;
    /** 预提费用 */
    @JsonProperty("ACCRUED_EXPENSE") public BigDecimal accruedExpense;
    /** 预收款项 */
    @JsonProperty("ADVANCE_RECEIVABLES") public BigDecimal advanceReceivables;
    /** 代理买卖证券款 */
    @JsonProperty("AGENT_TRADE_SECURITY") public BigDecimal agentTradeSecurity;
    /** 代理承销证券款 */
    @JsonProperty("AGENT_UNDERWRITE_SECURITY") public BigDecimal agentUnderwriteSecurity;
    /** 以摊余成本计量的金融资产 */
    @JsonProperty("AMORTIZE_COST_FINASSET") public BigDecimal amortizeCostFinasset;
    /** 以摊余成本计量的金融负债 */
    @JsonProperty("AMORTIZE_COST_FINLIAB") public BigDecimal amortizeCostFinliab;
    /** 以摊余成本计量的非流动金融资产 */
    @JsonProperty("AMORTIZE_COST_NCFINASSET") public BigDecimal amortizeCostNcfinasset;
    /** 以摊余成本计量的非流动金融负债 */
    @JsonProperty("AMORTIZE_COST_NCFINLIAB") public BigDecimal amortizeCostNcfinliab;
    /** 指定以公允价值计量且其变动计入当期损益的金融资产 */
    @JsonProperty("APPOINT_FVTPL_FINASSET") public BigDecimal appointFvtplFinasset;
    /** 指定以公允价值计量且其变动计入当期损益的金融负债 */
    @JsonProperty("APPOINT_FVTPL_FINLIAB") public BigDecimal appointFvtplFinliab;
    /** 资产平衡项目 */
    @JsonProperty("ASSET_BALANCE") public BigDecimal assetBalance;
    /** 资产其他项目 */
    @JsonProperty("ASSET_OTHER") public BigDecimal assetOther;
    /** 分配股利、利润或偿付利息支付的现金（表内科目） */
    @JsonProperty("ASSIGN_CASH_DIVIDEND") public BigDecimal assignCashDividend;
    /** 可供出售金融资产 */
    @JsonProperty("AVAILABLE_SALE_FINASSET") public BigDecimal availableSaleFinasset;
    /** 应付债券 */
    @JsonProperty("BOND_PAYABLE") public BigDecimal bondPayable;
    /** 拆入资金 */
    @JsonProperty("BORROW_FUND") public BigDecimal borrowFund;
    /** 买入返售金融资产 */
    @JsonProperty("BUY_RESALE_FINASSET") public BigDecimal buyResaleFinasset;
    /** 资本公积 */
    @JsonProperty("CAPITAL_RESERVE") public BigDecimal capitalReserve;
    /** 在建工程 */
    @JsonProperty("CIP") public BigDecimal cip;
    /** 消耗性生物资产 */
    @JsonProperty("CONSUMPTIVE_BIOLOGICAL_ASSET") public BigDecimal consumptiveBiologicalAsset;
    /** 合同资产 */
    @JsonProperty("CONTRACT_ASSET") public BigDecimal contractAsset;
    /** 合同负债 */
    @JsonProperty("CONTRACT_LIAB") public BigDecimal contractLiab;
    /** 外币报表折算差额 */
    @JsonProperty("CONVERT_DIFF") public BigDecimal convertDiff;
    /** 债权投资 */
    @JsonProperty("CREDITOR_INVEST") public BigDecimal creditorInvest;
    /** 流动资产平衡项目 */
    @JsonProperty("CURRENT_ASSET_BALANCE") public BigDecimal currentAssetBalance;
    /** 流动资产其他项目 */
    @JsonProperty("CURRENT_ASSET_OTHER") public BigDecimal currentAssetOther;
    /** 流动负债平衡项目 */
    @JsonProperty("CURRENT_LIAB_BALANCE") public BigDecimal currentLiabBalance;
    /** 流动负债其他项目 */
    @JsonProperty("CURRENT_LIAB_OTHER") public BigDecimal currentLiabOther;
    /** 递延收益 */
    @JsonProperty("DEFER_INCOME") public BigDecimal deferIncome;
    /** 一年内到期的递延收益 */
    @JsonProperty("DEFER_INCOME_1YEAR") public BigDecimal deferIncome1year;
    /** 递延所得税资产 */
    @JsonProperty("DEFER_TAX_ASSET") public BigDecimal deferTaxAsset;
    /** 递延所得税负债 */
    @JsonProperty("DEFER_TAX_LIAB") public BigDecimal deferTaxLiab;
    /** 衍生金融资产 */
    @JsonProperty("DERIVE_FINASSET") public BigDecimal deriveFinasset;
    /** 衍生金融负债 */
    @JsonProperty("DERIVE_FINLIAB") public BigDecimal deriveFinliab;
    /** 开发支出 */
    @JsonProperty("DEVELOP_EXPENSE") public BigDecimal developExpense;
    /** 持有待售资产 */
    @JsonProperty("DIV_HOLDSALE_ASSET") public BigDecimal divHoldsaleAsset;
    /** 持有待售负债 */
    @JsonProperty("DIV_HOLDSALE_LIAB") public BigDecimal divHoldsaleLiab;
    /** 应付股利 */
    @JsonProperty("DIVIDEND_PAYABLE") public BigDecimal dividendPayable;
    /** 应收股利 */
    @JsonProperty("DIVIDEND_RECE") public BigDecimal dividendRece;
    /** 所有者权益平衡项目 */
    @JsonProperty("EQUITY_BALANCE") public BigDecimal equityBalance;
    /** 所有者权益其他项目 */
    @JsonProperty("EQUITY_OTHER") public BigDecimal equityOther;
    /** 应收出口退税 */
    @JsonProperty("EXPORT_REFUND_RECE") public BigDecimal exportRefundRece;
    /** 应付手续费及佣金 */
    @JsonProperty("FEE_COMMISSION_PAYABLE") public BigDecimal feeCommissionPayable;
    /** 融出资金 */
    @JsonProperty("FIN_FUND") public BigDecimal finFund;
    /** 应收款项融资 */
    @JsonProperty("FINANCE_RECE") public BigDecimal financeRece;
    /** 固定资产 */
    @JsonProperty("FIXED_ASSET") public BigDecimal fixedAsset;
    /** 固定资产清理 */
    @JsonProperty("FIXED_ASSET_DISPOSAL") public BigDecimal fixedAssetDisposal;
    /** 以公允价值计量且其变动计入其他综合收益的金融资产 */
    @JsonProperty("FVTOCI_FINASSET") public BigDecimal fvtociFinasset;
    /** 以公允价值计量且其变动计入其他综合收益的非流动金融资产 */
    @JsonProperty("FVTOCI_NCFINASSET") public BigDecimal fvtociNcfinasset;
    /** 以公允价值计量且其变动计入当期损益的金融资产 */
    @JsonProperty("FVTPL_FINASSET") public BigDecimal fvtplFinasset;
    /** 以公允价值计量且其变动计入当期损益的金融负债 */
    @JsonProperty("FVTPL_FINLIAB") public BigDecimal fvtplFinliab;
    /** 一般风险准备 */
    @JsonProperty("GENERAL_RISK_RESERVE") public BigDecimal generalRiskReserve;
    /** 商誉 */
    @JsonProperty("GOODWILL") public BigDecimal goodwill;
    /** 持有至到期投资 */
    @JsonProperty("HOLD_MATURITY_INVEST") public BigDecimal holdMaturityInvest;
    /** 划分为持有待售的资产 */
    @JsonProperty("HOLDSALE_ASSET") public BigDecimal holdsaleAsset;
    /** 划分为持有待售的负债 */
    @JsonProperty("HOLDSALE_LIAB") public BigDecimal holdsaleLiab;
    /** 保险合同准备金 */
    @JsonProperty("INSURANCE_CONTRACT_RESERVE") public BigDecimal insuranceContractReserve;
    /** 无形资产 */
    @JsonProperty("INTANGIBLE_ASSET") public BigDecimal intangibleAsset;
    /** 应付利息 */
    @JsonProperty("INTEREST_PAYABLE") public BigDecimal interestPayable;
    /** 应收利息 */
    @JsonProperty("INTEREST_RECE") public BigDecimal interestRece;
    /** 内部应付款 */
    @JsonProperty("INTERNAL_PAYABLE") public BigDecimal internalPayable;
    /** 内部应收款 */
    @JsonProperty("INTERNAL_RECE") public BigDecimal internalRece;
    /** 存货 */
    @JsonProperty("INVENTORY") public BigDecimal inventory;
    /** 投资性房地产 */
    @JsonProperty("INVEST_REALESTATE") public BigDecimal investRealestate;
    /** 租赁负债 */
    @JsonProperty("LEASE_LIAB") public BigDecimal leaseLiab;
    /** 融出资金（同业） */
    @JsonProperty("LEND_FUND") public BigDecimal lendFund;
    /** 负债平衡项目 */
    @JsonProperty("LIAB_BALANCE") public BigDecimal liabBalance;
    /** 负债及股东权益平衡项目 */
    @JsonProperty("LIAB_EQUITY_BALANCE") public BigDecimal liabEquityBalance;
    /** 负债及股东权益其他项目 */
    @JsonProperty("LIAB_EQUITY_OTHER") public BigDecimal liabEquityOther;
    /** 负债其他项目 */
    @JsonProperty("LIAB_OTHER") public BigDecimal liabOther;
    /** 发放贷款及垫款 */
    @JsonProperty("LOAN_ADVANCE") public BigDecimal loanAdvance;
    /** 向中央银行借款 */
    @JsonProperty("LOAN_PBC") public BigDecimal loanPbc;
    /** 长期股权投资 */
    @JsonProperty("LONG_EQUITY_INVEST") public BigDecimal longEquityInvest;
    /** 长期借款 */
    @JsonProperty("LONG_LOAN") public BigDecimal longLoan;
    /** 长期应付款 */
    @JsonProperty("LONG_PAYABLE") public BigDecimal longPayable;
    /** 长期待摊费用 */
    @JsonProperty("LONG_PREPAID_EXPENSE") public BigDecimal longPrepaidExpense;
    /** 长期应收款 */
    @JsonProperty("LONG_RECE") public BigDecimal longRece;
    /** 长期应付职工薪酬 */
    @JsonProperty("LONG_STAFFSALARY_PAYABLE") public BigDecimal longStaffsalaryPayable;
    /** 少数股东权益 */
    @JsonProperty("MINORITY_EQUITY") public BigDecimal minorityEquity;
    /** 货币资金 */
    @JsonProperty("MONETARYFUNDS") public BigDecimal monetaryfunds;
    /** 一年内到期的非流动资产 */
    @JsonProperty("NONCURRENT_ASSET_1YEAR") public BigDecimal noncurrentAsset1year;
    /** 非流动资产平衡项目 */
    @JsonProperty("NONCURRENT_ASSET_BALANCE") public BigDecimal noncurrentAssetBalance;
    /** 非流动资产其他项目 */
    @JsonProperty("NONCURRENT_ASSET_OTHER") public BigDecimal noncurrentAssetOther;
    /** 一年内到期的非流动负债 */
    @JsonProperty("NONCURRENT_LIAB_1YEAR") public BigDecimal noncurrentLiab1year;
    /** 非流动负债平衡项目 */
    @JsonProperty("NONCURRENT_LIAB_BALANCE") public BigDecimal noncurrentLiabBalance;
    /** 非流动负债其他项目 */
    @JsonProperty("NONCURRENT_LIAB_OTHER") public BigDecimal noncurrentLiabOther;
    /** 应付票据及应付账款 */
    @JsonProperty("NOTE_ACCOUNTS_PAYABLE") public BigDecimal noteAccountsPayable;
    /** 应收票据及应收账款 */
    @JsonProperty("NOTE_ACCOUNTS_RECE") public BigDecimal noteAccountsRece;
    /** 应付票据 */
    @JsonProperty("NOTE_PAYABLE") public BigDecimal notePayable;
    /** 应收票据 */
    @JsonProperty("NOTE_RECE") public BigDecimal noteRece;
    /** 油气资产 */
    @JsonProperty("OIL_GAS_ASSET") public BigDecimal oilGasAsset;
    /** 其他综合收益 */
    @JsonProperty("OTHER_COMPRE_INCOME") public BigDecimal otherCompreIncome;
    /** 其他债权投资 */
    @JsonProperty("OTHER_CREDITOR_INVEST") public BigDecimal otherCreditorInvest;
    /** 其他流动资产 */
    @JsonProperty("OTHER_CURRENT_ASSET") public BigDecimal otherCurrentAsset;
    /** 其他流动负债 */
    @JsonProperty("OTHER_CURRENT_LIAB") public BigDecimal otherCurrentLiab;
    /** 其他权益工具投资 */
    @JsonProperty("OTHER_EQUITY_INVEST") public BigDecimal otherEquityInvest;
    /** 其他权益其他项目 */
    @JsonProperty("OTHER_EQUITY_OTHER") public BigDecimal otherEquityOther;
    /** 其他权益工具 */
    @JsonProperty("OTHER_EQUITY_TOOL") public BigDecimal otherEquityTool;
    /** 其他非流动资产 */
    @JsonProperty("OTHER_NONCURRENT_ASSET") public BigDecimal otherNoncurrentAsset;
    /** 其他非流动金融资产 */
    @JsonProperty("OTHER_NONCURRENT_FINASSET") public BigDecimal otherNoncurrentFinasset;
    /** 其他非流动负债 */
    @JsonProperty("OTHER_NONCURRENT_LIAB") public BigDecimal otherNoncurrentLiab;
    /** 其他应付款 */
    @JsonProperty("OTHER_PAYABLE") public BigDecimal otherPayable;
    /** 其他应收款 */
    @JsonProperty("OTHER_RECE") public BigDecimal otherRece;
    /** 归属于母公司股东权益平衡项目 */
    @JsonProperty("PARENT_EQUITY_BALANCE") public BigDecimal parentEquityBalance;
    /** 归属于母公司股东权益其他项目 */
    @JsonProperty("PARENT_EQUITY_OTHER") public BigDecimal parentEquityOther;
    /** 永续债（权益端） */
    @JsonProperty("PERPETUAL_BOND") public BigDecimal perpetualBond;
    /** 应付永续债 */
    @JsonProperty("PERPETUAL_BOND_PAYBALE") public BigDecimal perpetualBondPaybale;
    /** 预计流动负债 */
    @JsonProperty("PREDICT_CURRENT_LIAB") public BigDecimal predictCurrentLiab;
    /** 预计负债 */
    @JsonProperty("PREDICT_LIAB") public BigDecimal predictLiab;
    /** 优先股（权益端） */
    @JsonProperty("PREFERRED_SHARES") public BigDecimal preferredShares;
    /** 应付优先股 */
    @JsonProperty("PREFERRED_SHARES_PAYBALE") public BigDecimal preferredSharesPaybale;
    /** 应收保费 */
    @JsonProperty("PREMIUM_RECE") public BigDecimal premiumRece;
    /** 预付款项 */
    @JsonProperty("PREPAYMENT") public BigDecimal prepayment;
    /** 生产性生物资产 */
    @JsonProperty("PRODUCTIVE_BIOLOGY_ASSET") public BigDecimal productiveBiologyAsset;
    /** 工程物资 */
    @JsonProperty("PROJECT_MATERIAL") public BigDecimal projectMaterial;
    /** 应收分保合同准备金 */
    @JsonProperty("RC_RESERVE_RECE") public BigDecimal rcReserveRece;
    /** 应付分保账款 */
    @JsonProperty("REINSURE_PAYABLE") public BigDecimal reinsurePayable;
    /** 应收分保账款 */
    @JsonProperty("REINSURE_RECE") public BigDecimal reinsureRece;
    /** 卖出回购金融资产款 */
    @JsonProperty("SELL_REPO_FINASSET") public BigDecimal sellRepoFinasset;
    /** 结算备付金 */
    @JsonProperty("SETTLE_EXCESS_RESERVE") public BigDecimal settleExcessReserve;
    /** 股本 */
    @JsonProperty("SHARE_CAPITAL") public BigDecimal shareCapital;
    /** 应付短期债券 */
    @JsonProperty("SHORT_BOND_PAYABLE") public BigDecimal shortBondPayable;
    /** 应付短期融资款 */
    @JsonProperty("SHORT_FIN_PAYABLE") public BigDecimal shortFinPayable;
    /** 短期借款 */
    @JsonProperty("SHORT_LOAN") public BigDecimal shortLoan;
    /** 专项应付款 */
    @JsonProperty("SPECIAL_PAYABLE") public BigDecimal specialPayable;
    /** 专项储备 */
    @JsonProperty("SPECIAL_RESERVE") public BigDecimal specialReserve;
    /** 应付职工薪酬 */
    @JsonProperty("STAFF_SALARY_PAYABLE") public BigDecimal staffSalaryPayable;
    /** 应收补贴款 */
    @JsonProperty("SUBSIDY_RECE") public BigDecimal subsidyRece;
    /** 盈余公积 */
    @JsonProperty("SURPLUS_RESERVE") public BigDecimal surplusReserve;
    /** 应交税费 */
    @JsonProperty("TAX_PAYABLE") public BigDecimal taxPayable;
    /** 资产总计 */
    @JsonProperty("TOTAL_ASSETS") public BigDecimal totalAssets;
    /** 流动资产合计 */
    @JsonProperty("TOTAL_CURRENT_ASSETS") public BigDecimal totalCurrentAssets;
    /** 流动负债合计 */
    @JsonProperty("TOTAL_CURRENT_LIAB") public BigDecimal totalCurrentLiab;
    /** 股东权益合计 */
    @JsonProperty("TOTAL_EQUITY") public BigDecimal totalEquity;
    /** 负债及股东权益总计 */
    @JsonProperty("TOTAL_LIAB_EQUITY") public BigDecimal totalLiabEquity;
    /** 负债合计 */
    @JsonProperty("TOTAL_LIABILITIES") public BigDecimal totalLiabilities;
    /** 非流动资产合计 */
    @JsonProperty("TOTAL_NONCURRENT_ASSETS") public BigDecimal totalNoncurrentAssets;
    /** 非流动负债合计 */
    @JsonProperty("TOTAL_NONCURRENT_LIAB") public BigDecimal totalNoncurrentLiab;
    /** 其他应付款合计 */
    @JsonProperty("TOTAL_OTHER_PAYABLE") public BigDecimal totalOtherPayable;
    /** 其他应收款合计 */
    @JsonProperty("TOTAL_OTHER_RECE") public BigDecimal totalOtherRece;
    /** 归属于母公司股东权益合计 */
    @JsonProperty("TOTAL_PARENT_EQUITY") public BigDecimal totalParentEquity;
    /** 交易性金融资产 */
    @JsonProperty("TRADE_FINASSET") public BigDecimal tradeFinasset;
    /** 交易性金融资产（非 FVTPL 分类项） */
    @JsonProperty("TRADE_FINASSET_NOTFVTPL") public BigDecimal tradeFinassetNotfvtpl;
    /** 交易性金融负债 */
    @JsonProperty("TRADE_FINLIAB") public BigDecimal tradeFinliab;
    /** 交易性金融负债（非 FVTPL 分类项） */
    @JsonProperty("TRADE_FINLIAB_NOTFVTPL") public BigDecimal tradeFinliabNotfvtpl;
    /** 库存股 */
    @JsonProperty("TREASURY_SHARES") public BigDecimal treasuryShares;
    /** 未分配利润 */
    @JsonProperty("UNASSIGN_RPOFIT") public BigDecimal unassignRpofit;
    /** 未确认投资损失 */
    @JsonProperty("UNCONFIRM_INVEST_LOSS") public BigDecimal unconfirmInvestLoss;
    /** 使用权资产 */
    @JsonProperty("USERIGHT_ASSET") public BigDecimal userightAsset;
    /** 吸收存款及同业存放，同比（%） */
    @JsonProperty("ACCEPT_DEPOSIT_INTERBANK_YOY") public BigDecimal acceptDepositInterbankYoy;
    /** 应付账款，同比（%） */
    @JsonProperty("ACCOUNTS_PAYABLE_YOY") public BigDecimal accountsPayableYoy;
    /** 应收账款，同比（%） */
    @JsonProperty("ACCOUNTS_RECE_YOY") public BigDecimal accountsReceYoy;
    /** 预提费用，同比（%） */
    @JsonProperty("ACCRUED_EXPENSE_YOY") public BigDecimal accruedExpenseYoy;
    /** 预收款项，同比（%） */
    @JsonProperty("ADVANCE_RECEIVABLES_YOY") public BigDecimal advanceReceivablesYoy;
    /** 代理买卖证券款，同比（%） */
    @JsonProperty("AGENT_TRADE_SECURITY_YOY") public BigDecimal agentTradeSecurityYoy;
    /** 代理承销证券款，同比（%） */
    @JsonProperty("AGENT_UNDERWRITE_SECURITY_YOY") public BigDecimal agentUnderwriteSecurityYoy;
    /** 以摊余成本计量的金融资产，同比（%） */
    @JsonProperty("AMORTIZE_COST_FINASSET_YOY") public BigDecimal amortizeCostFinassetYoy;
    /** 以摊余成本计量的金融负债，同比（%） */
    @JsonProperty("AMORTIZE_COST_FINLIAB_YOY") public BigDecimal amortizeCostFinliabYoy;
    /** 以摊余成本计量的非流动金融资产，同比（%） */
    @JsonProperty("AMORTIZE_COST_NCFINASSET_YOY") public BigDecimal amortizeCostNcfinassetYoy;
    /** 以摊余成本计量的非流动金融负债，同比（%） */
    @JsonProperty("AMORTIZE_COST_NCFINLIAB_YOY") public BigDecimal amortizeCostNcfinliabYoy;
    /** 指定以公允价值计量且其变动计入当期损益的金融资产，同比（%） */
    @JsonProperty("APPOINT_FVTPL_FINASSET_YOY") public BigDecimal appointFvtplFinassetYoy;
    /** 指定以公允价值计量且其变动计入当期损益的金融负债，同比（%） */
    @JsonProperty("APPOINT_FVTPL_FINLIAB_YOY") public BigDecimal appointFvtplFinliabYoy;
    /** 资产平衡项目，同比（%） */
    @JsonProperty("ASSET_BALANCE_YOY") public BigDecimal assetBalanceYoy;
    /** 资产其他项目，同比（%） */
    @JsonProperty("ASSET_OTHER_YOY") public BigDecimal assetOtherYoy;
    /** 分配股利、利润或偿付利息支付的现金（表内科目），同比（%） */
    @JsonProperty("ASSIGN_CASH_DIVIDEND_YOY") public BigDecimal assignCashDividendYoy;
    /** 可供出售金融资产，同比（%） */
    @JsonProperty("AVAILABLE_SALE_FINASSET_YOY") public BigDecimal availableSaleFinassetYoy;
    /** 应付债券，同比（%） */
    @JsonProperty("BOND_PAYABLE_YOY") public BigDecimal bondPayableYoy;
    /** 拆入资金，同比（%） */
    @JsonProperty("BORROW_FUND_YOY") public BigDecimal borrowFundYoy;
    /** 买入返售金融资产，同比（%） */
    @JsonProperty("BUY_RESALE_FINASSET_YOY") public BigDecimal buyResaleFinassetYoy;
    /** 资本公积，同比（%） */
    @JsonProperty("CAPITAL_RESERVE_YOY") public BigDecimal capitalReserveYoy;
    /** 在建工程，同比（%） */
    @JsonProperty("CIP_YOY") public BigDecimal cipYoy;
    /** 消耗性生物资产，同比（%） */
    @JsonProperty("CONSUMPTIVE_BIOLOGICAL_ASSET_YOY") public BigDecimal consumptiveBiologicalAssetYoy;
    /** 合同资产，同比（%） */
    @JsonProperty("CONTRACT_ASSET_YOY") public BigDecimal contractAssetYoy;
    /** 合同负债，同比（%） */
    @JsonProperty("CONTRACT_LIAB_YOY") public BigDecimal contractLiabYoy;
    /** 外币报表折算差额，同比（%） */
    @JsonProperty("CONVERT_DIFF_YOY") public BigDecimal convertDiffYoy;
    /** 债权投资，同比（%） */
    @JsonProperty("CREDITOR_INVEST_YOY") public BigDecimal creditorInvestYoy;
    /** 流动资产平衡项目，同比（%） */
    @JsonProperty("CURRENT_ASSET_BALANCE_YOY") public BigDecimal currentAssetBalanceYoy;
    /** 流动资产其他项目，同比（%） */
    @JsonProperty("CURRENT_ASSET_OTHER_YOY") public BigDecimal currentAssetOtherYoy;
    /** 流动负债平衡项目，同比（%） */
    @JsonProperty("CURRENT_LIAB_BALANCE_YOY") public BigDecimal currentLiabBalanceYoy;
    /** 流动负债其他项目，同比（%） */
    @JsonProperty("CURRENT_LIAB_OTHER_YOY") public BigDecimal currentLiabOtherYoy;
    /** 一年内到期的递延收益，同比（%） */
    @JsonProperty("DEFER_INCOME_1YEAR_YOY") public BigDecimal deferIncome1yearYoy;
    /** 递延收益，同比（%） */
    @JsonProperty("DEFER_INCOME_YOY") public BigDecimal deferIncomeYoy;
    /** 递延所得税资产，同比（%） */
    @JsonProperty("DEFER_TAX_ASSET_YOY") public BigDecimal deferTaxAssetYoy;
    /** 递延所得税负债，同比（%） */
    @JsonProperty("DEFER_TAX_LIAB_YOY") public BigDecimal deferTaxLiabYoy;
    /** 衍生金融资产，同比（%） */
    @JsonProperty("DERIVE_FINASSET_YOY") public BigDecimal deriveFinassetYoy;
    /** 衍生金融负债，同比（%） */
    @JsonProperty("DERIVE_FINLIAB_YOY") public BigDecimal deriveFinliabYoy;
    /** 开发支出，同比（%） */
    @JsonProperty("DEVELOP_EXPENSE_YOY") public BigDecimal developExpenseYoy;
    /** 持有待售资产，同比（%） */
    @JsonProperty("DIV_HOLDSALE_ASSET_YOY") public BigDecimal divHoldsaleAssetYoy;
    /** 持有待售负债，同比（%） */
    @JsonProperty("DIV_HOLDSALE_LIAB_YOY") public BigDecimal divHoldsaleLiabYoy;
    /** 应付股利，同比（%） */
    @JsonProperty("DIVIDEND_PAYABLE_YOY") public BigDecimal dividendPayableYoy;
    /** 应收股利，同比（%） */
    @JsonProperty("DIVIDEND_RECE_YOY") public BigDecimal dividendReceYoy;
    /** 所有者权益平衡项目，同比（%） */
    @JsonProperty("EQUITY_BALANCE_YOY") public BigDecimal equityBalanceYoy;
    /** 所有者权益其他项目，同比（%） */
    @JsonProperty("EQUITY_OTHER_YOY") public BigDecimal equityOtherYoy;
    /** 应收出口退税，同比（%） */
    @JsonProperty("EXPORT_REFUND_RECE_YOY") public BigDecimal exportRefundReceYoy;
    /** 应付手续费及佣金，同比（%） */
    @JsonProperty("FEE_COMMISSION_PAYABLE_YOY") public BigDecimal feeCommissionPayableYoy;
    /** 融出资金，同比（%） */
    @JsonProperty("FIN_FUND_YOY") public BigDecimal finFundYoy;
    /** 应收款项融资，同比（%） */
    @JsonProperty("FINANCE_RECE_YOY") public BigDecimal financeReceYoy;
    /** 固定资产清理，同比（%） */
    @JsonProperty("FIXED_ASSET_DISPOSAL_YOY") public BigDecimal fixedAssetDisposalYoy;
    /** 固定资产，同比（%） */
    @JsonProperty("FIXED_ASSET_YOY") public BigDecimal fixedAssetYoy;
    /** 以公允价值计量且其变动计入其他综合收益的金融资产，同比（%） */
    @JsonProperty("FVTOCI_FINASSET_YOY") public BigDecimal fvtociFinassetYoy;
    /** 以公允价值计量且其变动计入其他综合收益的非流动金融资产，同比（%） */
    @JsonProperty("FVTOCI_NCFINASSET_YOY") public BigDecimal fvtociNcfinassetYoy;
    /** 以公允价值计量且其变动计入当期损益的金融资产，同比（%） */
    @JsonProperty("FVTPL_FINASSET_YOY") public BigDecimal fvtplFinassetYoy;
    /** 以公允价值计量且其变动计入当期损益的金融负债，同比（%） */
    @JsonProperty("FVTPL_FINLIAB_YOY") public BigDecimal fvtplFinliabYoy;
    /** 一般风险准备，同比（%） */
    @JsonProperty("GENERAL_RISK_RESERVE_YOY") public BigDecimal generalRiskReserveYoy;
    /** 商誉，同比（%） */
    @JsonProperty("GOODWILL_YOY") public BigDecimal goodwillYoy;
    /** 持有至到期投资，同比（%） */
    @JsonProperty("HOLD_MATURITY_INVEST_YOY") public BigDecimal holdMaturityInvestYoy;
    /** 划分为持有待售的资产，同比（%） */
    @JsonProperty("HOLDSALE_ASSET_YOY") public BigDecimal holdsaleAssetYoy;
    /** 划分为持有待售的负债，同比（%） */
    @JsonProperty("HOLDSALE_LIAB_YOY") public BigDecimal holdsaleLiabYoy;
    /** 保险合同准备金，同比（%） */
    @JsonProperty("INSURANCE_CONTRACT_RESERVE_YOY") public BigDecimal insuranceContractReserveYoy;
    /** 无形资产，同比（%） */
    @JsonProperty("INTANGIBLE_ASSET_YOY") public BigDecimal intangibleAssetYoy;
    /** 应付利息，同比（%） */
    @JsonProperty("INTEREST_PAYABLE_YOY") public BigDecimal interestPayableYoy;
    /** 应收利息，同比（%） */
    @JsonProperty("INTEREST_RECE_YOY") public BigDecimal interestReceYoy;
    /** 内部应付款，同比（%） */
    @JsonProperty("INTERNAL_PAYABLE_YOY") public BigDecimal internalPayableYoy;
    /** 内部应收款，同比（%） */
    @JsonProperty("INTERNAL_RECE_YOY") public BigDecimal internalReceYoy;
    /** 存货，同比（%） */
    @JsonProperty("INVENTORY_YOY") public BigDecimal inventoryYoy;
    /** 投资性房地产，同比（%） */
    @JsonProperty("INVEST_REALESTATE_YOY") public BigDecimal investRealestateYoy;
    /** 租赁负债，同比（%） */
    @JsonProperty("LEASE_LIAB_YOY") public BigDecimal leaseLiabYoy;
    /** 融出资金（同业），同比（%） */
    @JsonProperty("LEND_FUND_YOY") public BigDecimal lendFundYoy;
    /** 负债平衡项目，同比（%） */
    @JsonProperty("LIAB_BALANCE_YOY") public BigDecimal liabBalanceYoy;
    /** 负债及股东权益平衡项目，同比（%） */
    @JsonProperty("LIAB_EQUITY_BALANCE_YOY") public BigDecimal liabEquityBalanceYoy;
    /** 负债及股东权益其他项目，同比（%） */
    @JsonProperty("LIAB_EQUITY_OTHER_YOY") public BigDecimal liabEquityOtherYoy;
    /** 负债其他项目，同比（%） */
    @JsonProperty("LIAB_OTHER_YOY") public BigDecimal liabOtherYoy;
    /** 发放贷款及垫款，同比（%） */
    @JsonProperty("LOAN_ADVANCE_YOY") public BigDecimal loanAdvanceYoy;
    /** 向中央银行借款，同比（%） */
    @JsonProperty("LOAN_PBC_YOY") public BigDecimal loanPbcYoy;
    /** 长期股权投资，同比（%） */
    @JsonProperty("LONG_EQUITY_INVEST_YOY") public BigDecimal longEquityInvestYoy;
    /** 长期借款，同比（%） */
    @JsonProperty("LONG_LOAN_YOY") public BigDecimal longLoanYoy;
    /** 长期应付款，同比（%） */
    @JsonProperty("LONG_PAYABLE_YOY") public BigDecimal longPayableYoy;
    /** 长期待摊费用，同比（%） */
    @JsonProperty("LONG_PREPAID_EXPENSE_YOY") public BigDecimal longPrepaidExpenseYoy;
    /** 长期应收款，同比（%） */
    @JsonProperty("LONG_RECE_YOY") public BigDecimal longReceYoy;
    /** 长期应付职工薪酬，同比（%） */
    @JsonProperty("LONG_STAFFSALARY_PAYABLE_YOY") public BigDecimal longStaffsalaryPayableYoy;
    /** 少数股东权益，同比（%） */
    @JsonProperty("MINORITY_EQUITY_YOY") public BigDecimal minorityEquityYoy;
    /** 货币资金，同比（%） */
    @JsonProperty("MONETARYFUNDS_YOY") public BigDecimal monetaryfundsYoy;
    /** 一年内到期的非流动资产，同比（%） */
    @JsonProperty("NONCURRENT_ASSET_1YEAR_YOY") public BigDecimal noncurrentAsset1yearYoy;
    /** 非流动资产平衡项目，同比（%） */
    @JsonProperty("NONCURRENT_ASSET_BALANCE_YOY") public BigDecimal noncurrentAssetBalanceYoy;
    /** 非流动资产其他项目，同比（%） */
    @JsonProperty("NONCURRENT_ASSET_OTHER_YOY") public BigDecimal noncurrentAssetOtherYoy;
    /** 一年内到期的非流动负债，同比（%） */
    @JsonProperty("NONCURRENT_LIAB_1YEAR_YOY") public BigDecimal noncurrentLiab1yearYoy;
    /** 非流动负债平衡项目，同比（%） */
    @JsonProperty("NONCURRENT_LIAB_BALANCE_YOY") public BigDecimal noncurrentLiabBalanceYoy;
    /** 非流动负债其他项目，同比（%） */
    @JsonProperty("NONCURRENT_LIAB_OTHER_YOY") public BigDecimal noncurrentLiabOtherYoy;
    /** 应付票据及应付账款，同比（%） */
    @JsonProperty("NOTE_ACCOUNTS_PAYABLE_YOY") public BigDecimal noteAccountsPayableYoy;
    /** 应收票据及应收账款，同比（%） */
    @JsonProperty("NOTE_ACCOUNTS_RECE_YOY") public BigDecimal noteAccountsReceYoy;
    /** 应付票据，同比（%） */
    @JsonProperty("NOTE_PAYABLE_YOY") public BigDecimal notePayableYoy;
    /** 应收票据，同比（%） */
    @JsonProperty("NOTE_RECE_YOY") public BigDecimal noteReceYoy;
    /** 油气资产，同比（%） */
    @JsonProperty("OIL_GAS_ASSET_YOY") public BigDecimal oilGasAssetYoy;
    /** 其他综合收益，同比（%） */
    @JsonProperty("OTHER_COMPRE_INCOME_YOY") public BigDecimal otherCompreIncomeYoy;
    /** 其他债权投资，同比（%） */
    @JsonProperty("OTHER_CREDITOR_INVEST_YOY") public BigDecimal otherCreditorInvestYoy;
    /** 其他流动资产，同比（%） */
    @JsonProperty("OTHER_CURRENT_ASSET_YOY") public BigDecimal otherCurrentAssetYoy;
    /** 其他流动负债，同比（%） */
    @JsonProperty("OTHER_CURRENT_LIAB_YOY") public BigDecimal otherCurrentLiabYoy;
    /** 其他权益工具投资，同比（%） */
    @JsonProperty("OTHER_EQUITY_INVEST_YOY") public BigDecimal otherEquityInvestYoy;
    /** 其他权益其他项目，同比（%） */
    @JsonProperty("OTHER_EQUITY_OTHER_YOY") public BigDecimal otherEquityOtherYoy;
    /** 其他权益工具，同比（%） */
    @JsonProperty("OTHER_EQUITY_TOOL_YOY") public BigDecimal otherEquityToolYoy;
    /** 其他非流动资产，同比（%） */
    @JsonProperty("OTHER_NONCURRENT_ASSET_YOY") public BigDecimal otherNoncurrentAssetYoy;
    /** 其他非流动金融资产，同比（%） */
    @JsonProperty("OTHER_NONCURRENT_FINASSET_YOY") public BigDecimal otherNoncurrentFinassetYoy;
    /** 其他非流动负债，同比（%） */
    @JsonProperty("OTHER_NONCURRENT_LIAB_YOY") public BigDecimal otherNoncurrentLiabYoy;
    /** 其他应付款，同比（%） */
    @JsonProperty("OTHER_PAYABLE_YOY") public BigDecimal otherPayableYoy;
    /** 其他应收款，同比（%） */
    @JsonProperty("OTHER_RECE_YOY") public BigDecimal otherReceYoy;
    /** 归属于母公司股东权益平衡项目，同比（%） */
    @JsonProperty("PARENT_EQUITY_BALANCE_YOY") public BigDecimal parentEquityBalanceYoy;
    /** 归属于母公司股东权益其他项目，同比（%） */
    @JsonProperty("PARENT_EQUITY_OTHER_YOY") public BigDecimal parentEquityOtherYoy;
    /** 应付永续债，同比（%） */
    @JsonProperty("PERPETUAL_BOND_PAYBALE_YOY") public BigDecimal perpetualBondPaybaleYoy;
    /** 永续债（权益端），同比（%） */
    @JsonProperty("PERPETUAL_BOND_YOY") public BigDecimal perpetualBondYoy;
    /** 预计流动负债，同比（%） */
    @JsonProperty("PREDICT_CURRENT_LIAB_YOY") public BigDecimal predictCurrentLiabYoy;
    /** 预计负债，同比（%） */
    @JsonProperty("PREDICT_LIAB_YOY") public BigDecimal predictLiabYoy;
    /** 应付优先股，同比（%） */
    @JsonProperty("PREFERRED_SHARES_PAYBALE_YOY") public BigDecimal preferredSharesPaybaleYoy;
    /** 优先股（权益端），同比（%） */
    @JsonProperty("PREFERRED_SHARES_YOY") public BigDecimal preferredSharesYoy;
    /** 应收保费，同比（%） */
    @JsonProperty("PREMIUM_RECE_YOY") public BigDecimal premiumReceYoy;
    /** 预付款项，同比（%） */
    @JsonProperty("PREPAYMENT_YOY") public BigDecimal prepaymentYoy;
    /** 生产性生物资产，同比（%） */
    @JsonProperty("PRODUCTIVE_BIOLOGY_ASSET_YOY") public BigDecimal productiveBiologyAssetYoy;
    /** 工程物资，同比（%） */
    @JsonProperty("PROJECT_MATERIAL_YOY") public BigDecimal projectMaterialYoy;
    /** 应收分保合同准备金，同比（%） */
    @JsonProperty("RC_RESERVE_RECE_YOY") public BigDecimal rcReserveReceYoy;
    /** 应付分保账款，同比（%） */
    @JsonProperty("REINSURE_PAYABLE_YOY") public BigDecimal reinsurePayableYoy;
    /** 应收分保账款，同比（%） */
    @JsonProperty("REINSURE_RECE_YOY") public BigDecimal reinsureReceYoy;
    /** 卖出回购金融资产款，同比（%） */
    @JsonProperty("SELL_REPO_FINASSET_YOY") public BigDecimal sellRepoFinassetYoy;
    /** 结算备付金，同比（%） */
    @JsonProperty("SETTLE_EXCESS_RESERVE_YOY") public BigDecimal settleExcessReserveYoy;
    /** 股本，同比（%） */
    @JsonProperty("SHARE_CAPITAL_YOY") public BigDecimal shareCapitalYoy;
    /** 应付短期债券，同比（%） */
    @JsonProperty("SHORT_BOND_PAYABLE_YOY") public BigDecimal shortBondPayableYoy;
    /** 应付短期融资款，同比（%） */
    @JsonProperty("SHORT_FIN_PAYABLE_YOY") public BigDecimal shortFinPayableYoy;
    /** 短期借款，同比（%） */
    @JsonProperty("SHORT_LOAN_YOY") public BigDecimal shortLoanYoy;
    /** 专项应付款，同比（%） */
    @JsonProperty("SPECIAL_PAYABLE_YOY") public BigDecimal specialPayableYoy;
    /** 专项储备，同比（%） */
    @JsonProperty("SPECIAL_RESERVE_YOY") public BigDecimal specialReserveYoy;
    /** 应付职工薪酬，同比（%） */
    @JsonProperty("STAFF_SALARY_PAYABLE_YOY") public BigDecimal staffSalaryPayableYoy;
    /** 应收补贴款，同比（%） */
    @JsonProperty("SUBSIDY_RECE_YOY") public BigDecimal subsidyReceYoy;
    /** 盈余公积，同比（%） */
    @JsonProperty("SURPLUS_RESERVE_YOY") public BigDecimal surplusReserveYoy;
    /** 应交税费，同比（%） */
    @JsonProperty("TAX_PAYABLE_YOY") public BigDecimal taxPayableYoy;
    /** 资产总计，同比（%） */
    @JsonProperty("TOTAL_ASSETS_YOY") public BigDecimal totalAssetsYoy;
    /** 流动资产合计，同比（%） */
    @JsonProperty("TOTAL_CURRENT_ASSETS_YOY") public BigDecimal totalCurrentAssetsYoy;
    /** 流动负债合计，同比（%） */
    @JsonProperty("TOTAL_CURRENT_LIAB_YOY") public BigDecimal totalCurrentLiabYoy;
    /** 股东权益合计，同比（%） */
    @JsonProperty("TOTAL_EQUITY_YOY") public BigDecimal totalEquityYoy;
    /** 负债及股东权益总计，同比（%） */
    @JsonProperty("TOTAL_LIAB_EQUITY_YOY") public BigDecimal totalLiabEquityYoy;
    /** 负债合计，同比（%） */
    @JsonProperty("TOTAL_LIABILITIES_YOY") public BigDecimal totalLiabilitiesYoy;
    /** 非流动资产合计，同比（%） */
    @JsonProperty("TOTAL_NONCURRENT_ASSETS_YOY") public BigDecimal totalNoncurrentAssetsYoy;
    /** 非流动负债合计，同比（%） */
    @JsonProperty("TOTAL_NONCURRENT_LIAB_YOY") public BigDecimal totalNoncurrentLiabYoy;
    /** 其他应付款合计，同比（%） */
    @JsonProperty("TOTAL_OTHER_PAYABLE_YOY") public BigDecimal totalOtherPayableYoy;
    /** 其他应收款合计，同比（%） */
    @JsonProperty("TOTAL_OTHER_RECE_YOY") public BigDecimal totalOtherReceYoy;
    /** 归属于母公司股东权益合计，同比（%） */
    @JsonProperty("TOTAL_PARENT_EQUITY_YOY") public BigDecimal totalParentEquityYoy;
    /** 交易性金融资产（非 FVTPL 分类项），同比（%） */
    @JsonProperty("TRADE_FINASSET_NOTFVTPL_YOY") public BigDecimal tradeFinassetNotfvtplYoy;
    /** 交易性金融资产，同比（%） */
    @JsonProperty("TRADE_FINASSET_YOY") public BigDecimal tradeFinassetYoy;
    /** 交易性金融负债（非 FVTPL 分类项），同比（%） */
    @JsonProperty("TRADE_FINLIAB_NOTFVTPL_YOY") public BigDecimal tradeFinliabNotfvtplYoy;
    /** 交易性金融负债，同比（%） */
    @JsonProperty("TRADE_FINLIAB_YOY") public BigDecimal tradeFinliabYoy;
    /** 库存股，同比（%） */
    @JsonProperty("TREASURY_SHARES_YOY") public BigDecimal treasurySharesYoy;
    /** 未分配利润，同比（%） */
    @JsonProperty("UNASSIGN_RPOFIT_YOY") public BigDecimal unassignRpofitYoy;
    /** 未确认投资损失，同比（%） */
    @JsonProperty("UNCONFIRM_INVEST_LOSS_YOY") public BigDecimal unconfirmInvestLossYoy;
    /** 使用权资产，同比（%） */
    @JsonProperty("USERIGHT_ASSET_YOY") public BigDecimal userightAssetYoy;
    /** 审计意见类型 */
    @JsonProperty("OPINION_TYPE") public String opinionType;
    /** 其他审计意见类型 */
    @JsonProperty("OSOPINION_TYPE") public String osopinionType;
    /** 上市状态标识 */
    @JsonProperty("LISTING_STATE") public String listingState;
}
