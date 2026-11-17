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

import java.io.IOException;

/// The instance already has a running terminal.
///
/// One terminal per instance is a deliberate limit: two TUIs on the same home
/// would share the agent's session store and could corrupt it, and the second
/// window would show a different screen for the same agent.
@NotNullByDefault
public final class TerminalBusyException extends IOException {

    private final String instanceId;

    public TerminalBusyException(String instanceId) {
        super("instance " + instanceId + " already has a running terminal");
        this.instanceId = instanceId;
    }

    public String instanceId() {
        return instanceId;
    }
}
