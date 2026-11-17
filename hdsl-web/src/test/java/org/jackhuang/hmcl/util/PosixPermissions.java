/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2025 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.util;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// POSIX file permissions, on the platforms that have them.
///
/// The launcher asks for `0600` on the files that hold a credential — the user store, an
/// uploaded private key, a generated one. Windows has no POSIX mode bits: access is expressed
/// through ACLs, and `Files.getPosixFilePermissions` answers `UnsupportedOperationException`
/// rather than an approximation. That is why the production code catches it and a warning is
/// all that happens, and it is why a test asserting `0600` fails there for a reason that has
/// nothing to do with the launcher.
///
/// These helpers let such an assertion stay exactly as strong as it was on the platforms where
/// it means something, without deleting it or skipping the whole test — the rest of a test like
/// `loginLogoutAndWhoamiFlow` has nothing to do with permissions and should keep running.
@NotNullByDefault
public final class PosixPermissions {
    private PosixPermissions() {
    }

    /// Returns the permissions of a path, or empty where the platform has no mode bits.
    ///
    /// @param path the path to read
    /// @return the permissions, or empty when the platform cannot express them
    public static Optional<Set<PosixFilePermission>> of(Path path) {
        try {
            return Optional.of(Files.getPosixFilePermissions(path));
        } catch (UnsupportedOperationException e) {
            return Optional.empty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Asserts a file is readable and writable by its owner and by nobody else.
    ///
    /// @param path the file
    /// @param message what the file is, for the failure
    public static void assertOwnerOnly(Path path, String message) {
        of(path).ifPresent(permissions -> assertEquals(
                Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), permissions, message));
    }

    /// Asserts a directory is owner-only, and executable by its owner to be usable at all.
    ///
    /// @param path the directory
    /// @param message what the directory is, for the failure
    public static void assertOwnerOnlyDirectory(Path path, String message) {
        of(path).ifPresent(permissions -> assertEquals(
                Set.of(PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE), permissions, message));
    }
}
