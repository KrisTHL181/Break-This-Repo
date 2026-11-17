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

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.web.event.EventBus;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 用户资料与助手头像：默认用登录名，`img:<id>` 必须真的存在。
class ChatProfileTest {

    @Test
    void profileDefaultsToTheLoginNameAndKeepsAnUploadedAvatar() throws Exception {
        Path data = Files.createTempDirectory("hdsl-profile");
        ChatStore store = new ChatStore(data);
        ChatService service = new ChatService(store, new EventBus(), data);
        try {
            JsonObject fresh = service.profile("alice");
            assertEquals("alice", fresh.get("name").getAsString());
            assertTrue(fresh.get("avatar") == null || fresh.get("avatar").isJsonNull());

            String id = new ChatAvatars(data).save(png(96, 96));
            JsonObject body = new JsonObject();
            body.addProperty("name", "Alice");
            body.addProperty("avatar", "img:" + id);
            JsonObject saved = service.updateProfile(body, "alice");
            assertEquals("Alice", saved.get("name").getAsString());
            assertEquals("img:" + id, saved.get("avatar").getAsString());

            // 名字留空 = 回到登录名
            JsonObject reset = new JsonObject();
            reset.addProperty("name", "");
            assertEquals("alice", service.updateProfile(reset, "alice").get("name").getAsString());

            // 不存在的头像 id 要拒绝，而不是存进去以后变成裂图
            JsonObject bad = new JsonObject();
            bad.addProperty("avatar", "img:" + "0".repeat(64));
            ChatException error = assertThrows(ChatException.class,
                    () -> service.updateProfile(bad, "alice"));
            assertEquals(400, error.status());

            // 助手头像同样校验
            JsonObject assistant = new JsonObject();
            assistant.addProperty("name", "测试助手");
            assistant.addProperty("avatar", "img:" + "0".repeat(64));
            assertThrows(ChatException.class, () -> service.createAssistant(assistant));
            assistant.addProperty("avatar", "img:" + id);
            assertEquals("img:" + id, service.createAssistant(assistant).avatar());

            // 引用还在的头像不会被清理
            assertEquals("img:" + id, service.profile("alice").get("avatar").getAsString());
        } finally {
            service.close();
        }
    }

    @Test
    void changingTheAvatarPrunesTheOldFile() throws Exception {
        Path data = Files.createTempDirectory("hdsl-profile-prune");
        ChatStore store = new ChatStore(data);
        ChatService service = new ChatService(store, new EventBus(), data);
        try {
            ChatAvatars avatars = new ChatAvatars(data);
            JsonObject body = new JsonObject();
            String first = avatars.save(png(64, 64));
            body.addProperty("avatar", "img:" + first);
            service.updateProfile(body, "alice");

            // 和界面一样：先上传新的，再保存资料；保存时旧图才会被清掉
            String second = avatars.save(png(128, 128));
            body.addProperty("avatar", "img:" + second);
            service.updateProfile(body, "alice");
            assertEquals("img:" + second, service.profile("alice").get("avatar").getAsString());
            assertFalse(Files.exists(data.resolve("chat").resolve("avatars").resolve(first + ".png")),
                    "换头像后旧的图片应该被清掉");
        } finally {
            service.close();
        }
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0x884422 ^ (width * 7));
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
