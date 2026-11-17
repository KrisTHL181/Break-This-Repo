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

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

/// A Node.js runtime installed under [DshPaths#RUNTIMES].
///
/// Unlike [DshNodeRuntime], which describes whatever the system happens to have
/// on `PATH`, this is a runtime the launcher owns: it is versioned, isolated and
/// safe to pin an instance to. Where its executables live is the platform's
/// business — [RuntimeLayout] answers that, in both shapes.
///
/// @param version   the version without the leading `v`
/// @param directory the directory holding the unpacked distribution
@NotNullByDefault
public record NodeRuntime(String version, Path directory) {
    /// Returns the `node` executable.
    ///
    /// @return `bin/node`, or `node.exe` on Windows
    public Path node() {
        return RuntimeLayout.CURRENT.node(directory);
    }

    /// Returns the `npm` entry point.
    ///
    /// On POSIX the distribution ships `bin/npm` as a symlink into `lib/node_modules`;
    /// on Windows it ships `npm.cmd` beside `node.exe`. Both are what
    /// [RuntimeLayout] names.
    ///
    /// @return `bin/npm`, or `npm.cmd` on Windows
    public Path npm() {
        return RuntimeLayout.CURRENT.npm(directory);
    }

    /// Returns the `npx` entry point.
    ///
    /// @return `bin/npx`, or `npx.cmd` on Windows
    public Path npx() {
        return RuntimeLayout.CURRENT.npx(directory);
    }

    /// Returns the `pnpm` installed into this runtime, if any.
    ///
    /// @return `bin/pnpm`, or `pnpm.cmd` on Windows
    public Path pnpm() {
        return RuntimeLayout.CURRENT.pnpm(directory);
    }

    /// Returns the directory to put ahead of `PATH` for this runtime.
    ///
    /// @return `bin/`, or the distribution root on Windows
    public Path binDirectory() {
        return RuntimeLayout.CURRENT.binDirectory(directory);
    }

    /// Reports whether the archive was unpacked completely.
    ///
    /// @return whether the `node` executable exists
    public boolean isUsable() {
        return RuntimeLayout.CURRENT.isUsable(directory);
    }
}
