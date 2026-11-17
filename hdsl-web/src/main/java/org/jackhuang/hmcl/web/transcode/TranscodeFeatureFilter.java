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
package org.jackhuang.hmcl.web.transcode;

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

/// The feature gate in front of `/api/transcode/*`.
///
/// **Disabled looks like the feature does not exist**: everything answers 404
/// except the enable flow itself (feature status, the notice text, enable and
/// decline). That is the same contract as Minecraft's `McFeatureFilter`, and a
/// 403 would leak that there is something to find.
@NotNullByDefault
public final class TranscodeFeatureFilter implements Filter {

    private final TranscodeFeatureState feature;
    private final String basePath;

    /// @param feature  the feature switch
    /// @param basePath the panel's mount (`HDSL_BASE_PATH`), so the allowlist
    ///                 matches whether the panel is at `/` or under a subpath
    public TranscodeFeatureFilter(TranscodeFeatureState feature, String basePath) {
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
        if (feature.enabled() || allowedWhileDisabled(stripBase(http.getRequestURI()), http.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        Json.error(answer, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private String stripBase(String uri) {
        if (!basePath.isEmpty() && uri.startsWith(basePath)) {
            return uri.substring(basePath.length());
        }
        return uri;
    }

    /// The enable flow's own routes. Everything else is 404 while disabled.
    static boolean allowedWhileDisabled(String path, String method) {
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        if ("GET".equals(method)) {
            return path.equals("/api/transcode/feature") || path.equals("/api/transcode/terms");
        }
        return "POST".equals(method)
                && (path.equals("/api/transcode/feature/enable")
                || path.equals("/api/transcode/feature/decline"));
    }
}