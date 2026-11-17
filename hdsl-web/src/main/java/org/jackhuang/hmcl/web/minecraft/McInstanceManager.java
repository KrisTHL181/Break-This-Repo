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

import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/// The instances root of the Minecraft surface: one directory per server, a
/// manifest inside it, and the files the game expects beside the manifest.
///
/// The manager owns only the panel's bookkeeping. `server.jar`, the world and
/// the logs are ordinary files the server itself writes; the panel reads them
/// when it needs to and never treats their absence as "the instance is gone".
@NotNullByDefault
public final class McInstanceManager {

    /// The panel-side manifest.
    public static final String MANIFEST_NAME = "instance.json";
    /// The game's own configuration.
    public static final String PROPERTIES_NAME = "server.properties";
    /// The Mojang EULA marker; written only after the owner accepted.
    public static final String EULA_NAME = "eula.txt";
    /// The server program, downloaded per version.
    public static final String SERVER_JAR_NAME = "server.jar";
    public static final String WORLD_DIR_NAME = "world";
    public static final String LOG_DIR_NAME = "logs";
    public static final String LOG_NAME = "latest.log";

    private static final Logger LOG = Logger.LOG;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path root;

    /// Where instances lived before the panel moved them onto their own mount
    /// (`<data>/minecraft/instances`). Still scanned so an existing server keeps
    /// working after the move, and offered as a storage choice for deployments
    /// where the dedicated directory is not writable.
    private final @Nullable Path legacyRoot;

    /// @param root the directory new instances go into (`/home/hdsl/mc` in the
    ///             container image, `$HOME/mc` elsewhere)
    public McInstanceManager(Path root) {
        this(root, null);
    }

    /// @param root       the directory new instances go into
    /// @param legacyRoot the pre-move directory, or `null`
    public McInstanceManager(Path root, @Nullable Path legacyRoot) {
        this.root = root.toAbsolutePath().normalize();
        Path legacy = legacyRoot == null ? null : legacyRoot.toAbsolutePath().normalize();
        this.legacyRoot = legacy == null || legacy.equals(this.root) ? null : legacy;
    }

    /// The instances root new servers are created in.
    ///
    /// @return the root
    public Path root() {
        return root;
    }

    /// The pre-move root, when one exists.
    ///
    /// @return the legacy root, or `null`
    public @Nullable Path legacyRoot() {
        return legacyRoot;
    }

    /// Every root that may contain instances, primary first.
    private List<Path> roots() {
        return legacyRoot == null ? List.of(root) : List.of(root, legacyRoot);
    }

    /// Which storage area an instance lives in.
    ///
    /// @param id the instance id
    /// @return `external` for the dedicated directory, `data` for the legacy one
    public String storageOf(String id) {
        if (legacyRoot != null && Files.isDirectory(legacyRoot.resolve(id))
                && !Files.isDirectory(root.resolve(id))) {
            return "data";
        }
        return "external";
    }

    /// Lists every readable instance, newest first.
    ///
    /// Both roots are scanned; an id that exists in both is listed once, from
    /// the primary root (that is where a later create would put it).
    ///
    /// @return the instances
    public List<McInstance> list() {
        List<McInstance> instances = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Path root : roots()) {
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> entries = Files.list(root)) {
                for (Path directory : entries.filter(Files::isDirectory).toList()) {
                    McInstance instance = read(directory);
                    if (instance != null && seen.add(instance.id())) {
                        instances.add(instance);
                    }
                }
            } catch (IOException e) {
                LOG.warning("Failed to enumerate Minecraft instances in " + root, e);
            }
        }
        instances.sort(Comparator.comparingLong(McInstance::createdAt).reversed());
        return instances;
    }

    /// Finds one instance.
    ///
    /// @param id the instance id
    /// @return the instance, or `null` when it does not exist or is unreadable
    public @Nullable McInstance find(String id) {
        if (id == null || id.isBlank() || id.contains("/") || id.contains("\\")
                || id.equals(".") || id.equals("..")) {
            return null;
        }
        for (Path root : roots()) {
            McInstance instance = read(root.resolve(id));
            if (instance != null) {
                return instance;
            }
        }
        return null;
    }

    /// Creates an instance and writes its manifest and a default
    /// `server.properties` (the game would write the same defaults on first
    /// start; writing them now makes the ports visible in the panel).
    ///
    /// @param name      the display name
    /// @param version   the Minecraft version to install later
    /// @param port      the game port
    /// @param rconPort  the RCON port
    /// @param memoryMb  the heap size
    /// @param levelName the world directory name
    /// @param seed      the world seed, or `null` for a random one
    /// @return the created instance
    /// @throws McException when the ports are taken or the files cannot be written
    public McInstance create(String name, String version, int port, int rconPort,
                             int memoryMb, String levelName, @Nullable String seed) throws McException {
        return create(name, version, port, rconPort, memoryMb, levelName, seed, false);
    }

    /// Creates an instance in one of the two storage areas.
    ///
    /// @param name      the display name
    /// @param version   the Minecraft version to install later
    /// @param port      the game port
    /// @param rconPort  the RCON port
    /// @param memoryMb  the heap size
    /// @param levelName the world directory name
    /// @param seed      the world seed, or `null` for a random one
    /// @param legacy    whether to put it in the pre-move directory instead of
    ///                  the dedicated one (no-op when there is no legacy root)
    /// @return the created instance
    /// @throws McException when the ports are taken or the files cannot be written
    public McInstance create(String name, String version, int port, int rconPort,
                             int memoryMb, String levelName, @Nullable String seed,
                             boolean legacy) throws McException {
        return create(name, version, port, rconPort, memoryMb, levelName, seed, legacy,
                McInstance.CORE_VANILLA);
    }

    /// The same, with the server core the instance runs.
    ///
    /// @param name      the display name
    /// @param version   the game version
    /// @param port      the game port
    /// @param rconPort  the RCON port
    /// @param memoryMb  the heap size
    /// @param levelName the world directory name
    /// @param seed      the world seed, or `null` for a random one
    /// @param legacy    whether to put it in the pre-move directory instead of
    ///                  the dedicated one (no-op when there is no legacy root)
    /// @param core      the core id (`vanilla`, `paper`, …)
    /// @return the created instance
    /// @throws McException when the ports are taken or the files cannot be written
    public McInstance create(String name, String version, int port, int rconPort,
                             int memoryMb, String levelName, @Nullable String seed,
                             boolean legacy, String core) throws McException {
        McPorts.validate(port, rconPort);
        if (portTaken(port)) {
            throw new McException("port " + port + " is already used by another instance");
        }
        if (portTaken(rconPort)) {
            throw new McException("port " + rconPort + " is already used by another instance");
        }

        Path target = legacy && legacyRoot != null ? legacyRoot : root;
        String id = uniqueId(target, name);
        long now = System.currentTimeMillis();
        McInstance instance;
        try {
            instance = new McInstance(id, name == null ? "" : name.trim(), version.trim(),
                    port, rconPort, memoryMb, levelName, seed, now, core);
        } catch (IllegalArgumentException e) {
            throw new McException(e.getMessage(), e);
        }

        Path directory = target.resolve(id);
        try {
            Files.createDirectories(directory);
            // Forge/NeoForge run their installer as the agent **before** the first start, so
            // the directory has to be the agent's from creation; the start-time hand-over
            // happens too late for that path.
            org.jackhuang.hmcl.util.platform.Privileges.prepareAgentDirectory(
                    directory, org.jackhuang.hmcl.util.platform.Privileges.AGENT_SHARED_DIRECTORY);
            Files.createDirectories(directory.resolve(LOG_DIR_NAME));
            write(instance);
            defaultProperties(instance).save(propertiesFile(instance));
        } catch (IOException e) {
            throw new McException("Failed to create " + directory, e);
        }
        LOG.info("Created Minecraft instance " + id + " (" + instance.name() + ", " + instance.version() + ")");
        return instance;
    }

    /// Persists an edited instance.
    ///
    /// @param instance the instance
    /// @throws McException when it does not exist or cannot be written
    public void update(McInstance instance) throws McException {
        if (!Files.isDirectory(instanceDirectory(instance.id()))) {
            throw new McException("Minecraft instance " + instance.id() + " does not exist");
        }
        write(instance);
    }

    /// Removes an instance and everything it owns.
    ///
    /// The caller is responsible for stopping the server and taking a backup
    /// first; this method only refuses to delete outside the known roots.
    ///
    /// @param id the instance id
    /// @throws McException when the instance does not exist or cannot be removed
    public void delete(String id) throws McException {
        McInstance instance = find(id);
        if (instance == null) {
            throw new McException("Minecraft instance " + id + " does not exist");
        }
        Path directory = instanceDirectory(id).normalize();
        boolean inside = false;
        for (Path root : roots()) {
            if (directory.startsWith(root)) {
                inside = true;
                break;
            }
        }
        if (!inside) {
            throw new McException("Refusing to delete " + directory + ": it is outside the instances directories");
        }
        // A panel killed before it could stop its children leaves a `java`
        // process whose command line names this directory (the server jar path
        // is absolute). Windows also refuses to remove files it holds.
        org.jackhuang.hmcl.util.ProcessTree.killReferencing(directory);
        try (Stream<Path> walk = Files.walk(directory)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new McException("Failed to remove " + directory, e);
        }
        LOG.info("Removed Minecraft instance " + id);
    }

    /// Whether a port is already claimed by any instance (game or RCON).
    ///
    /// @param port the port
    /// @return whether it is taken
    public boolean portTaken(int port) {
        return portTaken(port, null);
    }

    /// Whether a port is claimed by an instance other than `exceptId`.
    ///
    /// Editing an instance's ports has to ignore the instance's own current
    /// numbers; otherwise every save would look like a conflict with itself.
    ///
    /// @param port     the port
    /// @param exceptId the instance being edited, or `null`
    /// @return whether another instance holds it
    public boolean portTaken(int port, @Nullable String exceptId) {
        for (McInstance instance : list()) {
            if (instance.id().equals(exceptId)) {
                continue;
            }
            if (instance.port() == port || instance.rconPort() == port) {
                return true;
            }
        }
        return false;
    }

    /// The directory holding an instance, searched in both roots.
    ///
    /// @param id the instance id
    /// @return the directory (the primary root's path when it does not exist yet)
    public Path instanceDirectory(String id) {
        if (legacyRoot != null && Files.isDirectory(legacyRoot.resolve(id))
                && !Files.isDirectory(root.resolve(id))) {
            return legacyRoot.resolve(id);
        }
        return root.resolve(id);
    }

    /// The world directory.
    ///
    /// @param instance the instance
    /// @return the directory
    public Path worldDirectory(McInstance instance) {
        return instanceDirectory(instance.id()).resolve(instance.levelName());
    }

    /// The game's `server.properties`.
    ///
    /// @param instance the instance
    /// @return the file
    public Path propertiesFile(McInstance instance) {
        return instanceDirectory(instance.id()).resolve(PROPERTIES_NAME);
    }

    /// The EULA marker.
    ///
    /// @param instance the instance
    /// @return the file
    public Path eulaFile(McInstance instance) {
        return instanceDirectory(instance.id()).resolve(EULA_NAME);
    }

    /// The server program.
    ///
    /// @param instance the instance
    /// @return the file
    public Path serverJar(McInstance instance) {
        return instanceDirectory(instance.id()).resolve(SERVER_JAR_NAME);
    }

    /// The log the server writes.
    ///
    /// @param instance the instance
    /// @return the file
    public Path logFile(McInstance instance) {
        return instanceDirectory(instance.id()).resolve(LOG_DIR_NAME).resolve(LOG_NAME);
    }

    /// Reads `server.properties`, answering the managed defaults when the file
    /// is absent (a just-created instance, or one whose file was deleted).
    ///
    /// @param instance the instance
    /// @return the properties
    /// @throws McException when the file exists but cannot be read
    public McProperties properties(McInstance instance) throws McException {
        try {
            McProperties properties = McProperties.load(propertiesFile(instance));
            if (properties.asMap().isEmpty()) {
                return defaultProperties(instance);
            }
            return properties;
        } catch (IOException e) {
            throw new McException("Failed to read " + propertiesFile(instance), e);
        }
    }

    /// Writes `server.properties`.
    ///
    /// @param instance   the instance
    /// @param properties the properties to write
    /// @throws McException when the file cannot be written
    public void saveProperties(McInstance instance, McProperties properties) throws McException {
        try {
            properties.save(propertiesFile(instance));
        } catch (IOException e) {
            throw new McException("Failed to write " + propertiesFile(instance), e);
        }
    }

    /// Persists new ports to both places that hold them.
    ///
    /// The panel reads the manifest and the game reads `server.properties`;
    /// writing only one of them means the server listens somewhere the panel is
    /// not looking. The caller validates conflicts (see [#portTaken(int, String)]).
    ///
    /// @param instance the instance
    /// @param port     the new game port
    /// @param rconPort the new RCON port
    /// @return the updated instance
    /// @throws McException when either file cannot be written
    public McInstance updatePorts(McInstance instance, int port, int rconPort) throws McException {
        McInstance updated = instance.withPorts(port, rconPort);
        McProperties properties = properties(instance);
        properties.set("server-port", String.valueOf(port));
        properties.set("rcon.port", String.valueOf(rconPort));
        update(updated);
        saveProperties(updated, properties);
        return updated;
    }

    /// Writes the Mojang EULA marker.
    ///
    /// Only call this after the owner accepted: the file is the operator's
    /// technical record of that acceptance, not the acceptance itself.
    ///
    /// @param instance the instance
    /// @throws McException when the file cannot be written
    public void acceptEula(McInstance instance) throws McException {
        try {
            Files.writeString(eulaFile(instance), "eula=true\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new McException("Failed to write " + eulaFile(instance), e);
        }
    }

    /// The last lines of the server log, when the server has written one.
    ///
    /// @param instance the instance
    /// @param maxLines how many lines to keep
    /// @return the lines, oldest first
    /// @throws IOException when the file cannot be read
    public List<String> tailLog(McInstance instance, int maxLines) throws IOException {
        Path log = logFile(instance);
        if (!Files.isRegularFile(log)) {
            return List.of();
        }
        List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
        int from = Math.max(0, lines.size() - Math.max(0, maxLines));
        return List.copyOf(lines.subList(from, lines.size()));
    }

    /// The properties a first start begins from: the two ports the panel
    /// allocated, RCON off (it is optional, §18), online mode on, and a random
    /// RCON password so enabling it later is not a plaintext surprise.
    private McProperties defaultProperties(McInstance instance) {
        McProperties properties = McProperties.empty();
        properties.set("server-port", String.valueOf(instance.port()));
        properties.set("rcon.port", String.valueOf(instance.rconPort()));
        properties.set("enable-rcon", "false");
        properties.set("rcon.password", randomPassword());
        properties.set("online-mode", "true");
        properties.set("white-list", "false");
        properties.set("enforce-whitelist", "false");
        properties.set("level-name", instance.levelName());
        properties.set("level-seed", instance.seed() == null ? "" : instance.seed());
        properties.set("motd", "A Minecraft Server");
        properties.set("max-players", "20");
        return properties;
    }

    private static String randomPassword() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /// A filesystem-safe id derived from the name, made unique by a numeric
    /// suffix. Chinese names collapse to `mc` — the id is internal, the display
    /// name keeps the user's text.
    ///
    /// @param target the root the instance is about to be created in
    /// @param name   the display name
    /// @return an id no root is using yet
    private String uniqueId(Path target, @Nullable String name) {
        String base = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        StringBuilder slug = new StringBuilder();
        for (int i = 0; i < base.length() && slug.length() < 24; i++) {
            char c = base.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_') {
                slug.append(c);
            } else if (c == ' ' && !slug.isEmpty() && slug.charAt(slug.length() - 1) != '-') {
                slug.append('-');
            }
        }
        String candidate = slug.toString().replaceAll("-+$", "");
        if (candidate.isEmpty() || candidate.equals(".") || candidate.equals("..")) {
            candidate = "mc";
        }
        String id = candidate;
        int suffix = 2;
        while (taken(target, id)) {
            id = candidate + "-" + suffix++;
        }
        return id;
    }

    /// Whether an id is already a directory in any root.
    private boolean taken(Path target, String id) {
        if (Files.exists(target.resolve(id))) {
            return true;
        }
        for (Path root : roots()) {
            if (Files.exists(root.resolve(id))) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable McInstance read(Path directory) {
        Path manifest = directory.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifest)) {
            return null;
        }
        try {
            McInstance instance = JsonUtils.fromJsonFile(manifest, McInstance.class);
            if (instance == null || instance.id() == null) {
                return null;
            }
            return instance;
        } catch (Exception e) {
            LOG.warning("Failed to read the Minecraft instance manifest " + manifest, e);
            return null;
        }
    }

    private void write(McInstance instance) throws McException {
        try {
            Path directory = instanceDirectory(instance.id());
            Files.createDirectories(directory);
            Path target = directory.resolve(MANIFEST_NAME);
            Path temp = Files.createTempFile(directory, "instance-manifest", ".json");
            try {
                JsonUtils.writeToJsonFile(temp, instance);
                move(temp, target);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new McException("Failed to write the instance manifest for " + instance.id(), e);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}