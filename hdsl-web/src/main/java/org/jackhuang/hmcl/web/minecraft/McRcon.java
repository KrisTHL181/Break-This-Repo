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

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// The small part of RCON the panel uses: `list` for the player roster, and the
/// four moderation commands.
///
/// RCON is plaintext, so the panel only ever connects to `127.0.0.1`. The port
/// and password come from the instance's `server.properties`; both are written
/// by the panel at creation (RCON is off by default, §18 of the design doc), so
/// "RCON is off" is a clear 409 rather than a connection refused.
@NotNullByDefault
public final class McRcon {

    /// What `list` answered.
    ///
    /// @param online how many players are on
    /// @param max    the configured maximum
    /// @param names  the names, in server order
    public record Players(int online, int max, List<String> names) {
    }

    private static final int TYPE_AUTH = 3;
    private static final int TYPE_COMMAND = 2;
    private static final int TIMEOUT_MILLIS = 5000;
    private static final Pattern LIST = Pattern.compile(
            "There are (\\d+) of a max(?: of)? (\\d+) players online:?\\s*(.*)", Pattern.CASE_INSENSITIVE);

    private McRcon() {
    }

    /// Reads the roster.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @return the players
    /// @throws McException when RCON is off/unreachable or the answer is unexpected
    public static Players players(McInstance instance, McInstanceManager manager) throws McException {
        String answer = command(instance, manager, "list");
        Matcher matcher = LIST.matcher(answer.strip());
        if (!matcher.find()) {
            throw new McException("the server answered `list` in an unexpected way: " + answer.strip());
        }
        List<String> names = new ArrayList<>();
        String tail = matcher.group(3).strip();
        if (!tail.isEmpty()) {
            for (String name : tail.split(",")) {
                String trimmed = name.strip();
                if (!trimmed.isEmpty()) {
                    names.add(trimmed);
                }
            }
        }
        return new Players(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), names);
    }

    /// Runs one command and returns the server's answer.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @param command  the command, without a leading slash
    /// @return the answer
    /// @throws McException when RCON is off, unreachable, or rejects the password
    public static String command(McInstance instance, McInstanceManager manager, String command)
            throws McException {
        McProperties properties;
        try {
            properties = manager.properties(instance);
        } catch (McException e) {
            throw new McException("cannot read server.properties: " + e.getMessage(), e);
        }
        if (!"true".equalsIgnoreCase(properties.get("enable-rcon"))) {
            throw new McException("RCON is disabled in server.properties (enable-rcon=false)");
        }
        String password = properties.get("rcon.password");
        if (password == null || password.isBlank()) {
            throw new McException("RCON has no password in server.properties");
        }
        int port;
        try {
            port = Integer.parseInt(properties.get("rcon.port"));
        } catch (RuntimeException e) {
            throw new McException("server.properties has no valid rcon.port");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), TIMEOUT_MILLIS);
            socket.setSoTimeout(TIMEOUT_MILLIS);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            write(out, 1, TYPE_AUTH, password);
            Packet auth = read(in);
            if (auth.id() == -1) {
                throw new McException("RCON rejected the password");
            }
            write(out, 2, TYPE_COMMAND, command);
            // Some servers emit an extra empty packet around the auth; take the
            // first answer that belongs to the command.
            for (int attempt = 0; attempt < 4; attempt++) {
                Packet answer = read(in);
                if (answer.id() == 2) {
                    return answer.body();
                }
            }
            throw new McException("RCON did not answer the command");
        } catch (IOException e) {
            throw new McException("RCON connection failed: " + e.getMessage(), e);
        }
    }

    /// One decoded packet.
    private record Packet(int id, int type, String body) {
    }

    private static void write(OutputStream out, int id, int type, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + payload.length + 2;
        ByteBuffer packet = ByteBuffer.allocate(4 + length).order(ByteOrder.LITTLE_ENDIAN);
        packet.putInt(length).putInt(id).putInt(type).put(payload).put((byte) 0).put((byte) 0);
        out.write(packet.array());
        out.flush();
    }

    /// Reads one packet.
    private static Packet read(InputStream in) throws IOException {
        byte[] size = in.readNBytes(4);
        if (size.length < 4) {
            throw new IOException("RCON closed before answering");
        }
        int length = ByteBuffer.wrap(size).order(ByteOrder.LITTLE_ENDIAN).getInt();
        if (length < 10 || length > 4 * 1024 * 1024) {
            throw new IOException("RCON answered an invalid packet length " + length);
        }
        byte[] rest = in.readNBytes(length);
        if (rest.length < length) {
            throw new IOException("RCON packet was truncated");
        }
        ByteBuffer body = ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
        int id = body.getInt();
        int type = body.getInt();
        int textLength = length - 10;
        byte[] text = new byte[textLength];
        body.get(text);
        return new Packet(id, type, new String(text, StandardCharsets.UTF_8));
    }
}