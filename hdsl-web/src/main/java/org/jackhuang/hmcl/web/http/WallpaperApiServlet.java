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
package org.jackhuang.hmcl.web.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// 壁纸，映射在 `/api/wallpapers/*`：
///
/// - `GET    /api/wallpapers` — `{wallpapers:[{id,name,url,builtin,width,height,bytes}]}`，
///   内置的在前、用户上传的按上传时间排在后面。
/// - `POST   /api/wallpapers` — `multipart/form-data`，字段 `file`；收下并校验后返回那一条。
/// - `DELETE /api/wallpapers/<id>` — 只能删用户上传的，内置的没有可删的副本。
/// - `GET    /api/wallpapers/<id>` — 图片本身。
///
/// 上传的东西**只有一种能留下来**：一张能解码的静态位图。文件名不参与任何决定——
/// 存的是随机 id 和我们自己按嗅探结果给的后缀，浏览器拿到的 `Content-Type` 也是我们从
/// 那份记录里读出来的，所以「改个扩展名的脚本」在这里既不会被命名成脚本、也不会被当成脚本发出去。
/// 校验分四层，缺一不可（见 [inspect]）：魔数、动图特征、真的去读图片头确认能解码、
/// 尺寸与体积上限。SVG 直接拒：它是能带脚本的矢量格式，不是位图。
///
/// 存储是 `<data>/wallpapers/`：`<id>.<kind>` 一个图片文件，外加 `index.json` 记元数据。
/// 图片文件权限 `0600`——壁纸是这台机器上某个人的审美，不是公共资源。
///
/// 登录页看不到用户上传的壁纸：`/api/*` 在会话门后面，未登录拿不到列表，
/// 所以认证页固定用内置壁纸。
@NotNullByDefault
public final class WallpaperApiServlet extends jakarta.servlet.http.HttpServlet {

    /// 一次上传允许的最大字节数。4K 的 JPEG 壁纸通常在几 MB 到十几 MB 之间，12 MiB 够用。
    static final long MAX_BYTES = 12L * 1024 * 1024;

    /// 像素上限：挡住「1×1 声明、解码后炸开」的解压炸弹。4000 万像素足够一张 4K 壁纸。
    static final long MAX_PIXELS = 40_000_000L;

    static final int MAX_EDGE = 12_000;

    /// 我们肯存、也肯发出去的两种位图。GIF 不在其中：它太容易是动画。
    /// WebP 也不在：JDK 的 ImageIO 没有 WebP 解码器，收下来也过不了「真读一遍图片头」那步，
    /// 与其留一个永远失败的上传格式，不如在入口就说清只有 JPEG/PNG。
    private static final Set<String> KINDS = Set.of("jpg", "png");

    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{8,16}");

    private static final String INDEX_FILE = "index.json";

    /// 随面板打包的内置壁纸：由 StaticServlet 从 jar 里发，这里只登记，
    /// 好让「可选壁纸」这一份列表对前端是完整的。加一张就要在这里加一行，
    /// 同时在 `src/main/resources/web/assets-img/wallpapers/` 放下文件。
    private static final List<String> BUILTIN = List.of(
            "2015-06-22.jpg",
            "2016-02-25.jpg",
            "2021-08-26.jpg");

    private static final String BUILTIN_URL = "assets-img/wallpapers/";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Gson GSON = new Gson();

    private final Path root;
    private final Object lock = new Object();

    public WallpaperApiServlet(Path dataDir) {
        this.root = dataDir.resolve("wallpapers");
    }

    /// 上传的体积上限：`maxFileSize` 卡文件本身，`maxRequestSize` 多给 multipart 的边界留余量。
    public static MultipartConfigElement multipartConfig(Path stagingDir) {
        return new MultipartConfigElement(stagingDir.toString(),
                MAX_BYTES + 256L * 1024, MAX_BYTES + 64L * 1024, 64 * 1024);
    }

    /// 一条壁纸记录。`id` 对内置的是 `builtin-<文件名去后缀>`，对上传的是随机 token；
    /// 两者共用一个命名空间，所以选择结果（前端存 localStorage）不会被格式弄混。
    record Wallpaper(String id, String name, String url, boolean builtin,
                     @Nullable Integer width, @Nullable Integer height, @Nullable Long bytes) {
    }

    /// `index.json` 里的一项。`file` 是我们自己拼的名字，从不来自客户端。
    private record Entry(String id, String file, String name, String kind,
                         int width, int height, long bytes, long createdAt) {
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        String id = path == null || path.isEmpty() || "/".equals(path) ? null : path.substring(1);
        if (id != null && (id.indexOf('/') >= 0 || !ID_PATTERN.matcher(id).matches())) {
            // 也挡住 `..`：id 只允许这些字符，路径就不可能被拼出来
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        switch (request.getMethod()) {
            case "GET" -> {
                if (id == null) {
                    list(response);
                } else {
                    serve(request, response, id);
                }
            }
            case "POST" -> {
                if (id != null) {
                    Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
                    return;
                }
                upload(request, response);
            }
            case "DELETE" -> {
                if (id == null) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "which wallpaper?");
                    return;
                }
                delete(request, response, id);
            }
            default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "only GET, POST and DELETE");
        }
    }

    // ------------------------------------------------------------------ 列表 --

    private void list(HttpServletResponse response) throws IOException {
        JsonArray array = new JsonArray();
        for (String name : BUILTIN) {
            array.add(json(new Wallpaper("builtin-" + stripExtension(name), name,
                    BUILTIN_URL + name, true, null, null, null)));
        }
        for (Entry entry : readIndex()) {
            array.add(json(new Wallpaper(entry.id(), entry.name(), "api/wallpapers/" + entry.id(),
                    false, entry.width(), entry.height(), entry.bytes())));
        }
        JsonObject body = new JsonObject();
        body.add("wallpapers", array);
        Json.write(response, body);
    }

    private static JsonObject json(Wallpaper wallpaper) {
        JsonObject object = new JsonObject();
        object.addProperty("id", wallpaper.id());
        object.addProperty("name", wallpaper.name());
        object.addProperty("url", wallpaper.url());
        object.addProperty("builtin", wallpaper.builtin());
        if (wallpaper.width() != null) {
            object.addProperty("width", wallpaper.width());
        }
        if (wallpaper.height() != null) {
            object.addProperty("height", wallpaper.height());
        }
        if (wallpaper.bytes() != null) {
            object.addProperty("bytes", wallpaper.bytes());
        }
        return object;
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    // ------------------------------------------------------------------ 上传 --

    private void upload(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Part part;
        try {
            part = request.getPart("file");
        } catch (jakarta.servlet.ServletException | IllegalStateException e) {
            // Jetty 在 part 超过 maxFileSize 时先抛异常，走不到下面那道体积检查；
            // 和"字段缺失"共用一句英文会把"文件太大"说成"表单不对"。
            String detail = String.valueOf(e.getMessage());
            boolean tooLarge = detail.contains("bad multipart") || detail.toLowerCase(Locale.ROOT).contains("size");
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, tooLarge
                    ? "壁纸最大 " + (MAX_BYTES / (1024 * 1024)) + " MiB，这份超过上限（或不是带 `file` 字段的表单）"
                    : "expected a multipart form with a `file` field: " + detail);
            return;
        }
        if (part == null || part.getSize() == 0) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a `file` field is required");
            return;
        }
        if (part.getSize() > MAX_BYTES) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "壁纸最大 " + (MAX_BYTES / (1024 * 1024)) + " MiB，这份是 "
                            + (part.getSize() / 1024) + " KiB");
            return;
        }

        byte[] bytes;
        try (var in = part.getInputStream()) {
            bytes = in.readAllBytes();
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "读不了这份文件：" + e.getMessage());
            return;
        }

        Image image;
        try {
            image = inspect(bytes);
        } catch (InvalidImage e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        String id = newId();
        Entry entry = new Entry(id, id + "." + image.kind(),
                displayName(request, part.getSubmittedFileName()),
                image.kind(), image.width(), image.height(), bytes.length, System.currentTimeMillis());
        try {
            write(entry, bytes);
        } catch (IOException e) {
            LOG.warning("Could not store the uploaded wallpaper", e);
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "壁纸没能存下来：" + e.getMessage());
            return;
        }

        AuditLog.log(AuditLog.Channel.WEB, "wallpaper.upload", entry.id(), "success",
                actorOf(request), request.getRemoteAddr());
        Json.write(response, HttpServletResponse.SC_CREATED,
                json(new Wallpaper(entry.id(), entry.name(), "api/wallpapers/" + entry.id(),
                        false, entry.width(), entry.height(), entry.bytes())));
    }

    /// 展示用的名字：只取客户端文件名里的可见字符并截断，**不参与落盘路径**。
    private static String displayName(HttpServletRequest request, String submitted) {
        String raw = request.getParameter("name");
        if (raw == null || raw.isBlank()) {
            raw = submitted == null || submitted.isBlank() ? "自定义壁纸" : submitted;
        }
        String base = raw.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\p{Cntrl}]", "").trim();
        if (base.isEmpty()) {
            base = "自定义壁纸";
        }
        return base.length() > 64 ? base.substring(0, 64) : base;
    }

    private static String newId() {
        byte[] random = new byte[9];
        RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private void write(Entry entry, byte[] bytes) throws IOException {
        synchronized (lock) {
            Files.createDirectories(root);
            Path target = root.resolve(entry.file());
            Files.write(target, bytes);
            try {
                Files.setPosixFilePermissions(target, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException ignored) {
                // 没有 POSIX 权限的平台：文件落在数据目录里，权限由那儿的挂载决定
            }
            List<Entry> entries = readIndex();
            entries.add(entry);
            writeIndex(entries);
        }
    }

    // ------------------------------------------------------------------ 删除 --

    private void delete(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        synchronized (lock) {
            List<Entry> entries = readIndex();
            Entry found = null;
            for (Entry entry : entries) {
                if (entry.id().equals(id)) {
                    found = entry;
                    break;
                }
            }
            if (found == null) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such wallpaper");
                return;
            }
            Files.deleteIfExists(root.resolve(found.file()));
            entries.remove(found);
            writeIndex(entries);
        }
        AuditLog.log(AuditLog.Channel.WEB, "wallpaper.delete", id, "success",
                actorOf(request), request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    // ------------------------------------------------------------------ 出图 --

    private void serve(HttpServletRequest request, HttpServletResponse response, String id)
            throws IOException {
        Entry entry = null;
        for (Entry candidate : readIndex()) {
            if (candidate.id().equals(id)) {
                entry = candidate;
                break;
            }
        }
        if (entry == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no such wallpaper");
            return;
        }
        Path file = root.resolve(entry.file());
        if (!Files.isRegularFile(file)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "the wallpaper file is gone");
            return;
        }
        // 我们只按自己记下的种类发出去：客户端说什么 Content-Type 都不算。
        response.setContentType("image/" + ("jpg".equals(entry.kind()) ? "jpeg" : entry.kind()));
        response.setHeader("X-Content-Type-Options", "nosniff");
        // 这张图永远不会是文档：给它一个什么都没有的策略，浏览器就不会替它解释任何脚本。
        response.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'none'; style-src 'none'");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("Cache-Control", "private, max-age=31536000, immutable");
        response.setContentLengthLong(Files.size(file));
        Files.copy(file, response.getOutputStream());
    }

    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(org.jackhuang.hmcl.web.auth.AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof org.jackhuang.hmcl.web.auth.AuthService.Session s ? s.username() : null;
    }

    // -------------------------------------------------------------- 索引文件 --

    private List<Entry> readIndex() {
        Path file = root.resolve(INDEX_FILE);
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        try {
            Entry[] stored = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Entry[].class);
            List<Entry> entries = new ArrayList<>();
            if (stored != null) {
                for (Entry entry : stored) {
                    if (entry != null && ID_PATTERN.matcher(entry.id()).matches()
                            && KINDS.contains(entry.kind())) {
                        entries.add(entry);
                    }
                }
            }
            entries.sort(Comparator.comparingLong(Entry::createdAt));
            return entries;
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read " + file + "; the wallpaper list starts empty", e);
            return new ArrayList<>();
        }
    }

    private void writeIndex(List<Entry> entries) throws IOException {
        Files.createDirectories(root);
        Path file = root.resolve(INDEX_FILE);
        Path temp = root.resolve(INDEX_FILE + ".tmp");
        Files.writeString(temp, GSON.toJson(entries.toArray(new Entry[0]), Entry[].class),
                StandardCharsets.UTF_8);
        // 索引里是用户自己起的壁纸名，和图片一样按私有处理
        try {
            Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // 没有 POSIX 权限的平台：见 write() 里同样的说明
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    // ---------------------------------------------------------------- 校验 --

    record Image(String kind, int width, int height) {
    }

    /// 校验失败的原因，直接就是给用户看的那句话。
    static final class InvalidImage extends Exception {
        InvalidImage(String message) {
            super(message);
        }
    }

    /// 这张图是不是我们能存、能发的静态位图。四道检查，任何一道不过就拒：
    ///
    /// 1. **魔数**：头几个字节必须是 JPEG / PNG 之一。客户端给的
    ///    `Content-Type` 和文件名一律不看——那是攻击者写的。
    /// 2. **动图与矢量**：GIF、APNG（`acTL`）和 SVG 全部拒绝。SVG 是重点：它是文本容器，
    ///    能带 `<script>`，而"图片"是浏览器唯一愿意直接渲染文本格式的地方。
    /// 3. **真的读一遍图片头**：用 ImageIO 拿宽高。声明 1×1、解码后 60000×60000
    ///    那类解压炸弹在这一步被尺寸上限拦掉。
    /// 4. **体积**：由调用方按 [MAX_BYTES] 卡，multipart 配置再兜一层。
    ///
    /// WebP 不在这里：JDK 没有 WebP 解码器（`ImageIO.getImageReaders` 为空），收下来
    /// 只会卡在第 3 步。要支持就引入一个 ImageIO WebP 插件，而不是留一个假入口。
    static Image inspect(byte[] bytes) throws InvalidImage {
        if (bytes.length < 16) {
            throw new InvalidImage("文件太小，不是一张图片");
        }
        if (isGif(bytes)) {
            throw new InvalidImage("不支持 GIF（可能是动画）；请用 JPEG 或 PNG");
        }
        if (looksLikeSvg(bytes)) {
            throw new InvalidImage("不支持 SVG：它能携带脚本，不是位图");
        }
        if (isWebP(bytes)) {
            throw new InvalidImage("暂不支持 WebP：面板的解码器只能读 JPEG 与 PNG");
        }
        String kind;
        if (isJpeg(bytes)) {
            kind = "jpg";
        } else if (isPng(bytes)) {
            if (contains(bytes, "acTL".getBytes(StandardCharsets.US_ASCII))) {
                throw new InvalidImage("不支持 APNG（动图）；请用静态 PNG");
            }
            kind = "png";
        } else {
            throw new InvalidImage("只接受静态的 JPEG 或 PNG 图片");
        }

        int[] size = decodeDimensions(bytes);
        long pixels = (long) size[0] * size[1];
        if (size[0] > MAX_EDGE || size[1] > MAX_EDGE || pixels > MAX_PIXELS) {
            throw new InvalidImage("图片尺寸太大（" + size[0] + "×" + size[1] + "）");
        }
        return new Image(kind, size[0], size[1]);
    }

    private static boolean isJpeg(byte[] b) {
        return (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        // 签名是 89 50 4E 47 0D 0A 1A 0A —— 中间那对是 CR LF，不是 LF CR
        return (b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
    }

    private static boolean isGif(byte[] b) {
        return b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
    }

    private static boolean isWebP(byte[] b) {
        return b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    /// 前 512 字节里出现 `<svg` 就拒。真正的 SVG 魔数就是它的文本开头，
    /// 而带 BOM / 注释的变体同样过不了后面那道 ImageIO 解码。
    private static boolean looksLikeSvg(byte[] b) {
        int limit = Math.min(b.length, 512);
        String head = new String(b, 0, limit, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return head.contains("<svg");
    }

    private static boolean contains(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /// 只读图片头拿宽高：不解码像素，所以一张伪装成图片的巨型文件不会在这里把内存吃掉。
    private static int[] decodeDimensions(byte[] bytes) throws InvalidImage {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new InvalidImage("这份文件解不开，不是一张能用的图片");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new InvalidImage("图片尺寸无效");
                }
                return new int[]{width, height};
            } finally {
                reader.dispose();
            }
        } catch (InvalidImage e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new InvalidImage("这份文件读不出图片头：" + e.getMessage());
        }
    }
}
