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

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins what the wallpaper upload accepts. This is the boundary that decides
/// whether a hostile file becomes a served asset, so every rejection path is a
/// case here: the client's own `Content-Type` and file name are never consulted,
/// only the bytes.
class WallpaperApiServletTest {

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] jpeg() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    @Test
    void aRealPngIsAcceptedWithItsDimensions() throws Exception {
        WallpaperApiServlet.Image image = WallpaperApiServlet.inspect(png(64, 32));
        assertEquals("png", image.kind());
        assertEquals(64, image.width());
        assertEquals(32, image.height());
    }

    @Test
    void aRealJpegIsAccepted() throws Exception {
        assertEquals("jpg", WallpaperApiServlet.inspect(jpeg()).kind());
    }

    @Test
    void aScriptInsideAnImageHeaderIsStillRejectedByTheDecodeStep() {
        // 完整的 PNG 签名，后面全是垃圾：魔数过了，读图片头那一步必须拦住
        byte[] bytes = new byte[64];
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        for (int i = signature.length; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        WallpaperApiServlet.InvalidImage e = assertThrows(WallpaperApiServlet.InvalidImage.class,
                () -> WallpaperApiServlet.inspect(bytes));
        assertTrue(e.getMessage().contains("读不出") || e.getMessage().contains("解不开"), e.getMessage());
    }

    @Test
    void svgIsRejectedBecauseItCanCarryScript() {
        byte[] bytes = ("<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"><rect/></svg>")
                .getBytes(StandardCharsets.UTF_8);
        WallpaperApiServlet.InvalidImage e = assertThrows(WallpaperApiServlet.InvalidImage.class,
                () -> WallpaperApiServlet.inspect(bytes));
        assertTrue(e.getMessage().contains("SVG"), e.getMessage());
    }

    @Test
    void gifIsRejectedEvenAsStillImage() {
        byte[] bytes = "GIF89a".concat("xxxxxxxxxxxxxxxx").getBytes(StandardCharsets.US_ASCII);
        WallpaperApiServlet.InvalidImage e = assertThrows(WallpaperApiServlet.InvalidImage.class,
                () -> WallpaperApiServlet.inspect(bytes));
        assertTrue(e.getMessage().contains("GIF"), e.getMessage());
    }

    @Test
    void animatedPngIsRejected() throws Exception {
        byte[] base = png(8, 8);
        // An `acTL` chunk name anywhere in the stream marks it as APNG.
        byte[] bytes = new byte[base.length + 12];
        System.arraycopy(base, 0, bytes, 0, base.length);
        System.arraycopy("acTL".getBytes(StandardCharsets.US_ASCII), 0, bytes, base.length + 4, 4);
        WallpaperApiServlet.InvalidImage e = assertThrows(WallpaperApiServlet.InvalidImage.class,
                () -> WallpaperApiServlet.inspect(bytes));
        assertTrue(e.getMessage().contains("APNG"), e.getMessage());
    }

    @Test
    void webpIsRejectedBecauseTheJdkCannotDecodeIt() {
        // 它不是"不支持的动图"，而是根本没有解码器：留个入口只会让用户上传后拿到一句
        // 「读不出图片头」。这里钉住的是"明确拒绝并说明原因"。
        byte[] bytes = new byte[64];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, bytes, 8, 4);
        System.arraycopy("VP8X".getBytes(StandardCharsets.US_ASCII), 0, bytes, 12, 4);
        WallpaperApiServlet.InvalidImage e = assertThrows(WallpaperApiServlet.InvalidImage.class,
                () -> WallpaperApiServlet.inspect(bytes));
        assertTrue(e.getMessage().contains("WebP"), e.getMessage());
    }

    @Test
    void unknownBytesAreRejected() {
        byte[] bytes = new byte[64];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) 'Z';
        }
        WallpaperApiServlet.InvalidImage e = assertThrows(WallpaperApiServlet.InvalidImage.class,
                () -> WallpaperApiServlet.inspect(bytes));
        assertTrue(e.getMessage().contains("JPEG"), e.getMessage());
    }

    @Test
    void aTinyFileIsRejected() {
        byte[] bytes = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
        assertThrows(WallpaperApiServlet.InvalidImage.class, () -> WallpaperApiServlet.inspect(bytes));
    }

    @Test
    void anAbsurdDeclaredSizeIsRejected() throws Exception {
        byte[] bytes = png(8, 8);
        // IHDR width/height live at offsets 16..23 (big-endian). Claim 20000×20000:
        // the header still parses, so this is the pixel budget that has to refuse it.
        bytes[16] = 0;
        bytes[17] = 0;
        bytes[18] = 0x4E;
        bytes[19] = 0x20;
        bytes[20] = 0;
        bytes[21] = 0;
        bytes[22] = 0x4E;
        bytes[23] = 0x20;
        WallpaperApiServlet.InvalidImage e = assertThrows(WallpaperApiServlet.InvalidImage.class,
                () -> WallpaperApiServlet.inspect(bytes));
        assertTrue(e.getMessage().contains("尺寸太大"), e.getMessage());
    }
}
