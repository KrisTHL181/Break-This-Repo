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
package org.jackhuang.hmcl.util.platform;

import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.Comparator;

/// How much of the kernel's inotify watch budget is left to spend.
///
/// DeepSeek Harness watches its own profile directory (chokidar over
/// `DSH_HOME/profiles/<profile>`) and **dies** when the kernel refuses a watch:
/// `fs.watch` surfaces the refusal as an uncatchable ENOSPC in the middle of
/// startup, so the harness either exits, or never reaches the point of printing
/// its readiness line. From the panel the two look like an instance that
/// started and cannot be opened, and one that never becomes ready — neither of
/// which names the real cause anywhere the user can see it.
///
/// The budget is the kernel's `fs.inotify.max_user_watches`, and it is counted
/// **per user (uid) for the whole machine** — containers do not get their own.
/// The panel runs as `hdsl` (uid 1000), and so does everything else the host
/// runs as uid 1000, this container or not: one recursive watcher elsewhere (an
/// editor server over a large tree, a file-sync client) can hold the entire
/// budget and leave nothing for the instances here.
///
/// The amount in use cannot be read — only the limit is, and refusals are per
/// call — so the headroom is measured by asking: [PROBE] registrations are
/// attempted and the number the kernel granted is the answer. A registered
/// directory costs exactly one watch, the same unit chokidar spends.
@NotNullByDefault
public final class WatcherBudget {

    /// The kernel knob that bounds how many watches one user may hold.
    public static final String LIMIT_PATH = "/proc/sys/fs/inotify/max_user_watches";

    /// How many registrations one probe attempts.
    ///
    /// Small on purpose: every registration is a directory that has to be
    /// created, and the question worth answering is "is there room" rather than
    /// "exactly how much room".
    public static final int PROBE = 64;

    /// The headroom below which an instance can no longer be relied on to
    /// register its own watchers.
    ///
    /// Above what an instance actually spends (a fresh profile directory costs
    /// a handful), because a budget this close to gone leaves nothing for the
    /// walk itself or for a second instance, which is where the refusals
    /// happen.
    public static final int MIN_FREE = 32;

    private WatcherBudget() {
    }

    /// The configured watch limit, or `-1` when it cannot be read.
    ///
    /// @return the limit, or `-1`
    public static int limit() {
        return limit(Path.of(LIMIT_PATH));
    }

    /// Reads a limit out of the kernel's knob file, or reports that it is not
    /// there.
    ///
    /// Read a line at a time rather than with `Files.readString`: a `/proc` file
    /// reports a size of **zero**, and the size-based read path stops after one
    /// byte of it. Measured in the panel's own container, where the knob holds
    /// `65536`: `Files.readString` answers `"6"`, `readLine` answers `"65536"`.
    /// A wrong-but-plausible limit is worse than no limit, because the row would
    /// print it as fact — so the read is the one every other reader of a procfs
    /// knob uses.
    ///
    /// @param knob the file holding the limit
    /// @return the limit, or `-1` when the file cannot be read or parsed
    static int limit(Path knob) {
        try (var reader = Files.newBufferedReader(knob, StandardCharsets.US_ASCII)) {
            String line = reader.readLine();
            return line == null ? -1 : Integer.parseInt(line.trim());
        } catch (IOException | NumberFormatException e) {
            return -1;
        }
    }

    /// How many of the next [PROBE] watches the kernel would grant.
    ///
    /// Every watch belongs to a directory of its own inside a temporary
    /// directory, so the count maps one to one onto `inotify_add_watch` calls.
    /// Everything created is removed before returning, and the kernel drops the
    /// watches when the service closes.
    ///
    /// @return the number granted (0 when the budget is gone), or `-1` when the
    ///         question cannot be asked here at all — no `/proc`, or a
    ///         filesystem that cannot be watched. `-1` means *unknown* and must
    ///         never be read as "none left": a caller that acts on it would
    ///         refuse work on a host that is merely unusual.
    public static int headroom() {
        Path root = null;
        try {
            root = Files.createTempDirectory("hdsl-watch-probe");
            try (WatchService service = FileSystems.getDefault().newWatchService()) {
                int granted = 0;
                for (int i = 0; i < PROBE; i++) {
                    Path dir = Files.createDirectory(root.resolve("probe-" + i));
                    try {
                        dir.register(service, StandardWatchEventKinds.ENTRY_MODIFY);
                        granted++;
                    } catch (IOException refused) {
                        // The kernel said no: that is the answer, not a failure.
                        return granted;
                    }
                }
                return granted;
            }
        } catch (IOException | RuntimeException e) {
            return -1;
        } finally {
            delete(root);
        }
    }

    /// Whether a headroom reading is small enough to endanger a launch.
    ///
    /// @param headroom what [headroom] returned
    /// @return whether the budget is too small, never true for the unknown
    ///         reading
    public static boolean tooSmall(int headroom) {
        return headroom >= 0 && headroom < MIN_FREE;
    }

    /// Removes the probe's temporary directory, ignoring whatever goes wrong on
    /// the way: it is scratch space, and failing to clean it up must not turn
    /// into a failure of the measurement.
    ///
    /// @param root the directory, or `null`
    private static void delete(@org.jetbrains.annotations.Nullable Path root) {
        if (root == null) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Scratch space: nothing to report and nothing to retry.
                }
            });
        } catch (IOException ignored) {
            // Same.
        }
    }
}
