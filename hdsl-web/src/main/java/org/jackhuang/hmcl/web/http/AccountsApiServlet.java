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
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshInstanceSettings;
import org.jackhuang.hmcl.dsh.DshVendor;
import org.jackhuang.hmcl.setting.LauncherSettings;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.event.EventBus;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/// The account REST surface, mapped at `/api/accounts/*`:
///
/// - `GET    /api/accounts`              — every account, keys masked
/// - `POST   /api/accounts`              — create `{vendor|endpoint?, apiKey, label?, model?, kind?, protocol?}`;
///   the key is checked against the vendor before the answer, and a key that
///   does not pass is still saved (`verified:false` + `verifyError`). `protocol`
///   is the wire protocol for a supplier this launcher does not ship with (see
///   `GET /api/models/providers`), and is ignored for the ones it does.
/// - `GET    /api/accounts/{name}/verify` — ask the vendor live: `{ok, models}` or `{ok:false, error}`
/// - `PATCH  /api/accounts/{name}`       — edit `{label?, model?, apiKey?}`
/// - `DELETE /api/accounts/{name}`       — remove, clearing the instance settings that name it
///
/// `name` is the account's key — `vendorId` or `vendorId|label`, exactly as
/// the `name` field reports it; a `|` travels percent-encoded. **The API never
/// returns an apiKey**: the list and the create/edit answers carry `maskedKey`
/// (first 8 characters + `…`, `null` for an empty key) and nothing else about
/// the key. Every mutation broadcasts `{"type":"accounts-changed"}` on the
/// `accounts` topic.
@NotNullByDefault
public final class AccountsApiServlet extends HttpServlet {

    private final EventBus bus;

    public AccountsApiServlet(EventBus bus) {
        this.bus = bus;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] segments = split(request.getPathInfo());
        try {
            if (segments.length == 0) {
                switch (request.getMethod()) {
                    case "GET" -> listAccounts(response);
                    case "POST" -> createAccount(request, response);
                    default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            String name = segments[0];
            if (segments.length == 1) {
                switch (request.getMethod()) {
                    case "PATCH" -> updateAccount(request, response, name);
                    case "DELETE" -> deleteAccount(response, name);
                    default -> Json.error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "method not allowed");
                }
                return;
            }
            if (segments.length == 2 && "verify".equals(segments[1]) && "GET".equals(request.getMethod())) {
                verifyAccount(response, name);
                return;
            }
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "not found");
        } catch (DshException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // ------------------------------------------------------------------- list --

    private void listAccounts(HttpServletResponse response) throws IOException {
        JsonArray accounts = new JsonArray();
        for (DshAccount account : settings().getAccounts()) {
            accounts.add(accountJson(account));
        }
        JsonObject body = new JsonObject();
        body.add("accounts", accounts);
        Json.writePreservingNulls(response, body);
    }

    // ----------------------------------------------------------------- create --

    private void createAccount(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        String kindParam = stringField(body, "kind");
        String vendor = stringField(body, "vendor");
        String endpoint = stringField(body, "endpoint");
        String apiKey = stringField(body, "apiKey");
        String label = stringField(body, "label");
        String model = stringField(body, "model");
        String protocol = stringField(body, "protocol");

        if (label != null && label.contains("/")) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "a label must not contain `/` (the account's name appears in URLs)");
            return;
        }

        if (protocol != null && !protocol.isBlank() && !DshVendor.APIS.contains(protocol.trim())) {
            // Refused rather than dropped: the value decides which wire the route is
            // written with, and a route written with the wrong one fails at launch
            // rather than here, where it can still be said.
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "protocol must be one of " + String.join(", ", DshVendor.APIS)
                            + " (got \"" + protocol.trim() + "\")");
            return;
        }

        DshAccount.AccountKind kind;
        if (kindParam == null || kindParam.isBlank()) {
            kind = null;
        } else {
            kind = switch (kindParam.trim().toLowerCase(Locale.ROOT)) {
                case "official" -> DshAccount.AccountKind.OFFICIAL;
                case "third-party", "thirdparty", "third_party" -> DshAccount.AccountKind.THIRD_PARTY;
                case "offline" -> DshAccount.AccountKind.OFFLINE;
                default -> {
                    // Name what arrived: the vendors API reports *protocols* in its
                    // `kinds` (openai-completions and friends), and a client that
                    // confuses the two sends one here.
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                            "kind must be one of official, third-party, offline (got \""
                                    + kindParam.trim() + "\")");
                    yield null;
                }
            };
            if (kind == null) {
                return;
            }
        }

        DshAccount account;
        if (kind == DshAccount.AccountKind.OFFLINE) {
            account = DshAccount.offline(label == null || label.isBlank() ? "offline" : label.trim());
        } else {
            if (apiKey == null || apiKey.isBlank()) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "apiKey is required");
                return;
            }
            String vendorId = resolveVendor(vendor, endpoint, protocol, response);
            if (vendorId == null) {
                return; // the answer was written
            }
            if (kind == null) {
                kind = vendorId.equalsIgnoreCase(DshVendor.offered().get(0).id())
                        ? DshAccount.AccountKind.OFFICIAL : DshAccount.AccountKind.THIRD_PARTY;
            }
            account = new DshAccount(kind, vendorId, apiKey.trim(),
                    endpoint == null || endpoint.isBlank() ? null : endpoint.trim(),
                    label == null || label.isBlank() ? null : label.trim(),
                    model == null || model.isBlank() ? null : model.trim(), null);
        }

        for (DshAccount existing : settings().getAccounts()) {
            if (existing.key().equals(account.key())) {
                Json.error(response, HttpServletResponse.SC_CONFLICT,
                        "an account named " + account.key() + " already exists");
                return;
            }
        }

        // The key is checked against the vendor before the answer; a key that
        // does not pass is still saved — the check's failure may be this
        // machine's network rather than the key.
        boolean verified = true;
        String verifyError = null;
        if (account.carriesAKey()) {
            DshAccount.Check check = account.check();
            verified = check.outcome() == DshAccount.Outcome.VALID;
            if (!verified) {
                verifyError = check.message();
            }
        }

        settings().getAccounts().add(account);
        SettingsManager.save();
        announceChanged();

        JsonObject result = accountJson(account);
        result.addProperty("verified", verified);
        if (verifyError != null) {
            result.addProperty("verifyError", verifyError);
        }
        JsonObject body1 = new JsonObject();
        body1.add("account", result);
        Json.writePreservingNulls(response, HttpServletResponse.SC_CREATED, body1);
    }

    /// Resolves the vendor id a new account is for, adding a custom vendor when
    /// the account names an address this launcher has not heard of. Writes the
    /// error answer itself and returns `null` then.
    ///
    /// @param protocol the wire protocol the caller worked out for a supplier this
    ///                 launcher does not ship with, or `null` to let it be derived.
    ///                 Ignored for the suppliers it does ship with, whose protocol
    ///                 is the one this launcher writes into every route for them.
    private @Nullable String resolveVendor(@Nullable String vendor, @Nullable String endpoint,
                                           @Nullable String protocol, HttpServletResponse response)
            throws IOException {
        if (vendor != null && !vendor.isBlank()) {
            String trimmed = vendor.trim();
            DshVendor known = DshVendor.byId(trimmed);
            if (known != null) {
                return known.id();
            }
            for (DshVendor custom : settings().getCustomVendors()) {
                if (custom.id().equalsIgnoreCase(trimmed)) {
                    return custom.id();
                }
            }
            if (endpoint == null || endpoint.isBlank()) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "unknown vendor `" + trimmed + "`; pass `endpoint` to add a supplier by address");
                return null;
            }
            if (!DshVendor.isUsableId(trimmed)) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                        "vendor id `" + trimmed + "` cannot be a route name");
                return null;
            }
            return ensureCustomVendor(trimmed.toLowerCase(Locale.ROOT), trimmed, endpoint.trim(),
                    protocol).id();
        }
        if (endpoint == null || endpoint.isBlank()) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "vendor or endpoint is required");
            return null;
        }
        String trimmed = endpoint.trim();
        DshVendor byAddress = DshVendor.byBaseUrl(trimmed);
        if (byAddress != null) {
            return byAddress.id();
        }
        String host = DshVendor.hostOf(trimmed);
        if (host == null) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "endpoint `" + trimmed + "` is not a usable address");
            return null;
        }
        String id = host.replace('.', '-');
        if (!DshVendor.isUsableId(id)) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                    "no vendor id can be derived from `" + host + "`; pass `vendor` explicitly");
            return null;
        }
        return ensureCustomVendor(id, host, trimmed, protocol).id();
    }

    /// Records a supplier the launcher did not ship with, or returns the one
    /// already recorded under the id.
    ///
    /// A supplier already on record is returned as it is: its protocol is written
    /// into the routes of every account using it, so re-deriving it from whatever a
    /// later request passed would rewrite those routes without being asked to.
    private DshVendor ensureCustomVendor(String id, String name, String baseUrl,
                                         @Nullable String protocol) {
        for (DshVendor custom : settings().getCustomVendors()) {
            if (custom.id().equalsIgnoreCase(id)) {
                return custom;
            }
        }
        DshVendor discovered = DshVendor.discovered(id, name, baseUrl, protocol);
        settings().getCustomVendors().add(discovered);
        return discovered;
    }

    // ----------------------------------------------------------------- verify --

    private void verifyAccount(HttpServletResponse response, String name) throws IOException {
        DshAccount account = find(name);
        if (account == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "account not found");
            return;
        }
        if (!account.carriesAKey()) {
            // An offline account is a name and a face; there is nothing to check.
            JsonObject body = new JsonObject();
            body.addProperty("ok", true);
            body.add("models", new JsonArray());
            Json.write(response, body);
            return;
        }
        // Asked of the vendor on every call, never cached: the answer is only
        // worth anything while it is fresh.
        DshAccount.Check check = account.check();
        JsonObject body = new JsonObject();
        if (check.outcome() == DshAccount.Outcome.VALID) {
            body.addProperty("ok", true);
            // 列表和“为什么没有列表”一起给：验证通过但一个模型都没有，正是
            // “key 能通、端点却列不出模型”的样子，界面要能把这句原话摆出来。
            DshAccount.ModelsAnswer answer = account.fetchModelsAnswer();
            JsonArray models = new JsonArray();
            for (String model : answer.models()) {
                models.add(model);
            }
            body.add("models", models);
            if (answer.empty() && answer.failure() != null) {
                body.addProperty("note", answer.failure());
            }
        } else {
            body.addProperty("ok", false);
            body.addProperty("error", check.message());
        }
        Json.write(response, body);
    }

    // ----------------------------------------------------------------- update --

    private void updateAccount(HttpServletRequest request, HttpServletResponse response, String name)
            throws IOException, DshException {
        JsonObject body = body(request, response);
        if (body == null) {
            return;
        }
        DshAccount account = find(name);
        if (account == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "account not found");
            return;
        }

        String label = account.label();
        if (body.has("label")) {
            JsonElement element = body.get("label");
            if (element.isJsonNull()) {
                label = null;
            } else if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (value.contains("/")) {
                    Json.error(response, HttpServletResponse.SC_BAD_REQUEST,
                            "a label must not contain `/` (the account's name appears in URLs)");
                    return;
                }
                label = value.isBlank() ? null : value.trim();
            }
        }
        String model = account.model();
        if (body.has("model")) {
            JsonElement element = body.get("model");
            if (element.isJsonNull()) {
                model = null;
            } else if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                model = value.isBlank() ? null : value.trim();
            }
        }
        String apiKey = account.apiKey();
        if (body.has("apiKey")) {
            JsonElement element = body.get("apiKey");
            if (element.isJsonNull() || !element.isJsonPrimitive() || element.getAsString().isBlank()) {
                Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "apiKey must be a non-empty string");
                return;
            }
            apiKey = element.getAsString().trim();
        }

        DshAccount updated = new DshAccount(account.kind(), account.vendorId(), apiKey,
                account.baseUrl(), label, model, account.skin());
        if (!updated.key().equals(account.key())) {
            for (DshAccount existing : settings().getAccounts()) {
                if (existing != account && existing.key().equals(updated.key())) {
                    Json.error(response, HttpServletResponse.SC_CONFLICT,
                            "an account named " + updated.key() + " already exists");
                    return;
                }
            }
        }

        List<DshAccount> accounts = settings().getAccounts();
        int index = accounts.indexOf(account);
        if (index < 0) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "account not found");
            return;
        }
        accounts.set(index, updated);

        // A rename re-keys the account; every reference follows it, or the
        // instance that named the old key would launch with no account.
        if (!updated.key().equals(account.key())) {
            rekeyReferences(account.key(), updated.key());
        }
        SettingsManager.save();
        announceChanged();
        JsonObject body1 = new JsonObject();
        body1.add("account", accountJson(updated));
        Json.writePreservingNulls(response, body1);
    }

    // ----------------------------------------------------------------- delete --

    private void deleteAccount(HttpServletResponse response, String name) throws IOException, DshException {
        DshAccount account = find(name);
        if (account == null) {
            Json.error(response, HttpServletResponse.SC_NOT_FOUND, "account not found");
            return;
        }
        settings().getAccounts().remove(account);

        // Everything that named the account forgets it: the per-instance
        // choices and the launcher-wide one. An instance left pointing at a
        // gone account would launch with no key and only a log line to say why.
        String key = account.key();
        for (DshInstance instance : DshInstanceManager.list()) {
            if (key.equals(DshInstanceSettings.accountKey(instance))) {
                DshInstanceSettings.setAccountKey(instance, null);
            }
        }
        if (key.equals(settings().activeAccountKey())) {
            settings().setActiveAccountKey("");
        }
        SettingsManager.save();
        announceChanged();
        Json.write(response, java.util.Map.of("ok", true));
    }

    // ----------------------------------------------------------------- helpers --

    /// Points every reference to `oldKey` at `newKey`.
    private void rekeyReferences(String oldKey, String newKey) throws DshException {
        for (DshInstance instance : DshInstanceManager.list()) {
            if (oldKey.equals(DshInstanceSettings.accountKey(instance))) {
                DshInstanceSettings.setAccountKey(instance, newKey);
            }
        }
        if (oldKey.equals(settings().activeAccountKey())) {
            settings().setActiveAccountKey(newKey);
        }
    }

    private static @Nullable DshAccount find(String name) {
        for (DshAccount account : SettingsManager.settings().getAccounts()) {
            if (account.matchesKey(name)) {
                return account;
            }
        }
        return null;
    }

    private void announceChanged() {
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "accounts-changed");
        bus.publish("accounts", payload);
    }

    /// The account as the contract reports it. The key is **masked**: nothing
    /// here ever carries the apiKey itself.
    static JsonObject accountJson(DshAccount account) {
        JsonObject json = new JsonObject();
        json.addProperty("name", account.key());
        json.addProperty("vendor", account.vendorId());
        json.addProperty("kind", kindName(account.kind()));
        json.add("label", account.label() == null ? JsonNull.INSTANCE : new JsonPrimitive(account.label()));
        json.add("model", account.model() == null ? JsonNull.INSTANCE : new JsonPrimitive(account.model()));
        json.add("endpoint", account.endpoint() == null ? JsonNull.INSTANCE : new JsonPrimitive(account.endpoint()));
        String key = account.apiKey() == null ? "" : account.apiKey().trim();
        json.add("maskedKey", key.isEmpty() ? JsonNull.INSTANCE : new JsonPrimitive(maskKey(key)));
        if (account.skin() != null) {
            json.addProperty("skinSet", true);
        }
        return json;
    }

    /// The key with only its head showing: the first eight characters plus an
    /// ellipsis, which is enough to tell two keys apart and nothing more.
    static String maskKey(String key) {
        return key.substring(0, Math.min(8, key.length())) + "…";
    }

    static String kindName(DshAccount.AccountKind kind) {
        return switch (kind) {
            case OFFICIAL -> "official";
            case THIRD_PARTY -> "third-party";
            case OFFLINE -> "offline";
        };
    }

    private static LauncherSettings settings() {
        return SettingsManager.settings();
    }

    private static String[] split(@Nullable String pathInfo) {
        if (pathInfo == null || pathInfo.isEmpty() || pathInfo.equals("/")) {
            return new String[0];
        }
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        return trimmed.split("/");
    }

    private static @Nullable JsonObject body(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            JsonObject object = JsonParser
                    .parseString(new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            if (object.isJsonNull()) {
                throw new JsonParseException("not an object");
            }
            return object;
        } catch (JsonParseException | IllegalStateException e) {
            Json.error(response, HttpServletResponse.SC_BAD_REQUEST, "request body must be a JSON object");
            return null;
        }
    }

    private static @Nullable String stringField(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive() ? object.get(name).getAsString() : null;
    }
}
