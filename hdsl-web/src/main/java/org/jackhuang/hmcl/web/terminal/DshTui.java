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
package org.jackhuang.hmcl.web.terminal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshLauncher;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.DshPluginInstaller;
import org.jackhuang.hmcl.dsh.DshTuiPlugins;
import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.web.instance.InstanceRuntime;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// dsh's terminal surface, which is a plugin on a profile of its own.
///
/// There is no `dsh-tui` subcommand and no TUI from the harness itself — [DshSurface]
/// enumerates WEB, HEADLESS, ACP and SDK. The terminal is installed like any other
/// plugin and started by booting **its own profile** (`dsh --profile tui`, which upstream
/// also accepts as the bare `dsh tui`: a bare word is expanded into `--profile <word>`).
/// A terminal profile is not one of the harness's templates, so it exists only once a
/// plugin has been installed into it — which is exactly what the panel's install route
/// does, and why the profile comes from the catalogue rather than from the instance.
///
/// **The instance's web profile is never used here.** A terminal interface and
/// `dsh-web-app` are both *app* plugins: each parses the same argv with its own grammar
/// and upstream arbitrates nothing between them, so the pair stops the instance from
/// starting at all (the community's report is `error: unknown option '--no-open'`). This
/// class therefore asks [DshTuiPlugins] which profile the installed plugin belongs to,
/// builds the launch plan for that profile, and refuses a plan that would boot the web
/// app — a user's own `--profile web` in the instance's arguments included.
///
/// Two command shapes come out of that:
///
/// - a plugin that ships its own executable is started through it (`node
///   <profile>/node_modules/<package>/<its own bin>`), the way [KimiTui] starts an agent:
///   a package manager's `.bin` entry is a shell shim and starting through it would make
///   `/bin/sh` rather than the terminal the root process;
/// - a pure bundle has no entry of its own, so its profile is booted — and so is a
///   bin-shipping plugin whose file is not there, because that path always works.
@NotNullByDefault
public final class DshTui {

    private DshTui() {
    }

    /// Builds the terminal target for one dsh instance.
    ///
    /// @param instance      the instance
    /// @param resumeSession a session id to resume, or `null`
    /// @throws DshException when no terminal plugin is installed, the instance is not
    ///                      installed, or its runtime is missing
    public static TerminalTarget target(DshInstance instance, @Nullable String resumeSession)
            throws DshException {
        // Which plugin comes first, and deliberately: building a launch plan writes to the
        // instance's home — the account contract, the route left by an earlier launch, the
        // default model — so an instance that has no terminal plugin must be refused before
        // all of that happens rather than after.
        DshTuiPlugins.Entry plugin = terminalPlugin(instance);

        // Two profiles, and they are not always the same one:
        //
        // - **where the plugin's files live** — the profile it was installed into;
        // - **the profile the terminal process boots** — `dsh` takes the profile from its
        //   argv, and for a program that is the program's own choice, not the catalogue's.
        //
        // A mode bundle boots its own profile, so the two coincide. The ACP terminal is a
        // program: it boots the harness's own `acp` template (the one that mounts the ACP
        // server), and its own profile is only where its files sit. The distinction is not
        // cosmetic — the launcher writes the account's supplier route and default model into
        // the profile it is told the launch boots, so a plan built for the storage profile
        // leaves the running harness with no route at all.
        //
        // The plan is still built through `DshLauncher` rather than by hand: same home, same
        // workspace, same account contract — only the profile differs.
        DshInstance onItsProfile = instance.withProfile(runProfile(plugin));
        DshInstance onItsFiles = instance.withProfile(plugin.profile());
        DshLauncher.LaunchPlan plan =
                DshLauncher.plan(onItsProfile, InstanceRuntime.accountOf(instance));
        refuseWebProfile(plannedProfile(plan.command()));

        List<String> command = null;
        if (plugin.hasExecutable()) {
            Path entry = entryPoint(onItsFiles, plugin);
            if (entry != null) {
                command = executableCommand(DshLauncher.resolveRuntime(instance), entry, resumeSession);
            }
        }
        if (command == null) {
            // No bin — booting the profile is what a bundle's own documentation shows.
            // A package that *does* ship one cannot arrive here: [terminalPlugin] returns
            // only an entry whose entry point resolves whenever its manifest declares a
            // bin, so this is the bundle case and not a fallback for a missing file.
            // Reading it as "the one path that cannot be wrong about which files a
            // package shipped" is what let an instance whose plugin was only half
            // installed boot an empty profile and die with nothing on screen.
            command = cliCommand(plan.command(), resumeSession);
        }

        Map<String, String> environment = terminalEnvironment(
                plan.environment(), instance.dshEntryPoint().toString(), plugin);
        return new TerminalTarget("dsh", instance.id(), command, plan.workingDirectory(),
                Map.copyOf(environment));
    }

    /// The environment handed to the terminal process.
    ///
    /// Two things are stated here rather than inherited: **which harness** to spawn
    /// (`HSDL_DSH_ENTRY`, so a program drives this instance's dsh and not whichever one is
    /// first on `PATH`) and, for a program that picks a profile itself, **which profile**
    /// (`HSDL_TUI_ACP_PROFILE`, the same one [runProfile] put the plan on). The second is
    /// what keeps the account route, which the launcher writes into the planned profile,
    /// in the profile that actually boots.
    ///
    /// @param planEnvironment what the launch plan set: the account contract and its key
    /// @param dshEntry        the instance's harness entry point
    /// @param plugin          the terminal plugin being started
    /// @return the environment, never `null`
    static Map<String, String> terminalEnvironment(Map<String, String> planEnvironment,
                                                   String dshEntry,
                                                   DshTuiPlugins.Entry plugin) {
        Map<String, String> environment = new HashMap<>(System.getenv());
        environment.putAll(planEnvironment);
        environment.put("TERM", "xterm-256color");
        environment.put("COLORTERM", "truecolor");
        environment.put("HSDL_DSH_ENTRY", dshEntry);
        String declared = DshTuiPlugins.harnessProfile(plugin);
        if (declared != null) {
            environment.put(ACP_PROFILE_ENVIRONMENT_VARIABLE, declared);
        }
        return environment;
    }

    /// The variable a terminal program reads to learn which harness profile to boot.
    ///
    /// Named for the ACP terminal that introduced it; a program that takes its profile from
    /// the panel rather than from its own default is what it exists for. Also read by
    /// `locateHarness` in `plugins/dsh-acp-tui/src/dsh.js`.
    static final String ACP_PROFILE_ENVIRONMENT_VARIABLE = "HSDL_TUI_ACP_PROFILE";

    /// The profile the terminal process boots, and therefore the one its plan is built for.
    ///
    /// A mode bundle boots its own profile; a program boots the harness's own template
    /// ([DshTuiPlugins#harnessProfile]). The two are only the same by coincidence, and the
    /// account's supplier route follows the profile that boots — see the comment in
    /// [target].
    ///
    /// @param plugin the terminal plugin
    /// @return the profile name, never `null`
    static String runProfile(DshTuiPlugins.Entry plugin) {
        String declared = DshTuiPlugins.harnessProfile(plugin);
        return declared == null ? plugin.profile() : declared;
    }

    /// The terminal plugin an instance has installed, in its own profile.
    ///
    /// The first entry of [DshTuiPlugins] whose profile both **declares** the package and
    /// **holds** it, so the order of the list is the preference. Each entry is looked for
    /// in **its own** profile: three of the four use three different profiles, and one
    /// installed into `tui` is absent from the instance's `web` profile by design.
    ///
    /// Both halves are required. The manifest is written before pnpm fetches anything, so
    /// a run that could not fetch leaves a profile that claims a plugin it does not have —
    /// and that state used to be accepted here, which is what made the panel open a
    /// terminal that died instantly and silently. A declared-but-missing entry is skipped
    /// (a later one that really is installed is still offered), and when nothing else is
    /// there it is named in the refusal, because "reinstall this one" is a different
    /// instruction from "install one".
    ///
    /// Public because the REST handler asks it before it opens a terminal: opening takes
    /// the instance over — the web runtime is stopped first — so an instance with nothing
    /// to run has to be refused before that happens, not after.
    ///
    /// @param instance the instance
    /// @return the entry to start
    /// @throws DshException when none is installed, the declared one is not on disk, or the
    ///                       installed one cannot run on the harness this instance has
    public static DshTuiPlugins.Entry terminalPlugin(DshInstance instance) throws DshException {
        DshTuiPlugins.Entry declared = null;
        DshTuiPlugins.Entry misfit = null;
        for (DshTuiPlugins.Entry entry : DshTuiPlugins.ENTRIES) {
            // A capability plugin is listed in the same catalogue but is not a screen:
            // picking it here would open a "terminal" that is really the panel bridge.
            if (!entry.terminal()) {
                continue;
            }
            Map<String, String> dependencies =
                    DshPluginInstaller.readDependencies(instance.homeDirectory(), entry.profile());
            if (!DshTuiPlugins.isInstalled(entry, dependencies.keySet())) {
                continue;
            }
            // The manifest says what was asked for; the files say what is there. An
            // install that failed after writing the dependency leaves the first true and
            // the second false, and an instance in that state has to be refused **here**:
            // booting that profile dies before it can print a single byte, which reads as
            // the panel having opened a terminal that does nothing.
            if (!filesPresent(instance, entry)) {
                if (declared == null) {
                    declared = entry;
                }
                continue;
            }
            // Installed and on disk is still not enough: the tool surface comes from the
            // agent-preset layer, and a plugin written for it cannot mount on a 0.1.x
            // harness. That session starts and then has no tools at all — see
            // [DshTuiPlugins#fitsHost]. A later entry that does fit is still preferred.
            if (!DshTuiPlugins.fitsHost(entry, instance.version())) {
                if (misfit == null) {
                    misfit = entry;
                }
                continue;
            }
            return entry;
        }
        if (misfit != null) {
            throw new DshException(DshTuiPlugins.hostMismatch(misfit, instance.version()));
        }
        if (declared != null) {
            throw new DshException("实例的 " + declared.profile() + " profile 声明了 "
                    + declared.packageName() + "，但它的文件不在——安装没有真的落地（或者被删了）。"
                    + "请到实例的「安装」页重新安装这个 TUI 插件。");
        }
        throw new DshException("未安装终端界面插件，请先到实例的「安装」页安装一个 TUI 插件（推荐 "
                + DshTuiPlugins.ENTRIES.get(0).packageName() + "）。");
    }

    /// Whether a declared plugin's files are really in its profile.
    ///
    /// Two questions, because a package declares which of the two it is: a package that
    /// ships an executable is started through it, so the file its own manifest names has
    /// to be there — booting the profile instead is the bin-less path, and this package
    /// says it has a bin. A pure bundle only has to be on disk, because booting its
    /// profile is how its authors document starting it.
    ///
    /// "On disk" is a directory **and its manifest**, not the directory alone. `pnpm`
    /// creates `node_modules/<package>` before it fetches, and an install that failed
    /// half-way — or somebody deleting the contents — leaves a directory that is empty or
    /// has lost its `package.json`. That shell is not an installation: a bundle is composed
    /// from the patch its manifest names, so a manifest-less directory boots nothing.
    ///
    /// @param instance the instance
    /// @param plugin   the plugin the profile declares
    /// @return whether the profile holds what starting it needs
    /// @throws DshException when the instance cannot name its home
    public static boolean filesPresent(DshInstance instance, DshTuiPlugins.Entry plugin) throws DshException {
        Path nodeModules = profileNodeModules(instance, plugin.profile());
        Path directory = nodeModules.resolve(plugin.packageName());
        if (!Files.isDirectory(directory)) {
            return false;
        }
        if (!Files.isRegularFile(directory.resolve("package.json"))) {
            return false;
        }
        return !plugin.hasExecutable() || entryPoint(nodeModules, plugin.packageName(), plugin.bin()) != null;
    }

    /// Refuses a launch that would boot the harness's own web app.
    ///
    /// The catalogue never names `web` as a terminal plugin's profile, so this is the
    /// second half of the same rule: an instance whose own launch arguments name
    /// `--profile web` would otherwise boot the web app with a terminal plugin's files in
    /// the same profile, which is the combination that does not start. Asking the argv
    /// rather than re-deriving it is what keeps the answer true to the plan that was
    /// actually built — `dsh` takes the profile from the command line.
    ///
    /// @param profile the profile the plan boots, or `null` when it names none
    /// @throws DshException when that profile is the web one
    static void refuseWebProfile(@Nullable String profile) throws DshException {
        if (!DshTuiPlugins.WEB_PROFILE.equalsIgnoreCase(profile == null ? "" : profile)) {
            return;
        }
        throw new DshException("这个实例的启动参数把 profile 指回了 web（`--profile web`）："
                + "终端插件与 dsh-web-app 都是 app 插件，各自按自己的语法解析同一份 argv，"
                + "同 profile 装在一起会让实例起不来。请去掉该参数，让终端跑在插件自己的 profile 上。");
    }

    /// The profile a launch command boots.
    ///
    /// @param command the argv
    /// @return the profile named after `--profile`, or `null` when none is
    static @Nullable String plannedProfile(List<String> command) {
        for (int i = 0; i + 1 < command.size(); i++) {
            if ("--profile".equals(command.get(i))) {
                return command.get(i + 1);
            }
        }
        return null;
    }

    /// The directory a profile's packages live in.
    ///
    /// `dsh plugin` forwards its arguments to pnpm inside this directory, so this is
    /// where a plugin's files and its `.bin` links are.
    ///
    /// @param instance the instance
    /// @param profile  the profile
    /// @return `<DSH_HOME>/profiles/<profile>/node_modules`
    /// @throws DshException when the instance cannot name its home
    static Path profileNodeModules(DshInstance instance, String profile) throws DshException {
        return DshPluginInstaller.profileNodeModules(instance.homeDirectory(), profile);
    }

    /// Finds the executable an installed plugin ships.
    ///
    /// @param instance the instance
    /// @param plugin   the plugin
    /// @return the file to start, or `null` when the profile holds none
    /// @throws DshException when the instance cannot name its home
    static @Nullable Path entryPoint(DshInstance instance, DshTuiPlugins.Entry plugin) throws DshException {
        return entryPoint(profileNodeModules(instance, plugin.profile()),
                plugin.packageName(), plugin.bin());
    }

    /// Finds a package's executable under one `node_modules` directory.
    ///
    /// The package's own manifest is asked first: it names the script the package
    /// intends to be started, which is a file Node can run as it stands. Only then is
    /// the `.bin` entry considered, and only when it *is* that script — pnpm writes a
    /// POSIX shell shim there and npm a `.cmd` on Windows, and neither belongs as the
    /// root process of a terminal ([KimiTui] makes the same refusal).
    ///
    /// @param nodeModules the `node_modules` directory
    /// @param packageName the npm package name
    /// @param bin         the executable's name
    /// @return the script to run, or `null` when it is not there
    static @Nullable Path entryPoint(Path nodeModules, String packageName, String bin) {
        if (bin == null || bin.isBlank()) {
            return null;
        }
        Path declared = declaredBin(nodeModules.resolve(packageName), bin);
        if (declared != null) {
            return declared;
        }
        Path shim = Executables.resolve(nodeModules.resolve(".bin").resolve(bin).toString()).orElse(null);
        return shim != null && isScript(shim) ? shim : null;
    }

    /// Reads the file a package's own `package.json` names as its executable.
    ///
    /// @param packageDirectory the installed package directory
    /// @param bin              the executable's name
    /// @return the file, or `null` when the manifest names none or it is not there
    static @Nullable Path declaredBin(Path packageDirectory, String bin) {
        Path manifest = packageDirectory.resolve("package.json");
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        String relative = null;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(manifest));
            if (parsed.isJsonObject()) {
                JsonObject declared = parsed.getAsJsonObject().getAsJsonObject("bin");
                JsonElement named = declared == null ? null : declared.get(bin);
                if (named != null && named.isJsonPrimitive()) {
                    relative = named.getAsString();
                } else if (declared != null && declared.size() == 1) {
                    // One bin under another name is still the package's entry.
                    relative = declared.entrySet().iterator().next().getValue().getAsString();
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the executable of " + packageDirectory, e);
            return null;
        }
        if (relative == null || relative.isBlank()) {
            return null;
        }
        Path target = packageDirectory.resolve(relative.trim()).normalize();
        return Files.isRegularFile(target) ? target : null;
    }

    /// The argv for a plugin that ships its own executable: the interpreter and the
    /// script, so the terminal — not a shell — is the process the terminal is attached
    /// to. A native executable is started on its own.
    ///
    /// @param runtime       the instance's Node runtime
    /// @param entry         the script or binary
    /// @param resumeSession a session id to resume, or `null`
    /// @return the argv
    static List<String> executableCommand(DshNodeRuntime runtime, Path entry,
                                          @Nullable String resumeSession) {
        List<String> command = new ArrayList<>();
        if (isScript(entry)) {
            command.add(runtime.node().toString());
        }
        command.add(entry.toString());
        command.addAll(resumeArguments(resumeSession));
        return List.copyOf(command);
    }

    /// The argv for booting the plugin's profile: the instance's own boot command.
    ///
    /// The plan's command is the harness, its entry script, `--profile <the plugin's
    /// profile>` and that profile's flags — the upstream entry point, and the only thing
    /// there is to start for a pure bundle. A terminal profile boots no app of its own
    /// ([DshSurface] reads the bundles, and a terminal bundle is not one of the four), so
    /// nothing else is added to it.
    ///
    /// @param planCommand   the plan's command
    /// @param resumeSession a session id to resume, or `null`
    /// @return the argv
    /// @throws DshException when the plan cannot be one this class understands
    static List<String> cliCommand(List<String> planCommand, @Nullable String resumeSession)
            throws DshException {
        if (planCommand.size() < 2) {
            throw new DshException("unexpected dsh launch plan: " + planCommand);
        }
        List<String> command = new ArrayList<>(planCommand);
        command.addAll(resumeArguments(resumeSession));
        return List.copyOf(command);
    }

    /// The arguments a resume adds, if one was asked for.
    ///
    /// @param resumeSession the session id, or `null`
    /// @return the arguments, empty when there is nothing to resume
    private static List<String> resumeArguments(@Nullable String resumeSession) {
        if (resumeSession == null || resumeSession.isBlank()) {
            return List.of();
        }
        return List.of("--resume", resumeSession);
    }

    /// Reports whether a file is a JavaScript entry rather than a native program.
    ///
    /// @param file the file
    /// @return whether it has to be run by Node
    private static boolean isScript(Path file) {
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        return name.endsWith(".js") || name.endsWith(".mjs") || name.endsWith(".cjs");
    }
}