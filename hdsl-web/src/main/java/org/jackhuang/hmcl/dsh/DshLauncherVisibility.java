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

/// What the launcher does with itself once an instance is running.
///
/// An instance of DeepSeek Harness is used in a browser, so the launcher has nothing to show
/// while it runs — and the original offers the same choice for the same reason. Either way,
/// the launcher comes back when the instance is stopped: a window that hides itself and never
/// returns is a window somebody has to hunt for.
@NotNullByDefault
public enum DshLauncherVisibility {
    /// Stay where it is.
    KEEP("keep"),

    /// Get out of the way entirely.
    HIDE("hide"),

    /// Stay in the task bar, out of the way.
    MINIMIZE("minimize");

    /// The value the setting is stored as.
    private final String id;

    /// Creates a choice.
    ///
    /// @param id the stored value
    DshLauncherVisibility(String id) {
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
    /// @return the choice, or [DshLauncherVisibility#KEEP] when it is not one
    public static DshLauncherVisibility of(@Nullable String value) {
        if (value != null) {
            for (DshLauncherVisibility choice : values()) {
                if (choice.id.equalsIgnoreCase(value.trim())) {
                    return choice;
                }
            }
        }
        return KEEP;
    }
}
