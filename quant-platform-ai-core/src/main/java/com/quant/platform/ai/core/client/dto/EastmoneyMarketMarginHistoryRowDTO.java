package com.quant.platform.ai.core.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 东财 {@code RPTA_RZRQ_LSHJ} 全市场融资融券历史汇总单行（与接口 {@code data} 中单条 JSON 字段对应）。
 */
public record EastmoneyMarketMarginHistoryRowDTO(
        /** 统计日期（接口原样，常含 {@code 00:00:00}） */
        @JsonProperty("DIM_DATE") String dimDate,
        /** 接口字段 {@code NEW}（综合类数值指标，具体含义以东财报表为准） */
        @JsonProperty("NEW") BigDecimal newField,
        /** 涨跌幅（%） */
        @JsonProperty("ZDF") BigDecimal zdf,
        /** 流通市值（元） */
        @JsonProperty("LTSZ") BigDecimal ltsz,
        /** 3 日涨跌幅（%） */
        @JsonProperty("ZDF3D") BigDecimal zdf3d,
        /** 5 日涨跌幅（%） */
        @JsonProperty("ZDF5D") BigDecimal zdf5d,
        /** 10 日涨跌幅（%） */
        @JsonProperty("ZDF10D") BigDecimal zdf10d,
        /** 融资余额（元） */
        @JsonProperty("RZYE") Long rzye,
        /** 融资余额占流通市值比（%） */
        @JsonProperty("RZYEZB") BigDecimal rzyezb,
        /** 融资买入额（元） */
        @JsonProperty("RZMRE") Long rzmre,
        /** 近 3 日融资买入额（元） */
        @JsonProperty("RZMRE3D") Long rzmre3d,
        /** 近 5 日融资买入额（元） */
        @JsonProperty("RZMRE5D") Long rzmre5d,
        /** 近 10 日融资买入额（元） */
        @JsonProperty("RZMRE10D") Long rzmre10d,
        /** 融资偿还额（元） */
        @JsonProperty("RZCHE") Long rzche,
        /** 近 3 日融资偿还额（元） */
        @JsonProperty("RZCHE3D") Long rzche3d,
        /** 近 5 日融资偿还额（元） */
        @JsonProperty("RZCHE5D") Long rzche5d,
        /** 近 10 日融资偿还额（元） */
        @JsonProperty("RZCHE10D") Long rzche10d,
        /** 融资净买入额（元），可为负 */
        @JsonProperty("RZJME") Long rzjm,
        /** 近 3 日融资净买入额（元） */
        @JsonProperty("RZJME3D") Long rzjm3d,
        /** 近 5 日融资净买入额（元） */
        @JsonProperty("RZJME5D") Long rzjm5d,
        /** 近 10 日融资净买入额（元） */
        @JsonProperty("RZJME10D") Long rzjm10d,
        /** 融券余额（元） */
        @JsonProperty("RQYE") Long rqye,
        /** 融券余量（股/份） */
        @JsonProperty("RQYL") Long rqyl,
        /** 融券偿还量 */
        @JsonProperty("RQCHL") Long rqchl,
        /** 近 3 日融券偿还量 */
        @JsonProperty("RQCHL3D") Long rqchl3d,
        /** 近 5 日融券偿还量 */
        @JsonProperty("RQCHL5D") Long rqchl5d,
        /** 近 10 日融券偿还量 */
        @JsonProperty("RQCHL10D") Long rqchl10d,
        /** 融券卖出量 */
        @JsonProperty("RQMCL") Long rqmcl,
        /** 近 3 日融券卖出量 */
        @JsonProperty("RQMCL3D") Long rqmcl3d,
        /** 近 5 日融券卖出量 */
        @JsonProperty("RQMCL5D") Long rqmcl5d,
        /** 近 10 日融券卖出量 */
        @JsonProperty("RQMCL10D") Long rqmcl10d,
        /** 融券净卖出量（可为负） */
        @JsonProperty("RQJMG") Long rqjmg,
        /** 近 3 日融券净卖出量 */
        @JsonProperty("RQJMG3D") Long rqjmg3d,
        /** 近 5 日融券净卖出量 */
        @JsonProperty("RQJMG5D") Long rqjmg5d,
        /** 近 10 日融券净卖出量 */
        @JsonProperty("RQJMG10D") Long rqjmg10d,
        /** 融资融券余额合计（元） */
        @JsonProperty("RZRQYE") Long rzrqye,
        /** 融资融券余额差值（元） */
        @JsonProperty("RZRQYECZ") Long rzrqyecz) {
}
