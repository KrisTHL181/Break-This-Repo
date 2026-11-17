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

/// How the launcher reaches the network.
///
/// The original asks the same four things in the same order, and for the same reason: most people
/// want whatever the system already uses, some want no proxy at all, and the rest have a host and a
/// port to type. Which of them is chosen decides whether the two fields below mean anything.
@NotNullByDefault
public enum DshProxyMode {
    /// Whatever the system is configured to use, which is to say nothing of the launcher's own.
    SYSTEM("system"),

    /// No proxy, even if the system has one.
    NONE("none"),

    /// An HTTP proxy at a host and port.
    HTTP("http"),

    /// A SOCKS proxy at a host and port.
    SOCKS("socks");

    /// The value the setting is stored as.
    private final String id;

    /// Creates a mode.
    ///
    /// @param id the stored value
    DshProxyMode(String id) {
        this.id = id;
    }

    /// Returns the value the setting is stored as.
    ///
    /// @return the id
    public String id() {
        return id;
    }

    /// Returns whether this mode needs a host and a port.
    ///
    /// @return whether it does
    public boolean usesAddress() {
        return this == HTTP || this == SOCKS;
    }

    /// Reads a stored value.
    ///
    /// @param value the stored value, or `null`
    /// @return the mode, or [DshProxyMode#SYSTEM] when it is not one
    public static DshProxyMode of(@Nullable String value) {
        if (value != null) {
            for (DshProxyMode mode : values()) {
                if (mode.id.equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
        }
        return SYSTEM;
    }
}
