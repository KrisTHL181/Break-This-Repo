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
package org.jackhuang.hmcl.setting;

import org.jackhuang.hmcl.dsh.DshEnvironment;
import org.jetbrains.annotations.NotNullByDefault;

import static org.jackhuang.hmcl.setting.SettingsManager.settings;

/// Connects the launcher's environment settings to the layer that reads them.
///
/// An instance can follow the launcher for its Node runtime and its `DSH_HOME`
/// policy. Those defaults are settings, and the instance layer sits below them,
/// so the values are pushed down once rather than pulled up — pulling would make
/// the two packages depend on each other.
///
/// HMCL keeps the same split, with an instance's settings inheriting from the
/// global ones unless it states its own.
@NotNullByDefault
public final class EnvironmentDefaults {
    private EnvironmentDefaults() {
    }

    /// Installs the launcher's defaults into the instance layer.
    ///
    /// Called once at start-up. The suppliers read the settings on every lookup,
    /// so a change takes effect without a restart.
    public static void install() {
        DshEnvironment.install(
                () -> settings().defaultNodeRuntime(),
                () -> settings().defaultHomeMode());
    }
}
