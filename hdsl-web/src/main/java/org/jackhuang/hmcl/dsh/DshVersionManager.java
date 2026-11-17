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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Installs, enumerates and removes DeepSeek Harness versions.
///
/// Every version lives in its own npm prefix under [DshPaths#VERSIONS]. The
/// manager never touches the user's global npm installation, so HDSL and a
/// hand-installed `dsh` cannot interfere with each other.
///
/// Discovery uses `npm view`, which upstream's own tooling relies on as well:
/// there is no version manager or release feed to query instead.
@NotNullByDefault
public final class DshVersionManager {
    private DshVersionManager() {
    }

    /// The npm package name of the DeepSeek Harness CLI.
    public static final String PACKAGE_NAME = "@deepseek-ai/dsh";

    /// The npm package that provides the application boot library.
    ///
    /// Held to the same version as DeepSeek Harness itself: the two are
    /// published together, and a mismatch between them is a failure at import
    /// rather than a degradation.
    public static final String APP_BOOT_PACKAGE = "@deepseek-ai/dsh-app-boot";

    /// The line that pins the boot library, as the file writes it.
    ///
    /// Anchored on the key rather than looked for anywhere in a line, and that is the whole point:
    /// the workspace file also *lists* packages, in entries spelled "- '@scope/name@1.2.3'", and a
    /// scan for the name alone reads the boot library out of one of those. What it answers with then
    /// still carries the "@" that separated the two halves of that list entry — and that value
    /// travels: an exported pack records it, and installing it writes a workspace file whose pin is
    /// not a version at all, which pnpm then refuses.
    private static final Pattern APP_BOOT_PIN = Pattern.compile(
            "^[ \\t]*['\"]?" + Pattern.quote(APP_BOOT_PACKAGE) + "['\"]?[ \\t]*:[ \\t]*(\\S+)[ \\t]*$",
            Pattern.MULTILINE);

    /// The characters YAML reserves at the start of a plain scalar.
    ///
    /// A scalar beginning with one of these is an indicator to the reader rather than text, so
    /// anything starting with one has to be quoted.
    private static final String RESERVED_FIRST = "-?:,[]{}#&*!|>'\"%@`";

    /// Reports whether an instance's own DeepSeek Harness is in place.
    ///
    /// @param instance the instance
    /// @return whether its entry script is there
    public static boolean isInstalled(DshInstance instance) {
        try {
            // The flag first. While `dsh.installing` stands, the tree below it is still
            // being written — an entry script already on disk is half of an install, not
            // an installed version, and an interrupted install must never read as one.
            if (Files.exists(installingFlag(instance))) {
                return false;
            }
            return Files.isRegularFile(instance.dshEntryPoint());
        } catch (DshException e) {
            return false;
        }
    }

    /// Whether an instance can be started: installed **and** its tree has no holes.
    ///
    /// `isInstalled` is the cheap question ("is there an entry script?"), and it is asked
    /// on every status poll, so it stays cheap. This one walks the package store and is
    /// asked where the answer decides something: before a launch (a tree an interrupted
    /// install left behind would start and then fail every session with
    /// `waiting for workingDirectory`), and before an install is called done.
    ///
    /// A holey tree is **repairable by reinstalling** — `pnpm install` alone answers
    /// "already up to date" and changes nothing, while the panel's install moves the tree
    /// aside and writes a fresh one — so the launch route uses this to decide "install
    /// first" instead of refusing.
    ///
    /// @param instance the instance to judge
    /// @return whether the tree looks complete
    public static boolean isUsable(DshInstance instance) {
        if (!isInstalled(instance)) {
            return false;
        }
        try {
            return emptyPackages(instance.dshDirectory()) == 0;
        } catch (DshException e) {
            return false;
        }
    }

    /// Queries the npm registry for every published DeepSeek Harness version.
    ///
    /// Dist-tags are fetched in a second call and merged in, so the caller can
    /// tell a stable release from an `alpha` or `rc` snapshot.
    ///
    /// @return the published releases, newest first
    /// @throws DshException when no usable npm is available or the query fails
    /// The package the web application depends on with its own version line.
    ///
    /// `@deepseek-ai/dsh-web-app@X` requires this package at `^X`, and it is
    /// published separately, so a harness release whose twin here was never
    /// published cannot be installed: the registry has nothing to satisfy the
    /// requirement, and pnpm stops with `ERR_PNPM_NO_MATCHING_VERSION`. That is why
    /// an instance can move to some published versions and not to others, and why
    /// the version list says which is which before somebody tries one.
    public static final String LOCKSTEP_PACKAGE = "@deepseek-ai/dsh-client-ui-sidebar-documentpreview";

    /// Returns the published versions of the package the harness is published with.
    ///
    /// @return the versions, or an empty set when the registry cannot be reached —
    ///         in which case nothing is marked, because not knowing is not the same
    ///         as knowing something is missing
    /// Reports whether a harness version predates the lockstep package entirely.
    ///
    /// The package was introduced partway through the harness's history, so a release
    /// older than it cannot depend on it: a version without a twin there is not
    /// missing anything, and marking it was wrong — an old release installs perfectly
    /// well, as 0.0.1 did.
    ///
    /// @param version the harness version
    /// @param oldest  the oldest published version of the lockstep package, or `null`
    /// @return whether the harness version is older than the package
    public static boolean predatesLockstep(String version, @Nullable String oldest) {
        return oldest != null && compareVersions(version, oldest) < 0;
    }

    public static Set<String> lockstepVersions() {
        return lockstepVersionsAndOldest().versions();
    }

    /// The published versions of the lockstep package, and the oldest of them.
    ///
    /// The oldest matters because the package was introduced partway through the
    /// harness's history: a harness release older than it cannot depend on it, so a
    /// version without a twin there is not missing anything. Marking those was wrong
    /// — an old release installs perfectly well — and this is what tells them apart.
    ///
    /// @param versions the published versions
    /// @param oldest   the oldest published version, or `null`
    public record Lockstep(Set<String> versions, @Nullable String oldest) {
    }

    /// Reads the lockstep package's versions and its oldest one.
    ///
    /// @return what the registry holds, or nothing when it cannot be reached
    public static Lockstep lockstepVersionsAndOldest() {
        try {
            DshNodeRuntime runtime = requireRuntime();
            if (!runtime.canInstall()) {
                return new Lockstep(Set.of(), null);
            }
            java.util.Collection<String> versions = queryVersions(runtime.npm(), LOCKSTEP_PACKAGE);
            String oldest = null;
            for (String version : versions) {
                if (oldest == null || compareVersions(version, oldest) < 0) {
                    oldest = version;
                }
            }
            return new Lockstep(Set.copyOf(versions), oldest);
        } catch (DshException | RuntimeException e) {
            LOG.warning("Could not read the versions of " + LOCKSTEP_PACKAGE, e);
            return new Lockstep(Set.of(), null);
        }
    }

    public static List<DshRelease> fetchReleases() throws DshException {
        DshNodeRuntime runtime = requireRuntime();
        if (!runtime.canInstall()) {
            throw new DshException("npm was not found on PATH; reading the version list requires it");
        }

        Set<String> versions = queryVersions(runtime.npm());
        JsonObject distTags = queryDistTags(runtime.npm());
        JsonObject times = queryTimes(runtime.npm());

        List<DshRelease> releases = new ArrayList<>(versions.size());
        for (String version : versions) {
            Set<String> tags = new LinkedHashSet<>();
            for (var entry : distTags.entrySet()) {
                JsonElement value = entry.getValue();
                if (value != null && value.isJsonPrimitive() && version.equals(value.getAsString())) {
                    tags.add(entry.getKey());
                }
            }
            JsonElement published = times.get(version);
            releases.add(new DshRelease(version, Set.copyOf(tags),
                    published != null && published.isJsonPrimitive() ? published.getAsString() : null));
        }

        releases.sort(Comparator.comparing(DshRelease::version, DshVersionManager::compareVersions).reversed());
        return releases;
    }

    /// Whether DeepSeek Harness can run on a platform.
    ///
    /// FreeBSD is refused because of an upstream native dependency, not because of
    /// anything in this class: `node-addon-require-builtin` ships no FreeBSD binary,
    /// its source has no FreeBSD context reader, and `dsh-app-boot` calls it while
    /// installing the profile resolver.
    ///
    /// @param os the platform the panel runs on
    /// @return `false` on FreeBSD, `true` everywhere else
    static boolean installableOn(org.jackhuang.hmcl.util.platform.OperatingSystem os) {
        return os != org.jackhuang.hmcl.util.platform.OperatingSystem.FREEBSD;
    }

    /// Writes the manifest npm installs from.
    ///
    /// The launcher states its dependencies rather than letting npm pick them,
    /// so a version is reproducible and the libraries stay with the launcher
    /// that expects them.
    ///
    /// @param prefix      the directory the version is installed into
    /// @param version     the DeepSeek Harness version
    /// @param appBoot     the application boot library version to hold it to
    /// @throws DshException when the manifest cannot be written
    static void writeManifest(Path prefix, String version, String appBoot,
                              @Nullable DshNodeRuntime runtime, DshDependencyPolicy policy)
            throws DshException {
        JsonObject dependencies = new JsonObject();
        dependencies.addProperty(PACKAGE_NAME, version);

        // One map, and both files are written from it. Two computations — one per file — is how the
        // same key came to be written twice: the boot library is put in explicitly *and* it is one of
        // the vendor's packages, so the policy named it again and pnpm refused the file with
        // `duplicated mapping key`. Building the answer once makes that unrepresentable.
        Map<String, String> overrides = mergeOverrides(appBoot,
                heldDependencies(runtime, version, appBoot, policy));

        JsonObject manifest = new JsonObject();
        manifest.addProperty("private", true);
        manifest.add("dependencies", dependencies);

        // The override goes in a file of its own, not in this one. npm reads an
        // `overrides` field here and pnpm does not: since pnpm 10 the settings it
        // used to take from `package.json` live in `pnpm-workspace.yaml`, and a
        // `pnpm` field here is ignored with a warning. Writing only npm's spelling
        // left the pinning silently not applied, and a runtime came out with the
        // launcher at one version and its boot library at another — a pairing that
        // fails at import rather than degrading. The tests cover the pair.
        StringBuilder workspace = new StringBuilder("overrides:\n");
        overrides.forEach((name, held) -> workspace.append("  ").append(yamlScalar(name)).append(": ")
                .append(yamlScalar(held)).append('\n'));

        try {
            Files.createDirectories(prefix);
            var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Files.writeString(prefix.resolve("package.json"), gson.toJson(manifest));
            Files.writeString(prefix.resolve("pnpm-workspace.yaml"), workspace.toString());
            // `pnpm install` runs as the agent in this prefix, and a manifest and a workspace file
            // it cannot rewrite are the same failure as a root-owned profile: the tool that owns
            // its own configuration cannot save it. See `Privileges#handWrittenFile`.
            Privileges.handWrittenFile(prefix.resolve("package.json"), DshPaths.ROOT);
            Privileges.handWrittenFile(prefix.resolve("pnpm-workspace.yaml"), DshPaths.ROOT);
        } catch (IOException e) {
            throw new DshException("Failed to write the manifest for " + version, e);
        }
    }

    /// Combines the boot library's pin with the ones the policy holds.
    ///
    /// The boot library wins where the two name the same package, and that is not a tie-break but the
    /// point: it is the one the **caller** chose — the create page offers it — while the policy only
    /// knows what the harness's ranges say. Keeping them in one map is also what stops the same key
    /// being written twice, which is a file pnpm refuses outright.
    ///
    /// @param appBoot the boot library version the caller asked for
    /// @param held    the versions the policy holds
    /// @return the overrides, boot library first
    static Map<String, String> mergeOverrides(String appBoot, Map<String, String> held) {
        Map<String, String> overrides = new LinkedHashMap<>();
        overrides.put(APP_BOOT_PACKAGE, appBoot);
        held.forEach(overrides::putIfAbsent);
        return overrides;
    }

    /// Returns a value written so that YAML reads it back as the string it is.
    ///
    /// The file is built by hand rather than through an emitter, so a value that is not a plain
    /// scalar has to be quoted here or the file stops being YAML. Both halves of an override can be
    /// such a value: a package name starts with `@`, which YAML reserves, and a pin can be a range
    /// or a protocol as readily as a version. One this cannot write plainly is single-quoted, which
    /// is where a quote of its own is doubled — the one escape that style has.
    ///
    /// @param value the value
    /// @return it as a YAML scalar
    private static String yamlScalar(String value) {
        boolean plain = !value.isEmpty()
                && RESERVED_FIRST.indexOf(value.charAt(0)) < 0
                && value.equals(value.trim())
                && !value.contains(": ")
                && !value.contains(" #")
                && value.indexOf('\n') < 0;
        return plain ? value : "'" + value.replace("'", "''") + "'";
    }

    /// Returns the dependencies to hold to the versions the harness declares.
    ///
    /// The declared value is a range — `^4.0.2` — and its **floor** is the version the author built
    /// against: a caret asks for that version or anything newer that claims compatibility, and
    /// "anything newer" is what has twice broken a launch. So the floor is read out and written as an
    /// override, which is not a guess about what works but a statement of what the package itself
    /// says it was written for.
    ///
    /// Read from the **published manifests** rather than from disk: this runs before `pnpm install`,
    /// when the harness's own `package.json` is not on the machine yet. Both packages that take part
    /// in the pairing are asked — the harness, and the boot library it is held to — because each
    /// declares part of the tree and the boot library is where the plugin framework is named.
    ///
    /// **A failure here must not fail the install.** A registry that cannot be reached, or a range
    /// this does not recognise, leaves that dependency to resolve freely — which is exactly the
    /// behaviour of [DshDependencyPolicy#LATEST] and better than refusing to install at all.
    ///
    /// @param runtime the runtime whose npm reads the registry
    /// @param version the harness version
    /// @param appBoot the boot library version
    /// @param policy  how much of the tree to hold
    /// @return the name-to-version overrides
    private static Map<String, String> heldDependencies(@Nullable DshNodeRuntime runtime, String version,
                                                        String appBoot, DshDependencyPolicy policy) {
        Map<String, String> held = new LinkedHashMap<>();
        // No runtime means no npm, which means no registry to read the declarations from. Nothing is
        // held, which is the same answer as `LATEST` and better than refusing to install.
        if (policy == DshDependencyPolicy.LATEST || runtime == null) {
            return held;
        }
        for (String asked : List.of(PACKAGE_NAME + "@" + version, APP_BOOT_PACKAGE + "@" + appBoot)) {
            try {
                JsonObject declared = declaredDependencies(
                        runNpmView(runtime.npm(), asked, "dependencies").text());
                if (declared == null) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> entry : declared.entrySet()) {
                    String name = entry.getKey();
                    // First answer wins: the harness's own declaration is the one that matters, and
                    // the boot library is asked second only for what the harness did not name.
                    if (!policy.pins(name) || held.containsKey(name)
                            || !entry.getValue().isJsonPrimitive()) {
                        continue;
                    }
                    String floor = floorOf(entry.getValue().getAsString());
                    if (floor != null) {
                        held.put(name, floor);
                    }
                }
            } catch (DshException | RuntimeException e) {
                LOG.warning("Could not read what " + asked + " declares; leaving its dependencies "
                        + "to resolve freely", e);
            }
        }
        return held;
    }

    /// Reads a `dependencies` object out of what npm answered.
    ///
    /// npm answers a question about a **version specifier** with an array — one entry per version it
    /// matched — even when the specifier names exactly one, so `npm view pkg@1.2.3 dependencies
    /// --json` gives `[{…}]` rather than `{…}`. Reading only the object shape therefore found
    /// nothing, silently: the policy would have been offered, selected, saved, and applied to
    /// nothing at all. Hence a method of its own with the real shape written down, and a test that
    /// feeds it the real answer.
    ///
    /// A primitive is npm's answer for a package that declares no dependencies at all.
    ///
    /// @param json npm's answer
    /// @return the dependencies, or `null` when there are none to read
    static @Nullable JsonObject declaredDependencies(@Nullable String json) {
        JsonElement parsed = parseJson(json);
        if (parsed == null || parsed.isJsonNull()) {
            return null;
        }
        if (parsed.isJsonObject()) {
            return parsed.getAsJsonObject();
        }
        if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            }
        }
        return null;
    }

    /// Reads the floor out of a declared version range.
    ///
    /// Only the shapes that have a floor are answered: `4.0.2`, `^4.0.2`, `~4.0.2` and `>=4.0.2`.
    /// Anything else — `*`, `latest`, a tag, a git address, a compound range — is answered with
    /// `null` and left alone, because a range whose floor this cannot name is one it must not guess
    /// at.
    ///
    /// @param range the declared range
    /// @return the version, or `null`
    static @Nullable String floorOf(@Nullable String range) {
        String text = range == null ? "" : range.trim();
        for (String prefix : List.of("^", "~", ">=")) {
            if (text.startsWith(prefix)) {
                text = text.substring(prefix.length()).trim();
                break;
            }
        }
        return text.matches("\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z.-]+)?") ? text : null;
    }

    /// The flag that says an instance's copy is being written.
    ///
    /// A directory rather than a file, because that is what it always was; its presence
    /// is the whole of its meaning ([isInstalled] refuses while it stands).
    ///
    /// @param instance the instance
    /// @return the flag directory
    /// @throws DshException when the identifier is not usable as a path segment
    private static Path installingFlag(DshInstance instance) throws DshException {
        return instance.instanceDirectory().resolve("dsh.installing");
    }

    /// Where the working install is kept while a new one is written.
    ///
    /// Same directory as the install itself, so keeping and restoring it are renames
    /// rather than copies of a `node_modules` tree. The name is not reached by a launch:
    /// only [install], [discardPartial] and the rollback paths know it.
    ///
    /// @param instance the instance
    /// @return the kept-aside directory
    /// @throws DshException when the identifier is not usable as a path segment
    private static Path previousDirectory(DshInstance instance) throws DshException {
        return instance.instanceDirectory().resolve("dsh.previous");
    }

    /// Removes a copy that was being installed and did not finish.
    ///
    /// Both halves: the flag, and the tree that was being written under it. The tree is
    /// removed as well because it now lives at the path the instance would use — a
    /// partial one left there is `node_modules` a launch would read.
    ///
    /// The version kept aside by an interrupted install is put back: that is the version
    /// the instance was working with before the attempt started.
    ///
    /// @param instance the instance whose partial install to remove
    public static void discardPartial(DshInstance instance) {
        try {
            Path flag = installingFlag(instance);
            Path previous = previousDirectory(instance);
            Path target = instance.dshDirectory();
            if (Files.exists(flag)) {
                FileUtils.deleteQuietly(target);
                FileUtils.deleteQuietly(flag);
                restorePrevious(previous, target);
                LOG.info("Removed the partial install for " + instance.id());
            } else if (Files.exists(previous)) {
                // No flag: the install published, and the backup is a leftover.
                FileUtils.deleteQuietly(previous);
            }
        } catch (DshException e) {
            LOG.warning("Could not remove the partial install for " + instance.id(), e);
        }
    }

    /// Clears the remains of an install that never finished, before a new one starts.
    ///
    /// Three states are possible, and each has exactly one right answer:
    ///
    /// - the flag stands → the tree at `target` is partial. Remove it and the flag, and
    ///   put back the version that was kept aside;
    /// - no flag, but a kept-aside version → the install published and was interrupted
    ///   before the cleanup. The tree at `target` is the good one; the backup goes;
    /// - neither → nothing to do.
    ///
    /// @param target   the instance's install directory
    /// @param flag     the flag
    /// @param previous the version kept aside for a rollback
    static void recoverInterruptedInstall(String instanceId, Path target,
                                          Path flag, Path previous) {
        if (Files.exists(flag)) {
            LOG.info("Cleaning up the interrupted install for " + instanceId);
            FileUtils.deleteQuietly(target);
            FileUtils.deleteQuietly(flag);
            restorePrevious(previous, target);
            return;
        }
        if (Files.exists(previous)) {
            FileUtils.deleteQuietly(previous);
        }
    }

    /// Moves the working install aside so a failed one can be rolled back.
    ///
    /// A rename within the instance directory — same filesystem, and no copy of a
    /// `node_modules` tree. It is deliberately not `ATOMIC_MOVE`: a half-done rename is
    /// not a state that can exist here (the source and destination are the same
    /// directory), and `ATOMIC_MOVE` is not supported everywhere.
    ///
    /// @param target   the install directory
    /// @param previous where it is kept while the new tree is written
    /// @throws IOException when the move fails — the caller still has both trees intact
    static void moveAside(Path target, Path previous) throws IOException {
        FileUtils.deleteQuietly(previous);
        if (Files.exists(previous)) {
            // The old backup could not be removed (a file still held, a link the walker could
            // not pass). Moving onto it would fail the whole install with "Failed to prepare",
            // which is what an instance could never get past. Take a fresh name instead; the
            // leftover is swept with the tombstones.
            Path unique = previous.resolveSibling(previous.getFileName() + ".deleting-" + System.currentTimeMillis());
            Files.move(previous, unique);
            LOG.warning("Could not remove " + previous + "; moved it to " + unique.getFileName());
        }
        Files.move(target, previous);
    }

    /// Puts the version kept aside back on the path the instance uses.
    ///
    /// The path matters: pnpm's Windows junctions hold absolute targets, so a tree only
    /// works again once it is back at the exact path it was installed into. Best-effort —
    /// when it fails the tree is still there, and the warning says where.
    ///
    /// @param previous the kept-aside version, or a path that does not exist
    /// @param target   the install directory
    static void restorePrevious(Path previous, Path target) {
        if (!Files.exists(previous)) {
            return;
        }
        try {
            FileUtils.deleteQuietly(target);
            Files.move(previous, target);
            LOG.info("Restored the previous install at " + target);
        } catch (IOException e) {
            LOG.warning("Could not restore the previous install to " + target
                    + "; it is kept at " + previous, e);
        }
    }

    /// Holds an instance's boot library to a different release.
    ///
    /// The pairing is not a preference — the two are published together, and a
    /// release whose libraries disagree with it fails at import rather than
    /// degrading — so this is written only when someone asked for it, and the
    /// caller warns first.
    ///
    /// The choice belongs to the instance, because the runtime it changes belongs
    /// to the instance: it is installed into the instance's own copy and reaches
    /// nothing else.
    ///
    /// @param instance the instance to change
    /// @param appBoot  the boot library version to hold it to
    /// @throws DshException when the manifest cannot be written or pnpm fails
    public static void overrideAppBoot(DshInstance instance, String appBoot) throws DshException {
        overrideAppBoot(instance, appBoot, null);
    }

    /// Holds an instance's copy to a boot library version, reporting the install.
    ///
    /// @param instance the instance to change
    /// @param appBoot  the boot library version to hold it to
    /// @param onLine   receives the package manager's output, or `null`
    /// @throws DshException when the manifest cannot be written or pnpm fails
    public static void overrideAppBoot(DshInstance instance, String appBoot,
                                       @Nullable Consumer<String> onLine) throws DshException {
        Path target = instance.dshDirectory();
        if (!Files.isDirectory(target)) {
            throw new DshException("Instance " + instance.id() + " has no DeepSeek Harness to change");
        }

        DshNodeRuntime runtime = requireRuntimeWithPnpm();
        requireSupportedNode(runtime);
        if (!runtime.canManagePlugins()) {
            throw runtime.pnpmMissing("changing the boot library");
        }

        writeManifest(target, instance.version(), appBoot, runtime,
                org.jackhuang.hmcl.setting.SettingsManager.settings().dependencyPolicy());

        List<String> command = buildInstallCommand(runtime, target);

        LOG.info("Holding " + instance.id() + " to boot library " + appBoot);
        int exitCode;
        DshCommand.Result commandResult;
        try {
            commandResult = DshCommand.run(command, null, runtime.pathEnvironment(), onLine);
            exitCode = commandResult.exitCode();
        } catch (IOException e) {
            throw new DshException("Failed to run pnpm", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("The boot library change was interrupted", e);
        }
        if (exitCode != 0) {
            throw new DshException("pnpm exited with code " + exitCode
                    + " while changing the boot library of " + instance.id()
                    + ":\n" + tail(commandResult.output()));
        }
    }

    /// Returns the boot library version the workspace file pins, or null when it pins none.
    ///
    /// The seam the reader is written against, so the parse can be tested on a file's own text
    /// rather than through an instance that has to exist first.
    ///
    /// @param workspace the file's text
    /// @return the pinned version, or null
    static @Nullable String appBootPin(String workspace) {
        Matcher pin = APP_BOOT_PIN.matcher(workspace);
        if (!pin.find()) {
            return null;
        }
        String version = pin.group(1).trim();
        if (version.length() > 1
                && (version.charAt(0) == '\'' || version.charAt(0) == '"')
                && version.charAt(version.length() - 1) == version.charAt(0)) {
            version = version.substring(1, version.length() - 1);
        }
        return version.isEmpty() ? null : version;
    }

    /// Returns the boot library version an instance is held to.
    ///
    /// The pin is read back from where it was written, because that is the
    /// instance's own record of the choice: asking pnpm which copy it linked
    /// would mean reproducing its store layout, and the version that matters to
    /// the page is the one the instance asks for.
    ///
    /// @param instance the instance
    /// @return the version, or `null` when the instance has no pin to read
    public static @Nullable String readAppBoot(DshInstance instance) {
        Path prefix;
        try {
            prefix = instance.dshDirectory();
        } catch (DshException e) {
            return null;
        }

        Path workspace = prefix.resolve("pnpm-workspace.yaml");
        if (Files.isRegularFile(workspace)) {
            try {
                String pinned = appBootPin(Files.readString(workspace));
                if (pinned != null) {
                    return pinned;
                }
            } catch (IOException e) {
                LOG.warning("Failed to read the boot library pin of " + instance.id(), e);
            }
        }

        Path manifest = prefix.resolve("package.json");
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
            JsonElement overrides = root.get("overrides");
            if (overrides == null || !overrides.isJsonObject()) {
                return null;
            }
            JsonElement version = overrides.getAsJsonObject().get(APP_BOOT_PACKAGE);
            return version == null || !version.isJsonPrimitive() ? null : version.getAsString();
        } catch (IOException | RuntimeException e) {
            LOG.warning("Failed to read the boot library pin of " + instance.id(), e);
            return null;
        }
    }

    /// Builds the command that installs a project's dependencies with pnpm.
    ///
    /// Three decisions are in here.
    ///
    /// pnpm rather than npm, because pnpm links packages into a project from a
    /// store it keeps: a second project using the same packages costs a fraction
    /// of the first. Measured here, a second copy of the same version adds 51 MB
    /// against the 402 MB it appears to occupy.
    ///
    /// `append-only` because the progress tally the launcher reads is one line of
    /// the event-by-event output.
    ///
    /// And the build scripts are allowed. pnpm runs none of them by default and
    /// fails the install when it finds any, and several of DeepSeek Harness's
    /// dependencies build native modules — the spawn helper among them — so an
    /// install without them looks complete and fails the moment it is used. This
    /// is the same trust npm extends by default, which is what the upstream
    /// package is installed with.
    ///
    /// @param runtime the runtime to take pnpm from
    /// @param project the project directory
    /// @return the command
    private static List<String> buildInstallCommand(DshNodeRuntime runtime, Path project) {
        return List.of(
                runtime.pnpm().toString(),
                "install",
                "--dir", project.toString(),
                "--reporter=append-only",
                // Said out loud as well as written into pnpm's configuration: this is the install
                // that takes minutes when the source is wrong, and the argument is the one line of
                // the child's log that says which source it actually used.
                "--registry=" + NpmRegistry.effective().registry(),
                "--config.dangerously-allow-all-builds=true");
    }

    /// Installs the DeepSeek Harness an instance runs, into that instance.
    ///
    /// The tree is written where the instance will use it, and `dsh.installing` stands
    /// beside it while that happens: a flag that says the install is not finished, so an
    /// interrupted one can never look like a usable runtime.
    ///
    /// It used to be staged in `dsh.installing` and *moved* into place on success. That
    /// is the cleaner shape and it is what this did, but it cannot survive Windows: pnpm
    /// links `node_modules` with symlinks on POSIX — relative ones, so the whole tree can
    /// be moved — and with junctions on Windows, whose targets must be **absolute**. A
    /// staged tree therefore arrived with every link naming the directory the move had
    /// just deleted, pnpm having exited zero and the packages all being present. The
    /// install finished and the manifest could not be read back, which is what the panel
    /// reported: "pnpm reported success, but the installed version could not be read back".
    ///
    /// @param instance the instance to install for
    /// @param onLine   a consumer notified of pnpm output lines, or `null`
    /// @throws DshException when the runtime is missing or pnpm fails
    public static void install(DshInstance instance, @Nullable Consumer<String> onLine) throws DshException {
        install(instance, instance.version(), onLine);
    }

    /// Installs a version into an instance, which need not be the version it
    /// currently records.
    ///
    /// The two differ while an instance is being moved to another version: the
    /// new runtime has to be in place before the record can name it.
    ///
    /// @param instance the instance to install into
    /// @param version  the version to install
    /// @param onLine   a consumer notified of pnpm output lines, or `null`
    /// @throws DshException when the runtime is missing or pnpm fails
    public static void install(DshInstance instance, String version,
                               @Nullable Consumer<String> onLine) throws DshException {
        Path target = instance.dshDirectory();
        Path flag = installingFlag(instance);
        Path previous = previousDirectory(instance);

        // FreeBSD: dsh 0.2.x cannot boot there. Its `node-addon-require-builtin`
        // native addon has no FreeBSD build, the addon's own platform reader
        // answers "unsupported" for FreeBSD, and dsh-app-boot calls it
        // unconditionally while installing the profile resolver — the process
        // dies before the panel can talk to it. Failing here saves the download
        // and a dependency tree that can never run.
        if (!installableOn(org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS)) {
            throw new DshException("DeepSeek Harness cannot run on FreeBSD: its "
                    + "node-addon-require-builtin native addon has no FreeBSD build and "
                    + "dsh-app-boot requires it at startup. Use a brand instance or "
                    + "another host instead.");
        }

        DshNodeRuntime runtime = requireRuntimeWithPnpm();
        requireSupportedNode(runtime);
        if (!runtime.canManagePlugins()) {
            throw runtime.pnpmMissing("installing DeepSeek Harness");
        }
        requireSupportedPnpm(runtime);

        try {
            recoverInterruptedInstall(instance.id(), target, flag, previous);

            // The working install is **moved aside**, not deleted. An install that fails
            // must leave the instance with the version it had — that guarantee used to
            // come from staging the tree elsewhere and moving it in on success, which
            // cannot work here: on Windows pnpm links packages with junctions whose
            // targets are absolute, so a tree written under another name arrives with
            // every link naming a directory that no longer exists. Installing into the
            // final path fixes that; keeping the old tree beside it fixes the failure
            // case. Moving it back is a rename within the instance directory, and it
            // puts the absolute junctions on the path they name again.
            if (Files.exists(target)) {
                moveAside(target, previous);
            }
            Files.createDirectories(target);
            // `pnpm install` writes here as the agent; the panel is the one that just made the
            // directory, so it is the panel's to hand over (same rule as an instance home).
            Privileges.prepareAgentDirectory(target, Privileges.AGENT_SHARED_DIRECTORY);
            // The flag, raised for as long as the tree is being written. Its presence is
            // what [isInstalled] refuses to overlook — the job the staging directory used
            // to do by holding the tree away from the instance.
            Files.createDirectories(flag);
        } catch (IOException e) {
            restorePrevious(previous, target);
            FileUtils.deleteQuietly(flag);
            throw new DshException("Failed to prepare " + target, e);
        }

        // The manifest is written before pnpm runs, because pnpm would otherwise
        // choose the versions itself. Installing by name records a caret range,
        // and a caret cannot express what these packages actually promise: the
        // whole family is published in lockstep, one version for all of it. So
        // `^0.1.6-alpha.1` resolves to alpha.2's libraries, and a release whose
        // code and libraries disagree fails at import — which is exactly what
        // happened between 0.1.6-alpha.1 and alpha.2, where a library dropped an
        // export the launcher still imported.
        //
        // Pinning the launcher to its exact version and holding the application
        // boot library to the same one keeps a tree consistent. The override is
        // the value the create page offers, and it defaults to the matching one.
        writeManifest(target, version, version, runtime,
                org.jackhuang.hmcl.setting.SettingsManager.settings().dependencyPolicy());

        List<String> command = buildInstallCommand(runtime, target);

        LOG.info("Installing DSH " + version + " for " + instance.id() + ": " + String.join(" ", command));

        int exitCode;
        DshCommand.Result commandResult;
        try {
            commandResult = DshCommand.run(command, null, runtime.pathEnvironment(), onLine);
            exitCode = commandResult.exitCode();
        } catch (IOException e) {
            throw new DshException("Failed to run pnpm", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("Install was interrupted", e);
        }

        if (exitCode != 0) {
            // Nothing half-written is left to be mistaken for an install, and the flag
            // goes with it. The version that was working before this attempt comes back:
            // a failed install is not a reason to leave the instance without a runtime.
            FileUtils.deleteQuietly(target);
            FileUtils.deleteQuietly(flag);
            restorePrevious(previous, target);
            // pnpm's own last lines say what went wrong — a version that is not
            // published, a dependency tree that cannot be resolved, a registry that
            // cannot be reached — and an exit code says none of that.
            throw new DshException("pnpm exited with code " + exitCode
                    + " while installing " + version + " for " + instance.id()
                    + ":\n" + tail(commandResult.output()));
        }

        Path bin = target.resolve(DshVersion.PACKAGE_PATH).resolve("lib/bin.js");
        if (!Files.isRegularFile(bin)) {
            FileUtils.deleteQuietly(target);
            FileUtils.deleteQuietly(flag);
            restorePrevious(previous, target);
            throw new DshException("pnpm reported success but " + bin + " is missing");
        }
        // `bin.js` is one file; the store behind it can still have holes. An install that
        // was interrupted (the panel stopped, the disk filled) leaves empty package
        // directories, and the harness then fails at import time with a cascade that
        // names nothing. Refuse the tree here, where the words can say what to do.
        int empty = emptyPackages(target);
        if (empty > 0) {
            FileUtils.deleteQuietly(target);
            FileUtils.deleteQuietly(flag);
            restorePrevious(previous, target);
            throw new DshException("pnpm reported success but " + empty
                    + " package directories under " + target.resolve("node_modules/.pnpm")
                    + " are empty, so the install did not finish. Install this version again"
                    + " and let it finish — an instance started from this tree would fail with"
                    + " 'waiting for workingDirectory'.");
        }

        // The tree is already where the instance will use it — the flag comes down, and
        // the version kept aside for a rollback is no longer needed.
        FileUtils.deleteQuietly(flag);
        FileUtils.deleteQuietly(previous);

        LOG.info("Installed DSH " + version + " for " + instance.id() + " into " + target);
    }

    /// Counts the package directories of a pnpm store that hold nothing at all.
    ///
    /// "The entry exists" says nothing about its contents. An install interrupted while
    /// the store was being written leaves `.pnpm/<id>/node_modules/<name>` as an **empty
    /// directory**, and the tree still looks installed — the launcher script is there,
    /// the manifest is there — until the harness starts and a plugin cannot import its
    /// dependency. The visible symptom names none of it: a dsh instance that is half
    /// installed answers every session request with a cascade of
    /// `waiting for workingDirectory`, because the plugin that provides that service is
    /// the one whose dependency is missing.
    ///
    /// The question is "is this directory empty", not "is there a `package.json` in it". A
    /// pnpm store is a web of symlinks, and a **dangling** one (a directory renamed, moved or
    /// reinstalled over) resolves to nothing: asking for `package.json` through it says "no",
    /// which would make a complete tree look like a hole and get it deleted right after a
    /// successful install. An empty directory says "no" honestly, and one that cannot be read
    /// at all says nothing, so it is not counted — an unreadable tree is not evidence.
    ///
    /// `bin.js` is not enough of a check on its own (it is a single file, written before
    /// the store's links settle), so this walks the store and counts the holes.
    ///
    /// @param dshDirectory the instance's `dsh/` directory
    /// @return how many package directories are empty; `0` when the tree looks complete
    ///         or cannot be inspected at all (an unreadable tree is not evidence)
    public static int emptyPackages(Path dshDirectory) {
        Path store = dshDirectory.resolve("node_modules").resolve(".pnpm");
        if (!Files.isDirectory(store)) {
            return 0;
        }
        int empty = 0;
        try (java.util.stream.Stream<Path> entries = Files.list(store)) {
            for (Path entry : entries.toList()) {
                Path nested = entry.resolve("node_modules");
                if (!Files.isDirectory(nested)) {
                    continue;
                }
                try (java.util.stream.Stream<Path> packages = Files.list(nested)) {
                    for (Path candidate : packages.toList()) {
                        String name = candidate.getFileName().toString();
                        if (name.startsWith("@")) {
                            // A scoped package: `.pnpm/<id>/node_modules/@scope/name`.
                            try (java.util.stream.Stream<Path> scoped = Files.list(candidate)) {
                                for (Path packageDir : scoped.toList()) {
                                    if (isEmptyDirectory(packageDir)) {
                                        empty++;
                                    }
                                }
                            }
                        } else if (isEmptyDirectory(candidate)) {
                            empty++;
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.warning("Could not inspect the package store under " + store, e);
            return 0;
        }
        return empty;
    }

    /// Whether a package directory holds no entries at all.
    ///
    /// A directory that cannot be listed — a dangling symlink, most of all — is **not** empty:
    /// whatever is on the other side of a broken link is dsh's problem to report, and calling it
    /// a hole here would have the caller delete a tree that may be perfectly good.
    ///
    /// @param directory the candidate package directory
    /// @return whether it lists no entries
    private static boolean isEmptyDirectory(Path directory) {
        try (java.util.stream.Stream<Path> entries = Files.list(directory)) {
            return entries.findAny().isEmpty();
        } catch (IOException | RuntimeException e) {
            // A dangling link, a directory nobody may read, or not a directory at all.
            return false;
        }
    }

    /// Requires a Node runtime that can talk to the npm registry.
    ///
    /// Every caller of this one runs `npm`: it reads version lists, dist-tags and
    /// registry metadata. So a `PATH` Node without npm gives way to the panel's own
    /// runtime (设置 → Node.js 一键安装，落在 `<data>/hdsl/runtimes`) — otherwise a host
    /// whose `nodejs` package shipped without npm reports "npm was not found on PATH"
    /// while a working toolchain sits in the data directory unused.
    ///
    /// @return the detected runtime
    /// @throws DshException when no `node` was found at all
    public static DshNodeRuntime requireRuntime() throws DshException {
        return DshNodeRuntime.detectForNpm().orElseThrow(DshVersionManager::noRuntime);
    }

    /// Requires a Node runtime that carries `pnpm`, for the work that cannot run without it.
    ///
    /// Node alone is not enough here: installing DeepSeek Harness, changing an
    /// instance's boot library and touching plugins all go through `pnpm`, and a
    /// machine whose `PATH` has a Node.js without pnpm reports "pnpm was not found
    /// on PATH" even when the panel's own managed runtime — the one that is
    /// installed *with* pnpm — is sitting in the data directory. [DshNodeRuntime#detectWithPnpm]
    /// is the order that avoids that, and [requirePnpm] is the failure the caller
    /// reports when nothing qualifies.
    ///
    /// @return a runtime with pnpm
    /// @throws DshException when there is no Node.js at all
    public static DshNodeRuntime requireRuntimeWithPnpm() throws DshException {
        return DshNodeRuntime.detectWithPnpm().orElseThrow(DshVersionManager::noRuntime);
    }

    /// The refusal for a machine with no usable Node.js at all.
    ///
    /// @return the exception to throw
    private static DshException noRuntime() {
        return new DshException("Node.js was not found on PATH and no panel-managed runtime "
                + "is installed (" + DshNodeRuntime.requirement() + "). "
                + "Open Settings -> Node.js for a one-click managed runtime, or install Node.js yourself.");
    }

    /// Reads the published version list from the registry.
    ///
    /// @param npm the npm executable
    /// @return the published version strings
    /// @throws DshException when the query fails or returns unexpected data
    private static Set<String> queryVersions(Path npm) throws DshException {
        return queryVersions(npm, PACKAGE_NAME);
    }

    /// Reads a package's published version list from the registry.
    ///
    /// @param npm     the npm executable
    /// @param pkg     the package to read
    /// @return the published version strings, newest first
    /// @throws DshException when the query fails or returns unexpected data
    public static List<String> fetchPackageVersions(String pkg) throws DshException {
        return fetchPackageVersions(requireRuntime(), pkg);
    }

    /// Reads the published version list of one package, using a runtime the caller
    /// already resolved.
    ///
    /// An instance that runs on a launcher-managed Node has a runtime of its own, and
    /// asking whether the machine happens to have one on its path is asking the wrong
    /// question: the version list of a plugin is read with the runtime the instance
    /// would install it with.
    ///
    /// @param runtime the runtime to read with
    /// @param pkg     the package name
    /// @return the versions, newest first
    /// @throws DshException when npm is missing or the registry cannot be reached
    public static List<String> fetchPackageVersions(DshNodeRuntime runtime, String pkg) throws DshException {
        if (!runtime.canInstall()) {
            throw new DshException("npm was not found for this instance's Node runtime;"
                    + " reading the registry requires it");
        }
        List<String> versions = new ArrayList<>(queryVersions(runtime.npm(), pkg));
        versions.sort((left, right) -> compareVersions(right, left));
        return List.copyOf(versions);
    }

    /// Returns the last few output lines, for an error message.
    ///
    /// @param lines the captured output
    /// @return the trailing lines joined by newlines
    private static String tail(List<String> lines) {
        int from = Math.max(0, lines.size() - 12);
        return String.join("\n", lines.subList(from, lines.size()));
    }

    /// Reads the published version list of one package from the registry.
    ///
    /// @param npm the npm executable
    /// @param pkg the package to read
    /// @return the published version strings
    /// @throws DshException when the query fails or returns unexpected data
    private static Set<String> queryVersions(Path npm, String pkg) throws DshException {
        DshCommand.Result result = runNpmView(npm, pkg, "versions");
        JsonElement parsed = parseJson(result.text());
        if (parsed == null) {
            throw new DshException("npm returned no version list; check the network and the npm registry configuration");
        }
        Set<String> versions = new TreeSet<>();
        if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    versions.add(element.getAsString());
                }
            }
        } else if (parsed.isJsonPrimitive()) {
            // A package with exactly one published version answers with a bare string.
            versions.add(parsed.getAsString());
        }
        return versions;
    }

    /// Reads the dist-tag table from the registry.
    ///
    /// @param npm the npm executable
    /// @return the tag-to-version mapping, empty when the query fails
    /// Reads when each version was published.
    ///
    /// A failure is not fatal: the download page shows a version without its
    /// date rather than refusing to list it.
    ///
    /// @param npm the npm executable
    /// @return the version to publication-time map, empty when unavailable
    private static JsonObject queryTimes(Path npm) {
        try {
            return asObject(parseJson(runNpmView(npm, "time").text()));
        } catch (DshException e) {
            LOG.warning("Failed to read npm publication times", e);
            return new JsonObject();
        }
    }

    /// Returns the object npm answered with.
    ///
    /// `npm view <package> <field> --json` wraps its answer in an array when the
    /// field holds one value per package, which is the case for both `time` and
    /// `dist-tags`. Reading only a bare object silently yields nothing, and the
    /// symptom is an empty column rather than an error.
    ///
    /// @param parsed the parsed answer, possibly `null`
    /// @return the object, or an empty one when there is none
    private static JsonObject asObject(@Nullable JsonElement parsed) {
        if (parsed == null) {
            return new JsonObject();
        }
        if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    return element.getAsJsonObject();
                }
            }
            return new JsonObject();
        }
        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }

    private static JsonObject queryDistTags(Path npm) {
        try {
            return asObject(parseJson(runNpmView(npm, "dist-tags").text()));
        } catch (DshException e) {
            LOG.warning("Failed to read npm dist-tags", e);
            return new JsonObject();
        }
    }

    /// Runs `npm view <package> <field> --json`.
    ///
    /// @param npm   the npm executable
    /// @param field the registry field to read
    /// @return the command result
    /// @throws DshException when npm cannot be run
    private static DshCommand.Result runNpmView(Path npm, String field) throws DshException {
        return runNpmView(npm, PACKAGE_NAME, field);
    }

    /// PATH with the npm executable's own bin first — shebang scripts resolve
    /// `node` by name.
    private static java.util.Map<String, String> npmPathEnvironment(Path npm) {
        String bin = npm.getParent() == null ? "" : npm.getParent().toString();
        String existing = System.getenv("PATH");
        return java.util.Map.of("PATH", bin.isEmpty() ? String.valueOf(existing)
                : bin + java.io.File.pathSeparator + (existing == null ? "" : existing));
    }

    /// Runs `npm view <package> <field> --json`.
    ///
    /// @param npm   the npm executable
    /// @param pkg   the package to read
    /// @param field the field to read
    /// @return the command result
    /// @throws DshException when the command cannot be run
    private static DshCommand.Result runNpmView(Path npm, String pkg, String field) throws DshException {
        List<String> command = List.of(npm.toString(), "view", pkg, field, "--json");
        try {
            // npm 是 env-node shebang 脚本：裸部署下 spawn 必须带上运行时自己的 bin
            DshCommand.Result result = DshCommand.run(command, null, npmPathEnvironment(npm), null);
            if (!result.isSuccess()) {
                throw new DshException("npm view " + field + " exited with code " + result.exitCode()
                        + ": " + result.text());
            }
            return result;
        } catch (IOException e) {
            throw new DshException("Failed to run pnpm", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("npm view was interrupted", e);
        }
    }

    /// Parses JSON out of a command's output, returning `null` rather than throwing.
    ///
    /// npm writes warnings, deprecation notices and progress lines to the same stream as the
    /// answer, so the output is not JSON — it is some number of lines that are not JSON followed by
    /// the JSON. Reading from the first line that begins a value is what makes the answer findable
    /// whatever npm has decided to say first; parsing the whole output works only until the day npm
    /// has something to say, and then the version list is empty and the error blames the network.
    ///
    /// @param text the command's output
    /// @return the parsed element, or `null` when there is no value in it
    private static @Nullable JsonElement parseJson(String text) {
        if (text.isBlank()) {
            return null;
        }
        java.util.List<String> lines = text.lines().toList();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).stripLeading();
            if (line.isEmpty() || (line.charAt(0) != '{' && line.charAt(0) != '[')) {
                continue;
            }
            try {
                return JsonParser.parseString(String.join("\n", lines.subList(i, lines.size())));
            } catch (RuntimeException e) {
                // The first line that looked like a value was not the whole of it; the next one
                // that does may be.
                LOG.info("npm output from line " + (i + 1) + " did not parse as JSON; trying the next");
            }
        }
        LOG.warning("No JSON value in the command's output: "
                + text.substring(0, Math.min(200, text.length())));
        return null;
    }

    /// Deletes a directory, ignoring failures.
    ///
    /// @param directory the directory to remove
    /// Compares two version strings that may carry pre-release suffixes.
    ///
    /// Delegates to [org.jackhuang.hmcl.util.versioning.VersionNumber] — the
    /// Maven-precedence comparator HMCL already ships. The previous hand-rolled
    /// implementation fell back to `String.compareTo` between pre-releases of
    /// the same numeric prefix, which ranks `1.0.0-alpha.10` BELOW
    /// `1.0.0-alpha.2`; that ordering feeds the version picker, the lockstep
    /// "oldest" decision and the plugin/Node sort, so "what is newest" was
    /// genuinely misanswered.
    ///
    /// @param left  the first version
    /// @param right the second version
    /// @return a negative value, zero or a positive value as `left` sorts before, with or after `right`
    /// Reports whether a pnpm version is under the floor an installation needs.
    ///
    /// A version that cannot be read is not answered with a refusal: the string comes from a program
    /// the launcher did not write, and blocking an install over a version it could not parse would
    /// turn a working machine into a broken one. What is refused is a version that was read and is
    /// too low — including a prerelease of the floor itself, which is below it by the same rule the
    /// harness orders versions with.
    ///
    /// @param version the pnpm version, or `null` when it could not be read
    /// @return whether it is too old to install DeepSeek Harness with
    static boolean isPnpmTooOld(@Nullable String version) {
        if (version == null) {
            return false;
        }
        String normalized = version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        if (!normalized.matches("\\d+(\\.\\d+){0,2}([-+].*)?")) {
            return false;
        }
        int comparison = compareVersions(normalized, DshNodeRuntime.MINIMUM_PNPM);
        if (comparison < 0) {
            return true;
        }
        // A qualifier on the floor itself. The comparator this uses is the Maven-flavoured one the
        // rest of the launcher orders versions with, and Maven normalises a trailing `-0` (and `-ga`,
        // `-final`) to the release: `10.9.0-0` and `10.9.0` compare equal. The floor is the release,
        // so anything carrying a qualifier is under it — that is the one rule the comparator cannot
        // express, and the reason this is not a plain `< 0`. A `+build` is not a qualifier in that
        // sense and leaves the version where it is.
        return comparison == 0 && normalized.indexOf('-') >= 0;
    }

    /// Refuses an installation under a pnpm that is too old for it.
    ///
    /// @param runtime the runtime the install would use
    /// @throws DshException when its pnpm is under the floor
    static void requireSupportedPnpm(DshNodeRuntime runtime) throws DshException {
        if (isPnpmTooOld(runtime.pnpmVersion())) {
            throw new DshException("pnpm " + runtime.pnpmVersion()
                    + " is too old; installing DeepSeek Harness requires pnpm "
                    + DshNodeRuntime.MINIMUM_PNPM + " or newer");
        }
    }

    /// Refuses a runtime whose Node.js the harness cannot run on.
    ///
    /// Said before an install, not after one: the packages install fine under Node 20 and
    /// then the *launch* refuses, which reads as "the install worked, the harness is broken"
    /// a few minutes after the panel had everything it needed to know.
    ///
    /// @param runtime the runtime the work would use
    /// @throws DshException when its Node.js is outside the supported range
    static void requireSupportedNode(DshNodeRuntime runtime) throws DshException {
        if (!runtime.isNodeSupported()) {
            throw new DshException("The Node.js at " + runtime.node() + " is " + runtime.nodeVersion()
                    + ", outside the range DeepSeek Harness runs on (" + DshNodeRuntime.requirement()
                    + "). Install a suitable runtime on the Node page, or pick one for this instance.");
        }
    }

    static int compareVersions(String left, String right) {
        return org.jackhuang.hmcl.util.versioning.VersionNumber.compare(left, right);
    }
}
