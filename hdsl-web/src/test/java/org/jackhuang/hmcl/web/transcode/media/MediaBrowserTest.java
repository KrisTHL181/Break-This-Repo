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
package org.jackhuang.hmcl.web.transcode.media;

import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.transcode.runtime.TranscodeRuntime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaBrowserTest {

    private static MediaBrowser browser(Path root) {
        ServerConfig config = new ServerConfig();
        config.dataDir = root.resolve("data");
        return new MediaBrowser(new TranscodeRuntime(config));
    }

    @Test
    void listsDirectoriesFirstAndTagsKinds(@TempDir Path dir) throws Exception {
        Path root = Files.createDirectory(dir.resolve("media"));
        Files.createDirectory(root.resolve("子目录"));
        Files.writeString(root.resolve("movie.mkv"), "x");
        Files.writeString(root.resolve("song.flac"), "x");
        Files.writeString(root.resolve("subs.srt"), "x");
        Files.writeString(root.resolve("cover.jpg"), "x");
        Files.writeString(root.resolve("notes.txt"), "x");

        var listing = browser(root).list(root, "", 0, 50);
        assertEquals(6, listing.total());
        assertEquals("子目录", listing.entries().get(0).name());
        assertTrue(listing.entries().get(0).directory());
        assertTrue(listing.entries().stream().anyMatch(e -> e.name().equals("movie.mkv") && e.kind().equals("video")));
        assertTrue(listing.entries().stream().anyMatch(e -> e.name().equals("song.flac") && e.kind().equals("audio")));
        assertTrue(listing.entries().stream().anyMatch(e -> e.name().equals("subs.srt") && e.kind().equals("subtitle")));
        assertTrue(listing.entries().stream().anyMatch(e -> e.name().equals("cover.jpg") && e.kind().equals("image")));
        assertTrue(listing.entries().stream().anyMatch(e -> e.name().equals("notes.txt") && e.kind().equals("other")));
    }

    @Test
    void paginates(@TempDir Path dir) throws Exception {
        Path root = Files.createDirectory(dir.resolve("media"));
        for (int i = 0; i < 5; i++) {
            Files.writeString(root.resolve("f" + i + ".mp4"), "x");
        }
        var first = browser(root).list(root, "", 0, 2);
        assertEquals(5, first.total());
        assertEquals(2, first.entries().size());
        var second = browser(root).list(root, "", 1, 2);
        assertEquals(2, second.entries().size());
        assertEquals("f2.mp4", second.entries().get(0).name());
    }

    @Test
    void scanCollectsMediaRecursivelyAndSkipsNoise(@TempDir Path dir) throws Exception {
        Path root = Files.createDirectory(dir.resolve("media"));
        Files.writeString(root.resolve("a.mkv"), "x");
        Files.writeString(root.resolve("cover.jpg"), "x");
        Files.writeString(root.resolve("notes.txt"), "x");
        Path sub = Files.createDirectory(root.resolve("sub"));
        Files.writeString(sub.resolve("b.mp4"), "x");
        Files.writeString(sub.resolve("c.srt"), "x");
        Path hidden = Files.createDirectory(root.resolve(".hidden"));
        Files.writeString(hidden.resolve("d.mkv"), "x");

        MediaBrowser browser = browser(root);
        var recursive = browser.scan(root, "", true, 100);
        assertEquals(2, recursive.entries().size());
        assertTrue(recursive.entries().stream().anyMatch(e -> e.path().equals("a.mkv")));
        assertTrue(recursive.entries().stream().anyMatch(e -> e.path().equals("sub/b.mp4")));
        assertTrue(recursive.entries().stream().noneMatch(e -> e.path().contains(".hidden")));
        assertTrue(recursive.entries().stream().noneMatch(e -> e.kind().equals("other")));

        var topOnly = browser.scan(root, "", false, 100);
        assertEquals(1, topOnly.entries().size());
        assertEquals("a.mkv", topOnly.entries().get(0).path());

        var truncated = browser.scan(root, "", true, 1);
        assertEquals(1, truncated.entries().size());
        assertTrue(truncated.truncated());
    }

    @Test
    void refusesToEscapeTheRoot(@TempDir Path dir) throws Exception {
        Path root = Files.createDirectory(dir.resolve("media"));
        Files.writeString(dir.resolve("outside.txt"), "x");
        MediaBrowser browser = browser(root);

        assertThrows(IOException.class, () -> browser.resolve(root, "../outside.txt"));
        assertThrows(IOException.class, () -> browser.resolve(root, "/etc/passwd"));
        assertThrows(IOException.class, () -> browser.list(root, "..", 0, 10));
    }

    @Test
    void refusesASymlinkThatEscapes(@TempDir Path dir) throws Exception {
        Path root = Files.createDirectory(dir.resolve("media"));
        Path outside = Files.createDirectory(dir.resolve("outside"));
        Files.writeString(outside.resolve("secret.mp4"), "x");
        Path link = root.resolve("link");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException e) {
            return; // Windows without symlink privilege; the textual check above still runs.
        }
        assertThrows(IOException.class, () -> browser(root).resolve(root, "link/secret.mp4"));
    }
}