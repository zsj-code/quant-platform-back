package com.quant.platform.business.trader.service.impl;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.quant.platform.business.trader.pdf.TraderDecisionPdfFontProvider;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Markdown（{@code llm_summary_text}）→ HTML（Flexmark）→ PDF（iText html2pdf）。
 */
@Component
public class TraderDecisionRunPdfRenderer {

    private static final String EMPTY_PLACEHOLDER = "（暂无 LLM 摘要内容）";

    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;
    private final TraderDecisionPdfFontProvider fontProvider;

    public TraderDecisionRunPdfRenderer(TraderDecisionPdfFontProvider fontProvider) {
        this.fontProvider = fontProvider;
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create(), StrikethroughExtension.create(),
            AutolinkExtension.create(), GitLabExtension.create()));
        this.markdownParser = Parser.builder(options).build();
        this.htmlRenderer = HtmlRenderer.builder(options).build();
    }

    /**
     * 将 {@code llm_summary_text}（Markdown）转为 PDF；正文原样保留（含 emoji），字体见 {@link TraderDecisionPdfFontProvider}。
     */
    public byte[] renderLlmSummaryPdf(String llmSummaryMarkdown) throws IOException {
        String markdown = llmSummaryMarkdown == null ? "" : llmSummaryMarkdown.trim();
        if (markdown.isEmpty()) {
            markdown = EMPTY_PLACEHOLDER;
        }
        String bodyHtml = htmlRenderer.render(markdownParser.parse(markdown));
        TraderDecisionPdfFontProvider.PdfFontContext fonts = fontProvider.getFontContext();
        String fullHtml = wrapDocument(bodyHtml, fonts);
        ConverterProperties props = new ConverterProperties();
        props.setCharset(StandardCharsets.UTF_8.name());
        props.setFontProvider(fonts.fontProvider());
        if (fonts.baseUri() != null && !fonts.baseUri().isBlank()) {
            props.setBaseUri(fonts.baseUri());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(fullHtml, out, props);
        return out.toByteArray();
    }

    private static final String HTML_DOCUMENT_SHELL = """
        <!DOCTYPE html>
        <html lang="zh-CN">
        <head>
        <meta charset="UTF-8"/>
        <style>
        {{FONT_FACE_CSS}}
        html, body, table, th, td, p, li, blockquote, h1, h2, h3, h4, h5, h6 {
          font-family: {{CSS_FONT_FAMILY}};
        }
        body {
          font-size: 11pt;
          line-height: 1.55;
          color: #1a1a1a;
          margin: 0;
          padding: 0;
        }
        h1, h2, h3, h4, h5, h6 { margin: 1em 0 0.4em; font-weight: 600; }
        h1 { font-size: 18pt; }
        h2 { font-size: 15pt; }
        h3 { font-size: 13pt; }
        p { margin: 0.5em 0; }
        ul, ol { margin: 0.4em 0; padding-left: 1.6em; }
        blockquote {
          margin: 0.6em 0;
          padding: 0.2em 0.8em;
          border-left: 3px solid #ccc;
          color: #444;
        }
        table { border-collapse: collapse; width: 100%; margin: 0.6em 0; font-size: 10pt; }
        th, td { border: 1px solid #bbb; padding: 4px 8px; text-align: left; vertical-align: top; }
        th { background: #f0f0f0; }
        pre, code {
          font-family: {{CSS_FONT_FAMILY}};
          font-size: 9.5pt;
        }
        pre {
          background: #f5f5f5;
          padding: 8px 10px;
          white-space: pre-wrap;
          word-wrap: break-word;
          overflow-wrap: break-word;
        }
        code { background: #f5f5f5; padding: 1px 4px; }
        hr { border: none; border-top: 1px solid #ddd; margin: 1em 0; }
        a { color: #1565c0; text-decoration: none; }
        img { max-width: 100%; height: auto; }
        </style>
        </head>
        <body>
        {{BODY_HTML}}
        </body>
        </html>
        """;

    private static String wrapDocument(String bodyHtml, TraderDecisionPdfFontProvider.PdfFontContext fonts) {
        String safeFont = escapeCssFontFamily(fonts.cssFontFamily());
        String safeBody = bodyHtml == null ? "" : bodyHtml;
        String fontFace = fonts.fontFaceCss() == null ? "" : fonts.fontFaceCss();
        return HTML_DOCUMENT_SHELL.replace("{{FONT_FACE_CSS}}", fontFace).replace("{{CSS_FONT_FAMILY}}", safeFont)
            .replace("{{BODY_HTML}}", safeBody);
    }

    private static String escapeCssFontFamily(String cssFontFamily) {
        if (cssFontFamily == null || cssFontFamily.isBlank()) {
            return TraderDecisionPdfFontProvider.CSS_FONT_FAMILY;
        }
        return cssFontFamily.trim();
    }
}
