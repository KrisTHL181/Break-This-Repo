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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;

/// Catches whatever a servlet lets escape. An NPE in a handler is a 500 like
/// any other, but Jetty's default error page is HTML and `/api/*` speaks JSON
/// — one unhandled exception used to hand the frontend an HTML page where a
/// JSON body was the contract. Mapped over `/api/*`; a committed response can
/// no longer be rewritten and is rethrown so the container still sees it.
@NotNullByDefault
public final class ApiGuardFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // H2 (glm-5.2): reject oversized request bodies before they reach a handler.
        //
        // The cap exists because the JSON handlers on this mapping read the whole body into
        // memory. Two details matter more than the number:
        //
        // 1. **The cap comes from the path, not from `Content-Type`.** Deciding by content type
        //    meant anyone could label a 4 GB body `multipart/form-data` or `application/zip` and
        //    walk past the cap on an endpoint that still parsed it as JSON. Only the routes that
        //    genuinely stream to a file are exempt, and they are listed by name below.
        // 2. **Unknown length is refused, not trusted.** `Content-Length` is absent on a chunked
        //    request, and `getContentLengthLong()` then answers -1 — the old check compared only
        //    `> 1 MiB`, so a chunked body of any size sailed through. A JSON endpoint has no
        //    reason to accept chunked input; an upload route does, so the rule applies to the
        //    former only.
        if (request instanceof HttpServletRequest req && response instanceof HttpServletResponse http) {
            String path = req.getRequestURI() == null ? "" : req.getRequestURI();
            if (expectsBody(req.getMethod()) && !streamed(path)) {
                long length = req.getContentLengthLong();
                if (length < 0) {
                    Json.error(http, HttpServletResponse.SC_LENGTH_REQUIRED,
                            "content-length required: chunked bodies are not accepted here");
                    return;
                }
                if (length > MAX_JSON_BODY_BYTES) {
                    Json.error(http, 413, "payload too large");
                    return;
                }
            }
        }
        try {
            chain.doFilter(request, response);
        } catch (Throwable t) {
            if (!(response instanceof HttpServletResponse http) || http.isCommitted()) {
                rethrow(t);
                return;
            }
            String path = request instanceof HttpServletRequest req ? req.getRequestURI() : String.valueOf(request);
            Logger.LOG.warning("Uncaught exception at " + path + ": " + t);
            Json.error(http, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "internal error");
        }
    }

    /// 1 MiB：JSON 处理器会把正文整段读进内存，这个上限就是为它设的。
    static final int MAX_JSON_BODY_BYTES = 1048576;

    /// `/api/mc/instances/<id>/files/<kind>` 的正文就是插件/模组 jar。挂载点前缀可有可无，
    /// 所以正则允许前面还有 `/hdsl` 一类；具体大小由 `McMods` 自己的上限兜底。
    private static final java.util.regex.Pattern MC_FILE_UPLOAD =
            java.util.regex.Pattern.compile(".*/api/mc/instances/[^/]+/files/[^/]+");

    /// 这些路由把正文流式写文件（multipart 暂存或原始 zip），不适用 JSON 上限。
    ///
    /// 名单必须按**路径**写：按 `Content-Type` 判断等于让调用方自己决定走不走闸门。
    static boolean streamed(String path) {
        return path.equals("/api/settings/tls")
                || path.equals("/api/wallpapers")
                || path.startsWith("/api/wallpapers/")
                || path.startsWith("/api/packs/")
                || path.startsWith("/api/plugins/")
                || path.startsWith("/api/chat/attachments")
                || path.equals("/api/chat/avatars")
                || path.endsWith("/plugins/install")
                || path.endsWith("/world/import")
                || MC_FILE_UPLOAD.matcher(path).matches();
    }

    /// 只有这三个方法可能带正文；GET/HEAD/DELETE 没有正文，不必要求长度。
    static boolean expectsBody(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method);
    }

    /// Rethrows what [#doFilter] is declared to throw; anything else becomes a
    /// [ServletException] so the container still reports the failure.
    private static void rethrow(Throwable t) throws IOException, ServletException {
        if (t instanceof IOException io) {
            throw io;
        }
        if (t instanceof ServletException servletException) {
            throw servletException;
        }
        if (t instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (t instanceof Error error) {
            throw error;
        }
        throw new ServletException(t);
    }
}
