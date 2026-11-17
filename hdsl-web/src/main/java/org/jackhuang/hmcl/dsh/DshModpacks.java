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

import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// A description of an instance that is enough to build it again elsewhere.
///
/// The original's modpack is a manifest plus the files that go with it: what to
/// download, and what to copy in afterwards. This is that for an instance of
/// DeepSeek Harness, and it is deliberately *configuration only* — the version of
/// the harness, the boot library it is paired with, the profile's plugin list in
/// its load order, and the profile's own patch layer. Nothing installed travels:
/// an installed tree is a few hundred megabytes of packages resolved for one
/// machine's platform and collected by one machine's package-manager store, and a
/// released Desktop build only gets away with shipping one because it vendors
/// integrity-recorded archives and filters them per platform. What is reproducible
/// is the *recipe*, and the recipe is a few kilobytes.
///
/// The plugin list is the part worth being careful about, because the profile's
/// bundle list is the order its patch layers are applied in: a later bundle
/// overrides an earlier one's rows, and a patch replaces a row's whole
/// configuration rather than merging into it. So the order is recorded exactly as
/// it is, and restored by writing the profile manifest and then letting pnpm
/// resolve the whole list in one run — installing bundles one at a time would
/// append each at the end and lose the order the pack was made with.
///
/// What a plugin was *configured* with travels too: the harness keeps one section per plugin in
/// `settings.yaml`, and that file is most of what "the same environment" means for a plugin — a
/// sidebar's custom CSS, a market's preferences. The sections travel, the values that look like
/// credentials do not, and a section is merged key by key into the next home so that a value the pack
/// left out cannot delete the one that machine already had. See [DshPluginSettings].
///
/// A plugin the instance installed from a file is the one thing that cannot be named and fetched, so
/// it travels as its own files and is put back into the next instance's plugin directory. That is the
/// exception to *configuration only*, and it is a deliberate one: a pack that named a path from the
/// machine it was made on would describe an installation nobody can make. A local plugin that the
/// registry *does* publish at the version the instance has is not carried — the pack names
/// `name@version` instead, which installs the same thing from the registry.
///
/// Credentials never travel. The harness keeps provider keys in `.credentials.yaml`
/// beside the sessions, and this reads nothing from there; the profile's patch
/// layer is included because it is composition, and the launcher says so when it
/// writes a pack, since a patch is a file a person can put anything into.
@NotNullByDefault
public final class DshModpacks {
    /// What a pack says it is.
    public static final String FORMAT = "hdsl-modpack";

    /// The file extension this launcher's own packs are written with.
    ///
    /// The container is a plain ZIP, so `.zip` opened it — and told nobody anything. A pack written
    /// by this launcher is a different thing from an archive somebody zipped by hand, and the name
    /// is the only place that can say so before anything is unzipped: it is what a file manager
    /// shows, what a browser offers to download, and what somebody types into the import box.
    ///
    /// `.dspack` is the community's and stays theirs; this is the launcher's own format.
    public static final String FILE_EXTENSION = ".hdslp";

    /// The extensions an import accepts.
    ///
    /// `.zip` is kept because packs written before the extension existed are `.zip`, and refusing
    /// them would be refusing the user's own files to make a naming point. What a file *is* is
    /// decided by its manifest, which both spellings carry.
    public static final java.util.List<String> ACCEPTED_EXTENSIONS = java.util.List.of(".hdslp", ".zip");

    /// The format's version.
    ///
    /// Version 2 added the plugins a pack carries — a plugin installed from a file that the registry
    /// does not publish now travels inside the pack, under `plugins/`, and is put back into the
    /// instance's own plugin directory on install. A version 2 pack is therefore one a version 1
    /// launcher cannot install correctly: it would read the plugin as a local one it cannot fetch and
    /// leave it out, so the pack is refused instead, which is the honest answer.
    ///
    /// Version 3 added the skill packs a pack carries — the skills an instance had under its
    /// home, under `skills/`, put back into the next home on install. Skills are content rather
    /// than configuration, which is why they are the second exception to the rule at the top of
    /// this file: they cannot be named and fetched, so they travel as their own files. A version
    /// 2 launcher would leave them out in silence, so a version 3 pack is refused by it instead.
    public static final int FORMAT_VERSION = 3;

    /// The manifest's name inside the archive.
    public static final String MANIFEST = "manifest.json";

    /// The profile patch layer's name inside the archive.
    private static final String PATCH = "cordis.patch.yml";

    private DshModpacks() {
    }

    /// One plugin the pack names.
    ///
    /// @param name    the package name
    /// @param version the declared version, or the path a locally installed plugin
    ///                was installed from
    /// @param active  whether the package is in the profile's bundle list
    /// @param local   whether the plugin was installed from a file this instance
    ///                keeps, and so cannot be fetched by anyone else
    /// One plugin a pack names.
    ///
    /// @param name    the package name
    /// @param version the version the pack pins, or a local specification for a plugin that was not
    ///                fetched — which is a path, and only meaningful on the machine it names
    /// @param active  whether the pack boots it as a bundle
    /// @param local   whether the instance installed it from a file rather than a registry
    /// @param bundled whether the pack carries the plugin's own files, which is what makes a local
    ///                plugin installable somewhere else
    public record Plugin(String name, String version, boolean active, boolean local, boolean bundled) {
    }

    /// What a pack describes.
    ///
    /// @param format     the format identifier
    /// @param version    the format version
    /// @param createdAt  when the pack was written
    /// @param instanceId the instance it came from
    /// @param dshVersion the DeepSeek Harness version it pins
    /// @param appBoot    the boot library it is paired with, or `null` for the same version
    /// @param profile    the profile the plugins belong to
    /// @param plugins    the plugins, in the profile manifest's order
    /// @param bundles    the active bundle list, in load order
    /// @param hasPatch   whether the pack carries the profile's patch layer
    /// @param name        what the pack is called
    /// @param packVersion the pack's own version
    /// @param author      who made it, or an empty string
    /// @param description what it is for, or an empty string
    /// @param sessionCount how many conversations it carries, zero for none
    /// @param settings        the `settings.yaml` sections the pack carries, in the file's order
    /// @param settingsOmitted the `section.key` paths left out of them because they look like
    ///                        credentials, so whoever opens the pack can see what it does not hold
    /// @param skills          the skill packs the pack carries, by the name each has on disk
    public record Manifest(String format, int version, String createdAt, String instanceId,
                           String dshVersion, @Nullable String appBoot, String profile,
                           List<Plugin> plugins, List<String> bundles, boolean hasPatch,
                           String name, String packVersion, String author, String description,
                           String url, String referenceUrl, int sessionCount,
                           List<String> settings, List<String> settingsOmitted,
                           List<String> skills) {

        /// Returns the plugins that should be installed, in the order they are
        /// listed in.
        ///
        /// A pack can name a package that is not in the bundle list — an installed
        /// but switched-off plugin. It is still installed, because that is what
        /// the instance it came from had; whether the harness then contributes its
        /// layer is decided by the bundle list, which is what the pack restores
        /// separately.
        ///
        /// @return the installation specifications, in order
        public List<String> installSpecs() {
            List<String> specs = new ArrayList<>();
            for (Plugin plugin : plugins) {
                if (plugin.local()) {
                    continue;
                }
                specs.add(plugin.version() == null || plugin.version().isBlank()
                        ? plugin.name() : plugin.name() + "@" + plugin.version());
            }
            return specs;
        }

        /// Returns the plugins that were installed from a file rather than fetched.
        ///
        /// A profile records the path it installed a local plugin from, which is a
        /// path inside the instance that had it; another instance cannot install
        /// from it, and a pack that pretended otherwise would describe an
        /// installation that cannot be made.
        ///
        /// @return the local plugins
        public List<Plugin> localPlugins() {
            return plugins.stream().filter(Plugin::local).toList();
        }

        /// Returns the local plugins a pack carries the files of.
        ///
        /// @return the plugins whose files travel inside the pack
        public List<Plugin> bundledPlugins() {
            return plugins.stream().filter(Plugin::bundled).toList();
        }

        /// Returns the local plugins a pack cannot install.
        ///
        /// A pack written before it carried local plugin files names one, and so does a pack whose
        /// instance had already lost the file: either way there is nothing to install from, and
        /// saying which plugin it is beats a resolve that fails on a path from another machine.
        ///
        /// @return the plugins that cannot be installed
        public List<Plugin> uninstallablePlugins() {
            return plugins.stream().filter(plugin -> plugin.local() && !plugin.bundled()).toList();
        }
    }

    /// What an export wrote.
    ///
    /// @param plugins how many plugins were recorded
    /// @param bytes   the pack's size
    public record ExportResult(int plugins, long bytes) {
    }

    /// What a load restored.
    ///
    /// @param instance   the instance that was built
    /// @param installed  whether the harness itself had to be installed
    /// @param plugins    how many plugins were resolved
    /// @param bundles    how many bundles are active
    public record InstallResult(DshInstance instance, boolean installed, int plugins, int bundles) {
    }

    /// Writes an instance's configuration into a pack.
    ///
    /// What a pack says about itself, and what it carries.
    ///
    /// @param name            what the pack is called, or an empty string for the instance's id
    /// @param version         the pack's own version
    /// @param author          who made it
    /// @param description     what it is for
    /// @param includeSessions whether the instance's conversations travel with it
    /// @param excludedBundles the bundles the person chose to leave out
    /// @param settings        the `settings.yaml` sections that travel: a plugin's own settings, and
    ///                        most of what "the same environment" means for it
    /// @param skills          the skill packs that travel, by the name each has under the home's
    ///                        skills directory; names rather than packs, so the choice is a small
    ///                        thing to hold and is read against what is there when the pack is
    ///                        written
    public record Options(String name, String version, String author, String description,
                          String url, String referenceUrl,
                          boolean includeSessions, Set<String> excludedBundles, Set<String> settings,
                          Set<String> skills) {
        /// Returns options that carry no addresses.
        ///
        /// @param name            what the pack is called
        /// @param version         the pack's own version
        /// @param author          who made it
        /// @param description     what it is for
        /// @param includeSessions whether the conversations travel
        public Options(String name, String version, String author, String description,
                       boolean includeSessions) {
            this(name, version, author, description, "", "", includeSessions, Set.of(), Set.of(),
                    Set.of());
        }

        /// Returns options that carry every bundle.
        ///
        /// @param name            what the pack is called
        /// @param version         the pack's own version
        /// @param author          who made it
        /// @param description     what it is for
        /// @param includeSessions whether the conversations travel

        /// Returns the options a pack is written with when nobody chose any.
        ///
        /// Everything a plugin keeps in the harness's settings travels, because that is what makes a
        /// pack an environment rather than a list of packages — and because the alternative, a pack
        /// that names a sidebar but not the stylesheet it was configured with, is the difference
        /// nobody notices until they open it. Values that look like credentials are removed on the
        /// way out; see [DshPluginSettings].
        ///
        /// The same is true of the skill packs an instance has: a pack that reproduces an
        /// environment without the skills it was set up around is missing the part that took the
        /// longest to assemble, and a skill is content rather than configuration, so there is
        /// nowhere else for it to come from.
        ///
        /// @param instance the instance
        /// @return the options
        public static Options of(DshInstance instance) {
            Set<String> settings = Set.of();
            try {
                settings = new java.util.LinkedHashSet<>(DshPluginSettings.sectionsOf(instance.homeDirectory()));
            } catch (DshException e) {
                LOG.warning("Failed to read the settings of " + instance.id(), e);
            }
            Set<String> skills = Set.of();
            try {
                skills = new java.util.LinkedHashSet<>(DshSkills.packableNames(instance.homeDirectory()));
            } catch (DshException e) {
                LOG.warning("Failed to read the skills of " + instance.id(), e);
            }
            return new Options(instance.id(), "1.0", "", "", "", "", false, Set.of(), settings,
                    skills);
        }

        /// Returns whether a bundle travels.
        ///
        /// @param bundle the bundle's name
        /// @return whether it does
        public boolean includesBundle(String bundle) {
            return !excludedBundles.contains(bundle);
        }
    }

    /// @param instance the instance to describe
    /// @param target   the archive to create
    /// @param onStage  receives progress lines, or `null`
    /// @return what was written
    /// @throws DshException when the configuration cannot be read or written
    public static ExportResult export(DshInstance instance, Path target,
                                      @Nullable Consumer<String> onStage) throws DshException {
        return export(instance, target, Options.of(instance), onStage);
    }

    /// Writes an instance's configuration into a pack.
    ///
    /// @param instance the instance to describe
    /// @param target   the archive to create
    /// @param options  what the pack should say and carry
    /// @param onStage  receives progress lines, or `null`
    /// @return what was written
    /// @throws DshException when the configuration cannot be read or written
    public static ExportResult export(DshInstance instance, Path target, Options options,
                                      @Nullable Consumer<String> onStage) throws DshException {
        Path profileDirectory = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        Map<String, String> dependencies = DshPluginInstaller.readDependencies(
                instance.homeDirectory(), instance.profile());
        List<String> bundles = new ArrayList<>(
                DshPluginInstaller.readBundles(instance.homeDirectory(), instance.profile()));
        // A bundle somebody unticked is not in the pack and is not recorded as one of its plugins:
        // a pack that named a plugin it does not install would describe an installation nobody can
        // make, and an import would try.
        bundles.removeIf(bundle -> !options.includesBundle(bundle));

        List<Plugin> plugins = new ArrayList<>();
        List<DshPluginBundle.Payload> carried = new ArrayList<>();
        for (Map.Entry<String, String> entry : dependencies.entrySet()) {
            if (!options.includesBundle(entry.getKey())) {
                continue;
            }
            boolean active = bundles.contains(entry.getKey());
            if (!isLocalSpec(entry.getValue())) {
                plugins.add(new Plugin(entry.getKey(), entry.getValue(), active, false, false));
                continue;
            }
            plugins.add(localPlugin(profileDirectory, entry.getKey(), entry.getValue(), active,
                    carried, onStage));
        }

        Path patch = profileDirectory.resolve("cordis.patch.yml");
        boolean hasPatch = Files.isRegularFile(patch);
        String appBoot = DshVersionManager.readAppBoot(instance);
        if (appBoot != null && appBoot.equals(instance.version())) {
            // A pin equal to the version is what an instance has by default; a pack
            // does not need to say so, and saying it would make the pack claim a
            // choice nobody made.
            appBoot = null;
        }

        List<DshSession> sessions = options.includeSessions()
                ? DshSessions.list(instance.homeDirectory())
                : List.of();

        DshPluginSettings.Carried settings = options.settings().isEmpty()
                ? new DshPluginSettings.Carried("", List.of(), List.of())
                : DshPluginSettings.extract(instance.homeDirectory(), options.settings());
        if (!settings.sections().isEmpty()) {
            report(onStage, "Carrying the settings of " + settings.sections().size() + " plugin(s): "
                    + String.join(", ", settings.sections()));
        }
        if (!settings.omitted().isEmpty()) {
            // Named rather than counted: whoever opens the pack should be able to see what was left
            // out, and whoever made it should be able to see that a setting did not travel.
            report(onStage, "Left out " + settings.omitted().size() + " value(s) that look like credentials: "
                    + String.join(", ", settings.omitted()));
        }

        // The pack carries the skill packs the options asked for and the home actually has:
        // a name for a skill that is no longer there is not something to fail over, because the
        // alternative is a pack that cannot be written after a skill was deleted.
        List<DshSkill> skills = DshSkills.packable(instance.homeDirectory(), options.skills());
        List<String> skillNames = skills.stream().map(DshSkill::fileName).toList();

        Manifest manifest = new Manifest(FORMAT, FORMAT_VERSION, Instant.now().toString(), instance.id(),
                instance.version(), appBoot, instance.profile(), List.copyOf(plugins), List.copyOf(bundles),
                hasPatch, options.name(), options.version(), options.author(), options.description(),
                options.url(), options.referenceUrl(),
                sessions.size(), settings.sections(), settings.omitted(), skillNames);

        report(onStage, "Recording " + plugins.size() + " plugin(s), " + bundles.size() + " active bundle(s)");
        Path parent = target.toAbsolutePath().getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
                if (hasPatch) {
                    // The copy goes in repaired, for the same reason the installer repairs it on the
                    // way out: a profile that both boots a plugin as a bundle and inserts it applies
                    // it twice and does not start. Fixing it only on install would leave every pack
                    // made from such an instance carrying the defect forward.
                    byte[] body = DshProfilePatch.withoutRedundantInserts(
                            Files.readString(patch, StandardCharsets.UTF_8), bundles).text()
                            .getBytes(StandardCharsets.UTF_8);
                    zip.putNextEntry(new ZipEntry(PATCH));
                    zip.write(body);
                    zip.closeEntry();
                }
                if (!settings.text().isEmpty()) {
                    zip.putNextEntry(new ZipEntry(DshPluginSettings.ENTRY));
                    zip.write(settings.text().getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
                for (DshPluginBundle.Payload payload : carried) {
                    // A local plugin nobody publishes travels as its own files, and there is no
                    // smaller way to say it: the alternative is a pack that cannot be installed.
                    DshPluginBundle.writeInto(zip, payload, onStage);
                }
                if (!sessions.isEmpty()) {
                    // The conversations are written the way a session pack writes
                    // them, because that is the layout the harness reads and the
                    // rules are the same either way.
                    DshSessionPacks.writeInto(zip, instance.homeDirectory(), sessions, onStage);
                    DshSessionPacks.writeAttachmentsInto(zip, instance.homeDirectory(), sessions, onStage);
                }

                if (!skills.isEmpty()) {
                    // The layout under the home is the layout in the pack, which is what makes
                    // restoring one a copy rather than a translation.
                    DshSkills.writeInto(zip, skills, onStage);
                }
                zip.putNextEntry(new ZipEntry(MANIFEST));
                zip.write(JsonUtils.GSON.toJson(manifest).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new DshException("Failed to write " + target, e);
        }

        long size = sizeOf(target);
        report(onStage, "Wrote a pack describing DeepSeek Harness " + instance.version()
                + (appBoot == null ? "" : " with boot library " + appBoot)
                + ", " + plugins.size() + " plugin(s)");
        LOG.info("Wrote a modpack for " + instance.id() + " to " + target);
        return new ExportResult(plugins.size(), size);
    }

    /// Records a plugin the instance installed from a file.
    ///
    /// The registry is asked first, because a plugin that is published at the version the instance
    /// has does not need to travel: the pack names `name@version` and the install fetches it, which is
    /// smaller and is the same code either way. Only when the registry does not publish it — or cannot
    /// be asked at all, which is not the same as an answer — are the files carried, because a pack
    /// that named a package nobody can fetch would be a pack that does not install.
    ///
    /// @param profileDirectory the profile the plugin was installed into
    /// @param name             the dependency name
    /// @param declared         what the profile declares for it
    /// @param active           whether it is in the bundle list
    /// @param carried          collects the payloads the pack has to carry
    /// @param onStage          receives progress lines, or `null`
    /// @return what the pack records for it
    private static Plugin localPlugin(Path profileDirectory, String name, String declared, boolean active,
                                      List<DshPluginBundle.Payload> carried,
                                      @Nullable Consumer<String> onStage) {
        DshPluginBundle.Payload payload = DshPluginBundle.locate(profileDirectory, name, declared);
        if (payload == null) {
            // Two reasons, and the difference is what a person can do about it: a file that was
            // deleted can be put back where it was, while a plugin somebody is working on has no
            // copy here at all — the profile holds a link to their directory, not the directory.
            report(onStage, DshPluginBundle.namesADirectory(declared, profileDirectory)
                    ? "Note: " + name + " is a plugin under development, installed from the directory"
                            + " it is worked on in, so the pack does not carry it"
                    : "Note: " + name + " was installed from a file this instance no longer has,"
                            + " so the pack cannot carry it");
            return new Plugin(name, declared, active, true, false);
        }

        DshPackageRegistry.Availability availability =
                DshPackageRegistry.availability(name, payload.version());
        if (availability == DshPackageRegistry.Availability.PUBLISHED) {
            report(onStage, name + " " + payload.version() + " is published, so the pack fetches it"
                    + " from the registry rather than carrying it");
            return new Plugin(name, payload.version(), active, false, false);
        }
        if (availability == DshPackageRegistry.Availability.UNKNOWN) {
            report(onStage, "Could not ask the registry about " + name + " " + payload.version()
                    + "; carrying its files instead");
        } else {
            report(onStage, name + " " + payload.version() + " is not published, so the pack must carry it");
        }
        carried.add(payload);
        return new Plugin(name, payload.version(), active, true, true);
    }

    /// One plugin an instance installed from this machine rather than from a registry.
    ///
    /// @param name        the dependency name
    /// @param development whether it was installed from a directory somebody is working on — a
    ///                    `link:`, or a `file:` that is not an archive — which a pack does not carry
    public record LocalPlugin(String name, boolean development) {
    }

    /// Returns the plugins an instance installed from this machine rather than from a registry.
    ///
    /// A `file:` or `link:` specification names something that was on the machine the instance was
    /// made on, and a pack is handed to somebody else. What the export does about it depends on
    /// which of the two it is: a packed plugin the launcher copied into the instance travels as the
    /// file it is, while a directory somebody is working on — `link:`, and a `file:` that names a
    /// directory — has no files of the instance's own to carry and is left out of the pack
    /// entirely. Both are worth saying **before** the export starts, which is the last moment at
    /// which the person can still do something about either.
    ///
    /// Upstream names this `PackExporter.localPlugins` (fdc8349) and its flag `development`
    /// (ad517a1); this launcher has no such class, its export code is this one, and
    /// [Manifest#localPlugins] already means the recorded half of it, so the name here is
    /// `localPluginsOf`.
    ///
    /// @param instance the instance
    /// @return the plugins, in the profile's order, empty when there are none
    /// @throws DshException when the profile cannot be read
    public static List<LocalPlugin> localPluginsOf(DshInstance instance) throws DshException {
        Path profileDirectory = instance.homeDirectory()
                .resolve("profiles").resolve(instance.profile());
        List<LocalPlugin> plugins = new ArrayList<>();
        for (Map.Entry<String, String> entry
                : DshPluginInstaller.readDependencies(instance.homeDirectory(), instance.profile())
                        .entrySet()) {
            if (isLocalSpec(entry.getValue())) {
                plugins.add(new LocalPlugin(entry.getKey(),
                        DshPluginBundle.namesADirectory(entry.getValue(), profileDirectory)));
            }
        }
        return List.copyOf(plugins);
    }

    /// Reads a pack's manifest.
    ///
    /// @param pack the archive
    /// @return the manifest
    /// @throws DshException when the archive holds no manifest
    public static Manifest readManifest(Path pack) throws DshException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (MANIFEST.equals(entry.getName())) {
                    JsonElement parsed = JsonParser.parseString(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                    if (!parsed.isJsonObject()) {
                        throw new DshException("The pack's manifest is not an object");
                    }
                    return manifestOf(parsed.getAsJsonObject());
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + pack, e);
        }
        throw new DshException("This file holds no modpack manifest");
    }

    /// Builds an instance from a pack.
    ///
    /// The harness is installed at the version the pack pins, held to the boot
    /// library it was paired with; the profile is then filled with the pack's
    /// plugins at their recorded versions and with its bundle list, in the order
    /// the pack recorded.
    ///
    /// @param pack      the archive
    /// @param id        the id to give the instance
    /// @param workspace the working directory the instance starts sessions in
    /// @param onStage   receives progress lines, or `null`
    /// @return what was built
    /// @throws DshException when the pack cannot be read, the id is taken, or an
    ///                      installation fails
    public static InstallResult install(Path pack, String id, Path workspace,
                                        @Nullable Consumer<String> onStage) throws DshException {
        Manifest manifest = readManifest(pack);
        requireFormat(manifest);

        DshInstance existing = DshInstanceManager.find(id);
        if (existing == null) {
            report(onStage, "Creating instance " + id);
            existing = DshInstanceManager.create(id, manifest.dshVersion(), manifest.profile(), workspace,
                    DshHomeMode.ISOLATED, null, List.of(), Map.of());
        }

        boolean installed = DshVersionManager.isInstalled(existing);
        if (!installed) {
            report(onStage, "Installing DeepSeek Harness " + manifest.dshVersion());
            DshVersionManager.install(existing, manifest.dshVersion(), onStage);
        } else {
            report(onStage, "Instance " + id + " already has its own DeepSeek Harness");
        }

        if (manifest.appBoot() != null && !manifest.appBoot().equals(existing.version())) {
            report(onStage, "Holding it to boot library " + manifest.appBoot());
            DshVersionManager.overrideAppBoot(existing, manifest.appBoot(), onStage);
        }

        int plugins = restoreProfile(existing, manifest, pack, onStage);
        if (manifest.sessionCount() > 0) {
            DshSessionPacks.restoreInto(pack, existing.homeDirectory(), onStage);
        }
        if (!manifest.skills().isEmpty()) {
            // Content rather than configuration, so it is put back the way it came out: a copy
            // under the home's own skills directory. The harness reads that directory when it
            // starts, so nothing else has to be told.
            DshSkills.restoreInto(pack, existing.homeDirectory(), onStage);
        }
        return new InstallResult(existing, !installed, plugins, manifest.bundles().size());
    }

    /// Puts a pack's profile into an instance that already exists.
    ///
    /// The harness's own profile initialization is used — an empty `dsh plugin
    /// install` run creates the profile manifest, the patch template and the
    /// package-manager settings exactly as upstream writes them — and only the two
    /// fields a pack has something to say about are then replaced: the declared
    /// dependencies, at the versions the pack recorded, and the ordered bundle
    /// list. One resolve of that manifest installs everything and leaves the order
    /// alone, because reconciliation preserves the entries a manifest already
    /// lists and only appends what is missing.
    ///
    /// @param instance the instance to fill
    /// @param manifest the pack's manifest
    /// @param pack     the archive, for its patch layer
    /// @param onStage  receives progress lines, or `null`
    /// @return how many plugins were resolved
    /// @throws DshException when the profile cannot be written or the resolve fails
    public static int restoreProfile(DshInstance instance, Manifest manifest, Path pack,
                                     @Nullable Consumer<String> onStage) throws DshException {
        Path profileDirectory = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        Path manifestFile = profileDirectory.resolve("package.json");

        if (!Files.isRegularFile(manifestFile)) {
            // The harness's own initialization writes the profile manifest, the
            // patch template and the package-manager settings; writing them here
            // would be reproducing upstream's file formats, which is exactly the
            // kind of guessing that has broken an instance before.
            report(onStage, "Initializing profile " + instance.profile());
            DshPluginInstaller.resolve(instance, onStage);
        }

        if (manifest.hasPatch()) {
            Path patch = profileDirectory.resolve("cordis.patch.yml");
            report(onStage, "Restoring the profile's patch layer");
            writePatch(pack, patch);
        }

        String settings = readSettings(pack);
        if (!settings.isBlank()) {
            List<String> merged = DshPluginSettings.merge(instance.homeDirectory(), settings);
            report(onStage, "Put the pack's settings for " + merged.size() + " plugin(s) into"
                    + " settings.yaml: " + String.join(", ", merged));
        }

        if (!manifest.plugins().isEmpty() || !manifest.bundles().isEmpty()) {
            report(onStage, "Writing the pack's plugin list");
            Map<String, Path> released = releaseCarried(instance, manifest, pack, onStage);
            writeProfileManifest(manifestFile, manifest, released);
            if (!manifest.uninstallablePlugins().isEmpty()) {
                report(onStage, "Note: " + manifest.uninstallablePlugins().size()
                        + " plugin(s) were installed from a file the pack does not carry, so they are left"
                        + " out rather than pointed at a path from another machine: " + String.join(", ",
                                manifest.uninstallablePlugins().stream().map(Plugin::name).toList()));
            }
            report(onStage, "Resolving " + manifest.installSpecs().size() + " plugin(s)");
            DshPluginInstaller.resolve(instance, onStage);
        }

        return manifest.plugins().size();
    }

    /// Puts the plugins a pack carries back into the instance.
    ///
    /// They are written under the directory the launcher keeps local plugin files in, which is where
    /// an instance that installed one by hand would hold it, and the path returned is what the profile
    /// then depends on.
    ///
    /// @param instance the instance being filled
    /// @param manifest the pack's manifest
    /// @param pack     the archive
    /// @param onStage  receives progress lines, or `null`
    /// @return where each carried plugin was put, by package name
    /// @throws DshException when the files cannot be written
    private static Map<String, Path> releaseCarried(DshInstance instance, Manifest manifest, Path pack,
                                                    @Nullable Consumer<String> onStage) throws DshException {
        List<Plugin> bundled = manifest.bundledPlugins();
        if (bundled.isEmpty()) {
            return Map.of();
        }
        Path pluginsDirectory = instance.instanceDirectory().resolve(DshLocalPlugins.DIRECTORY);
        Map<String, Path> released = new LinkedHashMap<>();
        for (Plugin plugin : bundled) {
            Path path = DshPluginBundle.release(pack, pluginsDirectory, plugin.name(), plugin.version());
            if (path == null) {
                report(onStage, "Note: the pack says it carries " + plugin.name()
                        + ", but it holds no files for it");
                continue;
            }
            report(onStage, "Put " + plugin.name() + " " + plugin.version() + " into the instance");
            released.put(plugin.name(), path);
        }
        return released;
    }

    /// Writes a pack's dependencies and bundle list into a profile manifest.
    ///
    /// Only those two fields are touched: everything else in the file — the
    /// profile's name, and anything a harness version puts there that this launcher
    /// does not know about — is left as it is.
    ///
    /// A plugin the pack carries is declared by the path its files were put back at, on *this*
    /// machine: a profile records the file it installed from and resolves it again on every later
    /// operation, so a path from the machine the pack was made on would break every one of them.
    ///
    /// @param manifestFile the profile's `package.json`
    /// @param pack         the pack's manifest
    /// @param released     where the pack's carried plugins were put, by package name
    /// @throws DshException when the file cannot be read or written
    private static void writeProfileManifest(Path manifestFile, Manifest pack, Map<String, Path> released)
            throws DshException {
        JsonObject manifest;
        try {
            manifest = JsonUtils.fromJsonFile(manifestFile, JsonObject.class);
            if (manifest == null) {
                manifest = new JsonObject();
            }
        } catch (Exception e) {
            throw new DshException("Failed to read " + manifestFile, e);
        }

        JsonObject dependencies = new JsonObject();
        for (Map.Entry<String, String> entry : dependenciesOf(pack, released).entrySet()) {
            dependencies.addProperty(entry.getKey(), entry.getValue());
        }
        manifest.add("dependencies", dependencies);

        JsonObject dsh = manifest.has("dsh") && manifest.get("dsh").isJsonObject()
                ? manifest.getAsJsonObject("dsh") : new JsonObject();
        JsonObject profile = dsh.has("profile") && dsh.get("profile").isJsonObject()
                ? dsh.getAsJsonObject("profile") : new JsonObject();
        JsonArray bundles = new JsonArray();
        listsBootable(pack, dependencies).forEach(bundles::add);
        profile.add("bundles", bundles);
        dsh.add("profile", profile);
        manifest.add("dsh", dsh);

        Path staging = manifestFile.resolveSibling("package.json.pack");
        try {
            Files.writeString(staging, JsonUtils.GSON.toJson(manifest));
            Files.move(staging, manifestFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DshException("Failed to write " + manifestFile, e);
        }
        LOG.info("Restored the plugin list of " + manifestFile.getParent().getFileName()
                + ": " + pack.plugins().size() + " plugin(s), " + pack.bundles().size() + " bundle(s)");
    }

    /// Returns the dependencies a pack's profile should declare.
    ///
    /// Three cases, and the difference between them is the whole of what a local plugin needs:
    ///
    /// - **Carried and put back.** The value is the path *this* machine now holds the files at. A
    ///   profile records the file it installed from and resolves it again on every later operation,
    ///   so the path has to be one that exists here; a path from the machine the pack was made on is
    ///   the bug this replaces.
    /// - **Carried but not found in the pack.** Nothing to install from, so nothing is declared.
    /// - **Local and not carried.** A pack written before packs carried local plugins names one. Its
    ///   path is another machine's, and declaring it would make every later operation on the profile
    ///   fail with a message about a missing file. It is left out instead.
    ///
    /// @param pack     the pack's manifest
    /// @param released where the pack's carried plugins were put, by package name
    /// @return the dependency name to declaration map, in the pack's order
    static Map<String, String> dependenciesOf(Manifest pack, Map<String, Path> released) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (Plugin plugin : pack.plugins()) {
            if (plugin.bundled()) {
                Path path = released.get(plugin.name());
                if (path != null) {
                    ordered.put(plugin.name(), path.toString());
                }
                continue;
            }
            if (plugin.local()) {
                continue;
            }
            ordered.put(plugin.name(), plugin.version() == null ? "" : plugin.version());
        }
        return ordered;
    }

    /// Returns the bundles a profile with these dependencies can boot.
    ///
    /// A bundle that could not be installed is not booted either: naming a package the profile does
    /// not have stops the instance from starting, which is a worse answer than starting without a
    /// plugin that was never there. A bundle the pack does not name as a plugin is kept — the
    /// harness's own bundles are packages a profile does not declare.
    ///
    /// @param pack         the pack's manifest
    /// @param dependencies what the profile will declare
    /// @return the bundle list, in the pack's order
    static List<String> listsBootable(Manifest pack, JsonObject dependencies) {
        List<String> bootable = new ArrayList<>();
        for (String bundle : pack.bundles()) {
            boolean named = pack.plugins().stream().anyMatch(plugin -> plugin.name().equals(bundle));
            if (!named || dependencies.has(bundle)) {
                bootable.add(bundle);
            }
        }
        return bootable;
    }

    /// Reads the settings a pack carries.
    ///
    /// @param pack the archive
    /// @return the text, or an empty string when the pack carries none
    /// @throws DshException when the archive cannot be read
    private static String readSettings(Path pack) throws DshException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (DshPluginSettings.ENTRY.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + pack, e);
        }
        return "";
    }

    /// Reads a pack's patch layer into a profile.
    ///
    /// @param pack   the archive
    /// @param target the file to write
    /// @throws DshException when the archive cannot be read or the file written
    private static void writePatch(Path pack, Path target) throws DshException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(pack))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (PATCH.equals(entry.getName())) {
                    byte[] body = zip.readAllBytes();
                    Files.createDirectories(target.getParent());
                    Path staging = target.resolveSibling("cordis.patch.yml.pack");
                    Files.write(staging, body);
                    Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + pack, e);
        }
        throw new DshException("The pack says it carries a patch layer, but does not");
    }

    /// Reads a manifest object.
    ///
    /// @param root the manifest
    /// @return the manifest record
    private static Manifest manifestOf(JsonObject root) {
        List<Plugin> plugins = new ArrayList<>();
        JsonElement list = root.get("plugins");
        if (list != null && list.isJsonArray()) {
            for (JsonElement element : list.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String name = string(object, "name");
                if (name == null) {
                    continue;
                }
                plugins.add(new Plugin(name, string(object, "version"), bool(object, "active"),
                        bool(object, "local"), bool(object, "bundled")));
            }
        }

        List<String> bundles = new ArrayList<>();
        JsonElement active = root.get("bundles");
        if (active != null && active.isJsonArray()) {
            for (JsonElement element : active.getAsJsonArray()) {
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                    bundles.add(element.getAsString());
                }
            }
        }

        return new Manifest(
                string(root, "format") == null ? "" : string(root, "format"),
                integer(root, "version"),
                string(root, "createdAt") == null ? "" : string(root, "createdAt"),
                string(root, "instanceId") == null ? "" : string(root, "instanceId"),
                string(root, "dshVersion") == null ? "" : string(root, "dshVersion"),
                string(root, "appBoot"),
                string(root, "profile") == null ? DshInstance.DEFAULT_PROFILE : string(root, "profile"),
                List.copyOf(plugins), List.copyOf(bundles), bool(root, "hasPatch"),
                string(root, "name") == null ? "" : string(root, "name"),
                string(root, "packVersion") == null ? "1.0" : string(root, "packVersion"),
                string(root, "author") == null ? "" : string(root, "author"),
                string(root, "description") == null ? "" : string(root, "description"),
                string(root, "url") == null ? "" : string(root, "url"),
                string(root, "referenceUrl") == null ? "" : string(root, "referenceUrl"),
                integer(root, "sessionCount"), strings(root, "settings"), strings(root, "settingsOmitted"),
                strings(root, "skills"));
    }

    /// Reads an array of strings.
    ///
    /// @param root the object
    /// @param name the field
    /// @return the strings, empty when the field is absent or not an array
    private static List<String> strings(JsonObject root, String name) {
        List<String> values = new ArrayList<>();
        JsonElement element = root.get(name);
        if (element != null && element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive() && item.getAsJsonPrimitive().isString()) {
                    values.add(item.getAsString());
                }
            }
        }
        return List.copyOf(values);
    }

    /// Reports whether a declared version is really the path of a local file.
    ///
    /// A package manager records a local installation by the specification it was
    /// given, in the field a version would otherwise hold, so a profile that
    /// installed a packed plugin says `file:/…/plugins/x.tgz` where a version
    /// belongs. Reading that as a version would have a pack ask for a package
    /// called `x@file:/…`, which resolves to nothing.
    ///
    /// @param version the declared version
    /// @return whether it is a local installation's path
    static boolean isLocalSpec(@Nullable String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        String value = version.trim().toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("file:") || value.startsWith("link:") || value.startsWith("/")
                || value.startsWith("./") || value.startsWith("../");
    }

    /// Refuses a pack this launcher cannot read.
    ///
    /// @param manifest the pack's manifest
    /// @throws DshException when the format or version is not one this reads
    private static void requireFormat(Manifest manifest) throws DshException {
        if (!FORMAT.equals(manifest.format())) {
            throw new DshException("This file is not an instance pack (" + manifest.format() + ")");
        }
        if (manifest.version() > FORMAT_VERSION) {
            throw new DshException("This pack was written by a newer launcher (format " + manifest.version()
                    + "), which this one cannot read");
        }
        if (manifest.dshVersion().isBlank()) {
            throw new DshException("This pack does not say which DeepSeek Harness version it wants");
        }
    }

    /// Returns a member as a primitive, or `null`.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the value, or `null`
    private static @Nullable JsonElement member(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || !element.isJsonPrimitive() ? null : element;
    }

    /// Returns a string member, or `null`.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the string, or `null`
    private static @Nullable String string(JsonObject object, String name) {
        JsonElement element = member(object, name);
        return element == null || !((JsonPrimitive) element).isString() ? null : element.getAsString();
    }

    /// Returns a boolean member, or false.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the value, or false
    private static boolean bool(JsonObject object, String name) {
        JsonElement element = member(object, name);
        if (element == null) {
            return false;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /// Returns an integer member, or zero.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the number, or zero
    private static int integer(JsonObject object, String name) {
        JsonElement element = member(object, name);
        if (element == null || !((JsonPrimitive) element).isNumber()) {
            return 0;
        }
        return element.getAsInt();
    }

    /// Reports progress.
    ///
    /// @param onStage the sink, or `null`
    /// @param message the message
    private static void report(@Nullable Consumer<String> onStage, String message) {
        if (onStage != null) {
            onStage.accept(message);
        }
    }

    /// Returns a file's size.
    ///
    /// @param file the file
    /// @return the size, or zero
    private static long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }
}
