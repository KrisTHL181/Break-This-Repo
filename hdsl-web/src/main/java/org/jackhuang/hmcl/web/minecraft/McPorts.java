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

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.function.IntPredicate;

/// The two ports a Minecraft server owns: the game port and RCON.
///
/// They are allocated and validated **together**. A game port without its RCON
/// counterpart, or a pair where both numbers are the same, is a configuration
/// that only fails later — when the server is already starting and the player
/// is looking at a stack trace.
@NotNullByDefault
public final class McPorts {

    /// Below 1024 the process would need privileges the panel does not have.
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    private McPorts() {
    }

    /// A validated pair.
    ///
    /// @param game the game port (25565 by convention)
    /// @param rcon the RCON port; never equal to `game`
    public record Pair(int game, int rcon) {
        public Pair {
            if (!inRange(game) || !inRange(rcon)) {
                throw new IllegalArgumentException("a port must be between " + MIN_PORT + " and " + MAX_PORT);
            }
            if (game == rcon) {
                throw new IllegalArgumentException("the game port and the RCON port must differ");
            }
        }
    }

    /// Whether a number can serve as a game or RCON port.
    ///
    /// @param port the candidate
    /// @return whether it is in range
    public static boolean inRange(int port) {
        return port >= MIN_PORT && port <= MAX_PORT;
    }

    /// Validates a pair chosen by a human.
    ///
    /// @param game the game port
    /// @param rcon the RCON port
    /// @return the validated pair
    /// @throws IllegalArgumentException when either port is out of range or they are equal
    public static Pair validate(int game, int rcon) {
        return new Pair(game, rcon);
    }

    /// Whether the operating system will let a server bind this port now.
    ///
    /// This is deliberately a real bind, not a table lookup: on a bare
    /// deployment the port may belong to some unrelated service, or to a
    /// Minecraft server left behind by a panel that was killed. `SO_REUSEADDR`
    /// is enabled only where it is safe for a *probe*: it lets Linux ignore a
    /// lingering `TIME_WAIT`, while on Windows/macOS it would also let the
    /// probe succeed next to a real listener.
    ///
    /// @param port the port
    /// @return whether a listener can bind it
    public static boolean isFree(int port) {
        if (!inRange(port)) {
            return false;
        }
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX);
            socket.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /// Picks a free pair inside `[start, end]`.
    ///
    /// The game port is the first free number in range; RCON is the next free
    /// one after it (wrapping), so the pair stays close together and is easy to
    /// write into a firewall rule. The predicate is asked about the game port of
    /// every existing instance, so callers pass "is this number already taken?".
    ///
    /// @param taken whether a port is already in use
    /// @param start the first candidate (inclusive)
    /// @param end the last candidate (inclusive)
    /// @return a pair with two free ports
    /// @throws IllegalStateException when the range has no free pair
    public static Pair allocate(IntPredicate taken, int start, int end) {
        if (!inRange(start) || !inRange(end) || start > end) {
            throw new IllegalArgumentException("invalid port range " + start + "-" + end);
        }
        int game = -1;
        for (int port = start; port <= end; port++) {
            if (!taken.test(port)) {
                game = port;
                break;
            }
        }
        if (game < 0) {
            throw new IllegalStateException("no free port in " + start + "-" + end);
        }
        int span = end - start + 1;
        int rcon = -1;
        for (int i = 1; i < span; i++) {
            int port = start + Math.floorMod(game - start + i, span);
            if (port != game && !taken.test(port)) {
                rcon = port;
                break;
            }
        }
        if (rcon < 0) {
            throw new IllegalStateException("no free RCON port in " + start + "-" + end);
        }
        return new Pair(game, rcon);
    }
}