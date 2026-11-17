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
package org.jackhuang.hmcl.web.zcode;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The per-instance root secret ZCode 3.18.x needs before its standalone server
/// will open its services.
///
/// ZCode removed the old derivable fallback key and now expects a 32–4096 byte
/// private `credential-secret` file in the credential directory (or an explicit
/// `ZCODE_CREDENTIAL_SECRET[_FILE]`). In a headless container the deployment
/// operator is the only one who can provide it, and this panel is that operator:
/// the file lives in the instance's data volume, is generated once, and is never
/// overwritten. If an operator mounts their own secret, the environment wins and
/// the panel does not create a file.
///
/// The secret is never logged. Losing it means the encrypted `credentials.json`
/// cannot be decrypted; the operator must restore the original key or reset the
/// credentials and re-authenticate.
@NotNullByDefault
final class ZcodeCredentials {

    /// The private file ZCode's `createServerCredentialCipher` discovers when no
    /// explicit environment secret is set.
    static final String SECRET_FILE = "credential-secret";

    /// Written by ZCode when a key source has been pinned. A pinned source must
    /// not be switched by generating a file underneath it.
    static final String SOURCE_FILE = "credential-source";

    /// ZCode's encrypted credential container, beside the secret.
    static final String CREDENTIALS_FILE = "credentials.json";

    /// Explicit environment overrides understood by ZCode.
    static final String SECRET_ENV = "ZCODE_CREDENTIAL_SECRET";
    static final String SECRET_FILE_ENV = "ZCODE_CREDENTIAL_SECRET_FILE";

    /// ZCode's random key is 32 bytes; base64url without padding is 43 bytes,
    /// inside its 32–4096 byte file contract.
    private static final int SECRET_BYTES = 32;
    private static final int MIN_FILE_BYTES = 32;
    private static final int MAX_FILE_BYTES = 4096;

    private static final SecureRandom RANDOM = new SecureRandom();

    private ZcodeCredentials() {
    }

    /// The directory ZCode calls its credential/config directory:
    /// `<ZCODE_DATA_BASE_DIR>/.zcode/v2`.
    static Path configDirectory(ZcodeInstanceManager manager, ZcodeInstance instance) {
        return manager.dataDirectory(instance).resolve(".zcode").resolve("v2");
    }

    /// Makes sure the instance has a usable root secret before its process starts.
    ///
    /// @param manager  the instance manager, for the data directory
    /// @param instance the instance about to launch
    /// @param env      the extra environment passed to the child, checked for
    ///                 explicit ZCode overrides
    /// @throws ZcodeException when an existing key source must not be replaced,
    ///                        or a secret cannot be written
    static void ensure(ZcodeInstanceManager manager, ZcodeInstance instance,
                       @Nullable Map<String, String> env) throws ZcodeException {
        if (hasText(env == null ? null : env.get(SECRET_ENV))
                || hasText(env == null ? null : env.get(SECRET_FILE_ENV))
                || hasText(System.getenv(SECRET_ENV))
                || hasText(System.getenv(SECRET_FILE_ENV))) {
            return;
        }

        Path directory = configDirectory(manager, instance);
        Path secret = directory.resolve(SECRET_FILE);
        if (Files.isRegularFile(secret)) {
            validateSize(secret);
            return;
        }

        if (Files.exists(directory.resolve(SOURCE_FILE))) {
            throw new ZcodeException(
                    "ZCode 的凭据来源已被固定（credential-source），不能自动生成本地密钥；"
                            + "请先重置凭据或恢复原密钥。");
        }

        if (hasV2Ciphertext(directory.resolve(CREDENTIALS_FILE))) {
            throw new ZcodeException(
                    "该实例已有 v2 加密凭据，但缺少 credential-secret。"
                            + "请恢复原密钥，或通过“重置凭据”重新登录。");
        }

        try {
            writeSecret(directory, secret, randomBytes());
            LOG.info("Generated ZCode credential secret for instance " + instance.id());
        } catch (IOException e) {
            throw new ZcodeException(
                    "无法为 ZCode 实例生成 credential-secret: " + e.getMessage(), e);
        }
    }

    /// Writes a seed-derived key exactly once. Used at creation time; replacing
    /// an existing key must go through [#rotate] because the old ciphertext has
    /// to be discarded with it.
    static void writeSeed(ZcodeInstanceManager manager, ZcodeInstance instance,
                          String seed) throws ZcodeException {
        if (seed == null || seed.isBlank()) {
            throw new ZcodeException("种子不能为空");
        }
        if (externalSecretConfigured()) {
            throw new ZcodeException("当前进程已配置外部的 ZCODE_CREDENTIAL_SECRET[_FILE]；"
                    + "面板不会另写 credential-secret，请在外部轮换密钥。");
        }
        Path directory = configDirectory(manager, instance);
        Path secret = directory.resolve(SECRET_FILE);
        if (Files.exists(secret)) {
            throw new ZcodeException("该实例已有 credential-secret；更换密钥请使用重置流程。");
        }
        try {
            Files.createDirectories(directory);
            writeSecret(directory, secret, derive(seed, instance.id()));
        } catch (IOException e) {
            throw new ZcodeException("无法写入 credential-secret: " + e.getMessage(), e);
        }
        LOG.info("Wrote seed-derived ZCode credential secret for instance " + instance.id());
    }

    /// Rotates the key by discarding the encrypted credentials first. This is
    /// intentionally not a decrypt/re-encrypt: the operator accepted that the
    /// stored API key / login tokens must be entered again.
    static void rotate(ZcodeInstanceManager manager, ZcodeInstance instance,
                       @Nullable String seed, boolean clearProviderKey) throws ZcodeException {
        if (externalSecretConfigured()) {
            throw new ZcodeException("当前进程已配置外部的 ZCODE_CREDENTIAL_SECRET[_FILE]；"
                    + "面板不管理这把密钥，请在外部轮换。");
        }
        Path directory = configDirectory(manager, instance);
        try {
            Files.deleteIfExists(directory.resolve(CREDENTIALS_FILE));
            Files.deleteIfExists(directory.resolve(SOURCE_FILE));
            Files.deleteIfExists(directory.resolve(SECRET_FILE));
            if (clearProviderKey) {
                Files.deleteIfExists(directory.resolve("provider_config.json"));
            }
        } catch (IOException e) {
            throw new ZcodeException("无法清理旧 ZCode 凭据: " + e.getMessage(), e);
        }
        if (seed != null && !seed.isBlank()) {
            writeSeed(manager, instance, seed);
        } else {
            ensure(manager, instance, null);
        }
        LOG.info("Rotated ZCode credential secret for instance " + instance.id());
    }

    /// The key fingerprint ZCode writes into `enc:v2:env:<fingerprint>:...`,
    /// safe to show in the browser (it is SHA-256(SHA-256(secret))[0..24]).
    static @Nullable String fingerprint(ZcodeInstanceManager manager, ZcodeInstance instance) {
        Path secret = configDirectory(manager, instance).resolve(SECRET_FILE);
        if (!Files.isRegularFile(secret)) {
            return null;
        }
        try {
            byte[] secretBytes = Files.readString(secret, StandardCharsets.UTF_8)
                    .getBytes(StandardCharsets.UTF_8);
            byte[] key = MessageDigest.getInstance("SHA-256").digest(secretBytes);
            byte[] fingerprint = MessageDigest.getInstance("SHA-256").digest(key);
            return HexFormat.of().formatHex(fingerprint).substring(0, 24);
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    /// Whether the instance's data already contains v2 ciphertext. Used to
    /// infer the used credential format for manifests written before the field
    /// existed; this is what stops a v2 instance from being moved to a v1
    /// release.
    static boolean hasV2Data(ZcodeInstanceManager manager, ZcodeInstance instance) {
        Path credentials = configDirectory(manager, instance).resolve(CREDENTIALS_FILE);
        if (!Files.isRegularFile(credentials)) {
            return false;
        }
        try {
            return Files.readString(credentials, StandardCharsets.UTF_8).contains("enc:v2:");
        } catch (IOException e) {
            return false;
        }
    }

    static int effectiveFormat(ZcodeInstanceManager manager, ZcodeInstance instance) {
        if (instance.credentialFormat() > 0) {
            return instance.credentialFormat();
        }
        if (hasV2Data(manager, instance)) {
            return 2;
        }
        return ZcodeCatalog.formatOf(instance.version());
    }

    private static byte[] derive(String seed, String instanceId) throws ZcodeException {
        PBEKeySpec spec = new PBEKeySpec(seed.toCharArray(),
                ("zcode-credential:" + instanceId).getBytes(StandardCharsets.UTF_8),
                210_000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ZcodeException("无法从种子派生 ZCode 密钥: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    private static void writeSecret(Path directory, Path secret, byte[] key) throws IOException {
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, SECRET_FILE + ".", ".tmp");
        try {
            String value = Base64.getUrlEncoder().withoutPadding().encodeToString(key);
            Files.writeString(temporary, value, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            restrict(temporary);
            Files.move(temporary, secret);
            restrict(secret);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void validateSize(Path secret) throws ZcodeException {
        try {
            long size = Files.size(secret);
            if (size < MIN_FILE_BYTES || size > MAX_FILE_BYTES) {
                throw new ZcodeException("credential-secret 长度不合法（" + size
                        + " 字节，要求 32–4096）：" + secret);
            }
        } catch (IOException e) {
            throw new ZcodeException("无法读取 credential-secret: " + e.getMessage(), e);
        }
    }

    private static boolean hasV2Ciphertext(Path credentials) throws ZcodeException {
        if (!Files.isRegularFile(credentials)) {
            return false;
        }
        try {
            return Files.readString(credentials, StandardCharsets.UTF_8).contains("enc:v2:");
        } catch (IOException e) {
            throw new ZcodeException("无法读取 ZCode 凭据文件 " + credentials
                    + ": " + e.getMessage(), e);
        }
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static void restrict(Path path) throws IOException {
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            return;
        }
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystem; the data directory's own permissions apply.
        }
    }

    private static boolean externalSecretConfigured() {
        return hasText(System.getenv(SECRET_ENV)) || hasText(System.getenv(SECRET_FILE_ENV));
    }

    private static boolean hasText(@Nullable String value) {
        return value != null && !value.isBlank();
    }
}
