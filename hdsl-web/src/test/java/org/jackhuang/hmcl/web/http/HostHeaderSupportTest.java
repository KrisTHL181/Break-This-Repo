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
package org.jackhuang.hmcl.web.http;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pins the fix for the 2026-10-28 regression: a static `HttpClient` created
/// before the property was set made every `/i/*` mount answer
/// `500 restricted header name: "Host"`.
///
/// The in-process case cannot reproduce it (this JVM already set the property at
/// class-load time, which is exactly why the old tests were green), so the two
/// ordering cases run in **fresh JVMs** through [HostHeaderProbe].
class HostHeaderSupportTest {

    @Test
    void allowIsIdempotentAndTheHeaderIsUsableHere() {
        HostHeaderSupport.allow();
        HostHeaderSupport.allow();
        assertEquals("host", System.getProperty(HostHeaderSupport.PROPERTY));
        assertTrue(HostHeaderSupport.isHostHeaderUsable());
    }

    @Test
    void aClientCreatedBeforeThePropertyIsCaughtByVerify() throws Exception {
        assertEquals("REFUSED", runProbe("bad"),
                "loading a class with a static HttpClient first must make verify() refuse");
    }

    @Test
    void settingThePropertyFirstKeepsTheHeaderUsable() throws Exception {
        assertEquals("OK", runProbe("good"));
    }

    /// Runs [HostHeaderProbe] in a new JVM and returns the line it printed.
    private static String runProbe(String mode) throws Exception {
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        ProcessBuilder builder = new ProcessBuilder(
                java, "-cp", System.getProperty("java.class.path"),
                HostHeaderProbe.class.getName(), mode);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        assertTrue(process.waitFor(60, TimeUnit.SECONDS), "probe timed out");
        // The probe's own diagnostics (a warning about the class loader, for
        // instance) are allowed; the verdict is the last line.
        String[] lines = output.split("\n");
        return lines[lines.length - 1].trim();
    }
}
