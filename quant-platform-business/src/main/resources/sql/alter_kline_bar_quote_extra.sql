-- kline_bar：与 EastmoneyKlineBarDTO（东财 f57–f61）对齐的扩展列
-- 在目标库执行一次即可（列已存在则跳过或手工调整）。

ALTER TABLE kline_bar
    ADD COLUMN amount DECIMAL(20, 4) NULL COMMENT '成交额 f57' AFTER volume,
    ADD COLUMN amplitude DECIMAL(16, 6) NULL COMMENT '振幅% f58' AFTER amount,
    ADD COLUMN change_pct DECIMAL(16, 6) NULL COMMENT '涨跌幅% f59' AFTER amplitude,
    ADD COLUMN change_amount DECIMAL(20, 4) NULL COMMENT '涨跌额 f60' AFTER change_pct,
    ADD COLUMN turnover_rate DECIMAL(16, 6) NULL COMMENT '换手率% f61' AFTER change_amount;
