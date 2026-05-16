package com.quant.platform.business.trader;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * PDF 在浏览器内预览（{@code Content-Disposition: inline}）与下载（{@code attachment}）的公共封装。
 * <p>
 * 统一设置 {@code application/pdf}，并对文件名做简单净化、使用 {@link ContentDisposition} 生成 {@code filename*}（UTF-8），减少中文名乱码。
 */
public final class PdfResponseSupport {

    public static final MediaType PDF_MEDIA_TYPE = MediaType.APPLICATION_PDF;

    private static final int COPY_BUFFER = 8192;

    private PdfResponseSupport() {
    }

    /**
     * 预览：浏览器尽量以内嵌方式打开（是否内嵌仍取决于浏览器与插件）。
     */
    public static void writePreview(HttpServletResponse response, byte[] body, String filenameOrNull) throws IOException {
        applyPdfHeaders(response, filenameOrNull, false);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    /**
     * 下载：触发「另存为」或下载栏；{@code filename} 会净化并保证以 {@code .pdf} 结尾（若原本无扩展名则补上）。
     */
    public static void writeDownload(HttpServletResponse response, byte[] body, String filename) throws IOException {
        applyPdfHeaders(response, safeFilename(filename, true), true);
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    /**
     * 流式写出，适合大文件；{@code contentLength} 未知时可传 {@code -1}（不设置 Content-Length）。
     */
    public static void writePreview(HttpServletResponse response, InputStream body, long contentLength, String filenameOrNull)
            throws IOException {
        applyPdfHeaders(response, filenameOrNull, false);
        if (contentLength >= 0) {
            response.setContentLengthLong(contentLength);
        }
        copy(body, response.getOutputStream());
    }

    public static void writeDownload(HttpServletResponse response, InputStream body, long contentLength, String filename)
            throws IOException {
        applyPdfHeaders(response, safeFilename(filename, true), true);
        if (contentLength >= 0) {
            response.setContentLengthLong(contentLength);
        }
        copy(body, response.getOutputStream());
    }

    public static ResponseEntity<byte[]> previewEntity(byte[] body, String filenameOrNull) {
        HttpHeaders headers = pdfHeaders(filenameOrNull, false);
        headers.setContentLength(body.length);
        return ResponseEntity.ok().headers(headers).body(body);
    }

    public static ResponseEntity<byte[]> downloadEntity(byte[] body, String filename) {
        HttpHeaders headers = pdfHeaders(safeFilename(filename, true), true);
        headers.setContentLength(body.length);
        return ResponseEntity.ok().headers(headers).body(body);
    }

    /**
     * 大文件优先用 {@link InputStreamResource}，避免整段读入内存。
     */
    public static ResponseEntity<Resource> previewResource(InputStream body, long contentLength, String filenameOrNull) {
        HttpHeaders headers = pdfHeaders(filenameOrNull, false);
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(body));
    }

    public static ResponseEntity<Resource> downloadResource(InputStream body, long contentLength, String filename) {
        HttpHeaders headers = pdfHeaders(safeFilename(filename, true), true);
        if (contentLength >= 0) {
            headers.setContentLength(contentLength);
        }
        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(body));
    }

    public static HttpHeaders pdfHeaders(String filenameOrNull, boolean attachment) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(PDF_MEDIA_TYPE);
        h.setAccessControlExposeHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));
        h.add(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filenameOrNull, attachment).toString());
        return h;
    }

    private static void applyPdfHeaders(HttpServletResponse response, String filenameOrNull, boolean attachment) {
        response.setContentType(PDF_MEDIA_TYPE.toString());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition(filenameOrNull, attachment).toString());
        response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
    }

    private static ContentDisposition buildContentDisposition(String filenameOrNull, boolean attachment) {
        String name = filenameOrNull == null ? null : filenameOrNull.trim();
        if (name == null || name.isEmpty()) {
            return attachment ? ContentDisposition.attachment().build() : ContentDisposition.inline().build();
        }
        name = safeFilename(name, attachment);
        if (attachment) {
            return ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build();
        }
        return ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build();
    }

    /**
     * 去掉路径片段与换行，降低响应头注入风险；下载场景下无 {@code .pdf} 时补上。
     */
    public static String safeFilename(String raw, boolean ensurePdfExtension) {
        if (raw == null) {
            return "document.pdf";
        }
        String s = raw.trim();
        int slash = Math.max(s.lastIndexOf('/'), s.lastIndexOf('\\'));
        if (slash >= 0 && slash < s.length() - 1) {
            s = s.substring(slash + 1);
        }
        s = s.replace("\r", "").replace("\n", "").replace("\"", "");
        if (s.isEmpty()) {
            return "document.pdf";
        }
        if (ensurePdfExtension && !s.toLowerCase().endsWith(".pdf")) {
            return s + ".pdf";
        }
        return s;
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[COPY_BUFFER];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
    }
}
