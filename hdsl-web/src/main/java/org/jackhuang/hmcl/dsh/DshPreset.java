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

/// One entry in the quick-install catalogue.
///
/// Upstream ships no plugin store, no categories and no icons: plug-in
/// discovery is plain npm, and the only metadata a bundle carries is its own
/// `package.json`. The catalogue is therefore HDSL's own curated data set,
/// the counterpart of the Forge/Fabric/NeoForge buttons in HMCL's installer
/// list.
///
/// @param id             a stable identifier
/// @param name           the display name shown in the wizard
/// @param spec           the package spec handed to `dsh plugin add`
/// @param description    a one-line explanation
/// @param recommended    whether the entry is preselected
/// @param requiresPnpm   whether installing needs `pnpm` on `PATH`
@NotNullByDefault
public record DshPreset(
        String id,
        String name,
        String spec,
        String description,
        boolean recommended,
        boolean requiresPnpm) {

    /// Creates a recommended preset that needs `pnpm`.
    ///
    /// @param id          the identifier
    /// @param name        the display name
    /// @param spec        the package spec
    /// @param description the one-line explanation
    /// @return the preset
    public static DshPreset recommended(String id, String name, String spec, String description) {
        return new DshPreset(id, name, spec, description, true, true);
    }

    /// Creates an optional preset that needs `pnpm`.
    ///
    /// @param id          the identifier
    /// @param name        the display name
    /// @param spec        the package spec
    /// @param description the one-line explanation
    /// @return the preset
    public static DshPreset optional(String id, String name, String spec, String description) {
        return new DshPreset(id, name, spec, description, false, true);
    }
}
