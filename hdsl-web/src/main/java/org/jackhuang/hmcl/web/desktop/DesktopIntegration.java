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
package org.jackhuang.hmcl.web.desktop;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Windows desktop conveniences: open a folder in Explorer, a text file in
/// Notepad.
///
/// These are only meaningful when a person is sitting at the machine — a bare
/// Windows deployment has a desktop, and the operator is already able to do
/// the same thing by hand. They do **not** widen the local attack surface;
/// the endpoints that call them still sit behind the panel's session gate and
/// only ever receive paths the server computed from an instance record.
@NotNullByDefault
public final class DesktopIntegration {

    private DesktopIntegration() {
    }

    /// Whether this process has an interactive Windows desktop to open windows
    /// on: a console or RDP session has `SESSIONNAME`; a Windows service
    /// (session 0) does not, and a window started there would be invisible.
    /// The endpoints answer 409 instead of pretending it worked.
    public static boolean supported() {
        return OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS
                && System.getenv("SESSIONNAME") != null;
    }

    /// Opens `directory` in Explorer.
    ///
    /// @throws IOException when the platform has no desktop integration or the
    ///                     directory does not exist
    public static void reveal(Path directory) throws IOException {
        requireDirectory(directory);
        run("explorer.exe", directory.toString());
    }

    /// Opens `file` in Notepad.
    public static void openText(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("no such file: " + file);
        }
        run("notepad.exe", file.toString());
    }

    private static void requireDirectory(Path directory) throws IOException {
        if (!supported()) {
            throw new IOException("desktop integration is only available on Windows");
        }
        if (!Files.isDirectory(directory)) {
            throw new IOException("no such directory: " + directory);
        }
    }

    private static void run(String... command) throws IOException {
        if (!supported()) {
            throw new IOException("desktop integration is only available on Windows");
        }
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            throw new IOException("could not start " + command[0] + ": " + e.getMessage(), e);
        }
    }
}
