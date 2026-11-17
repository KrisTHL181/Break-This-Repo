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
package org.jackhuang.hmcl.web.zcode;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.Tombstones;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jackhuang.hmcl.util.platform.UserHome;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Creates, enumerates, edits and removes [ZcodeInstance]s, and injects the
/// provider credentials ZCode picks up on its own.
///
/// Each instance is stored as `zcode/instances/<id>/instance.json` under the
/// server data directory — the same manifest style as
/// [org.jackhuang.hmcl.dsh.DshInstanceManager], in a directory tree of its own
/// so the experimental category never touches dsh state. The manager itself is
/// bound to the instances root at construction time because the root comes
/// from the server config rather than from the launcher's home directory.
///
/// An instance directory also holds everything the process owns at runtime:
/// `data/` (passed to ZCode as `ZCODE_DATA_BASE_DIR`: the provider config and
/// credentials), `home/` (passed as `HOME`: the session database lives at
/// `<home>/.zcode/cli/db/db.sqlite`, see [#prepareHome]) and `logs/zcode.log`
/// (the process's merged stdout and stderr). The default workspace of a new
/// instance lives next to the instances instead, under
/// `zcode/workspaces/<id>-<name slug>` — a project directory the operator can
/// see, keep and reuse, rather than state hidden inside the instance. The
/// `<instance>/workspace/` directory is only still read as the fallback for
/// manifests written before that move.
///
/// **Experimental.** Credential injection writes the personal provider config
/// ZCode reads from `<ZCODE_DATA_BASE_DIR>/.zcode/v2/provider_config.json`.
/// That file's shape is reverse-engineered from the ZCode v3.14.3 sources
/// (`packages/provider-node/src/provider-config-file-codec.ts` and
/// `packages/provider/src/config/rule-data-schema.ts`) and is **not a
/// documented, stable contract** — a future ZCode can rename or restructure it,
/// in which case the injection silently stops doing anything. Best effort,
/// by design.
@NotNullByDefault
public final class ZcodeInstanceManager {

    /// The manifest file inside an instance directory.
    public static final String MANIFEST_NAME = "instance.json";

    /// The directory inside an instance directory that becomes the process's
    /// `ZCODE_DATA_BASE_DIR`; ZCode keeps its own state at `.zcode/` below it.
    public static final String DATA_DIR_NAME = "data";

    /// The directory inside an instance directory that becomes the process's
    /// `HOME`; ZCode's session database lives at `.zcode/cli/db/db.sqlite`
    /// below it.
    public static final String HOME_DIR_NAME = "home";

    /// The default workspace directory of old instances, kept only so manifests
    /// written before the move to [workspacesRoot] still resolve.
    public static final String WORKSPACE_DIR_NAME = "workspace";

    /// The directory under the parent of [root] holding every default
    /// workspace: `zcode/workspaces/`.
    public static final String WORKSPACES_DIR_NAME = "workspaces";

    /// The process log: merged stdout and stderr, truncated at every launch.
    public static final String LOG_FILE_NAME = "zcode.log";

    /// The provider config this panel writes when an API key is configured,
    /// relative to the instance's data directory.
    public static final String PROVIDER_CONFIG_FILE = ".zcode/v2/provider_config.json";

    /// The entries of the panel's own home a new instance home links in.
    ///
    /// They are how `git`, `ssh` and `npm` authenticate. Before the instance got
    /// a home of its own they were simply there — `HOME` was the panel's — so
    /// without this an agent that could push yesterday cannot push today, and
    /// nothing in the panel would say why.
    ///
    /// Linked rather than copied on purpose: a credential rotation still reaches
    /// every instance, and a private key never ends up duplicated inside a
    /// volume the operator may hand to somebody else. Missing entries are not an
    /// error — most deployments have none of them.
    private static final List<String> INHERITED_HOME_ENTRIES = List.of(
            ".gitconfig", ".git-credentials", ".netrc", ".npmrc", ".ssh");

    private static final SecureRandom RANDOM = new SecureRandom();

    /// How much of a name a default workspace directory keeps.
    private static final int SLUG_MAX_LENGTH = 24;

    private final Path root;

    /// Binds a manager to the `zcode/instances` directory.
    ///
    /// @param root the instances root
    public ZcodeInstanceManager(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /// The directory every instance lives under.
    ///
    /// @return the instances root
    public Path root() {
        return root;
    }

    /// The directory holding the default workspace of every instance.
    ///
    /// It is a sibling of the instances root, so a workspace outlives the
    /// instance that created it: deleting an instance removes its manifest,
    /// its data and its log, never the project the agent worked in.
    ///
    /// @return the workspaces root, `<zcode>/workspaces`
    public Path workspacesRoot() {
        Path parent = root.getParent();
        return (parent == null ? root : parent).resolve(WORKSPACES_DIR_NAME);
    }

    /// Lists every readable instance, newest first.
    ///
    /// Directories without a manifest, or with an unreadable one, are skipped
    /// rather than failing the whole listing.
    ///
    /// @return the known instances
    public List<ZcodeInstance> list() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<ZcodeInstance> instances = new ArrayList<>();
        try (Stream<Path> entries = Files.list(root)) {
            for (Path directory : entries.filter(Files::isDirectory).toList()) {
                ZcodeInstance instance = read(directory);
                if (instance != null) {
                    instances.add(instance);
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to enumerate ZCode instances in " + root, e);
        }
        instances.sort(Comparator.comparingLong(ZcodeInstance::createdAt).reversed());
        return instances;
    }

    /// Finds an instance by id.
    ///
    /// @param id the instance id
    /// @return the instance, or `null` when it does not exist or is unreadable
    public @Nullable ZcodeInstance find(String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\")
                || id.equals(".") || id.equals("..")) {
            return null;
        }
        return read(root.resolve(id));
    }

    /// Creates and persists a new instance.
    ///
    /// The instance receives a random id, a random access token, its own
    /// `data/` directory, a workspace under [workspacesRoot], and — when an API
    /// key is given — its first provider config. The workspace is created eagerly
    /// so a later launch cannot fail merely because the directory is missing.
    ///
    /// @param name    the display name; must not be blank
    /// @param baseUrl the provider base URL, or `null`/blank for the default
    /// @param apiKey  the provider API key, or `null`/blank for none
    /// @return the created instance
    /// @throws ZcodeException when the name is blank or the instance cannot be written
    public ZcodeInstance create(String name, @Nullable String baseUrl, @Nullable String apiKey)
            throws ZcodeException {
        return create(name, baseUrl, apiKey, null);
    }

    /// Creates an instance that works in a chosen directory, or in a default one.
    ///
    /// A ZCode instance usually points at a project the operator already has; asking them
    /// to move or copy it into the instance's own directory is the kind of thing that makes
    /// a tool feel like it was built for somebody else. The path is taken as given (made
    /// absolute), created when missing, and recorded — the launch and the terminal both
    /// read it back, so an instance has exactly one answer for "where does it work".
    ///
    /// With no path given the instance gets a workspace of its own under
    /// [workspacesRoot], named `<id>-<name slug>`, created right away and recorded in the
    /// manifest like an operator-provided one. `<instance>/workspace/` is not used for new
    /// instances any more; it stays as the fallback for manifests that predate the move.
    ///
    /// @param name      the display name; must not be blank
    /// @param baseUrl   the provider base URL, or `null`/blank for the default
    /// @param apiKey    the provider API key, or `null`/blank for none
    /// @param workspace the directory to work in, or `null` for a default workspace
    /// @return the created instance
    /// @throws ZcodeException when the name is blank, the path is not absolute, or the
    ///                        instance cannot be written
    public ZcodeInstance create(String name, @Nullable String baseUrl, @Nullable String apiKey,
                               @Nullable Path workspace) throws ZcodeException {
        return create(name, baseUrl, apiKey, workspace, null);
    }

    /// Creates an instance pinned to an upstream ref. The floor and the used
    /// credential format start empty: they move only after a successful launch.
    public ZcodeInstance create(String name, @Nullable String baseUrl, @Nullable String apiKey,
                               @Nullable Path workspace, @Nullable String version) throws ZcodeException {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new ZcodeException("A ZCode instance needs a name");
        }

        String id = newTokenLike(9);
        Path directory = root.resolve(id);
        Path workspacePath = defaultWorkspaceFor(id, normalized);
        if (workspace != null) {
            if (!workspace.isAbsolute()) {
                throw new ZcodeException("工作区必须是绝对路径：" + workspace);
            }
            workspacePath = workspace.toAbsolutePath().normalize();
            if (Files.exists(workspacePath) && !Files.isDirectory(workspacePath)) {
                throw new ZcodeException("工作区不是目录：" + workspacePath);
            }
        }
        ZcodeInstance instance = new ZcodeInstance(id, normalized,
                workspacePath.toString(),
                baseUrl, apiKey, newTokenLike(32), 0,
                version == null || version.isBlank() ? null : version.trim(),
                null, 0, 0, System.currentTimeMillis());

        try {
            Files.createDirectories(dataDirectoryOf(directory));
            Files.createDirectories(workspacePath);
            Files.createDirectories(logDirectoryOf(directory));
        } catch (IOException e) {
            throw new ZcodeException("Failed to create " + directory, e);
        }

        write(instance);
        prepareHome(instance);
        try {
            writeProviderConfig(instance);
        } catch (ZcodeException e) {
            LOG.warning("Could not inject the provider config for " + id, e);
        }
        LOG.info("Created experimental ZCode instance " + id + " (" + normalized + ")");
        return instance;
    }

    /// Overwrites an existing instance definition.
    ///
    /// @param instance the instance to persist
    /// @throws ZcodeException when the manifest cannot be written
    public void update(ZcodeInstance instance) throws ZcodeException {
        if (!Files.isDirectory(instanceDirectory(instance.id()))) {
            throw new ZcodeException("ZCode instance " + instance.id() + " does not exist");
        }
        write(instance);
    }

    /// Removes an instance and everything it owns.
    ///
    /// Unlike a shared dsh home, a ZCode instance keeps its manifest, data and
    /// log inside its own directory, so deleting the directory deletes the whole
    /// instance. A default workspace is **not** one of those things: it lives
    /// under [workspacesRoot] and is left untouched, because it holds the
    /// operator's project rather than the instance's state.
    ///
    /// The directory is renamed out of the way before it is emptied (see
    /// [FileUtils#deleteByTombstone]): the listing keys on the directory name, so an
    /// instance whose log another program still holds is gone from the list instead of
    /// lingering as a row that cannot start.
    ///
    /// @param id the instance id
    /// @throws ZcodeException when the instance does not exist or cannot be removed
    public void delete(String id) throws ZcodeException {
        ZcodeInstance instance = find(id);
        if (instance == null) {
            throw new ZcodeException("ZCode instance " + id + " does not exist");
        }
        Path directory = instanceDirectory(id);
        if (!directory.normalize().startsWith(root)) {
            throw new ZcodeException("Refusing to delete " + directory + " because it is outside the ZCode instances directory");
        }
        // A process started by an earlier panel is not in this one's registry, and on
        // Windows a directory cannot be removed while any process has it as its current
        // directory — which is exactly how an instance's tool is started. Without this,
        // "delete" fails with an empty error while something invisible sits on the tree.
        int strays = org.jackhuang.hmcl.util.ProcessTree.killReferencing(directory);
        if (strays > 0) {
            LOG.info("Stopped " + strays + " process(es) still referencing " + directory);
        }
        Path leftover;
        try {
            leftover = FileUtils.deleteByTombstone(directory, ".deleting-" + System.currentTimeMillis());
        } catch (IOException e) {
            throw new ZcodeException("Failed to remove " + directory, e);
        }
        if (leftover != null) {
            LOG.warning("Could not delete " + directory + " completely; renamed it to "
                    + leftover.getFileName() + " and it will be swept later");
        }
        LOG.info("Removed experimental ZCode instance " + id);
    }

    /// Removes the tombstones a delete leaves behind.
    ///
    /// A directory that could not be deleted is renamed to `<id>.deleting-<millis>` (see
    /// [delete]), which takes the name away from the instance even when a file inside it is
    /// still held. All that is left to do is to try again — at startup, when whatever held
    /// them is most likely gone.
    ///
    /// What counts as a leftover is decided by [Tombstones#isSweepable], never by the suffix
    /// alone: an instance's directory is named after an id the user chose, so `v1.deleting-2`
    /// is a perfectly ordinary instance and sweeping it would delete it whole.
    public void sweepTombstones() {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> entries = Files.list(root)) {
            for (Path entry : entries.toList()) {
                if (Tombstones.isSweepable(entry, MANIFEST_NAME, ZcodeInstanceManager::recordedId)
                        && FileUtils.deleteDirectoryQuietly(entry)) {
                    LOG.info("Removed the leftover " + entry.getFileName());
                }
            }
        } catch (IOException e) {
            LOG.warning("Could not sweep the tombstones under " + root, e);
        }
    }

    /// The id a directory's manifest records, or `null` when it has none.
    ///
    /// @param directory the candidate instance directory
    /// @return the recorded id
    private static @Nullable String recordedId(Path directory) {
        ZcodeInstance instance = read(directory);
        return instance == null ? null : instance.id();
    }

    /// The directory holding an instance's manifest and runtime state.
    ///
    /// @param id the instance id
    /// @return the instance directory
    public Path instanceDirectory(String id) {
        return root.resolve(id);
    }

    /// The directory passed to the process as `ZCODE_DATA_BASE_DIR`.
    ///
    /// @param instance the instance
    /// @return the data directory
    public Path dataDirectory(ZcodeInstance instance) {
        return dataDirectoryOf(instanceDirectory(instance.id()));
    }
    /// The directory passed to the process as `HOME`.
    ///
    /// @param instance the instance
    /// @return the home directory
    public Path homeDirectory(ZcodeInstance instance) {
        return instanceDirectory(instance.id()).resolve(HOME_DIR_NAME);
    }

    /// Creates the instance's home when it is missing, links the panel's shared
    /// credentials into it, and hands it to the agent.
    ///
    /// **Why a home of its own.** The session database is the one piece of ZCode
    /// state that does not follow `ZCODE_DATA_BASE_DIR`: `getDefaultSessionDbPath()`
    /// uses `os.homedir()`, with no environment variable to redirect it (see
    /// `apps/zcode-cli/packages/adapters/src/storage/session-store/paths.ts`).
    /// While `HOME` was the panel's own, every instance's sessions landed in the
    /// container's `/home/hdsl` — the writable layer, not the data volume — so a
    /// rebuild (`docker compose up -d` after an image bump) emptied them while
    /// `/data` kept the task index that lists them, and the panel then answered
    /// `Session not found` for a conversation the operator could still see.
    ///
    /// Pointing `HOME` at the instance puts the database on the volume and gives
    /// each instance its own history — two instances no longer share one
    /// `~/.zcode/config.json` either, which is how one instance's provider key
    /// used to reach another's.
    ///
    /// Idempotent, so both the create path and every launch call it: an instance
    /// made by an older panel gets its home on the next start.
    ///
    /// @param instance the instance
    /// @return the home directory
    public Path prepareHome(ZcodeInstance instance) {
        Path home = homeDirectory(instance);
        boolean created = !Files.isDirectory(home);
        // The agent writes below its own home (the session database, logs, its
        // own tools), and it is a different uid from the panel in a container.
        Privileges.prepareAgentDirectory(home, Privileges.AGENT_SHARED_DIRECTORY);
        // …and under `data/` too: that is where ZCode keeps `tasks-index.sqlite` and the
        // provider state it rewrites, and the panel is the one that created the directory.
        // Handing over only `home/` left the other half root-owned, which is invisible on a
        // setgid volume and fatal on a plain bind mount.
        Privileges.prepareAgentDirectory(dataDirectory(instance), Privileges.AGENT_SHARED_DIRECTORY);
        if (!Files.isDirectory(home)) {
            return home; // prepareAgentDirectory logged why
        }
        // Only on a fresh home: a link the agent removed on purpose is not put
        // back on the next launch.
        if (created) {
            linkInheritedEntries(home);
        }
        return home;
    }

    /// Links the entries of [INHERITED_HOME_ENTRIES] the panel's home has into
    /// `home`, skipping the ones already there and the ones that do not exist.
    ///
    /// Best effort: Windows refuses `symlink` to an unprivileged process, and a
    /// missing credential is a working instance that cannot push, not a broken
    /// one.
    ///
    /// @param home the instance home
    private static void linkInheritedEntries(Path home) {
        Path panelHome = UserHome.directory();
        for (String name : INHERITED_HOME_ENTRIES) {
            Path source = panelHome.resolve(name);
            Path link = home.resolve(name);
            if (!Files.exists(source) || Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            if (source.equals(link)) {
                // A panel whose home *is* the instance home has nothing to link.
                continue;
            }
            try {
                Files.createSymbolicLink(link, source);
            } catch (IOException | UnsupportedOperationException e) {
                LOG.info("Could not link " + source + " into the ZCode home " + home
                        + " (" + e + "); the agent will run without it");
            }
        }
    }

    /// The working directory the process is launched with.
    ///
    /// @param instance the instance
    /// @return the workspace directory
    public Path workspaceDirectory(ZcodeInstance instance) {
        String recorded = instance.workspacePath();
        if (recorded != null && !recorded.isBlank()) {
            return Path.of(recorded);
        }
        return workspaceDirectoryOf(instanceDirectory(instance.id()));
    }

    /// The file the process's merged output is appended to.
    ///
    /// @param instance the instance
    /// @return the log file
    public Path logFile(ZcodeInstance instance) {
        return logDirectoryOf(instanceDirectory(instance.id())).resolve(LOG_FILE_NAME);
    }

    /// Reads the last lines of an instance's process log.
    ///
    /// @param instance the instance
    /// @param maxLines the most lines to return
    /// @return the trailing log lines, oldest first; empty when there is no log
    /// @throws IOException when the log exists but cannot be read
    public List<String> tailLog(ZcodeInstance instance, int maxLines) throws IOException {
        Path log = logFile(instance);
        if (!Files.isRegularFile(log)) {
            return List.of();
        }
        try (var lines = Files.lines(log, StandardCharsets.UTF_8)) {
            List<String> all = lines.toList();
            return List.copyOf(all.subList(Math.max(0, all.size() - maxLines), all.size()));
        }
    }

    /// Writes the personal provider config ZCode reads on startup, when the
    /// instance carries an API key. With no key the method writes nothing and
    /// keeps whatever ZCode created on its own.
    ///
    /// The file is written atomically (a sibling temp file, then an atomic
    /// move) so a crash mid-write cannot leave a truncated config behind a
    /// live process.
    ///
    /// The shape mirrors what ZCode v3.14.3 itself writes (see the class
    /// comment). One verified detail: `personalModelConfigRulesSchema` is
    /// `.strict()` and **requires both** `providerModelRules` and
    /// `manualProviderModelRules`, which is why an empty `providerModelRules`
    /// is spelled out instead of omitted.
    ///
    /// @param instance the instance
    /// @throws ZcodeException when a key is configured but the file cannot be written
    public void writeProviderConfig(ZcodeInstance instance) throws ZcodeException {
        String apiKey = instance.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }

        JsonObject access = new JsonObject();
        access.addProperty("type", "api-key");
        access.addProperty("apiKey", apiKey);

        JsonObject api = new JsonObject();
        api.addProperty("type", "openai-chat-completions");
        api.addProperty("baseUrl", instance.baseUrlOrDefault());

        JsonObject config = new JsonObject();
        config.addProperty("group", "standard-personal");
        config.add("access", access);
        config.add("api", api);
        config.add("personalModelIds", new JsonArray());
        config.addProperty("visibility", "visible");

        JsonObject rule = new JsonObject();
        rule.addProperty("providerId", "hdsl-injected");
        rule.add("config", config);

        JsonArray providerRules = new JsonArray();
        providerRules.add(rule);
        JsonObject providerConfigRules = new JsonObject();
        providerConfigRules.add("providerRules", providerRules);

        JsonObject modelConfigRules = new JsonObject();
        modelConfigRules.add("providerModelRules", new JsonArray());
        modelConfigRules.add("manualProviderModelRules", new JsonArray());

        JsonObject inner = new JsonObject();
        inner.add("providerConfigRules", providerConfigRules);
        inner.add("modelConfigRules", modelConfigRules);

        JsonObject body = new JsonObject();
        body.addProperty("schemaVersion", 1);
        body.add("config", inner);

        Path file = dataDirectory(instance).resolve(PROVIDER_CONFIG_FILE);
        try {
            Files.createDirectories(file.getParent());
            Path temp = Files.createTempFile(file.getParent(), file.getFileName().toString(), ".tmp");
            try {
                Files.writeString(temp, JsonUtils.GSON.toJson(body), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new ZcodeException("Failed to write the ZCode provider config for " + instance.id(), e);
        }
    }

    private Path dataDirectoryOf(Path directory) {
        return directory.resolve(DATA_DIR_NAME);
    }

    /// The workspace a new instance gets when the operator did not name one.
    ///
    /// `<workspaces root>/<id>-<name slug>`, so the directory is recognisable in
    /// a file manager; a name with nothing usable in it (Chinese, emoji) leaves
    /// just the id.
    private Path defaultWorkspaceFor(String id, String name) {
        String slug = slugOf(name);
        return workspacesRoot().resolve(slug.isEmpty() ? id : id + "-" + slug);
    }

    /// Turns a display name into something safe to use as a path segment:
    /// ASCII letters, digits, `.`, `_` and `-` survive, every other character
    /// (and every run of them) collapses into a single `-`, and the result is
    /// capped at [SLUG_MAX_LENGTH] characters with no `.` or `-` at either end.
    ///
    /// @param name the display name
    /// @return the slug; empty when the name contributes no usable characters
    private static String slugOf(String name) {
        StringBuilder slug = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '_' || c == '-') {
                slug.append(c);
            } else if (slug.length() > 0 && slug.charAt(slug.length() - 1) != '-') {
                slug.append('-');
            }
        }
        int start = 0;
        int end = Math.min(slug.length(), SLUG_MAX_LENGTH);
        while (start < end && (slug.charAt(start) == '.' || slug.charAt(start) == '-')) {
            start++;
        }
        while (end > start && (slug.charAt(end - 1) == '.' || slug.charAt(end - 1) == '-')) {
            end--;
        }
        return slug.substring(start, end);
    }

    private Path workspaceDirectoryOf(Path directory) {
        return directory.resolve(WORKSPACE_DIR_NAME);
    }

    private Path logDirectoryOf(Path directory) {
        return directory.resolve("logs");
    }

    /// Mints a random base64url token of the given byte length — the same
    /// construction [org.jackhuang.hmcl.web.auth.AuthService] uses for sessions.
    private static String newTokenLike(int bytes) {
        byte[] random = new byte[bytes];
        RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    /// Reads the manifest inside an instance directory.
    ///
    /// @param directory the candidate instance directory
    /// @return the instance, or `null` when there is no readable manifest
    private static @Nullable ZcodeInstance read(Path directory) {
        Path manifest = directory.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            ZcodeInstance instance = JsonUtils.fromJsonFile(manifest, ZcodeInstance.class);
            if (instance == null || instance.id() == null || instance.token() == null) {
                return null;
            }
            return instance;
        } catch (Exception e) {
            LOG.warning("Failed to read ZCode instance manifest " + manifest, e);
            return null;
        }
    }

    /// Writes an instance manifest.
    ///
    /// @param instance the instance to persist
    /// @throws ZcodeException when the manifest cannot be written
    private void write(ZcodeInstance instance) throws ZcodeException {
        try {
            Path directory = instanceDirectory(instance.id());
            Files.createDirectories(directory);
            JsonUtils.writeToJsonFile(directory.resolve(MANIFEST_NAME), instance);
        } catch (IOException e) {
            throw new ZcodeException("Failed to write the ZCode instance manifest for " + instance.id(), e);
        }
    }
}
