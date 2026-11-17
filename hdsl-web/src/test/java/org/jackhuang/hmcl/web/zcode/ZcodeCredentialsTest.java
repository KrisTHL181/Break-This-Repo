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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZcodeCredentialsTest {

    @TempDir
    Path root;

    private ZcodeInstance instance() {
        return new ZcodeInstance("test", "ZCode", root.resolve("ws").toString(),
                null, null, "token", 0, null, null, 0, 0, 0L);
    }

    private Path secret(ZcodeInstanceManager manager, ZcodeInstance instance) {
        return ZcodeCredentials.configDirectory(manager, instance)
                .resolve(ZcodeCredentials.SECRET_FILE);
    }

    @Test
    void generatesOnceAndKeepsTheSameSecret() throws Exception {
        ZcodeInstanceManager manager = new ZcodeInstanceManager(root);
        ZcodeInstance instance = instance();

        ZcodeCredentials.ensure(manager, instance, Map.of());
        Path secret = secret(manager, instance);
        assertTrue(Files.isRegularFile(secret));
        String first = Files.readString(secret, StandardCharsets.UTF_8);
        assertEquals(43, first.length());

        ZcodeCredentials.ensure(manager, instance, Map.of());
        assertEquals(first, Files.readString(secret, StandardCharsets.UTF_8));

        if (secret.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                    Files.getPosixFilePermissions(secret));
        }
    }

    @Test
    void seedDerivationIsStableAndRotateRewritesIt() throws Exception {
        ZcodeInstanceManager manager = new ZcodeInstanceManager(root);
        ZcodeInstance instance = instance();

        ZcodeCredentials.writeSeed(manager, instance, "correct horse battery staple");
        Path secret = secret(manager, instance);
        String first = Files.readString(secret, StandardCharsets.UTF_8);

        ZcodeCredentials.rotate(manager, instance, "correct horse battery staple", true);
        assertEquals(first, Files.readString(secret, StandardCharsets.UTF_8));
        assertTrue(ZcodeCredentials.fingerprint(manager, instance) != null);
    }

    @Test
    void respectsExplicitSecretEnvironment() throws Exception {
        ZcodeInstanceManager manager = new ZcodeInstanceManager(root);
        ZcodeInstance instance = instance();

        ZcodeCredentials.ensure(manager, instance,
                Map.of(ZcodeCredentials.SECRET_FILE_ENV, "/tmp/operator-secret"));

        assertFalse(Files.exists(secret(manager, instance)));
    }

    @Test
    void refusesToReplaceMissingKeyForV2Data() throws Exception {
        ZcodeInstanceManager manager = new ZcodeInstanceManager(root);
        ZcodeInstance instance = instance();
        Path directory = ZcodeCredentials.configDirectory(manager, instance);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(ZcodeCredentials.CREDENTIALS_FILE),
                "{\"token\":\"enc:v2:env:deadbeef:payload\"}", StandardCharsets.UTF_8);

        assertThrows(ZcodeException.class,
                () -> ZcodeCredentials.ensure(manager, instance, Map.of()));
    }

    @Test
    void refusesToOverridePinnedSource() throws Exception {
        ZcodeInstanceManager manager = new ZcodeInstanceManager(root);
        ZcodeInstance instance = instance();
        Path directory = ZcodeCredentials.configDirectory(manager, instance);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(ZcodeCredentials.SOURCE_FILE), "os",
                StandardCharsets.UTF_8);

        assertThrows(ZcodeException.class,
                () -> ZcodeCredentials.ensure(manager, instance, Map.of()));
    }

    @Test
    void rejectsExistingSecretOutsideLengthContract() throws Exception {
        ZcodeInstanceManager manager = new ZcodeInstanceManager(root);
        ZcodeInstance instance = instance();
        Path secret = secret(manager, instance);
        Files.createDirectories(secret.getParent());
        Files.writeString(secret, "too-short", StandardCharsets.UTF_8);

        assertThrows(ZcodeException.class,
                () -> ZcodeCredentials.ensure(manager, instance, Map.of()));
    }
}
