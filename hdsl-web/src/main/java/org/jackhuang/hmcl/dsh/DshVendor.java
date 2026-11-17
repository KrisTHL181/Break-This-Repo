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

import java.util.List;
import java.util.Locale;

/// A model supplier this launcher can be told about.
///
/// DeepSeek Harness knows a long list of them — thirty-nine, from the catalogue that ships with the
/// `pi-ai` provider bundle — and each is addressed by an id, reads its key from an environment
/// variable of its own, and talks one of three wire protocols. The three things this launcher needs
/// to know about each are exactly those: what to call it, which variable the harness reads, and
/// which protocol to declare when the route is written out by hand.
///
/// Only a few are offered. A list of thirty-nine in a dropdown is a list nobody reads, and the
/// others can be reached by typing a route of one's own — which is what [DshAccount] allows. The
/// ones here are the ones people actually start with: the harness's own vendor, the two largest
/// general-purpose ones, the two aggregators that resell everything else, and the coding-plan
/// endpoints that a Chinese network can reach without a proxy.
@NotNullByDefault
public record DshVendor(
        /// The id `dsh` addresses this vendor by, which is also the route name.
        String id,

        /// What the interface calls it.
        String displayName,

        /// The environment variable the harness reads this vendor's key from.
        String apiKeyEnv,

        /// The wire protocol its endpoint speaks.
        String api,

        /// The endpoint, or `null` for a vendor whose address is per account.
        @Nullable String baseUrl,

        /// Whether this is the harness's own vendor, which the interface leads with.
        boolean preferred) {

    /// The protocols a hand-written route may declare.
    ///
    /// The harness accepts exactly these three; anything else is rejected when the route is
    /// registered, which fails the launch rather than the request.
    public static final List<String> APIS = List.of(
            "openai-completions", "openai-responses", "anthropic-messages");

    /// The vendors offered, in the order the interface shows them.
    ///
    /// @return the list
    public static List<DshVendor> offered() {
        return OFFERED;
    }

    private static final List<DshVendor> OFFERED = List.of(
            new DshVendor("deepseek", "DeepSeek", "DEEPSEEK_API_KEY",
                    "openai-completions", "https://api.deepseek.com", true),
            new DshVendor("openai", "OpenAI", "OPENAI_API_KEY",
                    "openai-responses", "https://api.openai.com/v1", false),
            new DshVendor("anthropic", "Anthropic", "ANTHROPIC_API_KEY",
                    "anthropic-messages", "https://api.anthropic.com", false),
            new DshVendor("openrouter", "OpenRouter", "OPENROUTER_API_KEY",
                    "openai-completions", "https://openrouter.ai/api/v1", false),
            new DshVendor("vercel-ai-gateway", "Vercel AI Gateway", "AI_GATEWAY_API_KEY",
                    "openai-completions", "https://ai-gateway.vercel.sh", false),
            new DshVendor("moonshotai", "Moonshot (Kimi)", "MOONSHOT_API_KEY",
                    "openai-completions", "https://api.moonshot.ai/v1", false),
            new DshVendor("moonshotai-cn", "Moonshot 中国站", "MOONSHOT_API_KEY",
                    "openai-completions", "https://api.moonshot.cn/v1", false),
            new DshVendor("zai", "智谱 GLM", "ZAI_API_KEY",
                    "anthropic-messages", "https://api.z.ai/api/coding/paas/v4", false),
            new DshVendor("zai-coding-cn", "智谱 GLM 中国站", "ZAI_CODING_CN_API_KEY",
                    "anthropic-messages", "https://open.bigmodel.cn/api/coding/paas/v4", false),
            new DshVendor("google", "Google Gemini", "GEMINI_API_KEY",
                    "openai-completions", "https://generativelanguage.googleapis.com/v1beta", false),
            new DshVendor("groq", "Groq", "GROQ_API_KEY",
                    "openai-completions", "https://api.groq.com/openai/v1", false),
            new DshVendor("mistral", "Mistral", "MISTRAL_API_KEY",
                    "openai-completions", "https://api.mistral.ai", false),
            new DshVendor("xai", "xAI", "XAI_API_KEY",
                    "openai-completions", "https://api.x.ai/v1", false));

    /// Finds a vendor by its id.
    ///
    /// @param id the id, or `null`
    /// @return the vendor, or `null` when it is not one this launcher offers
    public static @Nullable DshVendor byId(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        for (DshVendor vendor : offered()) {
            if (vendor.id.equalsIgnoreCase(trimmed)) {
                return vendor;
            }
        }
        // The catalogue ships suppliers the launcher does not offer as cards but can still
        // route (xiaomi and the token-plan entries, for instance). The providers list hands
        // them out as `known`, so account creation has to resolve them here as well —
        // otherwise picking one fails with "pass `endpoint` to add a supplier by address".
        for (DshVendor vendor : catalogue()) {
            // Only the catalogue entries the launcher can actually route: some lines have no
            // address or no protocol on purpose (per-region or per-account suppliers), and the
            // providers list hands those out as unknown so the person types an address.
            if (vendor.id.equalsIgnoreCase(trimmed)
                    && !vendor.api().isBlank() && vendor.hasBaseUrl()) {
                return vendor;
            }
        }
        return null;
    }

    /// Returns the whole catalogue the harness ships, not only the ones offered.
    ///
    /// [`offered`] is a short list chosen for a dropdown; this is all thirty-seven, and it is what a
    /// base URL is looked up in. The two are different questions — "what should be easy to pick" and
    /// "what does this address belong to" — and answering the second with the first would mean a
    /// person who pastes a perfectly good Kimi endpoint is told it is not a provider.
    ///
    /// Read from `assets/dsh-providers.txt` rather than written out in Java, so that re-reading the
    /// harness's own catalogue is a diff of one data file.
    ///
    /// @return the catalogue, in id order
    public static List<DshVendor> catalogue() {
        List<DshVendor> known = CATALOGUE;
        return known != null ? known : List.of();
    }

    /// Reports whether a name is one a supplier already answers to.
    ///
    /// A route is named after its account, so an account carrying a supplier's own name would build a
    /// route the harness already serves under that name — and the launcher's own tidying, which takes
    /// away the routes it made, would take that one away with them.
    ///
    /// Compared without case: what is refused is a **name**, and `DeepSeek` and `deepseek` are one
    /// supplier to anybody reading them.
    ///
    /// @param name  the name to test, or `null`
    /// @param match which suppliers count
    /// @return whether one of them already answers to it
    public static boolean answersTo(@Nullable String name, java.util.function.Predicate<DshVendor> match) {
        if (name == null || name.isBlank()) {
            return false;
        }
        String trimmed = name.trim();
        for (DshVendor vendor : offered()) {
            if (vendor.id().equalsIgnoreCase(trimmed) && match.test(vendor)) {
                return true;
            }
        }
        for (DshVendor vendor : catalogue()) {
            if (vendor.id().equalsIgnoreCase(trimmed) && match.test(vendor)) {
                return true;
            }
        }
        return false;
    }

    /// Finds the provider an address belongs to.
    ///
    /// Compared by **host**, not by the whole string: an endpoint is written with or without a
    /// trailing slash, with or without `/v1`, and both spellings are the same service. The host is
    /// the part that identifies it, and matching on more than that would fail on exactly the
    /// variations people type. A provider that publishes no address of its own is never found this
    /// way, which is correct — there is nothing to compare against.
    ///
    /// @param url the address
    /// @return the provider, or `null` when the address belongs to none of them
    public static @Nullable DshVendor byBaseUrl(@Nullable String url) {
        String host = hostOf(url);
        if (host == null) {
            return null;
        }
        for (DshVendor vendor : catalogue()) {
            if (host.equals(hostOf(vendor.baseUrl))) {
                return vendor;
            }
        }
        // The offered list may name an address the catalogue types differently; it wins because it
        // carries the name this launcher shows.
        for (DshVendor vendor : offered()) {
            if (host.equals(hostOf(vendor.baseUrl))) {
                return vendor;
            }
        }
        return null;
    }

    /// Returns the host of an address, or `null` when there is not one.
    ///
    /// @param url the address
    /// @return the lowercase host
    public static @Nullable String hostOf(@Nullable String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String host = java.net.URI.create(url.trim()).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /// Makes a vendor for a provider the catalogue does not hold.
    ///
    /// The key's variable name still cannot be discovered — no service publishes the name of the
    /// environment variable a launcher should use — so it is derived from the id, the same way the
    /// harness's own catalogue derives it, and the address is the one that answered. The protocol
    /// sometimes can be: models.dev names the SDK each supplier is published under, and
    /// [DshModelCatalog#protocolOf] reads a wire protocol out of that name.
    ///
    /// @param id      the id, which is also the route name
    /// @param name    what the person called it
    /// @param baseUrl the address, or `null` for one whose address is per account
    /// @return the vendor
    public static DshVendor discovered(String id, String name, @Nullable String baseUrl) {
        return discovered(id, name, baseUrl, null);
    }

    /// Makes a vendor for a provider the catalogue does not hold, stating its protocol.
    ///
    /// @param id      the id, which is also the route name
    /// @param name    what the person called it
    /// @param baseUrl the address, or `null` for one whose address is per account
    /// @param api     the protocol, or `null` to use the first of [APIS]
    /// @return the vendor
    public static DshVendor discovered(String id, String name, @Nullable String baseUrl,
                                       @Nullable String api) {
        String protocol = api != null && APIS.contains(api.trim()) ? api.trim() : APIS.get(0);
        return new DshVendor(id, name, keyVariableOf(id), protocol, baseUrl, false);
    }

    /// The environment variable a key for a vendor with this id is read from.
    ///
    /// Nothing publishes this name, so it is derived from the id exactly as the harness's own
    /// catalogue derives it for the suppliers it does not ship — which is the only reason
    /// deriving it can be right.
    ///
    /// @param id the vendor's id
    /// @return the variable name
    public static String keyVariableOf(String id) {
        return id.toUpperCase(Locale.ROOT).replace('-', '_') + "_API_KEY";
    }

    /// Report whether an id may be used as a route name.
    ///
    /// The id becomes the name of the route the harness is handed, so it has to be something the
    /// harness can address. Same rule as an account's name, and for the same reason.
    ///
    /// @param id the id
    /// @return whether it may be used
    public static boolean isUsableId(@Nullable String id) {
        return id != null && id.matches("[a-z0-9][a-z0-9._-]*");
    }

    /// The catalogue, read once.
    private static @Nullable List<DshVendor> CATALOGUE;

    static {
        CATALOGUE = readCatalogue();
    }

    /// Reads the catalogue from the launcher's own resources.
    ///
    /// A line that cannot be read is skipped rather than fatal: a catalogue is a convenience, and a
    /// launcher that will not start because one line of it is malformed is worse than one that
    /// offers thirty-six providers instead of thirty-seven.
    ///
    /// @return the catalogue
    private static List<DshVendor> readCatalogue() {
        List<DshVendor> vendors = new java.util.ArrayList<>();
        try (java.io.InputStream stream = DshVendor.class
                .getResourceAsStream("/assets/dsh-providers.txt")) {
            if (stream == null) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "No provider catalogue in the jar; only the offered vendors will be known");
                return List.of();
            }
            for (String line : new String(stream.readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8).split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("\\|", -1);
                if (parts.length != 4 || parts[0].isEmpty()) {
                    continue;
                }
                vendors.add(new DshVendor(parts[0], readableName(parts[0]), parts[3], parts[2],
                        parts[1].isEmpty() ? null : parts[1], false));
            }
        } catch (java.io.IOException | RuntimeException e) {
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Could not read the provider catalogue", e);
            return List.of();
        }
        return List.copyOf(vendors);
    }

    /// Turns an id into something readable to show for it.
    ///
    /// The catalogue gives ids and no display names, and the id is what the interface has to show
    /// anyway — it is the name a route is written with. Only the shape is tidied: `zai-coding-cn`
    /// reads better as `Zai Coding Cn` than as itself, and the id is still shown beside it.
    ///
    /// @param id the id
    /// @return the name to display
    private static String readableName(String id) {
        StringBuilder name = new StringBuilder();
        for (String word : id.split("-")) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    /// Reports whether the endpoint is one a key can be checked against.
    ///
    /// A vendor whose address depends on the account cannot be checked without being told the
    /// address, which is what the account's own base URL is for.
    ///
    /// @return whether this vendor states its own address
    public boolean hasBaseUrl() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    /// Returns what the interface shows for this vendor.
    ///
    /// @return the name and the id, because the id is what a settings file will hold
    public String label() {
        return displayName + " (" + id + ")";
    }

    /// Reports whether a key looks like one for this vendor.
    ///
    /// Not a validation — only the vendor can say that — but a key pasted with the wrong vendor
    /// chosen is the commonest mistake, and the prefixes differ enough to catch it: the harness's
    /// own keys begin `sk-`, Anthropic's `sk-ant-`, OpenRouter's `sk-or-`. A key that matches no
    /// known shape is accepted, because this is a guess and refusing on a guess is worse than
    /// letting the vendor answer.
    ///
    /// @param key the key
    /// @return whether it is shaped like one of this vendor's
    public boolean looksLikeItsKey(String key) {
        String trimmed = key == null ? "" : key.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return switch (id.toLowerCase(Locale.ROOT)) {
            case "anthropic" -> trimmed.startsWith("sk-ant-") || trimmed.startsWith("sk-");
            case "openrouter" -> trimmed.startsWith("sk-or-") || trimmed.startsWith("sk-");
            default -> true;
        };
    }
}
