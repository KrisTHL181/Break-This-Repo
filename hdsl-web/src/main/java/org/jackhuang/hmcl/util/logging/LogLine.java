/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2024 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.util.logging;

import org.jackhuang.hmcl.util.Log4jLevel;

/// One captured line of child-process output together with its inferred log level.
///
/// Lives in `util.logging` in HDSL-web: the desktop copy sat in the `ui`
/// package, and this project has no `ui` package.
public final class LogLine {
    /// How many lines the log view keeps.
    ///
    /// A constant rather than a setting: the log window's own line-count box is
    /// where the count is chosen, which is where the original keeps it, so a
    /// second control for the same number in the launcher's settings was a
    /// setting with nothing of its own to decide.
    public static final int DEFAULT_LOG_LINES = 2000;

    /// Returns how many lines the log view keeps.
    ///
    /// @return the line count
    public static int getLogLines() {
        return DEFAULT_LOG_LINES;
    }

    private final String log;
    private Log4jLevel level;

    public LogLine(String log) {
        this.log = log;
    }

    public LogLine(String log, Log4jLevel level) {
        this.log = log;
        this.level = level;
    }

    public String getLog() {
        return log;
    }

    public Log4jLevel getLevel() {
        Log4jLevel level = this.level;
        if (level == null) {
            level = Log4jLevel.guessLevel(log);
            if (level == null)
                level = Log4jLevel.INFO;
            this.level = level;
        }
        return level;
    }

    @Override
    public String toString() {
        return log;
    }
}
