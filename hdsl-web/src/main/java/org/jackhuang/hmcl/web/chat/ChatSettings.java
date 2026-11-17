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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/// 聊天设置（docs/chat.md §6.3/§6.4/§9/§12.4）。
///
/// 只影响聊天，不写回主界面的账户页：账户仍归 dsh 使用，这里只记住聊天自己的
/// 协议、路径、保留策略、备份与私网策略。
@NotNullByDefault
public final class ChatSettings {

    private static final String KEY = "settings";
    private static final Set<String> RETENTIONS =
            Set.of("forever", "30d", "90d", "180d", "365d", "custom");
    private static final Set<String> PRIVATE_MODES = Set.of("allow", "strict");

    public record Settings(
            String retention,
            int retentionDays,
            boolean backupEnabled,
            String backupTime,
            int backupKeep,
            String backupDir,
            String privateEndpoints,
            List<String> privateAllowlist,
            int longTextTokens,
            int dailyTokenLimit,
            int streamFlushMs,
            @Nullable String defaultAccount,
            @Nullable String defaultModel,
            String searchProvider,
            java.util.Map<String, String> searchKeys,
            int searchMaxResults,
            /// 把这一轮的搜索/抓正文链路存进会话，追问时模型能看到上一轮搜过什么。
            boolean persistToolTrail,
            /// 是否在角色列表里启用内置的 DeepSeek 官方预设助手。
            boolean deepseekPresetEnabled,
            /// agent 票的 chat-write 单次输入上限（估算 token）；0 = 不限制。
            int agentChatMaxInputTokens) {

        public Settings(String retention, int retentionDays, boolean backupEnabled,
                        String backupTime, int backupKeep, String backupDir,
                        String privateEndpoints, List<String> privateAllowlist,
                        int longTextTokens, int dailyTokenLimit, int streamFlushMs,
                        @Nullable String defaultAccount, @Nullable String defaultModel,
                        String searchProvider, java.util.Map<String, String> searchKeys,
                        int searchMaxResults, boolean persistToolTrail) {
            this(retention, retentionDays, backupEnabled, backupTime, backupKeep, backupDir,
                    privateEndpoints, privateAllowlist, longTextTokens, dailyTokenLimit,
                    streamFlushMs, defaultAccount, defaultModel, searchProvider, searchKeys,
                    searchMaxResults, persistToolTrail, true, 8192);
        }

        /// 某个服务商的 key；没有配置返回 null。
        public @Nullable String searchKey(String provider) {
            String value = searchKeys.get(provider);
            return value == null || value.isBlank() ? null : value;
        }

        public long retentionCutoffMillis() {
            return switch (retention) {
                case "30d" -> System.currentTimeMillis() - 30L * 24 * 3600 * 1000;
                case "90d" -> System.currentTimeMillis() - 90L * 24 * 3600 * 1000;
                case "180d" -> System.currentTimeMillis() - 180L * 24 * 3600 * 1000;
                case "365d" -> System.currentTimeMillis() - 365L * 24 * 3600 * 1000;
                case "custom" -> System.currentTimeMillis() - Math.max(1, retentionDays) * 24L * 3600 * 1000;
                default -> 0;
            };
        }

        public @Nullable LocalTime backupLocalTime() {
            try {
                return LocalTime.parse(backupTime);
            } catch (DateTimeParseException e) {
                return LocalTime.of(3, 17);
            }
        }

        public Settings withBackupDir(String dir) {
            return new Settings(retention, retentionDays, backupEnabled, backupTime, backupKeep, dir,
                    privateEndpoints, privateAllowlist, longTextTokens, dailyTokenLimit,
                    streamFlushMs, defaultAccount, defaultModel, searchProvider, searchKeys,
                    searchMaxResults, persistToolTrail, deepseekPresetEnabled,
                    agentChatMaxInputTokens);
        }
    }

    private ChatSettings() {
    }

    public static Settings defaults(Path dataDir) {
        Path backup = Path.of("/data/backup");
        if (!dataDir.getFileSystem().getSeparator().equals("/") || !java.nio.file.Files.isDirectory(Path.of("/data"))) {
            backup = dataDir.resolve("backup");
        }
        return new Settings("forever", 0, true, "03:17", 7, backup.toString(), "allow",
                List.of(), 4000, 0, 40, null, null, "bocha", java.util.Map.of(), 5, true, true, 8192);
    }

    public static Settings load(ChatStore store, Path dataDir) throws SQLException {
        Settings defaults = defaults(dataDir);
        String raw = store.setting(KEY);
        if (raw == null || raw.isBlank()) {
            return defaults;
        }
        try {
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            String retention = string(json, "retention", defaults.retention());
            if (!RETENTIONS.contains(retention)) {
                retention = defaults.retention();
            }
            String privateMode = string(json, "privateEndpoints", defaults.privateEndpoints());
            if (!PRIVATE_MODES.contains(privateMode)) {
                privateMode = defaults.privateEndpoints();
            }
            List<String> allowlist = new ArrayList<>();
            JsonElement allow = json.get("privateAllowlist");
            if (allow != null && allow.isJsonArray()) {
                for (JsonElement element : allow.getAsJsonArray()) {
                    if (element.isJsonPrimitive()) {
                        String value = element.getAsString().trim();
                        if (!value.isEmpty()) {
                            allowlist.add(value);
                        }
                    }
                }
            }
            String searchProvider = string(json, "searchProvider", defaults.searchProvider());
            java.util.Map<String, String> searchKeys = new java.util.LinkedHashMap<>();
            JsonElement keysElement = json.get("searchKeys");
            if (keysElement != null && keysElement.isJsonObject()) {
                for (var entry : keysElement.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        String value = entry.getValue().getAsString().trim();
                        if (!value.isEmpty()) {
                            searchKeys.put(entry.getKey().toLowerCase(java.util.Locale.ROOT), value);
                        }
                    }
                }
            }
            // 旧格式只有一个 searchApiKey：认给它当时选中的服务商
            String legacyKey = nullableString(json, "searchApiKey");
            if (legacyKey != null) {
                searchKeys.putIfAbsent(searchProvider, legacyKey);
            }
            return sanitize(new Settings(
                    retention,
                    intValue(json, "retentionDays", defaults.retentionDays()),
                    bool(json, "backupEnabled", defaults.backupEnabled()),
                    string(json, "backupTime", defaults.backupTime()),
                    intValue(json, "backupKeep", defaults.backupKeep()),
                    string(json, "backupDir", defaults.backupDir()),
                    privateMode,
                    List.copyOf(allowlist),
                    intValue(json, "longTextTokens", defaults.longTextTokens()),
                    intValue(json, "dailyTokenLimit", defaults.dailyTokenLimit()),
                    intValue(json, "streamFlushMs", defaults.streamFlushMs()),
                    nullableString(json, "defaultAccount"),
                    nullableString(json, "defaultModel"),
                    searchProvider,
                    searchKeys,
                    intValue(json, "searchMaxResults", defaults.searchMaxResults()),
                    bool(json, "persistToolTrail", defaults.persistToolTrail()),
                    bool(json, "deepseekPresetEnabled", defaults.deepseekPresetEnabled()),
                    intValue(json, "agentChatMaxInputTokens", defaults.agentChatMaxInputTokens())), defaults);
        } catch (RuntimeException e) {
            return defaults;
        }
    }

    /// 合并一个 PATCH：只改出现的字段，其余保留。非法值被拒绝（抛 [IllegalArgumentException]）。
    public static Settings merge(Settings current, JsonObject body, Settings defaults) {
        String retention = body.has("retention") ? string(body, "retention", current.retention())
                : current.retention();
        if (!RETENTIONS.contains(retention)) {
            throw new IllegalArgumentException("retention must be one of " + RETENTIONS);
        }
        int retentionDays = body.has("retentionDays")
                ? intValue(body, "retentionDays", current.retentionDays()) : current.retentionDays();
        boolean backupEnabled = body.has("backupEnabled")
                ? bool(body, "backupEnabled", current.backupEnabled()) : current.backupEnabled();
        String backupTime = body.has("backupTime")
                ? string(body, "backupTime", current.backupTime()) : current.backupTime();
        int backupKeep = body.has("backupKeep")
                ? intValue(body, "backupKeep", current.backupKeep()) : current.backupKeep();
        String backupDir = body.has("backupDir")
                ? string(body, "backupDir", current.backupDir()) : current.backupDir();
        String privateMode = body.has("privateEndpoints")
                ? string(body, "privateEndpoints", current.privateEndpoints()) : current.privateEndpoints();
        if (!PRIVATE_MODES.contains(privateMode)) {
            throw new IllegalArgumentException("privateEndpoints must be allow or strict");
        }
        List<String> allowlist = current.privateAllowlist();
        if (body.has("privateAllowlist")) {
            JsonElement element = body.get("privateAllowlist");
            if (!element.isJsonArray()) {
                throw new IllegalArgumentException("privateAllowlist must be an array");
            }
            List<String> values = new ArrayList<>();
            for (JsonElement item : element.getAsJsonArray()) {
                if (!item.isJsonPrimitive()) {
                    throw new IllegalArgumentException("privateAllowlist entries must be strings");
                }
                String value = item.getAsString().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
            allowlist = List.copyOf(values);
        }
        int longTextTokens = body.has("longTextTokens")
                ? intValue(body, "longTextTokens", current.longTextTokens()) : current.longTextTokens();
        int dailyTokenLimit = body.has("dailyTokenLimit")
                ? intValue(body, "dailyTokenLimit", current.dailyTokenLimit()) : current.dailyTokenLimit();
        int streamFlushMs = body.has("streamFlushMs")
                ? intValue(body, "streamFlushMs", current.streamFlushMs()) : current.streamFlushMs();
        String defaultAccount = body.has("defaultAccount")
                ? nullableString(body, "defaultAccount") : current.defaultAccount();
        String defaultModel = body.has("defaultModel")
                ? nullableString(body, "defaultModel") : current.defaultModel();
        String searchProvider = body.has("searchProvider")
                ? string(body, "searchProvider", current.searchProvider()) : current.searchProvider();
        // key 按服务商分开存：切换服务商不会把另一家的 key 覆盖掉。
        java.util.Map<String, String> searchKeys = new java.util.LinkedHashMap<>(current.searchKeys());
        if (body.has("searchApiKey")) {
            String value = nullableString(body, "searchApiKey");
            if (value == null) {
                searchKeys.remove(searchProvider == null ? current.searchProvider() : searchProvider);
            } else {
                searchKeys.put((searchProvider == null ? current.searchProvider() : searchProvider)
                        .toLowerCase(java.util.Locale.ROOT), value);
            }
        }
        int searchMaxResults = body.has("searchMaxResults")
                ? intValue(body, "searchMaxResults", current.searchMaxResults()) : current.searchMaxResults();
        boolean persistToolTrail = body.has("persistToolTrail")
                ? bool(body, "persistToolTrail", current.persistToolTrail())
                : current.persistToolTrail();
        boolean deepseekPresetEnabled = body.has("deepseekPresetEnabled")
                ? bool(body, "deepseekPresetEnabled", current.deepseekPresetEnabled())
                : current.deepseekPresetEnabled();
        int agentChatMaxInputTokens = body.has("agentChatMaxInputTokens")
                ? intValue(body, "agentChatMaxInputTokens", current.agentChatMaxInputTokens())
                : current.agentChatMaxInputTokens();
        return sanitize(new Settings(retention, retentionDays, backupEnabled, backupTime, backupKeep,
                backupDir, privateMode, allowlist, longTextTokens, dailyTokenLimit, streamFlushMs,
                defaultAccount, defaultModel,
                searchProvider == null ? current.searchProvider() : searchProvider,
                searchKeys, searchMaxResults, persistToolTrail, deepseekPresetEnabled,
                agentChatMaxInputTokens), defaults);
    }

    private static Settings sanitize(Settings value, Settings defaults) {
        int keep = value.backupKeep() < 1 ? defaults.backupKeep() : Math.min(value.backupKeep(), 100);
        int longText = value.longTextTokens() < 500 ? defaults.longTextTokens()
                : Math.min(value.longTextTokens(), 200_000);
        int daily = Math.max(0, value.dailyTokenLimit());
        int flush = value.streamFlushMs() < 10 ? 10 : Math.min(value.streamFlushMs(), 500);
        String time;
        try {
            LocalTime.parse(value.backupTime());
            time = value.backupTime();
        } catch (DateTimeParseException e) {
            time = defaults.backupTime();
        }
        String dir = value.backupDir() == null || value.backupDir().isBlank()
                ? defaults.backupDir() : value.backupDir().trim();
        String provider = value.searchProvider();
        if (!java.util.Set.of("bocha", "deepseek", "zhipu", "tavily", "brave", "exa").contains(provider)) {
            provider = defaults.searchProvider();
        }
        int maxResults = value.searchMaxResults() < 1 ? defaults.searchMaxResults()
                : Math.min(value.searchMaxResults(), 10);
        int agentTokens = value.agentChatMaxInputTokens() <= 0 ? 0
                : Math.max(1024, Math.min(value.agentChatMaxInputTokens(), 131_072));
        return new Settings(value.retention(), Math.max(1, value.retentionDays()),
                value.backupEnabled(), time, keep, dir, value.privateEndpoints(),
                value.privateAllowlist(), longText, daily, flush,
                blankToNull(value.defaultAccount()), blankToNull(value.defaultModel()),
                provider, java.util.Map.copyOf(value.searchKeys()), maxResults,
                value.persistToolTrail(), value.deepseekPresetEnabled(), agentTokens);
    }

    public static void save(ChatStore store, Settings settings) throws SQLException {
        JsonObject json = new JsonObject();
        json.addProperty("retention", settings.retention());
        json.addProperty("retentionDays", settings.retentionDays());
        json.addProperty("backupEnabled", settings.backupEnabled());
        json.addProperty("backupTime", settings.backupTime());
        json.addProperty("backupKeep", settings.backupKeep());
        json.addProperty("backupDir", settings.backupDir());
        json.addProperty("privateEndpoints", settings.privateEndpoints());
        JsonArray allow = new JsonArray();
        settings.privateAllowlist().forEach(allow::add);
        json.add("privateAllowlist", allow);
        json.addProperty("longTextTokens", settings.longTextTokens());
        json.addProperty("dailyTokenLimit", settings.dailyTokenLimit());
        json.addProperty("streamFlushMs", settings.streamFlushMs());
        json.add("defaultAccount", settings.defaultAccount() == null
                ? com.google.gson.JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(settings.defaultAccount()));
        json.add("defaultModel", settings.defaultModel() == null
                ? com.google.gson.JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(settings.defaultModel()));
        json.addProperty("searchProvider", settings.searchProvider());
        JsonObject searchKeys = new JsonObject();
        settings.searchKeys().forEach(searchKeys::addProperty);
        json.add("searchKeys", searchKeys);
        json.addProperty("searchMaxResults", settings.searchMaxResults());
        json.addProperty("persistToolTrail", settings.persistToolTrail());
        json.addProperty("deepseekPresetEnabled", settings.deepseekPresetEnabled());
        json.addProperty("agentChatMaxInputTokens", settings.agentChatMaxInputTokens());
        store.putSetting(KEY, json.toString());
    }

    private static String string(JsonObject json, String name, String fallback) {
        JsonElement element = json.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private static @Nullable String nullableString(JsonObject json, String name) {
        JsonElement element = json.get(name);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonPrimitive()) {
            return null;
        }
        return blankToNull(element.getAsString());
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean bool(JsonObject json, String name, boolean fallback) {
        JsonElement element = json.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return element.getAsBoolean();
    }

    private static int intValue(JsonObject json, String name, int fallback) {
        JsonElement element = json.get(name);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        return element.getAsInt();
    }
}