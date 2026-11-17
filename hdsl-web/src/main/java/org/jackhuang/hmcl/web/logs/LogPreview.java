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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// 界面上能翻的日志：**最近三天**，与保留窗口无关。
///
/// 两件事故意分成两个数字：
///
/// - [LogRetention] 回答“磁盘上留多久”——审计 180 天起，最长四年；
/// - 这里回答“界面里能看多久”——写死 [VIEW_DAYS] 天。
///
/// 把界面窗口调成和保留窗口一样长，等于让一次会话把一整年的审计记录摊在屏幕上；
/// 把保留窗口缩到三天，又会让“上季度谁删了那个世界”无从回答。
/// 所以三天以外的日志**留在磁盘上**，要看请走终端或 `curl`。
///
/// 读的是文件尾（[MAX_TAIL_BYTES]）而不是整份：一份几百兆的面板日志不该为了
/// “看最后 200 行”被整个读进内存。
@NotNullByDefault
public final class LogPreview {

    /// 界面可见窗口（天）。改这个数字就是改“前端能看多久”。
    public static final int VIEW_DAYS = 3;

    /// 一次最多回多少行；请求再大也截到这里。
    public static final int MAX_TAIL_LINES = 2000;

    /// 默认回多少行。
    public static final int DEFAULT_TAIL_LINES = 200;

    /// 从文件尾最多读多少字节。
    static final long MAX_TAIL_BYTES = 256 * 1024;

    private static final String AUDIT_PREFIX = "audit-";
    private static final String AUDIT_SUFFIX = ".jsonl";
    private static final String COMPRESSED_SUFFIX = ".xz";

    /// 一份日志在界面上的样子。
    ///
    /// @param name       文件名（也是它在这套接口里的 id）
    /// @param kind       `audit` 或 `panel`
    /// @param bytes      文件大小
    /// @param modified   最后修改时间
    /// @param compressed 是否 `.xz` 归档（归档只列不读）
    public record Entry(String name, String kind, long bytes, Instant modified, boolean compressed) {
    }

    /// 这份文件不能给界面看：太旧、名字不认识、是压缩归档，或者根本不存在。
    public static final class Refused extends Exception {
        public Refused(String message) {
            super(message);
        }
    }

    private LogPreview() {
    }

    /// 窗口内、可以列出来的日志，最新的在前。
    ///
    /// @param logsDir 日志目录（`<data>/logs`）
    /// @param now     当前时间（测试注入）
    /// @param days    可见窗口，单位天
    /// @return 条目；目录不存在时为空表
    public static List<Entry> recent(Path logsDir, Instant now, int days) {
        List<Entry> entries = new ArrayList<>();
        if (!Files.isDirectory(logsDir)) {
            return entries;
        }
        Instant cutoff = now.minus(Duration.ofDays(days));
        LocalDate cutoffDay = LocalDate.ofInstant(cutoff, ZoneId.systemDefault());
        try (DirectoryStream<Path> listing = Files.newDirectoryStream(logsDir)) {
            for (Path file : listing) {
                String name = file.getFileName().toString();
                String kind = kindOf(name);
                if (kind == null) {
                    continue;
                }
                boolean compressed = name.endsWith(COMPRESSED_SUFFIX);
                Instant modified = modifiedAt(file);
                if (!"audit".equals(kind)) {
                    // 面板日志按 mtime：名字里没有可信的日期（滚动规则在 Logger 里）。
                    if (modified.isBefore(cutoff)) {
                        continue;
                    }
                } else {
                    // 审计按文件名里的日期：整体拷贝数据目录不该把旧文件“洗新”。
                    LocalDate day = LogRetention.auditDay(file);
                    if (day == null || day.isBefore(cutoffDay)) {
                        continue;
                    }
                }
                entries.add(new Entry(name, kind, sizeOf(file), modified, compressed));
            }
        } catch (IOException e) {
            return entries;
        }
        entries.sort(Comparator.comparing(Entry::modified).reversed());
        return entries;
    }

    /// 窗口内某份日志的尾巴。
    ///
    /// @param logsDir 日志目录
    /// @param name    文件名，必须与目录里的一项完全同名
    /// @param now     当前时间（测试注入）
    /// @param days    可见窗口，单位天
    /// @param lines   要多少行，1–[MAX_TAIL_LINES]
    /// @return 文本行，末尾是文件最后一行
    /// @throws Refused 名字不合法、文件不在窗口内、是压缩归档或不存在
    public static List<String> tail(Path logsDir, String name, Instant now, int days, int lines)
            throws Refused {
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || !name.equals(Path.of(name).getFileName().toString())) {
            throw new Refused("a bare file name is required");
        }
        String kind = kindOf(name);
        if (kind == null) {
            throw new Refused("not a log file");
        }
        Path file = logsDir.resolve(name);
        if (!Files.isRegularFile(file)) {
            throw new Refused("no such log file");
        }
        if (name.endsWith(COMPRESSED_SUFFIX)) {
            throw new Refused("this is a compressed archive; read it with a terminal or curl");
        }
        Instant cutoff = now.minus(Duration.ofDays(days));
        boolean tooOld = "audit".equals(kind)
                ? isBeforeDay(LogRetention.auditDay(file), cutoff)
                : modifiedAt(file).isBefore(cutoff);
        if (tooOld) {
            throw new Refused("this log is older than the " + days + "-day window; "
                    + "read it with a terminal or curl");
        }
        int wanted = Math.max(1, Math.min(MAX_TAIL_LINES, lines));
        return readTail(file, wanted);
    }

    private static boolean isBeforeDay(@Nullable LocalDate day, Instant cutoff) {
        if (day == null) {
            return true;
        }
        return day.isBefore(LocalDate.ofInstant(cutoff, ZoneId.systemDefault()));
    }

    /// 从文件尾往上读，直到攒够 `lines` 行或撞到 [MAX_TAIL_BYTES]。
    private static List<String> readTail(Path file, int lines) throws Refused {
        try (RandomAccessFile handle = new RandomAccessFile(file.toFile(), "r")) {
            long length = handle.length();
            long from = Math.max(0, length - MAX_TAIL_BYTES);
            byte[] buffer = new byte[(int) (length - from)];
            handle.seek(from);
            handle.readFully(buffer);
            String text = new String(buffer, StandardCharsets.UTF_8);
            String[] all = text.split("\n", -1);
            int start = 0;
            if (from > 0) {
                // 第一行可能是从半个字中间切开的：丢掉它，别把乱码当成日志。
                start = 1;
            }
            int end = all.length;
            if (end > start && all[end - 1].isEmpty()) {
                end--;
            }
            start = Math.max(start, end - lines);
            List<String> result = new ArrayList<>(end - start);
            for (int i = start; i < end; i++) {
                result.add(all[i]);
            }
            return result;
        } catch (IOException e) {
            throw new Refused("could not read the log: " + e.getMessage());
        }
    }

    /// `audit` / `panel`，其余一律不认——这是路径校验的第一道门。
    private static @Nullable String kindOf(String name) {
        boolean audit = name.startsWith(AUDIT_PREFIX) && name.endsWith(AUDIT_SUFFIX);
        if (audit) {
            return "audit";
        }
        String plain = name.endsWith(COMPRESSED_SUFFIX)
                ? name.substring(0, name.length() - COMPRESSED_SUFFIX.length())
                : name;
        return plain.endsWith(".log") ? "panel" : null;
    }

    private static Instant modifiedAt(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }
}
