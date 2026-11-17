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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

/// A stand-in for `server.jar`, used by the runtime tests and the panel's own
/// smoke test.
///
/// It behaves like a vanilla server in the ways the panel depends on: it prints
/// the readiness line after a moment, it stops when `stop` arrives on stdin,
/// and `crash` / `hang` reproduce the two failures the timeout has to survive.
/// It listens on no port — port behaviour belongs to the network layer, not to
/// the process supervision this stands in for.
public final class FakeMcServer {

    private FakeMcServer() {
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "ok";
        if (mode.equals("silent")) {
            // A child that dies before its first byte — the shape a permission or exec
            // failure takes, and the one the panel has to explain without any output.
            System.exit(1);
        }
        System.out.println("[Server thread/INFO]: Starting minecraft server version 1.21.4");
        System.out.flush();
        if (mode.equals("crash")) {
            System.out.println("[Server thread/ERROR]: Failed to start the minecraft server");
            System.out.flush();
            System.exit(3);
        }
        Thread.sleep(150);
        System.out.println("[Server thread/INFO]: Done (0.15s)! For help, type \"help\"");
        System.out.flush();
        if (mode.equals("hang")) {
            Thread.sleep(Long.MAX_VALUE);
        }
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = input.readLine()) != null) {
            if (line.strip().equals("stop")) {
                System.out.println("[Server thread/INFO]: Stopping the server");
                System.out.flush();
                return;
            }
            System.out.println("[Server thread/INFO]: Unknown command: " + line.strip());
            System.out.flush();
        }
    }
}