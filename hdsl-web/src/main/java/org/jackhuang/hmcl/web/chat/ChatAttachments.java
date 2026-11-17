/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.web.chat;

import jakarta.servlet.http.Part;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// 附件落盘、MIME 嗅探与文本抽取（docs/chat.md §8）。
///
/// 文件按 sha256 去重，元数据进 SQLite；Office/PDF 只在有界线程池里抽文本，
/// 不执行任何嵌入对象。宏文档、SVG/HTML、可执行文件一律拒绝。
@NotNullByDefault
public final class ChatAttachments {

    public static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_DOCUMENT_BYTES = 50L * 1024 * 1024;
    public static final int MAX_PER_MESSAGE = 8;
    private static final long MAX_EXTRACTED_CHARS = 400_000;
    private static final int MAX_PDF_PAGES = 300;

    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final Set<String> DOCUMENT_EXT = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx");
    private static final Set<String> TEXT_EXT = Set.of(
            "txt", "md", "markdown", "csv", "json", "log", "yml", "yaml", "xml", "ini", "conf");
    private static final Set<String> AUDIO_EXT = Set.of("mp3", "wav", "m4a", "aac", "ogg", "flac");
    private static final Set<String> VIDEO_EXT = Set.of("mp4", "webm", "mov", "mkv", "avi");

    public record Stored(
            String name, String mime, String kind, long size, String sha256, String path,
            @Nullable String textPath, String status, @Nullable String error) {
    }

    private final ChatStore store;
    private final ExecutorService extractor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "chat-attachment-extract");
        thread.setDaemon(true);
        return thread;
    });

    public ChatAttachments(ChatStore store) {
        this.store = store;
    }

    public Stored ingest(String sessionId, Part part) throws IOException {
        String original = part.getSubmittedFileName();
        String name = original == null || original.isBlank() ? "attachment" : sanitizeName(original);
        String extension = extensionOf(name);
        if (isMacro(extension)) {
            throw new Refused("refused_macro: macro documents are not accepted");
        }
        long max = IMAGE_EXT.contains(extension) ? MAX_IMAGE_BYTES : MAX_DOCUMENT_BYTES;
        Path temp = Files.createTempFile(store.filesDir(), "upload-", ".tmp");
        Path stored;
        String sha;
        String mime;
        String kind;
        long size;
        try {
            copyCapped(part.getInputStream(), temp, max);
            byte[] head = readHead(temp, 1024);
            String sniffed = sniffMime(head);
            kind = kind(extension, sniffed);
            if (kind == null) {
                throw new Refused("refused_type: " + name + " is not a supported attachment");
            }
            if (isDangerous(head)) {
                throw new Refused("refused_type: executable, SVG and HTML attachments are not accepted");
            }
            if (("image".equals(kind) || "document".equals(kind)) && extension.isEmpty()) {
                throw new Refused("refused_type: the file has no extension, cannot verify it");
            }
            if ("image".equals(kind) && !"webp".equals(extension)) {
                validateImage(temp);
                sanitizeImage(temp, extension);
            }
            sha = ChatStore.sha256(temp);
            stored = store.filesDir().resolve(sha + (extension.isEmpty() ? "" : "." + extension));
            if (!Files.exists(stored)) {
                Files.move(temp, stored, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(temp);
            }
            setPrivate(stored);
            size = Files.size(stored);
            byte[] finalHead = readHead(stored, 1024);
            mime = sniffMime(finalHead);
            if (mime == null) {
                mime = mimeOf(extension);
            }
            kind = kind(extension, mime);
            if (kind == null) {
                throw new Refused("refused_type: the stored file is not a supported attachment");
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
        String extractStatus = "skipped";
        String textPath = null;
        String error = null;
        if ("document".equals(kind) || "text".equals(kind)) {
            try {
                String text = extract(stored, extension, kind);
                if (text != null) {
                    Path textFile = store.filesDir().resolve(sha + ".txt");
                    Files.writeString(textFile, text, StandardCharsets.UTF_8);
                    setPrivate(textFile);
                    textPath = textFile.toString();
                    extractStatus = "done";
                }
            } catch (Exception e) {
                extractStatus = "failed";
                error = message(e);
            }
        }
        return new Stored(name, mime, kind, size, sha, stored.toString(), textPath, extractStatus, error);
    }

    private static void setPrivate(Path path) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path,
                        java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            }
        } catch (IOException | UnsupportedOperationException ignored) {
            // Windows / exotic filesystem.
        }
    }

    private static long copyCapped(InputStream input, Path target, long max) throws IOException {
        long total = 0;
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream output = Files.newOutputStream(target)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > max) {
                    throw new Refused("too_large: the attachment exceeds " + (max / (1024 * 1024)) + " MB");
                }
                output.write(buffer, 0, read);
            }
        }
        return total;
    }

    private static byte[] readHead(Path path, int max) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return input.readNBytes(max);
        }
    }

    private static String sanitizeName(String name) {
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[\\p{Cntrl}]", "").trim();
        if (base.isEmpty()) {
            base = "attachment";
        }
        return base.length() > 180 ? base.substring(base.length() - 180) : base;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isMacro(String extension) {
        return Set.of("docm", "pptm", "xlsm", "dotm", "potm", "xlam").contains(extension);
    }

    private static boolean isDangerous(byte[] head) {
        if (head.length >= 2 && head[0] == 'M' && head[1] == 'Z') {
            return true;
        }
        if (head.length >= 4 && head[0] == 0x7f && head[1] == 'E' && head[2] == 'L' && head[3] == 'F') {
            return true;
        }
        String text = new String(head, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        return text.startsWith("<!doctype html") || text.startsWith("<html")
                || text.startsWith("<svg") || text.startsWith("<?xml") && text.contains("<svg");
    }

    private static @Nullable String sniffMime(byte[] head) {
        if (startsWith(head, new int[]{0x89, 'P', 'N', 'G'})) return "image/png";
        if (startsWith(head, new int[]{0xFF, 0xD8, 0xFF})) return "image/jpeg";
        if (startsWith(head, new int[]{'G', 'I', 'F', '8'})) return "image/gif";
        if (head.length >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "image/webp";
        }
        if (startsWith(head, new int[]{'%', 'P', 'D', 'F'})) return "application/pdf";
        if (startsWith(head, new int[]{'P', 'K', 0x03, 0x04})) return "application/zip";
        if (startsWith(head, new int[]{0xD0, 0xCF, 0x11, 0xE0})) return "application/x-ole-storage";
        return null;
    }

    private static boolean startsWith(byte[] bytes, int[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xff) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String kind(String extension, @Nullable String mime) {
        if (IMAGE_EXT.contains(extension)) {
            return "image";
        }
        if (DOCUMENT_EXT.contains(extension)) {
            return "document";
        }
        if (TEXT_EXT.contains(extension)) {
            return "text";
        }
        if (AUDIO_EXT.contains(extension)) {
            return "audio";
        }
        if (VIDEO_EXT.contains(extension)) {
            return "video";
        }
        if (mime != null && mime.startsWith("image/")) {
            return "image";
        }
        return null;
    }

    private static String mimeOf(String extension) {
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            case "md", "markdown" -> "text/markdown";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            default -> "text/plain";
        };
    }

    // --------------------------------------------------------------- images --

    private static void validateImage(Path path) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw new Refused("invalid_image: cannot read the image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new Refused("invalid_image: unsupported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                if (width <= 0 || height <= 0 || width > 16_384 || height > 16_384 || pixels > 80_000_000L) {
                    throw new Refused("invalid_image: " + width + "x" + height + " is too large");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    /// JPEG/PNG 重新编码一次，顺带丢掉 EXIF；WebP/GIF 只校验，不改写。
    private static void sanitizeImage(Path path, String extension) throws IOException {
        if (!Set.of("png", "jpg", "jpeg").contains(extension)) {
            return;
        }
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new Refused("invalid_image: cannot decode the image");
        }
        String format = "png".equals(extension) ? "png" : "jpeg";
        Path temp = Files.createTempFile(path.getParent(), "image-", ".tmp");
        try {
            if (!ImageIO.write(image, format, temp.toFile())) {
                return;
            }
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    // ----------------------------------------------------------- extraction --

    private String extract(Path path, String extension, String kind) throws Exception {
        if (Set.of("docx", "pptx", "xlsx").contains(extension)) {
            checkZipBomb(path);
        }
        Future<String> future = extractor.submit(() -> {
            try {
                return switch (extension) {
                    case "pdf" -> pdf(path);
                    case "docx" -> docx(path);
                    case "doc" -> doc(path);
                    case "pptx" -> pptx(path);
                    case "ppt" -> ppt(path);
                    case "xlsx" -> xlsx(path);
                    case "xls" -> xls(path);
                    default -> kind.equals("text") ? text(path) : null;
                };
            } catch (Exception e) {
                throw new IOException(message(e), e);
            }
        });
        try {
            String text = future.get(45, TimeUnit.SECONDS);
            if (text == null) {
                return null;
            }
            text = text.replace("\u0000", "").replace("\r\n", "\n").trim();
            if (text.length() > MAX_EXTRACTED_CHARS) {
                text = text.substring(0, (int) MAX_EXTRACTED_CHARS) + "\n\n[内容过长，已截断]";
            }
            return text.isEmpty() ? null : text;
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new IOException("text extraction timed out");
        }
    }

    /// OOXML 是 zip：拒绝展开后超过 200 MB、单条目超过 50 MB 或条目数过万的包。
    private static void checkZipBomb(Path path) throws IOException {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(path.toFile())) {
            long total = 0;
            int entries = 0;
            var iterator = zip.entries();
            while (iterator.hasMoreElements()) {
                var entry = iterator.nextElement();
                if (++entries > 20_000) {
                    throw new IOException("document has too many entries");
                }
                if (entry.getSize() > 50L * 1024 * 1024) {
                    throw new IOException("document contains an entry larger than 50 MB");
                }
                if (entry.getSize() > 0) {
                    total += entry.getSize();
                    if (total > 200L * 1024 * 1024) {
                        throw new IOException("document expands to more than 200 MB");
                    }
                }
            }
            long compressed = Math.max(1, Files.size(path));
            if (total / compressed > 200) {
                throw new IOException("document has an unreasonable compression ratio");
            }
        }
    }

    private static String pdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()) {
                throw new IOException("encrypted PDFs are not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(Math.min(document.getNumberOfPages(), MAX_PDF_PAGES));
            String text = stripper.getText(document);
            if (document.getNumberOfPages() > MAX_PDF_PAGES) {
                text += "\n\n[只读取了前 " + MAX_PDF_PAGES + " 页]";
            }
            return text;
        }
    }

    private static String docx(Path path) throws IOException {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String doc(Path path) throws IOException {
        try (HWPFDocument document = new HWPFDocument(Files.newInputStream(path));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String pptx(Path path) throws IOException {
        try (XMLSlideShow slideShow = new XMLSlideShow(Files.newInputStream(path))) {
            StringBuilder text = new StringBuilder();
            slideShow.getSlides().forEach(slide -> {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        text.append(textShape.getText()).append('\n');
                    }
                }
                text.append('\n');
            });
            return text.toString();
        }
    }

    private static String ppt(Path path) throws IOException {
        try (HSLFSlideShow slideShow = new HSLFSlideShow(Files.newInputStream(path))) {
            StringBuilder text = new StringBuilder();
            slideShow.getSlides().forEach(slide -> {
                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        text.append(textShape.getText()).append('\n');
                    }
                }
                text.append('\n');
            });
            return text.toString();
        }
    }

    private static String xlsx(Path path) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(path))) {
            return workbook(workbook);
        }
    }

    private static String xls(Path path) throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook(Files.newInputStream(path))) {
            return workbook(workbook);
        }
    }

    private static String workbook(Workbook workbook) {
        StringBuilder text = new StringBuilder();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        int sheets = 0;
        for (Sheet sheet : workbook) {
            if (sheets++ > 20) {
                text.append("\n[更多工作表已省略]");
                break;
            }
            text.append("## ").append(sheet.getSheetName()).append('\n');
            int rows = 0;
            for (Row row : sheet) {
                if (rows++ > 2000) {
                    text.append("[更多行已省略]\n");
                    break;
                }
                List<String> cells = new ArrayList<>();
                int columns = 0;
                for (Cell cell : row) {
                    if (columns++ > 100) {
                        cells.add("…");
                        break;
                    }
                    cells.add(formatter.formatCellValue(cell).replace('\n', ' '));
                }
                text.append(String.join("\t", cells)).append('\n');
            }
            text.append('\n');
        }
        return text.toString();
    }

    private static String text(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        String utf8 = decode(bytes, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return utf8;
        }
        String gb = decode(bytes, Charset.forName("GB18030"));
        return gb != null ? gb : new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private static @Nullable String decode(byte[] bytes, Charset charset) {
        try {
            return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    /// 文本附件预览用：UTF-8 优先，其次 GB18030（中文 Windows 的常见编码）。
    ///
    /// 只读开头一段做判断，避免 50MB 的日志每预览一次就整个读进内存。
    public static Charset sniffTextCharset(Path path) throws IOException {
        byte[] sample;
        try (InputStream input = Files.newInputStream(path)) {
            sample = input.readNBytes(256 * 1024);
        }
        return isUtf8(sample) ? StandardCharsets.UTF_8 : Charset.forName("GB18030");
    }

    private static boolean isUtf8(byte[] bytes) {
        java.nio.CharBuffer chars = java.nio.CharBuffer.allocate(bytes.length);
        java.nio.charset.CoderResult result = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(bytes), chars, false);
        return !result.isError();
    }

    // --------------------------------------------------------------- misc ----

    public @Nullable String loadText(ChatStore.Attachment attachment) {
        if (attachment.extractedTextPath() == null) {
            return null;
        }
        try {
            return Files.readString(Path.of(attachment.extractedTextPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /// 删除没有任何附件记录再引用的文件（会话删除后调用）。
    public void pruneOrphans() throws IOException, java.sql.SQLException {
        Set<String> referenced = new java.util.HashSet<>();
        for (String path : store.attachmentFilePaths()) {
            referenced.add(Path.of(path).getFileName().toString());
        }
        try (var stream = Files.list(store.filesDir())) {
            for (Path path : stream.toList()) {
                if (!referenced.contains(path.getFileName().toString())) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    public void close() {
        extractor.shutdownNow();
    }

    public static final class Refused extends IOException {
        public Refused(String message) {
            super(message);
        }
    }

    private static String message(Throwable error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? error.getClass().getSimpleName() : error.getMessage();
    }
}