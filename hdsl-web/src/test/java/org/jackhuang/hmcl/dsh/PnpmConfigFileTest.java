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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The one line of pnpm's own `config.yaml` the panel owns.
///
/// pnpm does not read its registry from the environment, so a mirror chosen in the panel is worth
/// nothing unless it lands in this file — and the file is not the panel's: an operator's `storeDir`
/// lives in it too, and losing that line silently stops two instances from sharing a package store.
/// So the file is rewritten line by line here, and what this test watches is what survives the
/// rewrite: every other line, and the same address on the way back out.
class PnpmConfigFileTest {

    @TempDir
    Path dir;

    private static final String ADDRESS = "https://registry.npmmirror.com";

    /// Where the writer is pointed: the property pnpm's path is overridden with, and the reason
    /// these tests never touch the configuration of the machine they run on.
    private Path configFile() {
        return dir.resolve("pnpm").resolve("config.yaml");
    }

    @BeforeEach
    void pointTheWriterAtATemporaryFile() {
        System.setProperty("hdsl.pnpmConfig", configFile().toString());
    }

    @AfterEach
    void stopRedirectingTheWriter() {
        System.clearProperty("hdsl.pnpmConfig");
    }

    @Test
    void thePropertyPointsTheWriterSomewhereElse() {
        assertEquals(configFile(), PnpmConfigFile.path());
    }

    @Test
    void withoutThePropertyItIsTheFilePnpmReads() {
        System.clearProperty("hdsl.pnpmConfig");
        // The claim being tested is "the default is the file pnpm actually reads", and that
        // file is not the same one on every platform: pnpm keeps its registry in an `auth.ini`
        // under `%LOCALAPPDATA%` on Windows and in a YAML file under `$XDG_CONFIG_HOME`
        // elsewhere. Both are asserted, so the claim keeps its meaning on either — and it is
        // the measurement that made them differ (see PnpmConfigFile's class comment).
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData == null || localAppData.isBlank()) {
                // The fallbacks below `%LOCALAPPDATA%` are a degraded path, not the intended
                // one, and asserting them here would pin something no real machine uses.
                return;
            }
            assertEquals(Path.of(localAppData.trim()).resolve("pnpm").resolve("config").resolve("auth.ini"),
                    PnpmConfigFile.path());
            return;
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path expected = xdg != null && !xdg.isBlank()
                ? Path.of(xdg.trim()).resolve("pnpm/config.yaml")
                // Same source the product uses: `$HOME`, not the passwd entry. They differ
                // under systemd, sudo and any container that starts a process with HOME set
                // to something else, and `user.home` is only the second one.
                : org.jackhuang.hmcl.util.platform.UserHome.directory()
                        .resolve(".config/pnpm/config.yaml");
        assertEquals(expected, PnpmConfigFile.path());
    }

    @Test
    void everyOtherLineAndTheStoreDirectorySurviveTheRewrite() throws IOException {
        Files.createDirectories(configFile().getParent());
        Files.writeString(configFile(), """
                # written by the container's entrypoint
                storeDir: /data/pnpm-store
                storeDirShared: true
                """, StandardCharsets.UTF_8);

        assertTrue(PnpmConfigFile.write(ADDRESS), "a file with no registry line must be updated");

        List<String> lines = Files.readAllLines(configFile(), StandardCharsets.UTF_8);
        assertTrue(lines.contains("# written by the container's entrypoint"), lines.toString());
        assertTrue(lines.contains("storeDir: /data/pnpm-store"),
                "losing this line stops two instances from sharing a package store: " + lines);
        assertTrue(lines.contains("storeDirShared: true"), lines.toString());
        assertEquals(ADDRESS, unquote(registryLine(configFile())));
    }

    @Test
    void anAddressAlreadyInTheFileIsNotWrittenTwice() throws IOException {
        assertTrue(PnpmConfigFile.write(ADDRESS), "the first write creates the file");

        String written = Files.readString(configFile(), StandardCharsets.UTF_8);
        assertFalse(PnpmConfigFile.write(ADDRESS), "the same address changes nothing");
        assertEquals(written, Files.readString(configFile(), StandardCharsets.UTF_8));

        assertTrue(PnpmConfigFile.write("https://a.example"), "a different address does");
        assertEquals("https://a.example", unquote(registryLine(configFile())));
        assertEquals(1, registryLineCount(configFile()), "the old line was replaced, not kept beside the new one");
    }

    @Test
    void whatIsWrittenIsQuotedSoItReadsBackAsExactlyTheAddress() throws IOException {
        for (String address : List.of(
                ADDRESS, "http://192.0.2.1:4873", "https://nexus.example.com/repository/npm")) {
            PnpmConfigFile.write(address);
            String scalar = registryLine(configFile());
            assertEquals(YamlScalar.of(address), scalar, "the value goes through YamlScalar's quoting");
            assertEquals(address, unquote(scalar), "and reads back as the address that was written");
        }
    }

    @Test
    void theFileIsReadableOnlyByItsOwner() throws IOException {
        assertTrue(PnpmConfigFile.write(ADDRESS));
        if (org.jackhuang.hmcl.util.platform.Privileges.dropsChildren()) {
            // Inside the container the panel is root and pnpm is not, so the opposite is
            // required: a file root owns is one the agent cannot open at all, which is what
            // made every install fail with `EACCES … /home/hdsl/.config/pnpm/config.yaml`.
            // Assert that half here rather than skipping the test outright.
            assertEquals(org.jackhuang.hmcl.util.platform.Privileges.AGENT_UID,
                    ((Number) Files.getAttribute(configFile(), "unix:uid")).intValue(),
                    "inside the container the file must belong to the uid pnpm runs as");
            return;
        }
        org.jackhuang.hmcl.util.PosixPermissions.assertOwnerOnly(configFile(),
                "the pnpm config must be owner-only");
    }

    @Test
    void aRewriteLeavesNoTemporaryFileBehind() throws IOException {
        assertTrue(PnpmConfigFile.write(ADDRESS));
        try (var entries = Files.list(configFile().getParent())) {
            assertEquals(List.of(configFile()), entries.toList(),
                    "the file is replaced by a rename; a leftover temporary is one the agent can find");
        }
    }

    @Test
    void aStoreDirectoryLineIsAddedOnlyWhenTheEnvironmentNamesOne() throws IOException {
        assertTrue(PnpmConfigFile.write(ADDRESS));

        String store = System.getenv("PNPM_STORE_DIR");
        List<String> expected = store == null || store.isBlank()
                ? List.of("registry: " + YamlScalar.of(ADDRESS))
                : List.of("storeDir: " + YamlScalar.of(store.trim()), "registry: " + YamlScalar.of(ADDRESS));
        assertEquals(expected, Files.readAllLines(configFile(), StandardCharsets.UTF_8));
    }

    @Test
    void applyWarnsRatherThanFailingWhenTheFileCannotBeWritten() throws IOException {
        // A configuration file that cannot be written is a warning, not a failed settings save: the
        // next save tries again. A path whose parent is a regular file cannot be created at all.
        Path blocker = dir.resolve("blocker");
        Files.writeString(blocker, "not a directory", StandardCharsets.UTF_8);
        System.setProperty("hdsl.pnpmConfig", blocker.resolve("config.yaml").toString());

        assertDoesNotThrow(PnpmConfigFile::apply);
    }

    // ----------------------------------------------------------------- helpers --

    /// The value on the one `registry:` line, with the indentation pnpm writes.
    private static String registryLine(Path file) throws IOException {
        String prefix = "registry:";
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        throw new AssertionError("no registry line in " + file + ": "
                + Files.readString(file, StandardCharsets.UTF_8));
    }

    private static int registryLineCount(Path file) throws IOException {
        int found = 0;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.startsWith("registry:")) {
                found++;
            }
        }
        return found;
    }

    /// Reads back what [YamlScalar] wrote: the double-quoted form with its escapes undone.
    ///
    /// Deliberately a separate implementation of the same rule rather than a call into the writer,
    /// because the claim under test is that the value survives the trip through the file.
    private static String unquote(String scalar) {
        assertTrue(scalar.length() >= 2 && scalar.startsWith("\"") && scalar.endsWith("\""),
                "the value must be quoted to be text: " + scalar);
        String body = scalar.substring(1, scalar.length() - 1);
        StringBuilder text = new StringBuilder(body.length());
        for (int i = 0; i < body.length(); i++) {
            char character = body.charAt(i);
            if (character != '\\') {
                text.append(character);
                continue;
            }
            char escape = body.charAt(++i);
            switch (escape) {
                case '"' -> text.append('"');
                case '\\' -> text.append('\\');
                case 'n' -> text.append('\n');
                case 'r' -> text.append('\r');
                case 't' -> text.append('\t');
                case 'x' -> {
                    text.append((char) Integer.parseInt(body.substring(i + 1, i + 3), 16));
                    i += 2;
                }
                default -> throw new AssertionError("unknown escape in " + scalar);
            }
        }
        return text.toString();
    }
}
