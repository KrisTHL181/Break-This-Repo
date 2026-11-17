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

import java.util.Locale;

/// The icon shown for a launcher instance.
///
/// The mechanism and the set are HMCL's: an instance carries an icon selection,
/// the list and the sidebar show it, and the chooser offers the same images the
/// original does. The mod-loader artwork is kept for that parity — a launcher
/// that offers fewer choices than the one it is modelled on reads as a
/// limitation rather than a decision.
///
/// The DeepSeek Harness marks lead the set. They were supplied as SVG and are
/// kept as PNG, which is what every other icon here is and what the image
/// container reads: HMCL's own icons are inline path data in an enum, and
/// introducing a second mechanism for three images would not pay for itself.
///
/// Each icon is a 32-pixel image with a 64-pixel `@2x` beside it, matching the
/// set it joins. Rendering the wrong one is not obvious in a still: the
/// container scales what it is given, so a mis-sized asset looks merely soft.
@NotNullByDefault
public enum DshInstanceIcon {
    /// The DeepSeek Harness mark, in colour.
    DSH_APPLICATION("dsh_application"),
    /// The DeepSeek Harness mark on a light background.
    DSH_BLACK("dsh_black"),
    /// The DeepSeek Harness mark on a dark background.
    DSH_WHITE("dsh_white"),
    /// The default icon.
    DEFAULT("grass"),
    /// Grass block.
    GRASS("grass"),
    /// Chest.
    CHEST("chest"),
    /// Chicken.
    CHICKEN("chicken"),
    /// Command block.
    COMMAND("command"),
    /// The April Fools artwork.
    APRIL_FOOLS("april_fools"),
    /// The OptiFine logo.
    OPTIFINE("optifine"),
    /// Crafting table.
    CRAFT_TABLE("craft_table"),
    /// The Fabric logo.
    FABRIC("fabric"),
    /// The Legacy Fabric logo.
    LEGACY_FABRIC("legacyfabric"),
    /// The Forge logo.
    FORGE("forge"),
    /// The Cleanroom logo.
    CLEANROOM("cleanroom"),
    /// The NeoForge logo.
    NEO_FORGE("neoforge"),
    /// Furnace.
    FURNACE("furnace"),
    /// The Quilt logo.
    QUILT("quilt");

    private final String asset;

    DshInstanceIcon(String asset) {
        this.asset = asset;
    }

    /// Returns the identifier stored with an instance.
    ///
    /// @return the lower-case identifier
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /// Returns the bundled image this icon uses.
    ///
    /// Not the same as [#id]: `DEFAULT` and `GRASS` are separate choices that
    /// share one image, because a stored choice records what the user picked
    /// rather than which file it came from.
    ///
    /// @return the asset name without its extension
    public String assetName() {
        return asset;
    }

    /// Resolves an icon from its stored identifier.
    ///
    /// @param value the stored identifier, possibly `null` or unknown
    /// @return the matching icon, or [DshInstanceIcon#DEFAULT]
    public static DshInstanceIcon of(@Nullable String value) {
        if (value != null) {
            for (DshInstanceIcon icon : values()) {
                if (icon.id().equalsIgnoreCase(value.trim())) {
                    return icon;
                }
            }
        }
        return DEFAULT;
    }

    /// Loads the icon's classpath resource path.
    ///
    /// The HDSL-web copy does not decode pixels: the browser fetches the asset
    /// by path, so the answer is where the image lives in the jar rather than
    /// a decoded image.
    ///
    /// @return the resource path, `/assets/img/<asset>.png`
    public String resourcePath() {
        return "/assets/img/" + asset + ".png";
    }

    /// Resolves the resource path at the resolution a display asks for.
    ///
    /// Uses the `@2x` asset when one is bundled and the screen is dense enough
    /// to benefit, which is what the rest of the set does.
    ///
    /// @param scale the display scale, `1.0` for an ordinary screen
    /// @return the resource path that exists for the scale
    public String resourcePath(double scale) {
        String suffix = scale > 1.5 ? "@2x" : "";
        String candidate = "/assets/img/" + asset + suffix + ".png";
        if (!suffix.isEmpty()
                && DshInstanceIcon.class.getResource(candidate) == null) {
            candidate = resourcePath();
        }
        return candidate;
    }

    /// Returns the URL of the bundled image, for code that still wants to
    /// stream the bytes.
    ///
    /// @return the resource URL, or `null` when the asset is missing
    public @Nullable java.net.URL resourceUrl() {
        return DshInstanceIcon.class.getResource(resourcePath());
    }
}
