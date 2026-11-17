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
package org.jackhuang.hmcl.web.terminal.pty;

import org.jetbrains.annotations.NotNullByDefault;

/// A terminal size, in character cells.
///
/// The values travel to the child through `TIOCSWINSZ`; the kernel turns the
/// change into a `SIGWINCH` for the foreground process group, so nothing in
/// this package sends signals for resizing.
///
/// @param cols visible columns
/// @param rows visible rows
@NotNullByDefault
public record PtySize(int cols, int rows) {

    /// The size a terminal starts with when the browser has not spoken yet.
    public static final PtySize DEFAULT = new PtySize(80, 24);

    public PtySize {
        if (cols < 1 || rows < 1 || cols > 65535 || rows > 65535) {
            throw new IllegalArgumentException("terminal size out of range: " + cols + "x" + rows);
        }
    }
}
