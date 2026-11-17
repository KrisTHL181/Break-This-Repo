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
import com.google.gson.JsonObject;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Writes an instance as a pack the community's tooling reads.
///
/// The `DSH-PackForge` specification is not this launcher's own, and it describes a different
/// thing from the pack this launcher writes for itself: that one is a recipe — a version, an
/// ordered plugin list, a patch layer — while this one is a **snapshot**: the profile's files,
/// scanned, filtered and carried inside a `.dspack` container with a manifest that says what
/// they are. Both are useful and neither replaces the other, which is why the export wizard asks
/// which one to write.
///
/// The container is a plain ZIP with `dspack.json` at its root — the marker that says this is a
/// `.dspack` and which version of the container it is — and the profile's files live under
/// `overrides/`, which is what an importer copies into the profile it is rebuilding. Content from
/// the level above the profile, the global `skills/` and `.agent-presets/` among it, can travel
/// under `home/`, which an importer lands at the `$DSH_HOME` root; it goes in only when the export
/// was told which paths to carry. The manifest is v5 with `type: "profile"`.
///
/// The specification's filter is the part worth taking literally: an exported pack is something
/// people publish, so installed trees, credentials, keys, private keys, tokens and nested archives
/// never travel, whatever the exporter would otherwise copy.
@NotNullByDefault
public final class DshPackForge {
    /// The format's marker file, at the container's root.
    public static final String MARKER = "dspack.json";

    /// The container version this writes.
    public static final int CONTAINER_VERSION = 3;

    /// The manifest's name inside the container.
    public static final String MANIFEST = "manifest.json";

    /// Where a profile's own files go, which is where an importer copies them from.
    public static final String OVERRIDES = "overrides/";

    /// Where content from the home rather than the profile goes.
    ///
    /// A profile pack may carry the level above the profile — the global `skills/`, the
    /// `.agent-presets/`, `AGENTS.md` — and this is the container's name for that level. An importer
    /// lands these at the `$DSH_HOME` root, which is where they belong: they are not the profile's
    /// own files, and installing them into the profile would put a global skill somewhere nothing
    /// looks for one.
    public static final String HOME = "home/";

    /// The home's own profiles, which a profile pack never carries.
    ///
    /// `overrides/` is how a pack carries *its* profile; the home's `profiles/` directory holds every
    /// other profile on the machine, which is not this pack's to distribute.
    private static final String HOME_PROFILES = "profiles/";

    /// The manifest version this writes.
    public static final int MANIFEST_VERSION = 5;

    /// Credential names, refused by **both** sides of a pack.
    ///
    /// One set of rules rather than one per direction, and that is the point: the export side guards
    /// the author's secrets from being published and [DshPackInstaller] guards the machine from being
    /// written to, but a credential is both, and two hand-written lists are how a name ends up
    /// refused on one side and published on the other — which is what had happened here. The export
    /// side is the superset: everything [isCredentialName] refuses, it refuses, and what it knows and
    /// the import side does not is below.
    static final List<String> CREDENTIAL_NAMES = List.of(
            ".env", ".netrc", ".pypirc", ".npmrc", ".yarnrc", ".yarnrc.yml", ".credentials.yaml");

    /// The private keys an SSH client keeps, matched as prefixes.
    ///
    /// A key file is named for its algorithm and then carries whatever extension the tool that made
    /// it chose, so `id_rsa` and `id_rsa.pub` are one rule and a list of exact names would miss both.
    /// `id_ed25519` with no extension at all is the spelling that used to escape: the export side
    /// knew only `id_rsa` here.
    static final List<String> CREDENTIAL_KEY_PREFIXES = List.of(
            "id_rsa", "id_ed25519", "id_ecdsa", "id_ed448");

    /// Extensions that are key material rather than content, refused by both sides.
    static final List<String> CREDENTIAL_EXTENSIONS = List.of(
            ".key", ".pem", ".p12", ".pfx", ".jks", ".keystore");

    /// Shapes that are credentials whatever they are called, refused by both sides.
    ///
    /// A file named after credentials is one rule rather than a list of spellings: `credentials.yaml`
    /// and `credentials.yml` are the same file to whatever reads it, and the import side knew only
    /// the first. The export side's own shapes are [DENY_FILE_NAMES], and they are the ones a profile
    /// may legitimately hold — `*token*` in a name is a credential to publish and ordinary prose in a
    /// package.
    private static final List<Pattern> CREDENTIAL_SHAPES = List.of(
            Pattern.compile("^credentials.*\\.ya?ml$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\.credentials$", Pattern.CASE_INSENSITIVE));

    /// Names excluded anywhere in a path.
    ///
    /// The credential names plus the things a profile accumulates and a pack has no business
    /// carrying: an installed tree, its lock files, and the instance's own settings. `.env` and
    /// `.credentials.yaml` are in both lists, and the credential list is named first so the reason
    /// reported for them is unchanged.
    private static final List<String> DENY_NAMES = concat(CREDENTIAL_NAMES,
            "node_modules", "dist", "build", "coverage", ".cache", "cordis.yml", "manifest.json",
            "package-lock.json", "yarn.lock", ".anonymous-user-id", "settings.yaml", ".dshpkcfg");

    /// Extensions that are credentials rather than content.
    ///
    /// The shared list plus the certificates and signatures the export side knows and the import
    /// side does not: a publication carrying a `.crt` or a signature file leaks as surely as one
    /// carrying a key, while writing one into a profile is not a leak at all.
    private static final List<String> DENY_EXTENSIONS = concat(CREDENTIAL_EXTENSIONS,
            ".crt", ".der", ".asc");

    /// Names that are credentials whatever they are called, and that only the export side refuses.
    private static final List<Pattern> DENY_FILE_NAMES = List.of(
            Pattern.compile("^\\.env.*"),
            Pattern.compile("^secrets.*\\.(json|ya?ml)$", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*token.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*api[_-]?key.*", Pattern.CASE_INSENSITIVE));

    /// Reports whether a file name is credential material, whichever side is asking.
    ///
    /// The one place both directions ask the question. It is a predicate rather than a list because
    /// what is refused is not only a spelling: a private key is recognised by its algorithm, a key
    /// file by its extension, and a credential file by the word in its name — and the reason a side
    /// refuses it is that side's own business, which is why the export side's reasons for these are
    /// still reported one tier at a time.
    ///
    /// @param name the file's name
    /// @return whether it is credential material
    static boolean isCredentialName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String credential : CREDENTIAL_NAMES) {
            if (lower.equals(credential)) {
                return true;
            }
        }
        for (String prefix : CREDENTIAL_KEY_PREFIXES) {
            if (lower.startsWith(prefix)) {
                return true;
            }
        }
        for (String extension : CREDENTIAL_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        for (Pattern shape : CREDENTIAL_SHAPES) {
            if (shape.matcher(name).matches()) {
                return true;
            }
        }
        return false;
    }

    /// Archives, which are never nested inside a pack.
    private static final Pattern DENY_ARCHIVE = Pattern.compile(
            ".*\\.(tgz|tar\\.gz|zip|dspack)$", Pattern.CASE_INSENSITIVE);

    /// Runtime and baseline directories, which belong to the home rather than to a profile.
    private static final List<String> DENY_PREFIXES = List.of(
            "attachments/", "profiles/web/", "profiles/headless/", "skills/.system/");

    /// What counts as a version string.
    ///
    /// Used for the extra harness versions a pack claims to have been measured against: the manifest
    /// spec asks for version strings there, and a number that is not one would make the install
    /// decision compare a pin against something no version manager can name.
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)*(-[0-9A-Za-z.-]+)?(\\+[0-9A-Za-z.-]+)?");

    /// Joins one deny list to the names that extend it.
    ///
    /// @param shared the list both sides refuse
    /// @param more   the names this side refuses as well
    /// @return the joined list
    private static List<String> concat(List<String> shared, String... more) {
        List<String> joined = new ArrayList<>(shared);
        joined.addAll(List.of(more));
        return List.copyOf(joined);
    }

    private DshPackForge() {
    }

    /// What a pack says about itself.
    ///
    /// @param name        the pack's identifier, which has to be kebab-case
    /// @param version     the pack's own version, which has to be a version
    /// @param displayName what people see, or an empty string for the name
    /// @param description what it is for, or an empty string
    /// @param author      who made it, or an empty string
    /// @param dshVersions the other harness versions this pack was measured against, or an empty
    ///                    list for the instance's own version alone
    public record Options(String name, String version, String displayName, String description,
                          String author, List<String> dshVersions) {

        /// Returns options that describe an instance without being told anything.
        ///
        /// @param instance the instance
        /// @return the options
        public static Options of(DshInstance instance) {
            String name = kebab(instance.id());
            return new Options(name, "1.0.0", instance.id(), "", "", List.of());
        }

        /// Turns anything into a kebab-case identifier.
        ///
        /// The specification requires one, and an instance's name is whatever somebody typed.
        ///
        /// @param text the text
        /// @return the identifier
        public static String kebab(String text) {
            String cleaned = text == null ? "" : text.trim().toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-+)|(-+$)", "");
            return cleaned.isEmpty() ? "pack" : cleaned;
        }
    }

    /// One file the scan found, or refused.
    ///
    /// @param relative the path inside the profile, always with `/` separators
    /// @param size     its size in bytes
    public record Entry(String relative, long size) {
    }

    /// What a scan found.
    ///
    /// @param files    the files that travel, sorted by path
    /// @param excluded what was left out and why
    public record Scan(List<Entry> files, Map<String, String> excluded) {
    }

    /// What an export wrote.
    ///
    /// @param files    how many files travelled
    /// @param excluded how many were left out
    /// @param bytes    the container's size
    /// @param sha256   the container's digest, which is what a publisher writes beside it
    public record Result(int files, int excluded, long bytes, String sha256) {
    }

    /// Scans a profile for what a pack carries.
    ///
    /// @param profile the profile directory
    /// @return what travels and what does not
    /// @throws DshException when the directory cannot be read
    public static Scan scan(Path profile) throws DshException {
        return scan(profile, null);
    }

    /// Scans a home for the content a profile pack may carry beside its profile.
    ///
    /// The same scan, minus the home's own `profiles/`: this launcher runs an instance per profile
    /// under `$DSH_HOME/profiles/`, none of which is this pack's to distribute. Everything else the
    /// security filter allows — the global `skills/`, the `.agent-presets/`, `AGENTS.md` — is a
    /// candidate, and which of them actually travels is the caller's whitelist.
    ///
    /// @param home the `$DSH_HOME` directory
    /// @return what could travel and what could not
    /// @throws DshException when the directory cannot be read
    public static Scan scanHome(Path home) throws DshException {
        return scan(home, HOME_PROFILES);
    }

    /// Scans a root, leaving one prefix out.
    ///
    /// @param root the directory
    /// @param skip a prefix that never travels, or `null`
    /// @return what travels and what does not
    /// @throws DshException when the directory cannot be read
    private static Scan scan(Path root, @Nullable String skip) throws DshException {
        List<Entry> files = new ArrayList<>();
        Map<String, String> excluded = new LinkedHashMap<>();

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    if (directory.equals(root)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (Files.isSymbolicLink(directory)) {
                        // A link is never followed: it can leave the profile, and it can loop.
                        excluded.put(relative(root, directory), "symlink");
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String rel = relative(root, directory) + "/";
                    if (skip != null && rel.startsWith(skip)) {
                        excluded.put(rel, "profile");
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    String reason = exclusionOf(rel);
                    if (reason != null) {
                        excluded.put(rel, reason);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    String rel = relative(root, file);
                    if (Files.isSymbolicLink(file)) {
                        excluded.put(rel, "symlink");
                        return FileVisitResult.CONTINUE;
                    }
                    String reason = exclusionOf(rel);
                    if (reason != null) {
                        excluded.put(rel, reason);
                        return FileVisitResult.CONTINUE;
                    }
                    files.add(new Entry(rel, attributes.size()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) {
                    // Unreadable is the same as absent for a pack's purposes.
                    excluded.put(relative(root, file), "unreadable");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new DshException("Failed to scan " + root, e);
        }

        files.sort((left, right) -> left.relative().compareTo(right.relative()));
        return new Scan(List.copyOf(files), Map.copyOf(excluded));
    }

    /// Reports why a path does not travel, or `null` when it does.
    ///
    /// @param relative the path inside the profile
    /// @return the reason, or `null`
    static @Nullable String exclusionOf(String relative) {
        String path = relative.endsWith("/") ? relative.substring(0, relative.length() - 1) : relative;
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (DENY_NAMES.contains(segment)) {
                return "name";
            }
        }

        String name = segments.length == 0 ? path : segments[segments.length - 1];
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : DENY_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return "extension";
            }
        }
        // Everything both sides refuse — a private key's name, a key extension, a file named after
        // credentials — and then the shapes only this side knows.
        if (isCredentialName(name)) {
            return "credentials";
        }
        for (Pattern pattern : DENY_FILE_NAMES) {
            if (pattern.matcher(name).matches()) {
                return "credentials";
            }
        }
        if (DENY_ARCHIVE.matcher(path).matches()) {
            return "archive";
        }
        for (String prefix : DENY_PREFIXES) {
            if ((path + "/").startsWith(prefix)) {
                return "runtime";
            }
        }
        return null;
    }

    /// Writes an instance as a `.dspack`, carrying only the profile.
    ///
    /// @param instance the instance
    /// @param target   the archive to create
    /// @param options  what it should say about itself
    /// @param onStage  receives progress lines, or `null`
    /// @return what was written
    /// @throws DshException when the profile cannot be read or written
    public static Result export(DshInstance instance, Path target, Options options,
                               @Nullable Consumer<String> onStage) throws DshException {
        return export(instance, target, options, List.of(), onStage);
    }

    /// Writes an instance as a `.dspack`.
    ///
    /// Both of the container's places are used the way the specification says. The files that
    /// describe the profile rather than override anything — `package.json`, the lock file, the
    /// workspace file — are written at the archive root, which is where the installer copies them
    /// **before** it resolves the dependencies; everything else goes under `overrides/`, which lands
    /// afterwards. Writing the first group under `overrides/` as well would be a pack that installs
    /// and then boots with none of its plugins: the resolve that installs them ran while the profile
    /// was still empty, because the manifest it needed had not been copied yet.
    ///
    /// `home/` carries what is **not** the profile's own: the global `skills/`, the
    /// `.agent-presets/`, `AGENTS.md`. The specification permits it and the installer lands it at the
    /// `$DSH_HOME` root, but nothing travels there unless the caller names it in `homeInclude` —
    /// the home is the machine's, not the profile's, and a pack that swept it up unasked would
    /// publish things its author never chose to publish. What the whitelist asks for is still put
    /// through the security filter below, so naming a credential does not carry it.
    ///
    /// @param instance    the instance
    /// @param target      the archive to create
    /// @param options     what it should say about itself
    /// @param homeInclude the home-relative paths to carry, or empty for none
    /// @param onStage     receives progress lines, or `null`
    /// @return what was written
    /// @throws DshException when the profile cannot be read or written
    public static Result export(DshInstance instance, Path target, Options options,
                               Collection<String> homeInclude,
                               @Nullable Consumer<String> onStage) throws DshException {
        Path profile = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        if (!Files.isDirectory(profile)) {
            throw new DshException("The instance has no profile at " + profile);
        }

        report(onStage, "Scanning " + profile.getFileName());
        Scan scan = scan(profile);
        report(onStage, scan.files().size() + " file(s) travel, " + scan.excluded().size() + " left out");

        Path home = instance.homeDirectory();
        List<Entry> fromHome = selectHome(home, homeInclude, onStage);

        JsonObject manifest = buildManifest(instance, options, onStage);

        Path parent = target.toAbsolutePath().getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
                // The marker comes first: a loader reads it before anything else.
                put(zip, MARKER, "{\"format\":\"dspack\",\"version\":" + CONTAINER_VERSION + "}");
                put(zip, MANIFEST, JsonUtils.GSON.toJson(manifest));
                for (Entry entry : scan.files()) {
                    // The two places are not a matter of taste: the root files are copied before the
                    // dependency install and the overrides after it, so a manifest under
                    // `overrides/` arrives too late to be installed from. One predicate decides
                    // which is which, and the installer reads the archive with the same one.
                    copy(zip, profile.resolve(entry.relative()),
                            DshPackInstaller.isMachineFile(entry.relative())
                                    ? entry.relative()
                                    : OVERRIDES + entry.relative());
                }
                for (Entry entry : fromHome) {
                    copy(zip, home.resolve(entry.relative()), HOME + entry.relative());
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to write " + target, e);
        }

        long size = sizeOf(target);
        String digest = sha256(target);
        report(onStage, "Wrote " + target.getFileName() + " (" + (size / 1024) + " KiB, sha256 "
                + digest.substring(0, 12) + "…)");
        LOG.info("Wrote a DSH-PackForge pack of " + instance.id() + " to " + target);
        return new Result(scan.files().size() + fromHome.size(), scan.excluded().size(), size, digest);
    }

    /// Works out which home-level files travel, and says what does not.
    ///
    /// A path the caller named but the scan did not offer is reported rather than silently dropped:
    /// the person selected it, and the two reasons it can be missing — it is not there any more, or
    /// the security filter refuses it — are both things they would want to know.
    ///
    /// @param home    the home
    /// @param include the home-relative paths the caller selected
    /// @param onStage receives progress lines, or `null`
    /// @return the files that travel, in path order
    /// @throws DshException when the home cannot be read
    private static List<Entry> selectHome(Path home, Collection<String> include,
                                          @Nullable Consumer<String> onStage) throws DshException {
        if (include.isEmpty()) {
            return List.of();
        }
        Scan scan = scanHome(home);
        Set<String> wanted = new LinkedHashSet<>();
        for (String path : include) {
            if (path != null && !path.isBlank()) {
                wanted.add(path.replace('\\', '/').replaceAll("^/+", ""));
            }
        }
        List<Entry> chosen = new ArrayList<>();
        for (Entry entry : scan.files()) {
            if (wanted.remove(entry.relative())) {
                chosen.add(entry);
            }
        }
        if (!wanted.isEmpty()) {
            report(onStage, "Not carried from the home, because they are not there or the filter"
                    + " leaves them out: " + wanted);
        }
        report(onStage, chosen.size() + " file(s) from the home travel");
        return List.copyOf(chosen);
    }

    /// Builds the v5 manifest for an instance.
    ///
    /// @param instance the instance
    /// @param options  what it should say about itself
    /// @param onStage  receives progress lines, or `null`
    /// @return the manifest
    /// @throws DshException when a dependency cannot be pinned to an exact version
    static JsonObject buildManifest(DshInstance instance, Options options,
                                    @Nullable Consumer<String> onStage) throws DshException {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("manifestVersion", MANIFEST_VERSION);
        manifest.addProperty("type", "profile");
        manifest.addProperty("name", Options.kebab(options.name()));
        manifest.addProperty("version", options.version() == null || options.version().isBlank()
                ? "1.0.0" : options.version().trim());
        if (options.displayName() != null && !options.displayName().isBlank()) {
            manifest.addProperty("displayName", options.displayName().trim());
        }
        if (options.description() != null && !options.description().isBlank()) {
            manifest.addProperty("description", options.description().trim());
        }
        if (options.author() != null && !options.author().isBlank()) {
            manifest.addProperty("author", options.author().trim());
        }
        // Exact, because the specification requires it: a pack is a reproducible snapshot.
        manifest.addProperty("dshVersion", instance.version());
        // And the set the author measured it against, which is what an importer chooses from when
        // the pinned version is not on the machine. The pinned version comes first and is always
        // there: the specification requires it to be a member, and a set that omitted it would
        // describe a pack nobody could install.
        manifest.add("dshVersions", measuredVersions(instance, options, onStage));
        manifest.addProperty("profileName", instance.profile());

        JsonArray bundles = new JsonArray();
        DshPluginInstaller.readBundles(instance.homeDirectory(), instance.profile())
                .forEach(bundles::add);
        manifest.add("bundles", bundles);

        JsonObject dependencies = new JsonObject();
        // Named before the loop rather than when each one is reached. The loop refuses the first plugin
        // it cannot name, so a pack with two of them would report one name and stop, and the second
        // would be found only by exporting again — and this is the last moment at which the person can
        // still do something about it: install a published version, or take the plugin out.
        for (DshModpacks.LocalPlugin local : DshModpacks.localPluginsOf(instance)) {
            if (local.development()) {
                report(onStage, local.name() + " is a plugin under development — installed from a"
                        + " directory outside this instance — and a pack of published sources cannot"
                        + " carry it. Install a published version instead, or export a pack this"
                        + " launcher reads (.hdslp) to carry the files themselves.");
            }
        }
        for (Map.Entry<String, String> entry
                : DshPluginInstaller.readDependencies(instance.homeDirectory(), instance.profile())
                        .entrySet()) {
            if (DshModpacks.isLocalSpec(entry.getValue())) {
                // A specification this instance installed from a file. This format names sources, and
                // a file on this machine is not one — unless the registry publishes the same version,
                // in which case the source it names is the one the file was built from and the pack
                // stays a pack of published sources.
                dependencies.addProperty(entry.getKey(),
                        publishedVersionOf(instance, entry.getKey(), entry.getValue()));
                report(onStage, entry.getKey() + " was installed from a file and is published, so the"
                        + " pack names its published version");
                continue;
            }
            dependencies.addProperty(entry.getKey(), pin(entry.getKey(), entry.getValue(), onStage));
        }
        manifest.add("dependencies", dependencies);
        return manifest;
    }

    /// Builds the set of harness versions the pack says it was measured against.
    ///
    /// The instance's own version first, then whatever the author added, deduplicated. A string that
    /// is not a version is left out and said so, rather than written: the manifest spec asks for
    /// version strings there, and an importer compares them against what it has installed, which no
    /// other spelling can match.
    ///
    /// @param instance the instance the pack is of
    /// @param options  what the author said about it
    /// @param onStage  receives progress lines, or `null`
    /// @return the versions, first one first
    private static JsonArray measuredVersions(DshInstance instance, Options options,
                                              @Nullable Consumer<String> onStage) {
        Set<String> versions = new LinkedHashSet<>();
        versions.add(instance.version());
        if (options.dshVersions() != null) {
            for (String version : options.dshVersions()) {
                String cleaned = version == null ? "" : version.trim();
                if (cleaned.isEmpty() || versions.contains(cleaned)) {
                    continue;
                }
                if (!VERSION.matcher(cleaned).matches()) {
                    report(onStage, cleaned + " is not a version, so the pack does not claim to"
                            + " have been measured against it");
                    continue;
                }
                versions.add(cleaned);
            }
        }
        JsonArray array = new JsonArray();
        versions.forEach(array::add);
        return array;
    }

    /// Returns the published version of a plugin an instance installed from a file.
    ///
    /// @param instance the instance the plugin belongs to
    /// @param name     the dependency name
    /// @param declared what the profile declares for it
    /// @return the published version
    /// @throws DshException when the registry does not publish it, which this format cannot express
    private static String publishedVersionOf(DshInstance instance, String name, String declared)
            throws DshException {
        Path profileDirectory = instance.homeDirectory().resolve("profiles").resolve(instance.profile());
        // A plugin somebody is working on is not a plugin the registry could have published a source
        // for: what the profile holds is a directory outside this instance, and the file this instance
        // kept is not missing — it never existed. Saying "the registry does not publish that version"
        // here would describe the other case, and describe it wrongly.
        if (DshPluginBundle.namesADirectory(declared, profileDirectory)
                || DshPluginBundle.isLinked(profileDirectory, name)) {
            throw new DshException("The plugin " + name + " is one under development: the profile"
                    + " installed it from a directory outside this instance (" + declared + "), so its"
                    + " files belong to whoever is working on it and this format, which names published"
                    + " sources, cannot carry it. Export a pack this launcher reads (.hdslp) to carry the"
                    + " files themselves, or install a published version first.");
        }
        DshPluginBundle.Payload payload = DshPluginBundle.locate(profileDirectory, name, declared);
        if (payload != null
                && DshPackageRegistry.availability(name, payload.version())
                        == DshPackageRegistry.Availability.PUBLISHED) {
            return payload.version();
        }
        throw new DshException("The plugin " + name + " was installed from a file this instance keeps ("
                + declared + "), and the registry does not publish that version, so a pack of published"
                + " sources cannot name it. Export a pack this launcher reads (.hdslp) to carry the files"
                + " themselves, or install a published version first.");
    }

    /// Pins a dependency to what the specification requires.
    ///
    /// npm versions are already exact in a profile. A git source names a branch or a tag, and a
    /// pack has to name the commit instead — a branch moves, and a pack that moved with it would
    /// not be a snapshot of anything.
    ///
    /// @param name     the package
    /// @param declared what the profile declares
    /// @param onStage  receives progress lines, or `null`
    /// @return the pinned version or commit
    /// @throws DshException when a git source cannot be resolved to a commit
    static String pin(String name, String declared, @Nullable Consumer<String> onStage) throws DshException {
        if (declared == null || declared.isBlank()) {
            throw new DshException("The plugin " + name + " has no version to pin");
        }
        if (!declared.startsWith("github:") && !declared.startsWith("git+")
                && !declared.contains("github.com/")) {
            return declared.trim();
        }

        String repository = declared.trim();
        if (repository.startsWith("github:")) {
            repository = "https://github.com/" + repository.substring("github:".length());
        }
        report(onStage, "Resolving " + name + " to a commit");
        try {
            DshCommand.Result result = DshCommand.run(
                    List.of("git", "ls-remote", repository), null, null);
            if (result.exitCode() != 0) {
                throw new DshException("git could not list " + repository + ": "
                        + String.join("\n", result.output()));
            }
            for (String line : result.output()) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2 && parts[0].length() >= 40) {
                    return parts[0];
                }
            }
            throw new DshException("No branch or tag was found at " + repository);
        } catch (IOException e) {
            throw new DshException("Failed to run git for " + name, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("Resolving " + name + " was interrupted", e);
        }
    }

    /// Returns the path of a file inside a root, with `/` separators.
    ///
    /// @param root the root
    /// @param file the file
    /// @return the relative path
    private static String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }

    /// Writes one entry of text.
    ///
    /// @param zip  the archive
    /// @param name the entry's name
    /// @param body the entry's text
    /// @throws IOException when the archive cannot be written
    private static void put(ZipOutputStream zip, String name, String body) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(body.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /// Copies one file into the archive.
    ///
    /// @param zip  the archive
    /// @param file the file
    /// @param name the entry's name
    /// @throws IOException when the file cannot be read or the archive written
    private static void copy(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        try (InputStream input = Files.newInputStream(file)) {
            input.transferTo(zip);
        }
        zip.closeEntry();
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

    /// Returns a file's SHA-256 digest, which is what a publisher writes beside a pack.
    ///
    /// @param file the file
    /// @return the digest in lower-case hexadecimal
    /// @throws DshException when the file cannot be read
    static String sha256(Path file) throws DshException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder text = new StringBuilder();
            for (byte value : digest.digest()) {
                text.append(String.format("%02x", value));
            }
            return text.toString();
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            throw new DshException("Failed to hash " + file, e);
        }
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
}
