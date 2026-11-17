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
package org.jackhuang.hmcl.web.brand;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The GitHub token the panel uses for its own API calls.
///
/// **A panel-owned secret, not an agent one.** The catalog and the installer
/// read it; brand processes never need it and must not see it. That is why it
/// lives in its own `0600` file (`<data>/auth/github.json`, panel-owned) rather
/// than in the environment: `ProcessBuilder` copies the panel's environment to
/// every child, so `GITHUB_TOKEN=` would hand the user's GitHub identity to the
/// agent and to every terminal it opens.
///
/// The token is never returned to the browser — [Status] carries only whether
/// one is configured and which GitHub login it belongs to.
@NotNullByDefault
public final class GitHubToken {

    /// The public API's anonymous quota, and the authenticated one.
    public static final int ANONYMOUS_LIMIT = 60;
    public static final int AUTHENTICATED_LIMIT = 5000;

    /// A status safe to send to the browser.
    public record Status(boolean configured, @Nullable String login) {
        public static final Status NONE = new Status(false, null);
    }

    /// The token was rejected by GitHub (401), so it must not be saved.
    public static final class InvalidTokenException extends Exception {
        public InvalidTokenException(String message) {
            super(message);
        }
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final String FILE_NAME = "github.json";

    private static volatile @Nullable Path file;
    private static volatile @Nullable String cachedToken;
    private static volatile @Nullable String cachedLogin;
    private static volatile long cachedModified = -1;

    private GitHubToken() {
    }

    /// Binds the store to the panel's data directory.
    ///
    /// @param dataDir the panel data directory
    public static void bind(Path dataDir) {
        file = dataDir.resolve("auth").resolve(FILE_NAME);
        cachedModified = -1;
    }

    /// The configured token, or `null`.
    ///
    /// @return the token, never logged
    public static @Nullable String get() {
        read();
        return cachedToken;
    }

    /// Whether a token is configured and which login it belongs to.
    ///
    /// @return the status
    public static Status status() {
        read();
        return cachedToken == null ? Status.NONE : new Status(true, cachedLogin);
    }

    /// Validates and stores a token.
    ///
    /// Validation calls `/rate_limit` (which does not consume quota) and
    /// `/user` (one authenticated request) before anything is written, so a
    /// typo leaves the previous token in place.
    ///
    /// @param token the fine-grained or classic token
    /// @return the status of the saved token
    /// @throws InvalidTokenException when GitHub answers 401
    /// @throws IOException when GitHub is unreachable or the file cannot be written
    public static Status save(String token) throws IOException, InvalidTokenException {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty()) {
            throw new InvalidTokenException("令牌为空");
        }
        if (normalized.length() > 512 || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidTokenException("令牌格式不正确");
        }
        String login = validate(normalized);
        Path target = file;
        if (target == null) {
            throw new IOException("GitHub token store is not bound to a data directory");
        }
        JsonObject document = new JsonObject();
        document.addProperty("token", normalized);
        if (login != null) {
            document.addProperty("login", login);
        }
        document.addProperty("savedAt", System.currentTimeMillis());
        try {
            Files.createDirectories(target.getParent());
            handToPanel(target.getParent(), true);
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temp, document.toString(), StandardCharsets.UTF_8);
            handToPanel(temp, false);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IOException("无法保存 GitHub 令牌: " + e.getMessage(), e);
        }
        cachedToken = normalized;
        cachedLogin = login;
        cachedModified = Files.getLastModifiedTime(target).toMillis();
        BrandCatalog.afterTokenChanged();
        return new Status(true, login);
    }

    /// Removes the stored token.
    ///
    /// @throws IOException when the file exists but cannot be removed
    public static void clear() throws IOException {
        Path target = file;
        if (target != null) {
            Files.deleteIfExists(target);
            Files.deleteIfExists(target.resolveSibling(target.getFileName() + ".tmp"));
        }
        cachedToken = null;
        cachedLogin = null;
        cachedModified = -1;
        BrandCatalog.afterTokenChanged();
    }

    /// Test seam: forget the cached token without touching the file.
    static void clearForTests() {
        file = null;
        cachedToken = null;
        cachedLogin = null;
        cachedModified = -1;
    }

    /// Reads the file when its mtime changed; a corrupt file behaves like "no
    /// token configured" instead of failing every version request.
    private static void read() {
        Path target = file;
        if (target == null) {
            return;
        }
        try {
            if (!Files.isRegularFile(target)) {
                cachedToken = null;
                cachedLogin = null;
                cachedModified = -1;
                return;
            }
            long modified = Files.getLastModifiedTime(target).toMillis();
            if (modified == cachedModified) {
                return;
            }
            JsonObject document = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonElement value = document.get("token");
            cachedToken = value != null && value.isJsonPrimitive() && !value.getAsString().isBlank()
                    ? value.getAsString().trim() : null;
            JsonElement login = document.get("login");
            cachedLogin = login != null && login.isJsonPrimitive() ? login.getAsString() : null;
            cachedModified = modified;
        } catch (Exception e) {
            LOG.warning("Could not read the GitHub token file " + target + ": " + e.getMessage());
            cachedToken = null;
            cachedLogin = null;
            cachedModified = -1;
        }
    }

    /// Calls `/rate_limit` and `/user`; returns the login when known.
    private static @Nullable String validate(String token) throws IOException, InvalidTokenException {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(TIMEOUT)
                .build();
        HttpRequest rate = HttpRequest.newBuilder(URI.create("https://api.github.com/rate_limit"))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> answer;
        try {
            answer = client.send(rate, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("校验 GitHub 令牌时被中断", e);
        } catch (IOException e) {
            throw new IOException("无法连接 GitHub 校验令牌: " + e.getMessage(), e);
        }
        if (answer.statusCode() == 401) {
            throw new InvalidTokenException("GitHub 拒绝了该令牌（401），请检查是否过期或复制完整");
        }
        if (answer.statusCode() / 100 != 2) {
            throw new IOException("GitHub 校验接口返回 HTTP " + answer.statusCode());
        }
        HttpRequest user = HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        try {
            HttpResponse<String> profile = client.send(user, HttpResponse.BodyHandlers.ofString());
            if (profile.statusCode() / 100 == 2) {
                JsonObject object = JsonParser.parseString(profile.body()).getAsJsonObject();
                JsonElement login = object.get("login");
                return login != null && login.isJsonPrimitive() ? login.getAsString() : null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // The login is a nicety; a valid token without it is still usable.
            LOG.info("Could not read the GitHub login: " + e.getMessage());
        }
        return null;
    }

    /// `0600` for the token file, `0700` for its directory, on POSIX only.
    private static void handToPanel(Path path, boolean directory) {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            return;
        }
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(
                    directory ? "rwx------" : "rw-------"));
        } catch (IOException | UnsupportedOperationException e) {
            LOG.warning("Could not tighten the mode of " + path + ": " + e.getMessage());
        }
    }
}
