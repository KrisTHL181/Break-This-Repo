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
package org.jackhuang.hmcl.web.node;

import com.google.gson.JsonObject;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.DshProcessManager;
import org.jackhuang.hmcl.dsh.NodeChannels;
import org.jackhuang.hmcl.dsh.NodeLastGood;
import org.jackhuang.hmcl.dsh.NodeRuntime;
import org.jackhuang.hmcl.dsh.NodeRuntimeManager;
import org.jackhuang.hmcl.dsh.NodeSelection;
import org.jackhuang.hmcl.dsh.NodeSemver;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jackhuang.hmcl.web.brand.Brand;
import org.jackhuang.hmcl.web.brand.BrandInstance;
import org.jackhuang.hmcl.web.brand.BrandInstanceManager;
import org.jackhuang.hmcl.web.brand.BrandNodeRequirement;
import org.jackhuang.hmcl.web.brand.BrandPolicy;
import org.jackhuang.hmcl.web.brand.BrandRuntime;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/// The relationships the Node page shows: which runtime exists, which major
/// line it belongs to, and who consumes it.
///
/// `NodeApiServlet` used to return only `system` + a flat `managed` list, which
/// made the page look like a downloader: nothing said which instance or brand
/// used a version, so "uninstall" was a blind delete. This class builds the
/// missing edges once per request:
///
/// - DSH instances (`DshInstance.nodeRuntime`, resolved through the launcher
///   default);
/// - brand instances (per-instance selector, falling back to the brand default
///   in `policy.json`, then the launcher default);
/// - ZCode builds, which pin the modern line;
/// - the panel's own install/plugin tasks, which follow `auto`.
///
/// Resolution itself lives in [NodeSelection]; this class never decides what a
/// selector means, it only reports what the resolver answered.
@NotNullByDefault
public final class NodeEnvironment {

    private final ServerConfig config;

    public NodeEnvironment(ServerConfig config) {
        this.config = config;
    }

    /// One thing that runs on Node.js.
    ///
    /// @param kind       `instance`, `brand-instance`, `zcode-build` or `task`
    /// @param brand      the brand id for `brand-instance`, otherwise `null`
    /// @param id         the instance id, or a stable pseudo id
    /// @param selector   the stored selector, in display form
    /// @param resolved   the exact version it resolves to, or `null`
    /// @param source     `system`, `managed`, `channel`, `exact` or `none`
    /// @param compatible whether that version satisfies the declared range
    /// @param running    whether the consumer is running right now
    /// @param requirement the brand's declared `engines.node`, or `null`
    /// @param lastGood   the last version this consumer came up on, or `null`
    public record Consumer(String kind, @Nullable String brand, String id, String selector,
                           @Nullable String resolved, String source, boolean compatible,
                           boolean running, @Nullable String requirement, @Nullable String lastGood) {

        /// The label used on a runtime card, e.g. `kimi/DClDN1J3w0C_`.
        ///
        /// @return the label
        public String label() {
            return brand == null ? id : brand + "/" + id;
        }
    }

    /// One major line, as a card.
    ///
    /// @param name      the major, e.g. `22`
    /// @param major     the same number
    /// @param lts       whether Node publishes this line as LTS (even majors)
    /// @param tracked   the channel pointer, or `null` when it is derived only
    /// @param resolved  the highest installed version in the line, or `null`
    /// @param disabled  whether that version is disabled
    /// @param healthy   whether it can actually run
    /// @param consumers the labels of the consumers using it
    public record Channel(String name, int major, boolean lts, @Nullable String tracked,
                          @Nullable String resolved, boolean disabled, boolean healthy,
                          List<String> consumers, boolean supported) {
    }

    /// One installed distribution.
    ///
    /// @param version    the exact version
    /// @param path       the distribution directory
    /// @param healthy    whether it can actually run
    /// @param disabled   whether it is excluded from automatic resolution
    /// @param pnpm       whether a pnpm shim sits beside it
    /// @param sizeMb     the directory size in MiB, or `-1` when unreadable
    /// @param lastUsedAt the last recorded use, or `null`
    /// @param usedBy     the labels of the consumers resolving to it
    /// @param sha256     the recorded digest, or `null`
    /// @param verified   whether that digest matched a published checksum
    public record Runtime(String version, String path, boolean healthy, boolean disabled,
                          boolean pnpm, long sizeMb, @Nullable String lastUsedAt,
                          List<String> usedBy, @Nullable String sha256, boolean verified,
                          boolean supported) {
    }

    /// Everything the Node page needs for one render.
    ///
    /// @param defaultSelector the launcher-wide default selector
    /// @param channels        one entry per installed major, newest first
    /// @param runtimes        every installed distribution, newest first
    /// @param consumers       every consumer and its resolution
    public record View(String defaultSelector, List<Channel> channels,
                       List<Runtime> runtimes, List<Consumer> consumers) {
    }

    /// Builds the view.
    ///
    /// @return the view, never `null`
    public View view() {
        List<Consumer> consumers = consumers();
        Map<String, List<String>> usedBy = new HashMap<>();
        for (Consumer consumer : consumers) {
            if (consumer.resolved() != null) {
                usedBy.computeIfAbsent(consumer.resolved(), key -> new ArrayList<>())
                        .add(consumer.label());
            }
        }

        List<Runtime> runtimes = new ArrayList<>();
        for (NodeRuntime runtime : NodeRuntimeManager.listInstalled()) {
            runtimes.add(new Runtime(
                    runtime.version(),
                    runtime.directory().toString(),
                    NodeRuntimeManager.runs(runtime),
                    NodeRuntimeManager.isDisabled(runtime.version()),
                    Files.isRegularFile(runtime.pnpm()),
                    sizeMb(runtime.directory()),
                    NodeRuntimeManager.lastUsed(runtime.version()),
                    List.copyOf(usedBy.getOrDefault(runtime.version(), List.of())),
                    NodeRuntimeManager.digest(runtime.version()),
                    NodeRuntimeManager.digestVerified(runtime.version()),
                    DshNodeRuntime.isSupportedNodeVersion(runtime.version())));
        }

        List<Channel> channels = new ArrayList<>();
        Map<Integer, List<Runtime>> byMajor = new LinkedHashMap<>();
        for (Runtime runtime : runtimes) {
            byMajor.computeIfAbsent(major(runtime.version()), key -> new ArrayList<>()).add(runtime);
        }
        // Every line the page can install, whether or not it is on disk: an
        // uninstalled major still needs a card, otherwise "18 to 26" is a list
        // the UI cannot show.
        for (int major = 26; major >= 18; major--) {
            String name = String.valueOf(major);
            List<Runtime> installedOnLine = byMajor.get(major);
            String tracked = NodeChannels.version(name);
            if (installedOnLine == null) {
                channels.add(new Channel(name, major, major % 2 == 0, tracked,
                        null, false, false, List.of(), false));
                continue;
            }
            Runtime chosen = installedOnLine.get(0);
            if (tracked != null) {
                for (Runtime candidate : installedOnLine) {
                    if (candidate.version().equals(tracked)) {
                        chosen = candidate;
                        break;
                    }
                }
            }
            channels.add(new Channel(name, major, major % 2 == 0, tracked,
                    chosen.version(), chosen.disabled(), chosen.healthy(),
                    chosen.usedBy(), chosen.supported()));
        }
        // A future major installed by hand still gets a card.
        for (Map.Entry<Integer, List<Runtime>> entry : byMajor.entrySet()) {
            if (entry.getKey() >= 18 && entry.getKey() <= 26) {
                continue;
            }
            String name = String.valueOf(entry.getKey());
            String tracked = NodeChannels.version(name);
            Runtime chosen = entry.getValue().get(0);
            channels.add(new Channel(name, entry.getKey(), entry.getKey() % 2 == 0, tracked,
                    chosen.version(), chosen.disabled(), chosen.healthy(),
                    chosen.usedBy(), chosen.supported()));
        }

        return new View(defaultSelector(), channels, runtimes, consumers);
    }

    /// Every consumer and the runtime it resolves to.
    ///
    /// @return the consumers, never `null`
    public List<Consumer> consumers() {
        String global = defaultSelector();
        List<Consumer> consumers = new ArrayList<>();

        for (DshInstance instance : DshInstanceManager.list()) {
            NodeSelection.Resolution resolution =
                    NodeSelection.resolve(NodeSelection.parse(instance.nodeRuntime()), global, false);
            consumers.add(new Consumer(
                    "instance", null, instance.id(), display(instance.nodeRuntime()),
                    resolution.version(), source(resolution), resolution.compatible(),
                    DshProcessManager.find(instance.id()).isPresent(),
                    DshNodeRuntime.requirement(),
                    NodeLastGood.version(NodeLastGood.dshKey(instance.id()))));
        }

        BrandPolicy policy = new BrandPolicy(config.dataDir);
        for (Brand brand : Brand.values()) {
            Path root = config.dataDir.resolve("brands").resolve(brand.id()).resolve("instances");
            BrandInstanceManager manager = new BrandInstanceManager(brand, root);
            String brandDefault = policy.get(brand).nodeRuntime();
            String fallback = NodeSelection.parse(brandDefault).isInherit() ? global : brandDefault;
            for (BrandInstance instance : manager.list()) {
                NodeSelection.Resolution resolution =
                        NodeSelection.resolve(NodeSelection.parse(instance.nodeRuntime()), fallback, false);
                String requirement = BrandNodeRequirement.of(config.dataDir, brand, instance.version());
                boolean compatible = resolution.compatible()
                        && (requirement == null || NodeSemver.satisfies(resolution.version(), requirement));
                consumers.add(new Consumer(
                        "brand-instance", brand.id(), instance.id(), display(instance.nodeRuntime()),
                        resolution.version(), source(resolution), compatible,
                        BrandRuntime.isRunning(instance.id()),
                        requirement,
                        NodeLastGood.version(NodeLastGood.brandKey(brand.id(), instance.id()))));
            }
        }

        // ZCode builds pin the modern line; the panel's own install/plugin tasks
        // follow `auto` and need pnpm.
        NodeSelection.Resolution zcode = NodeSelection.resolve(NodeSelection.parse("24"), null, false);
        consumers.add(new Consumer("zcode-build", null, "zcode-build", "24",
                zcode.version(), source(zcode),
                zcode.compatible() && NodeSemver.satisfies(zcode.version(), ">=24"),
                false, ">=24", null));
        NodeSelection.Resolution tasks = NodeSelection.resolve(NodeSelection.parse("auto"), null, true);
        consumers.add(new Consumer("task", null, "panel-tasks", "auto",
                tasks.version(), source(tasks), tasks.compatible(), false,
                DshNodeRuntime.requirement(), null));

        return consumers;
    }

    /// The consumers resolving to one exact version.
    ///
    /// @param version the version
    /// @return the consumers, never `null`
    public List<Consumer> consumersOf(String version) {
        return consumers().stream()
                // The panel's install/plugin tasks are a transient answer, not a
                // pinned consumer: counting them would make the runtime `auto`
                // currently lands on impossible to uninstall.
                .filter(consumer -> !"task".equals(consumer.kind()))
                .filter(consumer -> version.equals(consumer.resolved()))
                .toList();
    }

    /// Whether an exact version is named as a default (global or brand-wide).
    /// Deleting it would leave that default dangling, so the uninstall route
    /// refuses instead of quietly resolving to nothing.
    ///
    /// @param version the exact version
    /// @return whether a default names it
    public boolean isPinnedAsDefault(String version) {
        if (version.equals(defaultSelector())) {
            return true;
        }
        BrandPolicy policy = new BrandPolicy(config.dataDir);
        for (Brand brand : Brand.values()) {
            if (version.equals(policy.get(brand).nodeRuntime())) {
                return true;
            }
        }
        return false;
    }

    /// Whether an exact version is a consumer's last-good anchor.
    ///
    /// @param version the exact version
    /// @return whether any consumer would lose its revert target
    public boolean isLastGood(String version) {
        return NodeLastGood.all().containsValue(version);
    }

    /// Deletes unused runtimes beyond `keepPerMajor` per major.
    ///
    /// In-use, default-pinned and last-good versions are never candidates: a
    /// cleanup that breaks a consumer or a revert anchor is worse than the disk
    /// it saves. Versions are visited newest first, so the ones kept are the
    /// recent ones.
    ///
    /// @param keepPerMajor how many unused versions to keep per major (at least 1)
    /// @return the versions removed
    public List<String> gc(int keepPerMajor) {
        int keep = Math.max(1, keepPerMajor);
        List<Consumer> consumers = consumers();
        java.util.Set<String> inUse = new java.util.HashSet<>();
        for (Consumer consumer : consumers) {
            if (!"task".equals(consumer.kind()) && consumer.resolved() != null) {
                inUse.add(consumer.resolved());
            }
        }
        java.util.Set<String> anchored = new java.util.HashSet<>(NodeLastGood.all().values());
        String defaultSelection = defaultSelector();
        Map<Integer, Integer> kept = new java.util.HashMap<>();
        List<String> removed = new ArrayList<>();

        for (NodeRuntime runtime : NodeRuntimeManager.listInstalled()) {
            String version = runtime.version();
            if (inUse.contains(version)
                    || version.equals(defaultSelection)
                    || anchored.contains(version)
                    || isPinnedAsDefault(version)) {
                continue;
            }
            int major = major(version);
            int count = kept.getOrDefault(major, 0);
            if (count < keep) {
                kept.put(major, count + 1);
                continue;
            }
            try {
                NodeRuntimeManager.uninstall(version);
                removed.add(version);
            } catch (org.jackhuang.hmcl.dsh.DshException e) {
                org.jackhuang.hmcl.util.logging.Logger.LOG.warning(
                        "Could not remove the unused Node.js " + version, e);
            }
        }
        return removed;
    }

    /// Switches a consumer back to the last runtime it came up on.
    ///
    /// @param kind  `instance` or `brand-instance`
    /// @param brand the brand id for `brand-instance`
    /// @param id    the instance id
    /// @return the version it was switched to, or `null` when there is no anchor
    /// @throws org.jackhuang.hmcl.dsh.DshException when the DSH instance cannot be updated
    /// @throws org.jackhuang.hmcl.web.brand.BrandException when the brand instance cannot be updated
    public @Nullable String revert(String kind, @Nullable String brand, String id)
            throws org.jackhuang.hmcl.dsh.DshException, org.jackhuang.hmcl.web.brand.BrandException {
        boolean dsh = "instance".equals(kind);
        String key = dsh ? NodeLastGood.dshKey(id) : NodeLastGood.brandKey(brand, id);
        String version = NodeLastGood.version(key);
        if (version == null) {
            return null;
        }
        if (dsh) {
            DshInstance instance = DshInstanceManager.find(id);
            if (instance == null) {
                return null;
            }
            DshInstanceManager.update(instance.withNodeRuntime(version));
        } else {
            Brand parsed = Brand.fromId(brand);
            if (parsed == null) {
                return null;
            }
            BrandInstanceManager manager = new BrandInstanceManager(parsed,
                    config.dataDir.resolve("brands").resolve(parsed.id()).resolve("instances"));
            BrandInstance instance = manager.find(id);
            if (instance == null) {
                return null;
            }
            manager.update(instance.withNodeRuntime(version));
        }
        return version;
    }

    /// A selector in the form the UI shows it.
    private static String display(@Nullable String raw) {
        return raw == null || raw.isBlank() ? "inherit" : raw;
    }

    private static String source(NodeSelection.Resolution resolution) {
        return resolution.source().name().toLowerCase(Locale.ROOT);
    }

    private static int major(String version) {
        int dot = version.indexOf('.');
        try {
            return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /// The distribution size in MiB, or `-1` when the tree cannot be walked.
    private static long sizeMb(Path root) {
        try (Stream<Path> entries = Files.walk(root)) {
            long bytes = entries.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
            return bytes / (1024L * 1024L);
        } catch (IOException e) {
            return -1;
        }
    }

    /// A tiny helper for the servlet: the view's `default` as a JSON-friendly
    /// value. Kept here so the servlet does not need `SettingsManager`.
    ///
    /// @return the default selector
    public String defaultSelector() {
        String value = SettingsManager.settings().defaultNodeRuntime();
        return value == null || value.isBlank() ? "auto" : value;
    }

    /// A placeholder so a future `resolve` endpoint can pass a consumer's
    /// fallback without rebuilding this class.
    ///
    /// @param brand the brand id, or `null` for DSH/global
    /// @return the fallback selector
    public @Nullable String fallbackFor(@Nullable String brand) {
        if (brand == null) {
            return defaultSelector();
        }
        Brand parsed = Brand.fromId(brand);
        if (parsed == null) {
            return defaultSelector();
        }
        String brandDefault = new BrandPolicy(config.dataDir).get(parsed).nodeRuntime();
        return NodeSelection.parse(brandDefault).isInherit()
                ? defaultSelector()
                : brandDefault;
    }

    /// A JSON form of the view, so the servlet does not duplicate the shape.
    ///
    /// @return the response body
    public JsonObject toJson() {
        JsonObject body = new JsonObject();
        View view = view();
        body.addProperty("default", view.defaultSelector());
        body.addProperty("requirement", DshNodeRuntime.requirement());

        com.google.gson.JsonArray channels = new com.google.gson.JsonArray();
        for (Channel channel : view.channels()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", channel.name());
            entry.addProperty("major", channel.major());
            entry.addProperty("lts", channel.lts());
            if (channel.tracked() == null) {
                entry.add("tracked", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("tracked", channel.tracked());
            }
            if (channel.resolved() == null) {
                entry.add("resolved", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("resolved", channel.resolved());
            }
            entry.addProperty("installed", channel.resolved() != null);
            entry.addProperty("disabled", channel.disabled());
            entry.addProperty("healthy", channel.healthy());
            entry.addProperty("supported", channel.supported());
            com.google.gson.JsonArray used = new com.google.gson.JsonArray();
            channel.consumers().forEach(used::add);
            entry.add("consumers", used);
            channels.add(entry);
        }
        body.add("channels", channels);

        com.google.gson.JsonArray runtimes = new com.google.gson.JsonArray();
        for (Runtime runtime : view.runtimes()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("version", runtime.version());
            entry.addProperty("path", runtime.path());
            entry.addProperty("healthy", runtime.healthy());
            entry.addProperty("disabled", runtime.disabled());
            entry.addProperty("pnpm", runtime.pnpm());
            entry.addProperty("sizeMb", runtime.sizeMb());
            if (runtime.sha256() == null) {
                entry.add("sha256", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("sha256", runtime.sha256());
            }
            entry.addProperty("verified", runtime.verified());
            entry.addProperty("supported", runtime.supported());
            if (runtime.lastUsedAt() == null) {
                entry.add("lastUsedAt", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("lastUsedAt", runtime.lastUsedAt());
            }
            com.google.gson.JsonArray used = new com.google.gson.JsonArray();
            runtime.usedBy().forEach(used::add);
            entry.add("usedBy", used);
            runtimes.add(entry);
        }
        body.add("runtimes", runtimes);

        com.google.gson.JsonArray consumers = new com.google.gson.JsonArray();
        for (Consumer consumer : view.consumers()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("kind", consumer.kind());
            if (consumer.brand() == null) {
                entry.add("brand", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("brand", consumer.brand());
            }
            entry.addProperty("id", consumer.id());
            entry.addProperty("selector", consumer.selector());
            if (consumer.resolved() == null) {
                entry.add("resolved", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("resolved", consumer.resolved());
            }
            entry.addProperty("source", consumer.source());
            entry.addProperty("compatible", consumer.compatible());
            entry.addProperty("running", consumer.running());
            if (consumer.requirement() == null) {
                entry.add("requirement", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("requirement", consumer.requirement());
            }
            if (consumer.lastGood() == null) {
                entry.add("lastGood", com.google.gson.JsonNull.INSTANCE);
            } else {
                entry.addProperty("lastGood", consumer.lastGood());
            }
            consumers.add(entry);
        }
        body.add("consumers", consumers);
        return body;
    }
}
