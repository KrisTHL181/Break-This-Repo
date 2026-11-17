/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.web.terminal.pty.ConPty;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;

/// Prints a diagnostics report describing everything HDSL needs to run.
///
/// This is the launcher's `--doctor`: it lets a user (or a bug report) show the
/// detected toolchain, the on-disk layout and registry reachability without
/// starting the desktop interface. It is also how the version-management core is
/// exercised outside the UI.
@NotNullByDefault
public final class DshDoctor {
    private DshDoctor() {
    }

    /// Runs the diagnostics and prints them.
    ///
    /// @param out the stream to print to
    /// @return the process exit code, non-zero when a hard requirement is missing
    public static int report(PrintStream out) {
        out.println("HDSL " + Metadata.VERSION + " diagnostics");
        out.println("Java:      " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        out.println("OS:        " + System.getProperty("os.name") + " / " + System.getProperty("os.arch"));
        out.println();

        out.println("Language");
        out.println("  system locale:   " + java.util.Locale.getDefault());
        out.println("  resolved locale: " + org.jackhuang.hmcl.util.i18n.I18n.getLocale().getLocale()
                + "  (display: " + org.jackhuang.hmcl.util.i18n.I18n.getLocale().getDisplayLocale() + ")");
        out.println("  supported:       " + org.jackhuang.hmcl.util.i18n.SupportedLocale.getSupportedLocales().size()
                + " locale(s)");
        out.println("  sample string:   " + org.jackhuang.hmcl.util.i18n.I18n.i18n("dsh.instance.create"));
        out.println();

        // No "Appearance" section here: HDSL-web has no theme engine, and the
        // desktop section read the desktop theme state.
        out.println("Settings");
        try {
            var settings = org.jackhuang.hmcl.setting.SettingsManager.settings();
            out.println("  accounts:        " + settings.getAccounts().size());
            out.println("  node runtime:    " + settings.defaultNodeRuntime());
            out.println("  home mode:       " + settings.defaultHomeMode());
            out.println("  open browser:    " + settings.isOpenBrowserOnLaunch());
            NpmRegistry.Effective registry = NpmRegistry.effective();
            // Which source the next install uses, and why: the one setting whose wrong value looks
            // like a hang rather than like an error.
            out.println("  npm registry:    " + registry.registry() + "  (" + registry.source() + ")");
        } catch (Throwable e) {
            out.println("  FAILED: " + e);
        }
        out.println();

        out.println("Directories");
        out.println("  user home: " + Metadata.HMCL_USER_HOME);
        out.println("  instances: " + DshPaths.INSTANCES);
        out.println();

        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            out.println("Windows");
            // Symbolic links need `SeCreateSymbolicLinkPrivilege`, which an ordinary account has
            // only with Developer Mode on or an elevated process. Nothing here requires them —
            // the launcher copies a file where it would have linked one, and writes a pointer
            // file where it would have pointed a link at a release — but which mode this machine
            // is in is the first thing a failed install raises, so it is worth answering.
            out.println("  symbolic links:  "
                    + (org.jackhuang.hmcl.util.io.Links.creatable()
                            ? "permitted"
                            : "NOT permitted (Developer Mode off, or not elevated)")
                    + "  [not required: a copy stands in, and a `current` pointer becomes a file]");
            // An instance's harness sits several directories below this, and pnpm's own nesting
            // is what pushes a path past the 260-character limit Windows still applies unless
            // long paths are enabled. The number is what says whether that is a risk here.
            // The launcher's user directory — where instances and their `node_modules` live —
            // is not the panel's own data directory (`HDSL_DATA`, default `./data`). The long
            // path risk belongs to the former, so the label has to say so.
            String userDirectory = Metadata.HMCL_USER_HOME.toString();
            out.println("  user directory:  " + userDirectory);
            out.println("                   (instances and runtimes; the panel's own data dir is HDSL_DATA)");
            out.println("  its length:      " + userDirectory.length() + " characters"
                    + (userDirectory.length() > 120
                            ? "  [LONG: an instance's node_modules may exceed the 260-character limit;"
                                    + " keep the user directory nearer the drive root]"
                            : "  [ok]"));
            out.println("  console encoding: " + org.jackhuang.hmcl.util.platform.OperatingSystem.NATIVE_CHARSET);
            // The doctor is where a refused terminal gets explained: the build number is
            // the one fact that turns "unsupported" into something a person can act on.
            out.println("  instance terminal: " + (ConPty.platformSupported()
                    ? ConPty.describe() + "  [ok]"
                    : "unsupported: needs build " + ConPty.REQUIRED_BUILD
                      + ", this is " + ConPty.buildNumber() + "  [unsupported]"));
            out.println();
        }

        out.println("JavaScript toolchain");
        // The launch path is `detect().or(detectManaged)`: a bare deployment
        // installs its Node from this panel's own settings page, so a doctor that
        // only looked at PATH answered "NOT FOUND" over a panel that was running
        // instances perfectly well.
        Optional<DshNodeRuntime> system = DshNodeRuntime.detect();
        Optional<DshNodeRuntime> detected = system.isPresent() ? system : DshNodeRuntime.detectManaged();
        boolean runtimeOk;
        if (detected.isEmpty()) {
            out.println("  node:  NOT FOUND");
            out.println("  required: " + DshNodeRuntime.requirement());
            runtimeOk = false;
        } else {
            DshNodeRuntime runtime = detected.get();
            out.println("  node:  " + runtime.nodeVersion() + "  (" + runtime.node() + ")"
                    + (system.isPresent() ? "" : "  [panel-managed runtime]")
                    + (runtime.isNodeSupported() ? "  [supported]" : "  [UNSUPPORTED]"));
            out.println("  npm:   " + describe(runtime.npm(), runtime.npmVersion()));
            boolean pnpmTooOld = DshVersionManager.isPnpmTooOld(runtime.pnpmVersion());
            out.println("  pnpm:  " + describe(runtime.pnpm(), runtime.pnpmVersion())
                    + (runtime.canManagePlugins() ? "" : "  [plugin management unavailable]")
                    + (pnpmTooOld ? "  [too old for installation]" : ""));
            out.println("  required: " + DshNodeRuntime.requirement());
            // A runtime that cannot manage plugins, or whose pnpm is under the floor an installation
            // needs, is not one the harness can be installed on: answering "ready" over it would be
            // the doctor reporting the state of the machine rather than the state of the job.
            runtimeOk = runtime.isNodeSupported() && runtime.canInstall()
                    && runtime.canManagePlugins() && !pnpmTooOld;
        }
        out.println();

        out.println("File watchers");
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            // The budget below is the kernel's inotify count, which is a Linux facility. Windows
            // has no equivalent to run out of, so this row is neither a pass nor a failure here —
            // printing an inotify number at all would be the kind of row that gets misread.
            out.println("  not applicable: the inotify watch budget is a Linux limit");
        } else {
            printFileWatchers(out, org.jackhuang.hmcl.util.platform.WatcherBudget.limit(),
                    org.jackhuang.hmcl.util.platform.WatcherBudget.headroom());
        }
        out.println();

        out.println("Instances and the DeepSeek Harness each carries");
        List<DshInstance> installed = DshInstanceManager.list();
        if (installed.isEmpty()) {
            out.println("  (none)");
        } else {
            for (DshInstance instance : installed) {
                String location;
                try {
                    location = instance.dshDirectory().toString();
                } catch (DshException e) {
                    location = "(unusable identifier)";
                }
                out.println("  " + instance.id() + "  dsh " + instance.version()
                        + "  ->  " + location
                        + (DshVersionManager.isInstalled(instance) ? "" : "  [INCOMPLETE]"));
            }
        }
        out.println();

        out.println("npm registry");
        if (!runtimeOk) {
            out.println("  skipped (toolchain incomplete)");
        } else {
            try {
                List<DshRelease> releases = DshVersionManager.fetchReleases();
                out.println("  reachable, " + releases.size() + " published version(s)");
                releases.stream().limit(5).forEach(release ->
                        out.println("    " + release.version()
                                + (release.primaryTag() == null ? "" : "  [" + release.primaryTag() + "]")));
            } catch (DshException e) {
                out.println("  FAILED: " + e.getMessage());
            }
        }

        out.println();
        out.println(runtimeOk ? "Result: ready to install DeepSeek Harness versions."
                : "Result: the JavaScript toolchain is incomplete.");
        return runtimeOk ? 0 : 1;
    }

    /// Prints the kernel's file-watch budget as a section of the report.
    ///
    /// A row rather than a hard failure: the panel itself needs no watches, so
    /// a host whose budget is gone still installs, updates and serves. What
    /// stops working is *launching* — an instance whose watcher the kernel
    /// refuses either exits on the spot or comes up without ever printing its
    /// readiness line, and neither ending names this cause. This row names it
    /// while nothing is broken yet.
    ///
    /// @param out   the stream to print to
    /// @param limit the kernel's limit, or `-1` when it cannot be read
    /// @param free  the measured headroom, or `-1` when it cannot be measured
    static void printFileWatchers(PrintStream out, int limit, int free) {
        if (free < 0) {
            out.println("  inotify watches: unmeasurable on this host");
            return;
        }
        out.println("  inotify watches: " + freeText(free) + " free of "
                + (limit > 0 ? String.valueOf(limit) : "an unknown limit")
                + (org.jackhuang.hmcl.util.platform.WatcherBudget.tooSmall(free) ? "  [SHORTAGE]" : "  [ok]"));
        if (!org.jackhuang.hmcl.util.platform.WatcherBudget.tooSmall(free)) {
            return;
        }
        out.println("  DeepSeek Harness watches its own profile directory, and the kernel counts");
        out.println("  watches per user: every process running as this uid shares the number, in");
        out.println("  this container or not. With the budget gone an instance dies of ENOSPC, or");
        out.println("  starts without ever reporting ready; the panel then has nothing to open.");
        out.println("  Raise the limit on the host — no container restart is needed:");
        out.println("    sysctl -w " + org.jackhuang.hmcl.util.platform.WatcherBudget.LIMIT_PATH + "=524288");
    }

    /// The headroom as the report words it.
    ///
    /// A probe that was granted everything it asked for has only shown that
    /// there is *at least* that much room — printing the number alone would
    /// read as "64 of 65536 left", which is the opposite of what it means.
    ///
    /// @param free the measured headroom, never negative
    /// @return the number, qualified when it is the probe's own ceiling
    private static String freeText(int free) {
        return free >= org.jackhuang.hmcl.util.platform.WatcherBudget.PROBE ? "at least " + free
                : String.valueOf(free);
    }

    /// Renders an optional executable and its version.
    ///
    /// @param path    the executable, or `null`
    /// @param version the reported version, or `null`
    /// @return a display string
    private static String describe(@Nullable java.nio.file.Path path, @Nullable String version) {
        if (path == null) {
            return "NOT FOUND";
        }
        return (version == null ? "unknown" : version) + "  (" + path + ")";
    }
}
