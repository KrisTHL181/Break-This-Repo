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

import java.io.IOException;

/// A PTY call failed, or the platform cannot provide one.
///
/// The errno is kept when the failure came from a system call: the message a
/// person sees should name what failed, and the number is what a developer
/// needs when it does.
@NotNullByDefault
public final class PtyException extends IOException {

    /// The errno of the failed call, or 0 when the failure was not one.
    private final int errno;

    public PtyException(String message) {
        this(message, 0);
    }

    public PtyException(String message, int errno) {
        super(errno == 0 ? message : message + " (errno " + errno + ")");
        this.errno = errno;
    }

    /// The errno of the failed system call, or 0.
    public int errno() {
        return errno;
    }
}
