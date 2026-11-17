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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.regex.Pattern;

/// 头像存储（用户与助手共用）。
///
/// 只收**静态位图**：按魔数只放行 PNG / JPEG / WebP，再用 ImageIO 解码一次；
/// GIF/SVG/APNG 会在这一步被挡下来（GIF 魔数直接拒绝，APNG 解出首帧后重编码成静态 PNG）。
/// 存下来的永远是服务端自己画的 512×512 PNG：EXIF、附加块、任何"图片里的二进制"
/// 都不会跟着进磁盘，也不会跟着回给浏览器。
@NotNullByDefault
public final class ChatAvatars {

    /// 上传上限。头像再大也没有意义，前端裁完通常只有几十 KB。
    public static final long MAX_BYTES = 4L * 1024 * 1024;
    /// 原图边长上限：再大就拒绝，而不是让解码器去啃一张 20000×20000 的图。
    static final int MAX_SOURCE_DIM = 4_096;
    /// 落盘尺寸。
    static final int SIZE = 512;
    /// 头像 id 只可能是 64 位十六进制，路径穿越在这种情况下连形式都不成立。
    private static final Pattern ID = Pattern.compile("[a-f0-9]{64}");

    private final Path dir;

    public ChatAvatars(Path dataDir) {
        this.dir = dataDir.resolve("chat").resolve("avatars");
    }

    /// 解码、方形裁切、缩放、重编码成 PNG，按内容哈希落盘。返回头像 id。
    public String save(byte[] raw) throws ChatException {
        if (raw == null || raw.length == 0) {
            throw new ChatException(400, "empty_image", "图片是空的");
        }
        if (raw.length > MAX_BYTES) {
            throw new ChatException(413, "image_too_large", "头像不能超过 4 MB");
        }
        if (!isStaticBitmap(raw)) {
            throw new ChatException(415, "unsupported_image",
                    "只接受 PNG / JPEG / WebP 静态图片（不接受 SVG、GIF 或其它格式）");
        }
        BufferedImage decoded;
        try {
            decoded = ImageIO.read(new ByteArrayInputStream(raw));
        } catch (IOException | RuntimeException e) {
            throw new ChatException(415, "unsupported_image", "图片解码失败：" + e.getMessage());
        }
        if (decoded == null) {
            throw new ChatException(415, "unsupported_image", "这个格式浏览器能看，但面板解不开；换 PNG 或 JPEG");
        }
        int width = decoded.getWidth();
        int height = decoded.getHeight();
        if (width <= 0 || height <= 0 || width > MAX_SOURCE_DIM || height > MAX_SOURCE_DIM) {
            throw new ChatException(400, "image_dimensions",
                    "头像边长必须在 1 到 " + MAX_SOURCE_DIM + " 像素之间");
        }

        try {
            byte[] png = squarePng(decoded);
            String id = sha256(png);
            Files.createDirectories(dir);
            Path file = dir.resolve(id + ".png");
            if (!Files.isRegularFile(file)) {
                Path temp = Files.createTempFile(dir, "avatar-", ".png");
                Files.write(temp, png);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            }
            return id;
        } catch (IOException e) {
            throw new ChatException(500, "avatar_store_failed", "头像保存失败：" + e.getMessage());
        }
    }

    /// 头像文件；id 不合法或文件不存在返回 null。
    public @Nullable Path file(String id) {
        if (id == null || !ID.matcher(id).matches()) {
            return null;
        }
        Path path = dir.resolve(id + ".png");
        return Files.isRegularFile(path) ? path : null;
    }

    /// 删掉没被引用的头像。改一次头像就多一个文件，不清理会一直涨。
    public void prune(Set<String> keep) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var stream = Files.list(dir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".png"))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        String id = name.substring(0, name.length() - 4);
                        if (!keep.contains(id)) {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // 下次再清。
                            }
                        }
                    });
        } catch (IOException ignored) {
            // 清理失败不影响功能。
        }
    }

    /// 方形裁切（取中心）+ 缩放，输出 512×512 PNG。透明区域保留，边缘不会出现黑边。
    private static byte[] squarePng(BufferedImage source) throws IOException {
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        BufferedImage square = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = square.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, SIZE, SIZE, x, y, x + side, y + side, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(square, "png", out);
        return out.toByteArray();
    }

    /// 只凭魔数判断，不信扩展名也不信 Content-Type。
    static boolean isStaticBitmap(byte[] raw) {
        if (raw.length >= 8
                && (raw[0] & 0xFF) == 0x89 && raw[1] == 'P' && raw[2] == 'N' && raw[3] == 'G'
                && raw[4] == 0x0D && raw[5] == 0x0A && raw[6] == 0x1A && raw[7] == 0x0A) {
            return true;
        }
        if (raw.length >= 3
                && (raw[0] & 0xFF) == 0xFF && (raw[1] & 0xFF) == 0xD8 && (raw[2] & 0xFF) == 0xFF) {
            return true;
        }
        return raw.length >= 12
                && raw[0] == 'R' && raw[1] == 'I' && raw[2] == 'F' && raw[3] == 'F'
                && raw[8] == 'W' && raw[9] == 'E' && raw[10] == 'B' && raw[11] == 'P';
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(bytes)) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required", e);
        }
    }
}