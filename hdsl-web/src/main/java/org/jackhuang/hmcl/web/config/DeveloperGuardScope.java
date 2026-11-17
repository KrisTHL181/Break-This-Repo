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
package org.jackhuang.hmcl.web.config;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// How far the developer elevation reaches (`developer_guard.scope`).
///
/// Each level includes the one above it, so an operator picks the smallest
/// scope that covers what they are worried about:
///
/// - [OFF] — the panel session is the only door (the default, and the
///   behaviour of every release before the guard existed);
/// - [PROXY] — the instance web pages (`/i/<id>/*`, HTTP and WebSocket) also
///   need the developer password;
/// - [INSTANCE] — and so do the **state-changing** instance APIs (start, stop,
///   delete, installs, settings). Reads stay open so the list is still usable;
/// - [ALL] — every `/api/*` request except [#alwaysAllowed(String)], which is
///   the minimum a browser needs to log in and to reach the developer page.
///
/// The scope only decides which *new* requests are gated. An already-open page,
/// terminal or agent is untouched: that is the emergency stop's job, not the
/// guard's.
@NotNullByDefault
public enum DeveloperGuardScope {
    OFF("off"),
    PROXY("proxy"),
    INSTANCE("instance"),
    ALL("all");

    private final String wireName;

    DeveloperGuardScope(String wireName) {
        this.wireName = wireName;
    }

    /// The value as it appears in `server.yaml` and in the API.
    public String wireName() {
        return wireName;
    }

    /// Parses a configuration value, answering `null` when it is not one of
    /// [#wireName] (the caller turns that into a configuration error).
    public static @Nullable DeveloperGuardScope parse(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        for (DeveloperGuardScope scope : values()) {
            if (scope.wireName.equalsIgnoreCase(trimmed)) {
                return scope;
            }
        }
        return null;
    }

    /// Whether the instance web pages are gated.
    public boolean coversProxy() {
        return this != OFF;
    }

    /// Whether state-changing instance APIs are gated.
    public boolean coversInstanceActions() {
        return this == INSTANCE || this == ALL;
    }

    /// Whether every `/api/*` request except the allowlist is gated.
    public boolean coversAllApi() {
        return this == ALL;
    }

    /// Paths that stay reachable at every scope: without them the browser could
    /// not log in, could not elevate, and could not read the very status that
    /// explains why it is being refused.
    ///
    /// Deliberately narrow: health, the auth endpoints, the developer endpoints
    /// themselves, and `/api/system/*` — the host readings are exactly what a
    /// shell in the same container can read, so a password prompt there would
    /// be theatre, not a boundary (and the monitor card must work at [ALL] or
    /// the general page would look broken).
    public static boolean alwaysAllowed(String path) {
        if (path.startsWith("/api/auth/")) {
            return true;
        }
        if (path.startsWith("/api/developer/")) {
            return true;
        }
        if (path.startsWith("/api/system/")) {
            return true;
        }
        return path.equals("/api/health") || path.equals("/api/version") || path.equals("/api");
    }

    /// Whether a state-changing method on an instance-family path is gated at
    /// [INSTANCE]: the mutation verbs on the routes that start processes, write
    /// files or change settings.
    public static boolean instanceMutation(String path, String method) {
        if (!isMutation(method)) {
            return false;
        }
        return path.startsWith("/api/instances/")
                || path.startsWith("/api/brands/")
                || path.startsWith("/api/zcode/")
                || path.startsWith("/api/packs/")
                || path.startsWith("/api/settings/")
                || path.startsWith("/api/node/")
                || path.startsWith("/api/versions");
    }

    private static boolean isMutation(String method) {
        return switch (method) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }
}
