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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// 聊天备份（docs/chat.md §6.4）。
///
/// `VACUUM INTO` 拿一致快照，附件目录同盘优先硬链接、跨盘退回复制；一份
/// `manifest.json` 记录 schema、计数与校验和。备份包含聊天内容与附件，按敏感数据处理。
@NotNullByDefault
public final class ChatBackupService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public record BackupEntry(
            String name, String path, long createdAt, long bytes, int sessions, int messages,
            int attachments, boolean complete, @Nullable String error) {
    }

    private final ChatStore store;
    private final Path dataDir;

    public ChatBackupService(ChatStore store, Path dataDir) {
        this.store = store;
        this.dataDir = dataDir;
    }

    public BackupEntry backupNow(ChatSettings.Settings settings) throws IOException, SQLException {
        Path root = resolve(settings.backupDir());
        boolean created = !Files.exists(root);
        Files.createDirectories(root);
        if (created) {
            setPrivate(root);
        }
        String name = "chat-backup-" + LocalDateTime.now().format(STAMP);
        Path target = root.resolve(name);
        int suffix = 1;
        while (Files.exists(target)) {
            target = root.resolve(name + "-" + suffix++);
        }
        Files.createDirectories(target);
        setPrivate(target);
        try {
            Path database = target.resolve("chat.db");
            synchronized (store) {
                try (Statement statement = store.connection().createStatement()) {
                    statement.execute("VACUUM INTO '" + database.toString().replace("'", "''") + "'");
                }
            }
            Path files = target.resolve("files");
            Files.createDirectories(files);
            copyFiles(store.filesDir(), files);
            String checksum = ChatStore.sha256(database);
            JsonObject manifest = new JsonObject();
            manifest.addProperty("schemaVersion", 1);
            manifest.addProperty("createdAt", Instant.now().toString());
            manifest.addProperty("appVersion", org.jackhuang.hmcl.Metadata.VERSION);
            manifest.addProperty("sessions", store.countSessions());
            manifest.addProperty("messages", store.countMessages());
            manifest.addProperty("attachments", store.countAttachments());
            manifest.addProperty("dbSha256", checksum);
            manifest.addProperty("complete", true);
            Files.writeString(target.resolve("manifest.json"), manifest.toString(),
                    StandardCharsets.UTF_8);
            prune(root, settings.backupKeep());
            return read(target);
        } catch (IOException | SQLException | RuntimeException e) {
            JsonObject manifest = new JsonObject();
            manifest.addProperty("schemaVersion", 1);
            manifest.addProperty("createdAt", Instant.now().toString());
            manifest.addProperty("complete", false);
            manifest.addProperty("error", message(e));
            try {
                Files.writeString(target.resolve("manifest.json"), manifest.toString(),
                        StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // The backup already failed; the manifest is best-effort.
            }
            throw e;
        }
    }

    public List<BackupEntry> list(ChatSettings.Settings settings) throws IOException {
        Path root = resolve(settings.backupDir());
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<BackupEntry> entries = new ArrayList<>();
        try (var stream = Files.list(root)) {
            for (Path path : stream.toList()) {
                if (Files.isDirectory(path) && path.getFileName().toString().startsWith("chat-backup-")) {
                    entries.add(read(path));
                }
            }
        }
        entries.sort(Comparator.comparing(BackupEntry::createdAt).reversed());
        return entries;
    }

    private BackupEntry read(Path path) throws IOException {
        JsonObject manifest = new JsonObject();
        Path file = path.resolve("manifest.json");
        if (Files.isRegularFile(file)) {
            try {
                manifest = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            } catch (RuntimeException ignored) {
                // A broken manifest still describes a directory worth listing.
            }
        }
        long createdAt = 0;
        if (manifest.has("createdAt")) {
            try {
                createdAt = Instant.parse(manifest.get("createdAt").getAsString()).toEpochMilli();
            } catch (RuntimeException ignored) {
                createdAt = 0;
            }
        }
        if (createdAt == 0) {
            try {
                createdAt = Files.getLastModifiedTime(path).toMillis();
            } catch (IOException ignored) {
                createdAt = 0;
            }
        }
        long bytes = 0;
        try (var walk = Files.walk(path)) {
            for (Path child : walk.toList()) {
                if (Files.isRegularFile(child)) {
                    bytes += Files.size(child);
                }
            }
        }
        boolean complete = !manifest.has("complete") || manifest.get("complete").getAsBoolean();
        String error = manifest.has("error") && !manifest.get("error").isJsonNull()
                ? manifest.get("error").getAsString() : null;
        return new BackupEntry(path.getFileName().toString(), path.toString(), createdAt, bytes,
                intOf(manifest, "sessions"), intOf(manifest, "messages"),
                intOf(manifest, "attachments"), complete, error);
    }

    private static int intOf(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive()
                ? object.get(name).getAsInt() : 0;
    }

    private static void copyFiles(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        try (var stream = Files.list(source)) {
            for (Path file : stream.toList()) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                Path destination = target.resolve(file.getFileName().toString());
                try {
                    Files.createLink(destination, file);
                } catch (IOException | UnsupportedOperationException e) {
                    Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
                }
                setPrivate(destination);
            }
        }
    }

    private static void prune(Path root, int keep) throws IOException {
        List<Path> backups = new ArrayList<>();
        try (var stream = Files.list(root)) {
            for (Path path : stream.toList()) {
                if (Files.isDirectory(path)
                        && path.getFileName().toString().startsWith("chat-backup-")) {
                    backups.add(path);
                }
            }
        }
        backups.sort(Comparator.comparing(path -> path.getFileName().toString()));
        int remove = backups.size() - Math.max(1, keep);
        for (int i = 0; i < remove; i++) {
            deleteRecursively(backups.get(i));
        }
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path child : paths) {
                Files.deleteIfExists(child);
            }
        }
    }

    private Path resolve(String configured) {
        Path path = Path.of(configured == null || configured.isBlank()
                ? ChatSettings.defaults(dataDir).backupDir() : configured);
        return path.isAbsolute() ? path : dataDir.resolve(path);
    }

    private static void setPrivate(Path path) {
        try {
            if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(path,
                        PosixFilePermissions.fromString("rwx------"));
            }
        } catch (IOException | UnsupportedOperationException ignored) {
            // Not a POSIX filesystem.
        }
    }

    private static String message(Throwable error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? error.getClass().getSimpleName() : error.getMessage();
    }
}