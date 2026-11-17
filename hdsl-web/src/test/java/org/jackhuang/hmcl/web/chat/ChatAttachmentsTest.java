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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// 文本附件预览时猜的编码：粘贴文本是 UTF-8，中文 Windows 的旧文件常是 GB18030。
class ChatAttachmentsTest {

    @Test
    void sniffedCharsetKeepsUtf8TextReadable(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("pasted.txt");
        Files.writeString(file, "粘贴进来的长文本：中文、emoji 🙂 和换行\n第二行", StandardCharsets.UTF_8);
        assertEquals(StandardCharsets.UTF_8, ChatAttachments.sniffTextCharset(file));
    }

    @Test
    void sniffedCharsetFallsBackToGb18030(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("legacy.txt");
        Files.writeString(file, "旧版记事本保存的中文文本", Charset.forName("GB18030"));
        assertEquals(Charset.forName("GB18030"), ChatAttachments.sniffTextCharset(file));
    }

    @Test
    void asciiIsTreatedAsUtf8(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ascii.log");
        Files.writeString(file, "GET /api/chat 200\n", StandardCharsets.US_ASCII);
        assertEquals(StandardCharsets.UTF_8, ChatAttachments.sniffTextCharset(file));
    }
}