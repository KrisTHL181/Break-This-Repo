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
package org.jackhuang.hmcl.util.platform;

import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;
import java.util.Map;

/// The home directory the panel's **children** will see.
///
/// `System.getProperty("user.home")` comes from the account database, not from
/// the environment: it answers "what does `/etc/passwd` say about the uid I run
/// as". That was fine while the panel and its agents were the same uid, and it
/// stopped being fine when the container started running the panel as root —
/// `user.home` becomes `/root`, so the default workspace became `/root/workspace`
/// (0750, and invisible to the agent at uid 1000, which could not even `cd`
/// into it: the terminal died 44 ms after it opened).
///
/// `HOME` is the answer to "where does the user of this process keep things",
/// which is what every child inherits and what systemd and the image both set.
/// On Windows the variable is absent and `user.home` is right, so the fallback
/// costs nothing.
@NotNullByDefault
public final class UserHome {

    private UserHome() {
    }

    /// The home directory, resolved the way the children resolve it.
    ///
    /// @param environment the process environment (injected so a test can drive both branches)
    /// @return an absolute, normalized path
    public static Path directory(Map<String, String> environment) {
        String home = environment.get("HOME");
        if (home != null && !home.isBlank()) {
            return Path.of(home.trim()).toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home", ".")).toAbsolutePath().normalize();
    }

    /// The same answer for this process, from its own environment.
    ///
    /// @return an absolute, normalized path
    public static Path directory() {
        return directory(System.getenv());
    }
}
