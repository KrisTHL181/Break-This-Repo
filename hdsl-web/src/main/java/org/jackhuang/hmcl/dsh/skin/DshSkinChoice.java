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
package org.jackhuang.hmcl.dsh.skin;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/// Which skin an account wears.
///
/// Ported from the original, which keeps exactly this on the account: a record of *how* the skin was
/// chosen rather than of the picture, so the choice survives the file being moved, deleted, or
/// re-imported. The alternative — keeping only the resulting image — cannot answer "which of the
/// launcher's own skins is this", and so cannot draw the chooser in the state it was left in.
///
/// A skin belongs to an **account**, not to the launcher. The original has one per account, offline
/// ones included, which is why two accounts can wear different faces; a single launcher-wide skin is
/// what made changing one change them all.
///
/// @param type         how the skin was chosen
/// @param textureModel which body it is drawn on
/// @param localSkinPath the file, for [#Type.LOCAL_FILE]
/// @param localCapePath the cape's file, for [#Type.LOCAL_FILE]
@NotNullByDefault
public record DshSkinChoice(
        Type type,
        TextureModel textureModel,
        @Nullable String localSkinPath,
        @Nullable String localCapePath) {

    /// How a skin was chosen.
    ///
    /// The original's list, less the three that are lookups against services serving Minecraft
    /// accounts — a nickname lookup on LittleSkin, a custom skin-loader API, and a Yggdrasil server.
    /// What is left is what can be answered without asking anybody.
    public enum Type {
        /// The body's own default skin.
        DEFAULT,
        /// The wide-armed model.
        STEVE,
        /// The slim-armed model.
        ALEX,
        /// A picture on this machine.
        LOCAL_FILE;

        /// Reads a type from what is stored, or `null` when the name means nothing here.
        ///
        /// @param stored the stored name
        /// @return the type, or `null`
        public static @Nullable Type fromStorage(@Nullable String stored) {
            if (stored == null) {
                return null;
            }
            for (Type type : values()) {
                if (type.name().toLowerCase(Locale.ROOT).equals(stored.toLowerCase(Locale.ROOT))) {
                    return type;
                }
            }
            return null;
        }
    }

    /// Which body a skin is drawn on.
    ///
    /// The names are the original's own — `default` and `slim` — rather than `wide` and `slim`,
    /// because they are not only stored names: they are the tail of the interface's i18n keys
    /// (`account.skin.model.default` reads "Classic", `account.skin.model.slim` reads "Slim"), and a
    /// different name here means a different label there.
    public enum TextureModel {
        /// Four pixels wide, the older of the two, and what the original calls the default.
        WIDE("default"),
        /// Three pixels wide.
        SLIM("slim");

        /// What the interface calls this body.
        public final String modelName;

        TextureModel(String modelName) {
            this.modelName = modelName;
        }

        /// Reads a body from what is stored.
        ///
        /// @param stored the stored name
        /// @return the body, defaulting to the wide one
        public static TextureModel fromStorage(@Nullable String stored) {
            return SLIM.modelName.equalsIgnoreCase(stored) ? SLIM : WIDE;
        }
    }

    /// The skin an account wears before anything is chosen.
    public static final DshSkinChoice DEFAULT =
            new DshSkinChoice(Type.DEFAULT, TextureModel.WIDE, null, null);

    /// Returns the body, never `null`.
    ///
    /// @return the body
    public TextureModel model() {
        return textureModel == null ? TextureModel.WIDE : textureModel;
    }

    /// Reports whether this is the skin nothing has been chosen for.
    ///
    /// @return whether the choice is the default one
    public boolean isDefault() {
        return type == Type.DEFAULT;
    }
}
