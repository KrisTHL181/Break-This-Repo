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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// What a value looks like once it is written into a settings file.
///
/// The rule is that a value is quoted unless it is provably safe as a plain scalar, because the
/// failure it prevents is silent in the other direction: `@scope/name` written plain is not a
/// package name with an at sign in front of it, it is a file the harness cannot parse — and the
/// harness reads these files while it starts, so the launch dies rather than the value being wrong.
class YamlScalarTest {
    @Test
    void aValueThatBeginsWithAnIndicatorIsQuoted() {
        assertEquals("\"@deepseek-ai/dsh-web-search-deepseek\"",
                YamlScalar.of("@deepseek-ai/dsh-web-search-deepseek"),
                "a package name is an indicator to YAML, so it is written as text");
        assertEquals("\"#fff\"", YamlScalar.of("#fff"));
        assertEquals("\"-1.0\"", YamlScalar.of("-1.0"), "a leading dash is an indicator, and a number");
    }

    @Test
    void anIndicatorInsideAValueIsOnlyText() {
        assertEquals("a@b", YamlScalar.of("a@b"), "the rule is about the first character");
        assertEquals("sk-test", YamlScalar.of("sk-test"));
        assertEquals("0.1.7-rc.1", YamlScalar.of("0.1.7-rc.1"), "a version needs no quotes");
        assertEquals("\"https://api.deepseek.com\"", YamlScalar.of("https://api.deepseek.com"),
                "the rule is deliberately narrow, so a URL is quoted for its colon");
    }

    @Test
    void theWordsAndNumbersYamlReadsAsSomethingElseAreQuoted() {
        assertEquals("\"true\"", YamlScalar.of("true"));
        assertEquals("\"null\"", YamlScalar.of("null"));
        assertEquals("\"\"", YamlScalar.of(""));
        assertEquals("\"123\"", YamlScalar.of("123"), "a model id that looks like a number stays text");
    }
}
