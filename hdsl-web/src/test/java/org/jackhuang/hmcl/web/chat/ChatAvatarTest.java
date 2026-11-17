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

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 头像只收静态位图，并且存下来的一定是服务端自己画的 PNG。
class ChatAvatarTest {

    @Test
    void pngAndJpegBecomeASquareServerSidePng() throws Exception {
        Path data = Files.createTempDirectory("hdsl-avatar");
        ChatAvatars avatars = new ChatAvatars(data);

        String png = avatars.save(bitmap("png", 800, 400));
        assertTrue(png.matches("[a-f0-9]{64}"));
        Path file = avatars.file(png);
        assertNotNull(file);
        BufferedImage stored = ImageIO.read(file.toFile());
        assertEquals(512, stored.getWidth(), "正方形裁切后固定 512×512");
        assertEquals(512, stored.getHeight());

        String jpeg = avatars.save(bitmap("jpg", 300, 300));
        assertTrue(jpeg.matches("[a-f0-9]{64}"));
        assertNotNull(avatars.file(jpeg));
    }

    @Test
    void onlyStaticBitmapsAreAccepted() throws Exception {
        Path data = Files.createTempDirectory("hdsl-avatar-bad");
        ChatAvatars avatars = new ChatAvatars(data);

        ChatException gif = assertThrows(ChatException.class,
                () -> avatars.save("GIF89a-not-really".getBytes()));
        assertEquals(415, gif.status());

        ChatException svg = assertThrows(ChatException.class,
                () -> avatars.save("<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes()));
        assertEquals(415, svg.status());

        ChatException empty = assertThrows(ChatException.class, () -> avatars.save(new byte[0]));
        assertEquals(400, empty.status());
    }

    @Test
    void oversizedUploadsAreRefusedBeforeDecoding() {
        Path data;
        try {
            data = Files.createTempDirectory("hdsl-avatar-big");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        ChatAvatars avatars = new ChatAvatars(data);
        byte[] huge = new byte[(int) ChatAvatars.MAX_BYTES + 1];
        huge[0] = (byte) 0x89;
        ChatException error = assertThrows(ChatException.class, () -> avatars.save(huge));
        assertEquals(413, error.status());
    }

    @Test
    void idsCannotEscapeTheAvatarDirectory() throws Exception {
        Path data = Files.createTempDirectory("hdsl-avatar-id");
        ChatAvatars avatars = new ChatAvatars(data);
        assertNull(avatars.file("../../etc/passwd"));
        assertNull(avatars.file("abc"));
        assertNull(avatars.file(null));
        assertFalse(ChatAvatars.isStaticBitmap("not an image".getBytes()));
    }

    @Test
    void pruningKeepsOnlyReferencedFiles() throws Exception {
        Path data = Files.createTempDirectory("hdsl-avatar-prune");
        ChatAvatars avatars = new ChatAvatars(data);
        String keep = avatars.save(bitmap("png", 64, 64));
        String drop = avatars.save(bitmap("png", 128, 128));
        avatars.prune(Set.of(keep));
        assertNotNull(avatars.file(keep));
        assertNull(avatars.file(drop));
    }

    private static byte[] bitmap(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int color = 0x3366CC ^ (width * 31);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, color);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
