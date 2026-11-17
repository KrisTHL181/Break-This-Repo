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

/// Whether a new instance keeps its own home.
///
/// The original asks the same question when it installs a version, and for the same reason: a
/// profile that carries its own plugins and its own settings should keep them to itself, while a
/// bare one is often meant to share. Its rule is a policy rather than a switch — always, never, or
/// when the thing being installed brings something of its own — and the middle answer is the
/// default, because that is the one that needs no thought.
@NotNullByDefault
public enum DshIsolationPolicy {
    /// Every new instance keeps its own home.
    ALWAYS("always"),

    /// A new instance keeps its own home when it is given plugins of its own.
    WITH_PLUGINS("modded"),

    /// New instances share one home unless somebody says otherwise.
    NEVER("never");

    /// The value the setting is stored as.
    private final String id;

    /// Creates a policy.
    ///
    /// @param id the stored value
    DshIsolationPolicy(String id) {
        this.id = id;
    }

    /// Returns the value the setting is stored as.
    ///
    /// @return the id
    public String id() {
        return id;
    }

    /// Reads a stored value.
    ///
    /// @param value the stored value, or `null`
    /// @return the policy, or [DshIsolationPolicy#WITH_PLUGINS] when it is not one
    public static DshIsolationPolicy of(@Nullable String value) {
        if (value != null) {
            for (DshIsolationPolicy policy : values()) {
                if (policy.id.equalsIgnoreCase(value.trim())) {
                    return policy;
                }
            }
        }
        return WITH_PLUGINS;
    }

    /// Decides how a new instance keeps its state.
    ///
    /// @param carriesItsOwn whether the installation brings plugins or presets of its own
    /// @param shared        the mode used when the instance does not keep a home of its own
    /// @return the mode to create the instance with
    public DshHomeMode homeMode(boolean carriesItsOwn, DshHomeMode shared) {
        return switch (this) {
            case ALWAYS -> DshHomeMode.ISOLATED;
            case NEVER -> shared;
            case WITH_PLUGINS -> carriesItsOwn ? DshHomeMode.ISOLATED : shared;
        };
    }
}
