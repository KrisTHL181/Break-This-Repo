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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sleeper {
    private Sleeper() {
    }

    /**
     * @param args how long to wait in seconds, and optionally the code to exit with
     * @throws Exception when the wait is cut short
     */
    public static void main(String[] args) throws Exception {
        Thread.sleep(Long.parseLong(args[0]) * 1000);
        System.exit(args.length > 1 ? Integer.parseInt(args[1]) : 0);
    }

    /**
     * @param args the arguments the child is given
     * @return the command that runs this class on the test's own classpath
     */
    public static List<String> command(String... args) {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Sleeper.class.getName());
        Collections.addAll(command, args);
        return command;
    }
}
