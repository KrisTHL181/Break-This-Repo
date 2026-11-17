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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.Part;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/// 聊天模块的业务门面：会话/助手、设置、模型发现、附件上传与备份调度。
@NotNullByDefault
public final class ChatService {

    private final ChatStore store;
    private final ChatAttachments attachments;
    private final ChatTurnManager turns;
    private final ChatBackupService backups;
    private final ChatProbeService probes;
    private final ChatAvatars avatars;
    private final EventBus bus;
    private final Path dataDir;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "chat-maintenance");
                thread.setDaemon(true);
                return thread;
            });
    private volatile ChatSettings.Settings settings;
    private volatile ScheduledFuture<?> backupTask;
    private volatile ScheduledFuture<?> retentionTask;
    private volatile ScheduledFuture<?> catalogTask;

    public ChatService(ChatStore store, EventBus bus, Path dataDir)
            throws SQLException {
        this.store = store;
        this.bus = bus;
        this.dataDir = dataDir;
        this.attachments = new ChatAttachments(store);
        this.settings = ChatSettings.load(store, dataDir);
        this.turns = new ChatTurnManager(store, attachments, bus, settings);
        this.backups = new ChatBackupService(store, dataDir);
        this.probes = new ChatProbeService(store, dataDir);
        this.avatars = new ChatAvatars(dataDir);
        ChatModelCatalog.install(dataDir.resolve("chat"));
        schedule();
        scheduleCatalog();
    }

    public ChatStore store() {
        return store;
    }

    public ChatSettings.Settings settings() {
        return settings;
    }

    public ChatTurnManager turns() {
        return turns;
    }

    public ChatAttachments attachments() {
        return attachments;
    }

    public ChatStore.Session requireSession(String id) throws ChatException, SQLException {
        ChatStore.Session session = store.session(id);
        if (session == null) {
            throw new ChatException(404, "session_not_found", "session not found");
        }
        return session;
    }

    // ---------------------------------------------------------- assistants --

    public JsonArray assistants() throws SQLException {
        JsonArray result = new JsonArray();
        for (ChatStore.Assistant assistant : store.assistants()) {
            if (assistant.preset() != null && !settings.deepseekPresetEnabled()) {
                continue;
            }
            result.add(assistantJson(assistant));
        }
        return result;
    }

    public JsonObject assistantJson(ChatStore.Assistant assistant) {
        JsonObject json = new JsonObject();
        json.addProperty("id", assistant.id());
        json.addProperty("name", assistant.name());
        json.addProperty("avatar", assistant.avatar());
        json.addProperty("description", assistant.description());
        json.addProperty("systemPrompt", assistant.systemPrompt());
        json.addProperty("openingMessage", assistant.openingMessage());
        addNullable(json, "account", assistant.accountName());
        addNullable(json, "model", assistant.model());
        json.add("temperature", assistant.temperature() == null
                ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(assistant.temperature()));
        json.add("maxTokens", assistant.maxTokens() == null
                ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(assistant.maxTokens()));
        addNullable(json, "reasoningLevel", assistant.reasoningLevel());
        json.add("quickPhrases", parseArray(assistant.quickPhrasesJson()));
        json.addProperty("builtin", assistant.builtin());
        addNullable(json, "preset", assistant.preset());
        addNullable(json, "protocol", assistant.protocol());
        json.addProperty("createdAt", assistant.createdAt());
        json.addProperty("updatedAt", assistant.updatedAt());
        return json;
    }

    private static final String PROFILE_KEY = "profile";
    private static final String AVATAR_PREFIX = "img:";

    /// 头像值只有两种：emoji 文本，或 `img:<id>`（指向 [ChatAvatars] 里那张重编码过的 PNG）。
    private void requireAvatar(String avatar) throws ChatException {
        if (avatar != null && avatar.startsWith(AVATAR_PREFIX)
                && avatars.file(avatar.substring(AVATAR_PREFIX.length())) == null) {
            throw new ChatException(400, "avatar_not_found", "头像图片不存在，请重新上传");
        }
    }

    private String profileName() throws SQLException {
        String raw = store.setting(PROFILE_KEY);
        if (raw != null && !raw.isBlank()) {
            try {
                com.google.gson.JsonObject object =
                        com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
                if (object.has("name") && object.get("name").isJsonPrimitive()) {
                    String name = object.get("name").getAsString();
                    if (!name.isBlank()) {
                        return name;
                    }
                }
            } catch (RuntimeException ignored) {
                // 坏掉的资料按默认处理。
            }
        }
        return "";
    }

    private @Nullable String profileAvatar() throws SQLException {
        String raw = store.setting(PROFILE_KEY);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            com.google.gson.JsonObject object =
                    com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
            if (object.has("avatar") && object.get("avatar").isJsonPrimitive()) {
                String avatar = object.get("avatar").getAsString();
                return avatar.isBlank() ? null : avatar;
            }
        } catch (RuntimeException ignored) {
            // 同上。
        }
        return null;
    }

    /// 当前用户的聊天资料。name 缺省就用面板登录名；avatar 可能为 null。
    public JsonObject profile(String username) throws SQLException {
        String name = profileName();
        JsonObject json = new JsonObject();
        json.addProperty("name", name.isBlank() ? username : name);
        addNullable(json, "avatar", profileAvatar());
        return json;
    }

    public JsonObject updateProfile(JsonObject body, String username) throws ChatException, SQLException {
        String storedName = profileName();
        String name = body.has("name") ? string(body, "name", "").trim() : storedName;
        if (name.length() > 32) {
            throw new ChatException(400, "invalid_name", "名字不能超过 32 个字符");
        }
        String avatar = body.has("avatar") ? nullable(body, "avatar") : profileAvatar();
        if (avatar != null) {
            avatar = avatar.trim();
            if (avatar.isEmpty()) {
                avatar = null;
            }
        }
        requireAvatar(avatar);
        JsonObject stored = new JsonObject();
        stored.addProperty("name", name);
        if (avatar == null) {
            stored.add("avatar", com.google.gson.JsonNull.INSTANCE);
        } else {
            stored.addProperty("avatar", avatar);
        }
        store.putSetting(PROFILE_KEY, stored.toString());
        pruneAvatars();
        return profile(username);
    }

    public String uploadAvatar(jakarta.servlet.http.Part part)
            throws ChatException, SQLException {
        if (part == null || part.getSize() == 0) {
            throw new ChatException(400, "file_required", "a file part is required");
        }
        if (part.getSize() > ChatAvatars.MAX_BYTES) {
            throw new ChatException(413, "image_too_large", "头像不能超过 4 MB");
        }
        try (java.io.InputStream input = part.getInputStream()) {
            byte[] bytes = input.readNBytes((int) ChatAvatars.MAX_BYTES + 1);
            if (bytes.length > ChatAvatars.MAX_BYTES) {
                throw new ChatException(413, "image_too_large", "头像不能超过 4 MB");
            }
            // 上传后先不清理：这张还没被资料/助手引用，按引用清理会把它当场删掉。
            // 取消上传留下的文件会在下一次保存资料或助手时被顺手清走。
            return avatars.save(bytes);
        } catch (java.io.IOException e) {
            throw new ChatException(400, "upload_failed", "读取上传失败：" + e.getMessage());
        }
    }

    public @Nullable java.nio.file.Path avatarFile(String id) {
        return avatars.file(id);
    }

    /// 只保留仍在引用的头像文件：资料里的一张 + 每个助手的 `img:<id>`。
    private void pruneAvatars() throws SQLException {
        java.util.Set<String> keep = new java.util.HashSet<>();
        String profile = profileAvatar();
        if (profile != null && profile.startsWith(AVATAR_PREFIX)) {
            keep.add(profile.substring(AVATAR_PREFIX.length()));
        }
        for (ChatStore.Assistant assistant : store.assistants()) {
            String avatar = assistant.avatar();
            if (avatar != null && avatar.startsWith(AVATAR_PREFIX)) {
                keep.add(avatar.substring(AVATAR_PREFIX.length()));
            }
        }
        avatars.prune(keep);
    }

    public ChatStore.Assistant createAssistant(JsonObject body) throws ChatException, SQLException {
        String name = required(body, "name", "name is required");
        String avatar = string(body, "avatar", "💬");
        requireAvatar(avatar);
        ChatStore.Assistant draft = new ChatStore.Assistant(null, name,
                avatar, string(body, "description", ""),
                string(body, "systemPrompt", ""), string(body, "openingMessage", ""),
                nullable(body, "account"), nullable(body, "model"),
                doubleValue(body, "temperature"), intValue(body, "maxTokens"),
                nullable(body, "reasoningLevel"), arrayJson(body, "quickPhrases"),
                false, 0, 0, null, null);
        ChatStore.Assistant created = store.createAssistant(draft);
        pruneAvatars();
        return created;
    }

    public ChatStore.Assistant updateAssistant(String id, JsonObject body)
            throws ChatException, SQLException {
        ChatStore.Assistant current = store.assistant(id);
        if (current == null) {
            throw new ChatException(404, "assistant_not_found", "assistant not found");
        }
        if (body.has("avatar")) {
            requireAvatar(string(body, "avatar", current.avatar()));
        }
        ChatStore.Assistant updated = new ChatStore.Assistant(id,
                body.has("name") ? string(body, "name", current.name()) : current.name(),
                body.has("avatar") ? string(body, "avatar", current.avatar()) : current.avatar(),
                body.has("description") ? string(body, "description", current.description()) : current.description(),
                body.has("systemPrompt") ? string(body, "systemPrompt", current.systemPrompt()) : current.systemPrompt(),
                body.has("openingMessage") ? string(body, "openingMessage", current.openingMessage()) : current.openingMessage(),
                body.has("account") ? nullable(body, "account") : current.accountName(),
                body.has("model") ? nullable(body, "model") : current.model(),
                body.has("temperature") ? doubleValue(body, "temperature") : current.temperature(),
                body.has("maxTokens") ? intValue(body, "maxTokens") : current.maxTokens(),
                body.has("reasoningLevel") ? nullable(body, "reasoningLevel") : current.reasoningLevel(),
                body.has("quickPhrases") ? arrayJson(body, "quickPhrases") : current.quickPhrasesJson(),
                current.builtin(), current.createdAt(), current.updatedAt(),
                current.preset(),
                current.preset() != null && body.has("protocol")
                        ? protocol(body) : current.protocol());
        ChatStore.Assistant saved = store.updateAssistant(updated);
        pruneAvatars();
        return saved;
    }

    public void deleteAssistant(String id) throws ChatException, SQLException {
        ChatStore.Assistant assistant = store.assistant(id);
        if (assistant == null) {
            throw new ChatException(404, "assistant_not_found", "assistant not found");
        }
        if (assistant.builtin()) {
            throw new ChatException(409, "builtin_assistant", "the built-in assistant cannot be deleted");
        }
        store.deleteAssistant(id);
        pruneAvatars();
        broadcast("chat-assistants-changed", null);
    }

    public ChatStore.Assistant duplicateAssistant(String id) throws ChatException, SQLException {
        if (store.assistant(id) == null) {
            throw new ChatException(404, "assistant_not_found", "assistant not found");
        }
        ChatStore.Assistant copy = store.duplicateAssistant(id);
        broadcast("chat-assistants-changed", null);
        return copy;
    }

    // ------------------------------------------------------------ sessions --

    public JsonArray sessions(@Nullable String assistantId) throws SQLException {
        JsonArray result = new JsonArray();
        if (assistantId != null) {
            ChatStore.Assistant assistant = store.assistant(assistantId);
            if (assistant != null && assistant.preset() != null
                    && !settings.deepseekPresetEnabled()) {
                return result;
            }
        }
        for (ChatStore.SessionSummary summary : store.sessions(assistantId)) {
            JsonObject json = new JsonObject();
            json.addProperty("id", summary.id());
            json.addProperty("assistantId", summary.assistantId());
            json.addProperty("title", summary.title());
            addNullable(json, "model", summary.model());
            addNullable(json, "account", summary.accountName());
            json.addProperty("createdAt", summary.createdAt());
            json.addProperty("updatedAt", summary.updatedAt());
            json.addProperty("messageCount", summary.messageCount());
            addNullable(json, "lastMessage", summary.lastMessage());
            result.add(json);
        }
        return result;
    }

    public JsonObject sessionJson(String id) throws ChatException, SQLException {
        ChatStore.Session session = requireSession(id);
        JsonObject json = new JsonObject();
        json.addProperty("id", session.id());
        json.addProperty("assistantId", session.assistantId());
        json.addProperty("title", session.title());
        addNullable(json, "model", session.model());
        addNullable(json, "account", session.accountName());
        addNullable(json, "reasoningLevel", session.reasoningLevel());
        json.addProperty("createdAt", session.createdAt());
        json.addProperty("updatedAt", session.updatedAt());
        json.addProperty("streaming", turns.busy(id));
        JsonArray nodes = new JsonArray();
        for (ChatStore.Node node : session.nodes()) {
            nodes.add(nodeJson(node));
        }
        json.add("nodes", nodes);
        return json;
    }

    public JsonObject createSession(JsonObject body) throws ChatException, SQLException {
        String assistantId = string(body, "assistantId", "default");
        ChatStore.Assistant assistant = store.assistant(assistantId);
        if (assistant == null) {
            throw new ChatException(404, "assistant_not_found", "assistant not found");
        }
        String title = string(body, "title", "新会话");
        String model = firstNonBlank(nullable(body, "model"), assistant.model(), settings.defaultModel());
        String account = firstNonBlank(nullable(body, "account"), assistant.accountName(),
                settings.defaultAccount());
        if (assistant.preset() != null) {
            account = deepseekAccount();
            model = firstNonBlank(nullable(body, "model"), assistant.model(), "deepseek-flash");
        }
        ChatStore.Session session = store.createSession(assistantId, title, model, account,
                assistant.systemPrompt());
        if (body.has("reasoningLevel")) {
            String level = string(body, "reasoningLevel", "");
            if (!level.isEmpty()) {
                store.updateSession(session.id(), null, null, null, level);
                session = store.session(session.id());
            }
        }
        if (assistant.openingMessage() != null && !assistant.openingMessage().isBlank()) {
            ChatStore.Node node = store.beginAssistantNode(session.id(), model);
            ChatStore.Message message = node.active();
            if (message != null) {
                store.updateMessage(message.id(), assistant.openingMessage(), "", "done", null, false);
            }
        }
        broadcast("chat-sessions-changed", session.id());
        return sessionJson(session.id());
    }

    /// 导出一个会话的可移植 JSON（见 [ChatStore#exportJson]）。
    public @Nullable JsonObject exportSession(String id) throws SQLException {
        return store.exportJson(id);
    }

    /// 导入会话：DeepSeek 官方导出包（zip）或 HSDL 自己的单会话 JSON。
    /// 解析与校验都在 [ChatArchive] 里，失败不会留下半截数据。
    ///
    /// @param file           上传的原始字节
    /// @param assistantId    导入到哪个助手
    /// @param titleOverride  覆盖每个会话的标题（可选）
    public JsonObject importSessions(byte[] file, String assistantId, @Nullable String titleOverride)
            throws ChatException, SQLException, java.io.IOException {
        ChatStore.Assistant assistant = store.assistant(assistantId);
        if (assistant == null) {
            throw new ChatException(404, "assistant_not_found", "assistant not found");
        }
        ChatArchive.Parsed parsed = ChatArchive.parse(file);
        JsonArray imported = new JsonArray();
        for (ChatArchive.ImportedSession session : parsed.sessions()) {
            String title = titleOverride == null || titleOverride.isBlank()
                    ? session.title() : titleOverride;
            String prompt = session.systemPrompt() != null
                    ? session.systemPrompt() : assistant.systemPrompt();
            String id = store.importSession(assistantId, title, prompt, session.nodes());
            broadcast("chat-sessions-changed", id);
            JsonObject item = new JsonObject();
            item.addProperty("id", id);
            item.addProperty("title", title);
            item.addProperty("messages", session.nodes().size());
            imported.add(item);
        }
        JsonObject body = new JsonObject();
        body.addProperty("format", parsed.format());
        body.add("imported", imported);
        JsonArray skipped = new JsonArray();
        for (String reason : parsed.skipped()) {
            skipped.add(reason);
        }
        body.add("skipped", skipped);
        return body;
    }

    /// 分叉：整段会话（节点、variants、附件行）复制一份，默认落到同一个助手，
    /// 也可以指定别的助手。原会话不动。流式生成中的会话不能分叉，否则复制到的
    /// 是半截回答。
    public JsonObject forkSession(String id, JsonObject body) throws ChatException, SQLException {
        ChatStore.Session source = store.session(id);
        if (source == null) {
            throw new ChatException(404, "session_not_found", "session not found");
        }
        if (turns.busy(id)) {
            throw new ChatException(409, "chat_busy",
                    "stop the running reply before forking the session");
        }
        String assistantId = string(body, "assistantId", source.assistantId());
        if (store.assistant(assistantId) == null) {
            throw new ChatException(404, "assistant_not_found", "assistant not found");
        }
        String title = string(body, "title", source.title() + "（分叉）");
        String newId = store.forkSession(id, assistantId, title);
        broadcast("chat-sessions-changed", newId);
        return sessionJson(newId);
    }

    public JsonObject updateSession(String id, JsonObject body) throws ChatException, SQLException {
        requireSession(id);
        String title = body.has("title") ? string(body, "title", "") : null;
        String model = body.has("model") ? string(body, "model", "") : null;
        String account = body.has("account") ? string(body, "account", "") : null;
        String reasoningLevel = body.has("reasoningLevel") ? string(body, "reasoningLevel", "") : null;
        store.updateSession(id, title, model, account, reasoningLevel);
        if (body.has("title")) {
            store.setTitleAuto(id, false);
        }
        broadcast("chat-sessions-changed", id);
        return sessionJson(id);
    }

    public void deleteSession(String id) throws ChatException, SQLException, IOException {
        ChatStore.Session session = store.session(id);
        if (session == null) {
            throw new ChatException(404, "session_not_found", "session not found");
        }
        if (turns.busy(id)) {
            throw new ChatException(409, "chat_busy", "stop the running reply before deleting the session");
        }
        store.deleteSession(id);
        attachments.pruneOrphans();
        broadcast("chat-sessions-changed", id);
    }

    public JsonObject switchVariant(String sessionId, String nodeId, String messageId)
            throws ChatException, SQLException {
        ChatStore.Session session = requireSession(sessionId);
        ChatStore.Node node = null;
        for (ChatStore.Node candidate : session.nodes()) {
            if (candidate.id().equals(nodeId)) {
                node = candidate;
                break;
            }
        }
        if (node == null) {
            throw new ChatException(404, "node_not_found", "node not found");
        }
        boolean found = false;
        for (ChatStore.Message variant : node.variants()) {
            if (variant.id().equals(messageId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ChatException(404, "message_not_found", "message not found");
        }
        store.setActiveVariant(nodeId, messageId);
        store.touchSession(sessionId);
        broadcast("chat-sessions-changed", sessionId);
        return sessionJson(sessionId);
    }

    public JsonObject deleteNode(String sessionId, String nodeId) throws ChatException, SQLException, IOException {
        requireSession(sessionId);
        if (turns.busy(sessionId)) {
            throw new ChatException(409, "chat_busy", "stop the running reply first");
        }
        store.deleteNodeAndAfter(sessionId, nodeId);
        attachments.pruneOrphans();
        broadcast("chat-sessions-changed", sessionId);
        return sessionJson(sessionId);
    }

    // --------------------------------------------------------- attachments --

    public JsonObject uploadAttachment(String sessionId, Part part)
            throws ChatException, IOException, SQLException {
        requireSession(sessionId);
        List<ChatStore.Attachment> existing = store.attachmentsForSession(sessionId);
        if (existing.size() >= 200) {
            throw new ChatException(413, "attachment_quota",
                    "this session already holds too many attachments");
        }
        ChatAttachments.Stored stored;
        try {
            stored = attachments.ingest(sessionId, part);
        } catch (ChatAttachments.Refused e) {
            throw new ChatException(415, "attachment_refused", e.getMessage());
        }
        ChatStore.Attachment attachment = store.addAttachment(sessionId, null, stored.name(),
                stored.mime(), stored.kind(), stored.size(), stored.sha256(), stored.path(),
                stored.textPath(), stored.status(), stored.error());
        return attachmentJson(attachment, true);
    }

    public ChatStore.Attachment attachment(String id) throws SQLException {
        return store.attachment(id);
    }

    public JsonObject attachmentJson(ChatStore.Attachment attachment, boolean includeText) {
        JsonObject json = new JsonObject();
        json.addProperty("id", attachment.id());
        json.addProperty("name", attachment.name());
        json.addProperty("mime", attachment.mime());
        json.addProperty("kind", attachment.kind());
        json.addProperty("size", attachment.size());
        json.addProperty("extractStatus", attachment.extractStatus());
        addNullable(json, "extractError", attachment.extractError());
        json.addProperty("url", "/api/chat/attachments/" + attachment.id());
        if (includeText) {
            String text = attachments.loadText(attachment);
            if (text != null && text.length() <= 2000) {
                json.addProperty("preview", text);
            }
        }
        return json;
    }

    // -------------------------------------------------------- model/endpoint --

    public JsonArray accounts() throws SQLException {
        JsonArray result = new JsonArray();
        for (DshAccount account : ChatEndpoints.accounts()) {
            if (!account.carriesAKey()) {
                continue;
            }
            JsonObject json;
            try {
                json = ChatEndpoints.profileJson(store, account, settings);
                ChatEndpoints.Profile profile = ChatEndpoints.profile(store, account.key());
                if (profile != null) {
                    JsonArray models = new JsonArray();
                    for (String model : profile.models()) {
                        models.add(modelJson(account.key(), model, profile.protocol(), profile.base()));
                    }
                    if (models.isEmpty() && account.model() != null && !account.model().isBlank()) {
                        models.add(modelJson(account.key(), account.model(), profile.protocol(),
                                profile.base()));
                    }
                    json.add("modelList", models);
                }
            } catch (IllegalArgumentException e) {
                json = new JsonObject();
                json.addProperty("name", account.key());
                json.addProperty("vendor", account.vendorId());
                json.addProperty("label", account.label() == null ? account.key() : account.label());
                json.addProperty("endpoint", account.endpoint());
                json.addProperty("protocol", ChatEndpoints.defaultProtocol(account));
                json.addProperty("configured", false);
                json.addProperty("error", e.getMessage());
            }
            result.add(json);
        }
        return result;
    }

    public JsonObject models(String accountName) throws ChatException, SQLException {
        DshAccount account = ChatEndpoints.account(accountName);
        if (account == null) {
            throw new ChatException(404, "account_not_found", "account not found");
        }
        ChatEndpoints.Profile profile = ChatEndpoints.profile(store, account.key());
        JsonObject json = new JsonObject();
        json.addProperty("account", account.key());
        json.addProperty("protocol", profile == null ? ChatEndpoints.defaultProtocol(account)
                : profile.protocol());
        JsonArray models = new JsonArray();
        if (profile != null) {
            for (String model : profile.models()) {
                models.add(modelJson(account.key(), model, profile.protocol(), profile.base()));
            }
        }
        if (models.isEmpty() && account.model() != null && !account.model().isBlank()) {
            models.add(modelJson(account.key(), account.model(), profile == null
                    ? ChatEndpoints.defaultProtocol(account) : profile.protocol(),
                    profile == null ? null : profile.base()));
        }
        json.add("models", models);
        return json;
    }

    private JsonObject modelJson(String accountName, String model, String protocol) throws SQLException {
        return modelJson(accountName, model, protocol, null);
    }

    private JsonObject modelJson(String accountName, String model, String protocol,
                                 @Nullable String base) throws SQLException {
        ChatCapabilities.Capabilities capability =
                ChatCapabilities.resolve(store, accountName, model, protocol);
        JsonObject json = new JsonObject();
        json.addProperty("id", model);
        JsonObject caps = new JsonObject();
        caps.addProperty("vision", capability.vision());
        caps.addProperty("files", capability.files());
        caps.addProperty("reasoning", capability.reasoning());
        caps.addProperty("tools", capability.tools());
        caps.addProperty("maxContext", capability.maxContext());
        caps.addProperty("maxOutput", capability.maxOutput());
        caps.addProperty("source", capability.source());
        ChatCapabilities.ReasoningMode reasoning =
                ChatCapabilities.reasoningMode(capability, protocol, base, model);
        JsonObject mode = new JsonObject();
        mode.addProperty("kind", reasoning.kind());
        mode.addProperty("canDisable", reasoning.canDisable());
        JsonArray efforts = new JsonArray();
        reasoning.efforts().forEach(efforts::add);
        mode.add("efforts", efforts);
        mode.addProperty("minBudget", reasoning.minBudget());
        mode.addProperty("maxBudget", reasoning.maxBudget());
        mode.addProperty("source", reasoning.source());
        caps.add("reasoningMode", mode);
        json.add("capabilities", caps);
        return json;
    }

    public JsonObject saveAccountProfile(String accountName, JsonObject body)
            throws ChatException, SQLException {
        DshAccount account = ChatEndpoints.account(accountName);
        if (account == null) {
            throw new ChatException(404, "account_not_found", "account not found");
        }
        Map<String, String> headers = null;
        if (body.has("extraHeaders")) {
            headers = new java.util.LinkedHashMap<>();
            JsonElement element = body.get("extraHeaders");
            if (element.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        headers.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        }
        try {
            ChatEndpoints.saveProfile(store, account,
                    body.has("protocol") ? nullable(body, "protocol") : null,
                    body.has("base") ? nullable(body, "base") : null,
                    body.has("modelsPath") ? nullable(body, "modelsPath") : null,
                    body.has("chatPath") ? nullable(body, "chatPath") : null,
                    body.has("authStyle") ? nullable(body, "authStyle") : null,
                    headers);
        } catch (IllegalArgumentException e) {
            throw new ChatException(400, "invalid_endpoint", e.getMessage());
        }
        return ChatEndpoints.profileJson(store, account, settings);
    }

    public JsonObject saveCapability(String accountName, JsonObject body)
            throws ChatException, SQLException {
        DshAccount account = ChatEndpoints.account(accountName);
        if (account == null) {
            throw new ChatException(404, "account_not_found", "account not found");
        }
        String model = required(body, "model", "model is required");
        if (body.has("reset") && body.get("reset").getAsBoolean()) {
            store.deleteCapability(account.key(), model);
            ChatEndpoints.Profile profile = ChatEndpoints.profile(store, account.key());
            return modelJson(account.key(), model, profile == null
                    ? ChatEndpoints.defaultProtocol(account) : profile.protocol(),
                    profile == null ? null : profile.base());
        }
        ChatEndpoints.Profile profile = ChatEndpoints.profile(store, account.key());
        ChatCapabilities.Capabilities current = ChatCapabilities.resolve(store, account.key(), model,
                profile == null ? ChatEndpoints.defaultProtocol(account) : profile.protocol());
        ChatStore.Capability capability = new ChatStore.Capability(account.key(), model,
                body.has("vision") ? body.get("vision").getAsBoolean() : current.vision(),
                body.has("files") ? body.get("files").getAsBoolean() : current.files(),
                body.has("reasoning") ? body.get("reasoning").getAsBoolean() : current.reasoning(),
                body.has("tools") ? body.get("tools").getAsBoolean() : current.tools(),
                body.has("maxContext") ? body.get("maxContext").getAsInt() : current.maxContext(),
                body.has("maxOutput") ? body.get("maxOutput").getAsInt() : current.maxOutput(),
                "manual", System.currentTimeMillis());
        store.saveCapability(capability);
        return modelJson(account.key(), model, profile == null
                ? ChatEndpoints.defaultProtocol(account) : profile.protocol(),
                profile == null ? null : profile.base());
    }

    public ChatEndpoints.Probe probe(String accountName) throws ChatException, SQLException, IOException {
        try {
            return ChatEndpoints.probe(store, accountName, settings);
        } catch (IllegalArgumentException e) {
            throw new ChatException(404, "account_not_found", e.getMessage());
        }
    }

    /// 真的跑一轮对话（消耗少量 token），验证 key 与 chat path。
    public JsonObject chatTest(String accountName, String model)
            throws ChatException, SQLException, IOException {
        return probes.chatTest(accountName, model, settings);
    }

    /// 发一张内置图片让模型读数字，确认它到底会不会看图。
    public JsonObject visionTest(String accountName, String model)
            throws ChatException, SQLException, IOException {
        return probes.visionTest(accountName, model, settings);
    }

    public List<String> refreshModels(String accountName) throws ChatException, SQLException, IOException {
        try {
            return ChatEndpoints.refreshModels(store, accountName, settings);
        } catch (IllegalArgumentException e) {
            throw new ChatException(404, "account_not_found", e.getMessage());
        }
    }

    // ------------------------------------------------------------- settings --

    public JsonObject settingsJson() throws IOException {
        ChatSettings.Settings current = settings;
        JsonObject json = new JsonObject();
        json.addProperty("retention", current.retention());
        json.addProperty("retentionDays", current.retentionDays());
        json.addProperty("backupEnabled", current.backupEnabled());
        json.addProperty("backupTime", current.backupTime());
        json.addProperty("backupKeep", current.backupKeep());
        json.addProperty("backupDir", current.backupDir());
        json.addProperty("privateEndpoints", current.privateEndpoints());
        JsonArray allow = new JsonArray();
        current.privateAllowlist().forEach(allow::add);
        json.add("privateAllowlist", allow);
        json.addProperty("longTextTokens", current.longTextTokens());
        json.addProperty("dailyTokenLimit", current.dailyTokenLimit());
        json.addProperty("streamFlushMs", current.streamFlushMs());
        json.addProperty("catalogModels", ChatModelCatalog.size());
        json.addProperty("catalogUpdatedAt", ChatModelCatalog.updatedAt());
        json.addProperty("searchProvider", current.searchProvider());
        json.addProperty("searchMaxResults", current.searchMaxResults());
        json.addProperty("persistToolTrail", current.persistToolTrail());
        json.addProperty("deepseekPresetEnabled", current.deepseekPresetEnabled());
        json.addProperty("agentChatMaxInputTokens", current.agentChatMaxInputTokens());
        addNullable(json, "searchKeyMasked", maskKey(current.searchKey(current.searchProvider())));
        addNullable(json, "defaultAccount", current.defaultAccount());
        addNullable(json, "defaultModel", current.defaultModel());
        JsonArray retentionOptions = new JsonArray();
        for (String option : List.of("forever", "30d", "90d", "180d", "365d", "custom")) {
            retentionOptions.add(option);
        }
        json.add("retentionOptions", retentionOptions);
        json.add("backups", backupsJson());
        return json;
    }

    /// 搜索配置状态（不需要提权）：前端用它决定搜索按钮能不能点。
    public JsonObject searchStatus() throws SQLException {
        ChatSettings.Settings current = settings;
        String provider = current.searchProvider();
        boolean configured = ChatSearchService.resolveKey(current, provider) != null;
        JsonObject json = new JsonObject();
        json.addProperty("provider", provider);
        json.addProperty("configured", configured);
        JsonArray providers = new JsonArray();
        for (String id : ChatSearchService.PROVIDERS) {
            JsonObject item = new JsonObject();
            item.addProperty("id", id);
            item.addProperty("name", ChatSearchService.displayName(id));
            item.addProperty("configured", ChatSearchService.configured(current, id));
            providers.add(item);
        }
        json.add("providers", providers);
        json.addProperty("maxResults", current.searchMaxResults());
        addNullable(json, "keyMasked", maskKey(current.searchKey(provider)));
        json.addProperty("deepseekPresetEnabled", current.deepseekPresetEnabled());
        String deepseek = deepseekAccount();
        json.addProperty("deepseekReady", deepseek != null);
        addNullable(json, "deepseekAccount", deepseek);
        return json;
    }

    /// 第一个带 key 的 DeepSeek 账户；预设角色用它，不依赖面板的搜索 provider。
    public @Nullable String deepseekAccount() {
        for (DshAccount account : ChatEndpoints.accounts()) {
            if (!account.carriesAKey()) {
                continue;
            }
            String vendor = account.vendorId() == null ? "" : account.vendorId();
            String endpoint = account.endpoint() == null ? "" : account.endpoint();
            if ("deepseek".equalsIgnoreCase(vendor) || endpoint.contains("deepseek.com")) {
                return account.key();
            }
        }
        return null;
    }

    /// 设置页的「测试搜索」：真跑一次，返回前几条结果。
    public JsonObject searchTest(String query) throws ChatException {
        ChatSearchService.Result result = new ChatSearchService()
                .search(settings, query);
        return result.toJson();
    }

    public JsonObject updateSettings(JsonObject body) throws ChatException, SQLException, IOException {
        ChatSettings.Settings merged;
        try {
            merged = ChatSettings.merge(settings,
                    body, ChatSettings.defaults(dataDir));
        } catch (IllegalArgumentException e) {
            throw new ChatException(400, "invalid_settings", e.getMessage());
        }
        ChatSettings.save(store, merged);
        settings = merged;
        turns.setSettings(merged);
        schedule();
        broadcast("chat-settings-changed", null);
        return settingsJson();
    }

    // -------------------------------------------------------------- backups --

    private JsonArray backupsJson() throws IOException {
        JsonArray result = new JsonArray();
        for (ChatBackupService.BackupEntry entry : backups.list(settings)) {
            JsonObject json = new JsonObject();
            json.addProperty("name", entry.name());
            json.addProperty("path", entry.path());
            json.addProperty("createdAt", entry.createdAt());
            json.addProperty("bytes", entry.bytes());
            json.addProperty("sessions", entry.sessions());
            json.addProperty("messages", entry.messages());
            json.addProperty("attachments", entry.attachments());
            json.addProperty("complete", entry.complete());
            addNullable(json, "error", entry.error());
            result.add(json);
        }
        return result;
    }

    public JsonObject backupNow() throws ChatException, IOException, SQLException {
        try {
            ChatBackupService.BackupEntry entry = backups.backupNow(settings);
            JsonObject json = new JsonObject();
            json.addProperty("name", entry.name());
            json.addProperty("path", entry.path());
            json.addProperty("createdAt", entry.createdAt());
            json.addProperty("bytes", entry.bytes());
            json.addProperty("sessions", entry.sessions());
            json.addProperty("messages", entry.messages());
            json.addProperty("attachments", entry.attachments());
            json.addProperty("complete", entry.complete());
            broadcast("chat-backups-changed", null);
            return json;
        } catch (IOException | SQLException e) {
            throw new ChatException(500, "backup_failed", e.getMessage());
        }
    }

    public JsonArray backups() throws IOException {
        return backupsJson();
    }

    // ----------------------------------------------------------- maintenance --

    private void schedule() {
        cancel(backupTask);
        cancel(retentionTask);
        ChatSettings.Settings current = settings;
        if (current.backupEnabled()) {
            LocalTime time = current.backupLocalTime() == null ? LocalTime.of(3, 17)
                    : current.backupLocalTime();
            backupTask = scheduler.schedule(() -> {
                try {
                    backups.backupNow(settings);
                    broadcast("chat-backups-changed", null);
                } catch (Exception e) {
                    org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Scheduled chat backup failed", e);
                } finally {
                    schedule();
                }
            }, delayUntil(time), TimeUnit.MILLISECONDS);
        }
        retentionTask = scheduler.schedule(() -> {
            try {
                retention();
            } catch (Exception e) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Chat retention sweep failed", e);
            } finally {
                schedule();
            }
        }, delayUntil(LocalTime.of(3, 47)), TimeUnit.MILLISECONDS);
    }

    /// models.dev 目录：启动 1 分钟后检查一次（没有缓存就拉），之后每天检查，
    /// 缓存不到 7 天不会重复下载。拉不到就保持现状，能力判断回落到内置规则表。
    private void scheduleCatalog() {
        cancel(catalogTask);
        catalogTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                ChatModelCatalog.refreshIfStale(Duration.ofDays(7));
            } catch (RuntimeException e) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning("models.dev refresh failed", e);
            }
        }, 1, 24 * 60, TimeUnit.MINUTES);
    }

    private static long delayUntil(LocalTime time) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.with(time);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        long millis = Duration.between(now, next).toMillis();
        return Math.max(60_000, Math.min(millis, Duration.ofDays(1).toMillis()));
    }

    private static void cancel(@Nullable ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(false);
        }
    }

    private void retention() throws SQLException, IOException {
        ChatSettings.Settings current = settings;
        if ("forever".equals(current.retention())) {
            return;
        }
        long cutoff = current.retentionCutoffMillis();
        for (String id : store.sessionIdsBefore(cutoff)) {
            store.deleteSession(id);
        }
        attachments.pruneOrphans();
    }

    private void broadcast(String type, @Nullable String sessionId) {
        JsonObject event = new JsonObject();
        event.addProperty("type", type);
        if (sessionId != null) {
            event.addProperty("sessionId", sessionId);
        }
        bus.publish(sessionId == null ? "chat" : "chat:" + sessionId, event);
        if (sessionId != null) {
            bus.publish("chat", event);
        }
    }

    // --------------------------------------------------------------- close --

    public void close() {
        cancel(backupTask);
        cancel(retentionTask);
        cancel(catalogTask);
        scheduler.shutdownNow();
        turns.close();
        attachments.close();
        store.close();
    }

    // ------------------------------------------------------------- helpers --

    private JsonObject nodeJson(ChatStore.Node node) {
        JsonObject json = new JsonObject();
        json.addProperty("id", node.id());
        json.addProperty("ordinal", node.ordinal());
        json.addProperty("role", node.role());
        addNullable(json, "activeVariantId", node.activeVariantId());
        json.addProperty("createdAt", node.createdAt());
        JsonArray variants = new JsonArray();
        for (ChatStore.Message message : node.variants()) {
            variants.add(messageJson(message));
        }
        json.add("variants", variants);
        return json;
    }

    private JsonObject messageJson(ChatStore.Message message) {
        JsonObject json = new JsonObject();
        json.addProperty("id", message.id());
        json.addProperty("nodeId", message.nodeId());
        json.addProperty("variantIndex", message.variantIndex());
        json.addProperty("content", message.content() == null ? "" : message.content());
        json.addProperty("reasoning", message.reasoning() == null ? "" : message.reasoning());
        if (message.blocksJson() != null && !message.blocksJson().isBlank()) {
            try {
                JsonElement blocks = JsonParser.parseString(message.blocksJson());
                if (blocks.isJsonArray()) {
                    json.add("blocks", blocks);
                }
            } catch (RuntimeException ignored) {
                // A malformed blocks column must not take down the whole session response.
            }
        }
        try {
            String parts = store.partsJson(message.id());
            if (parts != null && !parts.isBlank()) {
                JsonElement parsed = JsonParser.parseString(parts);
                if (parsed.isJsonArray()) {
                    json.add("parts", parsed);
                }
            }
        } catch (SQLException | RuntimeException ignored) {
            // A malformed parts column must not take down the session response.
        }
        json.addProperty("status", message.status());
        addNullable(json, "model", message.model());
        json.addProperty("createdAt", message.createdAt());
        json.add("editedAt", message.editedAt() == null
                ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(message.editedAt()));
        if (message.usageJson() != null) {
            try {
                JsonObject usage = JsonParser.parseString(message.usageJson()).getAsJsonObject();
                JsonElement search = usage.get("search");
                if (search != null && search.isJsonObject()
                        && search.getAsJsonObject().has("hits")) {
                    json.add("sources", search.getAsJsonObject().get("hits"));
                    JsonElement query = search.getAsJsonObject().get("query");
                    if (query != null && query.isJsonPrimitive()) {
                        json.addProperty("searchQuery", query.getAsString());
                    }
                }
                JsonElement tools = usage.get("tools");
                if (tools != null && tools.isJsonArray() && !tools.getAsJsonArray().isEmpty()) {
                    json.add("tools", tools);
                }
                json.add("usage", usage);
            } catch (RuntimeException ignored) {
                // Ignore a malformed usage blob.
            }
        }
        if (message.id() != null) {
            String rawSources;
            try {
                rawSources = store.sourcesJson(message.id());
            } catch (SQLException e) {
                rawSources = null;
            }
            if (rawSources != null) {
                try {
                    JsonObject sources = JsonParser.parseString(rawSources).getAsJsonObject();
                JsonElement hits = sources.get("sources");
                if (hits != null && hits.isJsonArray() && !hits.getAsJsonArray().isEmpty()) {
                    json.add("sources", hits);
                }
                JsonElement tools = sources.get("tools");
                if (tools != null && tools.isJsonArray() && !tools.getAsJsonArray().isEmpty()) {
                    json.add("tools", tools);
                }
            } catch (RuntimeException ignored) {
                    // Ignore a malformed sources blob.
                }
            }
        }
        JsonArray attachments = new JsonArray();
        for (ChatStore.Attachment attachment : message.attachments()) {
            attachments.add(attachmentJson(attachment, false));
        }
        json.add("attachments", attachments);
        return json;
    }

    private static JsonArray parseArray(@Nullable String json) {
        if (json == null || json.isBlank()) {
            return new JsonArray();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        } catch (RuntimeException e) {
            return new JsonArray();
        }
    }

    private static String arrayJson(JsonObject body, String name) {
        JsonElement element = body.get(name);
        return element != null && element.isJsonArray() ? element.toString() : "[]";
    }

    private static String required(JsonObject body, String name, String message) throws ChatException {
        String value = nullable(body, name);
        if (value == null || value.isBlank()) {
            throw new ChatException(400, "invalid_request", message);
        }
        return value.trim();
    }

    private static String string(JsonObject body, String name, String fallback) {
        String value = nullable(body, name);
        return value == null ? fallback : value;
    }

    private static @Nullable String nullable(JsonObject body, String name) {
        JsonElement element = body.get(name);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        return value.isBlank() ? null : value;
    }

    private static @Nullable Double doubleValue(JsonObject body, String name) {
        JsonElement element = body.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsDouble() : null;
    }

    private static @Nullable Integer intValue(JsonObject body, String name) {
        JsonElement element = body.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()
                ? element.getAsInt() : null;
    }

    /// 只回前后各四位，够确认「是不是这把 key」，又不会把 key 送给前端。
    private static @Nullable String maskKey(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String value = key.trim();
        if (value.length() <= 8) {
            return "…" + value.substring(Math.max(0, value.length() - 3));
        }
        return value.substring(0, 4) + "…" + value.substring(value.length() - 4);
    }

    private static void addNullable(JsonObject json, String name, @Nullable String value) {
        json.add(name, value == null ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(value));
    }

    private static @Nullable String firstNonBlank(@Nullable String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    /// 助手可以固定协议；只接受 v1 认识的三种，空值表示跟随账户配置。
    private static @Nullable String protocol(JsonObject body) throws ChatException {
        String value = nullable(body, "protocol");
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (!ChatEndpoints.OPENAI_COMPLETIONS.equals(normalized)
                && !ChatEndpoints.ANTHROPIC_MESSAGES.equals(normalized)
                && !ChatEndpoints.DEEPSEEK_ANTHROPIC.equals(normalized)) {
            throw new ChatException(400, "invalid_protocol", "unknown chat protocol: " + value);
        }
        return normalized;
    }
}