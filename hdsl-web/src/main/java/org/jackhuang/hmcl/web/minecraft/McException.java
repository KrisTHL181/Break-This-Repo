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
package org.jackhuang.hmcl.web.minecraft;

import org.jetbrains.annotations.NotNullByDefault;

/// A Minecraft instance operation that could not be carried out.
///
/// The message is meant for the API answer: it is what the user sees when a
/// port is taken or a directory cannot be written.
@NotNullByDefault
public final class McException extends Exception {

    /// @param message what went wrong
    public McException(String message) {
        super(message);
    }

    /// @param message what went wrong
    /// @param cause   the underlying failure
    public McException(String message, Throwable cause) {
        super(message, cause);
    }
}