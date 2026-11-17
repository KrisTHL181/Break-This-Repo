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

import java.util.Locale;

/// Where Node.js distributions are fetched from.
///
/// The published index and the archives under it are the one thing this launcher
/// downloads that does not come from npm, and npm's own registry is the only
/// download endpoint the launcher can be pointed at from outside — by the user's
/// `.npmrc`. A network that reaches a mirror of npm but not `nodejs.org` — which
/// is what a blocked or intercepted route to it looks like — can therefore run
/// DeepSeek Harness and its plugins but cannot install a Node runtime at all. The
/// original answers the same problem with a download source of its own, chosen in
/// the launcher's settings; this is that answer for the one download here that
/// needs it.
///
/// A mirror is a whole second base URL rather than a rewritten one: the index and
/// the archives keep their layout, which is what makes the same code fetch from
/// either.
@NotNullByDefault
public enum NodeSource {
    /// The distribution the Node.js project publishes.
    OFFICIAL("official", "https://nodejs.org/dist/"),

    /// The mirror the npm registry the launcher already reaches is run beside.
    MIRROR("mirror", "https://npmmirror.com/mirrors/node/");

    /// The name used in settings files.
    private final String id;

    /// The base URL the index and the archives live under.
    private final String base;

    NodeSource(String id, String base) {
        this.id = id;
        this.base = base;
    }

    /// Returns the name used in settings files.
    ///
    /// @return the identifier
    public String id() {
        return id;
    }

    /// Returns the host the source fetches from, for messages.
    ///
    /// @return the host
    public String host() {
        String withoutScheme = base.substring(base.indexOf("//") + 2);
        int slash = withoutScheme.indexOf('/');
        return slash < 0 ? withoutScheme : withoutScheme.substring(0, slash);
    }

    /// Returns the URL of the release index.
    ///
    /// @return the URL
    public String indexUrl() {
        return base + "index.json";
    }

    /// Returns the URL of one release's archive.
    ///
    /// @param version  the version without its leading `v`
    /// @param fileName the archive's file name
    /// @return the URL
    public String archiveUrl(String version, String fileName) {
        return base + "v" + version + "/" + fileName;
    }

    /// Returns the URL of the release's published SHA-256 list. Not every mirror
    /// carries it; the installer falls back to the other source and records the
    /// digest as unverified when neither does.
    ///
    /// @param version the version without its leading `v`
    /// @return the URL
    public String checksumsUrl(String version) {
        return base + "v" + version + "/SHASUMS256.txt";
    }

    /// Resolves a source from its stored name.
    ///
    /// @param value the stored name, possibly `null` or of the wrong case
    /// @return the matching source, or [NodeSource#OFFICIAL] when unrecognised
    public static NodeSource of(@Nullable String value) {
        if (value != null) {
            for (NodeSource source : values()) {
                if (source.id.equals(value.trim().toLowerCase(Locale.ROOT))) {
                    return source;
                }
            }
        }
        return OFFICIAL;
    }
}
