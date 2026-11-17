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
import com.google.gson.JsonPrimitive;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshModelCatalog;
import org.jackhuang.hmcl.dsh.DshVendor;
import org.jackhuang.hmcl.setting.LauncherSettings;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The model directory REST surface, mapped at `/api/models/*`:
///
/// - `GET /api/models/providers` — every supplier an account may be made for: the ones this
///   launcher ships (its offered list and the harness's own catalogue) first, then the ones only
///   models.dev knows. `?refresh=1` asks for the directory again; without it a copy fetched within
///   twelve hours is served.
/// - `GET /api/models/providers/{id}` — one supplier with its models, which is what the account
///   page offers instead of an empty text box.
///
/// The list is merged here rather than in the page for one reason: deciding that models.dev's
/// `vercel` is this launcher's `vercel-ai-gateway` needs the id-and-host matching [DshVendor]
/// already does, and doing it twice would eventually be doing it differently.
///
/// A directory that cannot be read at all is a 502 — the directory lives upstream and this server
/// is its gateway — but only when there is no copy on disk to serve instead. A stale list is worth
/// more than an error, so the failure path answers from the last copy and says so in `source`.
@NotNullByDefault
public final class ModelsApiServlet extends HttpServlet {

    /// How long a fetched directory is served without asking upstream again.
    ///
    /// Hours rather than the plugin catalogue's minutes: a new model appears every few weeks, the
    /// document is five megabytes, and the page has a refresh button for the times it is wrong.
    private static final long CACHE_TTL_MILLIS = java.time.Duration.ofHours(12).toMillis();

    /// How long a copy served after a failed fetch is kept before upstream is tried again.
    ///
    /// A minute would mean a request per page load on a network that stays broken; the whole TTL
    /// would mean twelve hours of answering from disk after one bad minute.
    private static final long STALE_TTL_MILLIS = java.time.Duration.ofMinutes(5).toMillis();

    private final Object cacheLock = new Object();
    private @Nullable DshModelCatalog.Catalog cached;
    private long cachedAtMillis;
    private @Nullable String cachedSource;
    /// When a background refresh last started, so a broken network is retried on the stale TTL
    /// rather than once per page load.
    private long lastRefreshAttemptMillis;

    /// A refresh already in flight: the page asks on every open, and a broken network must not
    /// turn that into one fetch per open.
    private final java.util.concurrent.atomic.AtomicBoolean refreshing =
            new java.util.concurrent.atomic.AtomicBoolean();

    /// One background refresher. Daemon, because a panel shutting down must not wait for a
    /// five-megabyte download it no longer needs.
    private static final java.util.concurrent.ExecutorService REFRESH =
            java.util.concurrent.Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "models-catalog-refresh");
                thread.setDaemon(true);
                return thread;
            });

    /// What the merged list looks like with no directory at all: the suppliers this launcher
    /// ships, and nothing from models.dev. Serving it beats a 502 — the account page can still
    /// create a DeepSeek account while the directory is unreachable.
    private static final DshModelCatalog.Catalog BUILTIN_ONLY =
            new DshModelCatalog.Catalog(0L, List.of());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
            return;
        }
        boolean refresh = isRefresh(request);
        if ("/providers".equals(path) || "/providers/".equals(path)) {
            providers(refresh, response);
            return;
        }
        if (path.startsWith("/providers/")) {
            provider(path.substring("/providers/".length()), refresh, response);
            return;
        }
        Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
    }

    private static boolean isRefresh(HttpServletRequest request) {
        return "1".equals(request.getParameter("refresh")) || "true".equals(request.getParameter("refresh"));
    }

    /// Writes the merged list.
    private void providers(boolean refresh, HttpServletResponse response) throws IOException {
        DshModelCatalog.Catalog catalog = catalog(refresh);
        List<Supplier> suppliers = suppliers(catalog);
        JsonArray array = new JsonArray();
        for (Supplier supplier : suppliers) {
            array.add(supplier.toJson());
        }
        JsonObject body = new JsonObject();
        body.addProperty("fetchedAt", catalog.fetchedAtMillis());
        body.addProperty("source", source());
        body.add("providers", array);
        Json.writePreservingNulls(response, body);
    }

    /// Writes one supplier and its models.
    ///
    /// A supplier this launcher knows but the directory does not is not a 404: it is a supplier
    /// with no models to offer, which the page draws as a text box, exactly as it did before there
    /// was a directory at all. An id in neither list *is* a 404, and the ids the launcher's own
    /// catalogue records no protocol for are in neither list by design — they are left out so the
    /// directory's view of them can be listed instead. A page never asks for one: every id it has
    /// came from the list.
    private void provider(String id, boolean refresh, HttpServletResponse response) throws IOException {
        DshModelCatalog.Catalog catalog = catalog(refresh);
        Supplier found = null;
        for (Supplier supplier : suppliers(catalog)) {
            if (supplier.id().equalsIgnoreCase(id)) {
                found = supplier;
                break;
            }
        }
        if (found == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "no supplier called " + id);
            return;
        }
        JsonArray models = new JsonArray();
        DshModelCatalog.Provider matched = found.matched();
        if (matched != null) {
            for (DshModelCatalog.Model model : matched.models()) {
                models.add(modelJson(model));
            }
        }
        JsonObject body = new JsonObject();
        body.add("vendor", found.toJson());
        body.add("models", models);
        Json.writePreservingNulls(response, body);
    }

    /// Returns the directory, from memory, from disk, or from upstream — and never blocks the
    /// account page on a network that is not answering.
    ///
    /// The order is deliberate:
    ///
    /// 1. a copy held in memory that is inside the TTL is the answer;
    /// 2. otherwise the copy on disk is served immediately and a refresh runs in the background
    ///    (a stale list is worth more than a page that waits);
    /// 3. with no copy anywhere, the suppliers this launcher ships are served as
    ///    `source: "builtin"` and the same background refresh runs;
    /// 4. `?refresh=1` is the exception: the operator asked, so it fetches now, and falls back to
    ///    whatever is held when the fetch fails.
    ///
    /// @param refresh whether to ask upstream now instead of in the background
    /// @return the directory to merge from, never `null`
    private DshModelCatalog.Catalog catalog(boolean refresh) {
        synchronized (cacheLock) {
            if (cached == null) {
                DshModelCatalog.Catalog disk = DshModelCatalog.readCached();
                if (disk != null) {
                    cached = disk;
                    cachedAtMillis = disk.fetchedAtMillis();
                    cachedSource = "cache";
                }
            }
            if (!refresh && cached != null
                    && System.currentTimeMillis() - cachedAtMillis < CACHE_TTL_MILLIS) {
                return cached;
            }
            if (!refresh) {
                DshModelCatalog.Catalog held = cached;
                refreshInBackground();
                if (held != null) {
                    return held;
                }
                cachedSource = "builtin";
                return BUILTIN_ONLY;
            }
        }
        return refreshNow();
    }

    /// Fetches once, updating the cache; on failure answers what is held, or the built-in list.
    private DshModelCatalog.Catalog refreshNow() {
        try {
            DshModelCatalog.Catalog fetched = DshModelCatalog.fetch();
            synchronized (cacheLock) {
                cached = fetched;
                cachedAtMillis = System.currentTimeMillis();
                cachedSource = "remote";
            }
            return fetched;
        } catch (DshException e) {
            DshModelCatalog.Catalog kept;
            synchronized (cacheLock) {
                kept = cached;
            }
            if (kept == null) {
                kept = DshModelCatalog.readCached();
            }
            if (kept != null) {
                LOG.info("Serving the kept model catalogue: " + e.getMessage());
                synchronized (cacheLock) {
                    cached = kept;
                    // Aged to the point of being retried soon, not to the point of being trusted
                    // for another twelve hours: the fetch failed this time and may work next time.
                    cachedAtMillis = System.currentTimeMillis() - CACHE_TTL_MILLIS + STALE_TTL_MILLIS;
                    cachedSource = "cache";
                }
                return kept;
            }
            LOG.info("Serving the built-in supplier list; the model catalogue is unreachable: "
                    + e.getMessage());
            synchronized (cacheLock) {
                cachedSource = "builtin";
            }
            return BUILTIN_ONLY;
        }
    }

    /// Starts a refresh unless one is already running or one was tried within the stale window.
    /// The request that triggers it does not wait: the point is to keep the page responsive on a
    /// broken network, not to turn every page load into another five-megabyte attempt.
    private void refreshInBackground() {
        synchronized (cacheLock) {
            long now = System.currentTimeMillis();
            if (lastRefreshAttemptMillis != 0 && now - lastRefreshAttemptMillis < STALE_TTL_MILLIS) {
                return;
            }
            lastRefreshAttemptMillis = now;
        }
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        REFRESH.submit(() -> {
            try {
                refreshNow();
            } finally {
                refreshing.set(false);
            }
        });
    }

    private String source() {
        synchronized (cacheLock) {
            return cachedSource == null ? "cache" : cachedSource;
        }
    }

    /// Builds the merged list.
    ///
    /// @param catalog the directory
    /// @return the suppliers, the ones this launcher ships first
    private static List<Supplier> suppliers(DshModelCatalog.Catalog catalog) {
        LauncherSettings settings = SettingsManager.settings();
        List<DshVendor> known = knownVendors(settings);
        Set<String> claimed = new LinkedHashSet<>();
        List<Supplier> entries = new ArrayList<>();

        for (DshVendor vendor : known) {
            String protocol = DshVendor.APIS.contains(vendor.api()) ? vendor.api() : null;
            if (protocol == null) {
                // A supplier the harness's own catalogue records no protocol for is one the
                // launcher cannot write a route for, and the directory is the only thing that can
                // say otherwise. Leaving it out lets the directory's own view of it be listed —
                // routable or not — instead of shadowing that view with a card nobody can pick.
                continue;
            }
            boolean custom = isCustom(settings, vendor);
            DshModelCatalog.Provider matched = DshModelCatalog.match(catalog, vendor.id(), vendor.baseUrl());
            if (matched != null) {
                claimed.add(matched.id().toLowerCase(Locale.ROOT));
            }
            entries.add(new Supplier(vendor.id(), vendor.displayName(), vendor.baseUrl(), vendor.apiKeyEnv(),
                    protocol, "dsh", matched == null ? null : matched.npm(),
                    true, custom, vendor.preferred(), matched == null ? null : matched.doc(),
                    matched == null ? null : matched.id(), matched));
        }

        for (DshModelCatalog.Provider provider : catalog.providers()) {
            if (claimed.contains(provider.id().toLowerCase(Locale.ROOT))) {
                continue;
            }
            String protocol = DshModelCatalog.protocolOf(provider.npm());
            entries.add(new Supplier(provider.id(), provider.name(), provider.endpoint(),
                    DshVendor.keyVariableOf(provider.id()), protocol,
                    protocol == null ? null : "directory", provider.npm(), false, false, false,
                    provider.doc(), provider.id(), provider));
        }

        entries.sort(Comparator.comparingInt(Supplier::rank)
                .thenComparing(supplier -> supplier.name().toLowerCase(Locale.ROOT)));
        return entries;
    }

    /// The suppliers this launcher ships, its own list first and then the harness's catalogue.
    ///
    /// The offered list wins where the two disagree, and they do disagree: the catalogue records
    /// no protocol at all for the ones the harness cannot route generically, while the offered list
    /// states the one this launcher writes into a route.
    ///
    /// @param settings the launcher's settings
    /// @return the suppliers
    private static List<DshVendor> knownVendors(LauncherSettings settings) {
        List<DshVendor> known = new ArrayList<>(DshVendor.offered());
        Set<String> ids = new LinkedHashSet<>();
        for (DshVendor vendor : known) {
            ids.add(vendor.id().toLowerCase(Locale.ROOT));
        }
        for (DshVendor vendor : DshVendor.catalogue()) {
            if (ids.add(vendor.id().toLowerCase(Locale.ROOT))) {
                known.add(vendor);
            }
        }
        for (DshVendor vendor : settings.getCustomVendors()) {
            if (ids.add(vendor.id().toLowerCase(Locale.ROOT))) {
                known.add(vendor);
            }
        }
        return known;
    }

    private static boolean isCustom(LauncherSettings settings, DshVendor vendor) {
        for (DshVendor custom : settings.getCustomVendors()) {
            if (custom.id().equalsIgnoreCase(vendor.id())) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject modelJson(DshModelCatalog.Model model) {
        JsonArray efforts = new JsonArray();
        for (String effort : model.reasoningEfforts()) {
            efforts.add(effort);
        }
        JsonObject json = new JsonObject();
        json.addProperty("id", model.id());
        json.addProperty("name", model.name());
        json.add("context", model.context() == null ? JsonNull.INSTANCE : new JsonPrimitive(model.context()));
        json.add("output", model.output() == null ? JsonNull.INSTANCE : new JsonPrimitive(model.output()));
        json.addProperty("reasoning", model.reasoning());
        json.add("reasoningEfforts", efforts);
        json.addProperty("toolCall", model.toolCall());
        json.addProperty("attachment", model.attachment());
        json.add("costInput", model.costInput() == null ? JsonNull.INSTANCE : new JsonPrimitive(model.costInput()));
        json.add("costOutput", model.costOutput() == null ? JsonNull.INSTANCE : new JsonPrimitive(model.costOutput()));
        json.add("status", model.status() == null ? JsonNull.INSTANCE : new JsonPrimitive(model.status()));
        return json;
    }

    /// One line of the merged list, before it is turned into JSON.
    ///
    /// `rank` is what decides the order the page sees: the harness's own supplier, then the ones
    /// this launcher has on record, then the ones only the directory knows.
    private record Supplier(
            String id,
            String name,
            @Nullable String endpoint,
            String envVar,
            @Nullable String protocol,
            @Nullable String protocolSource,
            @Nullable String npm,
            boolean known,
            boolean custom,
            boolean preferred,
            @Nullable String doc,
            @Nullable String catalogId,
            @Nullable DshModelCatalog.Provider matched) {

        int rank() {
            return preferred ? 0 : known ? 1 : 2;
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("id", id);
            json.addProperty("name", name);
            addText(json, "endpoint", endpoint);
            json.addProperty("envVar", envVar);
            addText(json, "protocol", protocol);
            addText(json, "protocolSource", protocolSource);
            addText(json, "npm", npm);
            addText(json, "doc", doc);
            addText(json, "catalogId", catalogId);
            json.addProperty("modelCount", matched == null ? 0 : matched.models().size());
            json.addProperty("known", known);
            json.addProperty("custom", custom);
            json.addProperty("preferred", preferred);
            return json;
        }

        private static void addText(JsonObject json, String name, @Nullable String value) {
            json.add(name, value == null ? JsonNull.INSTANCE : new JsonPrimitive(value));
        }
    }
}
