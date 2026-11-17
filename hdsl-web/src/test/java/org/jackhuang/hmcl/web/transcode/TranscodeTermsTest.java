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

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscodeTermsTest {

    @Test
    void theNoticeHasAStableVersionAndHash() {
        assertTrue(TranscodeTerms.VERSION >= 1);
        assertTrue(TranscodeTerms.text().contains("ffmpeg"));
        assertTrue(TranscodeTerms.hash().startsWith("sha256:"));
        assertTrue(TranscodeTerms.hash().length() == "sha256:".length() + 64);
        assertFalse(TranscodeTerms.hash().isBlank());
    }

    @Test
    void aConsentCanBeBuiltFromTheCurrentNotice(@TempDir Path dir) {
        TranscodeConsent consent = new TranscodeConsent(TranscodeTerms.VERSION, TranscodeTerms.hash(),
                "2026-11-10T00:00:00Z", "tester", "127.0.0.1",
                "checkbox", "0.6.82", "UA", "abc1234", true);
        TranscodeFeatureState state = new TranscodeFeatureState(dir.resolve("feature.json"));
        state.enable(consent);
        assertTrue(state.enabled());
    }
}