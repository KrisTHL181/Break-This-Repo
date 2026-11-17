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
package org.jackhuang.hmcl.web.chat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// SSE 的边界：多行 data、CRLF、注释、事件名、[DONE] 与半帧。
class SseParserTest {

    @Test
    void readsEventsAndMultiLineData() throws Exception {
        String input = """
                : keepalive
                event: message_start
                data: {"type":"message_start"}

                data: first
                data: second

                data: [DONE]

                """;
        List<String> events = new ArrayList<>();
        List<String> data = new ArrayList<>();
        SseParser.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                (event, payload) -> {
                    events.add(event);
                    data.add(payload);
                    return true;
                });
        assertEquals(List.of("message_start", "message", "message"), events);
        assertEquals("{\"type\":\"message_start\"}", data.get(0));
        assertEquals("first\nsecond", data.get(1));
        assertEquals("[DONE]", data.get(2));
    }

    @Test
    void handlesCrLfAndStopsWhenAsked() throws Exception {
        String input = "data: one\r\n\r\ndata: two\r\n\r\n";
        List<String> seen = new ArrayList<>();
        SseParser.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                (event, payload) -> {
                    seen.add(payload);
                    return seen.size() < 2;
                });
        assertEquals(List.of("one", "two"), seen);
    }

    @Test
    void flushesAnUnterminatedTail() throws Exception {
        List<String> seen = new ArrayList<>();
        SseParser.parse(new ByteArrayInputStream("data: tail".getBytes(StandardCharsets.UTF_8)),
                (event, payload) -> {
                    seen.add(payload);
                    return true;
                });
        assertEquals(List.of("tail"), seen);
        assertTrue(seen.size() == 1);
    }
}