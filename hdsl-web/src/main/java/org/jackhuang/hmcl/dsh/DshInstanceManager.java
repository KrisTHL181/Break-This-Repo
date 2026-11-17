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

import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.Tombstones;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Creates, enumerates, edits and removes [DshInstance]s.
///
/// Each instance is stored as `instances/<id>/instance.json`. The instance id
/// doubles as the directory name, which is what makes an isolated `DSH_HOME`
/// trivially private: it lives at `instances/<id>/home`.
@NotNullByDefault
public final class DshInstanceManager {
    private DshInstanceManager() {
    }

    /// The manifest file inside an instance directory.
    public static final String MANIFEST_NAME = "instance.json";

    /// Listeners notified after an instance is created, changed or removed.
    ///
    /// The manager is the only writer of instance manifests, so announcing
    /// writes here is what lets the interface observe the folder instead of
    /// polling it. Callers are notified on the thread that made the change.
    private static final java.util.List<Runnable> CHANGE_LISTENERS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /// Registers a listener notified after every write.
    ///
    /// @param listener the listener
    public static void addChangeListener(Runnable listener) {
        CHANGE_LISTENERS.add(listener);
    }

    /// Removes a listener registered by [#addChangeListener].
    ///
    /// @param listener the listener
    public static void removeChangeListener(Runnable listener) {
        CHANGE_LISTENERS.remove(listener);
    }

    /// Tells every listener that the instances on disk have changed.
    private static void fireChanged() {
        for (Runnable listener : CHANGE_LISTENERS) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                LOG.warning("Instance change listener failed", e);
            }
        }
    }

    /// Lists every readable instance, newest first.
    ///
    /// Directories without a manifest, or with an unreadable one, are skipped
    /// rather than failing the whole listing.
    ///
    /// @return the known instances
    public static List<DshInstance> listIn(java.nio.file.Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<DshInstance> instances = new ArrayList<>();
        try (var children = Files.list(root)) {
            for (Path child : children.filter(Files::isDirectory).toList()) {
                DshInstance instance = read(child);
                if (instance != null) {
                    instances.add(instance);
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to read instances in " + root, e);
            return List.of();
        }
        instances.sort(java.util.Comparator.comparing(DshInstance::createdAt).reversed());
        return List.copyOf(instances);
    }

    public static List<DshInstance> list() {
        List<DshInstance> instances = new ArrayList<>();
        Path root = DshPaths.INSTANCES;
        if (!Files.isDirectory(root)) {
            return instances;
        }

        try (Stream<Path> entries = Files.list(root)) {
            for (Path directory : entries.filter(Files::isDirectory).toList()) {
                DshInstance instance = read(directory);
                if (instance != null) {
                    instances.add(instance);
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to enumerate instances in " + root, e);
        }

        instances.sort(Comparator.comparingLong(DshInstance::createdAt).reversed());
        return instances;
    }

    /// Finds an instance by id.
    ///
    /// @param id the instance id
    /// @return the instance, or `null` when it does not exist or is unreadable
    public static @Nullable DshInstance find(String id) {
        try {
            return read(DshPaths.instanceDirectory(id));
        } catch (DshException e) {
            return null;
        }
    }

    /// Creates and persists a new instance.
    ///
    /// The isolated home directory is created eagerly so that a later launch
    /// cannot fail merely because the directory is missing.
    ///
    /// @param id            the instance id, which must be unique and a safe path segment
    /// @param version       the installed version to pin
    /// @param profile       the profile to boot
    /// @param workspace     the directory sessions are scoped to
    /// @param homeMode      how the `DSH_HOME` is resolved
    /// @param customHome    the custom home, required when `homeMode` is [DshHomeMode#CUSTOM]
    /// @param arguments     extra command-line arguments
    /// @param environment   extra environment variables
    /// @return the created instance
    /// @throws DshException when the id is taken, the version is not installed,
    ///                       or the instance cannot be written
    public static DshInstance create(String id,
                                     String version,
                                     String profile,
                                     Path workspace,
                                     DshHomeMode homeMode,
                                     @Nullable Path customHome,
                                     List<String> arguments,
                                     Map<String, String> environment) throws DshException {
        return create(id, version, profile, workspace, DshNodeRuntime.SYSTEM, homeMode, customHome,
                arguments, environment);
    }

    /// Creates and persists a new instance pinned to a Node runtime.
    ///
    /// @param id            the instance id, which must be unique and a safe path segment
    /// @param version       the installed version to pin
    /// @param profile       the profile to boot
    /// @param workspace     the directory sessions are scoped to
    /// @param nodeRuntime   the Node runtime selection, or `null` for the system runtime
    /// @param homeMode      how the `DSH_HOME` is resolved
    /// @param customHome    the custom home, required when `homeMode` is [DshHomeMode#CUSTOM]
    /// @param arguments     extra command-line arguments
    /// @param environment   extra environment variables
    /// @return the created instance
    /// @throws DshException when the id is taken, the version is not installed,
    ///                       or the instance cannot be written
    public static DshInstance create(String id,
                                     String version,
                                     String profile,
                                     Path workspace,
                                     @Nullable String nodeRuntime,
                                     DshHomeMode homeMode,
                                     @Nullable Path customHome,
                                     List<String> arguments,
                                     Map<String, String> environment) throws DshException {
        if (find(id) != null) {
            throw new DshException("An instance named \"" + id + "\" already exists");
        }
        if (homeMode == DshHomeMode.CUSTOM && customHome == null) {
            throw new DshException("A custom DSH_HOME must be chosen for this instance");
        }

        // The port is reserved here, once, for the whole life of the instance:
        // the browser interface keys its state by origin, so an instance that
        // reached the same history through two ports would have two writers. A
        // surface that serves nothing needs none.
        int port = DshSurface.ofProfile(profile).isWeb() ? DshPorts.reserve(id) : 0;

        DshInstance instance = new DshInstance(id, version, profile,
                workspace.toAbsolutePath().normalize().toString(),
                nodeRuntime,
                homeMode,
                customHome == null ? null : customHome.toAbsolutePath().normalize().toString(),
                List.copyOf(arguments), Map.copyOf(environment),
                DshInstanceIcon.DEFAULT.id(), null, DshPortMode.AUTO, port, System.currentTimeMillis());

        Path directory = instance.instanceDirectory();
        try {
            // Never adopt a directory that is already there: it may hold the user's own files, and a
            // rename of it is not this instance's to make. `createDirectory` is atomic, unlike
            // `exists()` followed by `createDirectories()`, so two creates racing for one name cannot
            // both come away believing the directory is theirs.
            Files.createDirectories(directory.getParent());
            Files.createDirectory(directory);
            if (homeMode == DshHomeMode.ISOLATED) {
                Files.createDirectories(instance.homeDirectory());
            }
        } catch (FileAlreadyExistsException e) {
            throw new DshException("Cannot create instance \"" + id + "\": " + directory
                    + " already exists", e);
        } catch (IOException e) {
            throw new DshException("Failed to create " + directory, e);
        }

        write(instance);
        LOG.info("Created instance " + id + " (dsh " + version + ", home " + instance.homeMode() + ")");
        fireChanged();
        return instance;
    }

    /// Overwrites an existing instance definition.
    ///
    /// @param instance the instance to persist
    /// @throws DshException when the manifest cannot be written
    public static void update(DshInstance instance) throws DshException {
        if (!Files.isDirectory(instance.instanceDirectory())) {
            throw new DshException("Instance " + instance.id() + " does not exist");
        }
        write(instance);
        fireChanged();
    }

    /// Removes an instance and, in isolated mode, everything it owns.
    ///
    /// A shared or custom home is never deleted: other instances, or the user's
    /// own `dsh` installation, may still depend on it.
    ///
    /// The directory loses its name before anything inside it is removed (see
    /// [FileUtils#deleteByTombstone]), so a file another program holds can no longer
    /// leave a half-deleted instance behind: the listing keys on the directory name,
    /// so the row is gone the moment the tree is renamed, whatever is still being
    /// pumped into it.
    ///
    /// @param id the instance id
    /// @throws DshException when the instance does not exist or cannot be removed
    public static void delete(String id) throws DshException {
        DshInstance instance = find(id);
        if (instance == null) {
            throw new DshException("Instance " + id + " does not exist");
        }
        Path directory = instance.instanceDirectory();
        if (instance.homeMode() != DshHomeMode.ISOLATED && !directory.startsWith(DshPaths.INSTANCES)) {
            throw new DshException("Refusing to delete " + directory + " because it is outside the instance directory");
        }
        // A process started by an earlier panel is not in this one's registry, and on
        // Windows a directory cannot be removed while any process has it as its current
        // directory — which is exactly how an instance's agent is started. Without this,
        // "delete" fails with an empty error while something invisible sits on the tree.
        int strays = org.jackhuang.hmcl.util.ProcessTree.killReferencing(directory);
        if (strays > 0) {
            LOG.info("Stopped " + strays + " process(es) still referencing " + directory);
        }
        Path leftover;
        try {
            leftover = FileUtils.deleteByTombstone(directory, ".deleting-" + System.currentTimeMillis());
        } catch (IOException e) {
            throw new DshException("Failed to remove " + directory, e);
        }
        if (leftover != null) {
            LOG.warning("Could not delete " + directory + " completely; renamed it to "
                    + leftover.getFileName() + " and it will be swept later");
        }
        LOG.info("Removed instance " + id);
        fireChanged();
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
    public static void sweepTombstones() {
        if (!Files.isDirectory(DshPaths.INSTANCES)) {
            return;
        }
        try (java.util.stream.Stream<Path> instances = Files.list(DshPaths.INSTANCES)) {
            for (Path instance : instances.toList()) {
                // Inside an instance, a previous tree that could not be removed was renamed to
                // `dsh.previous.deleting-<millis>`; at the instance level the same suffix marks
                // an instance directory a delete could not finish.
                try (java.util.stream.Stream<Path> entries = Files.list(instance)) {
                    for (Path entry : entries.toList()) {
                        sweepTombstone(entry);
                    }
                } catch (IOException ignored) {
                    // One unreadable instance is not a reason to skip the rest.
                }
                sweepTombstone(instance);
            }
        } catch (IOException e) {
            LOG.warning("Could not sweep the tombstones under " + DshPaths.INSTANCES, e);
        }
    }

    private static void sweepTombstone(Path entry) {
        if (Tombstones.isSweepable(entry, MANIFEST_NAME, DshInstanceManager::recordedId)
                && FileUtils.deleteDirectoryQuietly(entry)) {
            LOG.info("Removed the leftover " + entry.getFileName());
        }
    }

    /// The id a directory's manifest records, or `null` when it has none.
    ///
    /// @param directory the candidate instance directory
    /// @return the recorded id
    private static @Nullable String recordedId(Path directory) {
        DshInstance instance = read(directory);
        return instance == null ? null : instance.id();
    }

    /// Reports whether an instance id is already taken.
    ///
    /// @param id the candidate id
    /// @return whether an instance with that id exists
    public static boolean exists(String id) {
        return find(id) != null;
    }

    /// Renames an instance.
    ///
    /// The instance's own directory — the runtime it holds, the plugin files it keeps, and
    /// an isolated home if it has one — is named after the instance and moves with it. A
    /// home the instance shares, or one the user chose, lives elsewhere and is left alone —
    /// but it still records the instance's own files, so what it recorded moves too.
    ///
    /// @param id    the current id
    /// @param newId the new id
    /// @return the renamed instance
    /// @throws DshException when either id is unusable or the move fails
    public static DshInstance rename(String id, String newId) throws DshException {
        DshInstance existing = find(id);
        if (existing == null) {
            throw new DshException("Instance " + id + " does not exist");
        }
        String normalized = newId == null ? "" : newId.trim();
        if (normalized.isEmpty()) {
            throw new DshException("An instance needs a name");
        }
        if (normalized.equals(id)) {
            return existing;
        }
        if (exists(normalized)) {
            throw new DshException("Instance " + normalized + " already exists");
        }
        if (DshProcessManager.stateOf(id) != DshProcessManager.LaunchState.STOPPED) {
            // Refused before anything moves. The rename moves the directory the running process has
            // open and writes into — its own plugins, its own logs, the plugins pnpm is installing
            // right now — so a rename during it would not be a rename, it would be pulling the ground
            // out from under a process that has no idea its files went somewhere else.
            throw new DshException("Cannot rename instance \"" + id + "\" while it is running or stopping");
        }

        DshInstance renamed = existing.withId(normalized);
        Path oldDirectory = DshPaths.instanceDirectory(id);
        Path newDirectory = DshPaths.instanceDirectory(normalized);

        boolean moved = false;
        if (Files.isDirectory(oldDirectory)) {
            // Nothing at the destination is removed to make room for the move. What
            // resolves to the new name is not necessarily an instance's directory: it can
            // be what a deleted instance left behind, or a folder somebody put there, and
            // removing it would delete it, tree and all, with nothing said. The name is
            // refused instead, and the directory is named so it can be moved aside.
            if (Files.exists(newDirectory, LinkOption.NOFOLLOW_LINKS)) {
                throw new DshException("Cannot rename \"" + id + "\" to \"" + normalized + "\": "
                        + newDirectory + " already exists");
            }
            try {
                Files.move(oldDirectory, newDirectory);
                moved = true;
            } catch (IOException e) {
                throw new DshException("Failed to move " + oldDirectory + ": " + e.getMessage(), e);
            }
        }

        try {
            write(renamed);
            // A plugin installed from a file the instance holds is recorded by that file's
            // path, and every later operation on the profile resolves the path again — so a
            // rename that left those records alone would stop the instance installing or
            // removing any plugin at all. They move with the files they name.
            DshLocalPluginPaths.relocate(renamed, oldDirectory, newDirectory);
        } catch (DshException e) {
            // Put the directory back rather than leaving the home orphaned under
            // a name no instance answers to.
            if (moved) {
                try {
                    Files.move(newDirectory, oldDirectory);
                } catch (IOException rollback) {
                    LOG.warning("Failed to undo the rename of " + id, rollback);
                }
            }
            throw e;
        }

        fireChanged();
        return renamed;
    }

    /// Copies an instance into a new one, its configuration and all.
    ///
    /// A copy is the original's **configuration**: the harness version it pins, its profile, the
    /// plugin list in its own order, the profile's patch layer, the plugins it installed from files,
    /// the settings its plugins keep, and its skill packs. What it is not is the original's
    /// **state**: session history and keys stay behind, because a copy of somebody's past is not what
    /// "copy this instance" asks for, and a key duplicated into a second home is a key nobody meant
    /// to make. The instance's own launcher settings — its install-script policy, the commands that
    /// run around it, which account it uses — come along, because those are what "this instance"
    /// means to the launcher rather than to the harness.
    ///
    /// Both halves of that are already written down elsewhere: a pack *is* that description of an
    /// instance, and installing one *is* that rebuild — version, profile, patch, local plugin files,
    /// settings sections, skills, and nothing that looks like a key. So a duplicate writes a pack to
    /// a temporary file and installs it, rather than copying directories: copying would have to
    /// answer the same questions again — which of `node_modules`, `dsh/`, `sessions/` and
    /// `.credentials.yaml` may travel — and it would leave the copy's `file:` plugin paths pointing
    /// into the original's plugin directory.
    ///
    /// **Resumable on purpose.** pnpm refuses to run a package's install script until it is told to,
    /// and it says so as a failure. The copy is kept in that case — the question is written into its
    /// own profile, so the answer has somewhere to go — and the work runs again from the copy it
    /// already made. Every other failure takes the half-made copy with it: an instance that appears
    /// in the list and cannot start is worse than one that never appeared.
    ///
    /// @param id     the instance to copy
    /// @param newId  the new instance's id
    /// @param report receives progress lines, or `null`
    /// @return the copy
    /// @throws DshException when either id is unusable, or the copy cannot be made
    public static DshInstance duplicate(String id, String newId,
                                        @Nullable java.util.function.Consumer<String> report)
            throws DshException {
        DshInstance source = find(id);
        if (source == null) {
            throw new DshException("Instance " + id + " does not exist");
        }
        String normalized = newId == null ? "" : newId.trim();
        if (normalized.isEmpty()) {
            throw new DshException("An instance needs a name");
        }
        if (normalized.equals(id)) {
            // The copy would be installed over its own source: the id is the source's, so `find` below
            // answers with the source itself and every later step treats the original as the half-made
            // copy — including the cleanup a failed install runs, which is what takes the source's files
            // away. Refused before any of that is decided.
            throw new DshException("Cannot duplicate instance \"" + id + "\" onto itself");
        }

        DshInstance copy = find(normalized);
        if (copy == null) {
            // The copy carries the original's icon, and only its icon: a copy that
            // is indistinguishable from what it was copied from is a copy nobody can
            // find in the list. The port is deliberately not copied — two instances
            // may not be given the same one — and the write is what makes the icon
            // survive, because a copy built by `withIcon` alone is never persisted.
            copy = create(normalized, source.version(), source.profile(),
                    source.workspacePath(), source.nodeRuntime(), DshHomeMode.ISOLATED, null,
                    source.extraArguments(), source.environment())
                    .withIcon(source.iconOrDefault());
            update(copy);
        } else if (DshVersionManager.isInstalled(copy)) {
            // An id taken by an instance that is already whole is somebody else's. One that is
            // half-made is this call's own earlier attempt, kept because pnpm asked about an install
            // script: the answer goes into that instance's profile, so the work continues there
            // instead of starting over.
            throw new DshException("Instance " + normalized + " already exists");
        }

        Path pack = null;
        try {
            pack = temporaryPack();
            DshModpacks.export(source, pack, report);
            DshModpacks.install(pack, normalized, source.workspacePath(), report);
            copyOwnLauncherSettings(source, copy);
            DshInstance filled = find(normalized);
            return filled == null ? copy : filled;
        } catch (DshException | RuntimeException failed) {
            if (failed instanceof DshPluginInstaller.DshBuildScriptApprovalRequired) {
                // The one failure that must keep what it made: the question pnpm raised is written
                // into the copy's profile, and removing the copy would take the question with it.
                throw failed;
            }
            LOG.warning("Could not copy " + id + " to " + normalized + "; removing the copy", failed);
            DshVersionManager.discardPartial(copy);
            try {
                delete(normalized);
            } catch (DshException | RuntimeException cleanupFailure) {
                LOG.warning("Could not remove the half-made copy " + normalized, cleanupFailure);
            }
            throw failed;
        } finally {
            deleteQuietly(pack);
        }
    }

    /// Creates the temporary pack a duplicate describes itself into.
    ///
    /// It goes through the system's temporary directory rather than the launcher's own, because it is
    /// not a thing the launcher keeps: it is written, read back, and deleted in the same call, and a
    /// copy that is interrupted leaves a file the system already knows how to sweep.
    ///
    /// @return the file, which does not exist yet
    /// @throws DshException when the system cannot make one
    private static Path temporaryPack() throws DshException {
        try {
            return Files.createTempFile("hdsl-duplicate-", DshModpacks.FILE_EXTENSION);
        } catch (IOException e) {
            throw new DshException("Could not create a file for the copy", e);
        }
    }

    /// Copies an instance's own launcher settings to another instance.
    ///
    /// The file holds what somebody chose *for this instance*: its install-script policy, the
    /// commands that run around it, which account it uses, and how loudly it logs. None of that is a
    /// secret — the account is named, not keyed — and all of it is what a copy should inherit, which
    /// is why it is carried beside the pack rather than inside it.
    ///
    /// @param source the instance copied from
    /// @param copy   the instance being filled
    /// @throws DshException when the file exists but cannot be copied
    private static void copyOwnLauncherSettings(DshInstance source, DshInstance copy)
            throws DshException {
        Path from = source.instanceDirectory().resolve("settings.json");
        if (!Files.isRegularFile(from)) {
            return;
        }
        Path to = copy.instanceDirectory().resolve("settings.json");
        try {
            Files.copy(from, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DshException("Failed to copy " + from + " to " + to, e);
        }
    }

    /// Returns the next free id derived from a base name.
    ///
    /// @param base the base name
    /// @return the id, suffixed with a number when needed
    public static String nextId(String base) {
        String candidate = base + "-copy";
        int suffix = 2;
        while (exists(candidate)) {
            candidate = base + "-copy" + suffix;
            suffix++;
        }
        return candidate;
    }

    /// Deletes a path, ignoring anything that goes wrong.
    ///
    /// Used for the copies a rename or delete leaves behind, where a failure to
    /// tidy up must not fail the operation that already succeeded.
    ///
    /// @param path the path to delete
    private static void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var paths = Files.walk(path)) {
            for (Path entry : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(entry);
            }
        } catch (IOException e) {
            LOG.warning("Failed to delete " + path, e);
        }
    }

    /// Reads the manifest inside an instance directory.
    ///
    /// @param directory the candidate instance directory
    /// @return the instance, or `null` when there is no readable manifest
    private static @Nullable DshInstance read(Path directory) {
        Path manifest = directory.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            DshInstance instance = JsonUtils.fromJsonFile(manifest, DshInstance.class);
            if (instance == null || instance.id() == null || instance.version() == null) {
                return null;
            }
            return instance;
        } catch (Exception e) {
            LOG.warning("Failed to read instance manifest " + manifest, e);
            return null;
        }
    }

    /// Writes an instance manifest.
    ///
    /// @param instance the instance to persist
    /// @throws DshException when the manifest cannot be written
    // H3 (glm-5.2): atomic write — write to a temp file and move it into place
    // so a reader never opens a half-written manifest.
    private static void write(DshInstance instance) throws DshException {
        try {
            Path directory = instance.instanceDirectory();
            Files.createDirectories(directory);
            Path target = directory.resolve(MANIFEST_NAME);
            Path temp = Files.createTempFile(directory, "instance-manifest", ".json");
            try {
                JsonUtils.writeToJsonFile(temp, instance);
                moveManifest(temp, target);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new DshException("Failed to write the instance manifest for " + instance.id(), e);
        }
    }

    /// Moves a written manifest into place atomically when the filesystem
    /// supports it, falling back to a non-atomic move otherwise (glm-5.2).
    ///
    /// @param written the temp file to move
    /// @param target  where it goes
    /// @throws IOException when it cannot be moved
    private static void moveManifest(Path written, Path target) throws IOException {
        try {
            Files.move(written, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            LOG.info("Moving " + written + " into place atomically is not supported here", e);
            Files.move(written, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
