/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Files;
import java.nio.file.Path;

/// Where an unpacked Node.js distribution keeps the files it is driven through.
///
/// The two shapes are not variations on one theme, they are two layouts that share
/// nothing but the version of Node inside them:
///
/// | | Linux / macOS | Windows |
/// |---|---|---|
/// | `node` | `bin/node` | `node.exe` |
/// | `npm` | `bin/npm` | `npm.cmd` |
/// | `npx` | `bin/npx` | `npx.cmd` |
/// | `pnpm`, once installed | `bin/pnpm` | `pnpm.cmd` |
/// | what goes on `PATH` | `bin/` | the root |
///
/// On Linux `npm` and `npx` are symlinks into `lib/node_modules`; on Windows they are
/// batch shims that find `node.exe` *beside themselves* — which is why the directory to
/// put on `PATH` is the root there and `bin/` elsewhere, and why asking for
/// `bin/npm` on Windows does not fail loudly but simply never finds anything.
///
/// The platform is a parameter rather than a constant so the Windows shape can be
/// pinned by a test on any machine — the same reason [NodeRuntimeManager#platformTag]
/// gained the same parameter.
///
/// @param os the platform this layout describes
@NotNullByDefault
public record RuntimeLayout(OperatingSystem os) {
    /// The layout of the running system.
    public static final RuntimeLayout CURRENT = new RuntimeLayout(OperatingSystem.CURRENT_OS);

    /// The layout of a Linux or macOS distribution.
    public static final RuntimeLayout POSIX = new RuntimeLayout(OperatingSystem.LINUX);

    /// The layout of a Windows distribution.
    public static final RuntimeLayout WINDOWS = new RuntimeLayout(OperatingSystem.WINDOWS);

    /// Returns the `node` executable of a distribution.
    ///
    /// @param directory the distribution root
    /// @return `bin/node`, or `node.exe` on Windows
    public Path node(Path directory) {
        return os == OperatingSystem.WINDOWS
                ? directory.resolve("node.exe")
                : directory.resolve("bin/node");
    }

    /// Returns the `npm` entry point of a distribution.
    ///
    /// @param directory the distribution root
    /// @return `bin/npm`, or `npm.cmd` on Windows
    public Path npm(Path directory) {
        return os == OperatingSystem.WINDOWS
                ? directory.resolve("npm.cmd")
                : directory.resolve("bin/npm");
    }

    /// Returns the `npx` entry point of a distribution.
    ///
    /// @param directory the distribution root
    /// @return `bin/npx`, or `npx.cmd` on Windows
    public Path npx(Path directory) {
        return os == OperatingSystem.WINDOWS
                ? directory.resolve("npx.cmd")
                : directory.resolve("bin/npx");
    }

    /// Returns the `pnpm` an install into this distribution leaves behind.
    ///
    /// A Node distribution ships no pnpm: it is installed into the distribution's own
    /// global prefix afterwards, and that prefix is the root — so the shim lands in
    /// `bin/` on POSIX and in the root on Windows, beside `node.exe`.
    ///
    /// @param directory the distribution root
    /// @return `bin/pnpm`, or `pnpm.cmd` on Windows
    public Path pnpm(Path directory) {
        return os == OperatingSystem.WINDOWS
                ? directory.resolve("pnpm.cmd")
                : directory.resolve("bin/pnpm");
    }

    /// Returns the directory to put ahead of `PATH` for this distribution.
    ///
    /// A Windows `npm.cmd` locates `node.exe` through its own directory, so the root is
    /// what makes a managed runtime self-contained there; elsewhere `bin/` is what holds
    /// the entries that matter.
    ///
    /// @param directory the distribution root
    /// @return the directory to prepend to `PATH`
    public Path binDirectory(Path directory) {
        return os == OperatingSystem.WINDOWS ? directory : directory.resolve("bin");
    }

    /// Reports whether a distribution was unpacked completely.
    ///
    /// @param directory the distribution root
    /// @return whether the `node` executable is there
    public boolean isUsable(Path directory) {
        Path node = node(directory);
        return Files.isRegularFile(node) || Files.isSymbolicLink(node);
    }

}
