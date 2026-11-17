/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.util.io.HttpClients;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/// An account: a vendor and the key that is allowed to speak for it.
///
/// The original's accounts are logins it performs on the user's behalf; there is no login here,
/// because DeepSeek Harness is not a service with accounts — it is a program that talks to whichever
/// model supplier it is configured with. So the thing that stands in the same place is a key: what
/// the launcher can hold, check, and hand to an instance.
///
/// The key is kept in the launcher's own settings, which already holds this machine's proxy
/// password. It is not written into an instance, a profile or a pack.
///
/// @param vendorId  the vendor's id, which is also the route name in the harness
/// @param apiKey    the key
/// @param baseUrl   the endpoint, for a vendor whose address is per account; empty otherwise
/// @param label     what the person calls this account, for telling two of them apart
public record DshAccount(
        /// Which kind of account this is.
        ///
        /// A kind rather than a flag, because the two answer different questions. An **official** or
        /// **third-party** account is a key: the launcher hands it to the harness so the harness can
        /// talk to a supplier, and it is worth checking that the key still works before a launch. An
        /// **offline** account is a name and a face: nothing is handed to anybody, nothing is
        /// checked, and what it is for is being able to launch the harness the way it was configured
        /// by hand — which is a real arrangement and not an omission, the same way the original's
        /// offline mode lets somebody play without a login.
        AccountKind kind,
        String vendorId,
        String apiKey,
        @Nullable String baseUrl,
        @Nullable String label,
        /// The model the harness should start with on this account's route, or empty.
        ///
        /// Empty is a real answer and the default one: which model a route offers is the vendor's
        /// catalogue to describe, and the harness refuses to start on a default it cannot resolve,
        /// so a name invented here would turn a convenience into a launch that fails.
        @Nullable String model,

        /// How this account's skin was chosen.
        ///
        /// Stored as the *choice* rather than as the picture, as the original does: a picture alone
        /// cannot answer "which of the launcher's own skins is this", and so cannot draw the chooser
        /// in the state it was left in. Empty means the body's own default.
        @Nullable org.jackhuang.hmcl.dsh.skin.DshSkinChoice skin) {

    /// Returns the skin choice, never `null`.
    ///
    /// @return the choice, or the default one
    public org.jackhuang.hmcl.dsh.skin.DshSkinChoice skinOrDefault() {
        return skin == null ? org.jackhuang.hmcl.dsh.skin.DshSkinChoice.DEFAULT : skin;
    }

    /// Returns a copy with a different skin.
    ///
    /// @param newSkin the choice
    /// @return the copy
    public DshAccount withSkin(org.jackhuang.hmcl.dsh.skin.DshSkinChoice newSkin) {
        return new DshAccount(kind, vendorId, apiKey, baseUrl, label, model, newSkin);
    }

    /// Which kind of account this is.
    ///
    /// The names are pinned rather than left to the enum's own spelling, because they are written to
    /// a file: a rename in the code would otherwise be a rename of the stored format, and every
    /// account on disk would stop being readable. `THIRD_PARTY` is `third-party` for the same reason
    /// it is not `thirdParty` — the file is read by people as well as by this program.
    public enum AccountKind {
        /// The vendor the harness itself is built around, added as the leading choice.
        @com.google.gson.annotations.SerializedName("official")
        OFFICIAL,

        /// A supplier somebody asked for by name.
        @com.google.gson.annotations.SerializedName("third-party")
        THIRD_PARTY,

        /// A name and a skin, with nothing handed to the harness and nothing checked.
        @com.google.gson.annotations.SerializedName("offline")
        OFFLINE
    }

    /// Creates an official or third-party account with no model named.
    ///
    /// @param vendorId the vendor's id
    /// @param apiKey   the key
    /// @param baseUrl  the endpoint, or `null`
    /// @param label    what the person calls it, or `null`
    public DshAccount(String vendorId, String apiKey, @Nullable String baseUrl, @Nullable String label) {
        this(vendorId.equals(DshVendor.offered().get(0).id())
                        ? AccountKind.OFFICIAL : AccountKind.THIRD_PARTY,
                vendorId, apiKey, baseUrl, label, null, null);
    }

    /// Creates an account of a stated kind, naming no model.
    ///
    /// @param kind     the kind
    /// @param vendorId the vendor's id
    /// @param apiKey   the key
    /// @param baseUrl  the endpoint, or `null`
    /// @param label    what the person calls it, or `null`
    public DshAccount(AccountKind kind, String vendorId, String apiKey,
                      @Nullable String baseUrl, @Nullable String label) {
        this(kind, vendorId, apiKey, baseUrl, label, null, null);
    }

    /// Creates an offline account: a name, and nothing to check or hand over.
    ///
    /// @param label the name
    /// @return the account
    public static DshAccount offline(String label) {
        return new DshAccount(AccountKind.OFFLINE, "offline", "", null, label, null, null);
    }

    /// How long a key check is given.
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    /// The vendor this account is for.
    ///
    /// @return the vendor, or `null` when the id names one this launcher does not offer
    public @Nullable DshVendor vendor() {
        return DshVendor.byId(vendorId);
    }

    /// Reports whether a name can be used as a route name in the harness.
    ///
    /// The name an account is given becomes the name of the supplier the harness is handed, so it
    /// has to be something the harness can address: letters, digits, and the three separators a
    /// route name is made of. A space would arrive as two arguments' worth of something the harness
    /// cannot find, and a colon would end the YAML key early.
    ///
    /// @param name the name
    /// @return whether it may be used
    public static boolean isUsableName(@Nullable String name) {
        return name != null && name.matches("[A-Za-z0-9][A-Za-z0-9._-]*");
    }

    /// Reports whether this account has anything to hand to the harness.
    ///
    /// An offline account does not: it is a name and a face, and the launcher must not write a route
    /// for it, must not set a default model for it, and must not put a key in the child's
    /// environment. Everything that touches the harness asks this first.
    ///
    /// @return whether a key travels with this account
    public boolean carriesAKey() {
        return kind != AccountKind.OFFLINE && apiKey != null && !apiKey.isBlank();
    }

    /// Returns the key that names this account.
    ///
    /// The vendor id and the label together: a vendor may be used twice with two keys, and the
    /// label is what tells them apart. Stored on an instance to say which account it launches with,
    /// so it has to survive being written to a file and read back.
    ///
    /// @return the key
    public String key() {
        return vendorId + (label == null || label.isBlank() ? "" : "|" + label.trim());
    }

    /// Returns the model this account names, or empty.
    ///
    /// @return the model id
    public String modelOrDefault() {
        return model == null ? "" : model.trim();
    }

    /// Reports whether this account is the one a key names.
    ///
    /// @param key the key, or `null`
    /// @return whether they are the same account
    public boolean matchesKey(@Nullable String key) {
        return key != null && key.equals(key());
    }

    /// Returns the endpoint to talk to.
    ///
    /// The account's own address wins, so an account can point at a gateway the launcher has never
    /// heard of; otherwise the address is the vendor's. **The vendor's** includes the suppliers
    /// somebody added by address, which are not among the ones the launcher ships and so have to be
    /// looked for where they are kept. Without that second place an account made on a supplier
    /// somebody added reaches a launch with no address at all: the route is written without a
    /// `baseURL`, the harness refuses it, and the key it carries cannot even be checked.
    ///
    /// @return the address, or `null` when neither is known
    public @Nullable String endpoint() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.trim();
        }
        DshVendor vendor = vendor();
        if (vendor != null && vendor.hasBaseUrl()) {
            return vendor.baseUrl();
        }
        for (DshVendor added : SettingsManager.settings().getCustomVendors()) {
            if (added.hasBaseUrl() && added.id().equalsIgnoreCase(vendorId)) {
                return added.baseUrl();
            }
        }
        return null;
    }

    /// Returns what the interface shows for this account.
    ///
    /// @return the name
    public String displayName() {
        if (label != null && !label.isBlank()) {
            return label.trim();
        }
        DshVendor vendor = vendor();
        return vendor == null ? vendorId : vendor.displayName();
    }

    /// Returns the key with everything but its ends hidden, for showing on a row.
    ///
    /// @return the masked key
    public String maskedKey() {
        String key = apiKey == null ? "" : apiKey.trim();
        if (key.length() <= 8) {
            return "••••";
        }
        return key.substring(0, 4) + "••••" + key.substring(key.length() - 4);
    }

    /// Checks the key against the vendor.
    ///
    /// Done here rather than by asking the harness, and that is not a shortcut: the harness answers
    /// out of a local catalogue for a vendor it knows and never makes the request, so asking it
    /// would report success for any string. What actually decides is the vendor's own `/models`
    /// endpoint, which is the smallest call that requires a valid key and does not spend anything.
    ///
    /// The distinction that matters is between "the vendor says no" and "the vendor could not be
    /// reached": only the first means the key is wrong, and a network that cannot reach the endpoint
    /// must not be reported as a bad key.
    ///
    /// @return what the check found
    public Check check() {
        String address = endpoint();
        if (address == null || address.isBlank()) {
            return new Check(Outcome.UNREACHABLE, i18n("dsh.account.check.no_endpoint"));
        }
        DshVendor vendor = vendor();
        String api = vendor == null ? "openai-completions" : vendor.api();
        String url = listingUrl(address, api);

        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET();
            if ("anthropic-messages".equals(api)) {
                request.header("x-api-key", apiKey == null ? "" : apiKey.trim());
                request.header("anthropic-version", "2023-06-01");
            } else {
                request.header("Authorization", "Bearer " + (apiKey == null ? "" : apiKey.trim()));
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            try {
                HttpResponse<String> response = client.send(request.build(),
                        HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status >= 200 && status < 300) {
                    // The status alone is not the answer: a host that replies `200 Not Found` to
                    // every path would otherwise be reported as a reachable supplier with a working
                    // key. What says the key worked is a model list coming back.
                    return looksLikeAListing(response.body())
                            ? new Check(Outcome.VALID, i18n("dsh.account.check.valid"))
                            : new Check(Outcome.UNKNOWN, i18n("dsh.account.check.not_a_listing",
                                    String.valueOf(status), url));
                }
                if (status == 401 || status == 403) {
                    // What this can and cannot say. An address that did not accept the key is not the
                    // same thing as a key that is wrong, and the status does not tell them apart: one
                    // service answers the identical `401 Token is invalid.` to a request with no key,
                    // with a wrong one, and with a key issued for its other region — the last of which
                    // is a correct key at an address that will never take it. So the sentence names
                    // both possibilities, and repeats whatever the vendor was willing to say.
                    String said = saidIn(response.body());
                    return new Check(Outcome.REJECTED, i18n("dsh.account.check.not_accepted",
                            String.valueOf(status),
                            said == null ? "" : i18n("dsh.account.check.vendor_said", said)));
                }
                // 404 and 400 are the endpoint answering, which is not the same as the key being
                // wrong: some gateways do not serve a model list at all.
                if (status == 404 || status == 400 || status == 405) {
                    return new Check(Outcome.UNKNOWN,
                            i18n("dsh.account.check.no_listing", String.valueOf(status)));
                }
                return new Check(Outcome.UNREACHABLE, i18n("dsh.account.check.http", String.valueOf(status)));
            } finally {
                HttpClients.closeQuietly(client);
            }
        } catch (IOException e) {
            // A connection failure often carries no message at all — `ConnectException` from a
            // closed port says nothing — so the exception's own name is the more useful half.
            String why = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName() : e.getMessage();
            return new Check(Outcome.UNREACHABLE, i18n("dsh.account.check.unreachable", url, why));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Check(Outcome.UNREACHABLE, i18n("dsh.account.check.interrupted"));
        } catch (RuntimeException e) {
            return new Check(Outcome.UNREACHABLE, i18n("dsh.account.check.bad_address", String.valueOf(e.getMessage())));
        }
    }

    /// What an address answered when it was asked for its model list.
    ///
    /// @param status  the HTTP status, or `-1` when nothing answered at all
    /// @param listing whether the answer was a model listing
    public record Answer(int status, boolean listing) {

        /// Reports whether something that lists models answered here.
        ///
        /// A refusal counts as an answer: `401` and `403` are an API saying it wants a key, which is
        /// the ordinary thing to hear when adding a supplier, before there is one to send.
        ///
        /// A `2xx` alone does not. The status says a server replied, and a server that answers
        /// everything with `200 Not Found` — a site behind a proxy that serves a page for every path
        /// — is exactly the address this must refuse: taking the status for the answer is how one
        /// was added as a supplier, and a supplier that lists no models is a route that cannot be
        /// built. What the harness's own discovery reads is the body, and so does this.
        ///
        /// @return whether a model API is there
        public boolean listsModels() {
            return listing || status == 401 || status == 403;
        }
    }

    /// Asks an address for its model list, without a key.
    ///
    /// This is what tells a person who pasted an address that is not in the catalogue whether they
    /// have found a supplier or mistyped a hostname. The call is the one every OpenAI-compatible
    /// service has — `{address}/models`, the protocol the supplier being added will speak — and both
    /// halves of the answer are read, because either alone says the wrong thing: a status without a
    /// body accepts a page that answers `200` to everything, and a body without a status cannot tell
    /// "wants a key" from "nothing is listening".
    ///
    /// @param baseUrl the address, without `/models`
    /// @return what it answered
    public static Answer ask(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return new Answer(-1, false);
        }
        String url = listingUrl(baseUrl.trim(), "openai-completions");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            try {
                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                return new Answer(response.statusCode(), looksLikeAListing(response.body()));
            } finally {
                HttpClients.closeQuietly(client);
            }
        } catch (IOException e) {
            String why = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName() : e.getMessage();
            org.jackhuang.hmcl.util.logging.Logger.LOG.info("Nothing answered at " + url + ": " + why);
            return new Answer(-1, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Answer(-1, false);
        } catch (RuntimeException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.info("Not an address: " + baseUrl);
            return new Answer(-1, false);
        }
    }

    /// Reports whether an answer's body is a model listing.
    ///
    /// The **shape** and not the contents: a listing with no models in it is still a listing, and a
    /// supplier that hides its models until it is given a key answers with one. A body that is not
    /// JSON, or is JSON that holds neither a `data` array nor a `models` object, is a page or an
    /// error message — not something a model list can be read out of.
    ///
    /// @param body the answer
    /// @return whether it is a listing
    static boolean looksLikeAListing(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        try {
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return false;
            }
            com.google.gson.JsonObject root = parsed.getAsJsonObject();
            com.google.gson.JsonElement data = root.get("data");
            if (data != null && data.isJsonArray()) {
                return true;
            }
            com.google.gson.JsonElement models = root.get("models");
            return models != null && models.isJsonObject();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /// How the launcher's own requests name themselves.
    ///
    /// The harness identifies itself on every request it makes to a supplier, in the same shape:
    /// `product/version (+homepage)`. A request that names nobody is the one a proxy or a gateway is
    /// most willing to turn away, and this machine's launcher being the caller is something the
    /// supplier may as well be told.
    private static final String USER_AGENT =
            org.jackhuang.hmcl.Metadata.NAME + "/" + org.jackhuang.hmcl.Metadata.VERSION
                    + " (+" + org.jackhuang.hmcl.Metadata.HOMEPAGE_URL + ")";

    /// 一次“列出模型”的答案：拿到了哪几个，或者为什么没拿到。
    ///
    /// @param models  供应商给的模型 id，按它给的顺序；没拿到就是空表
    /// @param failure 没拿到的原因（给人看的一句话），拿到了就是 `null`
    public record ModelsAnswer(List<String> models, @Nullable String failure) {

        /// 一条都没拿到。
        ///
        /// @return 是否为空
        public boolean empty() {
            return models.isEmpty();
        }
    }

    /// Asks the vendor which models it serves, and says why when it cannot.
    ///
    /// **Failure is not an error here.** A launch must not depend on a supplier answering: a network
    /// that cannot reach it today may reach it tomorrow, and the caller falls back to a name it can
    /// at least register. So this never throws — but it does *not* throw the reason away either:
    /// “the endpoint answered HTTP 401 (invalid_api_key)” is the sentence that lets a person fix
    /// their key, where an empty list leaves them reading “启动失败” with no clue.
    ///
    /// @return the ids the vendor named, or an empty list plus the reason it is empty
    public ModelsAnswer fetchModelsAnswer() {
        String address = endpoint();
        if (address == null || address.isBlank()) {
            return new ModelsAnswer(List.of(), "no endpoint is configured for this account");
        }
        DshVendor vendor = vendor();
        String api = vendor == null ? "openai-completions" : vendor.api();
        String url = listingUrl(address, api);

        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .GET();
            if ("anthropic-messages".equals(api)) {
                request.header("x-api-key", apiKey == null ? "" : apiKey.trim());
                request.header("anthropic-version", "2023-06-01");
            } else {
                request.header("Authorization", "Bearer " + (apiKey == null ? "" : apiKey.trim()));
            }

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            try {
                HttpResponse<String> response = client.send(request.build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return new ModelsAnswer(List.of(),
                            "the endpoint answered HTTP " + response.statusCode() + excerpt(response.body()));
                }
                List<String> models = readModelIds(response.body());
                if (models.isEmpty()) {
                    return new ModelsAnswer(List.of(), "the endpoint answered 2xx but named no models"
                            + excerpt(response.body()));
                }
                return new ModelsAnswer(models, null);
            } finally {
                HttpClients.closeQuietly(client);
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            org.jackhuang.hmcl.util.logging.Logger.LOG.info("Could not read the model list of " + url + ": " + e.getMessage());
            String reason = e instanceof java.net.http.HttpTimeoutException
                    ? "the request timed out after " + TIMEOUT.toSeconds() + " s"
                    : (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return new ModelsAnswer(List.of(), reason);
        }
    }

    /// A short, single-line piece of an error body, with the key redacted.
    ///
    /// Gateways put the useful part in the body (`invalid_api_key`, `model_not_found`) and the
    /// useless part in the headers. Newlines and length are cut so this can live inside one
    /// sentence a person reads in a toast.
    private @Nullable String excerpt(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String text = body;
        if (apiKey != null && !apiKey.isBlank()) {
            text = text.replace(apiKey, "***");
        }
        text = text.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) {
            return "";
        }
        return ": " + (text.length() > 160 ? text.substring(0, 160) + "…" : text);
    }

    /// Asks the vendor which models it serves.
    ///
    /// The same call the key check makes — the one endpoint every OpenAI-compatible service has — but
    /// the answer is read instead of the status. The launcher needs it because a **route it writes
    /// itself is not in the harness's catalogue**, so the harness cannot fill in that route's model
    /// list the way it does for a vendor it knows; a route with no models cannot be registered at
    /// all, so the list has to come from somewhere. Asking the vendor is the same place the harness
    /// would have got it.
    ///
    /// **Failure is not an error here.** A launch must not depend on a supplier answering: a network
    /// that cannot reach it today may reach it tomorrow, and the caller falls back to a name it can
    /// at least register. Only a 2xx with a readable list is an answer; everything else is "no
    /// answer", which is why this returns a list rather than throwing.
    ///
    /// This is the shape for callers that only want the list; [fetchModelsAnswer] is the same
    /// request and keeps the reason.
    ///
    /// @return the model ids the vendor named, in the order it named them, or empty
    public List<String> fetchModels() {
        return fetchModelsAnswer().models();
    }

    /// Returns the address a supplier lists its models at.
    ///
    /// The protocols do not agree on where that is, and the harness's own discovery module is the
    /// authority on the difference: an OpenAI protocol lists at `{baseURL}/models`, while Anthropic
    /// Messages lists at `{root}/v1/models`, the root being the address without its trailing slashes
    /// and without one trailing `/v1` — gateway documentation publishes both spellings of the same
    /// root. Asking Anthropic's dialect at `{baseURL}/models` is a 404, and a 404 here is a route
    /// with no models at all. The query bound is Anthropic's own, and one page is read.
    ///
    /// @param address the account's endpoint
    /// @param api     the wire protocol the supplier speaks
    /// @return the address to list from
    static String listingUrl(String address, String api) {
        String base = address.replaceAll("/+$", "");
        if (!ANTHROPIC_API.equals(api)) {
            return base + "/models";
        }
        String root = base.endsWith("/v1") ? base.substring(0, base.length() - "/v1".length()) : base;
        return root + "/v1/models?limit=" + ANTHROPIC_MODEL_LIMIT;
    }

    /// The protocol whose listing lives under a `/v1` root rather than beside the address.
    private static final String ANTHROPIC_API = "anthropic-messages";

    /// The largest model-list page Anthropic's public endpoint accepts.
    private static final int ANTHROPIC_MODEL_LIMIT = 1000;

    /// The longest reason from a vendor that is repeated under an account's name.
    private static final int SAID_LIMIT = 120;

    /// Reads what a vendor said about a request it refused.
    ///
    /// The fields services put their reason in and no others: `message`, `error.message`, or a bare
    /// `error` string. A body that is a page rather than an answer carries none of them, and a reason
    /// longer than a sentence is cut short — this ends up on one line under a row.
    ///
    /// @param body the answer
    /// @return what it said, or `null` when it said nothing readable
    static @Nullable String saidIn(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return null;
            }
            com.google.gson.JsonObject root = parsed.getAsJsonObject();
            String said = saidInValue(root.get("message"));
            if (said == null) {
                com.google.gson.JsonElement error = root.get("error");
                said = error != null && error.isJsonObject()
                        ? saidInValue(error.getAsJsonObject().get("message"))
                        : saidInValue(error);
            }
            if (said == null) {
                return null;
            }
            String oneLine = said.replaceAll("\\s+", " ").trim();
            if (oneLine.isEmpty()) {
                return null;
            }
            return oneLine.length() <= SAID_LIMIT ? oneLine : oneLine.substring(0, SAID_LIMIT) + "...";
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Returns a reason held in one JSON value, when it is one.
    ///
    /// @param element the value
    /// @return the text, or `null` when there is none
    private static @Nullable String saidInValue(@Nullable com.google.gson.JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        String value = element.getAsString();
        return value.isBlank() ? null : value;
    }

    /// Reads model ids out of what a listing endpoint answered.
    ///
    /// Two shapes are read, because two are published: the `data` array OpenAI-compatible services
    /// share, and the `models` map some gateways expose instead. In the map the **key** is the id —
    /// that is what the harness's own reader takes it for — and an entry's own `id` is read only
    /// where the key says nothing. Anything else is not a model list, and saying so by returning
    /// nothing is better than writing a model the harness will fail to find.
    ///
    /// @param body the answer
    /// @return the ids, or empty when the body is not a model list
    static List<String> readModelIds(@Nullable String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            com.google.gson.JsonElement parsed = com.google.gson.JsonParser.parseString(body);
            if (!parsed.isJsonObject()) {
                return List.of();
            }
            com.google.gson.JsonObject root = parsed.getAsJsonObject();
            List<String> ids = new java.util.ArrayList<>();
            com.google.gson.JsonElement data = root.get("data");
            if (data != null && data.isJsonArray()) {
                for (com.google.gson.JsonElement element : data.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        addModelId(ids, idOf(element.getAsJsonObject()));
                    }
                }
            } else {
                com.google.gson.JsonElement models = root.get("models");
                if (models != null && models.isJsonObject()) {
                    for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry
                            : models.getAsJsonObject().entrySet()) {
                        com.google.gson.JsonElement value = entry.getValue();
                        if (value == null || !value.isJsonObject()) {
                            // What the harness's own reader skips, for the same reason: a value that
                            // is not an object describes no model.
                            continue;
                        }
                        String key = entry.getKey();
                        addModelId(ids, key != null && !key.isBlank()
                                ? key : idOf(value.getAsJsonObject()));
                    }
                }
            }
            return List.copyOf(ids);
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    /// Returns the `id` an entry carries, or `null` when it carries none.
    ///
    /// @param entry the entry
    /// @return the id
    private static @Nullable String idOf(com.google.gson.JsonObject entry) {
        com.google.gson.JsonElement id = entry.get("id");
        if (id == null || !id.isJsonPrimitive() || !id.getAsJsonPrimitive().isString()) {
            return null;
        }
        String text = id.getAsString();
        return text.isBlank() ? null : text;
    }

    /// Adds an id to a list, once, ignoring one that is not an id.
    ///
    /// @param ids the list
    /// @param id  the candidate, or `null`
    private static void addModelId(List<String> ids, @Nullable String id) {
        if (id != null && !id.isBlank() && !ids.contains(id)) {
            ids.add(id);
        }
    }

    /// Returns the account an instance launches with.
    ///
    /// The instance's own choice wins; then the account the person selected on the accounts page;
    /// then the first one the launcher holds. A person with one account means it for everything, and
    /// making them repeat that per instance would be asking a question with one answer.
    ///
    /// The selection is read here because it is the whole of what choosing an account does. It used
    /// to stop at the list's first entry, which made "use the offline account" a choice that changed
    /// nothing: the launch went on using whichever account happened to be stored first, key and all.
    ///
    /// @param instance the instance
    /// @return the account, or `null` when there is none to use
    public static @Nullable DshAccount forInstance(DshInstance instance) {
        try {
            java.util.List<DshAccount> accounts =
                    org.jackhuang.hmcl.setting.SettingsManager.settings().getAccounts();
            if (accounts.isEmpty()) {
                return null;
            }
            String chosen = DshInstanceSettings.accountKey(instance);
            if (chosen != null) {
                for (DshAccount account : accounts) {
                    if (account.matchesKey(chosen)) {
                        return account;
                    }
                }
                // The account it named is gone. Falling back to another would launch with a key
                // nobody chose, so it launches with none and says so.
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Instance " + instance.id() + " names an account that no longer exists: " + chosen);
                return null;
            }
            DshAccount selected =
                    org.jackhuang.hmcl.setting.SettingsManager.settings().activeAccount();
            return selected != null ? selected : accounts.get(0);
        } catch (RuntimeException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Could not read the accounts", e);
            return null;
        }
    }

    /// What a key check found.
    public enum Outcome {
        /// The vendor accepted the key.
        VALID,
        /// The vendor refused the key.
        REJECTED,
        /// The endpoint could not be reached, or cannot answer this question.
        UNREACHABLE,
        /// The endpoint answered but does not offer the call that would answer it.
        UNKNOWN
    }

    /// What a check found, and why.
    ///
    /// @param outcome what happened
    /// @param message a sentence for the person who pressed the button
    public record Check(Outcome outcome, String message) {
    }
}
