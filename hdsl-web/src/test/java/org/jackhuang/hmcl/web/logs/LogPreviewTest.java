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
package org.jackhuang.hmcl.web.logs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogPreviewTest {

    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

    @TempDir
    Path dir;

    private Path write(String name, String content, String modified) throws Exception {
        Path file = dir.resolve(name);
        Files.createDirectories(dir);
        Files.writeString(file, content);
        Files.setLastModifiedTime(file, FileTime.from(Instant.parse(modified)));
        return file;
    }

    @Test
    void onlyTheLastThreeDaysAreListed() throws Exception {
        write("audit-2026-10-30.jsonl", "{}\n", "2026-10-30T01:00:00Z");
        write("audit-2026-10-27.jsonl", "{}\n", "2026-10-27T01:00:00Z");
        // 十天前的审计：名字里的日期说了算，mtime 再新也不列。
        write("audit-2026-10-20.jsonl", "{}\n", "2026-10-30T01:00:00Z");
        write("2026-10-29T10-00-00.log", "panel\n", "2026-10-29T10:00:00Z");
        // 面板日志按 mtime：名字里没有可信日期。
        write("2026-09-01T10-00-00.log", "old\n", "2026-10-01T10:00:00Z");
        write("2026-10-01T10-00-00.log.xz", "archive\n", "2026-10-29T09:00:00Z");
        // 不是日志：一个字都不该出现。
        write("notes.txt", "hello\n", "2026-10-30T01:00:00Z");

        List<LogPreview.Entry> entries = LogPreview.recent(dir, NOW, LogPreview.VIEW_DAYS);

        assertEquals(List.of(
                "audit-2026-10-30.jsonl",
                "2026-10-29T10-00-00.log",
                "2026-10-01T10-00-00.log.xz",
                "audit-2026-10-27.jsonl"), entries.stream().map(LogPreview.Entry::name).toList());
        assertEquals("audit", entries.get(0).kind());
        assertTrue(entries.get(2).compressed(), "归档只列不读");
    }

    @Test
    void tailReturnsTheEndOfTheFile() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= 500; i++) {
            text.append("line ").append(i).append('\n');
        }
        write("2026-10-30T01-00-00.log", text.toString(), "2026-10-30T01:00:00Z");

        List<String> lines = LogPreview.tail(dir, "2026-10-30T01-00-00.log", NOW, LogPreview.VIEW_DAYS, 5);

        assertEquals(List.of("line 496", "line 497", "line 498", "line 499", "line 500"), lines);
    }

    @Test
    void aFileBiggerThanTheTailBudgetStillStartsOnAWholeLine() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 1; i <= 40_000; i++) {
            text.append("line ").append(i).append(" padding padding padding\n");
        }
        write("2026-10-30T01-00-00.log", text.toString(), "2026-10-30T01:00:00Z");

        List<String> lines = LogPreview.tail(dir, "2026-10-30T01-00-00.log", NOW, LogPreview.VIEW_DAYS, 3);

        assertEquals(3, lines.size());
        assertTrue(lines.get(0).startsWith("line "), "首行不能是从半个字中间切开的残片：" + lines.get(0));
        assertEquals("line 40000 padding padding padding", lines.get(2));
    }

    @Test
    void whatTheWindowHidesIsRefused() throws Exception {
        write("2026-10-01T10-00-00.log", "old\n", "2026-10-01T10:00:00Z");
        write("2026-10-20T10-00-00.log.xz", "archive\n", "2026-10-29T10:00:00Z");
        write("audit-2026-10-10.jsonl", "{}\n", "2026-10-10T10:00:00Z");
        write("notes.txt", "hello\n", "2026-10-29T10:00:00Z");

        assertThrows(LogPreview.Refused.class,
                () -> LogPreview.tail(dir, "2026-10-01T10-00-00.log", NOW, LogPreview.VIEW_DAYS, 200));
        assertThrows(LogPreview.Refused.class,
                () -> LogPreview.tail(dir, "audit-2026-10-10.jsonl", NOW, LogPreview.VIEW_DAYS, 200));
        assertThrows(LogPreview.Refused.class,
                () -> LogPreview.tail(dir, "2026-10-20T10-00-00.log.xz", NOW, LogPreview.VIEW_DAYS, 200));
        assertThrows(LogPreview.Refused.class,
                () -> LogPreview.tail(dir, "notes.txt", NOW, LogPreview.VIEW_DAYS, 200));
        assertThrows(LogPreview.Refused.class,
                () -> LogPreview.tail(dir, "../etc/passwd", NOW, LogPreview.VIEW_DAYS, 200));
        assertThrows(LogPreview.Refused.class,
                () -> LogPreview.tail(dir, "2026-10-30T01-00-00.log", NOW, LogPreview.VIEW_DAYS, 200));
    }

    @Test
    void theLineCountIsBounded() throws Exception {
        write("2026-10-30T01-00-00.log", "a\nb\nc\n", "2026-10-30T01:00:00Z");

        // 0 与负数被夹到 1，超大值被夹到上限：调用方没法用 tail 把整份文件拿走。
        assertEquals(List.of("c"), LogPreview.tail(dir, "2026-10-30T01-00-00.log", NOW, 3, 0));
        assertEquals(List.of("a", "b", "c"),
                LogPreview.tail(dir, "2026-10-30T01-00-00.log", NOW, 3, LogPreview.MAX_TAIL_LINES * 10));
        assertEquals(LogPreview.VIEW_DAYS, 3);
    }
}
