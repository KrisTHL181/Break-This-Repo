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

/// Writing a value into YAML that somebody else's parser will read back unchanged.
///
/// Every value this launcher writes into a harness settings file comes from a person or a vendor —
/// a vendor id, a model id, a name — so none of them may be assumed to be a harmless word. A plain
/// YAML scalar is a much smaller language than it looks:
///
/// - `model: qwen3:32b` is **not** a scalar with a colon in it. A colon inside a plain scalar must
///   not be followed by a space, and several parsers reject the line outright rather than guess;
///   the harness's own parser reports the document as malformed, and its settings layer then fails
///   on *every* later write. A model id with a colon is an ordinary thing to have.
/// - `model: a#b` is read as the scalar `a` with a comment after it: the rest is **silently
///   dropped**, so the harness would be pointed at a different model than the one asked for.
/// - `model: yes`, `model: "123"`, `model: null` are the booleans and numbers and null of YAML's
///   own vocabulary, not strings. Quoting is the only way to say "this is text".
///
/// So a value is quoted unless it is provably one of the few shapes that cannot be misread. Quoting
/// something that did not need it costs nothing; failing to quote something that did costs a
/// corrupted settings file.
@NotNullByDefault
public final class YamlScalar {
    private YamlScalar() {
    }

    /// The words that mean something other than themselves in YAML.
    private static final java.util.Set<String> RESERVED = java.util.Set.of(
            "true", "false", "yes", "no", "on", "off", "y", "n",
            "null", "~", "");

    /// The characters YAML reserves at the start of a plain scalar.
    ///
    /// A value beginning with one of these is an indicator rather than text, however ordinary the
    /// rest of it is: `@scope/name` is a package name to a person and a parse error to YAML. The
    /// rule is about the **first** character only, which is why it is separate from the walk in
    /// [#isPlainlySafe]: the same characters are ordinary inside a value.
    private static final String RESERVED_FIRST = "-?:,[]{}#&*!|>'\"%@`";

    /// Returns a value as a YAML scalar that reads back as exactly this text.
    ///
    /// @param value the value, or `null` for the empty string
    /// @return the scalar
    public static String of(String value) {
        String text = value == null ? "" : value;

        if (isPlainlySafe(text) && !isReserved(text)) {
            return text;
        }

        StringBuilder quoted = new StringBuilder(text.length() + 2);
        quoted.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> quoted.append("\\\"");
                case '\\' -> quoted.append("\\\\");
                case '\n' -> quoted.append("\\n");
                case '\r' -> quoted.append("\\r");
                case '\t' -> quoted.append("\\t");
                default -> {
                    // Control characters have no place in a settings value; a double-quoted YAML
                    // scalar can carry them as escapes, which is better than writing a byte that
                    // makes the file unreadable.
                    if (c < 0x20) {
                        quoted.append(String.format("\\x%02x", (int) c));
                    } else {
                        quoted.append(c);
                    }
                }
            }
        }
        return quoted.append('"').toString();
    }

    /// Reports whether YAML would read a plain scalar as something other than this text.
    ///
    /// Two families: the words that are booleans and null in YAML's own vocabulary, and the shapes
    /// that are numbers. `model: 123` is the number one hundred and twenty-three, and a harness
    /// reading back a number where it expects a model id is a different failure from a parse error
    /// only in how late it appears.
    ///
    /// @param text the text
    /// @return whether it must be quoted to stay text
    private static boolean isReserved(String text) {
        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (RESERVED.contains(lower)) {
            return true;
        }
        // Integers, decimals, and the same with a sign or an exponent: `123`, `-4`, `1.5`, `1e3`.
        return text.matches("[-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([eE][-+]?[0-9]+)?");
    }

    /// Reports whether a value can be written as a plain scalar.
    ///
    /// Deliberately narrow. Letters, digits, `-`, `_`, `.`, `/` and `@` cover vendor ids, model ids,
    /// names and URLs; anything else — a space, a colon, a hash, a quote, a bracket, a leading or
    /// trailing space — is quoted. It is not an attempt to be clever about the YAML grammar, it is a
    /// list of shapes that are certainly fine.
    ///
    /// @param text the text
    /// @return whether it may be written unquoted
    private static boolean isPlainlySafe(String text) {
        if (text.isEmpty()) {
            return false;
        }
        if (RESERVED_FIRST.indexOf(text.charAt(0)) >= 0) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '/' || c == '@' || c == '+';
            if (!safe) {
                return false;
            }
        }
        return true;
    }
}
