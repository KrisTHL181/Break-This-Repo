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

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Map;

/// Serves the SPA shell from the classpath `web/` directory (packaged from
/// `src/main/resources/web`, later the React build output).
///
/// The built index.html carries root-absolute asset references, which a
/// subpath-mounted deployment (the panel behind a path-stripping reverse
/// proxy, see `HDSL_BASE_PATH`) cannot serve. The servlet therefore rewrites
/// the shell at serve time: a `<base href>` + `window.__HDSL_BASE__` are
/// injected and the asset URLs are prefixed with the configured base path.
/// The frontend resolves every runtime path (API, WS, router, wallpaper)
/// relatively or through that base, so one build serves both mounts.
///
/// Any path that is not a real file — `/login`, `/instances/3`, a stale
/// hashed asset name after a deploy — falls back to `index.html` so client
/// side routing always gets a page to boot from. The resources themselves are
/// unauthenticated by design: the UI shell carries no data, and the login
/// page must be reachable before a session exists. All data planes live
/// behind [org.jackhuang.hmcl.web.auth.AuthFilter].
@NotNullByDefault
public final class StaticServlet extends HttpServlet {

    private static final String RESOURCE_ROOT = "web/";
    private static final String INDEX = "index.html";

    /// The mount the panel is served under ("" = root, "/panel" = subpath).
    private final String basePath;
    /// The rewritten shell, computed once per servlet (the base never changes
    /// at runtime and the jar's index.html is immutable).
    private volatile @org.jetbrains.annotations.Nullable String rewrittenIndex;

    public StaticServlet() {
        this("");
    }

    public StaticServlet(String basePath) {
        this.basePath = basePath == null || basePath.equals("/") ? "" : basePath;
    }

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("js", "text/javascript; charset=utf-8"),
            Map.entry("mjs", "text/javascript; charset=utf-8"),
            Map.entry("json", "application/json; charset=utf-8"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("png", "image/png"),
            // 壁纸是 .jpg：缺这两个键时它们会以 application/octet-stream 下发。
            // 浏览器会按内容嗅探渲染，所以功能上没坏，但类型是错的。
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("txt", "text/plain; charset=utf-8"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = normalize(requestPath(request));
        URL resource = find(path);
        if (resource == null) {
            resource = find(INDEX); // SPA fallback
            // The content type follows what is actually sent, so client-side
            // routes (e.g. `/login`) still come back as text/html.
            path = INDEX;
        }
        if (resource == null) {
            // This is the build's own defect, not a client mistake: a jar
            // without the packaged frontend answers every page with a bare
            // "not found" and the operator is left guessing. Name it.
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                    "web/index.html is not on the classpath — this build does not package the web frontend; "
                            + "use a release jar (shadowJar) instead of a bare classes/dev build");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("not found");
            return;
        }

        // Text browsers and IE get the no-script surface at the HTTP level, so
        // the fallback does not depend on a `<noscript>` meta refresh being
        // honoured (IE <= 9 does not parse noscript in <head>). The inline gate
        // in index.html still covers every other browser whose JavaScript is on
        // but too old to run the SPA.
        if (INDEX.equals(path)) {
            String fallback = plainFallback(request.getHeader("User-Agent"));
            if (fallback != null) {
                response.setStatus(HttpServletResponse.SC_FOUND);
                response.setHeader("Location", basePath + fallback);
                response.setHeader("Cache-Control", "no-store");
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(contentType(path));
        // 入口 HTML 必须每次重新拿，否则浏览器会一直用它缓存的那份、继续引用旧
        // hash 的 JS，部署了新版本页面却还是旧 UI。带 hash 的 assets 反过来可以长缓存。
        if (INDEX.equals(path)) {
            response.setHeader("Cache-Control", "no-store");
        } else if (path.startsWith("assets/")) {
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        } else {
            response.setHeader("Cache-Control", "no-cache");
        }
        // M7 (glm-5.2): security headers for the panel's own responses.
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        if (INDEX.equals(path)) {
            byte[] body = indexHtml(resource);
            response.setContentLengthLong(body.length);
            response.getOutputStream().write(body);
            return;
        }
        URLConnection connection = resource.openConnection();
        response.setContentLengthLong(connection.getContentLengthLong());
        try (InputStream in = connection.getInputStream()) {
            in.transferTo(response.getOutputStream());
        }
    }

    /// The no-script surface a user agent should be sent to instead of the SPA shell.
    ///
    /// Only the two families that cannot possibly run the bundle are named: text
    /// browsers (go straight to `/plain`) and IE (go to the friendly notice, which
    /// links to `/plain`). Everything else gets the shell and is judged by the
    /// inline capability gate — a header-based guess must not redirect a browser
    /// that could run the modern page.
    ///
    /// @param userAgent the request's `User-Agent`, or `null`
    /// @return the path to redirect to, or `null` to serve the shell
    private static @org.jetbrains.annotations.Nullable String plainFallback(@org.jetbrains.annotations.Nullable String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("w3m") || ua.contains("lynx") || ua.contains("links (")
                || ua.contains("elinks")) {
            return "/plain";
        }
        if (ua.contains("msie") || ua.contains("trident/")) {
            return "/plain/old";
        }
        return null;
    }

    /// The shell with the base-path machinery injected. Everything the SPA
    /// loads — hashed bundles, icons, wallpaper — is referenced root-absolute
    /// in the build output; under a subpath mount those URLs are prefixed
    /// here, and `<base href>` makes the frontend's relative runtime paths
    /// (fetch("api/…"), "assets-img/…") resolve inside the mount as well.
    private byte[] indexHtml(URL resource) throws IOException {
        String cached = rewrittenIndex;
        if (cached == null) {
            try (InputStream in = resource.openStream()) {
                cached = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            String head = "  <base href=\"" + (basePath.isEmpty() ? "/" : basePath + "/") + "\">\n"
                    + "  <script>window.__HDSL_BASE__=" + jsonString(basePath) + ";</script>\n";
            cached = cached.replaceFirst("<head>", "<head>\n" + head);
            if (!basePath.isEmpty()) {
                cached = cached.replace("=\"/assets", "=\"" + basePath + "/assets");
            }
            rewrittenIndex = cached;
        }
        return cached.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /// JSON-string escaping for the injected script literal (config
    /// validation already rejects quotes, this covers backslashes).
    private static String jsonString(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (char c : value.toCharArray()) {
            if (c == '\\' || c == '"') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.append('"').toString();
    }

    /// The request path to resolve against the classpath.
    ///
    /// This servlet is mounted at "/" — the default mapping — and the servlet
    /// spec says a default-mapped servlet receives **no** path info: the whole
    /// path arrives as the servlet path and `getPathInfo()` is null. Reading
    /// only `getPathInfo()` therefore resolved every request to index.html and
    /// served the HTML shell for the JS/CSS bundles too (the panel rendered as
    /// a black page). Fall back through servletPath and the raw URI so the
    /// lookup works under any mapping.
    private static String requestPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null) {
            path = request.getServletPath();
        }
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        return path;
    }

    /// Resolves `path` under the classpath `web/` root, rejecting traversal.
    private static URL find(String path) {
        if (path.isEmpty() || path.equals("/")) {
            path = INDEX;
        }
        return StaticServlet.class.getClassLoader().getResource(RESOURCE_ROOT + path);
    }

    /// Collapses `.`/`..` segments and rejects anything that would escape the
    /// root. `..` that cannot be resolved (more ups than downs) means the
    /// path was malicious.
    private static String normalize(String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return INDEX;
        }
        StringBuilder cleaned = new StringBuilder(pathInfo.length());
        int depth = 0;
        for (String segment : pathInfo.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                depth--;
                if (depth < 0) {
                    return INDEX;
                }
                int last = cleaned.lastIndexOf("/");
                cleaned.setLength(last <= 0 ? 0 : last);
                continue;
            }
            depth++;
            cleaned.append('/').append(segment);
        }
        return cleaned.isEmpty() ? INDEX : cleaned.substring(1);
    }

    private static String contentType(String path) {
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return "application/octet-stream";
        }
        return CONTENT_TYPES.getOrDefault(path.substring(dot + 1).toLowerCase(), "application/octet-stream");
    }
}
