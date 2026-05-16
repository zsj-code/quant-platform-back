package com.quant.platform.business.trader.pdf;

import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.font.FontProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * PDF 字体：仅使用 {@code classpath:fonts/} 下已下载的 Noto 文件，不做 emoji 转文字。
 * <ul>
 *   <li>{@link #NOTO_SANS_SC_FILE} → CSS {@code 'Noto Sans SC'}（中文）</li>
 *   <li>{@link #NOTO_EMOJI_FILE} → CSS {@code 'Noto Emoji'}（emoji，原样输出）</li>
 * </ul>
 * 均通过 {@link PdfEncodings#IDENTITY_H} 注册；缺失时见 {@link #buildFontContext()} 提示。
 */
@Component
public class TraderDecisionPdfFontProvider {

    /** 简体中文（notofonts/noto-cjk） */
    public static final String NOTO_SANS_SC_FILE = "NotoSansCJKsc-Regular.otf";
    /** 单色 Emoji（googlefonts/noto-emoji v2.028，v2.047+ 已移除该文件） */
    public static final String NOTO_EMOJI_FILE = "NotoEmoji-Regular.ttf";

    private static final String CLASSPATH_SANS_SC = "fonts/" + NOTO_SANS_SC_FILE;
    private static final String CLASSPATH_EMOJI = "fonts/" + NOTO_EMOJI_FILE;

    /** 完整 OTF 约 16MB，过小多为下载中断或 HTML 错误页 */
    private static final long MIN_SANS_SC_BYTES = 10L * 1024 * 1024;
    private static final long MIN_EMOJI_BYTES = 300_000L;

    /** 写入 HTML {@code font-family}，顺序即回退顺序 */
    public static final String CSS_FONT_FAMILY = "'Noto Sans SC', 'Noto Emoji'";

    private volatile PdfFontContext cached;

    public PdfFontContext getFontContext() throws IOException {
        PdfFontContext ctx = cached;
        if (ctx != null) {
            return ctx;
        }
        synchronized (this) {
            if (cached != null) {
                return cached;
            }
            cached = buildFontContext();
            return cached;
        }
    }

    private static PdfFontContext buildFontContext() throws IOException {
        ClassPathResource sansSc = new ClassPathResource(CLASSPATH_SANS_SC);
        ClassPathResource emoji = new ClassPathResource(CLASSPATH_EMOJI);
        if (!sansSc.exists() || !emoji.exists()) {
            throw new IllegalStateException("""
                PDF 字体文件缺失，请确认 classpath 下存在：
                  - src/main/resources/fonts/%s
                  - src/main/resources/fonts/%s
                可执行：scripts/download-trader-decision-pdf-font.ps1
                """.formatted(NOTO_SANS_SC_FILE, NOTO_EMOJI_FILE));
        }

        validateFontFile(sansSc, MIN_SANS_SC_BYTES);
        validateFontFile(emoji, MIN_EMOJI_BYTES);

        FontProvider fontProvider = new DefaultFontProvider(false, false, false);
        registerFont(fontProvider, sansSc, NOTO_SANS_SC_FILE);
        registerFont(fontProvider, emoji, NOTO_EMOJI_FILE);

        String baseUri = resolveFontsBaseUri(sansSc);
        String fontFaceCss = """
            @font-face {
              font-family: 'Noto Sans SC';
              src: url('%s') format('opentype');
              font-weight: normal;
              font-style: normal;
            }
            @font-face {
              font-family: 'Noto Emoji';
              src: url('%s') format('truetype');
              font-weight: normal;
              font-style: normal;
            }
            """.formatted(NOTO_SANS_SC_FILE, NOTO_EMOJI_FILE);

        return new PdfFontContext(fontProvider, CSS_FONT_FAMILY, fontFaceCss, baseUri);
    }

    private static void validateFontFile(ClassPathResource resource, long minBytes) throws IOException {
        long size = resource.contentLength();
        if (size >= 0 && size < minBytes) {
            throw new IllegalStateException("""
                字体文件不完整（%s，%d 字节，至少需要约 %d 字节）。请删除后重新执行：
                scripts/download-trader-decision-pdf-font.ps1
                """.formatted(resource.getFilename(), size, minBytes));
        }
    }

    private static void registerFont(FontProvider fontProvider, ClassPathResource resource, String label)
        throws IOException {
        byte[] bytes;
        try (InputStream in = resource.getInputStream()) {
            bytes = in.readAllBytes();
        }
        try {
            FontProgram program = FontProgramFactory.createFont(bytes);
            fontProvider.addFont(program, PdfEncodings.IDENTITY_H);
        } catch (com.itextpdf.io.exceptions.IOException ex) {
            throw new IllegalStateException("无法加载字体 " + label + "（" + resource.getFilename() + "，"
                + bytes.length + " 字节），请重新下载完整文件: " + ex.getMessage(), ex);
        }
    }

    private static String resolveFontsBaseUri(ClassPathResource anyFontInDir) throws IOException {
        URL fontUrl = anyFontInDir.getURL();
        String baseUri = fontUrl.toExternalForm();
        int slash = baseUri.lastIndexOf('/');
        if (slash >= 0) {
            baseUri = baseUri.substring(0, slash + 1);
        }
        return baseUri;
    }

    /**
     * @param cssFontFamily 写入 HTML {@code font-family}
     * @param fontFaceCss   {@code @font-face} 块（配合 {@link #baseUri()}）
     * @param baseUri       html2pdf 字体目录基准 URL（以 {@code /} 结尾）
     */
    public record PdfFontContext(FontProvider fontProvider, String cssFontFamily, String fontFaceCss, String baseUri) {
    }
}
