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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Resolves the image an instance shows.
///
/// A custom file wins over the built-in icon, because choosing one is the more
/// specific act and HMCL behaves the same way. A file that has since been moved
/// or deleted falls back rather than leaving the row blank.
///
/// The HDSL-web copy answers with paths rather than decoded images: the browser
/// fetches the asset, so the answer is where the image lives — an absolute file
/// path for a custom icon, a classpath resource path for a built-in one.
@NotNullByDefault
public final class DshInstanceIcons {
    private DshInstanceIcons() {
    }

    /// Returns the path of the image an instance shows.
    ///
    /// @param instance the instance
    /// @return the file path or the bundled resource path (`/assets/img/…`), or
    /// `null` when neither source is usable
    public static @Nullable String resolve(DshInstance instance) {
        Path file = instance.iconFileOrDefault();
        if (file != null) {
            if (Files.isRegularFile(file)) {
                return file.toAbsolutePath().normalize().toString();
            }
            LOG.info("The instance icon " + file + " is gone; falling back to the built-in icon");
        }
        return instance.iconOrDefault().resourcePath();
    }
}
