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
package org.jackhuang.hmcl.web.transcode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscodeFeatureStateTest {

    private static TranscodeConsent consent() {
        return new TranscodeConsent(1, TranscodeTerms.hash(),
                "2026-11-10T00:00:00Z", "tester", "127.0.0.1",
                "checkbox", "0.6.82", "UA", "abc1234", true);
    }

    @Test
    void startsDisabled(@TempDir Path dir) {
        TranscodeFeatureState state = new TranscodeFeatureState(dir.resolve("feature.json"));
        assertEquals(TranscodeFeatureState.State.DISABLED, state.state());
        assertFalse(state.enabled());
        assertTrue(state.consent().isEmpty());
        assertFalse(Files.exists(dir.resolve("feature.json")), "opening the state must not create it");
    }

    @Test
    void enableAndItsConsentSurviveAReopen(@TempDir Path dir) {
        TranscodeFeatureState state = new TranscodeFeatureState(dir.resolve("feature.json"));
        state.enable(consent());

        TranscodeFeatureState reopened = new TranscodeFeatureState(dir.resolve("feature.json"));
        assertTrue(reopened.enabled());
        assertEquals(TranscodeTerms.hash(), reopened.consent().orElseThrow().termsTextHash());
        assertEquals("tester", reopened.consent().orElseThrow().acceptedBy());
    }

    @Test
    void disablingKeepsTheEvidenceOfTheLastEnable(@TempDir Path dir) {
        TranscodeFeatureState state = new TranscodeFeatureState(dir.resolve("feature.json"));
        state.enable(consent());
        state.disable();

        TranscodeFeatureState reopened = new TranscodeFeatureState(dir.resolve("feature.json"));
        assertFalse(reopened.enabled());
        assertTrue(reopened.consent().isPresent(), "the last acceptance is still audit evidence");
    }

    @Test
    void aBrokenFileReadsAsDisabled(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("feature.json");
        Files.writeString(file, "{not json");
        TranscodeFeatureState state = new TranscodeFeatureState(file);
        assertFalse(state.enabled());
        assertTrue(state.consent().isEmpty());
    }
}