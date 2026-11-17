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

import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.DshTuiPlugins;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// The terminal is a plugin on **its own profile**, and the instance's web profile is
/// never it: a terminal interface and `dsh-web-app` are both app plugins, and sharing a
/// profile with a second app plugin is what stops the instance from starting. What is
/// started here is the plugin's own executable with Node, or its profile booted the
/// upstream way (`dsh --profile <name>`).
class DshTuiTest {

    /// The instance runs on a home of its own, so nothing here reads or writes the
    /// launcher's real one.
    private static DshInstance instanceWith(Path home, String profile) {
        return instanceWith(home, profile, "0.1.7-rc.1");
    }

    private static DshInstance instanceWith(Path home, String profile, String dshVersion) {
        return new DshInstance("tui-test", dshVersion, profile, home.resolve("workspace").toString(),
                null, DshHomeMode.CUSTOM, home.toString(), List.of(), Map.of(), null, null, null, 0, 0L);
    }

    /// Writes what the installer's own `dsh plugin add` leaves behind: the profile manifest,
    /// **and the files that came with it**.
    ///
    /// A manifest on its own is not an installed plugin — it is the state an install leaves
    /// when the fetch fails after the dependency was written, which is exactly the state the
    /// launcher refuses instead of starting. So every package also gets the directory pnpm
    /// would fill under `node_modules/<name>`, and the file its manifest names as a bin:
    /// a package that ships an executable is started through that file.
    private static void install(DshInstance instance, String profile, String... packages) throws Exception {
        declareOnly(instance, profile, packages);
        Path nodeModules = instance.homeDirectory().resolve("profiles").resolve(profile).resolve("node_modules");
        for (String name : packages) {
            layPackage(nodeModules.resolve(name), name);
        }
    }

    /// The files one installed package has: a manifest that declares a bin named after the
    /// package, and the script it points at.
    private static void layPackage(Path packageDirectory, String name) throws Exception {
        String bin = name.substring(name.lastIndexOf('/') + 1);
        Files.createDirectories(packageDirectory.resolve("lib"));
        Files.writeString(packageDirectory.resolve("lib/main.js"), "// " + name + "\n");
        Files.writeString(packageDirectory.resolve("package.json"),
                "{\"name\":\"" + name + "\",\"version\":\"1.0.0\",\"bin\":{\"" + bin + "\":\"lib/main.js\"}}");
    }

    /// A manifest with a dependency and nothing on disk: what an install that could not
    /// fetch leaves, and what the launcher must not treat as a terminal.
    private static void declareOnly(DshInstance instance, String profile, String... packages) throws Exception {
        StringBuilder dependencies = new StringBuilder();
        for (String name : packages) {
            if (!dependencies.isEmpty()) {
                dependencies.append(',');
            }
            dependencies.append('"').append(name).append("\":\"1.0.0\"");
        }
        Path profileDirectory = instance.homeDirectory().resolve("profiles").resolve(profile);
        Files.createDirectories(profileDirectory);
        Files.writeString(profileDirectory.resolve("package.json"),
                "{\"dependencies\":{" + dependencies + "}}");
    }

    @Test
    void anInstanceWithNoTerminalPluginIsRefusedWithWhereToGetOne(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web");

        DshException refused = assertThrows(DshException.class, () -> DshTui.terminalPlugin(instance));

        assertTrue(refused.getMessage().contains("未安装终端界面插件"), refused.getMessage());
        assertTrue(refused.getMessage().contains("「安装」"), refused.getMessage());
        assertTrue(refused.getMessage().contains(DshTuiPlugins.ENTRIES.get(0).packageName()),
                "the refusal names what to install: " + refused.getMessage());
    }

    /// The hard constraint, pinned from the side that can be tested here: the same
    /// package written into the *web* profile is not a terminal plugin this launcher will
    /// start, because that profile is where the two app plugins collide.
    @Test
    void aTerminalPluginInTheWebProfileIsNotInstalled(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web");
        install(instance, "web", "@deepseek-harness-tui/dsh-tui");

        assertThrows(DshException.class, () -> DshTui.terminalPlugin(instance));
    }

    @Test
    void aPluginThatShipsItsOwnExecutableIsStartedWithNode(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web");
        DshTuiPlugins.Entry plugin = DshTuiPlugins.find("@deepseek-harness-tui/dsh-tui").orElseThrow();
        assertEquals("dsh-tui", plugin.profile(), "the profile is the plugin's own, not the instance's");
        assertTrue(plugin.hasExecutable());
        install(instance, plugin.profile(), plugin.packageName());

        assertEquals(plugin.packageName(), DshTui.terminalPlugin(instance).packageName());

        // The package's own manifest names its entry; the file is what is started, and it
        // is looked for in the plugin's profile.
        Path packageDirectory = DshTui.profileNodeModules(instance, plugin.profile())
                .resolve(plugin.packageName());
        Files.createDirectories(packageDirectory.resolve("bin"));
        Files.writeString(packageDirectory.resolve("bin/dsh-tui.js"), "console.log('tui');\n");
        Files.writeString(packageDirectory.resolve("package.json"),
                "{\"name\":\"@deepseek-harness-tui/dsh-tui\",\"bin\":{\"dst\":\"bin/dsh-tui.js\",\"dsh-tui\":\"bin/dsh-tui.js\"}}");

        Path entry = DshTui.entryPoint(instance, plugin);
        assertEquals(packageDirectory.resolve("bin/dsh-tui.js").toAbsolutePath().normalize(), entry);

        DshNodeRuntime runtime = new DshNodeRuntime(Path.of("/opt/node/bin/node"), "22.19.0",
                null, null, null, null);
        assertEquals(List.of("/opt/node/bin/node", entry.toString(), "--resume", "sess-1"),
                DshTui.executableCommand(runtime, entry, "sess-1"));
        assertEquals(List.of("/opt/node/bin/node", entry.toString()),
                DshTui.executableCommand(runtime, entry, null),
                "no session to resume means no flag");
    }

    /// A profile that *declares* a bin-shipping plugin and does not hold its file is not a
    /// terminal, and booting it is exactly what leaves an empty screen: the harness starts,
    /// finds nothing to load, and dies before it can print. The declaration alone is
    /// therefore refused, and the refusal says which package and what to do about it —
    /// which is the difference between a user seeing "reinstall this" and watching a
    /// terminal flash.
    @Test
    void aDeclaredPluginWithNoFilesIsRefusedRatherThanBooted(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web");
        DshTuiPlugins.Entry plugin = DshTuiPlugins.find("@deepseek-harness-tui/dsh-tui").orElseThrow();
        assertTrue(plugin.hasExecutable());
        declareOnly(instance, plugin.profile(), plugin.packageName());

        assertNull(DshTui.entryPoint(instance, plugin), "there is no file to start");

        DshException refused = assertThrows(DshException.class, () -> DshTui.terminalPlugin(instance));
        assertTrue(refused.getMessage().contains(plugin.packageName()),
                "the refusal names the package: " + refused.getMessage());
        assertTrue(refused.getMessage().contains("重新安装"),
                "the refusal says what to do about it: " + refused.getMessage());
    }

    /// A declared-but-missing entry does not shadow a working one: the list's order is a
    /// preference, not a promise that everything ahead of an entry is usable.    @Test
    void aDeclaredButMissingEntryDoesNotShadowAWorkingOne(@TempDir Path home) throws Exception {
        // On a harness that can run the 0.2-only entries: on 0.1.x this would be the harness
        // refusal rather than the shadowing question, which is the test below.
        DshInstance instance = instanceWith(home, "web", "0.2.1-rc.3");
        DshTuiPlugins.Entry preferred = DshTuiPlugins.ENTRIES.get(0);
        DshTuiPlugins.Entry bundle = DshTuiPlugins.find("@tomowang/dsh-tui").orElseThrow();
        declareOnly(instance, preferred.profile(), preferred.packageName());
        install(instance, bundle.profile(), bundle.packageName());

        assertEquals(bundle.packageName(), DshTui.terminalPlugin(instance).packageName(),
                "the plugin whose files are really there is what gets started");
    }

    /// Installed and on disk is still not enough: a plugin written for the 0.2.0 harness
    /// mounts its own agent-preset layer, and that layer cannot mount on a 0.1.x instance —
    /// the window opens and the agent has no tools at all (the plugin says so in its own
    /// words: `agent-presets 未装配：本会话没有工具面`). The pair is refused before the
    /// takeover, with the same sentence the install route uses.
    @Test
    void aPluginForANewerHarnessIsRefusedInsteadOfStarted(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web");
        DshTuiPlugins.Entry plugin = DshTuiPlugins.find("@tomowang/dsh-tui").orElseThrow();
        install(instance, plugin.profile(), plugin.packageName());

        DshException refused = assertThrows(DshException.class, () -> DshTui.terminalPlugin(instance));
        assertTrue(refused.getMessage().contains(plugin.requiresDsh()), refused.getMessage());
        assertTrue(refused.getMessage().contains("agent-preset"), refused.getMessage());
        assertTrue(refused.getMessage().contains("0.1.7-rc.1"), refused.getMessage());
    }

    /// The same plugin starts normally on the harness it was written for: this is a pairing
    /// check, not a blacklist.
    @Test
    void theSamePluginStartsOnTheHarnessItWasWrittenFor(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web", "0.2.1-rc.3");
        DshTuiPlugins.Entry plugin = DshTuiPlugins.find("@tomowang/dsh-tui").orElseThrow();
        install(instance, plugin.profile(), plugin.packageName());

        assertEquals(plugin.packageName(), DshTui.terminalPlugin(instance).packageName());
    }

    /// pnpm writes `.bin/<tool>` as a shell shim; starting through it would make
    /// `/bin/sh` the root process of the terminal, so only a shim that resolves to
    /// the package's own script is taken.
    @Test
    void theShimIsTakenOnlyWhenItIsThePackagesOwnScript(@TempDir Path nodeModules) throws Exception {
        assumeTrue(OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS, "the refusal is about POSIX shims");

        Path bin = nodeModules.resolve(".bin");
        Files.createDirectories(bin);
        Path shim = bin.resolve("dsh-tui");
        Files.writeString(shim, "#!/bin/sh\nexec node \"$0/../lib/bin.js\" \"$@\"\n");
        Files.setPosixFilePermissions(shim, PosixFilePermissions.fromString("rwx------"));
        assertNull(DshTui.entryPoint(nodeModules, "@huiliyi37/dsh-tianshu-tui", "dsh-tui"),
                "a shell shim is not the entry to start");

        // The same name, linked to the script the package ships, is.
        Path script = nodeModules.resolve("tui-package/lib/bin.js");
        Files.createDirectories(script.getParent());
        Files.writeString(script, "console.log('tui');\n");
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));
        Files.delete(shim);
        Files.createSymbolicLink(shim, script);

        assertEquals(script.toRealPath(), DshTui.entryPoint(nodeModules, "tui-package", "dsh-tui"));
    }

    @Test
    void aPureBundleIsStartedByBootingItsProfile(@TempDir Path home) throws Exception {
        // The pure-bundle entries are written for the 0.2.x harness, so the instance has to
        // be one: otherwise the (correct) harness refusal would be what this test sees.
        DshInstance instance = instanceWith(home, "web", "0.2.1-rc.3");
        DshTuiPlugins.Entry plugin = DshTuiPlugins.find("@tomowang/dsh-tui").orElseThrow();
        assertEquals("tui", plugin.profile());
        assertFalse(plugin.hasExecutable(), "a pure bundle has no entry of its own");
        install(instance, plugin.profile(), plugin.packageName());

        assertEquals(plugin.packageName(), DshTui.terminalPlugin(instance).packageName());

        // The plan is the harness's own argv — interpreter, entry, `--profile <name>` —
        // and that is what is started; the profile in it is the plugin's, never `web`.
        List<String> plan = List.of("/opt/node/bin/node", "/data/dsh/lib/bin.js", "--profile", "tui");
        assertEquals("tui", DshTui.plannedProfile(plan));
        assertEquals(plan, DshTui.cliCommand(plan, null));
        assertEquals(List.of("/opt/node/bin/node", "/data/dsh/bin.js", "--profile", "tui", "--resume", "abc"),
                DshTui.cliCommand(List.of("/opt/node/bin/node", "/data/dsh/bin.js", "--profile", "tui"), "abc"));
        assertNull(DshTui.plannedProfile(List.of("/opt/node/bin/node", "/data/dsh/bin.js")));
    }

    /// The rule applies to the instance's own arguments too: `--profile web` in them
    /// would boot the web app over the terminal's profile, which is the combination that
    /// does not start.
    @Test
    void aPlanThatBootsTheWebProfileIsRefused() throws Exception {
        DshException refused = assertThrows(DshException.class, () -> DshTui.refuseWebProfile("web"));
        assertTrue(refused.getMessage().contains("--profile web"), refused.getMessage());
        assertTrue(refused.getMessage().contains("app 插件"), refused.getMessage());

        DshTui.refuseWebProfile("tui");
        DshTui.refuseWebProfile("pi-tui");
        DshTui.refuseWebProfile(null);
        DshTui.refuseWebProfile("");
    }

    @Test
    void theRecommendedPluginWinsWhenSeveralAreInstalled(@TempDir Path home) throws Exception {
        // The first entry is the in-process bundle, which needs the 0.2.x seams.
        DshInstance instance = instanceWith(home, "web", "0.2.1-rc.3");
        DshTuiPlugins.Entry recommended = DshTuiPlugins.ENTRIES.get(0);
        // Each in its own profile — the profiles differ, and that is the point.
        install(instance, "tui", "@tomowang/dsh-tui");
        install(instance, recommended.profile(), recommended.packageName());

        assertEquals(recommended.packageName(), DshTui.terminalPlugin(instance).packageName(),
                "the list's order is the preference");
    }

    /// Both terminals may be installed at once; which one runs is the list order plus
    /// `fitsHost`. On the current line the in-process one runs, on an older one the ACP
    /// terminal takes over — the version-bounce case the pair exists for.
    @Test
    void theInProcessTerminalRunsFirstOnTheCurrentLine(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web", "0.2.1-rc.3");
        install(instance, "dsh-acp-tui", "@nas200-timmy/dsh-acp-tui");
        install(instance, "dsh-inproc-tui", "@nas200-timmy/dsh-inproc-tui");

        assertEquals("@nas200-timmy/dsh-inproc-tui", DshTui.terminalPlugin(instance).packageName());
    }

    @Test
    void theAcpTerminalTakesOverWhenTheInProcessOneCannotRun(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web", "0.1.7-rc.1");
        install(instance, "dsh-inproc-tui", "@nas200-timmy/dsh-inproc-tui");
        install(instance, "dsh-acp-tui", "@nas200-timmy/dsh-acp-tui");

        assertEquals("@nas200-timmy/dsh-acp-tui", DshTui.terminalPlugin(instance).packageName(),
                "the fallback is what runs when the fuller terminal cannot fit the host");
    }

    /// The plan decides **where** the account's supplier route and default model are
    /// written, and the terminal program decides **which profile boots**. When those two
    /// disagree the account silently does not work: the harness starts on a profile with
    /// no route in it. The ACP terminal is a program — it boots the harness's own `acp`
    /// template, not the profile its files were installed into — so the plan has to be
    /// built for `acp`, and the same name has to be stated in the environment.
    @Test
    void theAcpTerminalIsPlannedForTheProfileItBoots() {
        DshTuiPlugins.Entry acp = DshTuiPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
        assertTrue(acp.hasExecutable(), "the ACP terminal is a program");
        assertEquals("dsh-acp-tui", acp.profile(), "its files live in its own profile");
        assertEquals("acp", DshTuiPlugins.harnessProfile(acp), "and it boots the harness's template");
        assertEquals("acp", DshTui.runProfile(acp), "the plan follows the profile that boots, not the storage one");
    }

    /// A mode bundle has no separate boot profile: `dsh --profile <its profile>` is how it
    /// is started, so nothing is stated twice and nothing can disagree.
    @Test
    void aModeBundleIsPlannedForItsOwnProfile() {
        DshTuiPlugins.Entry inproc = DshTuiPlugins.find("@nas200-timmy/dsh-inproc-tui").orElseThrow();
        assertFalse(inproc.hasExecutable());
        assertNull(DshTuiPlugins.harnessProfile(inproc));
        assertEquals(inproc.profile(), DshTui.runProfile(inproc));
    }

    /// The environment is where the agreement becomes real: a program is told which profile
    /// the plan was built for, and a bundle is not given a variable it would ignore.
    @Test
    void theTerminalIsToldTheProfileThePlanWasBuiltFor() {
        DshTuiPlugins.Entry acp = DshTuiPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
        Map<String, String> plan = Map.of("HDSL_LAUNCH_API_KEY_ACME_00000000", "sk-acme", "DSH_HOME", "/tmp/home");

        Map<String, String> program = DshTui.terminalEnvironment(plan, "/opt/dsh/lib/bin.js", acp);
        assertEquals("acp", program.get(DshTui.ACP_PROFILE_ENVIRONMENT_VARIABLE));
        assertEquals("/opt/dsh/lib/bin.js", program.get("HSDL_DSH_ENTRY"));
        assertEquals("sk-acme", program.get("HDSL_LAUNCH_API_KEY_ACME_00000000"),
                "the account key travels under its own name, never as another vendor's");
        assertEquals(null, program.get("DEEPSEEK_API_KEY"),
                "nothing renames a third-party key into the harness's own vendor variable");

        DshTuiPlugins.Entry inproc = DshTuiPlugins.find("@nas200-timmy/dsh-inproc-tui").orElseThrow();
        Map<String, String> bundle = DshTui.terminalEnvironment(plan, "/opt/dsh/lib/bin.js", inproc);
        assertEquals(null, bundle.get(DshTui.ACP_PROFILE_ENVIRONMENT_VARIABLE));
        assertEquals("/opt/dsh/lib/bin.js", bundle.get("HSDL_DSH_ENTRY"));
    }

    /// A directory is not an installation. `pnpm` creates `node_modules/<package>` before it
    /// fetches, and a fetch that fails — or a delete that leaves the directory — leaves a
    /// shell with no manifest in it. Booting that profile dies before it can print, so it is
    /// not "present".
    @Test
    void anEmptyPackageDirectoryIsNotAnInstalledBundle(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web", "0.2.1-rc.3");
        DshTuiPlugins.Entry bundle = DshTuiPlugins.find("@nas200-timmy/dsh-inproc-tui").orElseThrow();
        assertFalse(bundle.hasExecutable());
        declareOnly(instance, bundle.profile(), bundle.packageName());
        Path directory = DshTui.profileNodeModules(instance, bundle.profile()).resolve(bundle.packageName());
        Files.createDirectories(directory);

        assertFalse(DshTui.filesPresent(instance, bundle), "an empty directory holds nothing to boot");
        assertThrows(DshException.class, () -> DshTui.terminalPlugin(instance));

        Files.writeString(directory.resolve("package.json"), "{\"name\":\"" + bundle.packageName() + "\"}");
        assertTrue(DshTui.filesPresent(instance, bundle), "with its manifest it is a bundle again");
        assertEquals(bundle.packageName(), DshTui.terminalPlugin(instance).packageName());
    }

    /// The same question for a package that ships an executable: the manifest alone is not
    /// enough either, the file it names has to be there.
    @Test
    void aPackageWhoseEntryFileIsGoneIsNotPresent(@TempDir Path home) throws Exception {
        DshInstance instance = instanceWith(home, "web");
        install(instance, "dsh-acp-tui", "@nas200-timmy/dsh-acp-tui");
        DshTuiPlugins.Entry plugin = DshTuiPlugins.find("@nas200-timmy/dsh-acp-tui").orElseThrow();
        assertTrue(DshTui.filesPresent(instance, plugin));

        Files.delete(DshTui.profileNodeModules(instance, plugin.profile())
                .resolve(plugin.packageName()).resolve("lib/main.js"));

        assertFalse(DshTui.filesPresent(instance, plugin));
    }

    @Test
    void aPlanWithoutAnEntryIsRefused() {
        assertThrows(DshException.class, () -> DshTui.cliCommand(List.of("node"), null));
    }
}