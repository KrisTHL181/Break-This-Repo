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
package org.jackhuang.hmcl.web.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/// The persisted admin account: one user, stored at `<data>/auth/users.json`.
///
/// Passwords are hashed with PBKDF2WithHmacSHA256 (16-byte per-user salt,
/// 210000 iterations by default) using only the JDK; nothing here needs
/// Bouncy Castle. The file is written atomically with 0600 permissions.
///
/// First boot: if `HDSL_ADMIN_PASSWORD` is set, the admin account is created
/// from it; otherwise no password is generated at all — the store comes up
/// uninitialized and stays that way until a user is created through the
/// first-run setup (`POST /api/auth/setup`). Restarting never resets an
/// existing password.
@NotNullByDefault
public final class UserStore {

    /// The fallback username when the admin account is created from
    /// `HDSL_ADMIN_PASSWORD`; interactive setup may pick any valid name.
    public static final String ADMIN_USERNAME = "admin";

    /// Environment variable holding the initial admin password.
    public static final String ENV_ADMIN_PASSWORD = "HDSL_ADMIN_PASSWORD";

    /// Longest accepted username.
    public static final int USERNAME_MAX_LENGTH = 32;

    /// Shortest accepted password.
    public static final int PASSWORD_MIN_LENGTH = 6;

    /// Default PBKDF2 iteration count. Adjustable per construction; a value
    /// recorded in the file stays the value that file is verified with.
    public static final int DEFAULT_ITERATIONS = 210_000;

    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /// One stored account. `saltB64`/`hashB64` are standard base64.
    public record User(String username, String saltB64, int iterations, String hashB64, String createdAt) {
    }

    private final Path file;
    private final int defaultIterations;
    private @Nullable User admin;

    private UserStore(Path file, int defaultIterations, @Nullable User admin) {
        this.file = file;
        this.defaultIterations = defaultIterations;
        this.admin = admin;
    }

    /// Opens the store, creating the admin account on first boot when
    /// `HDSL_ADMIN_PASSWORD` is set; otherwise the store opens uninitialized
    /// and waits for the interactive first-run setup.
    ///
    /// @param dataDir the HDSL_DATA directory
    /// @param env      the process environment (for `HDSL_ADMIN_PASSWORD`)
    /// @param logger   where the first-boot note goes
    public static UserStore open(Path dataDir, Map<String, String> env, Logger logger) throws IOException {
        return open(dataDir, env, DEFAULT_ITERATIONS, logger);
    }

    /// Like [open] with an explicit PBKDF2 iteration count.
    public static UserStore open(Path dataDir, Map<String, String> env, int iterations, Logger logger) throws IOException {
        Path authDir = dataDir.resolve("auth");
        Files.createDirectories(authDir);
        setOwnerOnly(authDir);

        Path file = authDir.resolve("users.json");
        if (Files.exists(file)) {
            return new UserStore(file, iterations, load(file));
        }

        String password = env.getOrDefault(ENV_ADMIN_PASSWORD, "");
        if (password.isBlank()) {
            logger.info("No " + ENV_ADMIN_PASSWORD + " set and no user exists: "
                    + "waiting for the first-run setup at POST /api/auth/setup.");
            return new UserStore(file, iterations, null);
        }
        User admin = create(ADMIN_USERNAME, password, iterations);
        save(file, admin);
        logger.info("Created admin user `" + ADMIN_USERNAME + "` from " + ENV_ADMIN_PASSWORD + ".");
        return new UserStore(file, iterations, admin);
    }

    /// Whether a user record exists. False only on a fresh data directory
    /// whose `HDSL_ADMIN_PASSWORD` was not set.
    public synchronized boolean initialized() {
        return admin != null;
    }

    /// Whether `username` is acceptable: 1–32 characters, no whitespace.
    public static boolean isValidUsername(@Nullable String username) {
        return username != null && !username.isEmpty() && username.length() <= USERNAME_MAX_LENGTH
                && username.chars().noneMatch(Character::isWhitespace);
    }

    /// Whether `password` is acceptable: at least [PASSWORD_MIN_LENGTH] chars.
    public static boolean isValidPassword(@Nullable String password) {
        return password != null && password.length() >= PASSWORD_MIN_LENGTH;
    }

    /// Creates the first user. Only possible while uninitialized; a concurrent
    /// race loses with [IllegalStateException] and the caller maps that to 409.
    ///
    /// @return the freshly stored user
    public synchronized User createUser(String username, String password) throws IOException {
        if (admin != null) {
            throw new IllegalStateException("already initialized");
        }
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("invalid username");
        }
        if (!isValidPassword(password)) {
            throw new IllegalArgumentException("invalid password");
        }
        User user = create(username, password, defaultIterations);
        save(file, user);
        admin = user;
        return user;
    }

    /// Renames the stored user: same salt, hash, iteration count and creation
    /// time, only `username` changes — the same atomic 0600 write. Renaming to
    /// the current name is a successful no-op.
    ///
    /// @return false when no user exists at all
    /// @throws IllegalArgumentException when `newUsername` is not [valid][isValidUsername]
    public synchronized boolean updateUsername(String newUsername) throws IOException {
        User user = admin;
        if (user == null) {
            return false;
        }
        if (!isValidUsername(newUsername)) {
            throw new IllegalArgumentException("invalid username");
        }
        if (user.username().equals(newUsername)) {
            return true;
        }
        User updated = new User(newUsername, user.saltB64(), user.iterations(), user.hashB64(), user.createdAt());
        save(file, updated);
        admin = updated;
        return true;
    }

    /// Replaces the user's password: a fresh salt, the same PBKDF2 iteration
    /// count the file already records, the same atomic 0600 write.
    ///
    /// @return false when no such user exists
    public synchronized boolean updatePassword(String username, String password) throws IOException {
        User user = admin;
        if (user == null || !user.username().equals(username)) {
            return false;
        }
        User updated = create(username, password, user.iterations());
        save(file, updated);
        admin = updated;
        return true;
    }

    /// Verifies a password against the stored hash in constant time.
    public boolean verify(String username, String password) {
        User user = admin;
        if (user == null || !user.username().equals(username)) {
            // Burn the same work a real check costs so the response time does
            // not leak whether the username exists.
            hash(password, new byte[SALT_BYTES], user == null ? defaultIterations : user.iterations());
            return false;
        }
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(user.saltB64());
            expected = Base64.getDecoder().decode(user.hashB64());
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = hash(password, salt, user.iterations());
        return MessageDigest.isEqual(expected, actual);
    }

    /// The stored admin user, or null before first open.
    public @Nullable User admin() {
        return admin;
    }

    private static User create(String username, String password, int iterations) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(password, salt, iterations);
        return new User(username,
                Base64.getEncoder().encodeToString(salt),
                iterations,
                Base64.getEncoder().encodeToString(hash),
                Instant.now().toString());
    }

    private static byte[] hash(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(HASH_ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is missing from this JRE", e);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException("PBKDF2 key derivation failed", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static @Nullable User load(Path file) throws IOException {
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException("Failed to read " + file + ": " + e.getMessage(), e);
        }
        if (json.isBlank()) {
            return null;
        }
        try {
            JsonElement tree = JsonParser.parseString(json);
            if (tree.isJsonNull() || (tree.isJsonObject() && !tree.getAsJsonObject().has("username"))) {
                // The file exists but holds no record: a previous boot died
                // between creating it and writing the user. Treat as fresh.
                return null;
            }
            User user = GSON.fromJson(tree, User.class);
            if (user == null || user.username() == null || user.username().isBlank()
                    || user.saltB64() == null || user.hashB64() == null || user.iterations() < 1) {
                throw new IOException(file + " is malformed: expected {username, saltB64, iterations, hashB64, createdAt}");
            }
            return user;
        } catch (JsonParseException e) {
            throw new IOException(file + " is malformed JSON: " + e.getMessage(), e);
        }
    }

    private static void save(Path file, User user) throws IOException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, GSON.toJson(user), StandardCharsets.UTF_8);
        setOwnerOnly(tmp);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        setOwnerOnly(file);
    }

    private static final Set<PosixFilePermission> OWNER_FILE = EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
    private static final Set<PosixFilePermission> OWNER_DIR = EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

    private static void setOwnerOnly(Path path) {
        try {
            // A directory without the execute bit cannot be entered, which
            // would lock the store out of its own files.
            Files.setPosixFilePermissions(path, Files.isDirectory(path) ? OWNER_DIR : OWNER_FILE);
        } catch (IOException | UnsupportedOperationException e) {
            // Non-POSIX filesystems (Windows) cannot express this; the best
            // effort below is still applied by every other filesystem.
        }
    }
}
