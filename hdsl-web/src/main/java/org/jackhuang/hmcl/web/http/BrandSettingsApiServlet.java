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
package org.jackhuang.hmcl.web.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.util.io.HttpClients;
import org.jackhuang.hmcl.web.audit.AuditLog;
import org.jackhuang.hmcl.web.auth.AuthFilter;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.brand.Brand;
import org.jackhuang.hmcl.web.brand.BrandInstance;
import org.jackhuang.hmcl.web.brand.BrandInstanceManager;
import org.jackhuang.hmcl.web.brand.BrandPolicy;
import org.jackhuang.hmcl.web.brand.BrandRuntime;
import org.jackhuang.hmcl.web.brand.BrandSources;
import org.jackhuang.hmcl.web.brand.GitHubToken;
import org.jackhuang.hmcl.web.developer.DeveloperElevation;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The brand page's data, mapped at `/api/settings/brands/*`:
///
/// - `GET  /api/settings/brands` — everything the page shows without a password: the download
///   sources in force plus every brand's policy switches and instances. Read is open on purpose:
///   the page must be able to say "there are three Octop instances" before anybody has typed a
///   developer password, instead of pretending the brands do not exist.
/// - `PUT  /api/settings/brands/policy/<brand>` — the last-resort switches (single instance,
///   fixed mount, local source build). Needs elevation.
/// - `POST /api/settings/brands/sources` — the PyPI index, the CPython mirror and each brand's
///   release base. All-or-nothing: a body with one bad address changes nothing.
/// - `POST /api/settings/brands/test` — measures one candidate address from this machine and
///   answers `{kind, url, ok, status, millis}`. The page's "测速" is this endpoint; it exists
///   because "reachable but 40 KB/s" is exactly the problem the setting solves.
@NotNullByDefault
public final class BrandSettingsApiServlet extends HttpServlet {

    /// How long one candidate may take. A mirror nobody can answer from in five seconds is the
    /// answer, and the page probes several of them at once.
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(5);

    /// The package the PyPI probe asks for: small, and in every mirror that exists.
    private static final String PROBE_PACKAGE = "uv";

    /// The repository the archive probe asks for: a few hundred bytes, and old enough that any
    /// codeload-style mirror has it.
    private static final String PROBE_REPOSITORY = "octocat/Hello-World";

    private static final String USER_AGENT = Metadata.NAME + "/" + Metadata.VERSION
            + " (+" + Metadata.HOMEPAGE_URL + ")";

    private final ServerConfig config;
    private final DeveloperElevation elevation;
    private final AuthService auth;
    private final BooleanSupplier tlsActive;
    private final BrandPolicy policy;

    public BrandSettingsApiServlet(ServerConfig config, DeveloperElevation elevation, AuthService auth,
                                   BooleanSupplier tlsActive) {
        this.config = config;
        this.elevation = elevation;
        this.auth = auth;
        this.tlsActive = tlsActive;
        this.policy = new BrandPolicy(config.dataDir);
        GitHubToken.bind(config.dataDir);
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        String method = request.getMethod();
        String route = path == null ? "" : path;

        if ("GET".equals(method) && (route.isEmpty() || "/".equals(route))) {
            Json.writePreservingNulls(response, describe());
            return;
        }
        if ("PUT".equals(method) && route.startsWith("/policy/")) {
            if (!requireElevation(request, response)) {
                return;
            }
            setPolicy(request, response, route.substring("/policy/".length()));
            return;
        }
        if ("GET".equals(method) && "/github-token".equals(route)) {
            response.setHeader("Cache-Control", "no-store");
            Json.writePreservingNulls(response, describeGitHubToken(GitHubToken.status()));
            return;
        }
        if (("POST".equals(method) || "DELETE".equals(method)) && "/github-token".equals(route)) {
            if (!requireElevation(request, response)) {
                return;
            }
            if ("DELETE".equals(method)) {
                clearGitHubToken(request, response);
            } else {
                saveGitHubToken(request, response);
            }
            return;
        }
        if ("POST".equals(method) && "/sources".equals(route)) {
            if (!requireElevation(request, response)) {
                return;
            }
            JsonObject body = body(request, response);
            if (body == null) {
                return;
            }
            saveSources(request, body, response);
            return;
        }
        if ("POST".equals(method) && "/test".equals(route)) {
            if (!requireElevation(request, response)) {
                return;
            }
            JsonObject body = body(request, response);
            if (body == null) {
                return;
            }
            probe(body, response);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    /// The token's status: whether one is configured and which login it is,
    /// never the token itself.
    private static JsonObject describeGitHubToken(GitHubToken.Status status) {
        JsonObject body = new JsonObject();
        body.addProperty("configured", status.configured());
        if (status.login() == null) {
            body.add("login", JsonNull.INSTANCE);
        } else {
            body.addProperty("login", status.login());
        }
        return body;
    }

    private void saveGitHubToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String token = stringField(body, "token");
        if (token == null || token.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "token is required");
            return;
        }
        try {
            GitHubToken.Status status = GitHubToken.save(token);
            AuditLog.log(AuditLog.Channel.WEB, "settings.github-token", "set", "success",
                    actorOf(request), request.getRemoteAddr());
            response.setHeader("Cache-Control", "no-store");
            Json.writePreservingNulls(response, describeGitHubToken(status));
        } catch (GitHubToken.InvalidTokenException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_BAD_GATEWAY,
                    "无法保存 GitHub 令牌：" + e.getMessage());
        }
    }

    private void clearGitHubToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            GitHubToken.clear();
            AuditLog.log(AuditLog.Channel.WEB, "settings.github-token", "clear", "success",
                    actorOf(request), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "无法删除 GitHub 令牌：" + e.getMessage());
        }
    }

    /// Describes the sources and the policy as the page needs them.
    private JsonObject describe() {
        JsonObject sources = new JsonObject();
        sources.add("pypi", describeSource(
                BrandSources.pypiPresets(),
                SettingsManager.settings().getBrandPypiIndexPreset(),
                SettingsManager.settings().getBrandPypiIndex(),
                BrandSources.pypi()));
        sources.add("python", describeSource(
                BrandSources.pythonPresets(),
                SettingsManager.settings().getBrandPythonMirrorPreset(),
                SettingsManager.settings().getBrandPythonMirror(),
                BrandSources.python()));
        sources.add("archive", describeSource(
                BrandSources.archivePresets(),
                SettingsManager.settings().getBrandArchivePreset(),
                SettingsManager.settings().getBrandArchiveBase(),
                BrandSources.archive()));

        JsonObject release = new JsonObject();
        for (Brand brand : Brand.values()) {
            if (brand.source() != Brand.Source.GITHUB_RELEASE) {
                continue;
            }
            release.add(brand.id(), describeSource(
                    BrandSources.releasePresets(brand),
                    SettingsManager.settings().getBrandReleasePreset(brand.id()),
                    SettingsManager.settings().getBrandReleaseBase(brand.id()),
                    BrandSources.release(brand)));
        }
        sources.add("release", release);

        JsonObject result = new JsonObject();
        result.add("sources", sources);
        result.add("policy", policyStatus());
        return result;
    }

    /// One kind of source: what is chosen, what was typed, what is in force, and the mirrors.
    private static JsonObject describeSource(List<BrandSources.Preset> presets, String presetId,
                                             String custom, BrandSources.Effective effective) {
        JsonArray offered = new JsonArray();
        for (BrandSources.Preset preset : presets) {
            JsonObject entry = new JsonObject();
            entry.addProperty("id", preset.id());
            entry.addProperty("label", preset.label());
            entry.addProperty("url", preset.url());
            entry.addProperty("note", preset.note());
            offered.add(entry);
        }
        JsonObject body = new JsonObject();
        body.addProperty("preset", presetId);
        // The last address typed by hand, kept even while another preset is chosen: switching back
        // to `custom` should show what was there rather than an empty box.
        body.addProperty("url", custom);
        body.addProperty("effective", effective.url());
        body.addProperty("source", effective.source());
        body.add("presets", offered);
        return body;
    }

    /// Every brand's switches plus its instances.
    private JsonObject policyStatus() {
        JsonArray brands = new JsonArray();
        for (Brand brand : Brand.values()) {
            BrandPolicy.Entry entry = policy.get(brand);
            JsonObject body = new JsonObject();
            body.addProperty("brand", brand.id());
            JsonArray capabilities = new JsonArray();
            for (String capability : capabilities(brand)) {
                capabilities.add(capability);
            }
            body.add("capabilities", capabilities);
            body.addProperty("singleInstance", entry.singleInstance());
            if (entry.survivor() == null) {
                body.add("survivor", JsonNull.INSTANCE);
            } else {
                body.addProperty("survivor", entry.survivor());
            }
            body.addProperty("lockedRoute", entry.lockedRoute());
            body.addProperty("sourceBuild", entry.sourceBuild());
            if (entry.nodeRuntime() == null) {
                body.add("nodeRuntime", JsonNull.INSTANCE);
            } else {
                body.addProperty("nodeRuntime", entry.nodeRuntime());
            }
            JsonArray instances = new JsonArray();
            for (BrandInstance instance : manager(brand).list()) {
                JsonObject row = new JsonObject();
                row.addProperty("id", instance.id());
                row.addProperty("name", instance.name());
                row.addProperty("version", instance.version());
                row.addProperty("state", BrandRuntime.status(instance.id()).state().name()
                        .toLowerCase(java.util.Locale.ROOT));
                row.addProperty("disabled", policy.disabled(brand, instance.id()));
                instances.add(row);
            }
            body.add("instances", instances);
            brands.add(body);
        }
        JsonObject result = new JsonObject();
        result.add("brands", brands);
        return result;
    }

    /// The switches a brand actually has. Data, not a special case in the page: a brand that grows
    /// one later only changes this method.
    ///
    /// `singleInstance`/`lockedRoute` exist only where the tool's own agent can wedge the product
    /// ([Brand#supportsLastResort]); `sourceBuild` stays Octop's.
    private static List<String> capabilities(Brand brand) {
        List<String> capabilities = new java.util.ArrayList<>();
        if (brand.supportsLastResort()) {
            capabilities.add("singleInstance");
            capabilities.add("lockedRoute");
        }
        if (brand == Brand.OCTOP) {
            capabilities.add("sourceBuild");
        }
        return capabilities;
    }

    /// Applies a policy change for one brand.
    private void setPolicy(HttpServletRequest request, HttpServletResponse response, String brandId)
            throws IOException {
        Brand brand = Brand.fromId(brandId);
        if (brand == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "unknown brand");
            return;
        }
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        Boolean single = booleanField(body, "singleInstance");
        Boolean locked = booleanField(body, "lockedRoute");
        Boolean source = booleanField(body, "sourceBuild");
        String survivor = stringField(body, "survivor");
        if (survivor != null && survivor.isBlank()) {
            survivor = null;
        }
        boolean singleInstance = single != null && single;
        boolean lockedRoute = locked != null && locked;
        boolean sourceBuild = source != null && source;
        // Omitted means "leave it as it is": this PUT replaces the whole entry,
        // and an older client that knows nothing about the field must not clear
        // a brand default somebody set in the new UI.
        String nodeRuntime = policy.get(brand).nodeRuntime();
        if (body.has("nodeRuntime")) {
            JsonElement element = body.get("nodeRuntime");
            if (!element.isJsonNull() && (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "nodeRuntime must be a string selector or null");
                return;
            }
            nodeRuntime = element.isJsonNull() ? null : element.getAsString().trim();
            if (nodeRuntime != null && nodeRuntime.isBlank()) {
                nodeRuntime = null;
            }
            if (nodeRuntime != null && org.jackhuang.hmcl.dsh.NodeSelection.parse(nodeRuntime).kind()
                    == org.jackhuang.hmcl.dsh.NodeSelection.Kind.INVALID) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "unknown nodeRuntime selector: " + nodeRuntime);
                return;
            }
        }
        List<String> capabilities = capabilities(brand);
        if (singleInstance && !capabilities.contains("singleInstance")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "singleInstance is not supported for " + brand.id());
            return;
        }
        if (lockedRoute && !capabilities.contains("lockedRoute")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "lockedRoute is not supported for " + brand.id());
            return;
        }
        if (sourceBuild && !capabilities.contains("sourceBuild")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "sourceBuild is not supported for " + brand.id());
            return;
        }
        if (singleInstance && survivor == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "singleInstance requires a survivor instance");
            return;
        }
        BrandInstanceManager manager = manager(brand);
        if (survivor != null && manager.find(survivor) == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "survivor is not an instance of " + brand.id());
            return;
        }
        try {
            if (singleInstance) {
                for (BrandInstance instance : manager.list()) {
                    if (!instance.id().equals(survivor) && BrandRuntime.isRunning(instance.id())) {
                        BrandRuntime.stop(instance.id());
                    }
                }
            }
            policy.set(brand, new BrandPolicy.Entry(singleInstance, survivor, lockedRoute, sourceBuild, nodeRuntime));
            if (body.has("nodeRuntime")) {
                AuditLog.log(AuditLog.Channel.WEB, "brand.node", brand.id(), "success",
                        actorOf(request), request.getRemoteAddr());
            }
            if (brand.supportsLastResort()) {
                BrandRuntime.setLockedSurvivor(brand, lockedRoute ? survivor : null);
            }
            AuditLog.log(AuditLog.Channel.WEB, "brand.policy", brand.id(), "success",
                    actorOf(request), request.getRemoteAddr());
            JsonObject result = new JsonObject();
            result.addProperty("ok", true);
            Json.write(response, result);
        } catch (IllegalArgumentException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            Json.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "could not save the brand policy");
        }
    }

    /// Saves the download sources. All-or-nothing, like the npm registry's own save.
    private void saveSources(HttpServletRequest request, JsonObject body, HttpServletResponse response)
            throws IOException {
        SourceChoice pypi;
        SourceChoice python;
        SourceChoice archive;
        List<SourceChoice> releases = new java.util.ArrayList<>();
        try {
            pypi = readChoice(body.get("pypi"), BrandSources.pypiPresets(), "PyPI 索引", "");
            python = readChoice(body.get("python"), BrandSources.pythonPresets(), "CPython 镜像", "");
            // Optional: a caller that predates the archive source leaves it alone rather than
            // having its whole save refused.
            JsonElement archiveElement = body.get("archive");
            archive = archiveElement == null ? null
                    : readChoice(archiveElement, BrandSources.archivePresets(), "源码归档源", "");
            JsonObject release = body.get("release") instanceof JsonObject object ? object : new JsonObject();
            for (String brandId : release.keySet()) {
                Brand brand = Brand.fromId(brandId);
                if (brand == null || brand.source() != Brand.Source.GITHUB_RELEASE) {
                    throw new BadSource("unknown release brand: " + brandId);
                }
                releases.add(readChoice(release.get(brandId),
                        BrandSources.releasePresets(brand), brand.id() + " 的 release 源", brand.id()));
            }
        } catch (BadSource e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        // Everything validated; only now does anything get written.
        SettingsManager.settings().setBrandPypiIndexPreset(pypi.preset());
        SettingsManager.settings().setBrandPypiIndex(pypi.url());
        SettingsManager.settings().setBrandPythonMirrorPreset(python.preset());
        SettingsManager.settings().setBrandPythonMirror(python.url());
        if (archive != null) {
            SettingsManager.settings().setBrandArchivePreset(archive.preset());
            SettingsManager.settings().setBrandArchiveBase(archive.url());
        }
        for (SourceChoice choice : releases) {
            SettingsManager.settings().setBrandReleasePreset(choice.brandId(), choice.preset());
            SettingsManager.settings().setBrandReleaseBase(choice.brandId(), choice.url());
        }
        LOG.info("Brand download sources are now PyPI=" + BrandSources.pypi().url()
                + " (" + BrandSources.pypi().source() + "), CPython=" + BrandSources.python().url()
                + " (" + BrandSources.python().source() + "), archives="
                + BrandSources.archive().url() + " (" + BrandSources.archive().source() + ")");
        AuditLog.log(AuditLog.Channel.WEB, "settings.brand-sources",
                "pypi=" + BrandSources.pypi().url() + ", python=" + BrandSources.python().url()
                        + ", archive=" + BrandSources.archive().url(),
                "success", actorOf(request), request.getRemoteAddr());
        Json.writePreservingNulls(response, describe());
    }

    /// One source choice, already validated.
    ///
    /// @param brandId the brand a release choice belongs to; empty for the global kinds
    private record SourceChoice(String brandId, String preset, String url) {
    }

    /// A source choice the page should be told about rather than have applied.
    private static final class BadSource extends Exception {
        private static final long serialVersionUID = 1L;

        BadSource(String message) {
            super(message);
        }
    }

    /// Validates one source choice.
    ///
    /// @param brandId the brand a release choice belongs to; empty for the global kinds
    /// @throws BadSource when the choice must be refused, carrying the sentence the page shows
    private SourceChoice readChoice(@Nullable JsonElement element, List<BrandSources.Preset> presets,
                                    String what, String brandId) throws BadSource {
        if (!(element instanceof JsonObject object)) {
            throw new BadSource(what + "：缺少配置");
        }
        String preset = stringField(object, "preset");
        String url = stringField(object, "url");
        if (preset == null || preset.isBlank()) {
            throw new BadSource(what + "：没有选择下载源");
        }
        String wanted = preset.trim();
        if (BrandSources.CUSTOM.equals(wanted)) {
            try {
                return new SourceChoice(brandId, wanted, BrandSources.normalize(url));
            } catch (BrandSources.Invalid e) {
                throw new BadSource(what + "：" + e.getMessage());
            }
        }
        if (!BrandSources.ENVIRONMENT.equals(wanted) && BrandSources.preset(presets, wanted) == null) {
            throw new BadSource(what + "：没有这个下载源 " + wanted);
        }
        return new SourceChoice(brandId, wanted, url == null ? "" : url.trim());
    }

    /// Measures one address.
    private static void probe(JsonObject body, HttpServletResponse response) throws IOException {
        String kind = stringField(body, "kind");
        String brandId = stringField(body, "brand");
        String typed = stringField(body, "url");

        // The kind is checked first: a body naming no kind must not be treated as "probe whatever
        // address it carries", or the endpoint becomes a general fetcher.
        Brand brand = null;
        if ("pypi".equals(kind)) {
            // nothing else to resolve
        } else if ("python".equals(kind)) {
            // nothing else to resolve
        } else if ("archive".equals(kind)) {
            // nothing else to resolve
        } else if ("release".equals(kind)) {
            brand = brandId == null ? null : Brand.fromId(brandId);
            if (brand == null || brand.source() != Brand.Source.GITHUB_RELEASE) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown release brand");
                return;
            }
        } else {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "unknown source kind: " + kind);
            return;
        }

        String base = typed;
        if (base == null || base.isBlank()) {
            BrandSources.Effective effective = "pypi".equals(kind) ? BrandSources.pypi()
                    : "python".equals(kind) ? BrandSources.python()
                    : "archive".equals(kind) ? BrandSources.archive()
                    : BrandSources.release(brand);
            base = effective.url();
        }
        String normalized;
        try {
            normalized = BrandSources.normalize(base);
        } catch (BrandSources.Invalid e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        // Each kind probes something that exists on every mirror of that kind: a package's simple
        // index for PyPI, a tiny repository zip for the codeload shape (which also proves the
        // `<base>/<slug>/zip/refs/tags/<tag>` path is being proxied), and the base itself for the
        // two GitHub-release mirrors whose asset paths are version-specific.
        String url;
        if ("pypi".equals(kind)) {
            url = normalized + "/" + PROBE_PACKAGE + "/";
        } else if ("archive".equals(kind)) {
            url = normalized + "/" + PROBE_REPOSITORY + "/zip/refs/heads/master";
        } else {
            url = normalized + "/";
        }

        long started = System.currentTimeMillis();
        JsonObject answer = new JsonObject();
        answer.addProperty("kind", kind == null ? "" : kind);
        answer.addProperty("url", normalized);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(PROBE_TIMEOUT)
                    .header("Accept", "*/*")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(PROBE_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            try {
                HttpResponse<Void> probe = client.send(request, HttpResponse.BodyHandlers.discarding());
                answer.addProperty("status", probe.statusCode());
                // For PyPI and codeload the probed path exists on a correct mirror, so only a
                // 2xx/3xx counts. The two GitHub-release bases answer 404 at their root (the asset
                // path carries the version); an answer below 500 still proves the host is there.
                boolean strict = "pypi".equals(kind) || "archive".equals(kind);
                answer.addProperty("ok", strict
                        ? probe.statusCode() >= 200 && probe.statusCode() < 400
                        : probe.statusCode() < 500);
            } finally {
                HttpClients.closeQuietly(client);
            }
        } catch (IOException | RuntimeException e) {
            answer.add("status", JsonNull.INSTANCE);
            answer.addProperty("ok", false);
            answer.addProperty("error", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            answer.add("status", JsonNull.INSTANCE);
            answer.addProperty("ok", false);
            answer.addProperty("error", "interrupted");
        }
        answer.addProperty("millis", System.currentTimeMillis() - started);
        Json.writePreservingNulls(response, answer);
    }

    /// Refuses a write that did not come through the developer password.
    private boolean requireElevation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Elevation is only obtained over HTTPS (see DeveloperApiServlet), so a deployment
        // without TLS cannot elevate at all; saying that is clearer than "elevate first".
        if (!RequestSecurity.isSecure(request, tlsActive.getAsBoolean(), config)) {
            Json.error(response, HttpServletResponse.SC_CONFLICT,
                    "tls_required: the developer password is only accepted over HTTPS");
            return false;
        }
        if (!AuthFilter.elevationSatisfied(
                request, elevation, keyOf(request), sessionAlive(request))) {
            Json.error(response, HttpServletResponse.SC_FORBIDDEN,
                    "developer_elevation_required: elevate with the developer password first");
            return false;
        }
        return true;
    }

    private BrandInstanceManager manager(Brand brand) {
        return new BrandInstanceManager(brand,
                config.dataDir.resolve("brands").resolve(brand.id()).resolve("instances"));
    }

    private static String keyOf(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return DeveloperElevation.key(token instanceof String value ? value : null, request.getRemoteAddr());
    }

    private boolean sessionAlive(HttpServletRequest request) {
        Object token = request.getAttribute(AuthFilter.SESSION_TOKEN_ATTRIBUTE);
        return !(token instanceof String value) || value.isBlank() || auth.isAlive(value);
    }

    private static @Nullable String actorOf(HttpServletRequest request) {
        Object session = request.getAttribute(AuthFilter.SESSION_ATTRIBUTE);
        return session instanceof AuthService.Session s ? s.username() : null;
    }

    /// Reads the request body.
    ///
    /// @return the object, or `null` when the answer has already been written
    private static @Nullable JsonObject body(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String text = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (text.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON body is required");
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "a JSON object is required");
                return null;
            }
            return parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "the body is not JSON");
            return null;
        }
    }

    private static @Nullable String stringField(JsonObject body, String name) {
        JsonElement element = body.get(name);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static @Nullable Boolean booleanField(JsonObject body, String name) {
        JsonElement element = body.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean() : null;
    }
}