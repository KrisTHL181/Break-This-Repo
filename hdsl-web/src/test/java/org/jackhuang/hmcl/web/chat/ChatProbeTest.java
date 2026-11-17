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
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// 识图/对话测试要真的走一遍协议：假端点会说明 SSE、4xx 与「答非所问」分别该归成什么。
class ChatProbeTest {

    @Test
    void probeRunsAgainstARealOpenAiCompatibleEndpoint() throws Exception {
        AtomicReference<String> behavior = new AtomicReference<>("read");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            String mode = behavior.get();
            if ("reject".equals(mode)) {
                byte[] out = ("{\"error\":{\"message\":\"model does not support image input\"}}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, out.length);
                exchange.getResponseBody().write(out);
                exchange.close();
                return;
            }
            // thinking 模式只发 reasoning_content：思考模型把很小的 max_tokens 花光时就是这样
            String delta = "thinking".equals(mode)
                    ? "{\"reasoning_content\":\"图上的数字是 7314\"}"
                    : "{\"content\":\"" + ("read".equals(mode) ? "7314" : "这是一张白纸。") + "\"}";
            String sse = "data: {\"choices\":[{\"delta\":" + delta
                    + ",\"finish_reason\":\"stop\"}]}\n\ndata: [DONE]\n\n";
            byte[] out = sse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();

        Path data = Files.createTempDirectory("hdsl-probe");
        ChatStore store = new ChatStore(data);
        ChatProbeService probes = new ChatProbeService(store, data);
        ChatEndpoints.Profile profile = new ChatEndpoints.Profile(
                "probe-account", ChatEndpoints.OPENAI_COMPLETIONS,
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1",
                "/models", "/chat/completions", "bearer", Map.of(),
                List.of("probe-model"), null, null);
        try {
            JsonObject chat = probes.chatTest(profile, null);
            assertTrue(chat.get("ok").getAsBoolean(), chat.toString());
            assertEquals("7314", chat.get("answer").getAsString());
            assertEquals("probe-model", chat.get("model").getAsString());

            JsonObject read = probes.visionTest(profile, "probe-model");
            assertEquals("supported", read.get("verdict").getAsString());
            assertTrue(read.get("saved").getAsBoolean());
            ChatStore.Capability saved = store.capability("probe-account", "probe-model");
            assertTrue(saved.vision());
            assertEquals("probe", saved.source());

            behavior.set("reject");
            JsonObject rejected = probes.visionTest(profile, "probe-model");
            assertEquals("unsupported", rejected.get("verdict").getAsString());
            assertFalse(store.capability("probe-account", "probe-model").vision());

            // 只有 reasoning 输出时：对话测试仍算联通，识图仍能从思考里读到数字
            behavior.set("thinking");
            JsonObject thinkingChat = probes.chatTest(profile, null);
            assertTrue(thinkingChat.get("ok").getAsBoolean(), thinkingChat.toString());
            assertFalse(thinkingChat.get("empty").getAsBoolean());
            assertTrue(thinkingChat.get("reasoningChars").getAsInt() > 0);
            JsonObject thinkingVision = probes.visionTest(profile, "probe-model");
            assertEquals("supported", thinkingVision.get("verdict").getAsString());
            assertTrue(store.capability("probe-account", "probe-model").vision());

            behavior.set("paper");
            JsonObject uncertain = probes.visionTest(profile, "probe-model");
            assertEquals("uncertain", uncertain.get("verdict").getAsString());
            assertFalse(uncertain.get("saved").getAsBoolean());
            // 存疑不改库：库里的结论仍然是上一步的「支持」
            assertTrue(store.capability("probe-account", "probe-model").vision());
        } finally {
            server.stop(0);
            store.close();
        }
    }

    @Test
    void probeImageIsWrittenNextToTheChatStore() throws Exception {
        Path data = Files.createTempDirectory("hdsl-probe-image");
        ChatStore store = new ChatStore(data);
        try {
            ChatProbeService probes = new ChatProbeService(store, data);
            Path image = probes.probeImage();
            assertTrue(Files.size(image) > 1024);
            assertEquals(image, probes.probeImage());
        } finally {
            store.close();
        }
    }
}