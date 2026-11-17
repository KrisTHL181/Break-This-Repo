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
import com.google.gson.JsonObject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshRelease;
import org.jackhuang.hmcl.dsh.DshVersionManager;
import org.jackhuang.hmcl.web.task.TaskService;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/// `GET /api/versions` — the npm release list, cached.
///
/// Reading the list costs several `npm view` round-trips against the registry,
/// so the answer is computed on the task pool (never on a request thread) and
/// remembered for five minutes. Concurrent requests during a fetch share the
/// one in-flight computation instead of each starting their own.
@NotNullByDefault
public final class VersionsApiServlet extends HttpServlet {

    /// How long a successful answer is reused.
    private static final long CACHE_MILLIS = 5 * 60 * 1000L;

    /// How long a fetch may take before the request gives up.
    private static final long ASYNC_TIMEOUT_MILLIS = 120 * 1000L;

    private record Cached(List<JsonObject> versions, long fetchedAtMillis) {
    }

    private final TaskService tasks;
    private volatile @Nullable Cached cached;
    private final AtomicBoolean fetching = new AtomicBoolean();
    private final CopyOnWriteArrayList<AsyncContext> waiters = new CopyOnWriteArrayList<>();

    public VersionsApiServlet(TaskService tasks) {
        this.tasks = tasks;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        Cached now = cached;
        if (now != null && System.currentTimeMillis() - now.fetchedAtMillis() < CACHE_MILLIS) {
            write(response, now.versions());
            return;
        }
        AsyncContext async = request.startAsync();
        async.setTimeout(ASYNC_TIMEOUT_MILLIS);
        waiters.add(async);
        if (fetching.compareAndSet(false, true)) {
            tasks.execute(this::fetch);
        }
    }

    /// The one in-flight fetch: computes the list, answers every waiter, and
    /// unlocks the next fetch.
    private void fetch() {
        List<JsonObject> versions = List.of();
        String error = null;
        try {
            versions = compute();
            cached = new Cached(versions, System.currentTimeMillis());
        } catch (Exception e) {
            error = e.getMessage() == null ? e.toString() : e.getMessage();
        }
        for (AsyncContext waiter : waiters) {
            respond(waiter, versions, error);
        }
        waiters.clear();
        fetching.set(false);
    }

    /// Reads the registry and renders the contract's version objects.
    private static List<JsonObject> compute() throws Exception {
        List<DshRelease> releases = DshVersionManager.fetchReleases();
        DshVersionManager.Lockstep lockstep = DshVersionManager.lockstepVersionsAndOldest();
        List<JsonObject> versions = new java.util.ArrayList<>(releases.size());
        for (DshRelease release : releases) {
            JsonObject json = new JsonObject();
            json.addProperty("version", release.version());
            if (release.publishedAt() != null) {
                json.addProperty("time", release.publishedAt());
            }
            json.addProperty("channel", channelOf(release));
            json.addProperty("installable", DshVersionManager.predatesLockstep(release.version(), lockstep.oldest())
                    || lockstep.versions().contains(release.version()));
            versions.add(json);
        }
        return versions;
    }

    /// The channel spelling of the contract: the dist-tag `latest` first, then
    /// the version's own kind. Releases the domain layer classifies as ALPHA
    /// or OTHER (no suffix matches, no tags) are reported as `nightly` — the
    /// contract's list has no `alpha` member and a nightly snapshot is the
    /// closest meaning.
    private static String channelOf(DshRelease release) {
        if (release.isLatest()) {
            return "latest";
        }
        return switch (release.type()) {
            case STABLE -> "stable";
            case RC -> "rc";
            case BETA -> "beta";
            case ALPHA, OTHER -> "nightly";
        };
    }

    private static void respond(AsyncContext async, List<JsonObject> versions, @Nullable String error) {
        HttpServletResponse response = (HttpServletResponse) async.getResponse();
        try {
            if (error != null) {
                Json.error(response, HttpServletResponse.SC_BAD_GATEWAY, "npm 查询失败: " + error);
            } else {
                JsonObject body = new JsonObject();
                JsonArray array = new JsonArray();
                versions.forEach(array::add);
                body.add("versions", array);
                Json.write(response, body);
            }
        } catch (IOException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Failed to answer a version request", e);
        } finally {
            async.complete();
        }
    }

    private static void write(HttpServletResponse response, List<JsonObject> versions) {
        JsonObject body = new JsonObject();
        JsonArray array = new JsonArray();
        versions.forEach(array::add);
        body.add("versions", array);
        try {
            Json.write(response, body);
        } catch (IOException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Failed to write the version list", e);
        }
    }
}
