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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What npm says about its own ranges, asked of this launcher's reading of them.
///
/// The rule [DshPluginRequirements] follows is npm's, so the cases worth checking are npm's: the
/// fixture files of node-semver, the implementation npm itself resolves a `dependencies` entry
/// with. They are copied into `semver/npm-satisfies.tsv` rather than written here, because a case
/// written here is the answer this launcher already gives, checked against itself.
///
/// **This found forty-two answers that differ from npm's** the first time it ran, which is the
/// point of it: fourteen invented cases agreed with the code, and no number of invented cases would
/// have said that a plugin asking for `>= 1.0.0` is hidden from every instance. What they are is
/// `semver/npm-differences.tsv`, which may only shrink.
class DshPluginRequirementsCorpusTest {
    /// The cases, on the test classpath.
    private static final String CASES = "/semver/npm-satisfies.tsv";

    /// The cases this launcher answers differently, on the test classpath.
    private static final String DIFFERENCES = "/semver/npm-differences.tsv";

    /// Reads one of the two files.
    ///
    /// @param resource the file's name on the classpath
    /// @return one entry per line, with the comments and the blank lines left out
    /// @throws IOException when the file cannot be read
    private static List<String[]> read(String resource) throws IOException {
        InputStream stream = DshPluginRequirementsCorpusTest.class.getResourceAsStream(resource);
        assertNotNull(stream, "the corpus is not on the classpath: " + resource);
        List<String[]> entries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length >= 3) {
                    entries.add(parts);
                }
            }
        }
        return entries;
    }

    /// Returns a field as it stands for something.
    ///
    /// A file of lines cannot carry a tab inside a field, and one of npm's cases is about exactly
    /// that — \`<\t2.0.0\`, an operator separated from its version by one. It is written \`\\t\` there.
    ///
    /// @param field the field as the file holds it
    /// @return the text it stands for
    private static String text(String field) {
        return field.replace("\\t", "\t");
    }

    @Test
    void everyCaseNpmChecksItsOwnRangesWithIsAnsweredTheSameWay() throws IOException {
        List<String[]> cases = read(CASES);
        assertTrue(cases.size() > 100,
                "only " + cases.size() + " cases were read, so this is not reading them");

        List<String> wrong = new ArrayList<>();
        for (String[] entry : cases) {
            boolean expected = entry[0].equals("yes");
            boolean answered = DshPluginRequirements.satisfies(text(entry[1]), text(entry[2]));
            if (answered != expected) {
                wrong.add(entry[1] + " -> " + entry[2]);
                System.out.println("DIFF\t" + (expected ? "missed" : "wrongly fits") + "\t"
                        + entry[1] + "\t" + entry[2]);
            }
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " of " + cases.size()
                + " cases are answered differently from npm and are not written down as differences: "
                + wrong);
    }

    /// Asks the same question of the differences, in the other direction.
    ///
    /// A list of known differences is only honest while it is true: a case that has been fixed has
    /// to be struck off it, and this is what says which one. It is also what makes the list a
    /// ledger rather than a place to put a failing case and forget it.
    @Test
    void everyDifferenceFromNpmIsStillThere() throws IOException {
        List<String[]> differences = read(DIFFERENCES);
        assertTrue(differences.size() > 10,
                "only " + differences.size() + " differences were read, so this is not reading them");

        for (String[] entry : differences) {
            // "missed" is npm's yes that this launcher does not give, and "wrongly fits" is the
            // same disagreement the other way round: the two words are npm's answer.
            boolean npmSays = entry[0].equals("missed");
            assertEquals(!npmSays, DshPluginRequirements.satisfies(text(entry[1]), text(entry[2])),
                    "this launcher now answers " + entry[1] + " -> " + entry[2] + " the way npm does,"
                            + " so it is no longer a difference and has to be struck off the list");
        }
    }
}