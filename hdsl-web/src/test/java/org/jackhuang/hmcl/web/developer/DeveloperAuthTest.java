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
package org.jackhuang.hmcl.web.developer;

import org.jackhuang.hmcl.util.PosixPermissions;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The developer password store: set/verify semantics, the policy rules
/// (length, must differ from the panel password), persistence across
/// reopens, clearing, and the 0600-on-disk promise where POSIX exists.
class DeveloperAuthTest {

    private static final String PANEL_PASSWORD = "panel-password-1";
    private static final String DEV_PASSWORD = "dev-password-1";

    @TempDir
    Path dataDir;

    private Path storeFile() {
        return dataDir.resolve("auth").resolve(DeveloperAuth.FILE_NAME);
    }

    @Test
    void setVerifyAndClearRoundTrip() throws Exception {
        UserStore users = UserStore.open(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), Logger.LOG);
        DeveloperAuth auth = DeveloperAuth.open(dataDir);

        assertFalse(auth.isSet(), "a fresh store has no developer password");
        assertFalse(auth.verify(DEV_PASSWORD), "verifying an unset password is simply false");

        auth.set(DEV_PASSWORD, users);
        assertTrue(auth.isSet());
        assertTrue(auth.verify(DEV_PASSWORD));
        assertFalse(auth.verify("wrong-password"));
        assertFalse(auth.verify(PANEL_PASSWORD), "the panel password must not verify as the developer password");

        auth.clear();
        assertFalse(auth.isSet());
        assertFalse(auth.verify(DEV_PASSWORD));
        assertFalse(Files.exists(storeFile()), "clearing removes the file");
    }

    @Test
    void theStoredHashSurvivesAReopen() throws Exception {
        UserStore users = UserStore.open(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), Logger.LOG);
        DeveloperAuth.open(dataDir).set(DEV_PASSWORD, users);

        DeveloperAuth reopened = DeveloperAuth.open(dataDir);
        assertTrue(reopened.isSet());
        assertTrue(reopened.verify(DEV_PASSWORD));
        assertFalse(reopened.verify("wrong-password"));
    }

    @Test
    void shortPasswordsAndThePanelPasswordAreRefused() throws Exception {
        UserStore users = UserStore.open(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), Logger.LOG);
        DeveloperAuth auth = DeveloperAuth.open(dataDir);

        assertThrows(IllegalArgumentException.class, () -> auth.set("short", users));
        assertThrows(IllegalArgumentException.class, () -> auth.set(PANEL_PASSWORD, users),
                "the developer password must differ from the panel password");
        assertFalse(auth.isSet(), "a refused set leaves nothing behind");

        // The panel user store of a data dir without any user cannot enforce
        // the difference — but every other rule still applies.
        Path empty = dataDir.resolve("no-user-yet");
        UserStore uninitialized = UserStore.open(empty, Map.of(), Logger.LOG);
        DeveloperAuth auth2 = DeveloperAuth.open(empty);
        auth2.set(DEV_PASSWORD, uninitialized);
        assertTrue(auth2.verify(DEV_PASSWORD));
    }

    @Test
    void theStoreFileIsOwnerOnlyWherePosixExists() throws Exception {
        UserStore users = UserStore.open(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), Logger.LOG);
        DeveloperAuth.open(dataDir).set(DEV_PASSWORD, users);

        assumeTrue(PosixPermissions.of(storeFile()).isPresent(), "POSIX mode bits exist on this platform");
        PosixPermissions.assertOwnerOnly(storeFile(), "the developer password hash must be 0600");
    }

    @Test
    void replacingAHashKeepsTheFileIntact() throws Exception {
        UserStore users = UserStore.open(dataDir, Map.of(UserStore.ENV_ADMIN_PASSWORD, PANEL_PASSWORD), Logger.LOG);
        DeveloperAuth auth = DeveloperAuth.open(dataDir);
        auth.set(DEV_PASSWORD, users);
        assertDoesNotThrow(() -> auth.set("dev-password-2", users));
        assertTrue(auth.verify("dev-password-2"));
        assertFalse(auth.verify(DEV_PASSWORD), "the old password stops working immediately");
    }
}
