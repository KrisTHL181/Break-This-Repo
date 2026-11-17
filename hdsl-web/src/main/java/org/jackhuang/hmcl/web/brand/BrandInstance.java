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
package org.jackhuang.hmcl.web.brand;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// One brand-category instance: a [Brand] tool pinned to one installed version,
/// plus the panel-side record of how to launch it.
///
/// The record is deliberately thin, mirroring
/// [org.jackhuang.hmcl.web.zcode.ZcodeInstance]: the tool's own state
/// (configuration, sessions, credentials) lives in the instance's `home/`
/// directory — the child process's `HOME` — never in this manifest. The brand
/// id is stored denormalized so the manifest on disk is self-describing.
@NotNullByDefault
public record BrandInstance(
        @SerializedName("id") String id,
        @SerializedName("brand") String brand,
        @SerializedName("name") String name,
        @SerializedName("version") String version,
        @SerializedName("baseUrl") @Nullable String baseUrl,
        @SerializedName("lastPort") int lastPort,
        @SerializedName("createdAt") long createdAt,
        @SerializedName("nodeRuntime") @Nullable String nodeRuntime,
        /// `null` means the historical default, which is **web on**: manifests
        /// written before PI WEB existed have no such field and every other
        /// brand's web mode is the whole point of its instance.
        @SerializedName("webEnabled") @Nullable Boolean webEnabled,
        /// A locked instance keeps the release it was created on when the brand
        /// moves to a newer verified pair. `null` means unlocked.
        @SerializedName("versionLocked") @Nullable Boolean versionLocked) {

    /// The pre-PI-WEB shape, kept for callers that do not care about the two
    /// new choices (tests and the other brands' manifests): web on, unlocked.
    public BrandInstance(String id, String brand, String name, String version, @Nullable String baseUrl,
                         int lastPort, long createdAt, @Nullable String nodeRuntime) {
        this(id, brand, name, version, baseUrl, lastPort, createdAt, nodeRuntime, null, null);
    }

    /// Whether this instance starts its web process at all.
    ///
    /// PI WEB is the only brand with a terminal-only form; for every other
    /// brand the answer is `true` whatever the manifest says, because the
    /// panel's whole surface for them is the web UI.
    public boolean webEnabledOrDefault() {
        return webEnabled == null || webEnabled;
    }

    /// Whether the instance is pinned to its current release.
    public boolean versionLockedOrDefault() {
        return versionLocked != null && versionLocked;
    }

    public BrandInstance withName(String newName) {
        return new BrandInstance(id, brand, newName, version, baseUrl, lastPort, createdAt, nodeRuntime,
                webEnabled, versionLocked);
    }

    public BrandInstance withVersion(String newVersion) {
        return new BrandInstance(id, brand, name, newVersion, baseUrl, lastPort, createdAt, nodeRuntime,
                webEnabled, versionLocked);
    }

    public BrandInstance withLastPort(int newLastPort) {
        return new BrandInstance(id, brand, name, version, baseUrl, newLastPort, createdAt, nodeRuntime,
                webEnabled, versionLocked);
    }

    /// The endpoint the tool should talk to instead of its own default.
    ///
    /// Only Claude Web uses this today (`ANTHROPIC_BASE_URL`): the official
    /// kernel is refused outright in unsupported regions, and a self-hosted or
    /// otherwise reachable gateway is the documented way around it. A blank
    /// value means "keep the tool's own default", so old manifests and the
    /// other brands carry `null`.
    ///
    /// @param newBaseUrl the endpoint, or `null`/blank for the default
    /// @return the instance with the endpoint recorded
    public BrandInstance withBaseUrl(@Nullable String newBaseUrl) {
        return new BrandInstance(id, brand, name, version, newBaseUrl, lastPort, createdAt, nodeRuntime,
                webEnabled, versionLocked);
    }

    /// The Node.js selector this instance runs on, or `null` to follow the
    /// brand default. The syntax is [org.jackhuang.hmcl.dsh.NodeSelection]'s:
    /// `auto`, a major (`22`), an exact version, or `inherit`.
    ///
    /// @param newRuntime the selector, or `null`
    /// @return the instance with the selector recorded
    public BrandInstance withNodeRuntime(@Nullable String newRuntime) {
        return new BrandInstance(id, brand, name, version, baseUrl, lastPort, createdAt, newRuntime,
                webEnabled, versionLocked);
    }

    /// The terminal-only / terminal-and-web choice.
    ///
    /// @param enabled whether the web process should start with this instance
    /// @return the instance with the choice recorded
    public BrandInstance withWebEnabled(boolean enabled) {
        return new BrandInstance(id, brand, name, version, baseUrl, lastPort, createdAt, nodeRuntime,
                enabled, versionLocked);
    }

    /// Whether the instance follows the brand's verified pair or keeps its own.
    ///
    /// @param locked whether the current release is pinned
    /// @return the instance with the lock recorded
    public BrandInstance withVersionLocked(boolean locked) {
        return new BrandInstance(id, brand, name, version, baseUrl, lastPort, createdAt, nodeRuntime,
                webEnabled, locked);
    }
}
