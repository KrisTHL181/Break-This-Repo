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

import java.io.Serial;

/// Signals that a DeepSeek Harness operation could not be completed.
///
/// Used for conditions the user can act on — a missing runtime, an unknown
/// version, a failed install — rather than for programming errors.
public class DshException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    /// Creates an exception with a message.
    ///
    /// @param message the human-readable cause
    public DshException(String message) {
        super(message);
    }

    /// Creates an exception with a message and a cause.
    ///
    /// @param message the human-readable cause
    /// @param cause   the underlying failure
    public DshException(String message, Throwable cause) {
        super(message, cause);
    }
}
