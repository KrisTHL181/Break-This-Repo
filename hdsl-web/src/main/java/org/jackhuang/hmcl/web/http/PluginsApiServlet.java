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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshPluginCatalog;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/// The catalogue half of the plugin REST surface, mapped at `/api/plugins/*`:
///
/// - `GET /api/plugins/catalog` — the community catalogue, kept in memory for
///   ten minutes; `?refresh=1` forces a fetch. A failed fetch is a 502: the
///   catalogue lives upstream, and this server is its gateway.
///
/// Instance-level plugin management hangs off `/api/instances/{id}/plugins`,
/// in [InstancesApiServlet].
@NotNullByDefault
public final class PluginsApiServlet extends HttpServlet {

    /// How long a fetched catalogue is served without asking upstream again.
    private static final long CACHE_TTL_MILLIS = java.time.Duration.ofMinutes(10).toMillis();

    private final Object cacheLock = new Object();
    private @Nullable DshPluginCatalog.Catalog cached;
    private long cachedAtMillis;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        if ("/catalog".equals(path)) {
            catalog(request, response);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private void catalog(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean refresh = "1".equals(request.getParameter("refresh"))
                || "true".equals(request.getParameter("refresh"));
        DshPluginCatalog.Catalog catalog;
        synchronized (cacheLock) {
            long now = System.currentTimeMillis();
            if (!refresh && cached != null && now - cachedAtMillis < CACHE_TTL_MILLIS) {
                catalog = cached;
            } else {
                try {
                    catalog = DshPluginCatalog.fetch();
                } catch (DshException e) {
                    Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, e.getMessage());
                    return;
                }
                cached = catalog;
                cachedAtMillis = now;
            }
        }

        JsonArray plugins = new JsonArray();
        for (DshPluginCatalog.Plugin plugin : catalog.plugins()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", plugin.name());
            entry.addProperty("owner", plugin.owner());
            entry.addProperty("url", plugin.url());
            entry.addProperty("category", plugin.category());
            entry.add("description", plugin.description() == null
                    ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(plugin.description()));
            entry.add("descriptionZh", plugin.descriptionZh() == null
                    ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(plugin.descriptionZh()));
            entry.add("npm", plugin.npm() == null
                    ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(plugin.npm()));
            entry.add("version", plugin.version() == null
                    ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(plugin.version()));
            entry.addProperty("stars", plugin.stars());
            entry.addProperty("downloads", plugin.downloads());
            entry.add("tarball", plugin.tarball() == null
                    ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(plugin.tarball()));
            entry.add("added", plugin.added() == null
                    ? JsonNull.INSTANCE : new com.google.gson.JsonPrimitive(plugin.added()));
            plugins.add(entry);
        }
        JsonObject body = new JsonObject();
        body.add("plugins", plugins);
        Json.writePreservingNulls(response, body);
    }
}
