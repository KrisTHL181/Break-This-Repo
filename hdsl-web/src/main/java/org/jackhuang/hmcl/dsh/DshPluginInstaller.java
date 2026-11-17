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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Adds and removes profile plugins by driving `dsh plugin`.
///
/// The launcher deliberately does not talk to npm itself. Upstream already
/// ships a forwarder — `dsh plugin --profile <name> <pnpm args>` runs `pnpm`
/// inside the profile directory and then reconciles `dsh.profile.bundles` — so
/// shelling out to it keeps the lock handling, the bundle reconciliation and
/// the failure modes exactly as upstream defines them.
///
/// All calls run against the instance's own `DSH_HOME` and its pinned `dsh`
/// installation, so they cannot disturb another instance or the user's global
/// `dsh`.
@NotNullByDefault
public final class DshPluginInstaller {
    private DshPluginInstaller() {
    }

    /// Installs every given preset into an instance's profile.
    ///
    /// Presets are installed one at a time so that a failure names the package
    /// that caused it, and so partial progress is visible in the log.
    ///
    /// @param instance the instance whose profile is modified
    /// @param presets  the presets to install
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when the runtime or version is unavailable, `pnpm`
    ///                       is missing, or an install fails
    public static void install(DshInstance instance, List<DshPreset> presets,
                               @Nullable Consumer<String> onLine) throws DshException {
        installSpecs(instance, presets.stream().map(DshPreset::spec).toList(), onLine);
    }

    /// Installs explicit package specs into an instance's profile.
    ///
    /// The specs may pin a version — `name@1.2.3` — which is what the install
    /// wizard records when the user picks a specific release on a plugin's
    /// choice page.
    ///
    /// @param instance the instance whose profile is modified
    /// @param specs    the package specs to install
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when the runtime or version is unavailable, `pnpm`
    ///                       is missing, or an install fails
    public static void installSpecs(DshInstance instance, List<String> specs,
                                    @Nullable Consumer<String> onLine) throws DshException {
        installSpecs(instance, instance.profile(), specs, onLine);
    }

    /// Installs explicit package specs into a named profile.
    ///
    /// The profile is a parameter because not every plugin belongs in the instance's
    /// own. A terminal interface is an *app* plugin, exactly like `dsh-web-app`, and two
    /// app plugins in one profile each parse the whole argv with their own grammar while
    /// upstream arbitrates nothing between them — so it is installed into the profile its
    /// authors document (`tui`, `dsh-tui`, `pi-tui`) and started there. Everything else
    /// about the run is the same, which is why this is the same method: the profile is
    /// the only thing that differs.
    ///
    /// The refusal comes **before** the runtime is resolved, so being told "not into the
    /// web profile" does not depend on node and pnpm being present.
    ///
    /// @param instance the instance whose profile is modified
    /// @param profile  the profile inside its `DSH_HOME` to modify
    /// @param specs    the package specs to install
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when a spec may not go into that profile, the runtime or
    ///                       version is unavailable, `pnpm` is missing, or a run fails
    public static void installSpecs(DshInstance instance, String profile, List<String> specs,
                                    @Nullable Consumer<String> onLine) throws DshException {
        if (specs.isEmpty()) {
            return;
        }
        for (String spec : specs) {
            DshTuiPlugins.refuseAppProfile(spec, profile);
        }

        DshNodeRuntime runtime = DshLauncher.resolveRuntimeForPnpm(instance);
        if (!runtime.canManagePlugins()) {
            throw runtime.pnpmMissing("installing plugins");
        }

        Path home = instance.homeDirectory();
        for (String spec : specs) {
            report(onLine, "Installing " + spec + " ...");
            runPluginCommand(instance, runtime, home, profile, List.of("add", spec), onLine);
            requireLanded(home, profile, spec);
            report(onLine, "Installed " + spec);
        }
    }

    /// The `node_modules` directory of one profile.
    ///
    /// @param home    the `DSH_HOME` holding the profile
    /// @param profile the profile name
    /// @return the directory packages are installed under
    public static Path profileNodeModules(Path home, String profile) {
        return home.resolve("profiles").resolve(profile).resolve("node_modules");
    }

    /// Reports whether a profile really holds a package's files.
    ///
    /// A manifest is the record of what was *asked* for; this is the record of what is
    /// *there*. The two come apart when a package manager fails after the dependency was
    /// written — a registry that 404s, a store that came back empty — and an instance in
    /// that state is worse than an uninstalled one: the profile says the package is
    /// installed, so the panel starts it, and the harness dies with nothing on screen
    /// because the package it would load is not there.
    ///
    /// A pnpm link inside `node_modules` counts, which is why the question is asked of
    /// the path rather than of the directory entry: `Files.isDirectory` follows the link
    /// and is false only when it dangles, and a dangling link is exactly the half-installed
    /// state this exists to catch.
    ///
    /// @param home        the `DSH_HOME` holding the profile
    /// @param profile     the profile name
    /// @param packageName the npm package name, or `null`
    /// @return whether the package's directory is there
    public static boolean holdsPackage(Path home, String profile, @Nullable String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return false;
        }
        return Files.isDirectory(profileNodeModules(home, profile).resolve(packageName.trim()));
    }

    /// Fails a run that reported success without leaving the package behind.
    ///
    /// `dsh plugin add` forwards to pnpm, and pnpm can exit zero with the dependency
    /// written into the manifest and nothing under `node_modules`: the fetch is what makes
    /// the files, and a run that could not fetch reports a package it never placed — the
    /// field report behind this check is a profile whose `package.json` named a TUI plugin
    /// while its `node_modules` held nothing, so the panel opened a terminal that died in
    /// a fraction of a second with no output at all.
    ///
    /// Only registry specs are verified. A `file:`, `link:`, `github:` or URL spec resolves
    /// to a name this method cannot predict from the text — the local-install routes pass
    /// paths — and a false failure there would break a working install.
    ///
    /// @param home    the `DSH_HOME` holding the profile
    /// @param profile the profile name
    /// @param spec    the specification that was installed
    /// @throws DshException when a registry package did not land
    static void requireLanded(Path home, String profile, String spec) throws DshException {
        if (!isRegistrySpec(spec)) {
            return;
        }
        String name = DshTuiPlugins.packageNameOf(spec);
        if (holdsPackage(home, profile, name)) {
            return;
        }
        throw new DshException("pnpm 报告成功，但 " + profile + " profile 的 node_modules 里没有 " + name
                + "：依赖写进了 manifest，文件没落地。请重试安装；若一直如此，多半是这个包（或这个版本）"
                + "在当前的源上取不到。");
    }

    /// Whether a specification names a registry package rather than a file, a link or a
    /// repository.
    ///
    /// @param spec the specification as typed
    /// @return whether it is a plain `name`, `name@version` or `@scope/name@range`
    static boolean isRegistrySpec(@Nullable String spec) {
        if (spec == null) {
            return false;
        }
        String raw = spec.trim();
        if (raw.isEmpty() || raw.contains(":") || raw.startsWith("/") || raw.startsWith(".") || raw.contains("\\")) {
            return false;
        }
        String name = DshTuiPlugins.packageNameOf(raw);
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.startsWith("@")) {
            // Exactly one scope separator: `@scope/name`, never `@scope/name/other`.
            return name.indexOf('/') == name.lastIndexOf('/') && name.indexOf('/') > 1;
        }
        return !name.contains("/");
    }

    /// Resolves a profile from the manifest it holds.
    ///
    /// `install` forwarded to pnpm inside the profile directory, which is what both
    /// creates a profile that does not exist yet — the harness writes its manifest,
    /// patch template and package-manager settings first — and installs whatever
    /// that manifest then lists. Restoring a pack's plugin list means writing the
    /// list and running this once: reconciliation keeps the entries a manifest
    /// already has and only appends what is missing, so the order survives.
    ///
    /// @param instance the instance whose profile is resolved
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when pnpm is missing or the resolve fails
    public static void resolve(DshInstance instance, @Nullable Consumer<String> onLine) throws DshException {
        resolve(instance, instance.profile(), onLine);
    }

    /// Resolves a named profile from the manifest it holds.
    ///
    /// @param instance the instance whose profile is resolved
    /// @param profile  the profile inside its `DSH_HOME`
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when pnpm is missing or the resolve fails
    public static void resolve(DshInstance instance, String profile, @Nullable Consumer<String> onLine)
            throws DshException {
        DshNodeRuntime runtime = DshLauncher.resolveRuntimeForPnpm(instance);
        if (!runtime.canManagePlugins()) {
            throw runtime.pnpmMissing("installing plugins");
        }
        runPluginCommand(instance, runtime, instance.homeDirectory(), profile, List.of("install"), onLine);
    }

    /// Removes a package from an instance's profile.
    ///
    /// @param instance the instance whose profile is modified
    /// @param spec     the package spec to remove
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when the removal fails
    public static void remove(DshInstance instance, String spec, @Nullable Consumer<String> onLine)
            throws DshException {
        removeSpecs(instance, List.of(spec), onLine);
    }

    /// Removes several packages from an instance's profile in one run.
    ///
    /// One command rather than one per package, which is what the interface's
    /// multiple selection asks for: `dsh plugin` forwards its arguments to pnpm
    /// verbatim, so `remove a b c` is a single pnpm run and a single pass of
    /// bundle reconciliation. Installing deliberately does not work this way —
    /// see [DshPluginInstaller#installSpecs] — but removal has no order to lose:
    /// the packages are leaving, and pnpm removes them together or reports which
    /// one stopped it.
    ///
    /// @param instance the instance whose profile is modified
    /// @param specs    the package specs to remove
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when the removal fails
    public static void removeSpecs(DshInstance instance, List<String> specs,
                                   @Nullable Consumer<String> onLine) throws DshException {
        removeSpecs(instance, instance.profile(), specs, onLine);
    }

    /// Removes several packages from a named profile in one run.
    ///
    /// Removal is never refused for a plugin that should not have been there: a profile
    /// holding a terminal plugin *and* an app is one that does not boot, and taking the
    /// plugin out is the repair. See [DshTuiPlugins#refuseAppProfile] for the install
    /// half of that pair.
    ///
    /// @param instance the instance whose profile is modified
    /// @param profile  the profile inside its `DSH_HOME`
    /// @param specs    the package specs to remove
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when the removal fails
    public static void removeSpecs(DshInstance instance, String profile, List<String> specs,
                                   @Nullable Consumer<String> onLine) throws DshException {
        if (specs.isEmpty()) {
            return;
        }

        DshNodeRuntime runtime = DshLauncher.resolveRuntimeForPnpm(instance);
        if (!runtime.canManagePlugins()) {
            throw runtime.pnpmMissing("removing plugins");
        }

        report(onLine, "Removing " + String.join(", ", specs) + " ...");
        List<String> command = new ArrayList<>();
        command.add("remove");
        command.addAll(specs);
        runPluginCommand(instance, runtime, instance.homeDirectory(), profile, command, onLine);
        report(onLine, "Removed " + specs.size() + " package(s)");
    }

    /// Reads the packages a profile declares as dependencies.
    ///
    /// This is everything the user has installed, which is a superset of the
    /// bundle list: a package only joins `dsh.profile.bundles` when it ships a
    /// bundle patch *and* the reconciling `dsh plugin` run completed. Showing
    /// both is what makes an installed-but-inactive plugin visible instead of
    /// silently doing nothing.
    ///
    /// @param home    the `DSH_HOME` holding the profile
    /// @param profile the profile name
    /// @return the dependency name to version-range map, in manifest order
    public static Map<String, String> readDependencies(Path home, String profile) {
        Path manifest = home.resolve("profiles").resolve(profile).resolve("package.json");
        if (!Files.isRegularFile(manifest)) {
            return Map.of();
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(manifest));
            if (!parsed.isJsonObject()) {
                return Map.of();
            }
            JsonObject dependencies = parsed.getAsJsonObject().getAsJsonObject("dependencies");
            if (dependencies == null) {
                return Map.of();
            }
            Map<String, String> result = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : dependencies.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getAsString());
            }
            return result;
        } catch (IOException | RuntimeException e) {
            LOG.warning("Failed to read the profile manifest " + manifest, e);
            return Map.of();
        }
    }

    /// Reads the bundle list a profile declares.
    ///
    /// @param home    the `DSH_HOME` holding the profile
    /// @param profile the profile name
    /// @return the declared bundle package names, in load order
    public static List<String> readBundles(Path home, String profile) {
        Path manifest = home.resolve("profiles").resolve(profile).resolve("package.json");
        if (!Files.isRegularFile(manifest)) {
            return List.of();
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(manifest));
            if (!parsed.isJsonObject()) {
                return List.of();
            }
            JsonObject dsh = parsed.getAsJsonObject().getAsJsonObject("dsh");
            if (dsh == null) {
                return List.of();
            }
            JsonObject profileObject = dsh.getAsJsonObject("profile");
            if (profileObject == null) {
                return List.of();
            }
            JsonArray bundles = profileObject.getAsJsonArray("bundles");
            if (bundles == null) {
                return List.of();
            }
            List<String> names = new ArrayList<>(bundles.size());
            for (JsonElement element : bundles) {
                if (element.isJsonPrimitive()) {
                    names.add(element.getAsString());
                }
            }
            return names;
        } catch (IOException | RuntimeException e) {
            LOG.warning("Failed to read the profile manifest " + manifest, e);
            return List.of();
        }
    }

    /// Runs one `dsh plugin` invocation against an instance.
    ///
    /// @param instance the instance
    /// @param runtime  the resolved Node runtime
    /// @param home     the `DSH_HOME` to operate on
    /// @param args     the pnpm arguments to forward
    /// @param onLine   receives every output line, or `null`
    /// @throws DshException when the command cannot be run or exits non-zero
    private static void runPluginCommand(DshInstance instance,
                                         DshNodeRuntime runtime,
                                         Path home,
                                         String profile,
                                         List<String> args,
                                         @Nullable Consumer<String> onLine) throws DshException {
        Path script = instance.dshEntryPoint();
        if (!Files.isRegularFile(script)) {
            throw new DshException("Instance " + instance.id()
                    + " has no DeepSeek Harness of its own; " + script + " is missing");
        }
        if (!Files.isRegularFile(script)) {
            throw new DshException("The installed version is incomplete: " + script + " is missing");
        }

        List<String> command = new ArrayList<>();
        command.add(runtime.node().toString());
        command.add(script.toString());
        command.add("plugin");
        command.add("--profile");
        command.add(profile);
        command.addAll(args);

        // DSH_* cannot come from a .env file — upstream rejects those names there
        // — so DSH_HOME must travel in the child's environment. Getting this
        // wrong would silently operate on the user's real ~/.dsh.
        Map<String, String> environment = new java.util.LinkedHashMap<>();
        environment.put("DSH_HOME", home.toString());
        environment.putAll(runtime.pathEnvironment());
        environment.putAll(DshEnvironment.of(instance));

        boolean retried = false;
        while (true) {
        try {
            DshCommand.Result result = DshCommand.run(command, instance.workspacePath(), environment,
                    line -> report(onLine, line));
            int exitCode = result.exitCode();
            if (exitCode != 0 && !retried) {
                // Two ways a plugin is waiting to be allowed to build. pnpm writes a placeholder
                // into the profile for the packages whose build it merely ignored; for a package
                // fetched from a repository it writes nothing and names the exact entry it needs
                // instead, because that entry has to carry the resolved address. Recording what it
                // named as a question puts both onto the same path — see DshBuildScripts#propose.
                List<String> named = gitBuildKeys(result.output());
                boolean placeholder = explainIgnoredBuilds(result.output(), home, profile) != null;
                if (!named.isEmpty() && DshBuildScriptPolicy.of(instance) == DshBuildScriptPolicy.NEVER) {
                    // A repository-hosted package is built **by** being installed — there is no
                    // published build to fall back on — so refusing the build refuses the
                    // installation. Saying so beats a retry that cannot succeed.
                    throw new DshException(explainGitBuild(named, home, profile));
                }
                if (!named.isEmpty()) {
                    DshBuildScripts.propose(instance, named);
                }
                if (!named.isEmpty() || placeholder) {
                    List<String> waiting = DshBuildScripts.unanswered(instance);
                    DshBuildScriptPolicy policy = DshBuildScriptPolicy.of(instance);
                    if (!waiting.isEmpty() && policy == DshBuildScriptPolicy.AUTO) {
                        // This instance's plugins may build themselves, so the question
                        // is already answered: say so and run the same command again.
                        report(onLine, "Allowing install scripts for " + String.join(", ", waiting));
                        DshBuildScripts.answer(instance, waiting, true);
                        retried = true;
                        continue;
                    }
                    if (!waiting.isEmpty() && policy == DshBuildScriptPolicy.NEVER) {
                        // The answer is no, so the packages are installed and their
                        // scripts are not run: installing is not the same decision as
                        // running what comes with it.
                        report(onLine, "Not running install scripts for " + String.join(", ", waiting));
                        DshBuildScripts.answer(instance, waiting, false);
                        retried = true;
                        continue;
                    }
                    if (!waiting.isEmpty()) {
                        // Somebody has to decide, and only the interface can ask.
                        throw new DshBuildScriptApprovalRequired(waiting);
                    }
                }
            }
            if (exitCode != 0) {
                String explanation = explainIgnoredBuilds(result.output(), home, profile);
                if (explanation == null) {
                    explanation = explainGitBuild(gitBuildKeys(result.output()), home, profile);
                }
                if (explanation != null) {
                    throw new DshException(explanation);
                }
                throw new DshException("`dsh plugin " + String.join(" ", args)
                        + "` exited with code " + exitCode + ":\n" + tail(result.output()));
            }
        } catch (IOException e) {
            throw new DshException("Failed to run `dsh plugin " + String.join(" ", args) + "`", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("`dsh plugin " + String.join(" ", args) + "` was interrupted", e);
        }
            return;
        }
    }

    /// Thrown when an installation is waiting for somebody to allow its scripts.
    ///
    /// The installer does not answer for a person: it reports what is waiting, and
    /// the interface that started the installation asks.
    public static final class DshBuildScriptApprovalRequired extends DshException {
        /// The packages whose install scripts are waiting.
        private final transient List<String> packages;

        /// Creates the failure.
        ///
        /// @param packages the packages
        DshBuildScriptApprovalRequired(List<String> packages) {
            super("The plugin needs permission to run its install scripts: "
                    + String.join(", ", packages));
            this.packages = List.copyOf(packages);
        }

        /// Returns the packages whose install scripts are waiting.
        ///
        /// @return the package names
        public List<String> packages() {
            return packages;
        }
    }

    /// Turns pnpm's ignored-build-script failure into something actionable.
    ///
    /// A freshly created profile carries a placeholder in `pnpm-workspace.yaml`:
    ///
    /// ```yaml
    /// allowBuilds:
    ///   node-pty: set this to true or false
    /// ```
    ///
    /// Until that is resolved, pnpm refuses to run the package's build script
    /// and exits non-zero — after it has already added the package. The raw
    /// output is a wall of pnpm progress lines, so the launcher translates the
    /// condition instead of relaying it.
    ///
    /// The launcher does not resolve the placeholder itself: `allowBuilds`
    /// decides whether arbitrary post-install scripts may run, and that is the
    /// user's decision to make, not the launcher's.
    ///
    /// @param output  the captured command output
    /// @param home    the instance's `DSH_HOME`
    /// @param profile the profile name
    /// @return the explanation, or `null` when this is a different failure
    private static @Nullable String explainIgnoredBuilds(List<String> output, Path home, String profile) {
        String text = String.join("\n", output);
        if (!text.contains("ERR_PNPM_IGNORED_BUILDS") && !text.contains("approve-builds")) {
            return null;
        }

        String packages = "one or more dependencies";
        for (String line : output) {
            int marker = line.indexOf("Ignored build scripts:");
            if (marker >= 0) {
                packages = line.substring(marker + "Ignored build scripts:".length()).trim();
                break;
            }
        }

        Path profileDirectory = home.resolve("profiles").resolve(profile);
        return "The plugin was added, but its build script was not run.\n\n"
                + "pnpm ignored the build script for " + packages + " because the profile still carries the\n"
                + "template placeholder in its pnpm-workspace.yaml. Some features may not work until this\n"
                + "is resolved.\n\n"
                + "To resolve it, run in " + profileDirectory + ":\n"
                + "    pnpm approve-builds\n"
                + "or set allowBuilds for that package to true in\n"
                + "    " + profileDirectory.resolve("pnpm-workspace.yaml");
    }

    /// Reads the entries pnpm named for a package it will not build.
    ///
    /// A plugin fetched from a repository is built by its own `prepare` script, and pnpm refuses
    /// to run one until the profile says it may. For this it does **not** write a placeholder —
    /// the entry has to carry the address pnpm resolved the repository to — and prints the exact
    /// block it wants instead:
    ///
    /// ```yaml
    /// allowBuilds:
    ///   dshmarket@https://codeload.github.com/owner/name/tar.gz/<sha>: true
    /// ```
    ///
    /// Those entries are what this reads, so the launcher can ask about them the way it asks
    /// about every other build script.
    ///
    /// @param output the captured command output
    /// @return the entries, or an empty list when this is a different failure
    static List<String> gitBuildKeys(List<String> output) {
        if (!String.join("\n", output).contains("ERR_PNPM_GIT_DEP_PREPARE_NOT_ALLOWED")) {
            return List.of();
        }

        List<String> keys = new ArrayList<>();
        boolean inBlock = false;
        for (String line : output) {
            if (!inBlock) {
                inBlock = line.trim().equals("allowBuilds:");
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                // A blank line ends the block; one before the first entry is just spacing.
                if (!keys.isEmpty()) {
                    break;
                }
                continue;
            }
            int colon = trimmed.lastIndexOf(": ");
            if (colon <= 0) {
                break;
            }
            String value = trimmed.substring(colon + 2).trim();
            if (!value.equals("true") && !value.equals("false")) {
                break;
            }
            keys.add(trimmed.substring(0, colon).trim());
        }
        return List.copyOf(keys);
    }

    /// Explains a repository-hosted plugin that will not build.
    ///
    /// The raw output is the same wall of package-manager lines as the other build refusal, and
    /// the condition is worth naming: this plugin has no published build to install, so the
    /// build is not something that can be skipped.
    ///
    /// @param keys    the entries pnpm named
    /// @param home    the instance's `DSH_HOME`
    /// @param profile the profile name
    /// @return the explanation, or `null` when there is nothing to explain
    private static @Nullable String explainGitBuild(List<String> keys, Path home, String profile) {
        if (keys.isEmpty()) {
            return null;
        }
        Path workspace = home.resolve("profiles").resolve(profile).resolve("pnpm-workspace.yaml");
        StringBuilder entries = new StringBuilder("    allowBuilds:\n");
        for (String key : keys) {
            entries.append("      ").append(key).append(": true\n");
        }
        return "This plugin comes from a repository, and a repository-hosted plugin is built when it\n"
                + "is installed — there is no published build to fall back on, so the build has to be\n"
                + "allowed for the installation to finish.\n\n"
                + "pnpm asked for these entries in " + workspace + ":\n"
                + entries
                + "\nAllow this instance's build scripts (Settings → 构建脚本) and install again.";
    }

    /// Returns the last few output lines, for an error message.
    ///
    /// @param lines the captured output
    /// @return the trailing lines joined by newlines
    private static String tail(List<String> lines) {
        int from = Math.max(0, lines.size() - 12);
        return String.join("\n", lines.subList(from, lines.size()));
    }

    /// Forwards a progress message when a consumer is attached.
    ///
    /// @param onLine  the consumer, or `null`
    /// @param message the message
    private static void report(@Nullable Consumer<String> onLine, String message) {
        if (onLine != null) {
            onLine.accept(message);
        }
    }
}
