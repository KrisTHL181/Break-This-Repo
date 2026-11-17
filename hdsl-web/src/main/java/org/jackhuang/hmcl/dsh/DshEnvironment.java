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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/// The launcher-wide environment an instance may inherit from.
///
/// An instance can pin its own Node runtime and its own `DSH_HOME` policy, or
/// follow whatever the launcher is set to. The defaults live in the launcher's
/// settings, which are a layer above this package — so rather than reaching up
/// and creating a cycle, the values are pushed down here once at start-up.
///
/// The suppliers are read on every lookup rather than captured, so changing the
/// global setting takes effect without a restart.
@NotNullByDefault
public final class DshEnvironment {
    private DshEnvironment() {
    }

    /// Supplies the Node runtime an instance follows.
    private static volatile Supplier<String> nodeDefault = () -> DshNodeRuntime.SYSTEM;

    /// Supplies the DSH_HOME policy an instance follows.
    private static volatile Supplier<DshHomeMode> homeDefault = () -> DshHomeMode.ISOLATED;

    /// Installs the suppliers the launcher reads its defaults from.
    ///
    /// @param node the default Node runtime
    /// @param home the default DSH_HOME policy
    public static void install(Supplier<String> node, Supplier<DshHomeMode> home) {
        nodeDefault = node;
        homeDefault = home;
    }

    /// Returns the Node runtime an instance follows the launcher to.
    ///
    /// @return the default runtime selection
    public static String nodeRuntimeDefault() {
        String value = nodeDefault.get();
        return value == null || value.isBlank() ? DshNodeRuntime.SYSTEM : value;
    }

    /// Returns the DSH_HOME policy an instance follows the launcher to.
    ///
    /// @return the default policy, never [DshHomeMode#GLOBAL]
    public static DshHomeMode homeModeDefault() {
        DshHomeMode value = homeDefault.get();
        return value == null || value == DshHomeMode.GLOBAL ? DshHomeMode.ISOLATED : value;
    }


    /// Returns the variables an instance runs with.
    ///
    /// @param instance the instance
    /// @return the variables, never `null`
    public static Map<String, String> of(DshInstance instance) {
        Map<String, String> merged = new LinkedHashMap<>();
        try {
            Map<String, String> launcherWide =
                    org.jackhuang.hmcl.setting.SettingsManager.settings().globalEnvironment();
            merged.putAll(launcherWide);
        } catch (RuntimeException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                    "Could not read the launcher's environment", e);
        }
        merged.putAll(instance.environment());
        return merged;
    }

    /// Parses a set written as lines of `NAME=VALUE`.
    ///
    /// The launcher's own set is edited as text, because a list of one-line pairs is what people
    /// paste into a box; a line without an `=` is a name with an empty value, and blank lines and
    /// lines starting with `#` are comments.
    ///
    /// @param text the text
    /// @return the variables, in the order they were written
    public static Map<String, String> parse(String text) {
        Map<String, String> variables = new LinkedHashMap<>();
        if (text == null) {
            return variables;
        }
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.indexOf('=');
            if (equals < 0) {
                variables.put(trimmed, "");
            } else {
                variables.put(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
            }
        }
        return variables;
    }

    /// Writes a set as lines of `NAME=VALUE`.
    ///
    /// @param variables the variables
    /// @return the text, one variable per line
    public static String format(Map<String, String> variables) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }
        return text.toString();
    }
}
