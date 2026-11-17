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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/// models.dev 目录（[api.json](https://models.dev/api.json)）的本地缓存。
///
/// 这是内置规则表之外的另一层：能力解析顺序是
/// 手动覆盖 > 识图实测 > 供应商预设 > **模型目录** > 内置规则 > unknown。
/// 目录只在有能力数据时生效，拉不到就一直用缓存，缓存也没有就回到规则表——
/// 面板离线也能用，联网只是让新模型不必等代码更新。
///
/// 缓存是精简后的 `{"updatedAt": …, "models": {"gpt-4o": {…}}}`，不是原文件：
/// 原始 api.json 有 6 MB、236 个供应商，面板只需要每个 id 的四个字段。
@NotNullByDefault
public final class ChatModelCatalog {

    private static final URI API = URI.create("https://models.dev/api.json");
    private static final String FILE_NAME = "models.dev.json";
    /// 下载上限：原文件约 6 MB，给一倍余量即可，防止被一个坏端点灌满磁盘。
    private static final int MAX_BYTES = 16 * 1024 * 1024;

    /// 目录里每个模型的能力，外加它怎么控制思考（models.dev 的 `reasoning_options`）：
    /// kind = `toggle`（只有开关）/ `effort`（low/medium/high…）/ `budget_tokens`（数字预算）/
    /// `none`。values 是 effort 允许的词，min/max 是预算范围。
    record Entry(boolean vision, boolean tools, boolean reasoning, int maxContext, int maxOutput,
                 String reasoningKind, java.util.List<String> reasoningValues,
                 int reasoningMin, int reasoningMax) {
    }

    record Reasoning(String kind, java.util.List<String> values, int min, int max) {
        static final Reasoning NONE = new Reasoning("none", java.util.List.of(), 0, 0);
    }

    private static volatile Map<String, Entry> index = Map.of();
    private static volatile long updatedAt;
    private static volatile Path cacheDir;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ChatModelCatalog() {
    }

    /// 启动时读一次缓存；没有缓存也没关系，规则表还在。
    public static synchronized void install(Path chatDir) {
        cacheDir = chatDir;
        Path file = chatDir.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            // 指向一个没有缓存的目录就是「没有目录」：旧索引必须清掉，
            // 否则换数据目录之后还会拿着上一份快照回答。
            index = Map.of();
            updatedAt = 0;
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            JsonObject root = JsonParser.parseReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            updatedAt = root.has("updatedAt") ? root.get("updatedAt").getAsLong() : 0L;
            index = readEntries(root);
        } catch (IOException | RuntimeException e) {
            // 缓存坏了就当没有：规则表兜底，下一次刷新会覆盖它
            index = Map.of();
            updatedAt = 0;
        }
    }

    /// 缓存超过 `maxAge` 才去拉；网络失败保持现有缓存。
    public static boolean refreshIfStale(Duration maxAge) {
        Path dir = cacheDir;
        if (dir == null) {
            return false;
        }
        long age = System.currentTimeMillis() - updatedAt;
        if (!index.isEmpty() && age < maxAge.toMillis()) {
            return false;
        }
        return refresh();
    }

    /// 立即拉一次并写缓存。失败返回 false（旧缓存原样保留）。
    public static boolean refresh() {
        Path dir = cacheDir;
        if (dir == null) {
            return false;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(API)
                    .timeout(Duration.ofSeconds(300))
                    .header("Accept", "application/json")
                    .header("Accept-Encoding", "gzip")
                    .header("User-Agent", org.jackhuang.hmcl.Metadata.NAME + "/" + org.jackhuang.hmcl.Metadata.VERSION)
                    .GET()
                    .build();
            // 用 ofByteArray + 显式总时限：请求自带的 timeout 只保证响应头到达，
            // 网络慢或对端卡住时读 body 会把这个维护线程钉死（备份也跟着停）。
            HttpResponse<byte[]> response = CLIENT
                    .sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .get(300, java.util.concurrent.TimeUnit.SECONDS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return false;
            }
            byte[] bytes = response.body();
            if (bytes == null || bytes.length == 0) {
                return false;
            }
            String encoding = response.headers().firstValue("Content-Encoding").orElse("");
            if (encoding.toLowerCase(Locale.ROOT).contains("gzip")) {
                try (java.util.zip.GZIPInputStream gzip =
                             new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(bytes))) {
                    bytes = gzip.readNBytes(MAX_BYTES + 1);
                }
            }
            if (bytes.length > MAX_BYTES) {
                return false;
            }
            String text = new String(bytes, StandardCharsets.UTF_8);
            Map<String, Entry> parsed = parseApi(text);
            if (parsed.isEmpty()) {
                return false;
            }
            writeCache(dir, parsed);
            index = parsed;
            updatedAt = System.currentTimeMillis();
            return true;
        } catch (IOException | RuntimeException | InterruptedException
                 | java.util.concurrent.ExecutionException
                 | java.util.concurrent.TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    /// 目录条目（不转成能力，给思考挡位解析用）。
    static @Nullable Entry entry(String model) {
        String key = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || index.isEmpty()) {
            return null;
        }
        Entry entry = index.get(key);
        if (entry == null) {
            int slash = key.indexOf('/');
            if (slash >= 0) {
                entry = index.get(key.substring(slash + 1));
            }
        }
        return entry;
    }

    /// 目录里查不到（或还没装）返回 null，交给内置规则表。
    static @Nullable ChatCapabilities.Capabilities resolve(String model) {
        if (model == null || model.isBlank() || index.isEmpty()) {
            return null;
        }
        String key = model.trim().toLowerCase(Locale.ROOT);
        Entry entry = index.get(key);
        if (entry == null) {
            int slash = key.indexOf('/');
            if (slash >= 0) {
                entry = index.get(key.substring(slash + 1));
            }
        }
        if (entry == null) {
            int colon = key.indexOf(':');
            if (colon > 0) {
                entry = index.get(key.substring(0, colon));
            }
        }
        if (entry == null) {
            return null;
        }
        return new ChatCapabilities.Capabilities(entry.vision(), false, entry.reasoning(),
                entry.tools(), entry.maxContext(), entry.maxOutput(), "catalog");
    }

    public static int size() {
        return index.size();
    }

    public static long updatedAt() {
        return updatedAt;
    }

    /// 解析原始 api.json：`{provider: {models: {id: {modalities, tool_call, reasoning, limit}}}}`。
    private static Map<String, Entry> parseApi(String text) {
        JsonObject root = JsonParser.parseString(text).getAsJsonObject();
        Map<String, Vote> votes = new HashMap<>();
        for (Map.Entry<String, JsonElement> provider : root.entrySet()) {
            if (!provider.getValue().isJsonObject()) {
                continue;
            }
            JsonElement models = provider.getValue().getAsJsonObject().get("models");
            if (models == null || !models.isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> model : models.getAsJsonObject().entrySet()) {
                if (!model.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject info = model.getValue().getAsJsonObject();
                boolean vision = false;
                JsonElement modalities = info.get("modalities");
                if (modalities != null && modalities.isJsonObject()) {
                    JsonElement input = modalities.getAsJsonObject().get("input");
                    if (input != null && input.isJsonArray()) {
                        for (JsonElement item : input.getAsJsonArray()) {
                            if (item.isJsonPrimitive()
                                    && "image".equalsIgnoreCase(item.getAsString())) {
                                vision = true;
                            }
                        }
                    }
                }
                boolean tools = bool(info, "tool_call");
                boolean reasoning = bool(info, "reasoning");
                int context = 0;
                int output = 0;
                JsonElement limit = info.get("limit");
                if (limit != null && limit.isJsonObject()) {
                    context = intOf(limit.getAsJsonObject().get("context"));
                    output = intOf(limit.getAsJsonObject().get("output"));
                }
                Reasoning reasoningMode = reasoningOptions(info);
                votes.computeIfAbsent(model.getKey().toLowerCase(Locale.ROOT), key -> new Vote())
                        .add(vision, tools, reasoning, context, output, reasoningMode);
                JsonElement canonical = info.get("canonical_model_id");
                if (canonical != null && canonical.isJsonPrimitive()) {
                    votes.computeIfAbsent(canonical.getAsString().toLowerCase(Locale.ROOT), key -> new Vote())
                            .add(vision, tools, reasoning, context, output, reasoningMode);
                }
            }
        }
        Map<String, Entry> result = new LinkedHashMap<>();
        votes.forEach((key, vote) -> result.put(key, vote.toEntry()));
        return result;
    }

    private static Map<String, Entry> readEntries(JsonObject root) {
        Map<String, Entry> result = new HashMap<>();
        JsonElement models = root.get("models");
        if (models == null || !models.isJsonObject()) {
            return result;
        }
        for (Map.Entry<String, JsonElement> model : models.getAsJsonObject().entrySet()) {
            if (!model.getValue().isJsonObject()) {
                continue;
            }
            JsonObject info = model.getValue().getAsJsonObject();
            java.util.List<String> values = new java.util.ArrayList<>();
            JsonElement valueList = info.get("reasoningValues");
            if (valueList != null && valueList.isJsonArray()) {
                for (JsonElement item : valueList.getAsJsonArray()) {
                    if (item.isJsonPrimitive()) {
                        values.add(item.getAsString());
                    }
                }
            }
            result.put(model.getKey().toLowerCase(Locale.ROOT), new Entry(
                    bool(info, "vision"), bool(info, "tools"), bool(info, "reasoning"),
                    intOf(info.get("context")), intOf(info.get("output")),
                    string(info, "reasoningKind", "none"), java.util.List.copyOf(values),
                    intOf(info.get("reasoningMin")), intOf(info.get("reasoningMax"))));
        }
        return result;
    }

    private static void writeCache(Path dir, Map<String, Entry> models) throws IOException {
        Files.createDirectories(dir);
        JsonObject root = new JsonObject();
        root.addProperty("updatedAt", System.currentTimeMillis());
        root.addProperty("source", API.toString());
        JsonObject modelsJson = new JsonObject();
        models.forEach((id, entry) -> {
            JsonObject row = new JsonObject();
            row.addProperty("vision", entry.vision());
            row.addProperty("tools", entry.tools());
            row.addProperty("reasoning", entry.reasoning());
            row.addProperty("context", entry.maxContext());
            row.addProperty("output", entry.maxOutput());
            row.addProperty("reasoningKind", entry.reasoningKind());
            if (!entry.reasoningValues().isEmpty()) {
                com.google.gson.JsonArray values = new com.google.gson.JsonArray();
                entry.reasoningValues().forEach(values::add);
                row.add("reasoningValues", values);
            }
            if (entry.reasoningMin() > 0) {
                row.addProperty("reasoningMin", entry.reasoningMin());
            }
            if (entry.reasoningMax() > 0) {
                row.addProperty("reasoningMax", entry.reasoningMax());
            }
            modelsJson.add(id, row);
        });
        root.add("models", modelsJson);
        Path file = dir.resolve(FILE_NAME);
        Path temp = dir.resolve(FILE_NAME + ".tmp");
        Files.writeString(temp, root.toString(), StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /// models.dev 的 `reasoning_options`：可能给 effort 词表、budget_tokens 范围或 toggle。
    private static Reasoning reasoningOptions(JsonObject info) {
        JsonElement options = info.get("reasoning_options");
        if (options == null || !options.isJsonArray()) {
            return Reasoning.NONE;
        }
        Reasoning best = Reasoning.NONE;
        for (JsonElement element : options.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject option = element.getAsJsonObject();
            String type = string(option, "type", "");
            if ("effort".equals(type)) {
                java.util.List<String> values = new java.util.ArrayList<>();
                JsonElement list = option.get("values");
                if (list != null && list.isJsonArray()) {
                    for (JsonElement item : list.getAsJsonArray()) {
                        if (item.isJsonPrimitive()) {
                            values.add(item.getAsString().toLowerCase(Locale.ROOT));
                        }
                    }
                }
                return new Reasoning("effort", java.util.List.copyOf(values), 0, 0);
            }
            if ("budget_tokens".equals(type)) {
                best = new Reasoning("budget", java.util.List.of(),
                        intOf(option.get("min")), intOf(option.get("max")));
            } else if ("toggle".equals(type) && best.kind().equals("none")) {
                best = new Reasoning("toggle", java.util.List.of(), 0, 0);
            }
        }
        return best;
    }

    private static String string(JsonObject object, String name, String fallback) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static boolean bool(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return false;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static int intOf(@Nullable JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return 0;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /// 同一个 id 会出现在多个供应商下：能力按多数票。
    /// 上下文/输出取**最小正值**：目录里同一个模型有小窗口和大窗口两份报价时，
    /// 按大的算会让面板把超长上下文发出去，直接换来 400；按小的算只是少用一点。
    private static final class Vote {
        private int total;
        private int vision;
        private int tools;
        private int reasoning;
        private int context;
        private int output;
        private final java.util.Map<String, Integer> modeVotes = new java.util.HashMap<>();
        private final java.util.Map<String, Reasoning> modes = new java.util.HashMap<>();

        void add(boolean hasVision, boolean hasTools, boolean hasReasoning, int ctx, int out,
                 Reasoning mode) {
            total++;
            if (hasVision) {
                vision++;
            }
            if (hasTools) {
                tools++;
            }
            if (hasReasoning) {
                reasoning++;
            }
            context = minPositive(context, ctx);
            output = minPositive(output, out);
            if (!mode.kind().equals("none")) {
                // 同一 id 在多个供应商下的声明可能不同（有的只给 high/max，有的给全套）。
                // 取**出现最多**的那份，而不是并集：并集等于「哪家宽松就按哪家」，
                // 会把 only-high 的模型误判成可以关、可以选低。
                String key = mode.kind() + "|" + mode.values() + "|" + mode.min() + "|" + mode.max();
                modeVotes.merge(key, 1, Integer::sum);
                modes.put(key, mode);
            }
        }

        Entry toEntry() {
            Reasoning reasoningMode = dominantMode();
            return new Entry(vision * 2 > total, tools * 2 > total, reasoning * 2 > total,
                    context > 0 ? context : 128_000, output > 0 ? output : 16_384,
                    reasoningMode.kind(), reasoningMode.values(),
                    reasoningMode.min(), reasoningMode.max());
        }

        private Reasoning dominantMode() {
            Reasoning best = Reasoning.NONE;
            int bestVotes = 0;
            for (java.util.Map.Entry<String, Integer> entry : modeVotes.entrySet()) {
                Reasoning candidate = modes.get(entry.getKey());
                int votes = entry.getValue();
                // 票数相同取更严格的一份（词表更短）
                if (votes > bestVotes || (votes == bestVotes
                        && candidate.values().size() < best.values().size())) {
                    best = candidate;
                    bestVotes = votes;
                }
            }
            return best;
        }

        private static int minPositive(int current, int value) {
            if (value <= 0) {
                return current;
            }
            return current <= 0 ? value : Math.min(current, value);
        }
    }
}