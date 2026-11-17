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
package org.jackhuang.hmcl.web.proxy;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshProcess;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.web.http.Json;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/// The reverse proxy behind `/i/*`: one dsh web instance per mount, reached
/// through the loopback port its readiness line reported.
///
/// The translation follows the reference implementation in the dsh
/// repository (`apps/web/tests/prefix-proxy.ts`), ported to Jetty:
///
/// - `/i/<id>` without a trailing slash is a 301 to `/i/<id>/` (token exchange
///   needs the directory form), query string preserved;
/// - `/i/<id>/<rest>` forwards to `http://127.0.0.1:<port>/<rest>` where the
///   port comes from the readiness line, never from the manifest — a patch
///   layer may have moved the server;
/// - hop-by-hop headers are stripped in both directions; **the Host header
///   passes through untouched**, because dsh's browser-trust fence compares
///   Origin to Host and refuses rewritten requests;
/// - the browser's `Accept-Encoding` is **narrowed** to the encodings the body
///   rewriting can undo ([decodableAcceptEncoding]) — passing it through offered
///   the instance brotli, and a body this servlet cannot decode is a body it
///   cannot rewrite;
/// - `Set-Cookie: ...; Path=/` becomes `Path=/i/<id>/` so the cookies of two
///   instances (which share their names, decided by host alone) stop
///   overwriting each other;
/// - a redirect naming the instance's own address is rewritten onto the mount
///   — the token exchange answers with `303 Location: /`, and the browser that
///   follows it would leave the mount for the panel's own page;
/// - the page itself is rewritten on the way past: its `<base>` and its
///   `/plugins/` registrations are given the mount, and a small shim keeps the
///   requests it builds at runtime inside it (see [#rewritePage]);
/// - a third-party brand client (Kimi Code, OpenCode) gets its own script and
///   stylesheet rewritten as they pass: every root-absolute reference they build
///   at runtime — a lazily imported chunk, an icon, a font — is given the mount
///   it is served under ([#patchBrandScript], [#patchBrandStylesheet]);
/// - a client that routes by `location.pathname` gets one thing more from that
///   same rewrite: the address it reads is made mount-blind, so the address bar
///   can keep the mount and still match the client's routes (see [ROUTER_JS]);
/// - bodies stream in both directions (a `Flow.Publisher` over the servlet
///   input on the way up, `BodyHandlers.ofInputStream` on the way down), so
///   SSE — `/plugins/events` — works without special treatment;
/// - when the client goes away the upstream request is cancelled — the attempt
///   that is really in flight, first or retry (see [Upstream]).
///
/// WebSocket upgrades under `/i/*` are not handled here; Jetty routes them to
/// the [org.jackhuang.hmcl.web.proxy.InstanceProxyWebSocket] endpoint before
/// the servlet ever sees them.
///
/// That reference is a fixture for the dsh repository's own test client, and that
/// client is prefix-aware: it takes the mount as its base, follows the token
/// exchange itself (`redirect: 'manual'`, with a `Location` of `./` meaning "in
/// place"), and resolves every path against that base. The real web client is
/// not — it is built to own the origin — so four things here have no
/// counterpart upstream and are this launcher's own: the `Location` rewrite, the
/// page rewrite, the shim that keeps the requests the page builds at runtime
/// inside the mount, and the router patch that teaches a path-routed client the
/// mount it is served under (see [#rewritePage]). A reader comparing the two should not
/// read their absence upstream as an oversight here.
@NotNullByDefault
public final class InstanceProxyServlet extends HttpServlet {

    static {
        // java.net.http refuses to set Connection/Content-Length/Expect/Host/
        // Upgrade via its builder API unless the property names them. Host
        // passthrough is the one contract dsh itself enforces (Origin must
        // equal Host). `main` sets this before anything else — this line is the
        // fallback for tests and for an embedder that builds the servlet
        // directly, and it only works if no HttpClient exists yet.
        org.jackhuang.hmcl.web.http.HostHeaderSupport.allow();
        // Instance servers are Node and Bun, whose keep-alive timeout is five
        // seconds; the pooled connection this client would otherwise hold for
        // minutes is then a socket the other end has already closed. A request
        // that lands on one of those has no answer coming — measured 2026-10: a
        // `PUT` that saved an API key died that way, inside the JDK's own
        // `Http1RequestBodySubscriber`, and the browser was told nothing at all.
        // Retiring idle connections before the instance does keeps that rare;
        // [service] retries the one that still slips through, for the methods a
        // retry may be sent for at all (see there).
        System.setProperty("jdk.httpclient.keepalive.timeout", "4");
    }

    /// Headers that describe one hop and must not reach the other side.
    public static final Set<String> HOP_BY_HOP = Set.of(
            "connection", "keep-alive", "proxy-connection", "transfer-encoding", "upgrade", "te", "trailer");

    /// The methods a failed attempt may be sent again for: the request says the
    /// same thing twice, so sending it twice cannot apply anything twice (see
    /// [service]).
    private static final Set<String> RETRYABLE_METHODS = Set.of("GET", "HEAD");

    /// Matches the `Path=/` of a Set-Cookie value, at the start or after a
    /// separator, so it can be rewritten to the instance's mount.
    private static final Pattern COOKIE_PATH = Pattern.compile("(^|;\\s*)Path=/(?=;|$)", Pattern.CASE_INSENSITIVE);

    /// The per-mount script the generic page rewrite references: served by this
    /// servlet itself (see [service]) rather than inlined, because some brands
    /// (OpenCode) answer their pages with a Content-Security-Policy whose
    /// `script-src 'self'` refuses every inline script — a same-origin
    /// `<script src>` passes that policy where an inline shim is discarded
    /// before it ever runs. Measured 2026-10: the shim was present in the
    /// served HTML yet `window.fetch` stayed native, and every runtime API
    /// call 404'd at the panel root.
    static final String SHIM_RESOURCE = "__hdsl-shim.js";

    /// The per-mount script a client that routes by `location.pathname` needs
    /// ([Brand#needsRouterShim] — OpenCode): served under `/i/<id>/`, such a
    /// client is asked for a route it cannot match and renders an empty shell.
    /// The resource carries both halves of the answer — keep the mount in the
    /// address ([ROUTER_JS]) or hand it back to the panel root
    /// ([ROUTER_ROOT_JS], named by `?mode=root` when the client's bundle cannot
    /// be patched). Served as its own resource for the same CSP reason as the
    /// runtime shim.
    static final String ROUTER_RESOURCE = "__hdsl-router.js";

    private static final int BUFFER_SIZE = 8192;

    private final HttpClient client;
    private final TaskService tasks;
    /// The panel's own mount (`HDSL_BASE_PATH`): internal routing uses the
    /// bare `/i/<id>/` mount, but every browser-facing surface — injected
    /// base/shim references, patched asset URLs, redirect Locations, cookie
    /// paths — must carry it, or a subpath-mounted panel leaks root paths
    /// that its outer proxy does not serve.
    private final String basePath;

    public InstanceProxyServlet(TaskService tasks) {
        this(tasks, "");
    }

    public InstanceProxyServlet(TaskService tasks, String basePath) {
        this.tasks = tasks;
        this.basePath = basePath == null ? "" : basePath;
        // HTTP/1.1 is forced: the default HTTP/2 makes java.net send an h2c
        // upgrade probe (Upgrade: h2c), and dsh's bare node:http server —
        // which treats every Connection: Upgrade as a WebSocket candidate —
        // answers that by destroying the socket, surfacing here as a 502.
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uri = request.getRequestURI();
        String rest = uri.startsWith("/i/") ? uri.substring("/i/".length()) : "";
        int slash = rest.indexOf('/');
        String instanceId = slash < 0 ? rest : rest.substring(0, slash);
        if (instanceId.isEmpty()) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
            return;
        }

        // `/i/<id>` — the token exchange dsh performs needs the directory
        // form, so a bare mount is a redirect rather than a proxy target.
        if (slash < 0) {
            String query = request.getQueryString();
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", basePath + "/i/" + instanceId + "/" + (query == null ? "" : "?" + query));
            return;
        }

        // The per-mount scripts are served by the panel itself, not by the
        // instance: they have to exist before any page loads (CSP-safe, see
        // [SHIM_RESOURCE]) and never depend on the instance's state. The router
        // shim answers the same way for path-routed clients.
        String restPath = rest.substring(slash);
        if (restPath.equals("/" + SHIM_RESOURCE) || restPath.equals("/" + ROUTER_RESOURCE)) {
            boolean router = restPath.equals("/" + ROUTER_RESOURCE);
            // The router shim has two contracts (see [ROUTER_JS]): keep the mount
            // in the address bar, or — for a client whose bundle carries no
            // patchable expression — hand the address back to the panel root. The
            // page names the one it needs, because only the page knows: it probes
            // the client's bundle before it names this script.
            boolean backToRoot = router && "root".equals(request.getParameter("mode"));
            String template = router ? (backToRoot ? ROUTER_ROOT_JS : ROUTER_JS) : SHIM_JS;
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/javascript; charset=utf-8");
            response.setHeader("Cache-Control", "no-store");
            response.getOutputStream().write(
                    template.replace("__MOUNT__", basePath + "/i/" + instanceId + "/")
                            .replace("__PANEL_ROOT__", basePath + "/")
                            .getBytes(StandardCharsets.UTF_8));
            return;
        }

        DshInstance instance = DshInstanceManager.find(instanceId);
        int port;
        if (instance != null) {
            URI webUrl = DshProcessManager.find(instanceId).flatMap(DshProcess::webUrl).orElse(null);
            if (webUrl == null) {
                Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, "instance not running");
                return;
            }
            port = webUrl.getPort();
            if (port <= 0) {
                port = instance.portOrDefault();
            }
        } else {
            // The experimental ZCode category binds loopback too, so it reaches
            // the browser through this same mount.
            port = InstanceProxyTargets.resolve(instanceId);
            if (port == InstanceProxyTargets.UNKNOWN) {
                Json.error(response, HttpServletResponse.SC_NOT_FOUND, "instance not found");
                return;
            }
            if (port == InstanceProxyTargets.NOT_RUNNING) {
                Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, "instance not running");
                return;
            }
        }

        String targetPath = rest.substring(slash);
        if (request.getQueryString() != null) {
            targetPath += "?" + request.getQueryString();
        }
        URI target = URI.create("http://127.0.0.1:" + port + targetPath);
        String mount = "/i/" + instanceId + "/";
        String browserMount = basePath + mount;

        // The brand decides how much of the request the proxy may touch. A
        // prefix-aware server (Hermes) emits every reference under the mount it
        // was handed, so the proxy gives it the mount and leaves both the Host
        // and the page alone; every other brand keeps the dsh treatment.
        org.jackhuang.hmcl.web.brand.Brand brand =
                org.jackhuang.hmcl.web.brand.BrandRuntime.brandForMount(instanceId);
        boolean prefixClient = brand != null && brand.serverSidePrefix();

        // The input stream must be claimed before the request goes async. A body
        // is what the client announced — by content-length or by chunked framing;
        // `-1` with neither is a body-less request (the common GET), which must
        // not be given a publisher: a request that carries nothing is not this
        // panel's to turn into a chunked one.
        long announcedBody = request.getContentLengthLong();
        boolean hasBody = announcedBody > 0
                || (announcedBody < 0 && request.getHeader("transfer-encoding") != null);
        // A body that fits in memory is read here and handed over as a byte array
        // rather than streamed: that gives the upstream request a content-length,
        // keeps the JDK's own publisher in the picture instead of this servlet's,
        // and leaves the body replayable. Measured 2026-10: the streaming path is
        // where a proxied `PUT` (an API key being saved) died — a pooled
        // connection the instance had already closed took
        // `Http1RequestBodySubscriber` down with it.
        byte[] bufferedBody = hasBody ? readSmallBody(request) : null;
        InputStreamBodyPublisher streamedBody = hasBody && bufferedBody == null
                ? new InputStreamBodyPublisher(request.getInputStream(), announcedBody, tasks)
                : null;

        HttpRequest.Builder builder = HttpRequest.newBuilder(target);
        copyRequestHeaders(request, builder, prefixClient);
        if (prefixClient) {
            // `<basePath>/i/<id>` without the trailing slash: that is the form
            // upstream reads and echoes into its own references.
            builder.header("X-Forwarded-Prefix", browserMount.substring(0, browserMount.length() - 1));
        }
        if (bufferedBody != null) {
            builder.method(request.getMethod(), HttpRequest.BodyPublishers.ofByteArray(bufferedBody));
        } else if (streamedBody != null) {
            builder.method(request.getMethod(), streamedBody);
        } else {
            builder.method(request.getMethod(), HttpRequest.BodyPublishers.noBody());
        }
        HttpRequest upstreamRequest = builder.build();

        AsyncContext async = request.startAsync();
        // SSE streams stay open indefinitely; no timeout, the client's own
        // disconnect is what ends the pump.
        async.setTimeout(0);
        // What a failed attempt is retried for, and what it is not: a request
        // that says the same thing twice. A `POST`, `PUT`, `PATCH` or `DELETE`
        // that fails is answered with its failure instead — the reason those
        // methods are not idempotent is that the instance may have applied the
        // request before the connection died, and nothing here can tell that from
        // an instance that never saw it ("can be sent twice" is about the bytes,
        // and the bytes are not the question). A body that had to be streamed is
        // not replayable at all, so it cannot be retried whatever the method.
        boolean retryable = streamedBody == null && RETRYABLE_METHODS.contains(request.getMethod());
        // The attempt in flight, held on to — with the retry registered in it as
        // it starts — so that a cancellation reaches whichever request really is
        // in flight, and not a stage derived from the first one (see [Upstream]).
        Upstream upstream = attempt(client, upstreamRequest, retryable);
        async.addListener(new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                upstream.cancel();
            }

            @Override
            public void onError(AsyncEvent event) {
                upstream.cancel();
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        });

        boolean octopClient = brand == org.jackhuang.hmcl.web.brand.Brand.OCTOP;
        // Octop is built with a relative base and reads the injected <base>;
        // its chunks must not go through the Kimi/OpenCode script patcher.
        //
        // A prefix-aware brand (Hermes) already emits every reference under the
        // mount the proxy handed it, so the page/script/stylesheet rewriters are
        // exactly what must NOT run: they would prefix the dashboard's own
        // `X-Forwarded-Prefix` output a second time (`/i/<id>/i/<id>/assets/…`
        // for fonts, measured 2026-11). Its bundle gets one targeted edit
        // instead — the build-time assets base — see [patchPrefixScript].
        boolean brandClient = brand != null && !octopClient && !prefixClient && !brand.relativeClient();
        // ZCode 与 Kimi Code 同源（共享「原生认识 /i/<id>/ 挂载」的客户端代码），
        // 它的 bundle 同样需要运行时补丁（见 patchBrandScript 的挂载检测放宽）。
        boolean zcodeClient = org.jackhuang.hmcl.web.zcode.ZcodeRuntime.known(instanceId);
        boolean routerClient = brandClient && brand.needsRouterShim();
        int upstreamPort = port;
        upstream.answer().whenCompleteAsync(
                (answer, error) -> pump(response, async, upstream, mount, browserMount, brandClient, octopClient,
                        zcodeClient, routerClient, prefixClient, upstreamPort, answer, error),
                tasks.executor());
    }

    /// Starts the first attempt at `request`, and — for a request that may be
    /// sent twice — the composition through which its retry answers for it.
    ///
    /// A retry runs on a client that has never spoken to the instance, so it
    /// cannot land on the same stale connection the first attempt died on — see
    /// the keep-alive note in the static block. It is registered with the
    /// attempt holder as it starts, which is what lets a cancellation reach
    /// whichever request is really in flight (see [Upstream]); a client whose
    /// disconnect has already been recorded is not sent one at all.
    ///
    /// @param retryable whether a failed attempt may be answered with a second
    ///                  request — the methods a retry is legal for, and a body
    ///                  that could be replayed (see [service])
    /// @return the attempt, and the stage the pump waits on ([Upstream#answer])
    static Upstream attempt(HttpClient client, HttpRequest request, boolean retryable) {
        Upstream upstream = new Upstream(client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()));
        upstream.answer = retryable
                ? upstream.first().exceptionallyCompose(error -> upstream.isCancelled()
                        // The client went away before this failure was read: the
                        // answer to its disconnect is not another request.
                        ? CompletableFuture.failedFuture(error)
                        : upstream.retrying(freshClient().sendAsync(request,
                                HttpResponse.BodyHandlers.ofInputStream())))
                : upstream.first();
        return upstream;
    }

    /// Copies the response headers and streams the body back, running on the
    /// task pool so the client's I/O thread is never blocked on a write.
    ///
    /// Three bodies are not streamed but rebuilt: a page (the mount has to be in
    /// its references — see [rewritePage]), and — for a third-party brand client —
    /// its own script and stylesheet ([patchBrandScript], [patchBrandStylesheet]).
    /// An asset that turns out to hold more than [MAX_SCRIPT_BYTES] is the one
    /// exception among those three, and it is still sent whole — see [AssetRead].
    /// Everything else streams, which is what keeps `/plugins/events` (SSE) working.
    /// @param mount         the bare `/i/<id>/` — router-bundle cache key and probe
    /// @param browserMount  `mount` under the panel's own base — every surface
    ///                      the browser sees (injected references, patched asset
    ///                      URLs, redirect Locations, cookie paths)
    private static void pump(HttpServletResponse response, AsyncContext async, Upstream upstream, String mount,
                             String browserMount, boolean brandClient, boolean octopClient, boolean zcodeClient,
                             boolean routerClient, boolean prefixClient,
                             int port, @Nullable HttpResponse<InputStream> result, @Nullable Throwable error) {
        try {
            if (error != null || result == null) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Upstream fetch failed for mount " + mount + ": " + error);
                if (!response.isCommitted()) {
                    Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, "instance not running");
                }
                async.complete();
                return;
            }
            response.setStatus(result.statusCode());
            long contentLength = result.headers().firstValueAsLong("content-length").orElse(-1);
            String contentType = result.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
            // A prefix-aware server has already put the mount into its page.
            // Rewriting it again doubles the mount (`/i/<id>/i/<id>/assets/…`)
            // and injects a shim that fights the SPA's own router — this is the
            // one thing `brandClient=false` cannot switch off.
            boolean servesPage = contentType.contains("text/html") && !prefixClient;
            // An asset is worth rebuilding when it is small enough to hold. An
            // announced length only ever lets this say no early — never yes late
            // (see canPatchAsset). The probe that chose the shim read the entry
            // script under the same rule, so what the page was told and what the
            // browser gets agree.
            boolean holds = canPatchAsset(contentLength);
            boolean servesScript = (brandClient || prefixClient || zcodeClient) && !servesPage && holds
                    && contentType.contains("javascript");
            boolean servesStyle = brandClient && !servesPage && holds && contentType.contains("text/css");
            copyResponseHeaders(result, response, browserMount, servesPage || servesScript || servesStyle);
            try (InputStream in = result.body(); OutputStream out = response.getOutputStream()) {
                if (servesPage) {
                    // Buffered and rewritten, because the page dsh generates is rooted at the origin
                    // rather than at whatever path it is reached through — see rewritePage.
                    String html = readPage(in, result);
                    out.write((octopClient
                                    ? rewriteOctopPage(html, browserMount)
                                    : rewritePage(html, browserMount,
                                            routerClient ? routerShimFor(port, mount, html) : RouterShim.NONE,
                                            brandClient))
                            .getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } else if (servesScript || servesStyle) {
                    AssetRead asset = readAsset(in, result);
                    if (asset.rest() == null) {
                        // These bytes are no longer the instance's own and must not be kept
                        // under its address: a cached copy without the patch would route the
                        // client by the mount itself and ask for chunks at the panel root.
                        response.setHeader("Cache-Control", "no-store");
                        // ZCode 只放宽挂载检测正则：它原生按 /i/<id>/ 路由，
                        // 品牌那套路由读法补丁会把它的路由读坏。
                        out.write(servesScript
                                ? (prefixClient ? patchPrefixScript(asset.bytes(), browserMount)
                                        : zcodeClient ? patchZcodeScript(asset.bytes())
                                        : patchBrandScript(asset.bytes(), browserMount))
                                : patchBrandStylesheet(asset.bytes(), browserMount));
                        out.flush();
                    } else {
                        // Larger than this servlet will hold — either something that only
                        // looked like the client's asset, or a client whose bundle grew past
                        // MAX_SCRIPT_BYTES. Unpatched is the lesser evil (a bundle built for
                        // the origin root loses the chunks it imports lazily, and the router
                        // shim falls back to the panel root), but *truncated* is not an
                        // option: what was read is sent as it is, and the rest of the same
                        // decoded stream follows, so the browser gets the whole resource.
                        org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                                "Asset under " + mount + " holds more than " + MAX_SCRIPT_BYTES
                                        + " bytes — sent untouched (unpatched)");
                        out.write(asset.bytes());
                        try (InputStream rest = asset.rest()) {
                            copyThrough(rest, out);
                        }
                    }
                } else {
                    copyThrough(in, out);
                }
            }
        } catch (Exception e) {
            // M15 (glm-5.2): catch Exception, not just IOException — a
            // RuntimeException from the pump must still cancel the upstream.
            // The client went away, or the instance died mid-response. The
            // upstream request is cancelled either way — a half-finished
            // response has no consumer. That is the attempt in flight, so a retry
            // this servlet started is cancelled here too (see [Upstream]).
            upstream.cancel();
        } finally {
            async.complete();
        }
    }

    /// The upstream attempt that is in flight, the stage its answer arrives on,
    /// and the record of its cancellation.
    ///
    /// What cancels has to be the `sendAsync` that is really running, which is
    /// **not** the stage the pump waits on ([Upstream#answer]): for a request
    /// being retried that stage is the future `exceptionallyCompose` produced,
    /// and a derived stage carries the cancelable of the attempt it was derived
    /// *from* — so cancelling it cancels the first attempt's already-dead
    /// exchange and leaves the retry holding the instance's connection open.
    /// Measured 2026-10 in this package's tests: the retry was still in flight at
    /// the instance after the stage had been cancelled. The class doc's "when the
    /// client goes away the upstream request is cancelled" was not true of it.
    ///
    /// The retry is registered here as it starts ([Upstream#retrying]), and
    /// [Upstream#cancel] cancels whichever attempt that is, exactly once.
    ///
    /// Package-private, as is [InstanceProxyServlet#attempt], because that
    /// contract is what the proxy tests pin down.
    static final class Upstream {

        private final AtomicReference<CompletableFuture<HttpResponse<InputStream>>> inFlight;
        /// The stage the pump waits on: the first attempt, or the composition
        /// the retry answers through. Set once, by [InstanceProxyServlet#attempt].
        private CompletableFuture<HttpResponse<InputStream>> answer;
        /// Set *before* the attempt is cancelled, so that a retry whose
        /// composition is already under way sees it and does not start another
        /// request in answer to the client's going away.
        private final AtomicBoolean cancelled = new AtomicBoolean();

        private Upstream(CompletableFuture<HttpResponse<InputStream>> firstAttempt) {
            this.inFlight = new AtomicReference<>(firstAttempt);
        }

        /// The first attempt: what a retryable request's retry is composed onto,
        /// and the whole of what an unretryable one is.
        private CompletableFuture<HttpResponse<InputStream>> first() {
            return inFlight.get();
        }

        /// The stage whose completion is the answer — what the pump is run for.
        CompletableFuture<HttpResponse<InputStream>> answer() {
            return answer;
        }

        private boolean isCancelled() {
            return cancelled.get();
        }

        /// Registers the retry that is now in flight — and cancels it at once
        /// when the client went away while it was being started, which is the
        /// one window a cancel could otherwise miss.
        private CompletableFuture<HttpResponse<InputStream>> retrying(
                CompletableFuture<HttpResponse<InputStream>> attempt) {
            inFlight.set(attempt);
            if (cancelled.get()) {
                attempt.cancel(true);
            }
            return attempt;
        }

        /// Ends the attempt in flight, once: what the async listener and the
        /// pump do when the client is not there any more.
        void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                inFlight.get().cancel(true);
            }
        }
    }

    /// Copies the request headers, dropping hop-by-hop names and the
    /// content-length (the streamed body has none), passing Host through as it
    /// arrived, and narrowing the Accept-Encoding to the encodings the body
    /// rewriting can undo ([decodableAcceptEncoding]).
    private static void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder,
                                           boolean loopbackHost) {
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String lower = name.toLowerCase(Locale.ROOT);
            if (upperIsSkipped(lower, loopbackHost) || HOP_BY_HOP.contains(lower)
                    || lower.equals("content-length") || lower.equals("accept-encoding")) {
                // The encodings are set once, narrowed, after the loop: the value
                // the browser sent is one this servlet may not hand over as it is.
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                builder.header(name, values.nextElement());
            }
        }
        String encodings = decodableAcceptEncoding(request.getHeader("accept-encoding"));
        if (encodings != null) {
            builder.header("accept-encoding", encodings);
        }
    }

    /// Whether a header must not be forwarded.
    ///
    /// `Host` is the one that matters: dsh compares the browser's `Origin`
    /// against it and refuses rewritten requests, so it passes through for every
    /// brand — but a **prefix-aware** server refuses a Host it does not serve
    /// (Hermes answers 400 to anything that is not loopback). There the header is
    /// dropped and the HTTP client derives it from the loopback URI, which is
    /// exactly what that server wants.
    ///
    /// @param lower       the header name, lower-cased
    /// @param loopbackHost whether this request is going to a prefix-aware server
    /// @return whether to leave it out
    static boolean upperIsSkipped(String lower, boolean loopbackHost) {
        return loopbackHost && lower.equals("host");
    }

    /// The encodings the body rewriting can undo, and the only ones it may ask
    /// for: gzip and deflate. Brotli and zstd would each be a dependency this
    /// build does not carry, and [readPage]/[readAsset] have no decoder for them.
    private static final Set<String> DECODABLE_ENCODINGS = Set.of("gzip", "deflate");

    /// Narrows what the browser asked for down to what the rewriting can decode.
    ///
    /// The request side and the rewriting side have to agree, and this is the one
    /// of the two that can be decided before the answer exists: any response may
    /// turn out to be a body that gets rebuilt — the instance answers a path it
    /// does not know with its index page — so there is no point at which "this
    /// body will not be rewritten" is known. Passing the browser's header through
    /// meant an instance could answer brotli, and [readPage] then decoded those
    /// bytes as UTF-8 text: measured 2026-10, the browser was handed the mangled
    /// remains of the page, with the mount missing from it entirely.
    ///
    /// The tokens the browser did list are kept verbatim, `q` values and all; `*`
    /// is dropped rather than kept, because a wildcard is exactly the licence to
    /// answer brotli. A `null` result means the request carries no
    /// `Accept-Encoding` at all, which every server answers with the identity
    /// encoding — the one body shape this servlet reads as it is.
    ///
    /// @param requested the header the browser sent, or `null`
    /// @return the narrowed header value, or `null` when nothing is left of it
    static @Nullable String decodableAcceptEncoding(@Nullable String requested) {
        if (requested == null || requested.isBlank()) {
            return null;
        }
        StringBuilder narrowed = new StringBuilder();
        for (String token : requested.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int parameter = trimmed.indexOf(';');
            String name = parameter < 0 ? trimmed : trimmed.substring(0, parameter).trim();
            if (!DECODABLE_ENCODINGS.contains(name.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (!narrowed.isEmpty()) {
                narrowed.append(", ");
            }
            narrowed.append(trimmed);
        }
        return narrowed.isEmpty() ? null : narrowed.toString();
    }

    /// Copies the response headers, dropping hop-by-hop names, pointing a
    /// redirect back at the mount, and rewriting the Set-Cookie paths onto it.
    ///
    /// The mount is quoted wherever it goes into a replacement string: it is
    /// configuration (`basePath` and the instance id), and a `$` or `\` in it
    /// would otherwise be read as a group reference — `$1` silently taking the
    /// cookie's own separator for the path, `$x` throwing outright.
    ///
    /// @param rewritesBody whether the caller is replacing the body, in which
    ///                     case the headers that described the old one go too
    private static void copyResponseHeaders(HttpResponse<InputStream> result, HttpServletResponse response,
                                            String mount, boolean rewritesBody) {
        for (var entry : result.headers().map().entrySet()) {
            String name = entry.getKey();
            String lower = name.toLowerCase(Locale.ROOT);
            if (HOP_BY_HOP.contains(lower)) {
                continue;
            }
            if (rewritesBody && (lower.equals("content-length") || lower.equals("content-encoding"))) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (lower.equals("set-cookie")) {
                    value = COOKIE_PATH.matcher(value)
                            .replaceAll("$1Path=" + Matcher.quoteReplacement(mount));
                } else if (lower.equals("location")) {
                    value = mountLocation(value, mount);
                }
                response.addHeader(name, value);
            }
        }
    }

    /// Reads a page, decompressing it when the instance sent it compressed.
    ///
    /// The page arrives gzipped and cannot be rewritten while it is, so the answer goes back
    /// uncompressed — which is why the caller drops the `content-encoding` header with it.
    ///
    /// An encoding this servlet has no decoder for is read as text anyway, and
    /// logged on the way ([warnUndecodableEncoding]): the bytes are not the page
    /// then, and silence about it was half of what made the brotli case invisible.
    private static String readPage(InputStream in, HttpResponse<InputStream> result) throws IOException {
        String encoding = result.headers().firstValue("content-encoding").orElse("").toLowerCase(Locale.ROOT);
        if (encoding.contains("gzip")) {
            try (GZIPInputStream gzip = new GZIPInputStream(in)) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        if (encoding.contains("deflate")) {
            try (InflaterInputStream deflate = new InflaterInputStream(in)) {
                return new String(deflate.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        if (!encoding.isEmpty()) {
            warnUndecodableEncoding(encoding, result);
        }
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    /// The other half of the request-side narrowing ([decodableAcceptEncoding]).
    ///
    /// The narrowing is what keeps an instance from being offered an encoding
    /// this servlet cannot undo, but an instance — or a proxy in front of one —
    /// may answer one anyway. The body is then read as text, which is not the page
    /// the browser needs, and it was silent. Naming it here is what makes the next
    /// occurrence visible instead of a report about a mangled page.
    private static void warnUndecodableEncoding(String encoding, HttpResponse<InputStream> result) {
        org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                "Treating a \"" + encoding + "\" body as text: " + result.request().uri()
                        + " — the rewriting cannot decode it, so the browser will get it mangled");
    }

    /// The shim that keeps the page's runtime requests inside the mount.
    ///
    /// What the page asks for at runtime, the panel never sees in the HTML: the client calls
    /// `/api/...`, opens its live channel at `/api/remote.mux` and subscribes to `/plugins/events`
    /// — all as origin-absolute URLs it builds itself. Through a mount they reach the panel, which
    /// answers 404 for names it does not have, and the interface then sits at "Reconnecting…" for
    /// ever. These four interfaces are the ones a page can be made to go through another path at;
    /// they are wrapped so that a same-host URL gets the mount, and anything else is left exactly
    /// as it was. Dynamic `import()` cannot be wrapped, and does not need to be: the page's own
    /// script is rewritten instead ([patchBrandScript]), so the chunks and assets it names by
    /// absolute path are fetched through the mount rather than from the panel's root.
    ///
    /// The constant is the **bare script**, without `<script>` tags: dsh's page gets it inlined
    /// (tags added at the injection site), the generic brand pages get it as the external
    /// [SHIM_RESOURCE] — serving the tagged form as a standalone script would be a syntax error
    /// and the whole shim would silently never run.
    /// A `Request` that needs the mount is rebuilt from its fields rather than passed as the
    /// init of a new one — `new Request(url, someRequest)` looks like the same thing and is not:
    /// measured 2026-10 in Chrome, a `PUT` with a body died on it with
    /// `net::ERR_ALPN_NEGOTIATION_FAILED`, nothing reaching the panel at all (the stream body
    /// that copy carries is what does it, and the content-length goes missing with it). The body
    /// is read from a clone, so the caller's request stays usable if it retries.
    private static final String SHIM_JS = """
            (()=>{
            const mount="__MOUNT__";
            const fix=(value)=>{if(typeof value!=="string"||!value)return value;
            if(value.startsWith("/i/"))return value;
            try{const url=new URL(value,location.href);
            if(url.host!==location.host)return value;
            if(url.pathname.startsWith("/i/"))return value;
            return url.protocol+"//"+location.host+mount+url.pathname.slice(1)+url.search+url.hash
            }catch(e){return value}};
            const f=window.fetch;
            const rewrap=(url,input,body)=>{const init={method:input.method,headers:input.headers,mode:input.mode,credentials:input.credentials,cache:input.cache,redirect:input.redirect,referrer:input.referrer,integrity:input.integrity,keepalive:input.keepalive,signal:input.signal};
            if(body!==undefined)init.body=body;
            return new Request(url,init)};
            window.fetch=function(input,init){
            if(typeof input==="string")return f.call(this,fix(input),init);
            if(input instanceof URL)return f.call(this,fix(input.href),init);
            if(input instanceof Request){const u=fix(input.url);
            if(u===input.url)return f.apply(this,arguments);
            if(input.body==null)return f.call(this,rewrap(u,input),init);
            return input.clone().arrayBuffer().then(b=>f.call(this,rewrap(u,input,b),init),()=>f.call(this,new Request(u,input),init))}
            return f.apply(this,arguments)};
            const open=XMLHttpRequest.prototype.open;
            XMLHttpRequest.prototype.open=function(){const a=[].slice.call(arguments);a[1]=fix(String(a[1]));return open.apply(this,a)};
            const wrap=(C)=>{try{return new Proxy(C,{construct(t,a,n){if(a.length>0)a[0]=fix(String(a[0]));return Reflect.construct(t,a,n)}})}catch(e){return C}};
            window.WebSocket=wrap(window.WebSocket);
            if(window.EventSource)window.EventSource=wrap(window.EventSource)})()
            """;

    /// What a path-routed client's page is given in place of a router that works
    /// at the mount — see [ROUTER_JS] and [ROUTER_ROOT_JS].
    enum RouterShim {
        /// The client needs neither: it routes by the path it is served at, or is
        /// the origin owner itself (Kimi Code, dsh).
        NONE,
        /// The client's router takes its route from `location.pathname`. Its
        /// bundle is patched to read the mount away ([patchBrandScript]) and
        /// [ROUTER_JS] adds the mount back to everything the client writes: the
        /// address bar keeps `/i/<id>/…` and survives a refresh.
        MOUNT,
        /// The same client, but its bundle carries no expression this servlet
        /// knows how to patch: [ROUTER_ROOT_JS] hands the address back to the
        /// panel root. The app works; the address does not survive a refresh.
        ROOT
    }

    /// The largest script this servlet will hold in memory to patch. The bundles
    /// it exists for are single files a little under 3 MB (measured 2026-10 on
    /// both OpenCode builds); anything much larger is trusted to be something
    /// else and is streamed untouched.
    ///
    /// Package-private so the tests can name the boundary the rule is drawn at
    /// ([canPatchAsset]).
    static final long MAX_SCRIPT_BYTES = 16L * 1024 * 1024;

    /// The one expression a path-routed client turns the browser's address into
    /// its own route with — Solid Router's browser-history adapter, minified but
    /// stable: the path normalised to a single leading slash, then the search
    /// string. Measured 2026-10 in OpenCode 1.18.32 and 1.18.35
    /// (`assets/index-*.js`, the file's only `window.location.pathname`; the other
    /// `location.pathname` — bare, no `window.` — belongs to the client's
    /// boot-time `auth_token` cleanup and must stay untouched, or that cleanup
    /// would drop the mount from the address it rewrites).
    private static final String ROUTER_PATH_READ = "window.location.pathname.replace(";

    /// The same expression with the mount taken off it, so that under `/i/<id>/`
    /// the client matches the routes it would match at the origin root. The
    /// global is looked up rather than called outright: the bundle is served
    /// alongside the shim that defines it, but a bundle that ends up without one
    /// must still run rather than die on a `ReferenceError`.
    private static final String ROUTER_PATH_READ_PATCHED = "(window.__hdslUnmount"
            + "?window.__hdslUnmount(window.location.pathname):window.location.pathname)"
            + ".replace(";

    /// What a client that was built to own the origin asks for its own assets by:
    /// every root-absolute asset reference in its bundle is a literal starting
    /// with this (the markdown worker, the provider icons, the sprites). Through
    /// the mount those are requests to the panel's own root, which answers them
    /// with its page — measured 2026-10 on OpenCode, along with the two anchors
    /// below, from a Settings dialog that never finished rendering.
    private static final String ASSET_ROOT_REFERENCE = "\"/assets/";

    /// The other half of that problem: Vite's preload helper resolves the chunk
    /// map against the base the build was made for, baked in as
    /// `const <name> = function (<dep>) { return "/" + <dep> }`. That is what
    /// turns a lazily imported dialog into a request for `/assets/…` at the panel
    /// root — the dialog never loads and the app keeps nothing but its landing
    /// screen. Matched by shape (both identifiers are minified) and rewritten to
    /// the mount, which is where those chunks are served.
    private static final Pattern PRELOAD_BASE = Pattern.compile(
            "=function\\(([A-Za-z_$][A-Za-z0-9_$]*)\\)\\{return\"/\"\\+\\1\\}");

    /// Root-absolute references inside a stylesheet — `url(/assets/Inter.ttf)`,
    /// with or without quotes. Protocol-relative URLs (`url(//host/…)`) are left
    /// alone by the lookahead.
    private static final Pattern STYLE_ROOT_URL = Pattern.compile("(?i)url\\(\\s*(['\"]?)/(?!/)");

    /// The client's entry script, as the page names it — what the shim decision
    /// is probed on ([routerShimFor]). The first `<script src>` naming a `.js`
    /// file is the one a single-page app boots from (measured 2026-10: both
    /// brands' pages name exactly one, OpenCode's `/assets/index-<hash>.js`).
    private static final Pattern SCRIPT_SRC = Pattern.compile("<script[^>]*\\ssrc=\"([^\"]+)\"");

    /// Which shim each mount's bundle was found to take, as `<script src>|<mode>`
    /// — remembered because a page has to name its shim before the browser ever
    /// asks for the bundle, and the answer only changes when the client's own
    /// asset name does (a different build of it). One entry per mount.
    private static final Map<String, String> ROUTER_BUNDLES = new ConcurrentHashMap<>();

    /// The client the probe fetches bundles with — separate from the proxying
    /// client, which belongs to an instance and is built per servlet.
    private static final HttpClient PROBE_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /// Rewrites one of the client's own files so that everything it asks for by
    /// absolute path stays under the mount, and — for a client that routes by
    /// path — so that the address it reads is the one it would read at the origin
    /// root.
    ///
    /// Three edits, each skipped when its anchor is not there (an unrecognised
    /// build is served with the ones that do apply, never with a guess):
    ///
    /// - [ROUTER_PATH_READ]: the route the client matches, made mount-blind;
    /// - [ASSET_ROOT_REFERENCE]: the asset URLs it keeps as literals;
    /// - [PRELOAD_BASE]: the base its preload helper resolves chunk names with.
    ///
    /// Whether the first of them applies is also the question the shim decision is
    /// made on ([hasRouterPathRead], [routerShimFor]). The other two are what make
    /// a lazily imported dialog work at all, and they apply under
    /// [RouterShim#ROOT] just as much — a client whose router could not be patched
    /// still has to be able to load its own chunks.
    static byte[] patchBrandScript(byte[] script, String mount) {
        // Latin-1 maps every byte to one character and back, so whatever is not
        // matched comes out exactly as it went in, and the anchors — all ASCII —
        // still match.
        String text = new String(script, StandardCharsets.ISO_8859_1);
        if (hasRouterPathRead(text)) {
            text = text.replace(ROUTER_PATH_READ, ROUTER_PATH_READ_PATCHED);
        }

        text = text.replace(ASSET_ROOT_REFERENCE, "\"" + mount + "assets/");
        text = PRELOAD_BASE.matcher(text).replaceFirst(match -> "=function(" + match.group(1)
                + "){return\"" + mount + "\"+" + match.group(1) + "}");
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    /// The assets base a **prefix-aware** bundle resolves its lazy chunks and
    /// preload links against, matched by shape because both identifiers are
    /// minified: `=function(<dep>){return"/"+<dep>}` (Vite's classic helper) or
    /// `=function(<dep>){return`/`+<dep>}` (Rolldown/Vite 8, measured 2026-11 on
    /// Hermes 0.22.5). The base is the only reference in such a bundle that is
    /// *not* mount-aware: routes import each other relatively, and the dashboard
    /// prefixes its HTML and CSS from `X-Forwarded-Prefix`, but this one is baked
    /// at build time.
    ///
    /// Left alone, a page under `/i/<id>/` asks the origin root for every lazy
    /// chunk — the panel's own SPA answers those with `index.html`, the browser
    /// refuses a module or stylesheet that arrives as `text/html`, and a failed
    /// CSS preload takes the whole page down (a full-screen background and no
    /// UI). Measured 2026-11 on the Hermes Chat page.
    private static final Pattern PREFIX_ASSETS_BASE = Pattern.compile(
            "=function\\(([A-Za-z_$][A-Za-z0-9_$]*)\\)\\{return([\"`])/\\2\\+\\1\\}");

    /// Rewrites only that assets base to the mount and hands the rest of the
    /// bundle back byte for byte. A build whose helper changed shape is served
    /// untouched rather than guessed at.
    ///
    /// @param script the bundle, as the instance serves it
    /// @param mount  the mount with its trailing slash (`/i/<id>/`)
    /// @return the patched bytes, or {@code script} itself when the anchor is gone
    static byte[] patchPrefixScript(byte[] script, String mount) {
        String text = new String(script, StandardCharsets.ISO_8859_1);
        String patched = PREFIX_ASSETS_BASE.matcher(text)
                .replaceAll("=function($1){return$2" + mount + "$2+$1}");
        return patched.getBytes(StandardCharsets.ISO_8859_1);
    }

    /// ZCode 的唯一补丁：放宽它原生的 /i/<id>/ 挂载检测正则。Kimi Code / ZCode
    /// 的 web 端原生支持被挂在 /i/<id>/ 下（API base 来自
    /// `location.pathname.match(/^\/i\/[^/]+\//)`），但正则锚在路径开头——
    /// 面板自身被反代在子路径时浏览器地址是 /hdsl-web/i/<id>/，检测失配，
    /// 应用把 API/WS 打到源站根（实测 2026-10：/api/server-info 404 → Web 远程
    /// 控制报 Failed；WS 打到 ws://host/ws 被宿主 nginx 404）。
    /// 正则改为从路径开头非贪婪匹配到第一个 /i/<id>/（^.*?）——match() 返回
    /// 匹配子串，只去 ^ 锚会把 /hdsl-web 前缀丢在结果外（实测回归）。
    /// 根挂载下行为不变。不碰路由读法：ZCode 按 /i/<id>/ 原生路由，
    /// 品牌那套 mount-blind 补丁会把它的路由读坏。
    static byte[] patchZcodeScript(byte[] script) {
        String text = new String(script, StandardCharsets.ISO_8859_1);
        text = text.replace("/^\\/i\\/[^/]+\\//", "/^.*?\\/i\\/[^/]+\\//");
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    /// Rewrites a stylesheet's root-absolute references onto the mount — see
    /// [STYLE_ROOT_URL]. CSS is the one place the page rewrite cannot see, and the
    /// fonts a page reaches through it were fetched from the panel root and
    /// silently fell back (measured 2026-10: OpenCode's UI font and its terminal
    /// font).
    static byte[] patchBrandStylesheet(byte[] stylesheet, String mount) {
        String text = new String(stylesheet, StandardCharsets.ISO_8859_1);
        text = STYLE_ROOT_URL.matcher(text).replaceAll("url($1" + mount);
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    /// @return whether the client's router reads the browser address the way this
    ///         servlet knows how to make mount-blind — the question the shim
    ///         decision is made on. Twice is as good as never: which of the two
    ///         the router reads is not knowable, and a wrong guess takes the app
    ///         down with it.
    static boolean hasRouterPathRead(String script) {
        int at = script.indexOf(ROUTER_PATH_READ);
        return at >= 0 && script.indexOf(ROUTER_PATH_READ, at + 1) < 0;
    }

    /// The same question over the bytes the probe read.
    static boolean hasRouterPathRead(byte[] script) {
        return hasRouterPathRead(new String(script, StandardCharsets.ISO_8859_1));
    }

    /// Which router shim the page about to be served must name.
    ///
    /// The client's own bundle decides: if its router's path read can be patched
    /// the address bar keeps the mount ([RouterShim#MOUNT]); if it cannot, the
    /// page falls back to the panel root ([RouterShim#ROOT]) rather than
    /// rendering a shell. The bundle is fetched once per mount and asset name to
    /// answer this, because the shim is named *before* the browser asks for the
    /// bundle and the two still have to agree on the first load. A probe that
    /// fails says no: losing the address bar is recoverable, a page whose shim
    /// does not match its bundle is not.
    private static RouterShim routerShimFor(int port, String mount, String html) {
        String src = entryScriptSrc(html);
        if (src == null) {
            return RouterShim.ROOT;
        }
        String remembered = ROUTER_BUNDLES.get(mount);
        if (remembered != null && remembered.startsWith(src + "|")) {
            return remembered.endsWith("|mount") ? RouterShim.MOUNT : RouterShim.ROOT;
        }
        boolean patchable = probeRouterBundle(port, src);
        ROUTER_BUNDLES.put(mount, src + "|" + (patchable ? "mount" : "root"));
        return patchable ? RouterShim.MOUNT : RouterShim.ROOT;
    }

    /// @return the path of the client's entry script as the instance serves it,
    ///         or `null` when the page names none (or names one on another host,
    ///         which is not the client and cannot be patched through here).
    static @Nullable String entryScriptSrc(String html) {
        Matcher matcher = SCRIPT_SRC.matcher(html);
        while (matcher.find()) {
            String src = matcher.group(1);
            if (!src.endsWith(".js")) {
                continue;
            }
            if (src.startsWith("//") || src.contains("://")) {
                return null;
            }
            return src.startsWith("/") ? src : "/" + src;
        }
        return null;
    }

    /// @return whether the client's entry script carries the path read this
    ///         servlet patches — see [hasRouterPathRead]
    private static boolean probeRouterBundle(int port, String src) {
        try {
            HttpResponse<InputStream> response = PROBE_CLIENT.send(
                    HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + src))
                            .timeout(Duration.ofSeconds(20))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                long length = response.headers().firstValueAsLong("content-length").orElse(-1);
                // A length the instance did not announce is not a reason to give up:
                // OpenCode 1.19.5 answers its assets with `Transfer-Encoding: chunked`
                // and no content-length at all, and making the length a precondition
                // read that client as "carries no path read" — it then got the shim
                // that hands the address back to the panel root. The cap is enforced
                // by the read itself ([readBytes]); a *known* length over it is still
                // worth refusing before the bytes are read.
                if (response.statusCode() != 200 || length > MAX_SCRIPT_BYTES) {
                    return false;
                }
                byte[] bundle = readBytes(in, MAX_SCRIPT_BYTES);
                return bundle != null && hasRouterPathRead(bundle);
            }
        } catch (IOException | InterruptedException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                    "Could not probe the router bundle at " + src + ": " + e);
            return false;
        }
    }

    /// Whether a response is small enough to hold, given what the instance said
    /// about its length.
    ///
    /// An announced length is only ever used to *skip* the work: a body the
    /// instance said is larger than [MAX_SCRIPT_BYTES] is trusted to be
    /// something else, and is streamed untouched rather than read. Knowing
    /// nothing is not the same as knowing it is big — **a response of unknown
    /// length is patched**, and what the read can hold is enforced there
    /// ([readAsset]). This is not hypothetical: OpenCode 1.19.5 answers its own
    /// assets `Transfer-Encoding: chunked` with no content-length, and the old
    /// "sized" rule turned that into "stream it unpatched" — the markdown worker
    /// was then requested at the panel root and the app reported
    /// `Markdown highlighting worker failed`. The bundle itself was the same
    /// 2.7 MB file the sized build served.
    ///
    /// Package-private so the rule is directly testable.
    static boolean canPatchAsset(long contentLength) {
        return contentLength < 0 || contentLength <= MAX_SCRIPT_BYTES;
    }

    /// An asset read ahead of the response: the bytes read, and — when the
    /// stream held more than this servlet will carry — the stream to continue
    /// from, positioned right after the last byte of [bytes].
    ///
    /// `rest == null` means [bytes] is the whole asset and its stream is closed.
    /// Otherwise the caller sends [bytes] and then what is left of `rest`: past
    /// the cap the body is no longer patchable, but it is still the file the
    /// browser asked for, whole. A truncated script is not a smaller script — it
    /// is one that runs nothing at all.
    static final record AssetRead(byte[] bytes, @Nullable InputStream rest) {
    }

    /// Reads a script or stylesheet response into memory, decompressing it when
    /// the instance sent it compressed: the patch is matched against the bytes the
    /// browser would run, and a compressed body is not those bytes. The caller
    /// drops `content-encoding` with it — see [copyResponseHeaders].
    ///
    /// An encoding with no decoder is read as text and logged, exactly as in
    /// [readPage]: the patch is then matched against something that is not the
    /// asset, and the file the browser loads is not the one the instance served.
    ///
    /// @return the bytes, up to [MAX_SCRIPT_BYTES], and the stream to carry on
    ///         with when there are more of them (see [AssetRead])
    static AssetRead readAsset(InputStream in, HttpResponse<InputStream> result) throws IOException {
        String encoding = result.headers().firstValue("content-encoding").orElse("").toLowerCase(Locale.ROOT);
        if (encoding.contains("gzip")) {
            return readUpTo(new GZIPInputStream(in), MAX_SCRIPT_BYTES);
        }
        if (encoding.contains("deflate")) {
            return readUpTo(new InflaterInputStream(in), MAX_SCRIPT_BYTES);
        }
        if (!encoding.isEmpty()) {
            warnUndecodableEncoding(encoding, result);
        }
        return readUpTo(in, MAX_SCRIPT_BYTES);
    }

    /// Reads `in` up to `cap` bytes — and no further, so that a body which turns
    /// out to be too big costs one buffer past the cap rather than the whole
    /// thing.
    ///
    /// The decoder wrappers are deliberately **not** closed on the `rest` path:
    /// what is left has to be read through the same decompression, or the tail
    /// of the body would be the compressed bytes it started as.
    ///
    /// Package-private so the split is directly testable.
    ///
    /// @return the bytes read, and `in` itself when it holds more than that
    static AssetRead readUpTo(InputStream in, long cap) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[BUFFER_SIZE];
        long total = 0;
        while (total <= cap) {
            int read = in.read(chunk);
            if (read < 0) {
                in.close();
                return new AssetRead(buffer.toByteArray(), null);
            }
            buffer.write(chunk, 0, read);
            total += read;
        }
        return new AssetRead(buffer.toByteArray(), in);
    }

    /// Copies the rest of `in` to `out`, flushing as it goes so a body meant to
    /// arrive in pieces (SSE) still does.
    private static void copyThrough(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            out.write(buffer, 0, read);
            out.flush();
        }
    }

    /// @return the bytes of a stream, or `null` once it holds more than `cap`
    private static byte @Nullable [] readBytes(InputStream in, long cap) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[BUFFER_SIZE];
        long total = 0;
        int read;
        while ((read = in.read(chunk)) >= 0) {
            total += read;
            if (total > cap) {
                return null;
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    /// The shim a path-routed client's page gets: **the client reads the mount
    /// away, and everything it writes back gets the mount added**.
    /// ([org.jackhuang.hmcl.web.brand.Brand#needsRouterShim] — OpenCode.)
    ///
    /// Served through the mount, such a client matches no route and renders a
    /// bare shell (measured: the same page at any subpath of its own port does
    /// the same, so this is the client, not the proxy). Its router takes the route
    /// straight from the browser (`location.pathname`), and that read is what
    /// [patchBrandScript] makes mount-blind from the other side — the two halves
    /// are one mechanism and have to agree:
    ///
    /// - `window.__hdslUnmount` is what the patched bundle calls; it lives here
    ///   rather than inside the bundle so that one bundle can serve every mount;
    /// - `history.pushState` / `replaceState` get the mount, and are not prefixed
    ///   twice when the client hands back an address it read itself.
    ///
    /// What that buys over [ROUTER_ROOT_JS]: the address bar keeps `/i/<id>/…`,
    /// a refresh re-enters the app where it was (the client's own server answers
    /// an unknown path with its index page), and the address stays shareable.
    private static final String ROUTER_JS = """
            (()=>{
            const mount="__MOUNT__";
            window.__hdslUnmount=(path)=>path.indexOf(mount)===0?path.slice(mount.length-1):path;
            const fix=(value)=>{if(typeof value!=="string"||!value)return value;
            if(value.indexOf(mount)===0)return value;
            if(value.charAt(0)==="#"||value.charAt(0)==="?")return value;
            try{const url=new URL(value,location.href);
            if(url.host!==location.host)return value;
            if(url.pathname.indexOf(mount)===0)return value;
            return mount+url.pathname.replace(/^\\/+/,"")+url.search+url.hash
            }catch(e){return value}};
            const push=history.pushState,repl=history.replaceState;
            const wrap=(fn)=>function(state,title,url){try{return fn.call(history,state,title,fix(url))}catch(e){}};
            history.pushState=wrap(push);
            history.replaceState=wrap(repl);
            })()
            """;

    /// The shim for a path-routed client whose bundle could not be patched — the
    /// fallback ([RouterShim#ROOT]). The script runs before the app bundle and
    /// makes the browser's address look like the origin root the client expects:
    ///
    /// - the initial `/i/<id>/…` address is replaced with `/` (hash kept), so the
    ///   first route match is the root view;
    /// - `history.pushState` / `replaceState` are swallowed down to `/` as well,
    ///   so later addresses the app writes never re-expose the mount in
    ///   `location.pathname` — the router keeps matching what it was given.
    ///
    /// Everything the app *requests* still goes through the mount, because the
    /// runtime shim ([SHIM_JS]) prefixes same-origin absolute URLs. The trade-off
    /// is the one [ROUTER_JS] exists to remove: the tab's address bar shows the
    /// panel root, and a manual refresh lands on the panel instead of the app.
    /// `__hdslUnmount` is defined here too — a bundle patched by one panel build
    /// and not by the next must not be the difference between an app and a
    /// `ReferenceError`.
    private static final String ROUTER_ROOT_JS = """
            (()=>{
            const mount="__MOUNT__";
            window.__hdslUnmount=(path)=>path.indexOf(mount)===0?path.slice(mount.length-1):path;
            const root=()=>"__PANEL_ROOT__"+location.hash;
            try{if(location.pathname.indexOf(mount)===0)history.replaceState(history.state,"",root())}catch(e){}
            const push=history.pushState,repl=history.replaceState;
            const swallow=(fn)=>function(state,title,url){try{return fn.call(history,state,title,root())}catch(e){}};
            history.pushState=swallow(push);
            history.replaceState=swallow(repl);
            })()
            """;

    /// Points a page the instance generated at the mount it is reached through.
    ///
    /// Two shapes of page arrive here:
    ///
    /// - **dsh's page** (the original case): it opens with `<base href="/">`
    ///   and registers its client plugins as absolute `/plugins/...` URLs
    ///   (fifty-two of them, inside the inline `__DSH_BOOT__`). Handled by the
    ///   exact replacements below, unchanged.
    /// - **Any other single-page app** (the third-party brand categories — Kimi
    ///   Code, OpenCode): no `<base>` tag, but root-absolute asset references
    ///   (`src="/boot.js"`, `href="/assets/…"`). Every `(src|href|action)="/…"`
    ///   reference gets the mount as a prefix, and the runtime shim is injected
    ///   right after `<head>` so the URLs the app builds at runtime
    ///   (`fetch("/api/…")`, WebSocket, EventSource) stay inside the mount too.
    ///   A page that routes by path gets the router shim named by `shim` after
    ///   it, and its own bundle patched on the way past
    ///   ([patchBrandScript]) — one half without the other only gets the app
    ///   as far as a shell.
    ///
    /// A page carrying neither shape is passed through untouched.
    ///
    /// @param html  the page as the instance generated it
    /// @param mount the mount, with its trailing slash
    /// @return the page as the browser has to see it
    static String rewritePage(String html, String mount) {
        return rewritePage(html, mount, RouterShim.NONE);
    }

    /// The same, for a client that routes by `location.pathname`: `shim` says
    /// which half of [ROUTER_JS] its page must name.
    static String rewritePage(String html, String mount, RouterShim shim) {
        return rewritePage(html, mount, shim, false);
    }

    /// The full form, for a third-party brand page: `runtimeShim` asks for the
    /// runtime shim even when the page carries neither of the two shapes above.
    ///
    /// A page whose references are all relative — Vite built with `base: './'`,
    /// as Claude Web's is — needs no reference rewriting, but its runtime still
    /// builds same-host **absolute** URLs from `location`: Claude Web opens
    /// `wss://<host>/ws` for its one and only channel, and without the shim that
    /// address lands on the panel root (no such route — the UI reports
    /// "connection closed"). Such a page gets the runtime shim and nothing
    /// else: relative references already resolve inside the mount.
    ///
    /// @param html  the page as the instance generated it
    /// @param mount the mount, with its trailing slash
    /// @param shim  which router-shim contract the page must name, if any
    /// @param runtimeShim whether this is a brand page that needs the runtime
    ///                    shim even without a shape to key on
    /// @return the page as the browser has to see it
    static String rewritePage(String html, String mount, RouterShim shim, boolean runtimeShim) {
        if (html.contains("<base href=\"/\">")) {
            return html.replace("<base href=\"/\">",
                            "<base href=\"" + mount + "\">"
                                    + "<script>" + SHIM_JS.replace("__MOUNT__", mount) + "</script>")
                    .replace("=\"/plugins/", "=\"" + mount + "plugins/")
                    .replace(":\"/plugins/", ":\"" + mount + "plugins/")
                    // A manifest is fetched without credentials, so through the mount it is a 401 and
                    // nothing else — and an app served under someone else's path could not be installed
                    // from it anyway.
                    .replace("<link rel=\"manifest\" href=\"./manifest.webmanifest\" />", "");
        }
        if (hasRootAbsoluteReferences(html)) {
            // Rewrite the references first, then reference the shim — the
            // injected tag's src is mount-absolute already and must not be
            // rewritten a second time.
            String prefixed = html
                    .replace("src=\"/", "src=\"" + mount)
                    .replace("href=\"/", "href=\"" + mount)
                    .replace("action=\"/", "action=\"" + mount);
            // Same rule as dsh's manifest: the browser fetches a manifest
            // without credentials, so through the mount it is a 401 from the
            // session gate — nothing else — and an app served under someone
            // else's path could not be installed from it anyway. (Measured
            // 2026-10 on OpenCode: two 401s per page load.)
            return injectShim(prefixed, mount, shim)
                    .replaceAll("<link rel=\"manifest\"[^>]*>", "");
        }
        if (runtimeShim) {
            // Same manifest rule: through the mount it is a credential-less
            // fetch answered 401, so it is dropped rather than made noise.
            return injectShim(html, mount, RouterShim.NONE)
                    .replaceAll("<link rel=\"manifest\"[^>]*>", "");
        }
        return html;
    }

    /// Whether the page references anything from the origin root — the signal
    /// that it assumes it owns the origin and needs the generic rewrite.
    /// Octop's page: Vite is built with `base: './'`, so the only thing the
    /// proxy has to add is the mount itself. The `<base>` makes relative asset
    /// references resolve from deep links (`/i/<token>/agents/...`), and the
    /// global is what the patched `publicUrl`/router reads. No runtime shim and
    /// no chunk rewriting: the bundle already builds mount-aware URLs.
    static String rewriteOctopPage(String html, String mount) {
        String escaped = mount.replace("\\", "\\\\").replace("\"", "\\\"");
        String injection = "<base href=\"" + mount + "\">"
                + "<script>window.__OCTOP_BASE__=\"" + escaped + "\";</script>";
        String withoutBase = html.replaceAll("(?is)<base\\s[^>]*>", "");
        int head = indexOfIgnoreCase(withoutBase, "<head>");
        String rewritten;
        if (head >= 0) {
            int after = head + "<head>".length();
            rewritten = withoutBase.substring(0, after) + injection + withoutBase.substring(after);
        } else {
            rewritten = injection + withoutBase;
        }
        // A manifest fetched without credentials is a 401 through the session
        // gate, and an app served under someone else's path cannot be installed
        // from it anyway.
        return rewritten.replaceAll("<link rel=\"manifest\"[^>]*>", "");
    }

    private static boolean hasRootAbsoluteReferences(String html) {
        return html.contains("src=\"/") || html.contains("href=\"/") || html.contains("action=\"/");
    }

    /// Injects a reference to the per-mount shim (see [SHIM_RESOURCE]) as the
    /// first thing inside `<head>` (falling back to prepending it), so it runs
    /// before any module script the page loads. The shim itself is a separate
    /// same-origin resource rather than an inline script: OpenCode's CSP
    /// (`script-src 'self'`) discards inline scripts, and a shim that never
    /// runs is worse than none — the failure is silent.
    ///
    /// The router shim follows it, naming the contract it is wanted under: the
    /// same resource serves both (see [ROUTER_JS] and [ROUTER_ROOT_JS]).
    private static String injectShim(String html, String mount, RouterShim shim) {
        String tag = "<script src=\"" + mount + SHIM_RESOURCE + "\"></script>";
        if (shim != RouterShim.NONE) {
            tag = tag + "<script src=\"" + mount + ROUTER_RESOURCE
                    + (shim == RouterShim.ROOT ? "?mode=root" : "") + "\"></script>";
        }
        int head = indexOfIgnoreCase(html, "<head>");
        if (head >= 0) {
            int after = head + "<head>".length();
            return html.substring(0, after) + tag + html.substring(after);
        }
        return tag + html;
    }

    /// A case-insensitive indexOf for the literal `<head>` opening tag.
    private static int indexOfIgnoreCase(String html, String needle) {
        return html.toLowerCase(Locale.ROOT).indexOf(needle);
    }

    /// Points a redirect back at the mount.
    ///
    /// dsh answers the token exchange with `303 Location: /`, and the browser that follows it
    /// leaves the mount for the panel's own home page — which is what "the button just refreshes"
    /// was. A location naming the instance's own loopback port is rewritten the same way, because
    /// that address is not one the browser can reach.
    ///
    /// @param value the upstream location
    /// @param mount the mount, with its trailing slash
    /// @return the location the browser has to be sent to
    private static String mountLocation(String value, String mount) {
        if (value.startsWith("/")) {
            return mount + value.substring(1);
        }
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host != null && (host.equals("127.0.0.1") || host.equals("localhost") || host.equals("::1"))) {
                String path = uri.getRawPath() == null ? "" : uri.getRawPath();
                return mount + (path.startsWith("/") ? path.substring(1) : path)
                        + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            }
        } catch (IllegalArgumentException e) {
            // Not a location this understands: passed through as it arrived.
        }
        return value;
    }

    /// The largest request body this servlet will hold in memory to hand over as
    /// one buffer. Bigger bodies stream ([InputStreamBodyPublisher]); smaller ones
    /// are the API calls and form posts a client makes, and they are worth
    /// keeping whole — see [service].
    private static final int MAX_BUFFERED_BODY = 1 << 20;

    /// @return a request body small enough to hold whole, or `null` when there is
    ///         none or it has to be streamed — see [service]
    private static byte @Nullable [] readSmallBody(HttpServletRequest request) throws IOException {
        long length = request.getContentLengthLong();
        if (length <= 0 || length > MAX_BUFFERED_BODY) {
            return null;
        }
        return request.getInputStream().readNBytes((int) length);
    }

    /// A client that has never spoken to anyone, so the request it sends cannot
    /// land on a pooled connection the instance has since closed — see the
    /// keep-alive note in the static block and [InstanceProxyServlet#attempt].
    ///
    /// Deliberately not hoisted into a shared `static final`: a shared one's pool
    /// holds sockets the instance has already closed, which is the stale connection
    /// the retry exists to avoid. The cost of a client per retry is a daemon
    /// selector thread that lives until the process exits, and retries are rare.
    private static HttpClient freshClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /// A request body publisher that pumps the servlet input stream into the
    /// upstream request, one chunk at a time, with backpressure from the
    /// client's demand. Only bodies too big to hold take this path.
    private static final class InputStreamBodyPublisher implements HttpRequest.BodyPublisher {
        private final SubmissionPublisher<ByteBuffer> publisher = new SubmissionPublisher<>();
        private final InputStream input;
        private final long length;
        private final TaskService tasks;
        private final AtomicBoolean started = new AtomicBoolean();

        private InputStreamBodyPublisher(InputStream input, long length, TaskService tasks) {
            this.input = input;
            this.length = length;
            this.tasks = tasks;
        }

        @Override
        public long contentLength() {
            return length;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
            // The pump starts only once a subscriber exists: SubmissionPublisher
            // drops items submitted with nobody subscribed, which is what lost
            // the first chunks when the pump ran ahead of sendAsync.
            publisher.subscribe(subscriber);
            start();
        }

        /// Starts the pump thread once; the guard makes the subscribe-then-start
        /// order harmless either way around.
        private void start() {
            if (started.compareAndSet(false, true)) {
                tasks.execute(this::pump);
            }
        }

        private void pump() {
            try (InputStream in = input) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = in.read(buffer)) >= 0) {
                    ByteBuffer chunk = ByteBuffer.allocate(read).put(buffer, 0, read).flip();
                    publisher.submit(chunk);
                }
                publisher.close();
            } catch (IOException e) {
                publisher.closeExceptionally(e);
            }
        }
    }
}
