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

/// Selects which `DSH_HOME` an instance uses.
///
/// DeepSeek Harness keeps profiles, sessions, settings and credentials under a
/// single home directory. Sharing that directory across different `dsh`
/// versions is unsafe: the module fallback links are rewritten by whichever
/// version boots last and only ever accumulate, `dsh.profile.bundles` is
/// rewritten in place, the workspace storage domain has no compatible-version
/// list, and session logs are refused outright by older builds once the format
/// moves on.
///
/// [#ISOLATED] is therefore the default and the only mode that is safe under
/// version churn.
@NotNullByDefault
public enum DshHomeMode {
    /// Use whatever policy the launcher is set to.
    ///
    /// The value a new instance is given, so changing the launcher's default
    /// affects the instances that never chose one and leaves the rest alone.
    GLOBAL,

    /// The instance owns a private home under its own instance directory.
    ///
    /// This is the default. Profiles, sessions, settings and credentials are
    /// fully private to the instance, so installing or upgrading another
    /// version cannot disturb it.
    ISOLATED,

    /// All instances of the same `dsh` version share one home.
    ///
    /// Suitable for deliberately sharing profiles between instances pinned to
    /// an identical version. The launcher warns about this mode because the
    /// pinned version must never change while the home is in use.
    VERSION_SHARED,

    /// The user names the home directory, typically to adopt an existing
    /// `~/.dsh` installation.
    ///
    /// HDSL never moves or deletes anything in a custom home.
    CUSTOM
}
