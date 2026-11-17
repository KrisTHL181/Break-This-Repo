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

import org.jackhuang.hmcl.dsh.DshException;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshPackageRegistry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Works out whether a plugin fits the harness an instance runs.
///
/// A plugin declares what it needs in its `peerDependencies`, and those are the
/// harness's own packages — a marketplace plugin asks for
/// `@deepseek-ai/dsh-settings: ^0.1.0-rc.7 || ^0.1.1-rc.2 || ^0.1.2-alpha.2`. The
/// harness an instance runs ships exact versions of exactly those packages, so the
/// two can be compared without asking anybody: what the instance has is read from its
/// own installation, and what the plugin wants is what it published.
///
/// This decides *what is shown* and nothing else. A plugin that does not fit an
/// instance is still installable, because the launcher is not the one who knows
/// better — a person may be installing it to then move the instance, or may know
/// something the ranges do not say.
@NotNullByDefault
public final class DshPluginRequirements {
    /// The prefix the harness's own packages share.
    private static final String CORE_PREFIX = "@deepseek-ai/";

    /// The harness's own package: the name plugins declare most, and the one every range is
    /// read against.
    private static final String HARNESS = CORE_PREFIX + "dsh";

    /// The ranges that mean "whatever runtime this is installed into".
    private static final List<String> WORKSPACE = List.of("workspace:^", "workspace:~", "workspace:*");

    /// The dash between the two ends of a hyphen range, npm's \`1.2.3 - 2.3.4\`.
    private static final java.util.regex.Pattern HYPHEN = java.util.regex.Pattern.compile("\\s+-\\s+");

    private DshPluginRequirements() {
    }

    /// Reads the versions of the harness's packages that a published version ships.
    ///
    /// Asked of the registry, once per version and remembered, for the case the picker
    /// offers a version the machine does not have: what that release can run is what it
    /// declares it depends on.
    ///
    /// @param version the harness version
    /// @return the package versions, empty when they cannot be read
    public static Map<String, String> coreVersionsOf(String version) {
        Map<String, String> cached = PUBLISHED_CORE_VERSIONS.get(version);
        if (cached != null) {
            return cached;
        }

        Map<String, String> versions = new java.util.LinkedHashMap<>();
        // The harness's own package does not depend on itself, so it is never among the
        // dependencies below — and it is the name plugins declare most.
        versions.put(HARNESS, version);
        JsonObject dependencies = DshPackageRegistry.dependencies(HARNESS, version);
        for (Map.Entry<String, JsonElement> entry : dependencies.entrySet()) {
            if (entry.getKey().startsWith(CORE_PREFIX) && entry.getValue().isJsonPrimitive()) {
                versions.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        Map<String, String> result = Map.copyOf(versions);
        PUBLISHED_CORE_VERSIONS.put(version, result);
        return result;
    }

    /// The packages read so far, by harness version.
    private static final Map<String, Map<String, String>> PUBLISHED_CORE_VERSIONS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /// Reads the versions of the harness's packages that an instance holds.
    ///
    /// Read from the instance's own installation rather than from the registry: what an
    /// instance can run is what it has, and it has an exact set of these packages. The
    /// answer is remembered per instance version, because it cannot change while the
    /// instance does not.
    ///
    /// @param instance the instance
    /// @return the package versions, empty when they cannot be read
    public static Map<String, String> coreVersions(DshInstance instance) {
        String key = instance.id() + "@" + instance.version();
        Map<String, String> cached = CORE_VERSIONS.get(key);
        if (cached != null) {
            return cached;
        }

        Map<String, String> versions = Map.of();
        try {
            versions = readCoreVersions(instance.dshDirectory()
                    .resolve("node_modules").resolve("@deepseek-ai").resolve("dsh")
                    .resolve("package.json"));
        } catch (DshException | java.io.IOException | RuntimeException e) {
            LOG.warning("Could not read the packages of " + instance.id(), e);
        }

        CORE_VERSIONS.put(key, versions);
        return versions;
    }

    /// Reads the versions of the harness's packages that an installation holds.
    ///
    /// @param manifest the manifest of the harness's own package in that installation
    /// @return the versions, by package name, empty when there is no installation to read
    /// @throws java.io.IOException when the manifest is there and cannot be read
    static Map<String, String> readCoreVersions(java.nio.file.Path manifest) throws java.io.IOException {
        if (!java.nio.file.Files.isRegularFile(manifest)) {
            return Map.of();
        }
        JsonObject root = org.jackhuang.hmcl.util.gson.JsonUtils.fromJsonFile(manifest, JsonObject.class);
        if (root == null) {
            return Map.of();
        }

        Map<String, String> versions = new java.util.LinkedHashMap<>();
        if (root.has("version") && root.get("version").isJsonPrimitive()) {
            // This package does not depend on itself, so the loop below never sees it, yet
            // it is the name plugins declare most and the version the harness reads a range
            // against: one plugin asking for `>=0.1.7-rc.2` is asking about exactly this.
            versions.put(HARNESS, root.get("version").getAsString());
        }
        if (root.has("dependencies") && root.get("dependencies").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("dependencies").entrySet()) {
                if (entry.getKey().startsWith(CORE_PREFIX) && entry.getValue().isJsonPrimitive()) {
                    versions.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        return Map.copyOf(versions);
    }

    /// The versions read so far, per instance and version.
    private static final Map<String, Map<String, String>> CORE_VERSIONS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /// Reports whether a plugin fits the versions an instance holds.
    ///
    /// @param peerDependencies the plugin's peer dependencies, or `null`
    /// @param coreVersions     the versions of the harness's packages the instance has
    /// @return whether every requirement that can be judged is satisfied
    public static boolean fits(@Nullable JsonObject peerDependencies, Map<String, String> coreVersions) {
        if (peerDependencies == null) {
            return true;
        }
        for (Map.Entry<String, JsonElement> entry : peerDependencies.entrySet()) {
            if (!entry.getKey().startsWith(CORE_PREFIX) || !entry.getValue().isJsonPrimitive()) {
                continue;
            }
            String held = coreVersions.get(entry.getKey());
            if (held == null) {
                // A package the instance does not have says nothing about whether the
                // plugin fits: it may be one the harness folds into another, and
                // guessing would hide plugins for no reason.
                continue;
            }
            if (!satisfies(entry.getValue().getAsString(), held)) {
                return false;
            }
        }
        return true;
    }

    /// Reports whether a version satisfies an npm range.
    ///
    /// The ranges plugins publish are the ones a package manager writes: alternatives
    /// separated by `||`, each a version, a caret or tilde range, or a comparison.
    ///
    /// The rules are npm's, and they are applied the way the harness applies them: before it
    /// installs a plugin it asks npm's own `semver` with `includePrerelease`, so a prerelease
    /// is an ordinary version here too. That is what makes `>=0.1.7-rc.2` refuse `0.1.7-rc.1`
    /// — the two agree on every number and differ only in the part that tells them apart —
    /// and what keeps `<0.2.0` from refusing one merely for being a prerelease.
    ///
    /// Two answers are the harness's own rather than npm's: `workspace:^`, `workspace:~` and
    /// `workspace:*` mean "the runtime this is installed into", so whatever is asking
    /// satisfies them, and an empty requirement is refused outright, which npm reads as `*`.
    ///
    /// @param range   the range
    /// @param version the version
    /// @return whether it satisfies
    public static boolean satisfies(String range, String version) {
        if (range == null || version == null || range.isBlank() || version.isBlank()) {
            return false;
        }

        String asked = range.trim();
        if (WORKSPACE.contains(asked)) {
            return true;
        }

        String held = version.trim();
        for (String alternative : asked.split("\\|\\|", -1)) {
            if (satisfiesAlternative(alternative.trim(), held)) {
                return true;
            }
        }
        return false;
    }

    /// Reports whether a version satisfies one alternative of a range.
    ///
    /// An alternative is comparators separated by spaces, and every one of them has to hold:
    /// npm writes a conjunction that way, `>=0.1.0-rc.0 <0.2.0`.
    ///
    /// @param alternative the alternative
    /// @param version     the version
    /// @return whether it satisfies
    private static boolean satisfiesAlternative(String alternative, String version) {
        if (alternative.isEmpty()) {
            // An empty alternative is every version: npm reads `>=0.1.0 ||` as `*`.
            return true;
        }
        java.util.regex.Matcher dash = HYPHEN.matcher(alternative);
        if (!dash.find()) {
            for (String comparator : comparatorsOf(alternative)) {
                if (!satisfiesComparator(comparator, version)) {
                    return false;
                }
            }
            return true;
        }
        return satisfiesHyphen(alternative.substring(0, dash.start()).trim(),
                alternative.substring(dash.end()).trim(), version);
    }

    /// Splits an alternative into comparators, putting an operator back together with its version.
    ///
    /// npm allows whitespace between the two — \`>= 1.0.0\`, \`<=  2.0.0\`, \`~ 1.0\` — and writing a range
    /// that way is ordinary. Split on whitespace alone, the operator is left as a comparator of its
    /// own, nothing satisfies it, and a plugin whose range is written that way is hidden from every
    /// instance.
    ///
    /// @param alternative the alternative
    /// @return its comparators, the operator of each joined to its version
    private static List<String> comparatorsOf(String alternative) {
        List<String> comparators = new ArrayList<>();
        String operator = null;
        for (String token : alternative.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (operator != null) {
                comparators.add(operator + token);
                operator = null;
            } else if (isOperatorAlone(token)) {
                operator = token;
            } else {
                comparators.add(token);
            }
        }
        if (operator != null) {
            // An operator with nothing after it names no version, which satisfies nothing.
            comparators.add(operator);
        }
        return comparators;
    }

    /// Reports whether a comparator is an operator and nothing else.
    ///
    /// @param token the comparator
    /// @return whether it names no version
    private static boolean isOperatorAlone(String token) {
        for (int i = 0; i < token.length(); i++) {
            if ("^~<>=vV".indexOf(token.charAt(i)) < 0) {
                return false;
            }
        }
        return !token.isEmpty();
    }

    /// Reports whether a version lies between the two ends of a hyphen range.
    ///
    /// npm reads `1.2.3 - 2.3.4` as `>=1.2.3-0 <2.3.5-0`: one end is a floor and the other a
    /// ceiling, and each is moved out by the smallest step there is — the floor down to a
    /// prerelease of itself, the ceiling up to the next patch, or to the prerelease it names
    /// when it names one, which is then a version of its own and is let in.
    ///
    /// @param from    the floor
    /// @param to      the ceiling
    /// @param version the version
    /// @return whether it lies between them
    private static boolean satisfiesHyphen(String from, String to, String version) {
        if (!isBound(from) || !isBound(to)) {
            return false;
        }
        int floors = countOf(from);
        int ceilings = countOf(to);
        if (floors == 0 && ceilings == 0) {
            // `x - x` names nothing at either end, which npm cannot read either.
            return false;
        }

        Parsed held = parse(version);
        if (floors > 0) {
            Parsed floor = parse(from);
            if (floors < 3 || floor.prerelease() == null) {
                int[] named = floor.numbers();
                floor = release(named[0], named[1], named[2]);
            }
            if (compare(held, floor) < 0) {
                return false;
            }
        }
        if (ceilings == 0) {
            return true;
        }

        Parsed ceiling = parse(to);
        if (ceilings < 3) {
            return compare(held, upperBound(false, ceilings, ceiling.numbers())) < 0;
        }
        if (ceiling.prerelease() != null) {
            return compare(held, ceiling) <= 0;
        }
        int[] named = ceiling.numbers();
        return compare(held, release(named[0], named[1], named[2] + 1)) < 0;
    }

    /// Reports whether a version satisfies one comparator.
    ///
    /// A caret holds from the version it names up to the next change of the leftmost non-zero
    /// part it names, and a tilde up to the next minor. A version that stops short of naming
    /// all three numbers is read the way npm reads one: `^0.1` and `~1.2` are ranges, and a
    /// bare `0.1` is the range up to the next minor. `*` is every version.
    ///
    /// @param comparator the comparator
    /// @param version    the version
    /// @return whether it satisfies
    private static boolean satisfiesComparator(String comparator, String version) {
        if (comparator.isEmpty()) {
            return false;
        }
        if (comparator.startsWith("~>")) {
            // npm's older spelling of `~`, which it still reads and a range may still be written in.
            comparator = "~" + comparator.substring(2).trim();
        }
        if (isWildcard(comparator)) {
            return true;
        }

        char first = comparator.charAt(0);
        if (first == '^' || first == '~') {
            String base = comparator.substring(1).trim();
            if (isWildcard(base)) {
                // `^*` and `~*` are every version, npm's reading of a range that names none.
                return true;
            }
            if (!isBound(base)) {
                return false;
            }
            int count = countOf(base);
            if (count == 0) {
                return false;
            }
            int[] named = parse(base).numbers();
            // A range that leaves a number out starts at a prerelease of the version it does
            // name — npm's `-0`, as in `^1.2` holding `1.2.0-rc.1`, which `>=1.2.0` does not.
            Parsed lower = count == 3 ? parse(base) : release(named[0], named[1], named[2]);
            return within(version, lower, upperBound(first == '^', count, named));
        }

        if (first == '>' || first == '<' || first == '=') {
            String operator = comparator.startsWith(">=") || comparator.startsWith("<=")
                    ? comparator.substring(0, 2) : comparator.substring(0, 1);
            String base = comparator.substring(operator.length()).trim();
            if (isWildcard(base)) {
                // npm reads `>=*` as every version and `>*` as `<0.0.0-0`, which is none.
                return !operator.equals(">") && !operator.equals("<");
            }
            if (!isBound(base)) {
                return false;
            }
            int count = countOf(base);
            if (count == 0) {
                return false;
            }
            if (count < 3) {
                // A bound that does not name all three numbers is about a line rather than about a
                // version, which is how npm reads both a number left out and an `x` written where
                // one belongs: `<=0.7.x` is everything in the 0.7 line, and `>1.2` is everything
                // past the 1.2 line — not `>1.2.0`, which would hold 1.2.8.
                int[] named = parse(base).numbers();
                int above = compare(parse(version), plain(named[0], named[1], named[2]));
                int below = compare(parse(version), upperBound(false, count, named));
                return switch (operator) {
                    case ">=" -> above >= 0;
                    case ">" -> below >= 0;
                    case "<" -> compare(parse(version),
                            release(named[0], named[1], named[2])) < 0;
                    case "<=" -> below < 0;
                    default -> above >= 0 && below < 0;
                };
            }
            int order = compare(parse(version), parse(base));
            return switch (operator) {
                case ">=" -> order >= 0;
                case "<=" -> order <= 0;
                case ">" -> order > 0;
                case "<" -> order < 0;
                default -> order == 0;
            };
        }

        if (!isBound(comparator)) {
            // Nothing a version could be, so nothing satisfies it — which is npm's answer for
            // `latest`, and for anything else it cannot read as a range.
            return false;
        }
        int count = countOf(comparator);
        if (count == 0) {
            return false;
        }
        int[] named = parse(comparator).numbers();
        return count == 3
                ? compare(parse(version), parse(comparator)) == 0
                : within(version, release(named[0], named[1], named[2]),
                        upperBound(false, count, named));
    }

    /// Reports whether a comparator names no version at all.
    ///
    /// @param text the comparator
    /// @return whether it is `*`, `x` or `X`
    private static boolean isWildcard(String text) {
        return text.equals("*") || text.equals("x") || text.equals("X");
    }

    /// Reports whether a version is one npm could read.
    ///
    /// A range may leave a number out and may write an `x` where one belongs, but a fourth
    /// number, a stray dash or a word where a number belongs is not a range at all: npm
    /// answers nothing for one, and reading `1.2.3.4` as `1.2.3` would let a plugin that
    /// declares it look like it fits when the harness is certain to refuse it.
    ///
    /// @param version the version
    /// @return whether it could be one
    private static boolean isBound(String version) {
        String clean = compared(version);
        String[] halves = clean.split("-", 2);
        String[] written = halves[0].split("\\.", -1);
        if (written.length > 3) {
            return false;
        }
        for (String part : written) {
            if (!isNumber(part) && !isWildcard(part)) {
                return false;
            }
        }
        if (halves.length == 1) {
            return true;
        }
        if (halves[1].isEmpty()) {
            return false;
        }
        for (String identifier : halves[1].split("\\.", -1)) {
            if (identifier.isEmpty() || !isIdentifier(identifier)) {
                return false;
            }
        }
        return true;
    }

    /// Reports whether a string is one npm allows among a prerelease's identifiers.
    ///
    /// @param identifier the identifier
    /// @return whether it is letters, numbers and dashes
    private static boolean isIdentifier(String identifier) {
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (!(c >= '0' && c <= '9') && !(c >= 'a' && c <= 'z')
                    && !(c >= 'A' && c <= 'Z') && c != '-') {
                return false;
            }
        }
        return true;
    }

    /// Reports whether a version lies inside the half-open range a comparator draws.
    ///
    /// @param version the version
    /// @param lower   the first version inside it
    /// @param upper   the first version past it
    /// @return whether it is inside
    private static boolean within(String version, Parsed lower, Parsed upper) {
        Parsed held = parse(version);
        return compare(held, lower) >= 0 && compare(held, upper) < 0;
    }

    /// Returns the first version past the ones a caret or tilde range holds.
    ///
    /// The bound names the prerelease `0`, npm's way of writing the first version outside
    /// rather than the last one inside: `^0.1.7-rc.2` is `>=0.1.7-rc.2 <0.2.0-0`, which keeps
    /// `0.2.0-rc.1` out while `0.2.0` is simply past it.
    ///
    /// @param caret whether the range is a caret
    /// @param count how many numbers the range named, one to three
    /// @param named the numbers it named, the ones it left out read as zero
    /// @return the version the range stops at
    private static Parsed upperBound(boolean caret, int count, int[] named) {
        if (!caret) {
            return count >= 2 ? release(named[0], named[1] + 1, 0) : release(named[0] + 1, 0, 0);
        }
        if (count == 1 || named[0] > 0) {
            return release(named[0] + 1, 0, 0);
        }
        if (count == 2 || named[1] > 0) {
            return release(named[0], named[1] + 1, 0);
        }
        return release(named[0], named[1], named[2] + 1);
    }

    /// Returns a version with no pre-release part, which is where a line of versions starts.
    ///
    /// npm moves the ends of a range out to a pre-release of themselves — that is what
    /// [#release] writes — and where it does not, the version has to be the release itself to be
    /// inside: the floor of `1.2.x` is `>=1.2.0`, so `1.2.0-rc` is under it.
    ///
    /// @param major the major
    /// @param minor the minor
    /// @param patch the patch
    /// @return the version
    private static Parsed plain(int major, int minor, int patch) {
        return new Parsed(new int[]{major, minor, patch}, null);
    }

    /// Returns a version with the prerelease npm puts at the foot of a bound it made up.
    ///
    /// @param major the major
    /// @param minor the minor
    /// @param patch the patch
    /// @return the version, as in `0.2.0-0`
    private static Parsed release(int major, int minor, int patch) {
        return new Parsed(new int[]{major, minor, patch}, List.of("0"));
    }

    /// Counts the numbers a version names, `1.x` naming one and `1.2` two.
    ///
    /// @param version the version
    /// @return one to three, or none when it names no number at all
    private static int countOf(String version) {
        String clean = compared(version);
        int count = 0;
        for (String part : clean.split("-", 2)[0].split("\\.", -1)) {
            if (!isNumber(part)) {
                break;
            }
            count++;
        }
        return Math.min(count, 3);
    }

    /// Splits a version into the parts npm compares.
    ///
    /// @param version the version
    /// @return its three numbers and its pre-release identifiers, the identifiers being
    ///         `null` when the version carries none
    private static Parsed parse(String version) {
        String clean = compared(version);
        String[] halves = clean.split("-", 2);
        String[] written = halves[0].split("\\.", -1);
        int[] numbers = new int[3];
        for (int i = 0; i < 3; i++) {
            // A number a range left out, or wrote as an `x` where one belongs, counts as a
            // zero, which is how npm compares `1.x` with `1.0.0`.
            numbers[i] = i < written.length ? numeric(written[i]) : 0;
        }
        List<String> prerelease = halves.length > 1 && !halves[1].isEmpty()
                ? List.of(halves[1].split("\\.", -1))
                : null;
        return new Parsed(numbers, prerelease);
    }

    /// Returns a version with the parts npm does not compare taken off it.
    ///
    /// A leading \`v\` is decoration, and build metadata — everything from a \`+\` — says how a build
    /// was made rather than which version it is: npm reads \`v1.2.3+build\` as \`1.2.3\`, so a plugin
    /// asking for \`^1.2.3+build\` is asking for the same line as \`^1.2.3\`.
    ///
    /// @param version the version as it was written
    /// @return the version as it is compared
    private static String compared(String version) {
        String clean = version.trim();
        if (clean.startsWith("v") || clean.startsWith("V")) {
            clean = clean.substring(1);
        }
        int build = clean.indexOf('+');
        return build < 0 ? clean : clean.substring(0, build);
    }

    /// Reads a number out of one of a version's three leading parts.
    ///
    /// @param text the part
    /// @return its number, or zero when it is not one
    private static int numeric(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// Reports whether a pre-release identifier is a number.
    ///
    /// @param identifier the identifier
    /// @return whether it is one
    private static boolean isNumber(String identifier) {
        if (identifier.isEmpty()) {
            return false;
        }
        for (int i = 0; i < identifier.length(); i++) {
            char c = identifier.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /// Compares two parsed versions.
    ///
    /// The numbers first; then a version with no pre-release outranks the same version with
    /// one, so `0.1.7` comes after `0.1.7-rc.2`; then the pre-release identifiers in turn, a
    /// number before a name and the longer list after the shorter when everything before it
    /// is equal, so `alpha` is under `beta` and `rc` under `rc.1`.
    ///
    /// @param left  the left version
    /// @param right the right version
    /// @return negative, zero or positive
    private static int compare(Parsed left, Parsed right) {
        for (int i = 0; i < 3; i++) {
            if (left.numbers()[i] != right.numbers()[i]) {
                return Integer.compare(left.numbers()[i], right.numbers()[i]);
            }
        }

        List<String> mine = left.prerelease();
        List<String> theirs = right.prerelease();
        if (mine == null || theirs == null) {
            return mine == theirs ? 0 : mine == null ? 1 : -1;
        }

        int length = Math.max(mine.size(), theirs.size());
        for (int i = 0; i < length; i++) {
            if (i == mine.size()) {
                return -1;
            }
            if (i == theirs.size()) {
                return 1;
            }
            int order = compareIdentifiers(mine.get(i), theirs.get(i));
            if (order != 0) {
                return order;
            }
        }
        return 0;
    }

    /// Compares two pre-release identifiers.
    ///
    /// A number is under a name, two numbers are compared as numbers and two names as text,
    /// which is npm's rule and the one that tells `0.1.7-rc.1` from `0.1.7-rc.2`.
    ///
    /// @param left  the left identifier
    /// @param right the right identifier
    /// @return negative, zero or positive
    private static int compareIdentifiers(String left, String right) {
        boolean leftNumber = isNumber(left);
        boolean rightNumber = isNumber(right);
        if (leftNumber && rightNumber) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumber != rightNumber) {
            return leftNumber ? -1 : 1;
        }
        return left.compareTo(right);
    }

    /// A version read into the parts npm compares.
    ///
    /// @param numbers    major, minor and patch
    /// @param prerelease the pre-release identifiers in the order they were written, or
    ///                   `null` when the version carries none
    private record Parsed(int[] numbers, @Nullable List<String> prerelease) {
    }
}
