package com.quant.platform.business.trader.dto;

/**
 * 单次运行导出的 PDF 内容与建议文件名。
 */
public record TraderDecisionRunPdfExport(byte[] content, String fileName) {
}
