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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// 真实调用一次模型的两类测试（入口在聊天设置 → 模型服务）：
///
/// - 对话测试：发一条「只回复 OK」，验证 key、协议、chat path 真的能跑通，
///   而不只是 `/models` 能列出来。消耗几个 token。
/// - 识图测试：把 jar 里内置的 [#PROBE_CODE] 图片发过去，让模型读出图上的数字。
///   读对了 = 确认支持图片；明确拒收/说自己不看图 = 确认不支持；其余 = 存疑。
///
/// 只在**确定**时写能力库（source=`probe`）；存疑不落库——猜错一次下一次发图就是 400。
@NotNullByDefault
public final class ChatProbeService {

    /// 内置测试图上的数字。换图时同步改这里（图和 `vision-probe.png` 一起换）。
    static final String PROBE_CODE = "7314";

    private static final String PROBE_PROMPT =
            "这张图里有一串数字。只回复你看到的数字本身，不要解释，不要加标点。";

    private static final int MAX_ANSWER = 600;

    /// 模型明确表示「看不到图」的说法。命中按不支持处理（HTTP 200 也可能这样答）。
    private static final List<String> REFUSALS = List.of(
            "无法查看", "无法看到", "看不到图", "不能查看", "不能看到图", "不支持图片", "不支持图像",
            "无法识别图片", "无法读取图片", "没有图片", "未提供图片", "没有看到图片", "无法访问图片",
            "cannot see", "can't see", "can not see", "unable to view", "unable to see",
            "don't have access to images", "do not have access to images", "not support image",
            "does not support image", "no image provided", "no image was provided",
            "i can't view", "i cannot view", "text-only");

    /// 4xx 响应里出现这些词，说明是「端点/模型不收图」，而不是 key 错了。
    private static final List<String> IMAGE_ERROR_WORDS = List.of(
            "image", "vision", "modality", "multimodal", "content type", "image_url", "unsupported");

    private final ChatStore store;
    private final Path dataDir;
    private final Map<String, ChatProtocol> protocols = Map.of(
            ChatEndpoints.OPENAI_COMPLETIONS, new OpenAiProtocol(),
            ChatEndpoints.ANTHROPIC_MESSAGES, new AnthropicProtocol(),
            ChatEndpoints.DEEPSEEK_ANTHROPIC, new DeepseekProtocol());
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public ChatProbeService(ChatStore store, Path dataDir) {
        this.store = store;
        this.dataDir = dataDir;
    }

    /// 对话测试：`model` 为空时用已拉取列表的第一个。
    public JsonObject chatTest(String accountName, @Nullable String model,
                               ChatSettings.Settings settings)
            throws ChatException, SQLException, IOException {
        return chatTest(profile(accountName, settings), model);
    }

    JsonObject chatTest(ChatEndpoints.Profile profile, @Nullable String model)
            throws ChatException, SQLException, IOException {
        String target = model != null && !model.isBlank() ? model.trim() : pickModel(profile);
        ChatProtocol protocol = protocol(profile);
        // 8 个 token 对思考模型来说全花在 reasoning 上，正文必然为空；
        // 64 够它想完再回一个 OK，同时仍然很便宜。
        ChatProtocol.ChatRequest request = new ChatProtocol.ChatRequest(target, null,
                List.of(new ChatProtocol.ChatMessage("user", "只回复两个字母：OK", List.of())),
                null, 64, null, null);
        Call call = call(protocol, profile, request);
        JsonObject json = new JsonObject();
        json.addProperty("model", target);
        json.addProperty("ok", call.error() == null);
        json.addProperty("latencyMs", call.latencyMs());
        json.addProperty("answer", call.answer());
        json.addProperty("reasoningChars", call.reasoning().length());
        json.addProperty("empty", call.answer().isBlank() && call.reasoning().isBlank());
        if (call.status() != null) {
            json.addProperty("status", call.status());
        }
        if (call.error() != null) {
            json.addProperty("error", call.error());
        }
        return json;
    }

    /// 识图测试。判定为支持/不支持时写库（source=`probe`），存疑只回结果。
    public JsonObject visionTest(String accountName, String model, ChatSettings.Settings settings)
            throws ChatException, SQLException, IOException {
        return visionTest(profile(accountName, settings), model);
    }

    JsonObject visionTest(ChatEndpoints.Profile profile, String model)
            throws ChatException, SQLException, IOException {
        String accountName = profile.accountName();
        ChatProtocol protocol = protocol(profile);
        Path image = probeImage();
        ChatProtocol.Attachment attachment = new ChatProtocol.Attachment(
                "vision-probe.png", "image/png", "image", Files.size(image), image, null);
        // 识图测试也要给思考留空间：24 个 token 会被推理吃光，正文空 = 误判成「存疑」。
        ChatProtocol.ChatRequest request = new ChatProtocol.ChatRequest(model, null,
                List.of(new ChatProtocol.ChatMessage("user", PROBE_PROMPT, List.of(attachment))),
                null, 1_024, null, null);
        Call call = call(protocol, profile, request);

        String verdict = classify(call);
        JsonObject json = new JsonObject();
        json.addProperty("model", model);
        json.addProperty("verdict", verdict);
        json.addProperty("supported", "supported".equals(verdict));
        json.addProperty("latencyMs", call.latencyMs());
        json.addProperty("answer", call.answer());
        json.addProperty("reasoningChars", call.reasoning().length());
        if (call.status() != null) {
            json.addProperty("status", call.status());
        }
        if (call.error() != null) {
            json.addProperty("error", call.error());
        }
        json.addProperty("expected", PROBE_CODE);
        if ("supported".equals(verdict) || "unsupported".equals(verdict)) {
            ChatCapabilities.Capabilities current =
                    ChatCapabilities.resolve(store, accountName, model, profile.protocol());
            store.saveCapability(new ChatStore.Capability(accountName, model,
                    "supported".equals(verdict), current.files(), current.reasoning(),
                    current.tools(), current.maxContext(), current.maxOutput(),
                    "probe", System.currentTimeMillis()));
            json.addProperty("saved", true);
        } else {
            json.addProperty("saved", false);
        }
        return json;
    }

    private static String classify(Call call) {
        // 思考内容里读到了数字也算证据；模型可能只在 reasoning 里写答案。
        return verdict(call.status(), call.answer() + " " + call.reasoning(), call.error());
    }

    /// 把一次识图调用归成 supported / unsupported / uncertain / error。
    /// 4xx + 提到 image/vision 才叫「不支持」；429/5xx 是这次没测成，不写库。
    static String verdict(@Nullable Integer status, String answer, @Nullable String error) {
        if (error != null) {
            if (status != null && status >= 400 && status < 500 && mentionsImage(error)) {
                return "unsupported";
            }
            return "error";
        }
        String text = answer == null ? "" : answer;
        if (text.contains(PROBE_CODE)) {
            return "supported";
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String refusal : REFUSALS) {
            if (lower.contains(refusal)) {
                return "unsupported";
            }
        }
        return "uncertain";
    }

    private static boolean mentionsImage(String error) {
        String lower = error.toLowerCase(Locale.ROOT);
        for (String word : IMAGE_ERROR_WORDS) {
            if (lower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private ChatEndpoints.Profile profile(String accountName, ChatSettings.Settings settings)
            throws ChatException, SQLException, IOException {
        ChatEndpoints.Profile profile;
        try {
            profile = ChatEndpoints.profile(store, accountName);
        } catch (IllegalArgumentException e) {
            throw new ChatException(404, "account_not_found", e.getMessage());
        }
        if (profile == null) {
            throw new ChatException(404, "account_not_found", "account not found");
        }
        ChatEndpoints.checkPrivateEndpoint(profile, settings);
        return profile;
    }

    private ChatProtocol protocol(ChatEndpoints.Profile profile) throws ChatException {
        ChatProtocol protocol = protocols.get(profile.protocol());
        if (protocol == null) {
            throw new ChatException(400, "unsupported_protocol",
                    "protocol " + profile.protocol() + " is not supported");
        }
        return protocol;
    }

    private static String pickModel(ChatEndpoints.Profile profile) throws ChatException {
        if (!profile.models().isEmpty()) {
            return profile.models().get(0);
        }
        throw new ChatException(400, "no_model",
                "这个账户还没有模型列表，先「拉取模型」或指定一个模型");
    }

    private Call call(ChatProtocol protocol, ChatEndpoints.Profile profile,
                      ChatProtocol.ChatRequest request) throws ChatException {
        HttpRequest httpRequest;
        try {
            httpRequest = protocol.prepare(profile, request);
        } catch (IOException | RuntimeException e) {
            throw new ChatException(400, "bad_request", message(e));
        }
        long start = System.currentTimeMillis();
        Collector sink = new Collector();
        int status = -1;
        try {
            HttpResponse<InputStream> response = client
                    .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
                    .get(120, TimeUnit.SECONDS);
            status = response.statusCode();
            if (status < 200 || status >= 300) {
                String text;
                try (InputStream body = response.body()) {
                    text = new String(body.readNBytes(8192), StandardCharsets.UTF_8)
                            .replaceAll("\\s+", " ").trim();
                }
                return new Call(status, "", truncate(text), System.currentTimeMillis() - start, "");
            }
            try (InputStream body = response.body()) {
                protocol.read(body, sink);
            }
        } catch (TimeoutException e) {
            return new Call(null, sink.text(), "测试超时（120s），端点没有在时限内返回",
                    System.currentTimeMillis() - start, sink.reasoning());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Call(null, sink.text(), "测试被打断", System.currentTimeMillis() - start,
                    sink.reasoning());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return new Call(null, sink.text(), "请求失败：" + message(cause),
                    System.currentTimeMillis() - start, sink.reasoning());
        } catch (IOException e) {
            return new Call(status < 0 ? null : status, sink.text(), "读取响应失败：" + message(e),
                    System.currentTimeMillis() - start, sink.reasoning());
        }
        long latency = System.currentTimeMillis() - start;
        // HTTP 2xx + 流读完就算联通；正文为空是「思考吃掉了 / 端点没按 SSE 回」，
        // 由调用方展示成提示，而不是判成失败——deepseek 这类模型的 reasoning
        // 本来就可能把很小的 max_tokens 用光。
        return new Call(status, sink.text(), sink.error, latency, sink.reasoning());
    }

    private static String truncate(String text) {
        String value = text == null ? "" : text.trim();
        return value.length() <= MAX_ANSWER ? value : value.substring(0, MAX_ANSWER) + "…";
    }

    private static String message(Throwable error) {
        String text = error.getMessage();
        if (text == null || text.isBlank()) {
            return error.getClass().getSimpleName();
        }
        return text;
    }

    Path probeImage() throws IOException {
        Path dir = dataDir.resolve("chat");
        Files.createDirectories(dir);
        Path file = dir.resolve("vision-probe.png");
        if (!Files.exists(file) || Files.size(file) == 0) {
            try (InputStream in = ChatProbeService.class.getResourceAsStream("vision-probe.png")) {
                if (in == null) {
                    throw new IOException("vision probe image is missing from the jar");
                }
                Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return file;
    }

    /// 一次调用的结果。`error` 非空表示这次调用本身没成功（网络/HTTP/流错误）。
    private record Call(@Nullable Integer status, String answer, @Nullable String error,
                        long latencyMs, String reasoning) {
    }

    /// 把 SSE 的增量收成一段文本，最多留 [MAX_ANSWER] 个字符。
    private static final class Collector implements ChatProtocol.ChatEventSink {
        private final StringBuilder text = new StringBuilder();
        private final StringBuilder thinking = new StringBuilder();
        private volatile @Nullable String error;

        String reasoning() {
            return thinking.toString();
        }

        String text() {
            return text.toString();
        }

        @Override
        public void text(String delta) {
            if (delta != null && text.length() < MAX_ANSWER * 2) {
                text.append(delta);
            }
        }

        @Override
        public void reasoning(String delta) {
            // 思考挡位高的模型可能只吐 reasoning；联网测试要把它算成「有输出」，
            // 识图判定也可能在 reasoning 里读到图上的数字（最多留 8k 字）。
            if (delta != null && thinking.length() < 8_192) {
                thinking.append(delta);
            }
        }

        @Override
        public void usage(int input, int output) {
            // 测试不做用量统计
        }

        @Override
        public void done(String finishReason) {
            // 流结束
        }

        @Override
        public void error(String code, String message, boolean retryable) {
            this.error = message;
        }
    }
}