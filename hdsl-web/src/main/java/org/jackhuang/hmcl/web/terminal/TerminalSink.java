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
package org.jackhuang.hmcl.web.terminal;

import org.jetbrains.annotations.NotNullByDefault;

/// One attached viewer of a [TerminalSession]: the WebSocket in production,
/// a collector in tests.
///
/// The session calls [data] from its pump thread and [exited] exactly once,
/// after the last [data]. Implementations must not block the pump: a slow
/// viewer is disconnected, not allowed to stall the agent.
@NotNullByDefault
public interface TerminalSink {

    /// A chunk of the child's output.
    void data(byte[] bytes, int offset, int length);

    /// The child is gone; this is the last callback the sink receives.
    void exited(int code);
}
