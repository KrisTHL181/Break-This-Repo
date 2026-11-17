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
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/// The curated quick-install catalogue.
///
/// The entries are ordinary npm packages that install into a profile through
/// `dsh plugin --profile <name> add <spec>`. DeepSeek Harness appends any
/// package that ships a `dsh.bundle.patch` to the profile's bundle list, so an
/// entry becomes active after the profile is restarted.
///
/// Three tiers exist upstream and the catalogue mirrors that, but only the
/// installable tier is offered here: shipped-but-disabled rows are already part
/// of the dependency closure and belong to a profile patch, not to a download.
@NotNullByDefault
public final class DshPresetCatalog {
    private DshPresetCatalog() {
    }

    /// The built-in catalogue.
    ///
    /// Ordered so the marketplace — the entry that makes every other plugin
    /// discoverable — comes first and is preselected.
    /// The presets offered when an instance is created.
    ///
    /// One card, deliberately. The marketplace is the way anything else is
    /// obtained — it browses and installs plugins itself — so offering a
    /// hand-picked dozen alongside it duplicates a job it already does, and
    /// freezes a list that goes stale the moment the ecosystem moves.
    ///
    /// The application boot library is not here because it is not a plugin: it
    /// is a dependency of DeepSeek Harness, and it is chosen on the create page
    /// as a version rather than installed as a package.
    private static final @Unmodifiable List<DshPreset> BUILTIN = List.of(
            DshPreset.recommended("dshmarket", "dsh-market",
                    "dshmarket",
                    "Plugin marketplace: browse, install and share DeepSeek Harness plugins."));

    /// Returns the built-in presets.
    ///
    /// @return the catalogue, in display order
    public static @Unmodifiable List<DshPreset> builtin() {
        return BUILTIN;
    }

    /// Returns the presets that should be selected by default.
    ///
    /// @return the recommended presets
    public static @Unmodifiable List<DshPreset> recommended() {
        return BUILTIN.stream().filter(DshPreset::recommended).toList();
    }
}
