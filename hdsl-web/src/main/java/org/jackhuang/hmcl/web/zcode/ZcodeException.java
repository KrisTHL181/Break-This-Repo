/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
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
package org.jackhuang.hmcl.web.zcode;

import org.jetbrains.annotations.NotNullByDefault;

/// A failure in the experimental ZCode category.
///
/// Mirrors [org.jackhuang.hmcl.dsh.DshException]: a checked exception whose
/// message is written to the user as-is by the REST layer.
@NotNullByDefault
public class ZcodeException extends Exception {

    public ZcodeException(String message) {
        super(message);
    }

    public ZcodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
