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
package org.jackhuang.hmcl.web.brand;

import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.Tombstones;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Creates, enumerates, edits and removes [BrandInstance]s for one [Brand].
///
/// Each instance is stored as `brands/<brand>/instances/<id>/instance.json`
/// under the server data directory — the same manifest style as
/// [org.jackhuang.hmcl.web.zcode.ZcodeInstanceManager], in a tree of its own so
/// third-party state never touches dsh state.
///
/// An instance directory also holds everything the process owns at runtime:
/// `home/` becomes the child process's `HOME` (Kimi Code keeps its state at
/// `~/.kimi-code`, OpenCode at `~/.config/opencode` — both land inside the
/// instance directory and therefore inside the volume), `workspace/` is the
/// working directory, and `logs/<brand>.log` collects the process's merged
/// output.
///
/// Unlike the ZCode category there is deliberately **no credential injection**:
/// these tools manage their own providers, and the panel's disclaimer says the
/// vendor owns that behaviour. Logging in happens inside the tool's own web UI.
@NotNullByDefault
public final class BrandInstanceManager {

    /// The manifest file inside an instance directory.
    public static final String MANIFEST_NAME = "instance.json";

    /// The directory that becomes the child process's `HOME`.
    public static final String HOME_DIR_NAME = "home";

    /// The working directory the instance is launched with.
    public static final String WORKSPACE_DIR_NAME = "workspace";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Brand brand;
    private final Path root;

    /// Binds a manager to one brand's `brands/<id>/instances` directory.
    ///
    /// @param brand the brand
    /// @param root the instances root
    public BrandInstanceManager(Brand brand, Path root) {
        this.brand = brand;
        this.root = root.toAbsolutePath().normalize();
    }

    /// The brand this manager serves.
    ///
    /// @return the brand
    public Brand brand() {
        return brand;
    }

    /// The directory every instance lives under.
    ///
    /// @return the instances root
    public Path root() {
        return root;
    }

    /// Lists every readable instance, newest first.
    ///
    /// @return the known instances
    public List<BrandInstance> list() {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<BrandInstance> instances = new ArrayList<>();
        try (Stream<Path> entries = Files.list(root)) {
            for (Path directory : entries.filter(Files::isDirectory).toList()) {
                BrandInstance instance = read(directory);
                if (instance != null) {
                    instances.add(instance);
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to enumerate " + brand.id() + " instances in " + root, e);
        }
        instances.sort(Comparator.comparingLong(BrandInstance::createdAt).reversed());
        return instances;
    }

    /// Finds an instance by id.
    ///
    /// @param id the instance id
    /// @return the instance, or `null` when it does not exist or is unreadable
    public @Nullable BrandInstance find(String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\")
                || id.equals(".") || id.equals("..")) {
            return null;
        }
        return read(root.resolve(id));
    }

    /// Creates and persists a new instance pinned to the given tool version.
    ///
    /// @param name the display name; must not be blank
    /// @param version the tool version to run; must be installed
    /// @return the created instance
    /// @throws BrandException when the name is blank or the instance cannot be written
    public BrandInstance create(String name, String version) throws BrandException {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new BrandException("A " + brand.id() + " instance needs a name");
        }
        if (version == null || version.isBlank()) {
            throw new BrandException("A " + brand.id() + " instance needs a version");
        }

        String id = newTokenLike(9);
        Path directory = root.resolve(id);
        BrandInstance instance = new BrandInstance(id, brand.id(), normalized,
                version.trim(), null, 0, System.currentTimeMillis(), null, null, null);

        try {
            Files.createDirectories(homeDirectoryOf(directory));
            Files.createDirectories(workspaceDirectoryOf(directory));
            Files.createDirectories(logDirectoryOf(directory));
        } catch (IOException e) {
            throw new BrandException("Failed to create " + directory, e);
        }
        // The directories above were just created as root; the tool that will run
        // in them is the agent. See BrandRuntime.handInstanceDirsToAgent for what
        // happens when this is skipped (Hermes refuses a root-owned home).
        BrandRuntime.handInstanceDirsToAgent(this, instance);

        write(instance);
        LOG.info("Created " + brand.id() + " instance " + id + " (" + normalized + ", " + version + ")");
        return instance;
    }

    /// Overwrites an existing instance definition.
    ///
    /// @param instance the instance to persist
    /// @throws BrandException when the manifest cannot be written
    public void update(BrandInstance instance) throws BrandException {
        if (!Files.isDirectory(instanceDirectory(instance.id()))) {
            throw new BrandException(brand.id() + " instance " + instance.id() + " does not exist");
        }
        write(instance);
    }

    /// Removes an instance and everything it owns.
    ///
    /// The directory is renamed out of the way before it is emptied (see
    /// [FileUtils#deleteByTombstone]): the listing keys on the directory name, so an
    /// instance whose log another program still holds is gone from the list instead of
    /// lingering as a row that cannot start.
    ///
    /// @param id the instance id
    /// @throws BrandException when the instance does not exist or cannot be removed
    public void delete(String id) throws BrandException {
        BrandInstance instance = find(id);
        if (instance == null) {
            throw new BrandException(brand.id() + " instance " + id + " does not exist");
        }
        Path directory = instanceDirectory(id);
        if (!directory.normalize().startsWith(root)) {
            throw new BrandException("Refusing to delete " + directory + " because it is outside the instances directory");
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
            throw new BrandException("Failed to remove " + directory, e);
        }
        if (leftover != null) {
            LOG.warning("Could not delete " + directory + " completely; renamed it to "
                    + leftover.getFileName() + " and it will be swept later");
        }
        LOG.info("Removed " + brand.id() + " instance " + id);
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
                if (Tombstones.isSweepable(entry, MANIFEST_NAME, BrandInstanceManager::recordedId)
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
        BrandInstance instance = read(directory);
        return instance == null ? null : instance.id();
    }

    /// The directory holding an instance's manifest and runtime state.
    ///
    /// @param id the instance id
    /// @return the instance directory
    public Path instanceDirectory(String id) {
        return root.resolve(id);
    }

    /// The directory that becomes the child process's `HOME`.
    ///
    /// @param instance the instance
    /// @return the home directory
    public Path homeDirectory(BrandInstance instance) {
        return homeDirectoryOf(instanceDirectory(instance.id()));
    }

    /// The working directory the process is launched with.
    ///
    /// @param instance the instance
    /// @return the workspace directory
    public Path workspaceDirectory(BrandInstance instance) {
        return workspaceDirectoryOf(instanceDirectory(instance.id()));
    }

    /// The file the process's merged output is appended to.
    ///
    /// @param instance the instance
    /// @return the log file
    public Path logFile(BrandInstance instance) {
        return logDirectoryOf(instanceDirectory(instance.id())).resolve(brand.id() + ".log");
    }

    /// Reads the last lines of an instance's process log.
    ///
    /// @param instance the instance
    /// @param maxLines the most lines to return
    /// @return the trailing log lines, oldest first; empty when there is no log
    /// @throws IOException when the log exists but cannot be read
    public List<String> tailLog(BrandInstance instance, int maxLines) throws IOException {
        Path log = logFile(instance);
        if (!Files.isRegularFile(log)) {
            return List.of();
        }
        try (var lines = Files.lines(log, StandardCharsets.UTF_8)) {
            List<String> all = lines.toList();
            return List.copyOf(all.subList(Math.max(0, all.size() - maxLines), all.size()));
        }
    }

    private Path homeDirectoryOf(Path directory) {
        return directory.resolve(HOME_DIR_NAME);
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
    private static @Nullable BrandInstance read(Path directory) {
        Path manifest = directory.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            BrandInstance instance = JsonUtils.fromJsonFile(manifest, BrandInstance.class);
            if (instance == null || instance.id() == null || instance.brand() == null) {
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
    /// @throws BrandException when the manifest cannot be written
    private void write(BrandInstance instance) throws BrandException {
        try {
            Path directory = instanceDirectory(instance.id());
            Files.createDirectories(directory);
            JsonUtils.writeToJsonFile(directory.resolve(MANIFEST_NAME), instance);
        } catch (IOException e) {
            throw new BrandException("Failed to write the instance manifest for " + instance.id(), e);
        }
    }
}
