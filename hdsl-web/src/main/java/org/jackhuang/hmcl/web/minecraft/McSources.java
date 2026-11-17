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

import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/// Where third-party cores are fetched from, and how much of each source the
/// panel is still allowed to ask for.
///
/// **Why a source layer at all.** The panel used to talk to each upstream
/// directly. That works from a network that can reach `meta.fabricmc.net` and
/// `maven.minecraftforge.net`; from a domestic line those hosts time out, and
/// PaperMC's v2 API has been retired altogether (HTTP 410 `sunset`). The
/// catalogue then looks empty and the wizard says "no versions", which reads
/// as a panel bug. It is not — it is four networks the panel refuses to route
/// around.
///
/// **Ordered, not exclusive.** Each core has an ordered list of sources. The
/// first source that answers wins; a source that fails is skipped for the rest
/// of the call and is retried on the next one. `official` always stays in the
/// list: when the host can reach upstream, no third party is involved.
///
/// **The mirror is metered.** MSL publishes its download-URL endpoint at 30
/// requests per hour and 60 per day per address, and says it may block
/// silently. So a resolved artifact is written beside the jar (see [McCores]);
/// the endpoint is asked at most once per `core/version`, and once the quota is
/// spent the source is skipped until its reset time instead of being hammered.
///
/// **Attribution.** Using MSL obliges the software to display 「本服务由MSL
/// 开服器提供」 with its logo and site. [Source#attribution] carries that bit
/// so the UI can show the line only while the source is actually enabled.
@NotNullByDefault
public final class McSources {

    /// The upstream project's own API. Always first, always acceptable.
    public static final String OFFICIAL = "official";
    /// BMCLAPI — a domestic mirror of the Forge/NeoForge Maven trees.
    public static final String BMCLAPI = "bmclapi";
    /// MSL — the mirror the two reference panels use. Metered.
    public static final String MSLMC = "mslmc";

    public static final URI OFFICIAL_PAPER = URI.create("https://fill.papermc.io");
    /// PaperMC's retired v2 host. Kept only so a stored custom base pointing at
    /// it can be recognised and reported instead of retried forever.
    public static final URI RETIRED_PAPER_V2 = URI.create("https://api.papermc.io");
    public static final URI OFFICIAL_PURPUR = URI.create("https://api.purpurmc.org");
    public static final URI OFFICIAL_FABRIC = URI.create("https://meta.fabricmc.net");
    public static final URI OFFICIAL_FORGE_MAVEN = URI.create("https://maven.minecraftforge.net");
    public static final URI OFFICIAL_FORGE_PROMOTIONS =
            URI.create("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json");
    public static final URI OFFICIAL_NEOFORGE_MAVEN = URI.create("https://maven.neoforged.net");

    public static final URI BMCLAPI_MAVEN = URI.create("https://bmclapi2.bangbang93.com/maven");
    public static final URI MSLMC_API = URI.create("https://api.mslmc.cn/v4");

    /// How the panel names itself upstream. MSL asks integrators to send a
    /// User-Agent that identifies the application.
    public static final String USER_AGENT = "HDSL-web (self-hosted panel)";

    /// One source, as the settings page shows it.
    ///
    /// @param id          the stable id (`official`, `bmclapi`, `mslmc`)
    /// @param label       the human name
    /// @param note        one line about what it is and what it costs
    /// @param cores       the cores it can serve
    /// @param attribution whether the UI must credit the provider while enabled
    public record Source(String id, String label, String note, List<String> cores,
                         boolean attribution) {
    }

    /// What one metered source has left.
    ///
    /// @param limit     the ceiling the provider published, `-1` when unknown
    /// @param remaining what is left right now, `-1` when unknown
    /// @param resetAt   when the window resets (epoch seconds), `0` when unknown
    /// @param used      how many calls this panel made in the current window
    /// @param updatedAt when this record was last written
    public record Quota(int limit, int remaining, long resetAt, int used, long updatedAt) {

        public static final Quota UNKNOWN = new Quota(-1, -1, 0, 0, 0);

        /// Whether the source has nothing left in this window.
        public boolean exhausted() {
            if (remaining > 0) {
                return false;
            }
            if (remaining < 0) {
                return false;
            }
            return resetAt <= 0 || Instant.now().getEpochSecond() < resetAt;
        }

        /// The same quota after this panel used one call.
        public Quota spend() {
            int nextUsed = used + 1;
            int nextRemaining = remaining < 0 ? remaining : Math.max(0, remaining - 1);
            return new Quota(limit, nextRemaining, resetAt, nextUsed, Instant.now().getEpochSecond());
        }

        /// The same quota after the provider told us where it stands.
        public Quota withHeaders(int headerLimit, int headerRemaining, long headerReset) {
            return new Quota(headerLimit >= 0 ? headerLimit : limit,
                    headerRemaining, headerReset > 0 ? headerReset : resetAt, used,
                    Instant.now().getEpochSecond());
        }
    }

    /// The persisted half: the operator's order and the last seen quota.
    private record Recorded(@SerializedName("order") @Nullable Map<String, List<String>> order,
                            @SerializedName("custom") @Nullable Map<String, String> custom,
                            @SerializedName("quota") @Nullable Map<String, Quota> quota) {
    }

    private static final Logger LOG = Logger.LOG;

    /// The sources this build knows, in the order the settings page lists them.
    private static final List<Source> KNOWN = List.of(
            new Source(OFFICIAL, "官方源",
                    "直连上游项目自己的 API。能用就一定用它，不经过任何第三方。",
                    List.of("paper", "folia", "purpur", "fabric", "forge", "neoforge"), false),
            new Source(BMCLAPI, "BMCLAPI",
                    "国内公共镜像，只代理 Forge/NeoForge 的 Maven 库和元数据。",
                    List.of("forge", "neoforge"), false),
            new Source(MSLMC, "MSL 镜像",
                    "MSLTeam 的服务端镜像。下载地址接口 30 次/小时、60 次/天，用满会自动跳过。",
                    List.of("paper", "folia", "purpur", "fabric", "forge", "neoforge"), true));

    private static final Map<String, List<String>> DEFAULT_ORDER = Map.of(
            "paper", List.of(OFFICIAL, MSLMC),
            "folia", List.of(OFFICIAL, MSLMC),
            "purpur", List.of(OFFICIAL, MSLMC),
            "fabric", List.of(OFFICIAL, MSLMC),
            "forge", List.of(OFFICIAL, BMCLAPI, MSLMC),
            "neoforge", List.of(OFFICIAL, BMCLAPI, MSLMC));

    private final @Nullable Path file;
    private final Map<String, List<String>> order = new LinkedHashMap<>();
    private final Map<String, String> custom = new LinkedHashMap<>();
    private final Map<String, Quota> quota = new LinkedHashMap<>();

    /// A settings store backed by a file.
    ///
    /// @param file the JSON file (`<data>/minecraft/sources.json`), or `null`
    ///             for an in-memory store (tests)
    public McSources(@Nullable Path file) {
        this.file = file;
        load();
    }

    /// The sources this build knows.
    ///
    /// @return the known sources
    public static List<Source> known() {
        return KNOWN;
    }

    /// One source by id.
    ///
    /// @param id the id
    /// @return the source, or `null` when the id is unknown
    public static @Nullable Source byId(String id) {
        for (Source source : KNOWN) {
            if (source.id().equals(id)) {
                return source;
            }
        }
        return null;
    }

    /// Whether a source can serve a core.
    ///
    /// @param sourceId the source
    /// @param core     the core
    /// @return whether the pair is meaningful
    public static boolean supports(String sourceId, String core) {
        Source source = byId(sourceId);
        return source != null && source.cores().contains(core);
    }

    /// The sources to try for one core, in order.
    ///
    /// @param core the core id
    /// @return the ordered source ids, never empty for a known core
    public List<String> orderFor(String core) {
        List<String> stored = order.get(core);
        if (stored != null && !stored.isEmpty()) {
            return List.copyOf(stored);
        }
        return DEFAULT_ORDER.getOrDefault(core, List.of(OFFICIAL));
    }

    /// The whole order table, for the settings page.
    ///
    /// @return core → ordered source ids
    public Map<String, List<String>> order() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (String core : List.of("paper", "folia", "purpur", "fabric", "forge", "neoforge")) {
            copy.put(core, orderFor(core));
        }
        return copy;
    }

    /// Replaces the order for one core. Unknown or unsupported ids are dropped;
    /// `official` is appended when the caller leaves it out, so a settings page
    /// cannot lock the panel out of the only source that never meters.
    ///
    /// @param core   the core
    /// @param source the requested order
    public synchronized void setOrder(String core, List<String> source) {
        List<String> cleaned = new ArrayList<>();
        for (String id : source) {
            if (supports(id, core) && !cleaned.contains(id)) {
                cleaned.add(id);
            }
        }
        if (!cleaned.contains(OFFICIAL)) {
            cleaned.add(OFFICIAL);
        }
        order.put(core, List.copyOf(cleaned));
        save();
    }

    /// The operator's base URL for a kind, if one is set.
    ///
    /// @param kind `paper`, `purpur`, `fabric`, `forge`, `neoforge`, `mslmc`
    /// @return the override, or `null`
    public @Nullable URI customBase(String kind) {
        String value = custom.get(kind);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URI.create(value.strip());
        } catch (IllegalArgumentException e) {
            LOG.warning("Ignoring the custom " + kind + " base `" + value + "`: " + e.getMessage());
            return null;
        }
    }

    /// The custom bases, for the settings page.
    ///
    /// @return kind → base URL
    public Map<String, String> customBases() {
        return Map.copyOf(custom);
    }

    /// Stores a custom base. A blank value removes the override.
    ///
    /// @param kind  the kind
    /// @param value the base URL
    public synchronized void setCustomBase(String kind, @Nullable String value) {
        if (value == null || value.isBlank()) {
            custom.remove(kind);
        } else {
            custom.put(kind, value.strip());
        }
        save();
    }

    /// The last quota seen for a source.
    ///
    /// @param sourceId the source
    /// @return the quota, or [Quota#UNKNOWN]
    public Quota quota(String sourceId) {
        Quota stored = quota.get(sourceId);
        if (stored == null) {
            return Quota.UNKNOWN;
        }
        // A window that has passed says nothing about the next one.
        if (stored.resetAt() > 0 && Instant.now().getEpochSecond() >= stored.resetAt()) {
            return new Quota(stored.limit(), stored.limit(), 0, 0, stored.updatedAt());
        }
        return stored;
    }

    /// Whether a source must be skipped right now.
    ///
    /// @param sourceId the source
    /// @return whether its window is spent
    public boolean exhausted(String sourceId) {
        return MSLMC.equals(sourceId) && quota(sourceId).exhausted();
    }

    /// Records a call this panel is about to make.
    ///
    /// @param sourceId the source
    public synchronized void spend(String sourceId) {
        Quota current = quota(sourceId);
        quota.put(sourceId, current.spend());
        save();
    }

    /// Records what the provider said about the window.
    ///
    /// @param sourceId the source
    /// @param limit    the `x-ratelimit-limit` header, `-1` when absent
    /// @param left     the `x-ratelimit-remaining` header, `-1` when absent
    /// @param reset    the `x-ratelimit-reset` header, `0` when absent
    public synchronized void observe(String sourceId, int limit, int left, long reset) {
        if (limit < 0 && left < 0 && reset <= 0) {
            return;
        }
        quota.put(sourceId, quota(sourceId).withHeaders(limit, left, reset));
        save();
    }

    /// The order the panel should try when a core is unknown to the table.
    ///
    /// @return the fallback order
    public static List<String> fallbackOrder() {
        return List.of(OFFICIAL);
    }

    // ------------------------------------------------------------------ storage --

    private void load() {
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try {
            Recorded recorded = JsonUtils.fromJsonFile(file, Recorded.class);
            if (recorded == null) {
                return;
            }
            if (recorded.order() != null) {
                for (Map.Entry<String, List<String>> entry : recorded.order().entrySet()) {
                    order.put(entry.getKey().toLowerCase(Locale.ROOT), List.copyOf(entry.getValue()));
                }
            }
            if (recorded.custom() != null) {
                custom.putAll(recorded.custom());
            }
            if (recorded.quota() != null) {
                quota.putAll(recorded.quota());
            }
        } catch (Exception e) {
            LOG.warning("Failed to read the Minecraft source settings " + file, e);
        }
    }

    private synchronized void save() {
        if (file == null) {
            return;
        }
        Recorded recorded = new Recorded(Map.copyOf(order), Map.copyOf(custom), Map.copyOf(quota));
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(parent, "mc-sources", ".json");
            try {
                JsonUtils.writeToJsonFile(temp, recorded);
                move(temp, file);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            LOG.warning("Failed to write the Minecraft source settings " + file, e);
        }
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
