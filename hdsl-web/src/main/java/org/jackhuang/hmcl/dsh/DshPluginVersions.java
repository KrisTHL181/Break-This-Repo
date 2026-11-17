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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// A plugin's published versions, read from the registry's document and arranged the way the page draws them.
///
/// The original groups a mod's files by the game version each one supports and marks each with its
/// release channel. This is the same arrangement for plugins, where the game version's place is taken by
/// **the DeepSeek Harness version**: a plugin is installed into a harness, and a version of it that was
/// written for another harness is the thing worth seeing before it is installed rather than after.
///
/// What a plugin says about the harness is its `@deepseek-ai/dsh*` peer dependencies, which is also what
/// the harness itself judges an installation by. There are three answers and the third one is why this is
/// worth being careful about: a version that declares **nothing** about the harness is not a version that
/// fits — it is a version that did not say, and it goes into a group of its own rather than being counted
/// as either. Nothing here hides a version: the page shows all of them, because the harness has an
/// exemption for a version that does not fit, which means "does not fit" is a fact to show rather than a
/// reason to take something away.
@NotNullByDefault
public final class DshPluginVersions {
    /// The prefix the harness's own packages share.
    private static final String HARNESS_PREFIX = "@deepseek-ai/dsh";

    /// A version named inside a range: `^0.1.0-rc.7`, `0.2.0-rc.1`, and the like.
    private static final Pattern CLAIMED = Pattern.compile("\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?");

    /// The channel a published version belongs to.
    ///
    /// Read from the version's own name first, and from the package's dist-tags for a version that does
    /// not say: a pre-release suffix is the author writing the channel into the name, and a dist-tag is
    /// the author pointing at it afterwards.
    public enum Channel {
        /// No pre-release suffix, and nothing but the release line.
        STABLE,

        /// A release candidate.
        RC,

        /// A beta.
        BETA,

        /// An alpha.
        ALPHA,

        /// A nightly or something else that only runs on trust.
        OTHER
    }

    /// What one version says about the harness an instance runs.
    public enum Fit {
        /// It declares the harness's packages, and everything it declares is satisfied.
        FITS,

        /// It declares the harness's packages, and something it declares is not satisfied.
        NOT_FITS,

        /// It declares nothing about the harness, so nothing can be judged.
        UNCLAIMED
    }

    /// One published version.
    ///
    /// @param version       the version
    /// @param channel       the channel it belongs to
    /// @param published     when it was published, or `null` when the registry did not say
    /// @param harnessPeers  what it declares about the harness's own packages
    /// @param claimed       the harness versions the declaration names, newest first
    public record Published(String version, Channel channel, @Nullable Instant published,
                            JsonObject harnessPeers, List<String> claimed) {
    }

    /// A run of versions shown under one heading.
    ///
    /// @param harnessVersion the harness version the group is about, or `null` for the group of
    ///                       versions that said nothing
    /// @param recommended    whether this is the group that fits the instance the page is for
    /// @param versions       the versions, newest first
    public record Group(@Nullable String harnessVersion, boolean recommended, List<Published> versions) {
    }

    private DshPluginVersions() {
    }

    /// Reads a registry document into the versions it describes, newest first.
    ///
    /// @param packument the document
    /// @return the versions, or an empty list when the document describes none
    public static List<Published> read(JsonObject packument) {
        JsonObject versions = object(packument, "versions");
        if (versions == null) {
            return List.of();
        }
        JsonObject times = object(packument, "time");
        JsonObject tags = object(packument, "dist-tags");

        List<Published> published = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : versions.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject manifest = entry.getValue().getAsJsonObject();
            String version = manifest.has("version") && manifest.get("version").isJsonPrimitive()
                    ? manifest.get("version").getAsString() : entry.getKey();
            if (version.isBlank()) {
                continue;
            }
            JsonObject peers = harnessPeers(object(manifest, "peerDependencies"));
            published.add(new Published(version, channelOf(version, tags), moment(times, version),
                    peers, claimedOf(peers)));
        }

        published.sort(Comparator.comparing(Published::published,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return List.copyOf(published);
    }

    /// Arranges versions into the groups the page draws.
    ///
    /// The groups are the harness versions the versions themselves name — one list each — with the
    /// instance's own harness version in front, because that is what the page was opened for.
    ///
    /// A version appears under **every** harness version it admits, which is the original's own
    /// arrangement: a mod file that lists three game versions is listed under all three, because that
    /// is what it says it runs on. A version that named nothing is in a group of its own at the end,
    /// and is never counted as fitting anything.
    ///
    /// @param versions       the versions
    /// @param harnessVersion the harness version the page is for, or `null` when there is none
    /// @return the groups, which are never empty unless nothing was published at all
    public static List<Group> group(List<Published> versions, @Nullable String harnessVersion) {
        List<String> panel = harnessVersions(versions, harnessVersion);
        Map<String, List<Published>> byHarness = new LinkedHashMap<>();
        for (String target : panel) {
            byHarness.put(target, new ArrayList<>());
        }
        List<Published> unclaimed = new ArrayList<>();

        for (Published version : versions) {
            if (version.harnessPeers().isEmpty()) {
                unclaimed.add(version);
                continue;
            }
            boolean listed = false;
            for (String target : panel) {
                if (admits(version, target)) {
                    byHarness.get(target).add(version);
                    listed = true;
                }
            }
            if (!listed) {
                // It declared something that names no harness version this page can list — a bare
                // `*`, or a `workspace:` range — so it stands with the versions that named nothing
                // rather than being dropped.
                unclaimed.add(version);
            }
        }

        List<Group> groups = new ArrayList<>();
        for (String target : panel) {
            List<Published> members = byHarness.get(target);
            if (members.isEmpty()) {
                // A group with nothing under it is a heading and a number: the panel holds a version
                // because something named it, and that something admits it by construction.
                continue;
            }
            groups.add(new Group(target, target.equals(harnessVersion), List.copyOf(members)));
        }
        if (!unclaimed.isEmpty()) {
            groups.add(new Group(null, false, List.copyOf(unclaimed)));
        }
        return List.copyOf(groups);
    }

    /// Reports whether a version says it runs on a harness version.
    ///
    /// The declaration is read against the harness version itself, which is the rule the harness
    /// applies to a plugin it is installing: its packages are released at its own version, so a range
    /// over them is a range over it.
    ///
    /// @param version        the version
    /// @param harnessVersion the harness version
    /// @return whether it admits it
    static boolean admits(Published version, String harnessVersion) {
        for (Map.Entry<String, JsonElement> entry : version.harnessPeers().entrySet()) {
            if (!DshPluginRequirements.satisfies(entry.getValue().getAsString(), harnessVersion)) {
                return false;
            }
        }
        return !version.harnessPeers().isEmpty();
    }

    /// Returns the harness versions the page draws groups for, the instance's own first.
    ///
    /// @param versions       the published versions
    /// @param harnessVersion the harness version the page is for, or `null`
    /// @return the versions, newest first, with the instance's own in front of them all
    private static List<String> harnessVersions(List<Published> versions, @Nullable String harnessVersion) {
        Set<String> named = new LinkedHashSet<>();
        for (Published version : versions) {
            named.addAll(version.claimed());
        }
        boolean asked = harnessVersion != null && !harnessVersion.isBlank();
        if (asked) {
            named.add(harnessVersion);
        }

        List<String> panel = new ArrayList<>(named);
        panel.sort((left, right) -> DshVersionManager.compareVersions(right, left));
        if (asked) {
            panel.remove(harnessVersion);
            panel.add(0, harnessVersion);
        }
        return panel;
    }

    /// Reports what one version says about the harness an instance runs.
    ///
    /// The judgement is the harness's own, on the data it makes it from. What a plugin declares is a
    /// range over the harness's packages, and those are released at the harness's own version, so the
    /// range can be read against either: against the packages an instance actually holds, which is what
    /// [DshPluginRequirements#fits] does, or — when those could not be read — against the harness version
    /// itself, which is the rule the harness applies to a plugin it is installing.
    ///
    /// @param version        the version
    /// @param harnessVersion the harness version, or `null` when there is none to judge against
    /// @param harnessCore    the harness packages that version holds, or `null` when they could not be read
    /// @return what it says
    public static Fit fitOf(Published version, @Nullable String harnessVersion,
                            @Nullable Map<String, String> harnessCore) {
        if (version.harnessPeers().isEmpty()) {
            return Fit.UNCLAIMED;
        }
        if (harnessCore != null) {
            return DshPluginRequirements.fits(version.harnessPeers(), harnessCore) ? Fit.FITS : Fit.NOT_FITS;
        }
        if (harnessVersion == null || harnessVersion.isBlank()) {
            // Nothing to judge against: the declaration is on the record, but it is not an answer.
            return Fit.UNCLAIMED;
        }
        return admits(version, harnessVersion) ? Fit.FITS : Fit.NOT_FITS;
    }

    /// Returns the channel a version belongs to.
    ///
    /// @param version the version
    /// @param tags    the package's dist-tags, or `null`
    /// @return the channel
    public static Channel channelOf(String version, @Nullable JsonObject tags) {
        int dash = version.indexOf('-');
        if (dash >= 0) {
            // The author wrote the channel into the name, which is the first thing to believe.
            String pre = version.substring(dash + 1).toLowerCase(Locale.ROOT);
            if (pre.startsWith("rc")) {
                return Channel.RC;
            }
            if (pre.startsWith("beta")) {
                return Channel.BETA;
            }
            if (pre.startsWith("alpha")) {
                return Channel.ALPHA;
            }
            return Channel.OTHER;
        }
        if (tags != null) {
            for (Map.Entry<String, JsonElement> tag : tags.entrySet()) {
                if (!tag.getValue().isJsonPrimitive() || !version.equals(tag.getValue().getAsString())) {
                    continue;
                }
                switch (tag.getKey().toLowerCase(Locale.ROOT)) {
                    case "rc" -> {
                        return Channel.RC;
                    }
                    case "beta" -> {
                        return Channel.BETA;
                    }
                    case "alpha" -> {
                        return Channel.ALPHA;
                    }
                    case "dev", "next", "canary", "snapshot" -> {
                        return Channel.OTHER;
                    }
                    default -> {
                        // `latest` says what a bare install would take, which for a version with no
                        // pre-release suffix is what the release line already means.
                    }
                }
            }
        }
        return Channel.STABLE;
    }

    /// Returns what a manifest declares about the harness's own packages.
    ///
    /// Only the harness's packages: `@deepseek-ai/cordis` and `@deepseek-ai/schemastery` are the
    /// framework's, shared by everything, and a version that names only those has said nothing about
    /// which harness it runs on.
    ///
    /// @param peers the manifest's peer dependencies, or `null`
    /// @return the declaration
    private static JsonObject harnessPeers(@Nullable JsonObject peers) {
        JsonObject harness = new JsonObject();
        if (peers == null) {
            return harness;
        }
        for (Map.Entry<String, JsonElement> entry : peers.entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                continue;
            }
            String name = entry.getKey();
            if (name.equals(HARNESS_PREFIX) || name.startsWith(HARNESS_PREFIX + "-")) {
                harness.addProperty(name, entry.getValue().getAsString());
            }
        }
        return harness;
    }

    /// Returns the harness versions a declaration names, newest first.
    ///
    /// A declaration is a range — `^0.1.0-rc.7 || ^0.1.1-rc.2 || ^0.2.0-rc.1` — and the versions inside
    /// it are what the author was writing against. They are read rather than interpreted: a range is
    /// judged by comparing it with what an instance holds, which is [DshPluginRequirements]'s business.
    ///
    /// @param peers the declaration
    /// @return the versions
    private static List<String> claimedOf(JsonObject peers) {
        Set<String> claimed = new LinkedHashSet<>();
        for (Map.Entry<String, JsonElement> entry : peers.entrySet()) {
            Matcher matcher = CLAIMED.matcher(entry.getValue().getAsString());
            while (matcher.find()) {
                claimed.add(matcher.group());
            }
        }
        List<String> versions = new ArrayList<>(claimed);
        versions.sort((left, right) -> DshVersionManager.compareVersions(right, left));
        return List.copyOf(versions);
    }

    /// Reads an object member.
    ///
    /// @param object the object
    /// @param name   the member
    /// @return the object, or `null` when it is absent or is not an object
    private static @Nullable JsonObject object(@Nullable JsonObject object, String name) {
        if (object == null || !object.has(name) || !object.get(name).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(name);
    }

    /// Reads the moment one version was published.
    ///
    /// @param times   the document's `time` member, or `null`
    /// @param version the version
    /// @return the moment, or `null` when the document did not say
    private static @Nullable Instant moment(@Nullable JsonObject times, String version) {
        if (times == null || !times.has(version) || !times.get(version).isJsonPrimitive()) {
            return null;
        }
        try {
            return Instant.parse(times.get(version).getAsString());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
