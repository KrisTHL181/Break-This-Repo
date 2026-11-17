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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McRconTest {

    /// A one-connection fake RCON: auth always accepted, `list` answered with
    /// the sample vanilla text, anything else echoed.
    private static ServerSocket fakeRcon(AtomicReference<String> lastCommand) throws IOException {
        ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress());
        Thread thread = new Thread(() -> {
            try (Socket socket = server.accept()) {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                readPacket(in); // auth
                writePacket(out, 1, 2, "ok");
                String command = readPacket(in);
                lastCommand.set(command);
                writePacket(out, 2, 2,
                        command.equals("list")
                                ? "There are 2 of a max of 20 players online: alice, bob"
                                : "Done");
            } catch (IOException ignored) {
                // The client may close early; the test asserts on what it got.
            }
        });
        thread.setDaemon(true);
        thread.start();
        return server;
    }

    private static String readPacket(InputStream in) throws IOException {
        byte[] size = in.readNBytes(4);
        int length = ByteBuffer.wrap(size).order(ByteOrder.LITTLE_ENDIAN).getInt();
        byte[] rest = in.readNBytes(length);
        ByteBuffer packet = ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN);
        packet.getInt(); // id
        packet.getInt(); // type
        byte[] body = new byte[length - 10];
        packet.get(body);
        return new String(body, StandardCharsets.UTF_8);
    }

    private static void writePacket(OutputStream out, int id, int type, String body) throws IOException {
        byte[] text = body.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + text.length + 2;
        ByteBuffer packet = ByteBuffer.allocate(4 + length).order(ByteOrder.LITTLE_ENDIAN);
        packet.putInt(length).putInt(id).putInt(type).put(text).put((byte) 0).put((byte) 0);
        out.write(packet.array());
        out.flush();
    }

    @Test
    void playersAreParsedFromTheListAnswer(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = manager.create("survival", "1.21.4", 25565, 25575, 1024, "world", null);
        AtomicReference<String> lastCommand = new AtomicReference<>();
        ServerSocket server = fakeRcon(lastCommand);
        try {
            writeRconProperties(manager, instance, server.getLocalPort(), "secret");
            McRcon.Players players = McRcon.players(instance, manager);
            assertEquals(2, players.online());
            assertEquals(20, players.max());
            assertEquals(java.util.List.of("alice", "bob"), players.names());
            assertEquals("list", lastCommand.get());
        } finally {
            server.close();
        }
    }

    @Test
    void rconOffIsAClearRefusal(@TempDir Path dir) throws Exception {
        McInstanceManager manager = new McInstanceManager(dir.resolve("instances"));
        McInstance instance = manager.create("survival", "1.21.4", 25565, 25575, 1024, "world", null);
        McException refused = assertThrows(McException.class, () -> McRcon.players(instance, manager));
        assertTrue(refused.getMessage().contains("RCON is disabled"), refused.getMessage());
    }

    private static void writeRconProperties(McInstanceManager manager, McInstance instance,
                                            int port, String password) throws Exception {
        Path file = manager.propertiesFile(instance);
        Files.writeString(file, """
                enable-rcon=true
                rcon.port=%d
                rcon.password=%s
                """.formatted(port, password));
    }
}