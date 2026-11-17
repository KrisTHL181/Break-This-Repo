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
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// DeepSeek 预设开关：默认开启、PATCH 能关掉、旧设置文件缺字段时仍为开启。
class ChatSettingsTest {

    @Test
    void deepseekPresetDefaultsOnAndCanBeDisabled(@TempDir Path data) throws Exception {
        ChatStore store = new ChatStore(data);
        try {
            ChatSettings.Settings defaults = ChatSettings.defaults(data);
            assertTrue(defaults.deepseekPresetEnabled());

            ChatSettings.Settings off = ChatSettings.merge(defaults,
                    JsonParser.parseString("{\"deepseekPresetEnabled\":false}").getAsJsonObject(),
                    defaults);
            assertFalse(off.deepseekPresetEnabled());

            ChatSettings.save(store, off);
            assertFalse(ChatSettings.load(store, data).deepseekPresetEnabled());

            JsonObject legacy = JsonParser.parseString("{\"searchProvider\":\"bocha\"}").getAsJsonObject();
            ChatSettings.Settings loaded = ChatSettings.merge(defaults, legacy, defaults);
            assertTrue(loaded.deepseekPresetEnabled());
        } finally {
            store.close();
        }
    }

    @Test
    void agentChatLimitDefaultsAndClamps(@TempDir Path data) {
        ChatSettings.Settings defaults = ChatSettings.defaults(data);
        assertEquals(8192, defaults.agentChatMaxInputTokens());

        ChatSettings.Settings custom = ChatSettings.merge(defaults,
                JsonParser.parseString("{\"agentChatMaxInputTokens\":2048}").getAsJsonObject(),
                defaults);
        assertEquals(2048, custom.agentChatMaxInputTokens());

        ChatSettings.Settings tooLow = ChatSettings.merge(defaults,
                JsonParser.parseString("{\"agentChatMaxInputTokens\":1}").getAsJsonObject(),
                defaults);
        assertEquals(1024, tooLow.agentChatMaxInputTokens());

        ChatSettings.Settings tooHigh = ChatSettings.merge(defaults,
                JsonParser.parseString("{\"agentChatMaxInputTokens\":999999}").getAsJsonObject(),
                defaults);
        assertEquals(131072, tooHigh.agentChatMaxInputTokens());
    }
}