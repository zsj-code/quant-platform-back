package com.quant.platform.business.trader.service.impl;

import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 *
 */
@Component
public class TraderDecisionRunPdfRenderer {

    /**
     * 将 {@code llm_summary_text}（Markdown）转为 PDF 字节流；正文仅来自该字段，不包含工作流步骤。
     */
    public byte[] renderLlmSummaryPdf(String llmSummaryMarkdown) throws IOException {
        String markdown = llmSummaryMarkdown == null ? "" : llmSummaryMarkdown.trim();
        return null;
    }

}
