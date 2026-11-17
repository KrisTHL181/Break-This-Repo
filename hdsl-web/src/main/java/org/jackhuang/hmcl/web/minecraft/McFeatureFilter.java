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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.web.http.Json;
import org.jetbrains.annotations.NotNullByDefault;

import java.io.IOException;

/// The feature gate in front of `/api/mc/*`.
///
/// **Disabled looks like the feature does not exist**: everything answers 404
/// except the handful of routes the enable flow itself needs (status, the EULA
/// text, enable, decline). That is the contract in `docs/minecraft.md` §5.5 —
/// "无页签、404、无监听", and a 403 would leak that there is something to find.
///
/// The lock is the second job: while it runs, creating an instance and starting
/// one are refused with 423, because the whole point of the hour is that no
/// server comes up during it. Reads stay open so the page can explain itself.
@NotNullByDefault
public final class McFeatureFilter implements Filter {

    private final McFeatureState feature;
    private final String basePath;

    /// @param feature  the feature switch
    /// @param basePath the panel's mount (`HDSL_BASE_PATH`), so the allowlist
    ///                 matches whether the panel is at `/` or under a subpath
    public McFeatureFilter(McFeatureState feature, String basePath) {
        this.feature = feature;
        this.basePath = basePath == null ? "" : basePath;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest http) || !(response instanceof HttpServletResponse answer)) {
            chain.doFilter(request, response);
            return;
        }
        String path = stripBase(http.getRequestURI());
        String method = http.getMethod();

        if (!feature.enabled()) {
            if (allowedWhileDisabled(path, method)) {
                chain.doFilter(request, response);
                return;
            }
            Json.error(answer, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }

        if (feature.locked() && blockedWhileLocked(path, method)) {
            long remaining = feature.lockRemainingSeconds();
            answer.setStatus(423);
            answer.setContentType("application/json; charset=utf-8");
            answer.getWriter().write("{\"error\":\"mc_locked: "
                    + "the Minecraft feature is locked for another " + remaining + " second(s)\","
                    + "\"remainingSeconds\":" + remaining + "}");
            return;
        }
        chain.doFilter(request, response);
    }

    private String stripBase(String uri) {
        if (!basePath.isEmpty() && uri.startsWith(basePath)) {
            return uri.substring(basePath.length());
        }
        return uri;
    }

    /// The enable flow's own routes, plus the status read the page needs.
    static boolean allowedWhileDisabled(String path, String method) {
        if ("GET".equals(method) && (path.equals("/api/mc/feature")
                || path.equals("/api/mc/eula") || path.equals("/api/mc/terms"))) {
            return true;
        }
        return "POST".equals(method)
                && (path.equals("/api/mc/feature/enable") || path.equals("/api/mc/feature/decline"));
    }

    /// What the hour refuses: a new server, or an existing one coming up.
    static boolean blockedWhileLocked(String path, String method) {
        if (!"POST".equals(method)) {
            return false;
        }
        if (path.equals("/api/mc/instances")) {
            return true;
        }
        return path.startsWith("/api/mc/instances/") && path.endsWith("/start");
    }
}