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
package org.jackhuang.hmcl.web.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;

/// The one JSON dialect every endpoint speaks: gson for (de)serialization and
/// `{"error": "..."}` for failures, always `application/json; charset=utf-8`.
@NotNullByDefault
public final class Json {
    public static final Gson GSON = new Gson();
    /// gson with `serializeNulls`: the default instance **drops** `JsonNull`
    /// members of a tree it writes (JsonWriter skips null members), which is
    /// right for most endpoints — but several contract fields are spelled as
    /// an explicit `null` (a masked key of an offline account, an instance
    /// with no account, a catalogue entry without a tarball), and those are
    /// written through this one.
    private static final Gson GSON_WITH_NULLS = new GsonBuilder().serializeNulls().create();
    public static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private Json() {
    }

    /// Writes `body` as JSON with status 200.
    public static void write(HttpServletResponse response, Object body) throws IOException {
        write(response, 200, body);
    }

    /// Writes `body` as JSON with the given status.
    public static void write(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(GSON.toJson(body));
    }

    /// Writes `body` as JSON with status 200, keeping explicit `null` members.
    public static void writePreservingNulls(HttpServletResponse response, Object body) throws IOException {
        writePreservingNulls(response, 200, body);
    }

    /// Writes `body` as JSON with the given status, keeping explicit `null` members.
    public static void writePreservingNulls(HttpServletResponse response, int status, Object body)
            throws IOException {
        response.setStatus(status);
        response.setContentType(CONTENT_TYPE);
        response.getWriter().write(GSON_WITH_NULLS.toJson(body));
    }

    /// Writes the uniform error shape `{"error": message}`.
    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, new ErrorBody(message));
    }

    private record ErrorBody(String error) {
    }
}
