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
import org.jetbrains.annotations.Nullable;

/// A Node.js release published on `nodejs.org`.
///
/// The launcher manages Node the way HMCL manages Java runtimes: DeepSeek
/// Harness declares `engines.node` as `^22.19.0 || >=24.0.0`, so a machine
/// whose system Node falls outside that range needs a runtime the launcher
/// installs and pins itself.
///
/// @param version the version without the leading `v`, for example `22.11.0`
/// @param lts     the LTS codename, or `null` for a non-LTS release
/// @param date    the release date as published
@NotNullByDefault
public record NodeRelease(String version, @Nullable String lts, String date) {
    /// Reports whether this release is an LTS line.
    ///
    /// @return whether the release carries an LTS codename
    public boolean isLts() {
        return lts != null && !lts.isBlank();
    }

    /// Reports whether this release satisfies DeepSeek Harness's Node range.
    ///
    /// @return whether the version is usable
    public boolean isSupported() {
        return DshNodeRuntime.isSupportedNodeVersion(version);
    }

    /// Returns the label shown next to the version.
    ///
    /// @return the LTS codename, or an empty string
    public String label() {
        return isLts() ? lts : "";
    }
}
