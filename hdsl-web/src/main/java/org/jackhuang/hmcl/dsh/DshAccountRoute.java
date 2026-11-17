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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/// The supplier an account gives the harness, kept in the profile's **own** patch layer.
///
/// The harness asks the person to configure a model supplier before it will answer anything. The
/// launcher can spare them that: it holds a key already — that is what an account is — so it hands
/// the harness a route for it.
///
/// The route goes into the same file the harness's own configuration editor writes,
/// `$DSH_HOME/profiles/<profile>/cordis.patch.yml`, under the supplier entry's
/// `config.providers.<route>`. It used to travel as a `--patch` overlay instead, and that is what had
/// to change, because an overlay cannot be composed with the editor:
///
/// - `readProfilePatches` applies the bundle layers, then the profile's own layer, then the home
///   layer, then the `--patch` overlays — the overlays last;
/// - `applyEntryPatches` sets each key of a patch row onto the entry (`target[key] = value`), so a
///   row's `config` **replaces** the entry's config rather than merging with it: an overlay naming
///   `llm-pi-ai` owned every supplier the run had, not just the account's;
/// - the editor refuses a save whose recomposed value is not what the person asked for, with
///   `Configuration for "llm-pi-ai" is overridden by a home patch or command-line overlay`.
///
/// So while an instance was launched with an account, adding a supplier in the harness was impossible
/// and the suppliers the person had saved were invisible — the same defect seen from two sides.
/// Written into the profile's own layer there is nothing above it: the document the editor writes is
/// the document that is composed, and its guard passes by construction.
///
/// The route lives in that file **for as long as the launch does**, and no longer. It has to be there
/// while the harness runs — that is what the models page draws and edits — and it has to go when the
/// launch ends, because its key travels in a variable only that launch set: a route left behind is a
/// supplier the harness offers with nothing behind it, and the next launch, with an account or with
/// none, would inherit it. So a launch writes its own route (refreshed from the vendor every time)
/// and the end of that launch takes it back, byte for byte. Two things keep that safe:
///
/// - the key travels in a variable named after the route ([#environmentVariable]), so a route from a
///   home whose launcher was killed names a variable nothing sets. It fails loudly, and it cannot
///   pick up the key of whichever account is launched next;
/// - every launch first takes back what it can recognise as its own — the ledger's routes — except
///   the one it is about to write.
///
/// The key itself is still never written anywhere: the route names an environment variable, and the
/// value travels in the child's environment, which leaves no trace and is gone with the process.
@NotNullByDefault
public final class DshAccountRoute {
    /// What a route's key variable starts with.
    ///
    /// Named for this launcher rather than for the vendor: the vendor's own variable may already be
    /// set in the person's environment with a different key. The route's own name is appended, so two
    /// accounts never share one variable — see [#environmentVariable].
    public static final String KEY_ENVIRONMENT_VARIABLE = "HDSL_LAUNCH_API_KEY";

    /// The variable the harness's own web search reads its key from.
    ///
    /// `dsh-base` gives the `web-search-deepseek` entry this name, and the launcher used to override
    /// the entry with a patch to point it at its own variable — which is the same trap as the
    /// supplier route, for a second entry. Setting the name the entry already reads costs nothing and
    /// touches nothing: the search works, and the person's own configuration of that entry is theirs.
    /// It is written only for a DeepSeek route, because it is DeepSeek's search service and another
    /// vendor's key would be refused by it.
    public static final String WEB_SEARCH_ENVIRONMENT_VARIABLE = "DEEPSEEK_API_KEY";

    /// The entry the harness mounts its supplier routes under.
    private static final String ENTRY = "llm-pi-ai";

    private static final org.jackhuang.hmcl.util.logging.Logger LOG =
            org.jackhuang.hmcl.util.logging.Logger.LOG;

    /// How much of a route's name a key variable keeps. Enough to recognise, short of any limit a
    /// platform puts on a variable's name.
    private static final int READABLE_LIMIT = 24;

    /// The model ids that accept images, under the names the suppliers publish them as.
    ///
    /// Only ids **known** to be image-capable are here. Guessing the other way is worse than leaving
    /// a model out: `read_image` would then hand an image to a supplier that cannot take one, and the
    /// failure would arrive as a request error rather than as a tool that is simply not offered.
    ///
    /// `deepseek-flash` is the one that matters in practice — the vision-experimental name was routed
    /// onto it, and it is the id the vendor's own model list returns — and `deepseek-v4-flash-vision-exp`
    /// is its catalogue name in the harness. Both are declared image-capable by the harness's own
    /// DeepSeek adapter.
    private static final Set<String> IMAGE_MODELS = Set.of(
            "deepseek-flash",
            "deepseek-v4-flash-vision-exp");

    /// The context capacity the harness's own DeepSeek adapter gives every model it serves.
    ///
    /// `dsh-llm-deepseek` declares `DEFAULT_CONTEXT_WINDOW = 1_000_000` and hands it to the models
    /// it ships. A route the launcher writes is not in that adapter's catalogue, so the harness
    /// cannot fill this in for it. Without it `pi-ai` falls back to `DEFAULT_CONTEXT_WINDOW =
    /// 262_144` for a model neither it nor its own catalogue sizes, which is 256 Ki where the same
    /// model gets 1M through the harness's own route.
    private static final int DEEPSEEK_CONTEXT_WINDOW = 1_000_000;

    /// The output cap the harness's own DeepSeek adapter gives every model it serves.
    ///
    /// The same mirror: `dsh-llm-deepseek` declares `DEFAULT_MAX_TOKENS = 256_000`, against
    /// `pi-ai`'s fallback of 32_768.
    private static final int DEEPSEEK_MAX_TOKENS = 256_000;

    private DshAccountRoute() {
    }

    /// The environment variable a route's key travels in.
    ///
    /// One variable per route, so that a route the launcher wrote for one account can never be handed
    /// the key of another: a route whose account is gone names a variable nothing sets. The name is
    /// what the route's own is, as far as a variable can spell it, and a digest of the exact name —
    /// because two routes may well spell the same way (`a-b` and `a_b`) and they must not share a key.
    ///
    /// @param route the route's name
    /// @return the variable
    public static String environmentVariable(String route) {
        String readable = route.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (readable.length() > READABLE_LIMIT) {
            readable = readable.substring(0, READABLE_LIMIT);
        }
        if (readable.isEmpty()) {
            readable = "ROUTE";
        }
        return KEY_ENVIRONMENT_VARIABLE + "_" + readable + "_"
                + String.format("%08X", route.hashCode());
    }

    /// An account's route, described but not yet written.
    ///
    /// The two halves of handing a supplier over take very different amounts of time. Saying what the
    /// supplier is called and where it lives costs nothing; asking it which models it serves is a
    /// request over the network, and is the whole of the wait. Keeping the two apart is what lets a
    /// launch show them as separate steps rather than one long silence.
    public static final class Prepared {
        private final String route;
        private final String api;
        private final String fallbackModel;
        private final @Nullable String endpoint;

        /// Whether this route is the one the harness's own DeepSeek adapter describes.
        ///
        /// A route the launcher writes is not in the harness's catalogue, so the harness cannot
        /// fill the model's reasoning capability or its capacity in itself; the launcher has to say
        /// both. For a vendor the harness does have an adapter for, the launcher says exactly what
        /// that adapter says, so the same model thinks the same way and holds the same context
        /// whichever account it is reached through.
        private final boolean deepSeek;

        private @Nullable List<String> served;

        /// Why the supplier named no models, when it named none. Carried so the refusal at
        /// [#render] can say what actually happened instead of only that nothing came back.
        private @Nullable String modelsFailure;

        private Prepared(String route, String api, String fallbackModel,
                         @Nullable String endpoint, boolean deepSeek) {
            this.route = route;
            this.api = api;
            this.fallbackModel = fallbackModel;
            this.endpoint = endpoint;
            this.deepSeek = deepSeek;
        }

        /// The route the harness will know this supplier by.
        public String route() {
            return route;
        }

        /// The variable this route's key travels in.
        public String environmentVariable() {
            return DshAccountRoute.environmentVariable(route);
        }

        /// Whether the harness's own web search should be given this key too.
        public boolean deepSeek() {
            return deepSeek;
        }

        /// Asks the supplier which models it serves — the one slow step, and the only thing here
        /// that touches the network.
        ///
        /// **Asked every launch and never remembered.** A list is the vendor's to state and it
        /// changes often: one written down once, by the person or by a launcher that cached it, goes
        /// stale in both directions, showing models that are gone and hiding the ones that arrived.
        /// And a route the launcher writes is not in the harness's catalogue, so the harness will not
        /// fill this in either — while a route with no models cannot be registered at all — so asking
        /// is the only source, not a convenience.
        ///
        /// @param account the account whose supplier is asked
        public void resolveModels(DshAccount account) {
            DshAccount.ModelsAnswer answer = account.fetchModelsAnswer();
            resolveModels(answer.models(), answer.failure());
            if (answer.empty() && !fallbackModel.isEmpty()) {
                // Say it out loud: the launch will succeed and run a model the person named earlier,
                // and the next person to read the log should not have to guess why the list is one
                // entry long instead of the vendor's twenty.
                LOG.warning("Could not read the models of " + endpoint + " for route " + route
                        + " (" + answer.failure() + "); launching with the model named on the account: "
                        + fallbackModel);
            }
        }

        /// Records the models the supplier served, for a caller that already asked it.
        ///
        /// The launch asks through [#resolveModels(DshAccount)]; this is the same answer arriving
        /// from anywhere else — a retry, or a test that has a payload and no network.
        ///
        /// @param models the model ids
        void resolveModels(List<String> models) {
            resolveModels(models, null);
        }

        /// The same, with the reason the supplier named no models.
        ///
        /// @param models  the model ids
        /// @param failure why the list is empty, or `null`
        void resolveModels(List<String> models, @Nullable String failure) {
            this.served = models;
            this.modelsFailure = failure;
        }

        /// Renders the route's own settings, as they go under `config.providers.<route>`.
        ///
        /// **Without the route's own key.** Where that key goes and how far it is indented is
        /// [DshProfilePatch]'s business, because the file's indentation is; a block that carried a key
        /// of its own would arrive as a route named after itself — `providers.MCXCC-sf1.MCXCC-sf1` —
        /// which describes no models and is refused with "resolves no models".
        ///
        /// Kept apart from writing so a test can read exactly what a launch would put in the file:
        /// the file itself is the instance's, which is not a test's to touch.
        ///
        /// @return the YAML for one provider's settings
        /// @throws DshException when there is no model to write, which is a launch that cannot be
        ///                       handed a supplier at all
        String render() throws DshException {
            String model = fallbackModel;
            StringBuilder yaml = new StringBuilder();
            yaml.append("apiKeyEnv: ").append(environmentVariable()).append("\n");
            yaml.append("api: ").append(api).append("\n");
            if (endpoint != null && !endpoint.isBlank()) {
                yaml.append("baseURL: ").append(YamlScalar.of(endpoint.trim())).append("\n");
            }
            if (deepSeek) {
                // Provider-level, not per model. `dsh-llm-deepseek` states a `defaultContextWindow`
                // and a `defaultMaxTokens` for the whole route, and every model it does not
                // otherwise size takes those. `pi-ai` sizes a model from its entry, then its own
                // catalogue, then this pair — its own fallbacks being 262_144 and 32_768. So this
                // is what makes the same model hold the same context and answer in the same length
                // through either route.
                yaml.append("defaultContextWindow: ").append(DEEPSEEK_CONTEXT_WINDOW).append("\n");
                yaml.append("defaultMaxTokens: ").append(DEEPSEEK_MAX_TOKENS).append("\n");
            }
            yaml.append("models:\n");
            // Which models this route serves, **asked of the vendor every launch and never remembered**.
            //
            // Both halves of that matter. A route the launcher writes is not in the harness's catalogue,
            // so the harness will not fill this in the way it does for a vendor it knows — and a route
            // with no models cannot be registered at all — so asking is the only source, not a
            // convenience. And a list is the vendor's to state and it changes often: one written down
            // once, by the person or by a launcher that cached it, goes stale in both directions, showing
            // models that are gone and hiding the ones that arrived.
            //
            // A model stored on the account survives as a fallback for a supplier that cannot be
            // reached at this moment — something has to be written, and a name this account used
            // before guesses better than an invented one. It is a fallback, not a source; nothing
            // asks for it any more.
            //
            // With neither, the launch stops here rather than inventing a name. This route is not in
            // the harness's catalogue, so the harness cannot fill the list in either, and both ways of
            // getting past that are worse than saying so: an empty list is refused for naming no
            // models, and a made-up one is refused for what that model lacks. Both sentences would be
            // about something the person never chose.
            List<String> models = new java.util.ArrayList<>(served == null ? List.of() : served);
            if (models.isEmpty() && !model.isEmpty()) {
                models.add(model);
            }
            if (models.isEmpty()) {
                StringBuilder message = new StringBuilder("Could not read the models of ")
                        .append(endpoint);
                if (modelsFailure != null && !modelsFailure.isBlank()) {
                    message.append(" (").append(modelsFailure).append(")");
                }
                message.append(", so there is no route to write for ").append(route)
                        .append(". This route is not one the harness ships, so it cannot fill the list in")
                        .append(" either: fix this account's key or address, or name a model on it")
                        .append(" (设置 → 账户 → 模型) and launch again.");
                throw new DshException(message.toString());
            }
            for (String one : models) {
                yaml.append("  - id: ").append(YamlScalar.of(one)).append("\n");
                yaml.append("    name: ").append(YamlScalar.of(one)).append("\n");
                if (IMAGE_MODELS.contains(one)) {
                    // A capability the harness cannot fill in for itself, because this route is one it
                    // has never heard of: `input` otherwise falls back to `["text"]`, and the harness's
                    // `read_image` refuses to hand an image to a model that "does not declare image
                    // input". The harness's own DeepSeek adapter declares this same model with
                    // `inputModalities: ["text", "image"]`, so the model reached through a supplier of
                    // the person's own is written the same way — the same model should not answer with
                    // and without eyes depending on which account it was launched with.
                    yaml.append("    input:\n");
                    yaml.append("      - text\n");
                    yaml.append("      - image\n");
                }
                if (deepSeek) {
                    // The harness's DeepSeek adapter offers these four levels for every model it
                    // serves, and reasoningEfforts is how a route it has never heard of says the
                    // same. Without it the model carries no reasoning metadata at all, and the
                    // model picker offers no effort control — only the provider's default. off
                    // carries no wire value because not thinking is the parameter's absence rather
                    // than a value to send; the other three are DeepSeek's own spellings.
                    yaml.append("    reasoningEfforts:\n");
                    yaml.append("      off: null\n");
                    yaml.append("      low: low\n");
                    yaml.append("      high: high\n");
                    yaml.append("      max: max\n");
                }
            }
            return yaml.toString();
        }
    }

    /// Describes an account's route, without asking the supplier anything.
    ///
    /// @param account the account, or `null` for none
    /// @return what to write, or empty when there is nothing to add
    public static Optional<Prepared> prepare(@Nullable DshAccount account) {
        if (account == null || !account.carriesAKey()) {
            return Optional.empty();
        }
        DshVendor vendor = account.vendor();

        // The route is named after the **account**, not after the vendor it borrows its settings
        // from. That is the whole shape of this: the harness is handed a supplier of the person's
        // own, named what they called it, speaking the protocol and living at the address of the
        // vendor they picked. Opening the harness then shows one supplier — their supplier — with
        // that vendor's models behind it, rather than a generic "deepseek" that could be anybody's.
        //
        // Which is also why the person is asked for a name that can be a route: a route name is an
        // identifier, and a name with a space in it would arrive in the harness as something it
        // cannot address. The dialog checks that before it gets here.
        return Optional.of(new Prepared(account.displayName(),
                vendor == null ? "openai-completions" : vendor.api(),
                account.modelOrDefault(), account.endpoint(), isDeepSeek(vendor, account.endpoint())));
    }

    /// Writes an account's route into the profile's own patch layer.
    ///
    /// Nothing else in the file is touched, and a file that already says exactly this is left alone,
    /// so a launch does not rewrite the person's file just to repeat itself.
    ///
    /// @param home    the instance's `DSH_HOME`
    /// @param profile the profile the instance boots
    /// @param route   the described route
    /// @return whether the file had to be written, which it does not when it already says this
    /// @throws DshException when the file cannot be read or written, or holds an entry written in a
    ///                      shape this will not rewrite — see [DshProfilePatch#putProvider]
    public static boolean apply(Path home, String profile, Prepared route) throws DshException {
        return DshProfilePatch.putProvider(DshPluginPatch.patchFile(home, profile), ENTRY,
                route.route(), route.render()).changed();
    }

    /// Takes an account's route back out of the profile's own patch layer.
    ///
    /// What the launch wrote, the end of that launch takes away — see
    /// [DshProfilePatch#removeProvider] for why a route cannot outlive its launch.
    ///
    /// @param home    the instance's `DSH_HOME`
    /// @param profile the profile the instance booted
    /// @param route   the route's name
    /// @return whether the file had to be written
    /// @throws DshException when the file cannot be read or written
    public static boolean remove(Path home, String profile, String route) throws DshException {
        return DshProfilePatch.removeProvider(DshPluginPatch.patchFile(home, profile), ENTRY, route)
                .changed();
    }

    /// Reports whether this route is the one the harness's own DeepSeek adapter describes.
    ///
    /// Only the DeepSeek adapter is mirrored, because that is the one the launcher's account
    /// plumbing is built around and the one whose models are known to think. The test is the
    /// harness's own: the vendor id, or an address under deepseek.com, which is how pi-ai decides
    /// to speak DeepSeek's dialect. A route neither test recognises is left alone — declaring
    /// reasoning levels for a model that has none would offer a control that does not work, and
    /// claiming a capacity it does not have could let a request overrun it.
    ///
    /// @param vendor   the route's vendor, or null
    /// @param endpoint the route's address, or null
    /// @return whether to mirror the adapter's reasoning levels and capacities
    private static boolean isDeepSeek(@Nullable DshVendor vendor, @Nullable String endpoint) {
        if (vendor != null && "deepseek".equals(vendor.id())) {
            return true;
        }
        String host = DshVendor.hostOf(endpoint);
        return host != null && (host.equals("deepseek.com") || host.endsWith(".deepseek.com"));
    }
}
