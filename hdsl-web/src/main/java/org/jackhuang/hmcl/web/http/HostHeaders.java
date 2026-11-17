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
package org.jackhuang.hmcl.web.http;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// Host header helpers, shared by the servlets and the server that each used to
/// carry (or, in the brand servlet's case, mis-carry) their own copy.
@NotNullByDefault
public final class HostHeaders {

    private HostHeaders() {
    }

    /// Drops the `:port` suffix of a Host header, keeping IPv6 brackets
    /// intact: `[::1]:8080` → `[::1]`, `example.com:80` → `example.com`.
    /// A suffix that is not all digits is not a port and is kept; so is a
    /// bare IPv6 literal without brackets.
    ///
    /// @param host the Host header value, or `null`
    /// @return the host without its port, or `null` when the header was absent
    public static @Nullable String stripPort(@Nullable String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String trimmed = host.trim();
        if (trimmed.startsWith("[")) {
            int close = trimmed.indexOf(']');
            return close >= 0 ? trimmed.substring(0, close + 1) : trimmed;
        }
        int colon = trimmed.lastIndexOf(':');
        if (colon > 0 && colon == trimmed.indexOf(':')) {
            String suffix = trimmed.substring(colon + 1);
            if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
                return trimmed.substring(0, colon);
            }
        }
        return trimmed;
    }
}
