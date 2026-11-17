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
package org.jackhuang.hmcl.web.minecraft;

import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/// Snapshots of one instance: the world plus the files that describe it.
///
/// **A backup is taken while the server is stopped.** Hot backups need
/// `save-off/save-all` and a way to wait for the flush; a snapshot that misses
/// that is a zip of a world being written, which is worse than no backup
/// because it looks like one. The caller refuses while the server runs.
///
/// Restoring first snapshots the current world, so "restore" is itself
/// undoable — the same rule the panel applies to installing a version.
///
/// The zip carries `instance.json`, `server.properties`, `eula.txt` and the
/// world directory. It deliberately does **not** carry `server.jar` (large,
/// re-downloadable, hash-verified) or logs.
@NotNullByDefault
public final class McBackups {

    /// One snapshot on disk.
    ///
    /// @param name      the file name, which is also the id the API uses
    /// @param createdAt ISO instant parsed from the name
    /// @param bytes     size on disk
    public record Backup(String name, String createdAt, long bytes) {
    }

    private static final Logger LOG = Logger.LOG;
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
    /// The entries a snapshot may contain; anything else in the zip is dropped
    /// on restore (zip-slip and "someone put a server.jar in there").
    private static final Set<String> ALLOWED_TOP = Set.of(
            McInstanceManager.MANIFEST_NAME, McInstanceManager.PROPERTIES_NAME,
            McInstanceManager.EULA_NAME, McInstanceManager.WORLD_DIR_NAME);

    private final Path root;

    /// @param root the backups root (`<data>/minecraft/backups`)
    public McBackups(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /// Lists an instance's snapshots, newest first.
    ///
    /// @param instanceId the instance
    /// @return the snapshots
    public List<Backup> list(String instanceId) {
        Path directory = directory(instanceId);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<Backup> backups = new ArrayList<>();
        try (var entries = Files.list(directory)) {
            for (Path file : entries.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".zip")) {
                    continue;
                }
                try {
                    backups.add(new Backup(name, createdAtOf(file), Files.size(file)));
                } catch (IOException e) {
                    LOG.warning("Failed to read backup " + file, e);
                }
            }
        } catch (IOException e) {
            LOG.warning("Failed to list backups in " + directory, e);
        }
        backups.sort(Comparator.comparing(Backup::name).reversed());
        return backups;
    }

    /// Snapshots an instance. The caller must have stopped the server.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @return the created snapshot
    /// @throws McException when the world cannot be read or the zip cannot be written
    public Backup create(McInstance instance, McInstanceManager manager) throws McException {
        Path instanceDir = manager.instanceDirectory(instance.id());
        if (!Files.isDirectory(instanceDir)) {
            throw new McException("instance " + instance.id() + " does not exist");
        }
        Path directory = directory(instance.id());
        try {
            Files.createDirectories(directory);
            String name = STAMP.format(Instant.now());
            // Two snapshots inside one second (a restore takes one right before
            // another) must not overwrite each other.
            String candidate = name + ".zip";
            int suffix = 2;
            while (Files.exists(directory.resolve(candidate))) {
                candidate = name + "-" + suffix++ + ".zip";
            }
            String finalName = candidate;
            Path target = directory.resolve(finalName);
            Path temp = Files.createTempFile(directory, "backup", ".zip.part");
            try {
                try (OutputStream out = Files.newOutputStream(temp);
                     ZipOutputStream zip = new ZipOutputStream(out)) {
                    zip.putNextEntry(new ZipEntry(McInstanceManager.MANIFEST_NAME));
                    Files.copy(instanceDir.resolve(McInstanceManager.MANIFEST_NAME), zip);
                    zip.closeEntry();
                    for (String file : List.of(McInstanceManager.PROPERTIES_NAME, McInstanceManager.EULA_NAME)) {
                        Path source = instanceDir.resolve(file);
                        if (Files.isRegularFile(source)) {
                            zip.putNextEntry(new ZipEntry(file));
                            Files.copy(source, zip);
                            zip.closeEntry();
                        }
                    }
                    Path world = instanceDir.resolve(instance.levelName());
                    if (Files.isDirectory(world)) {
                        zipTree(zip, instanceDir, world);
                    }
                }
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                long bytes = Files.size(target);
                LOG.info("Backed up Minecraft instance " + instance.id() + " → " + finalName + " (" + bytes + " bytes)");
                return new Backup(finalName, createdAtOf(target), bytes);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new McException("Failed to back up " + instance.id() + ": " + e.getMessage(), e);
        }
    }

    /// Restores a snapshot, after snapshotting what is there now.
    ///
    /// @param instance the instance
    /// @param manager  the instance manager
    /// @param name     the snapshot to restore
    /// @return the safety snapshot taken before the restore
    /// @throws McException when the snapshot is missing or the extract fails
    public Backup restore(McInstance instance, McInstanceManager manager, String name) throws McException {
        Path file = backupFile(instance.id(), name);
        if (!Files.isRegularFile(file)) {
            throw new McException("backup " + name + " does not exist");
        }
        Backup safety = create(instance, manager);
        Path instanceDir = manager.instanceDirectory(instance.id());
        try (InputStream in = Files.newInputStream(file); ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                String top = entryName.contains("/") ? entryName.substring(0, entryName.indexOf('/')) : entryName;
                if (!ALLOWED_TOP.contains(top)) {
                    continue;
                }
                Path target = instanceDir.resolve(entryName).normalize();
                if (!target.startsWith(instanceDir)) {
                    throw new McException("backup " + name + " contains an entry outside the instance: " + entryName);
                }
                Files.createDirectories(target.getParent());
                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new McException("Failed to restore " + name + ": " + e.getMessage(), e);
        }
        LOG.info("Restored Minecraft instance " + instance.id() + " from " + name
                + " (safety backup " + safety.name() + ")");
        return safety;
    }

    /// Deletes one snapshot.
    ///
    /// @param instanceId the instance
    /// @param name       the snapshot
    /// @throws McException when it cannot be removed
    public void delete(String instanceId, String name) throws McException {
        Path file = backupFile(instanceId, name);
        if (!Files.isRegularFile(file)) {
            throw new McException("backup " + name + " does not exist");
        }
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new McException("Failed to delete " + file, e);
        }
    }

    /// The path of one snapshot, refusing names that try to leave the root.
    public Path backupFile(String instanceId, String name) throws McException {
        if (name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
            throw new McException("invalid backup name");
        }
        Path file = directory(instanceId).resolve(name).normalize();
        if (!file.startsWith(root)) {
            throw new McException("backup " + name + " is outside the backups directory");
        }
        return file;
    }

    private Path directory(String instanceId) {
        return root.resolve(instanceId);
    }

    private static String createdAtOf(Path file) {
        try {
            return Instant.ofEpochMilli(Files.getLastModifiedTime(file).toMillis()).toString();
        } catch (IOException e) {
            return "";
        }
    }

    private static void zipTree(ZipOutputStream zip, Path base, Path directory) throws IOException {
        try (var walk = Files.walk(directory)) {
            for (Path path : walk.filter(Files::isRegularFile).sorted().toList()) {
                zip.putNextEntry(new ZipEntry(base.relativize(path).toString().replace('\\', '/')));
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
    }
}