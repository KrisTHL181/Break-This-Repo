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
package org.jackhuang.hmcl.web.system;

import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.util.platform.Architecture;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// What this platform can and cannot do, stated before the operator finds out
/// through a failed install.
///
/// The panel runs on more platforms than it is *released* for. FreeBSD is the
/// case that matters today: the panel, instances and the interactive terminal
/// work, while the managed Node runtime, opencode and ZCode have upstream
/// blockers. Those are facts about the platform, not bugs an operator can fix,
/// so they belong on the settings page rather than in a release note.
///
/// Everything here is declarative except the `procfs` probe: it is the one
/// runtime fact that changes from host to host (a FreeBSD machine may mount
/// `procfs`, most do not).
@NotNullByDefault
public final class PlatformCompatibility {

    @NotNullByDefault
    public enum Level {
        /// In the release-verification matrix.
        @SerializedName("supported") SUPPORTED,
        /// Runs, with named features missing or different.
        @SerializedName("limited") LIMITED,
        /// Not verified; expect the unexpected.
        @SerializedName("unsupported") UNSUPPORTED
    }

    /// One capability line. `id` is stable so the UI can group or translate it
    /// without matching on the sentence.
    ///
    /// @param id     stable key (`core`, `terminal`, `node`, …)
    /// @param level  whether this works, works differently, or does not work
    /// @param title  what the capability is, in the operator's language
    /// @param detail one sentence: what is true here, and the workaround if any
    @NotNullByDefault
    public record Item(String id, Level level, String title, String detail) {
    }

    /// @param os      `freebsd`, `linux`, `macos`, `windows`, `unknown`
    /// @param version the kernel version string, or `""`
    /// @param arch    `x86_64`, `arm64`, …
    /// @param level   the platform's overall answer
    /// @param summary one sentence for the card header
    /// @param items   capability lines, in display order
    @NotNullByDefault
    public record Report(String os, String version, String arch, Level level,
                         String summary, List<Item> items) {
    }

    private PlatformCompatibility() {
    }

    /// The report for the running panel.
    public static Report current() {
        return of(OperatingSystem.CURRENT_OS, Architecture.CURRENT_ARCH,
                System.getProperty("os.version", ""),
                Files.isDirectory(Path.of("/proc")) && Files.isRegularFile(Path.of("/proc/stat")));
    }

    /// The report for a platform, with the one runtime probe passed in so a test
    /// can drive both branches on any machine.
    ///
    /// @param os        the operating system
    /// @param arch      the CPU architecture
    /// @param version   the kernel version string
    /// @param procfs    whether `/proc/stat` is readable (Linux metrics source)
    /// @return the declaration
    static Report of(OperatingSystem os, Architecture arch, String version, boolean procfs) {
        String archName = arch.getCheckedName();
        if (os == OperatingSystem.LINUX || os == OperatingSystem.MACOS
                || os == OperatingSystem.WINDOWS) {
            return new Report(os.getCheckedName(), version, archName, Level.SUPPORTED,
                    "当前平台在发布验收范围内。", List.of());
        }
        if (os != OperatingSystem.FREEBSD) {
            return new Report(os.getCheckedName(), version, archName, Level.UNSUPPORTED,
                    "这是未验证的平台：面板可以启动，但没有任何功能经过发布验收。",
                    List.of(new Item("core", Level.LIMITED, "面板核心",
                            "HTTP、登录与设置应当可用，但没有任何针对该平台的回归测试。")));
        }

        List<Item> items = new ArrayList<>();
        items.add(new Item("core", Level.SUPPORTED, "面板核心",
                "登录、设置、实例管理、REST 与 WebSocket 正常。"));
        items.add(new Item("terminal", Level.SUPPORTED, "交互式终端（Web Shell）",
                "PTY 走 openpty + posix_spawn，已在 FreeBSD 15.1 验证 openpty、resize、输入与退出码。"));
        items.add(new Item("dsh", Level.UNSUPPORTED, "dsh 实例",
                "上游的 node-addon-require-builtin 原生模块没有 FreeBSD 构建，dsh-app-boot 启动时"
                        + "强依赖它；即使绕过这一层，会话持久化用的 flock 也被上游明确拒绝"
                        + "（ERR_FLOCK_UNSUPPORTED_PLATFORM）。面板直接拒绝安装。"));
        items.add(new Item("node", Level.LIMITED, "托管 Node",
                "nodejs.org 不发布 FreeBSD 构建；请 pkg install node npm，面板会使用 PATH 里的 Node。"));
        items.add(new Item("opencode", Level.UNSUPPORTED, "opencode 品牌",
                "上游只发布 Linux / macOS / Windows 二进制包，没有 FreeBSD 构建，安装会在 postinstall 失败。"));
        items.add(new Item("zcode", Level.UNSUPPORTED, "ZCode 构建",
                "构建需要 Electron 桌面端，Electron 没有 FreeBSD 构建；这个实验性品类在 FreeBSD 上暂不支持。"));
        items.add(new Item("mc", Level.LIMITED, "Minecraft 控制台",
                "只有 pipe 模式（日志 + 命令框），不是交互式 PTY。"));
        if (!procfs) {
            items.add(new Item("metrics", Level.LIMITED, "系统资源监控",
                    "未挂载 procfs（/proc），CPU、磁盘、网络与 GPU 指标会大部分为空。"));
        }
        items.add(new Item("privileges", Level.LIMITED, "面板与 agent 的 uid 分离",
                "Privileges 只在 Linux / macOS 实现；FreeBSD 上面板与 agent 以同一用户运行。"));

        return new Report(os.getCheckedName(), version, archName, Level.LIMITED,
                "FreeBSD 是「部分可用」的平台：面板、品牌实例与交互式终端正常；dsh 实例与"
                        + "依赖 Electron 的 ZCode 不可用。",
                List.copyOf(items));
    }
}