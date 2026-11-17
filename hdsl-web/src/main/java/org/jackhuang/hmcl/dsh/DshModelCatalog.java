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
import org.jackhuang.hmcl.util.io.HttpClients;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The model directory published at <https://models.dev>, and what this launcher reads out of it.
///
/// A supplier's own `/models` endpoint answers "which ids do you serve" — and only that: no
/// context window, no price, no display name. models.dev is the same information written down
/// once for all of them, keyed by the id `pi-ai` uses and the harness addresses routes by, which
/// is why the two lists line up: twenty-six of the thirty-seven suppliers in
/// `assets/dsh-providers.txt` are in it under the same id, and most of the rest under the same
/// host name.
///
/// What that buys is [DshAccount]'s `model`: instead of typing `deepseek-chat` from memory into a
/// text box, the account page can offer the supplier's own models by name. What it deliberately
/// does **not** buy is the wire protocol: models.dev names an npm package per supplier, and
/// mapping that to one of [DshVendor#APIS] is a guess, so the catalogue only ever supplements
/// what the launcher's own table already says. [protocolOf] is the guess, and it answers `null`
/// when it does not know — the interface shows such a supplier as unusable rather than writing a
/// route the harness cannot register.
@NotNullByDefault
public final class DshModelCatalog {
    /// Where the directory is published.
    public static final String DEFAULT_URL = "https://models.dev/api.json";

    /// How long a request may take in total.
    ///
    /// The document is about six megabytes uncompressed and a little under 500 KB gzipped.
    /// Twenty seconds was enough for a fast link and never for a slow one — on a line that
    /// trickles at single-digit KB/s even the gzipped body needs minutes, and every refresh
    /// timed out, so the snapshot silently aged for days. Gzip is asked for below, the limit
    /// is five minutes, and the callers that can wait do so in the background.
    private static final Duration TIMEOUT = Duration.ofSeconds(300);

    /// How long the connection may take to be established.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);

    /// Who is asking. Same shape as [DshAccount]'s, so the two are recognisable as one program.
    private static final String USER_AGENT = org.jackhuang.hmcl.Metadata.NAME + "/"
            + org.jackhuang.hmcl.Metadata.VERSION + " (+" + org.jackhuang.hmcl.Metadata.HOMEPAGE_URL + ")";

    /// The npm packages models.dev publishes, and the protocol each one speaks.
    ///
    /// Not a complete mapping and not meant to be: packages such as `@ai-sdk/azure` and
    /// `@ai-sdk/amazon-bedrock` are SDKs whose authentication is a signature this launcher cannot
    /// produce, so a supplier published under one of those is one the harness cannot route, and
    /// leaving it out is the answer. The value of the map is the long tail: openai-compatible
    /// gateways are the overwhelming majority of the directory.
    private static final Map<String, String> PROTOCOLS = Map.ofEntries(
            Map.entry("@ai-sdk/openai-compatible", "openai-completions"),
            Map.entry("@ai-sdk/openai", "openai-completions"),
            Map.entry("@ai-sdk/cerebras", "openai-completions"),
            Map.entry("@ai-sdk/google", "openai-completions"),
            Map.entry("@ai-sdk/groq", "openai-completions"),
            Map.entry("@ai-sdk/mistral", "openai-completions"),
            Map.entry("@ai-sdk/xai", "openai-completions"),
            Map.entry("@openrouter/ai-sdk-provider", "openai-completions"),
            Map.entry("ai-gateway-provider", "openai-completions"),
            Map.entry("@ai-sdk/anthropic", "anthropic-messages"));

    /// One model as the interface shows it.
    ///
    /// A fraction of what the document carries: the fields an account page puts beside a name —
    /// how much it can hold, what it can be asked to do, what it costs. The pricing numbers are
    /// US dollars per million tokens, as published.
    public record Model(
            String id,
            String name,
            /// Context window in tokens, or `null` when the directory does not say.
            @Nullable Integer context,
            /// Largest answer in tokens, or `null`.
            @Nullable Integer output,
            boolean reasoning,
            /// The efforts this model accepts, in the directory's order; empty when it names none.
            List<String> reasoningEfforts,
            boolean toolCall,
            /// Whether it takes files and images alongside the prompt.
            boolean attachment,
            @Nullable Double costInput,
            @Nullable Double costOutput,
            @Nullable String status) {
    }

    /// One supplier as the directory describes it.
    public record Provider(
            String id,
            String name,
            /// The address its models are served from, or `null` when it depends on the account.
            @Nullable String endpoint,
            /// The variable names the directory expects the key in. Shown, never used: the harness
            /// reads its own name, which is derived from the id.
            List<String> env,
            /// The npm package the directory publishes it under; what [protocolOf] reads.
            String npm,
            @Nullable String doc,
            List<Model> models) {
    }

    /// The directory, read once.
    public record Catalog(long fetchedAtMillis, List<Provider> providers) {

        /// Finds a supplier by its directory id.
        ///
        /// @param id the id, or `null`
        /// @return the supplier, or `null` when the directory does not hold it
        public @Nullable Provider byId(@Nullable String id) {
            if (id == null || id.isBlank()) {
                return null;
            }
            String wanted = id.trim();
            for (Provider provider : providers) {
                if (provider.id().equalsIgnoreCase(wanted)) {
                    return provider;
                }
            }
            return null;
        }
    }

    private DshModelCatalog() {
    }

    /// The address read, which the `hdsl.modelCatalog` property may override.
    ///
    /// The override exists so a test can point the launcher at one of its own, and so an operator
    /// on a network that cannot reach the public host has somewhere to point it instead.
    ///
    /// @return the address
    public static String catalogUrl() {
        String configured = System.getProperty("hdsl.modelCatalog");
        return configured == null || configured.isBlank() ? DEFAULT_URL : configured.trim();
    }

    /// Returns the file the fetched directory is kept in.
    ///
    /// The name comes from the address, as the plugin catalogue's does and for the same reason:
    /// pointing the launcher at another directory does not overwrite the copy of this one.
    ///
    /// @return the file, which may not exist
    static Path cachedFile() {
        String url = catalogUrl();
        String name;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            StringBuilder hex = new StringBuilder();
            for (byte value : digest.digest(url.getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", value));
            }
            name = hex.substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            name = "unhashed";
        }
        return DshPluginCatalog.cacheDirectory().resolve("models-dev-" + name + ".json");
    }

    /// Reads the directory from the network, keeping a copy for later.
    ///
    /// @return the directory
    /// @throws DshException when it cannot be read or does not parse
    public static Catalog fetch() throws DshException {
        String url = catalogUrl();
        String body;
        try {
            body = download(url);
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DshException("Failed to read the model catalogue from " + url
                    + " (" + e.getMessage() + ")", e);
        }
        try {
            // Parsed before it is kept: a document this cannot read must not replace the copy
            // that a later request will fall back on.
            Catalog parsed = parse(body);
            keepCopy(cachedFile(), body);
            return parsed;
        } catch (RuntimeException e) {
            throw new DshException("The model catalogue at " + url + " had an unexpected shape", e);
        }
    }

    /// Reads the copy kept from an earlier fetch.
    ///
    /// @return the directory, or `null` when there is no readable copy
    public static @Nullable Catalog readCached() {
        Path file = cachedFile();
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            Catalog parsed = parse(Files.readString(file));
            // The time of the copy, not of this call: what the interface shows is how old the
            // models are, and a fallback served now was not fetched now.
            return new Catalog(Files.getLastModifiedTime(file).toMillis(), parsed.providers());
        } catch (IOException | RuntimeException e) {
            LOG.info("Could not read the kept model catalogue at " + file, e);
            return null;
        }
    }

    /// Reads the protocol a supplier published under an npm package speaks.
    ///
    /// @param npm the package, or `null`
    /// @return one of [DshVendor#APIS], or `null` when this launcher cannot route it
    public static @Nullable String protocolOf(@Nullable String npm) {
        if (npm == null || npm.isBlank()) {
            return null;
        }
        String protocol = PROTOCOLS.get(npm.trim());
        return protocol != null && DshVendor.APIS.contains(protocol) ? protocol : null;
    }

    /// Finds the directory's entry for a supplier the launcher knows.
    ///
    /// Three questions, in the order that gets an answer right most often: the same id, an id the
    /// alias file says is the same supplier under another name, and finally the host of the
    /// address — compared by host alone, because the directory writes an address with and without
    /// `/v1` and with and without a trailing slash, and all of those are one service.
    ///
    /// @param catalog  the directory
    /// @param vendorId the launcher's id for the supplier, or `null`
    /// @param endpoint the supplier's address, or `null`
    /// @return the entry, or `null` when the directory does not hold the supplier
    public static @Nullable Provider match(Catalog catalog, @Nullable String vendorId, @Nullable String endpoint) {
        if (vendorId != null && !vendorId.isBlank()) {
            Provider exact = catalog.byId(vendorId);
            if (exact != null) {
                return exact;
            }
            String alias = aliases().get(vendorId.trim().toLowerCase(Locale.ROOT));
            if (alias != null) {
                Provider renamed = catalog.byId(alias);
                if (renamed != null) {
                    return renamed;
                }
            }
        }
        String host = DshVendor.hostOf(endpoint);
        if (host == null) {
            return null;
        }
        for (Provider provider : catalog.providers()) {
            if (host.equals(DshVendor.hostOf(provider.endpoint()))) {
                return provider;
            }
        }
        return null;
    }

    /// Parses the document.
    ///
    /// @param body the JSON
    /// @return the directory
    /// @throws RuntimeException when the document is not the shape this reads
    static Catalog parse(String body) {
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        List<Provider> providers = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject object = entry.getValue().getAsJsonObject();
            String id = text(object, "id", entry.getKey());
            if (id == null || id.isBlank()) {
                continue;
            }
            providers.add(new Provider(
                    id,
                    name(object, id),
                    optionalText(object, "api"),
                    strings(object.get("env")),
                    text(object, "npm", ""),
                    optionalText(object, "doc"),
                    models(object.get("models"))));
        }
        providers.sort(Comparator.comparing(provider -> provider.id().toLowerCase(Locale.ROOT)));
        return new Catalog(System.currentTimeMillis(), List.copyOf(providers));
    }

    /// Reads one supplier's models.
    ///
    /// @param element the `models` member
    /// @return the models, by name
    private static List<Model> models(@Nullable JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return List.of();
        }
        List<Model> models = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject object = entry.getValue().getAsJsonObject();
            String id = text(object, "id", entry.getKey());
            if (id == null || id.isBlank()) {
                continue;
            }
            JsonObject limit = member(object, "limit");
            JsonObject cost = member(object, "cost");
            models.add(new Model(
                    id,
                    name(object, id),
                    integer(limit, "context"),
                    integer(limit, "output"),
                    flag(object, "reasoning"),
                    efforts(object),
                    flag(object, "tool_call"),
                    flag(object, "attachment"),
                    number(cost, "input"),
                    number(cost, "output"),
                    optionalText(object, "status")));
        }
        models.sort(Comparator.comparing(model -> model.name().toLowerCase(Locale.ROOT)));
        return List.copyOf(models);
    }

    /// Reads the efforts a model accepts out of its reasoning options.
    ///
    /// @param model the model's object
    /// @return the effort names, empty when it names none
    private static List<String> efforts(JsonObject model) {
        JsonElement options = model.get("reasoning_options");
        if (options == null || !options.isJsonArray()) {
            return List.of();
        }
        for (JsonElement option : options.getAsJsonArray()) {
            if (!option.isJsonObject()) {
                continue;
            }
            JsonObject object = option.getAsJsonObject();
            if ("effort".equals(optionalText(object, "type"))) {
                return strings(object.get("values"));
            }
        }
        return List.of();
    }

    /// Reads the aliases, once.
    ///
    /// @return launcher id (lowercase) to directory id
    static Map<String, String> aliases() {
        Map<String, String> kept = ALIASES;
        if (kept != null) {
            return kept;
        }
        synchronized (DshModelCatalog.class) {
            if (ALIASES == null) {
                ALIASES = readAliases();
            }
            return ALIASES;
        }
    }

    private static volatile @Nullable Map<String, String> ALIASES;

    /// Reads the alias file.
    ///
    /// A line that cannot be read is skipped rather than fatal, for the same reason
    /// [DshVendor] skips one: knowing one supplier fewer is better than not starting.
    ///
    /// @return launcher id (lowercase) to directory id
    private static Map<String, String> readAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        try (java.io.InputStream stream = DshModelCatalog.class
                .getResourceAsStream("/assets/models-dev-aliases.txt")) {
            if (stream == null) {
                LOG.warning("No models.dev alias file in the jar; only ids will match");
                return Map.of();
            }
            for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                    continue;
                }
                aliases.put(parts[0].trim().toLowerCase(Locale.ROOT), parts[1].trim());
            }
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read the models.dev alias file", e);
            return Map.of();
        }
        return Map.copyOf(aliases);
    }

    /// Reads the document from the network.
    ///
    /// A client per call, as [DshAccount] does: this is a request a person waits for, not a
    /// stream, and the connection is not worth keeping open for twelve hours between them.
    ///
    /// @param url the address
    /// @return the body
    /// @throws IOException when the request fails or answers with an error
    private static String download(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                // The JDK client neither asks for gzip nor unpacks it; doing both turns a
                // six-megabyte document into ~460 KB, which is the difference between a refresh
                // that finishes on a slow line and one that times out forever.
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        try {
            // `send` with `BodyHandlers.ofString` is bounded by the request timeout only up to
            // the response head in practice: a slow body can hang for minutes while the page's
            // own 30-second abort fires first. Waiting on the future bounds the whole exchange,
            // body included — which is the one a five-megabyte document actually spends time on.
            java.util.concurrent.CompletableFuture<HttpResponse<byte[]>> pending =
                    client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
            HttpResponse<byte[]> response;
            try {
                response = pending.get(TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                pending.cancel(true);
                throw new IOException("timed out after " + TIMEOUT.toSeconds() + "s waiting for the body", e);
            } catch (java.util.concurrent.ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException io) {
                    throw io;
                }
                throw new IOException(cause == null ? "request failed" : cause.toString(), cause);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode());
            }
            byte[] body = response.body();
            String encoding = response.headers().firstValue("Content-Encoding").orElse("");
            if (body != null && encoding.toLowerCase(java.util.Locale.ROOT).contains("gzip")) {
                try (java.util.zip.GZIPInputStream gzip =
                             new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(body))) {
                    body = gzip.readAllBytes();
                }
            }
            if (body == null) {
                throw new IOException("empty body");
            }
            return new String(body, java.nio.charset.StandardCharsets.UTF_8);
        } finally {
            HttpClients.closeQuietly(client);
        }
    }

    /// Keeps a copy of the document so a later fetch has something to fall back on.
    ///
    /// @param file where to keep it
    /// @param body the document
    private static void keepCopy(Path file, String body) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, body);
        } catch (IOException e) {
            LOG.warning("Could not keep a copy of the model catalogue", e);
        }
    }

    private static @Nullable JsonObject member(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static @Nullable String optionalText(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        String value = element.getAsString();
        return value.isBlank() ? null : value;
    }

    private static String text(JsonObject object, String name, String fallback) {
        String value = optionalText(object, name);
        return value == null ? fallback : value;
    }

    /// Reads a name, falling back to the id when the document has none.
    ///
    /// @param object the object
    /// @param fallback what to show when it names nothing
    /// @return the name
    private static String name(JsonObject object, String fallback) {
        String value = optionalText(object, "name");
        return value == null ? fallback : value;
    }

    /// Reads a boolean.
    ///
    /// Written to survive a field of the wrong type rather than to read one: a directory with
    /// three thousand models in it is written by many hands, and one odd member must not cost the
    /// page the other two hundred and twenty-five suppliers.
    private static boolean flag(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean();
    }

    private static @Nullable Integer integer(@Nullable JsonObject object, String name) {
        JsonElement element = object == null ? null : object.get(name);
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isNumber() ? element.getAsInt() : null;
    }

    private static @Nullable Double number(@Nullable JsonObject object, String name) {
        JsonElement element = object == null ? null : object.get(name);
        return element != null && element.isJsonPrimitive()
                && element.getAsJsonPrimitive().isNumber() ? element.getAsDouble() : null;
    }

    private static List<String> strings(@Nullable JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement one : element.getAsJsonArray()) {
            if (one.isJsonPrimitive() && !one.getAsString().isBlank()) {
                values.add(one.getAsString());
            }
        }
        return List.copyOf(values);
    }
}
