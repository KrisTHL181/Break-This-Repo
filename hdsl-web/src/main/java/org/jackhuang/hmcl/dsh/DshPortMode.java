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

/// How the browser surface's port is chosen for an instance.
///
/// The port is not incidental: DeepSeek Harness keys its browser-facing session
/// state by origin, so reaching one instance's history through two different
/// ports lets two writers touch the same history and corrupt it. An instance
/// therefore keeps one port for its whole life, whichever mode is in use.
@NotNullByDefault
public enum DshPortMode {
    /// Pick a free port on first launch and keep it from then on.
    AUTO,

    /// Use the port the user chose.
    FIXED,

    /// Follow the launcher's own policy, which is what an instance starts at.
    GLOBAL;

    /// Returns the name used in settings files and on the command line.
    ///
    /// @return the lower-case name
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    /// Resolves a mode from its stored name.
    ///
    /// @param value the stored name, possibly `null` or of the wrong case
    /// @return the matching mode, or [DshPortMode#AUTO] when unrecognised
    public static DshPortMode of(@org.jetbrains.annotations.Nullable String value) {
        if (value != null) {
            for (DshPortMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
        }
        return AUTO;
    }
}
