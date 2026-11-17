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
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// `server.properties` as the panel edits it: **managed keys change, everything
/// else survives**.
///
/// Minecraft rewrites this file itself, and players hand-edit it; a parser that
/// round-trips through [java.util.Properties] loses the order, the comments and
/// every key the panel does not manage. So the file is kept line by line: a
/// comment, a blank line or an unknown entry is preserved verbatim, and only a
/// line this object actually rewrote is re-rendered.
///
/// Only the `key=value` shape is understood. That is what the server writes;
/// the `key: value` form Java's properties reader also accepts does not appear
/// in the file, so a line in that shape is treated as unknown and preserved.
@NotNullByDefault
public final class McProperties {

    /// One line of the file: a comment, a blank line, or an entry.
    private static final class Line {
        /// The original text, or `null` when this line was added or rewritten.
        private @Nullable String raw;
        /// The key of an entry, or `null` for everything that is not one.
        private final @Nullable String key;
        /// The value of an entry; `null` for everything that is not one.
        private @Nullable String value;
        private boolean removed;

        private Line(@Nullable String raw, @Nullable String key, @Nullable String value) {
            this.raw = raw;
            this.key = key;
            this.value = value;
        }

        private static Line unknown(String raw) {
            return new Line(raw, null, null);
        }

        private static Line entry(@Nullable String raw, String key, String value) {
            return new Line(raw, key, value);
        }

        private String render() {
            if (raw != null) {
                return raw;
            }
            return key + "=" + (value == null ? "" : value);
        }
    }

    private final List<Line> lines = new ArrayList<>();
    private String newline = "\n";
    private boolean endsWithNewline = true;

    private McProperties() {
    }

    /// An empty file (no comments, no entries).
    ///
    /// @return empty properties
    public static McProperties empty() {
        return new McProperties();
    }

    /// Parses the file's text.
    ///
    /// @param text the contents
    /// @return the parsed properties
    public static McProperties parse(String text) {
        McProperties properties = new McProperties();
        if (text.isEmpty()) {
            return properties;
        }
        String body = text;
        if (body.indexOf("\r\n") >= 0) {
            properties.newline = "\r\n";
        }
        properties.endsWithNewline = body.endsWith("\n") || body.endsWith("\r");
        String[] raw = body.split("\\R", -1);
        int count = raw.length;
        if (properties.endsWithNewline) {
            // split leaves an empty element after the final line break.
            count -= 1;
        }
        for (int i = 0; i < count; i++) {
            String line = raw[i];
            if (properties.newline.equals("\r\n") && line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            properties.lines.add(parseLine(line));
        }
        return properties;
    }

    private static Line parseLine(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return Line.unknown(line);
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
            // No separator, or nothing before it: not a managed shape. Keep it as-is.
            return Line.unknown(line);
        }
        String key = line.substring(0, eq).strip();
        String value = line.substring(eq + 1).strip();
        return Line.entry(line, key, value);
    }

    /// Reads a file.
    ///
    /// @param file the file to read
    /// @return the parsed properties; an absent file parses to empty
    /// @throws IOException when the file exists but cannot be read
    public static McProperties load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return new McProperties();
        }
        return parse(Files.readString(file, StandardCharsets.UTF_8));
    }

    /// Writes the file through a temporary file and a move, so a reader never
    /// opens a half-written properties file.
    ///
    /// @param file the destination
    /// @throws IOException when it cannot be written
    public void save(Path file) throws IOException {
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = Files.createTempFile(parent, "server-properties", ".tmp");
        try {
            Files.writeString(temp, render(), StandardCharsets.UTF_8);
            move(temp, file);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /// The value of a key.
    ///
    /// @param key the key
    /// @return the value, or `null` when the key is absent
    public @Nullable String get(String key) {
        for (Line line : lines) {
            if (!line.removed && key.equals(line.key)) {
                return line.value;
            }
        }
        return null;
    }

    /// Sets a key, keeping the line where it was; a new key is appended.
    ///
    /// @param key the key
    /// @param value the value
    public void set(String key, String value) {
        if (value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                || value.indexOf('\0') >= 0)) {
            // `key=value` is a line. A value carrying a line break is a second property the
            // caller was not allowed to write.
            throw new IllegalArgumentException("a property value must be a single line");
        }
        for (Line line : lines) {
            if (!line.removed && key.equals(line.key)) {
                line.value = value;
                // The original text holds the old value; render from key/value from now on.
                line.raw = null;
                return;
            }
        }
        lines.add(Line.entry(null, key, value));
    }

    /// Removes a key and its line.
    ///
    /// @param key the key
    /// @return whether a line was removed
    public boolean remove(String key) {
        boolean removed = false;
        for (Line line : lines) {
            if (!line.removed && key.equals(line.key)) {
                line.removed = true;
                removed = true;
            }
        }
        return removed;
    }

    /// Every key/value pair, in file order (last duplicate wins, like the game).
    ///
    /// @return the pairs
    public Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Line line : lines) {
            if (!line.removed && line.key != null) {
                map.put(line.key, line.value == null ? "" : line.value);
            }
        }
        return map;
    }

    /// The file's text as it would be written.
    ///
    /// @return the rendered contents
    public String render() {
        StringBuilder out = new StringBuilder(256);
        boolean first = true;
        for (Line line : lines) {
            if (line.removed) {
                continue;
            }
            if (!first) {
                out.append(newline);
            }
            out.append(line.render());
            first = false;
        }
        if (endsWithNewline && !first) {
            out.append(newline);
        }
        return out.toString();
    }
}