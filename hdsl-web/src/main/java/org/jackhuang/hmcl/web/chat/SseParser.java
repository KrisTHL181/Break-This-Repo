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

import org.jetbrains.annotations.NotNullByDefault;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/// 一个够用的 SSE 解析器：`event:` / `data:` / `id:` / 注释 / 空行分帧 /
/// 多行 data / CRLF。行长度有上限，避免一个不换行的恶意流把堆吃光。
@NotNullByDefault
public final class SseParser {

    private static final int MAX_LINE_CHARS = 1 << 20;

    @FunctionalInterface
    public interface Handler {
        /// @return false to stop parsing (the caller may be cancelled)
        boolean onEvent(String event, String data) throws IOException;
    }

    private SseParser() {
    }

    public static void parse(InputStream input, Handler handler) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String event = null;
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.length() > MAX_LINE_CHARS) {
                throw new IOException("SSE line too long");
            }
            if (line.isEmpty()) {
                if (!data.isEmpty() || event != null) {
                    String payload = data.length() > 0 && data.charAt(data.length() - 1) == '\n'
                            ? data.substring(0, data.length() - 1) : data.toString();
                    if (!handler.onEvent(event == null ? "message" : event, payload)) {
                        return;
                    }
                }
                event = null;
                data.setLength(0);
                continue;
            }
            if (line.startsWith(":")) {
                continue;
            }
            int colon = line.indexOf(':');
            String field = colon < 0 ? line : line.substring(0, colon);
            String value = colon < 0 ? "" : line.substring(colon + 1);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            switch (field) {
                case "event" -> event = value;
                case "data" -> data.append(value).append('\n');
                case "id", "retry" -> {
                    // Not needed for a request-scoped stream.
                }
                default -> {
                    // Unknown fields are ignored, as the SSE spec says.
                }
            }
        }
        if (!data.isEmpty() || event != null) {
            String payload = data.length() > 0 && data.charAt(data.length() - 1) == '\n'
                    ? data.substring(0, data.length() - 1) : data.toString();
            handler.onEvent(event == null ? "message" : event, payload);
        }
    }
}