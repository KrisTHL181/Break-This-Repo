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
package org.jackhuang.hmcl.dsh;

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

/// The subset of npm's `engines.node` syntax a brand actually uses.
///
/// A brand's `package.json` declares the range it can run on — Hermes 0.22.7
/// says `>=26`, 0.22.5 says `^22.22.0 || ^24.11.0 || >=26.0.0` — and picking a
/// runtime outside it is how "install the latest Hermes" turns into a tool that
/// dies in its own loader. This evaluator covers the operators those declarations
/// use (`^`, `~`, `>=`, `>`, `<=`, `<`, `=`, bare versions, `*`, and `||`), not
/// the whole npm grammar: a range it cannot parse answers `false` and the caller
/// treats that as "unknown", never as "satisfied".
@NotNullByDefault
public final class NodeSemver {

    private NodeSemver() {
    }

    /// Whether `version` satisfies `range`.
    ///
    /// @param version the concrete version, optionally prefixed with `v`
    /// @param range   the declared range, or `null`/blank for "any"
    /// @return whether it satisfies; `false` when either side cannot be parsed
    public static boolean satisfies(@Nullable String version, @Nullable String range) {
        if (version == null || version.isBlank()) {
            return false;
        }
        if (range == null || range.isBlank() || "*".equals(range.trim())) {
            return true;
        }
        int[] concrete = parse(version);
        if (concrete == null) {
            return false;
        }
        for (String alternative : range.split("\\|\\|")) {
            if (matches(concrete, alternative.trim())) {
                return true;
            }
        }
        return false;
    }

    /// Whether one `||` alternative (a whitespace-separated AND list) matches.
    private static boolean matches(int[] version, String expression) {
        if (expression.isEmpty()) {
            return true;
        }
        for (String token : expression.split("\\s+")) {
            if (!token.isEmpty() && !matchOne(version, token)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchOne(int[] version, String token) {
        if ("*".equals(token) || "x".equalsIgnoreCase(token) || "X".equals(token)) {
            return true;
        }
        if (token.startsWith("^")) {
            return caret(version, parse(token.substring(1)));
        }
        if (token.startsWith("~")) {
            return tilde(version, parse(token.substring(1)));
        }
        if (token.startsWith(">=")) {
            int[] bound = parse(token.substring(2));
            return bound != null && compare(version, bound) >= 0;
        }
        if (token.startsWith("<=")) {
            int[] bound = parse(token.substring(2));
            return bound != null && compare(version, bound) <= 0;
        }
        if (token.startsWith(">")) {
            int[] bound = parse(token.substring(1));
            return bound != null && compare(version, bound) > 0;
        }
        if (token.startsWith("<")) {
            int[] bound = parse(token.substring(1));
            return bound != null && compare(version, bound) < 0;
        }
        if (token.startsWith("=")) {
            return matchOne(version, token.substring(1));
        }
        return prefix(version, parse(token));
    }

    /// `^1.2.3` is `>=1.2.3 <2.0.0`; below 1.0 the leftmost non-zero component
    /// is the one that stops moving.
    private static boolean caret(int[] version, @Nullable int[] base) {
        if (base == null || compare(version, base) < 0) {
            return false;
        }
        if (base[0] > 0) {
            return version[0] == base[0];
        }
        if (base[1] > 0) {
            return version[0] == 0 && version[1] == base[1];
        }
        if (base[2] > 0) {
            return version[0] == 0 && version[1] == 0 && version[2] == base[2];
        }
        return true;
    }

    /// `~1.2.3` is `>=1.2.3 <1.3.0`.
    private static boolean tilde(int[] version, @Nullable int[] base) {
        if (base == null || compare(version, base) < 0) {
            return false;
        }
        return base[1] < 0
                ? version[0] == base[0]
                : version[0] == base[0] && version[1] == base[1];
    }

    /// A bare version: `22` means the 22.x line, `22.22` the 22.22.x line, and a
    /// full `22.22.0` is exact.
    private static boolean prefix(int[] version, @Nullable int[] base) {
        if (base == null) {
            return false;
        }
        if (base[1] < 0) {
            return version[0] == base[0];
        }
        if (base[2] < 0) {
            return version[0] == base[0] && version[1] == base[1];
        }
        return compare(version, base) == 0;
    }

    /// Parses `v22.22.0`, `22.22`, or `22`; a missing component is `-1` (a
    /// wildcard), not zero.
    private static int @Nullable [] parse(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        if (value.isEmpty()) {
            return null;
        }
        String[] parts = value.split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : -1;
            int patch = parts.length > 2 && !parts[2].isEmpty() ? Integer.parseInt(parts[2]) : -1;
            return new int[]{major, minor, patch};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int compare(int[] left, int[] right) {
        for (int i = 0; i < 3; i++) {
            int a = left[i] < 0 ? 0 : left[i];
            int b = right[i] < 0 ? 0 : right[i];
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }
}
