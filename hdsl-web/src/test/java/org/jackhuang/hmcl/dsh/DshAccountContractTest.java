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
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The account contract: what it says, and what it refuses to say.
class DshAccountContractTest {
    @TempDir
    private Path home;

    private DshInstance instance() {
        return new DshInstance("test", "0.1.6-alpha.2", "web", home.resolve("work").toString(),
                "system", DshHomeMode.CUSTOM, home.resolve("dsh-home").toString(),
                List.of(), Map.of(), null, null, null, 0, 0L);
    }

    @Test
    void noNameWouldBeRefusedByTheHarness() throws Exception {
        // The harness refuses these from any discovered .env and refuses by throwing, so one
        // here would stop it from starting at all. The check is over the real contract rather
        // than over a copy of it, which is the only version of this test worth having.
        Map<String, String> withAccount = DshAccountContract.values(instance(),
                new DshAccount("deepseek", "sk-test", null, null));
        for (String name : withAccount.keySet()) {
            assertFalse(DshAccountContract.refused(name), name);
        }
        for (String name : DshAccountContract.values(instance(), null).keySet()) {
            assertFalse(DshAccountContract.refused(name), name);
        }
        assertFalse(withAccount.containsKey("HDSL_LAUNCH_API_KEY"), withAccount.toString());
        for (Map.Entry<String, String> value : withAccount.entrySet()) {
            assertFalse(value.getValue().contains("sk-test"), name(value));
        }
    }

    private static String name(Map.Entry<String, String> value) {
        return value.getKey() + "=" + value.getValue();
    }

    @Test
    void theInstanceIsAlwaysDescribedAndTheAccountOnlyWhenThereIsOne() throws Exception {
        Map<String, String> bare = DshAccountContract.values(instance(), null);
        assertEquals(DshAccountContract.VERSION, bare.get(DshAccountContract.CONTRACT));
        assertEquals("test", bare.get(DshAccountContract.INSTANCE_ID));
        assertEquals("web", bare.get(DshAccountContract.INSTANCE_PROFILE));
        assertFalse(bare.containsKey(DshAccountContract.ACCOUNT_NAME));

        Map<String, String> withAccount = DshAccountContract.values(instance(),
                new DshAccount("deepseek", "sk-test", null, "work"));
        assertEquals("work", withAccount.get(DshAccountContract.ACCOUNT_NAME));
        assertEquals("work", withAccount.get(DshAccountContract.ACCOUNT_ROUTE));
        assertEquals("deepseek", withAccount.get(DshAccountContract.ACCOUNT_VENDOR));
        assertEquals("official", withAccount.get(DshAccountContract.ACCOUNT_KIND));
    }

    @Test
    void theBlockIsWrittenAndRewrittenWithoutTouchingAnythingElse() throws Exception {
        Map<String, String> first = DshAccountContract.values(instance(),
                new DshAccount("deepseek", "sk-test", null, null));
        String theirs = "# my own file\nSOME_VAR=1\n";
        String written = DshAccountContract.withBlock(theirs, first);
        assertTrue(written.startsWith("# my own file\nSOME_VAR=1\n"), written);
        assertTrue(written.contains("HDSL_ACCOUNT_NAME=DeepSeek"), written);

        // A second write replaces the block rather than appending another one, and leaves the
        // person's lines exactly where they were.
        Map<String, String> second = DshAccountContract.values(instance(), null);
        String again = DshAccountContract.withBlock(written, second);
        // One block, not two: the second write replaces the first.
        int blocks = again.split("HDSL_ACCOUNT_CONTRACT=", -1).length - 1;
        assertEquals(1, blocks, again);
        assertTrue(again.startsWith("# my own file\nSOME_VAR=1\n"), again);
        assertFalse(again.contains("HDSL_ACCOUNT_NAME"), again);
    }

    @Test
    void anEmptyFileGetsJustTheBlock() throws Exception {
        String written = DshAccountContract.withBlock("", DshAccountContract.values(instance(), null));
        assertTrue(written.startsWith("# >>> HDSL account contract"), written);
        assertTrue(written.endsWith("# <<< HDSL account contract\n"), written);
    }

    @Test
    void valuesAreQuotedOnlyWhenTheyHaveToBe() {
        assertEquals("deepseek", DshAccountContract.quoted("deepseek"));
        assertEquals("https://api.deepseek.com", DshAccountContract.quoted("https://api.deepseek.com"));
        assertEquals("\"two words\"", DshAccountContract.quoted("two words"));
        assertEquals("\"\"", DshAccountContract.quoted(""));
        assertEquals("\"say \\\"hi\\\"\"", DshAccountContract.quoted("say \"hi\""));
    }

    @Test
    void refusedNamesAreTheHarnessOnes() {
        assertTrue(DshAccountContract.refused("DSH_HOME"));
        assertTrue(DshAccountContract.refused("dsH_anything"));
        assertTrue(DshAccountContract.refused("PATH"));
        assertTrue(DshAccountContract.refused("XDG_DATA_HOME"));
        assertFalse(DshAccountContract.refused("HDSL_ACCOUNT_NAME"));
        assertFalse(DshAccountContract.refused("HTTP_PROXY"));
    }
}
