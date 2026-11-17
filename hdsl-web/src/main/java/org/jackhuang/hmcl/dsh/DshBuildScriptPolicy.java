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

/// What happens when a plugin wants to run an install script.
///
/// The script runs with the user's own rights and nobody has read it, which is why a
/// package manager stops and asks. Every answer is somebody's decision, so the launcher
/// offers all three rather than choosing: run them without asking, ask every time, or
/// never run them at all.
@NotNullByDefault
public enum DshBuildScriptPolicy {
    /// Answer yes and carry on, without asking.
    AUTO("auto"),

    /// Ask, with what is at stake, and do what the person says.
    MANUAL("manual"),

    /// Answer no: the package is installed and its script is not run.
    NEVER("never");

    /// The value the setting is stored as.
    private final String id;

    /// Creates a policy.
    ///
    /// @param id the stored value
    DshBuildScriptPolicy(String id) {
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
    /// @return the policy, or `null` when the value is not one
    public static @Nullable DshBuildScriptPolicy of(@Nullable String value) {
        if (value == null) {
            return null;
        }
        for (DshBuildScriptPolicy policy : values()) {
            if (policy.id.equalsIgnoreCase(value.trim())) {
                return policy;
            }
        }
        // Written before there were three answers: yes meant run without asking,
        // and no meant do not run them.
        if (value.equalsIgnoreCase("true")) {
            return AUTO;
        }
        if (value.equalsIgnoreCase("false")) {
            return NEVER;
        }
        return null;
    }

    /// Returns the policy an instance runs under.
    ///
    /// @param instance the instance
    /// @return the policy, the launcher-wide one when the instance follows it
    public static DshBuildScriptPolicy of(DshInstance instance) {
        Boolean legacy = DshInstanceSettings.approveBuildScripts(instance);
        if (legacy != null) {
            return legacy ? AUTO : NEVER;
        }
        String own = DshInstanceSettings.buildScriptPolicy(instance);
        DshBuildScriptPolicy policy = of(own);
        return policy != null ? policy
                : org.jackhuang.hmcl.setting.SettingsManager.settings().buildScriptPolicy();
    }
}
