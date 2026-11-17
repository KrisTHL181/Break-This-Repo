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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.dsh.DshVendor;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// 账户 → 聊天端点（docs/chat.md §4）。
///
/// 路径是**数据**：内置供应商有一张默认表，用户可以在聊天设置里覆盖。运行时绝不
/// 用 `base + "/v1/chat/completions"` 猜——猜错的形态是一个 404，而用户只会看到
/// “模型不回话”。
@NotNullByDefault
public final class ChatEndpoints {

    public static final String OPENAI_COMPLETIONS = "openai-completions";
    public static final String ANTHROPIC_MESSAGES = "anthropic-messages";
    /// DeepSeek 官方的 Anthropic 兼容扩展模式（服务端搜索 / 读链接 / 引用）。
    public static final String DEEPSEEK_ANTHROPIC = "deepseek-anthropic";

    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(20);
    private static final String USER_AGENT = Metadata.NAME + "/" + Metadata.VERSION;

    private ChatEndpoints() {
    }

    public record Profile(
            String accountName, String protocol, String base, String modelsPath, String chatPath,
            String authStyle, Map<String, String> extraHeaders,
            List<String> models, @Nullable Long modelsFetchedAt,
            @Nullable String lastProbeJson) {

        public URI modelsUri() {
            return URI.create(base + modelsPath);
        }

        public URI chatUri() {
            return URI.create(base + chatPath);
        }

        public JsonObject resolvedJson() {
            JsonObject json = new JsonObject();
            json.addProperty("modelsUrl", modelsUri().toString());
            json.addProperty("chatUrl", chatUri().toString());
            return json;
        }
    }

    public record Probe(
            boolean reachable, boolean tlsOk, boolean authOk, boolean modelsOk,
            List<String> models, long latencyMs, Map<String, String> resolved,
            List<String> warnings, @Nullable String error) {
    }

    public static @Nullable DshAccount account(String name) {
        DshAccount byLabel = null;
        int labelMatches = 0;
        String wantedLabel = name;
        int bar = name == null ? -1 : name.indexOf('|');
        if (bar >= 0) {
            wantedLabel = name.substring(bar + 1);
        }
        for (DshAccount account : SettingsManager.settings().getAccounts()) {
            if (account.matchesKey(name)) {
                return account;
            }
            // 前端缓存的账户 key 可能带着旧的 vendor 前缀（例如
            // alibaba-cn|alibaba-token-plan），而账户 label 仍然是 alibaba-token-plan。
            // 只在 label 唯一时兜底，避免两家同名账户被误选。
            if (account.label() != null && account.label().equals(wantedLabel)) {
                byLabel = account;
                labelMatches++;
            }
        }
        return labelMatches == 1 ? byLabel : null;
    }

    public static List<DshAccount> accounts() {
        return SettingsManager.settings().getAccounts();
    }

    public static String defaultProtocol(DshAccount account) {
        ChatStore.AccountProfile ignored = null;
        DshVendor vendor = account.vendor();
        String api = vendor == null ? OPENAI_COMPLETIONS : vendor.api();
        // v1 只实现两种协议；OpenAI 的 responses 协议先按 completions 走，
        // 用户仍可在聊天设置里显式改回（改回后会得到 unsupported_protocol）。
        if (OPENAI_COMPLETIONS.equals(api) || ANTHROPIC_MESSAGES.equals(api)) {
            return api;
        }
        return OPENAI_COMPLETIONS;
    }

    /// 解析一个账户的聊天端点；`null` 表示账户不存在。
    public static @Nullable Profile profile(ChatStore store, String accountName) throws SQLException {
        DshAccount account = account(accountName);
        if (account == null) {
            return null;
        }
        ChatStore.AccountProfile saved = store.profile(account.key());
        String protocol = saved != null && nonBlank(saved.protocol())
                ? saved.protocol().trim() : defaultProtocol(account);
        String base = saved != null && nonBlank(saved.baseOverride())
                ? saved.baseOverride().trim() : account.endpoint();
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("account " + account.key() + " has no endpoint");
        }
        base = normalizeBase(base);
        String[] paths = defaultPaths(account, protocol, base);
        String modelsPath = saved != null && nonBlank(saved.modelsPath())
                ? normalizePath(saved.modelsPath()) : paths[0];
        String chatPath = saved != null && nonBlank(saved.chatPath())
                ? normalizePath(saved.chatPath()) : paths[1];
        String authStyle = saved != null && nonBlank(saved.authStyle())
                ? saved.authStyle().trim() : defaultAuthStyle(protocol);
        Map<String, String> headers = readHeaders(saved == null ? null : saved.extraHeadersJson());
        List<String> models = readModels(saved == null ? null : saved.modelsJson());
        return new Profile(account.key(), protocol, base, modelsPath, chatPath, authStyle,
                headers, models, saved == null ? null : saved.modelsFetchedAt(),
                saved == null ? null : saved.lastProbeJson());
    }

    /// DeepSeek 官方预设使用的端点：复用账户的 base/key 与已有模型列表，但协议固定为
    /// `deepseek-anthropic`，chat path 走 `/anthropic/v1/messages`。
    public static Profile deepseekProfile(ChatStore store, DshAccount account) throws SQLException {
        Profile normal = profile(store, account.key());
        String base = normal != null && normal.base() != null && !normal.base().isBlank()
                ? normal.base() : account.endpoint();
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("account " + account.key() + " has no endpoint");
        }
        base = normalizeBase(base);
        String modelsPath = normal != null && nonBlank(normal.modelsPath())
                ? normal.modelsPath() : "/models";
        return new Profile(account.key(), DEEPSEEK_ANTHROPIC, base, modelsPath,
                "/anthropic/v1/messages", "bearer",
                normal == null ? Map.of() : normal.extraHeaders(),
                normal == null ? List.of() : normal.models(),
                normal == null ? null : normal.modelsFetchedAt(),
                normal == null ? null : normal.lastProbeJson());
    }

    /// 默认路径表。顺序：供应商特例 → 协议通用。返回 `{modelsPath, chatPath}`。
    public static String[] defaultPaths(DshAccount account, String protocol, String base) {
        String vendor = account.vendorId() == null ? "" : account.vendorId().toLowerCase(Locale.ROOT);
        if (ANTHROPIC_MESSAGES.equals(protocol)) {
            String lower = base.toLowerCase(Locale.ROOT);
            if (lower.contains("anthropic.com")) {
                return new String[]{"/v1/models?limit=1000", "/v1/messages"};
            }
            String path = URI.create(base).getPath() == null ? "" : URI.create(base).getPath();
            if (path.matches(".*/v[0-9]+$")) {
                return new String[]{"/models", "/messages"};
            }
            return new String[]{"/v1/models?limit=1000", "/v1/messages"};
        }
        switch (vendor) {
            case "mistral":
                return new String[]{"/v1/models", "/v1/chat/completions"};
            case "google":
                return new String[]{"/openai/models", "/openai/chat/completions"};
            default:
                break;
        }
        String path = URI.create(base).getPath() == null ? "" : URI.create(base).getPath();
        if (path.matches(".*/v[0-9]+$")) {
            return new String[]{"/models", "/chat/completions"};
        }
        return new String[]{"/models", "/chat/completions"};
    }

    public static String defaultAuthStyle(String protocol) {
        return ANTHROPIC_MESSAGES.equals(protocol) ? "anthropic" : "bearer";
    }

    public static Profile saveProfile(ChatStore store, DshAccount account,
                                      @Nullable String protocol, @Nullable String baseOverride,
                                      @Nullable String modelsPath, @Nullable String chatPath,
                                      @Nullable String authStyle,
                                      @Nullable Map<String, String> extraHeaders) throws SQLException {
        ChatStore.AccountProfile existing = store.profile(account.key());
        String resolvedProtocol = protocol != null && !protocol.isBlank()
                ? protocol.trim() : (existing == null ? defaultProtocol(account) : existing.protocol());
        if (resolvedProtocol == null || resolvedProtocol.isBlank()) {
            resolvedProtocol = OPENAI_COMPLETIONS;
        }
        if (!OPENAI_COMPLETIONS.equals(resolvedProtocol) && !ANTHROPIC_MESSAGES.equals(resolvedProtocol)) {
            throw new IllegalArgumentException("unsupported protocol: " + resolvedProtocol
                    + " (v1 speaks " + OPENAI_COMPLETIONS + " and " + ANTHROPIC_MESSAGES + ")");
        }
        String base = baseOverride != null ? normalizeBase(baseOverride)
                : existing != null && nonBlank(existing.baseOverride()) ? existing.baseOverride()
                : account.endpoint();
        if (base == null || base.isBlank()) {
            throw new IllegalArgumentException("an endpoint is required");
        }
        base = normalizeBase(base);
        String[] defaults = defaultPaths(account, resolvedProtocol, base);
        String models = modelsPath != null && !modelsPath.isBlank()
                ? normalizePath(modelsPath) : existing != null && nonBlank(existing.modelsPath())
                ? existing.modelsPath() : defaults[0];
        String chat = chatPath != null && !chatPath.isBlank()
                ? normalizePath(chatPath) : existing != null && nonBlank(existing.chatPath())
                ? existing.chatPath() : defaults[1];
        String auth = authStyle != null && !authStyle.isBlank()
                ? authStyle.trim() : existing != null && nonBlank(existing.authStyle())
                ? existing.authStyle() : defaultAuthStyle(resolvedProtocol);
        String headersJson = extraHeaders == null
                ? (existing == null ? "{}" : existing.extraHeadersJson())
                : org.jackhuang.hmcl.web.http.Json.GSON.toJson(extraHeaders);
        ChatStore.AccountProfile profile = new ChatStore.AccountProfile(account.key(), resolvedProtocol,
                base, models, chat, auth, headersJson,
                existing == null ? "[]" : existing.modelsJson(),
                existing == null ? null : existing.modelsFetchedAt(),
                existing == null ? null : existing.lastProbeJson(), System.currentTimeMillis());
        store.saveProfile(profile);
        return new Profile(account.key(), resolvedProtocol, base, models, chat, auth,
                extraHeaders == null ? readHeaders(headersJson) : extraHeaders,
                readModels(profile.modelsJson()), profile.modelsFetchedAt(), profile.lastProbeJson());
    }

    // ----------------------------------------------------------- validation --

    public static String normalizeBase(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("an endpoint is required");
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("not a valid URL: " + value);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("the endpoint must be http or https");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("the endpoint has no host");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("the endpoint must not carry userinfo");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("the endpoint must not carry a query or fragment");
        }
        String normalized = uri.toString();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static String normalizePath(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!value.startsWith("/")) {
            throw new IllegalArgumentException("a path must start with /");
        }
        if (value.contains("#")) {
            throw new IllegalArgumentException("a path must not carry a fragment");
        }
        for (String part : value.split("/")) {
            if (part.equals("..")) {
                throw new IllegalArgumentException("a path must not contain ..");
            }
        }
        return value;
    }

    /// `strict` 模式下拒绝 loopback / link-local / 私网地址，除非命中 allowlist。
    /// 默认 `allow`，因为 Ollama / LM Studio / vLLM 就住在这些地址上。
    public static void checkPrivateEndpoint(Profile profile, ChatSettings.Settings settings)
            throws IOException {
        if (!"strict".equals(settings.privateEndpoints())) {
            return;
        }
        URI uri = URI.create(profile.base());
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("the endpoint has no host");
        }
        if (allowed(host, settings.privateAllowlist())) {
            return;
        }
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new IOException("cannot resolve " + host, e);
        }
        for (InetAddress address : addresses) {
            if (isPrivate(address)) {
                throw new IllegalArgumentException("private_endpoint_refused: " + host
                        + " resolves to a private address; allow it in chat settings or switch"
                        + " private endpoints to allow");
            }
        }
    }

    static boolean allowed(String host, List<String> allowlist) {
        String lower = host.toLowerCase(Locale.ROOT);
        for (String entry : allowlist) {
            String value = entry.toLowerCase(Locale.ROOT);
            if (value.startsWith(".")) {
                if (lower.endsWith(value) || lower.equals(value.substring(1))) {
                    return true;
                }
            } else if (lower.equals(value) || lower.endsWith("." + value)) {
                return true;
            }
        }
        return false;
    }

    static boolean isPrivate(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            // fc00::/7 (unique local) and fe80::/10 are covered above for link-local.
            int first = bytes[0] & 0xff;
            return (first & 0xfe) == 0xfc;
        }
        if (bytes.length == 4) {
            int first = bytes[0] & 0xff;
            int second = bytes[1] & 0xff;
            // 100.64.0.0/10 — carrier-grade NAT, which is still not the public internet.
            return first == 100 && second >= 64 && second <= 127;
        }
        return false;
    }

    // --------------------------------------------------------------- probe --

    public static Probe probe(ChatStore store, String accountName, ChatSettings.Settings settings)
            throws SQLException, IOException {
        Profile profile = profile(store, accountName);
        if (profile == null) {
            throw new IllegalArgumentException("account not found");
        }
        checkPrivateEndpoint(profile, settings);
        List<String> warnings = new ArrayList<>();
        long start = System.currentTimeMillis();
        boolean reachable = false;
        boolean tlsOk = true;
        boolean authOk = false;
        boolean modelsOk = false;
        List<String> models = List.of();
        String error = null;

        HttpRequest.Builder builder = HttpRequest.newBuilder(profile.modelsUri())
                .timeout(PROBE_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET();
        applyAuth(builder, profile);
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(PROBE_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            try {
                HttpResponse<String> response = client.send(builder.build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                reachable = true;
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    authOk = true;
                    models = readModelIds(response.body());
                    modelsOk = !models.isEmpty();
                    if (models.isEmpty()) {
                        warnings.add("endpoint answered 2xx but listed no models");
                    }
                } else if (status == 401 || status == 403) {
                    authOk = false;
                    error = "the endpoint rejected the key (HTTP " + status + ")" + excerpt(response.body());
                } else if (status == 404 || status == 405 || status == 400) {
                    authOk = true;
                    error = "the endpoint answered HTTP " + status + " for the model list;"
                            + " the chat path may still work" + excerpt(response.body());
                    warnings.add("models endpoint is not available");
                } else {
                    authOk = true;
                    error = "the endpoint answered HTTP " + status + excerpt(response.body());
                }
            } finally {
                org.jackhuang.hmcl.util.io.HttpClients.closeQuietly(client);
            }
        } catch (SSLException e) {
            error = "TLS failed: " + message(e);
            tlsOk = false;
        } catch (IOException e) {
            error = "cannot reach " + profile.modelsUri() + ": " + message(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error = "interrupted";
        } catch (RuntimeException e) {
            error = message(e);
        }
        long latency = System.currentTimeMillis() - start;

        if (!models.isEmpty()) {
            saveModels(store, profile, models);
        }
        JsonObject probeJson = new JsonObject();
        probeJson.addProperty("reachable", reachable);
        probeJson.addProperty("tlsOk", tlsOk);
        probeJson.addProperty("authOk", authOk);
        probeJson.addProperty("modelsOk", modelsOk);
        probeJson.addProperty("latencyMs", latency);
        probeJson.addProperty("at", System.currentTimeMillis());
        com.google.gson.JsonArray probeModels = new com.google.gson.JsonArray();
        models.forEach(probeModels::add);
        probeJson.add("models", probeModels);
        com.google.gson.JsonArray probeWarnings = new com.google.gson.JsonArray();
        warnings.forEach(probeWarnings::add);
        probeJson.add("warnings", probeWarnings);
        JsonObject resolved = new JsonObject();
        resolved.addProperty("modelsUrl", profile.modelsUri().toString());
        resolved.addProperty("chatUrl", profile.chatUri().toString());
        probeJson.add("resolved", resolved);
        if (error != null) {
            probeJson.addProperty("error", error);
        }
        ChatStore.AccountProfile saved = store.profile(profile.accountName());
        if (saved != null) {
            store.saveProfile(new ChatStore.AccountProfile(saved.accountName(), saved.protocol(),
                    saved.baseOverride(), saved.modelsPath(), saved.chatPath(), saved.authStyle(),
                    saved.extraHeadersJson(), saved.modelsJson(), saved.modelsFetchedAt(),
                    probeJson.toString(), System.currentTimeMillis()));
        }
        return new Probe(reachable, tlsOk, authOk, modelsOk, models, latency,
                Map.of("modelsUrl", profile.modelsUri().toString(),
                        "chatUrl", profile.chatUri().toString()),
                warnings, error);
    }

    public static List<String> refreshModels(ChatStore store, String accountName,
                                             ChatSettings.Settings settings)
            throws SQLException, IOException {
        Profile profile = profile(store, accountName);
        if (profile == null) {
            throw new IllegalArgumentException("account not found");
        }
        checkPrivateEndpoint(profile, settings);
        HttpRequest.Builder builder = HttpRequest.newBuilder(profile.modelsUri())
                .timeout(PROBE_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET();
        applyAuth(builder, profile);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(PROBE_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        try {
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("the endpoint answered HTTP " + response.statusCode()
                        + excerpt(response.body()));
            }
            List<String> models = readModelIds(response.body());
            saveModels(store, profile, models);
            return models;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted", e);
        } finally {
            org.jackhuang.hmcl.util.io.HttpClients.closeQuietly(client);
        }
    }

    private static void saveModels(ChatStore store, Profile profile, List<String> models)
            throws SQLException {
        ChatStore.AccountProfile saved = store.profile(profile.accountName());
        if (saved == null) {
            saved = new ChatStore.AccountProfile(profile.accountName(), profile.protocol(),
                    profile.base(), profile.modelsPath(), profile.chatPath(), profile.authStyle(),
                    org.jackhuang.hmcl.web.http.Json.GSON.toJson(profile.extraHeaders()),
                    "[]", null, null, System.currentTimeMillis());
        }
        store.saveProfile(new ChatStore.AccountProfile(saved.accountName(), saved.protocol(),
                saved.baseOverride(), saved.modelsPath(), saved.chatPath(), saved.authStyle(),
                saved.extraHeadersJson(), org.jackhuang.hmcl.web.http.Json.GSON.toJson(models),
                System.currentTimeMillis(), saved.lastProbeJson(), System.currentTimeMillis()));
    }

    static void applyAuth(HttpRequest.Builder builder, Profile profile) {
        String style = profile.authStyle() == null ? defaultAuthStyle(profile.protocol())
                : profile.authStyle().toLowerCase(Locale.ROOT);
        String key = accountKey(profile.accountName());
        if ("anthropic".equals(style) || "x-api-key".equals(style)) {
            builder.header("x-api-key", key);
            builder.header("anthropic-version", "2023-06-01");
        } else {
            builder.header("Authorization", "Bearer " + key);
        }
        profile.extraHeaders().forEach(builder::header);
    }

    private static String accountKey(String accountName) {
        DshAccount account = account(accountName);
        return account == null || account.apiKey() == null ? "" : account.apiKey().trim();
    }

    static List<String> readModelIds(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return List.of();
            }
            JsonObject root = parsed.getAsJsonObject();
            List<String> ids = new ArrayList<>();
            JsonElement data = root.get("data");
            if (data != null && data.isJsonArray()) {
                for (JsonElement element : data.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        addId(ids, idOf(element.getAsJsonObject()));
                    }
                }
            } else {
                JsonElement models = root.get("models");
                if (models != null && models.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> entry : models.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            addId(ids, entry.getKey().isBlank()
                                    ? idOf(entry.getValue().getAsJsonObject()) : entry.getKey());
                        }
                    }
                } else if (models != null && models.isJsonArray()) {
                    for (JsonElement element : models.getAsJsonArray()) {
                        if (element.isJsonObject()) {
                            JsonObject object = element.getAsJsonObject();
                            addId(ids, idOf(object));
                            if (ids.isEmpty()) {
                                addId(ids, stringOf(object.get("name")));
                            }
                        } else if (element.isJsonPrimitive()) {
                            addId(ids, element.getAsString());
                        }
                    }
                }
            }
            return List.copyOf(ids);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private static @Nullable String idOf(JsonObject object) {
        return stringOf(object.get("id"));
    }

    private static @Nullable String stringOf(@Nullable JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static void addId(List<String> ids, @Nullable String id) {
        if (id != null && !id.isBlank() && !ids.contains(id)) {
            ids.add(id);
        }
    }

    static List<String> readModels(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonArray()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    result.add(element.getAsString());
                }
            }
            return List.copyOf(result);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    static Map<String, String> readHeaders(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Map.of();
            }
            Map<String, String> headers = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    headers.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Map.copyOf(headers);
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    private static boolean nonBlank(@Nullable String value) {
        return value != null && !value.isBlank();
    }

    private static String message(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName() : e.getMessage();
    }

    private static String excerpt(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String text = body.replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? "" : ": " + (text.length() > 200 ? text.substring(0, 200) + "…" : text);
    }

    /// 把一个账户的配置写成 API 形状。key 永远不出现。
    public static JsonObject profileJson(ChatStore store, DshAccount account,
                                         ChatSettings.Settings settings) throws SQLException {
        Profile profile = profile(store, account.key());
        JsonObject json = new JsonObject();
        json.addProperty("name", account.key());
        json.addProperty("vendor", account.vendorId());
        json.addProperty("label", account.label() == null ? account.key() : account.label());
        json.addProperty("endpoint", account.endpoint());
        json.add("model", account.model() == null
                ? com.google.gson.JsonNull.INSTANCE
                : new com.google.gson.JsonPrimitive(account.model()));
        String key = account.apiKey() == null ? "" : account.apiKey().trim();
        json.add("maskedKey", key.isEmpty()
                ? com.google.gson.JsonNull.INSTANCE
                : new com.google.gson.JsonPrimitive(key.substring(0, Math.min(8, key.length())) + "…"));
        if (profile == null) {
            json.addProperty("protocol", defaultProtocol(account));
            json.addProperty("configured", false);
            return json;
        }
        json.addProperty("protocol", profile.protocol());
        json.addProperty("base", profile.base());
        json.addProperty("modelsPath", profile.modelsPath());
        json.addProperty("chatPath", profile.chatPath());
        json.addProperty("authStyle", profile.authStyle());
        json.addProperty("configured", store.profile(account.key()) != null);
        json.add("resolved", profile.resolvedJson());
        JsonArray models = new JsonArray();
        profile.models().forEach(models::add);
        json.add("models", models);
        if (profile.modelsFetchedAt() != null) {
            json.addProperty("modelsFetchedAt", profile.modelsFetchedAt());
        }
        json.add("extraHeaders", org.jackhuang.hmcl.web.http.Json.GSON.toJsonTree(profile.extraHeaders()));
        if (profile.lastProbeJson() != null) {
            try {
                json.add("lastProbe", JsonParser.parseString(profile.lastProbeJson()));
            } catch (RuntimeException ignored) {
                // A malformed cached probe is not worth failing the list.
            }
        }
        return json;
    }
}