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
package org.jackhuang.hmcl.util;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/// The jar this panel is running from, and whether it is still that file.
///
/// Replacing `hdsl-web-x.y.z.jar` while the panel runs is ordinary on Windows —
/// the copy succeeds, the running JVM keeps its handle, and nothing complains
/// until the next resource is read: the ZIP index it cached points into bytes
/// that are no longer there, and every *not yet loaded* entry fails with
/// `ZipException: invalid LOC header (bad signature)`. The visible symptom is a
/// resource that clearly exists in the file ("读取自带插件 … 失败", a page that
/// cannot load its assets) while the jar, unpacked, is perfect.
///
/// Nothing here can repair that handle. What it can do is name the cause: the
/// resource read fails, and the answer says the file changed since start and a
/// restart is all that is needed. Without it the message blames the jar, which
/// sends whoever hit it to rebuild something that was never broken.
@NotNullByDefault
public final class PanelJar {

    /// The file the panel started from: `null` when it was not a jar at all
    /// (tests, an exploded `build/classes` run), in which case there is nothing
    /// to watch.
    private static volatile Snapshot started;

    private PanelJar() {
    }

    /// Records the jar the panel is running from. Called once, at startup.
    ///
    /// @param anchor a class loaded from that jar
    public static void remember(Class<?> anchor) {
        remember(location(anchor));
    }

    /// Records `jar` as the file this process started with.
    ///
    /// Package-private for the tests: the alternative is starting a JVM from a
    /// throwaway jar just to replace it underneath.
    ///
    /// @param jar the file, or `null` when the panel does not run from one
    static void remember(@Nullable Path jar) {
        started = jar == null ? null : Snapshot.of(jar);
    }

    /// The jar `anchor` was loaded from, or `null` when it was not loaded from a
    /// file at all.
    ///
    /// @param anchor a class loaded from the jar
    /// @return the jar's path
    public static @Nullable Path location(Class<?> anchor) {
        try {
            URL source = anchor.getProtectionDomain().getCodeSource().getLocation();
            if (source == null) {
                return null;
            }
            Path path = Path.of(source.toURI());
            return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar") ? path : null;
        } catch (RuntimeException | URISyntaxException e) {
            // A class loaded from somewhere exotic is one case this cannot answer,
            // not a reason to fail the startup that is asking.
            return null;
        }
    }

    /// The sentence to append when a resource read failed because the jar was
    /// replaced under the running process.
    ///
    /// @return the explanation, or empty when the jar is the one that was started
    public static Optional<String> replacedHint() {
        Snapshot before = started;
        if (before == null) {
            return Optional.empty();
        }
        if (before.equals(Snapshot.of(before.path()))) {
            return Optional.empty();
        }
        return Optional.of("这个面板的 jar 在启动之后被替换过（" + before.path()
                + "）：正在运行的进程还握着旧文件的索引，读不到新内容。重启面板即可。");
    }

    /// What a file looked like. A file that has since been deleted or become
    /// unreadable is a different snapshot from the one taken at start, which is
    /// the answer wanted as well.
    private record Snapshot(Path path, long size, long modified) {

        static Snapshot of(Path path) {
            try {
                return new Snapshot(path, Files.size(path), Files.getLastModifiedTime(path).toMillis());
            } catch (IOException | RuntimeException e) {
                return new Snapshot(path, -1, -1);
            }
        }
    }
}
