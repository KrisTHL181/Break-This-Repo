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

/// How much of the harness's dependency tree is held to the versions it declares.
///
/// The harness states its dependencies as ranges — `@deepseek-ai/cordis: ^4.0.2` — and a caret means
/// "4.0.2 **or anything newer that claims compatibility**". That is how a package the launcher never
/// chose arrives underneath it, and it has now broken a launch twice:
///
/// - `^0.1.6-alpha.1` resolved to alpha.2's libraries, which had dropped an export the launcher
///   imported;
/// - `@deepseek-ai/cordis-plugin-loader ^1.0.3` resolved to 1.0.4, whose `create()` returns before the
///   plugin it creates has registered its service — so the harness's own "load HMR, then use it"
///   step read `undefined` and the process exited with `requires the Cordis HMR service`.
///
/// Both times the version that worked was **the floor of the declared range**, which is the version
/// the author built against. So holding a dependency to its floor is not a guess: it is reading what
/// the package itself says it was written for.
@NotNullByDefault
public enum DshDependencyPolicy {
    /// Hold nothing. Whatever the ranges resolve to is what is installed.
    ///
    /// The behaviour before this setting existed, and the one that lets an unverified update break a
    /// launch. Kept because tracking upstream is a legitimate thing to want; **not** the default,
    /// because shipping it as the default is shipping the bug.
    LATEST {
        @Override
        public boolean pins(String packageName) {
            return false;
        }
    },

    /// Hold the vendor's own packages to the versions the harness declares.
    ///
    /// `@deepseek-ai/*` rather than only `@deepseek-ai/cordis*`, and the wider scope is deliberate:
    /// the plugin framework and the harness's own libraries are published together in lockstep, the
    /// launcher's own notes record a breakage from exactly that family floating, and a rule that
    /// named one of them would have to be widened the next time the other one moved. Everything
    /// outside the vendor's scope — the plugins a pack brings — is left free.
    CORE_PINNED {
        @Override
        public boolean pins(String packageName) {
            return packageName.startsWith("@deepseek-ai/");
        }
    },

    /// Hold every declared dependency to the version the harness declares.
    ///
    /// The tree the author published against, item for item. The cost is real and is the reason this
    /// is not the default: an upstream patch to any dependency is never picked up.
    LOCKED {
        @Override
        public boolean pins(String packageName) {
            return true;
        }
    };

    /// Reports whether this policy holds a dependency to its declared floor.
    ///
    /// @param packageName the dependency's name
    /// @return whether to pin it
    public abstract boolean pins(String packageName);
}
