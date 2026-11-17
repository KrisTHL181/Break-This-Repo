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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McPropertiesTest {

    private static final String SAMPLE = """
            #Minecraft server properties
            #Thu Oct 29 12:00:00 UTC 2026
            enable-rcon=false
            motd=A Minecraft Server
            level-name=world

            # hand-written, the panel must not touch this
            custom-key=keep me
            weird line without separator
            """;

    @Test
    void roundTripsCommentsBlanksAndUnknownKeys() {
        assertEquals(SAMPLE, McProperties.parse(SAMPLE).render());
    }

    @Test
    void setRewritesTheLineWhereItWasAndKeepsEverythingElse() {
        McProperties properties = McProperties.parse(SAMPLE);
        properties.set("motd", "Hej");
        String rendered = properties.render();
        assertTrue(rendered.indexOf("motd=Hej") > rendered.indexOf("enable-rcon=false"));
        assertTrue(rendered.contains("custom-key=keep me"));
        assertTrue(rendered.contains("# hand-written, the panel must not touch this"));
        assertTrue(rendered.contains("weird line without separator"));
        assertEquals(SAMPLE.lines().count(), rendered.lines().count());
    }

    @Test
    void setAppendsAKeyThatIsNotThereYet() {
        McProperties properties = McProperties.parse("a=1\n");
        properties.set("b", "2");
        assertEquals("a=1\nb=2\n", properties.render());
    }

    @Test
    void removeDropsTheWholeLine() {
        McProperties properties = McProperties.parse("a=1\nb=2\nc=3\n");
        assertTrue(properties.remove("b"));
        assertEquals("a=1\nc=3\n", properties.render());
        assertNull(properties.get("b"));
    }

    @Test
    void keepsCrlfAndTheMissingFinalNewline() {
        String crlf = "# c\r\na=1\r\nb=2\r\n";
        assertEquals(crlf, McProperties.parse(crlf).render());
        assertEquals("a=1\nb=2", McProperties.parse("a=1\nb=2").render());
    }

    @Test
    void aColonSeparatedLineIsNotManaged() {
        String text = "level-name: world\n";
        McProperties properties = McProperties.parse(text);
        assertNull(properties.get("level-name"));
        assertEquals(text, properties.render());
    }

    @Test
    void asMapKeepsFileOrderAndTheLastDuplicateWins() {
        McProperties properties = McProperties.parse("a=1\nb=2\na=3\n");
        assertEquals(java.util.List.of("a", "b"), java.util.List.copyOf(properties.asMap().keySet()));
        assertEquals("3", properties.asMap().get("a"));
    }

    @Test
    void saveThenLoadKeepsTheManagedValueAndTheComments(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path file = dir.resolve("server.properties");
        Files.writeString(file, SAMPLE);
        McProperties properties = McProperties.load(file);
        properties.set("enable-rcon", "true");
        properties.save(file);

        McProperties reloaded = McProperties.load(file);
        assertEquals("true", reloaded.get("enable-rcon"));
        assertTrue(Files.readString(file).contains("# hand-written"));
    }

    /// `key=value` is a line: a value carrying a line break writes a second property the
    /// caller was not allowed to set (for example `enable-rcon=true`).
    @Test
    void aValueWithANewlineIsRefused() {
        McProperties properties = McProperties.parse(SAMPLE);
        assertThrows(IllegalArgumentException.class, () -> properties.set("motd", "a\nb"));
        assertThrows(IllegalArgumentException.class, () -> properties.set("motd", "a\rb"));
        assertThrows(IllegalArgumentException.class, () -> properties.set("motd", "a\0b"));
        assertTrue(properties.render().contains("motd=A Minecraft Server"),
                "a refused write must leave the old line alone");
    }
}
