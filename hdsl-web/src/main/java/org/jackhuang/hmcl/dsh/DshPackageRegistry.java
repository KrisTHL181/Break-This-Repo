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
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Reads package metadata from the npm registry.
///
/// Plugins are ordinary npm packages, so the registry is the only catalogue
/// there is: upstream ships no plugin store and no version feed. The ordinary
/// `npm view` command is used rather than talking HTTP directly, so a user's
/// registry mirror or proxy configuration is honoured automatically.
@NotNullByDefault
public final class DshPackageRegistry {
    private DshPackageRegistry() {
    }

    /// Fetches the published versions of a package, newest first.
    ///
    /// @param packageName the package name
    /// @return the published versions, newest first
    /// @throws DshException when npm is unavailable or the query fails
    public static List<String> fetchVersions(String packageName) throws DshException {
        DshNodeRuntime runtime = DshNodeRuntime.detectForNpm()
                .orElseThrow(() -> new DshException("Node.js was not found on PATH and no panel-managed "
                        + "runtime is installed (" + DshNodeRuntime.requirement() + "). "
                        + "Open Settings -> Node.js for a one-click managed runtime, or install Node.js yourself."));
        if (!runtime.canInstall()) {
            throw new DshException("the Node.js this panel would use has no npm (" + runtime.node() + "); "
                    + "listing plugin versions requires it. Open Settings -> Node.js and install the "
                    + "panel-managed runtime, which ships with npm.");
        }

        List<String> command = List.of(runtime.npm().toString(), "view", packageName, "versions", "--json");
        DshCommand.Result result;
        try {
            result = DshCommand.run(command, null, runtime.pathEnvironment(), null);
        } catch (IOException e) {
            throw new DshException("Failed to run npm", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("npm view was interrupted", e);
        }

        if (!result.isSuccess()) {
            throw new DshException("`npm view " + packageName + " versions` exited with code "
                    + result.exitCode() + ": " + tail(result.output()));
        }

        List<String> versions = parseVersions(result.text());
        versions.sort(Comparator.comparing((String version) -> version,
                DshVersionManager::compareVersions).reversed());
        return versions;
    }

    /// Parses the version list npm printed.
    ///
    /// @param text the JSON text, which may be a lone string for a package with one release
    /// @return the versions
    /// @throws DshException when the output is not usable
    private static List<String> parseVersions(String text) throws DshException {
        if (text.isBlank()) {
            throw new DshException("npm returned no versions for this package");
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(text);
        } catch (RuntimeException e) {
            LOG.warning("Failed to parse npm output", e);
            throw new DshException("npm returned output that could not be parsed");
        }

        List<String> versions = new ArrayList<>();
        if (parsed.isJsonArray()) {
            for (JsonElement element : parsed.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    versions.add(element.getAsString());
                }
            }
        } else if (parsed.isJsonPrimitive()) {
            versions.add(parsed.getAsString());
        }
        if (versions.isEmpty()) {
            throw new DshException("npm returned no versions for this package");
        }
        return versions;
    }

    /// Returns the last few output lines, for an error message.
    ///
    /// @param lines the captured output
    /// @return the trailing lines joined by newlines
    private static String tail(List<String> lines) {
        int from = Math.max(0, lines.size() - 6);
        return String.join("\n", lines.subList(from, lines.size()));
    }

    /// Splits a package spec into its name and optional version range.
    ///
    /// @param spec the spec, for example `dshmarket` or `dshmarket@1.48.0`
    /// @return the package name
    /// Reads what a published package depends on.
    ///
    /// @param packageName the package
    /// @param version     the version, or `null` for the latest
    /// @return the dependencies, empty when there are none or they cannot be read
    public static JsonObject dependencies(String packageName, @Nullable String version) {
        String spec = version == null || version.isBlank() ? packageName : packageName + "@" + version;
        JsonObject cached = DEPENDENCIES.get(spec);
        if (cached != null) {
            return cached;
        }
        JsonObject dependencies = viewJson(spec, "dependencies");
        DEPENDENCIES.put(spec, dependencies);
        return dependencies;
    }

    /// The dependencies read so far, by specification.
    private static final Map<String, JsonObject> DEPENDENCIES = new java.util.concurrent.ConcurrentHashMap<>();

    /// Reads one object field of a published package.
    ///
    /// @param spec  the package specification
    /// @param field the field
    /// @return the object, or an empty one when it is absent or cannot be read
    private static JsonObject viewJson(String spec, String field) {
        try {
            DshNodeRuntime runtime = DshNodeRuntime.detect().or(DshNodeRuntime::detectManaged).orElse(null);
            if (runtime == null || runtime.npm() == null) {
                return new JsonObject();
            }
            DshCommand.Result result = DshCommand.run(
                    List.of(runtime.npm().toString(), "view", spec, field, "--json"),
                    null, runtime.pathEnvironment(), null);
            String body = String.join("\n", result.output()).trim();
            if (result.exitCode() == 0 && body.startsWith("{")) {
                JsonElement parsed = com.google.gson.JsonParser.parseString(body);
                if (parsed.isJsonObject()) {
                    return parsed.getAsJsonObject();
                }
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warning("Could not read " + field + " of " + spec, e);
        }
        return new JsonObject();
    }

    /// What the registry says about a package at a version.
    ///
    /// Three answers, because two would be a lie: a registry that is not reachable, or an `npm` that
    /// is not installed, has not said the package is absent — and the caller decides what to do about
    /// that, which for a pack is to carry the files rather than to trust a silence.
    public enum Availability {
        /// The registry publishes this exact version.
        PUBLISHED,

        /// The registry says there is no such package, or no such version of it.
        ABSENT,

        /// The registry could not be asked.
        UNKNOWN
    }

    /// Reads the registry's own document for a package.
    ///
    /// One request for everything a version list needs at once: which versions exist, when each was
    /// published, which one each dist-tag points at, and what each declares it needs. Asking `npm
    /// view` once per version is the same data at one process per version, which for a plugin with a
    /// hundred releases is a hundred processes on a page that is only being looked at.
    ///
    /// The document is kept in memory for the session and on disk for later ones: a popular plugin's
    /// is a few hundred kilobytes, and a page opened twice should not fetch it twice. A document that
    /// cannot be fetched falls back to the kept copy — which is what a launcher that cannot reach the
    /// registry can still draw from — and answers `null` when there is none.
    ///
    /// @param packageName the package
    /// @param refresh     whether to fetch again rather than answer from memory
    /// @return the document, or `null` when it could not be read
    public static @Nullable JsonObject packument(String packageName, boolean refresh) {
        if (!refresh) {
            JsonObject known = PACKUMENTS.get(packageName);
            if (known != null) {
                return known;
            }
        }

        String address = DshPluginCatalog.npmRegistry() + "/" + packageName.replace("/", "%2f");
        Path kept = DshPluginCatalog.cachedFile(address);
        String body = null;
        try {
            body = org.jackhuang.hmcl.util.io.NetworkUtils.doGet(java.net.URI.create(address));
            keep(kept, body);
        } catch (IOException | RuntimeException e) {
            LOG.info("Could not read " + address + " (" + e.getMessage() + "); using the kept copy");
            body = read(kept);
        }
        if (body == null) {
            return null;
        }

        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonObject document = parsed.getAsJsonObject();
            PACKUMENTS.put(packageName, document);
            return document;
        } catch (RuntimeException e) {
            LOG.warning("The registry's document for " + packageName + " could not be read", e);
            return null;
        }
    }

    /// The documents read so far, by package.
    private static final Map<String, JsonObject> PACKUMENTS = new java.util.concurrent.ConcurrentHashMap<>();

    /// Keeps a copy of a document so a later fetch has something to fall back on.
    ///
    /// @param file where to keep it
    /// @param body the document
    private static void keep(Path file, String body) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, body);
        } catch (IOException e) {
            LOG.warning("Could not keep a copy at " + file, e);
        }
    }

    /// Reads a kept copy.
    ///
    /// @param file the file
    /// @return the document, or `null` when there is none
    private static @Nullable String read(Path file) {
        try {
            return Files.isRegularFile(file) ? Files.readString(file) : null;
        } catch (IOException e) {
            return null;
        }
    }

    /// Asks the registry whether a package is published at a version.
    ///
    /// The query is `npm view <name>@<version> version`, which is one request, honours whatever
    /// registry and proxy the user has configured, and answers with an error precisely when the
    /// version does not exist — `E404`, which is an answer, unlike a timeout.
    ///
    /// @param packageName the package
    /// @param version     the version
    /// @return what the registry says
    public static Availability availability(String packageName, String version) {
        DshNodeRuntime runtime = DshNodeRuntime.detect().or(DshNodeRuntime::detectManaged).orElse(null);
        if (runtime == null || runtime.npm() == null) {
            return Availability.UNKNOWN;
        }
        try {
            DshCommand.Result result = DshCommand.run(List.of(runtime.npm().toString(),
                    "view", packageName + "@" + version, "version", "--json"),
                    null, runtime.pathEnvironment(), null);
            return availabilityOf(result.exitCode(), result.text());
        } catch (IOException e) {
            LOG.warning("Could not ask the registry about " + packageName + "@" + version, e);
            return Availability.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Availability.UNKNOWN;
        }
    }

    /// Reads what a registry answer means.
    ///
    /// Kept apart from the command that produces it so the reading can be pinned: exit zero is the
    /// version, and a 404 in the output is the registry saying there is no such thing — anything else
    /// (a missing network, a registry that is down, a package name the registry rejects the syntax
    /// of) is not an answer at all.
    ///
    /// @param exitCode the command's exit code
    /// @param output   what it printed
    /// @return what the registry says
    static Availability availabilityOf(int exitCode, String output) {
        if (exitCode == 0) {
            return Availability.PUBLISHED;
        }
        String text = output == null ? "" : output;
        return text.contains("E404") || text.contains("404 Not Found") || text.contains("is not in this registry")
                ? Availability.ABSENT : Availability.UNKNOWN;
    }

    /// Reads the peer requirements a published package declares.
    ///
    /// Asked of the package rather than of the catalogue, because the catalogue does not
    /// carry them. The answer is remembered: a page asks about the same packages again
    /// whenever it is searched, and a registry is not to be asked twice for the same
    /// thing.
    ///
    /// @param packageName the package
    /// @param version     the version, or `null` for the latest
    /// @return the peer requirements, empty when there are none or they cannot be read
    public static JsonObject peerDependencies(String packageName, @Nullable String version) {
        String spec = version == null || version.isBlank() ? packageName : packageName + "@" + version;
        JsonObject cached = PEERS.get(spec);
        if (cached != null) {
            return cached;
        }

        JsonObject peers = new JsonObject();
        try {
            DshNodeRuntime runtime = DshNodeRuntime.detect().or(DshNodeRuntime::detectManaged).orElse(null);
            if (runtime == null || runtime.npm() == null) {
                return peers;
            }
            DshCommand.Result result = DshCommand.run(
                    List.of(runtime.npm().toString(), "view", spec, "peerDependencies", "--json"),
                    null, runtime.pathEnvironment(), null);
            String body = String.join("\n", result.output()).trim();
            if (result.exitCode() == 0 && body.startsWith("{")) {
                JsonElement parsed = com.google.gson.JsonParser.parseString(body);
                if (parsed.isJsonObject()) {
                    peers = parsed.getAsJsonObject();
                }
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.warning("Could not read the peer requirements of " + spec, e);
        }

        PEERS.put(spec, peers);
        return peers;
    }

    /// The peer requirements read so far, by specification.
    private static final Map<String, JsonObject> PEERS = new java.util.concurrent.ConcurrentHashMap<>();

    public static String packageNameOf(String spec) {
        String trimmed = spec.trim();
        if (trimmed.startsWith("@")) {
            int slash = trimmed.indexOf('/');
            if (slash < 0) {
                return trimmed;
            }
            int at = trimmed.indexOf('@', slash);
            return at < 0 ? trimmed : trimmed.substring(0, at);
        }
        int at = trimmed.indexOf('@');
        return at < 0 ? trimmed : trimmed.substring(0, at);
    }

    /// Returns the version part of a package spec.
    ///
    /// @param spec the spec
    /// @return the version range, or `null` when the spec carries none
    public static @Nullable String versionOf(String spec) {
        String trimmed = spec.trim();
        int at = trimmed.startsWith("@") ? trimmed.indexOf('@', trimmed.indexOf('/') + 1) : trimmed.indexOf('@');
        return at < 0 || at + 1 >= trimmed.length() ? null : trimmed.substring(at + 1);
    }
}
