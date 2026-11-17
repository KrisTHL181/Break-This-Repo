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
package org.jackhuang.hmcl.web.zcode;

import org.jackhuang.hmcl.dsh.NpmRegistry;
import org.jackhuang.hmcl.setting.LauncherSettings;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The in-panel ZCode build: a local "source tarball" dressed in the layout the
/// patch expects, a stub `pnpm` that answers `install` and `build:zcode`, and
/// the real verify-and-install path in between — so the pipeline (download →
/// unpack → patch → install deps → build → sha256 check → `releases/<v>/` +
/// `current`) runs end to end without network or a real monorepo.
class ZcodeBuilderTest {

    @TempDir
    Path dir;

    @Test
    void buildsAndInstallsAReleaseFromALocalSourceTarball() throws Exception {
        copy(Path.of("src/test/resources/fake-zcode-source"), dir.resolve("srcroot"));
        Path tarball = dir.resolve("zcode-source.tar.gz");
        run(List.of("tar", "-czf", tarball.toString(), "-C", dir.toString(), "srcroot"));

        ZcodeBuilder.Settings settings = new ZcodeBuilder.Settings(
                dir.resolve("root"),
                "",
                fakePnpm(),
                tarball.toString(),
                false);

        ZcodeBuilder.start(settings, "main");
        ZcodeBuilder.Status status = awaitTerminal(120);
        assertEquals(ZcodeBuilder.State.DONE, status.state(),
                status.message() + " | log: " + String.join("\n", ZcodeBuilder.tailLog(40)));

        Path current = ZcodeBuilder.currentPackage(dir.resolve("root"));
        assertTrue(Files.isRegularFile(current.resolve("bin/zcode.mjs")), current.toString());
        assertTrue(Files.readString(current.resolve("package.json")).contains("9.9.9"), current.toString());

        List<ZcodeBuilder.Release> releases = ZcodeBuilder.releases(dir.resolve("root"));
        assertEquals(1, releases.size());
        assertEquals("9.9.9", releases.get(0).version());
        assertTrue(releases.get(0).current(), "the built release must be the current one");

        // The scratch tree is cleaned up unless keepSources asked otherwise.
        assertFalse(Files.exists(dir.resolve("root/build/src")));
        assertFalse(Files.exists(dir.resolve("root/build/source.tar.gz")));
    }

    @Test
    void keepsTheScratchTreeWhenAsked() throws Exception {
        copy(Path.of("src/test/resources/fake-zcode-source"), dir.resolve("srcroot"));
        Path tarball = dir.resolve("zcode-source.tar.gz");
        run(List.of("tar", "-czf", tarball.toString(), "-C", dir.toString(), "srcroot"));

        ZcodeBuilder.Settings settings = new ZcodeBuilder.Settings(
                dir.resolve("root"), "", fakePnpm(), tarball.toString(), true);

        ZcodeBuilder.start(settings, "main");
        assertEquals(ZcodeBuilder.State.DONE, awaitTerminal(120).state());
        // …and the patch is visible in the tree it kept.
        String vite = Files.readString(dir.resolve("root/build/src/packages/web/vite.config.ts"));
        assertTrue(vite.contains("base: \"./\","), vite);
        // …and the platform filter is gone with the checkout's `.npmrc`: pnpm reads
        // `supportedArchitectures` from `pnpm-workspace.yaml` rather than from `.npmrc`, so the one
        // the builder used to append never took effect — and the effect it asked for would have
        // broken the SEA staging step, which collects native assets for every target.
        assertFalse(Files.exists(dir.resolve("root/build/src/.npmrc")),
                "the builder must not narrow the install through an .npmrc of its own");
        // The generated `packages/shared/dist` the SEA staging step insists on is the step the
        // builder runs for itself (the stub writes what `pnpm exec tsc -b` would have).
        assertTrue(Files.isRegularFile(dir.resolve("root/build/src/packages/shared/dist/index.js")));
    }

    @Test
    void aMissingSourceTarballEndsInError() throws Exception {
        ZcodeBuilder.Settings settings = new ZcodeBuilder.Settings(
                dir.resolve("root"), "", fakePnpm(), dir.resolve("missing.tar.gz").toString(), false);

        ZcodeBuilder.start(settings, "v9.9.9");
        ZcodeBuilder.Status status = awaitTerminal(60);
        assertEquals(ZcodeBuilder.State.ERROR, status.state());
        assertNotNull(status.error());
    }

    @Test
    void rejectsARefNameThatIsNotARefName() throws Exception {
        ZcodeBuilder.Settings settings = new ZcodeBuilder.Settings(
                dir.resolve("root"), "", fakePnpm(), "%s", false);
        assertThrows(ZcodeException.class, () -> ZcodeBuilder.start(settings, "main; rm -rf /"));
    }

    /// The chosen download source reaches the child process on its own command line, and the mirror
    /// that publishes Electron's binaries reaches its environment — the two things that decide
    /// whether the install runs at tens of kilobytes a second or at megabytes a second.
    @Test
    void theInstallNamesTheChosenRegistryAndElectronMirror() throws Exception {
        copy(Path.of("src/test/resources/fake-zcode-source"), dir.resolve("srcroot"));
        Path tarball = dir.resolve("zcode-source.tar.gz");
        run(List.of("tar", "-czf", tarball.toString(), "-C", dir.toString(), "srcroot"));

        LauncherSettings settings = SettingsManager.settings();
        settings.setNpmRegistryPreset("npmmirror");
        String stub = fakePnpm();
        try {
            ZcodeBuilder.Settings builderSettings = new ZcodeBuilder.Settings(
                    dir.resolve("root"), "", stub, tarball.toString(), false);
            ZcodeBuilder.start(builderSettings, "main");
            ZcodeBuilder.Status status = awaitTerminal(120);
            assertEquals(ZcodeBuilder.State.DONE, status.state(),
                    status.message() + " | log: " + String.join("\n", ZcodeBuilder.tailLog(40)));

            // The stub recorded every call it was given: one line per call, the argv joined by
            // spaces, plus a line for ELECTRON_MIRROR when the builder set one.
            List<String> recorded = Files.readAllLines(Path.of(stub + ".args"), StandardCharsets.UTF_8);
            String install = recorded.stream().filter(line -> line.startsWith("install "))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("the stub recorded no `pnpm install`: " + recorded));
            assertTrue(install.contains("--registry=https://registry.npmmirror.com"),
                    "the install must name the chosen registry: " + recorded);
            assertTrue(recorded.contains("ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/"),
                    "the chosen mirror's Electron copy must reach the child's environment: " + recorded);
        } finally {
            settings.setNpmRegistryPreset(NpmRegistry.ENVIRONMENT);
        }
    }

    /// A pointer written as a text file — what [org.jackhuang.hmcl.util.io.Links]
    /// does where symbolic links are not permitted — must still mark its release
    /// as current. On Windows this is the ordinary case, not the exotic one.
    @Test
    void aTextPointerStillReportsTheReleaseAsCurrent() throws Exception {
        Path root = dir.resolve("root");
        Path release = root.resolve("releases").resolve("9.9.9");
        Files.createDirectories(release.resolve("bin"));
        Files.writeString(release.resolve("bin/zcode.mjs"), "// stub\n", StandardCharsets.UTF_8);
        Files.writeString(release.resolve("package.json"), "{\"version\":\"9.9.9\"}\n", StandardCharsets.UTF_8);
        Files.writeString(root.resolve("current"),
                release.toAbsolutePath().normalize() + System.lineSeparator(), StandardCharsets.UTF_8);

        List<ZcodeBuilder.Release> releases = ZcodeBuilder.releases(root);
        assertEquals(1, releases.size());
        assertTrue(releases.get(0).current(), "a pointer file must still mark its release as current");
        assertEquals(release, ZcodeBuilder.currentPackage(root));
    }

    /// Cleaning the `current` pointer must delete the pointer, never what it points
    /// at. Walking the release through the link is what made a rebuild empty an
    /// installed version instead of starting clean.
    @Test
    void cleaningThePointerDoesNotTouchTheRelease() throws Exception {
        Path root = dir.resolve("root");
        Path release = root.resolve("releases").resolve("9.9.9");
        Files.createDirectories(release.resolve("bin"));
        Files.writeString(release.resolve("bin/zcode.mjs"), "// stub\n", StandardCharsets.UTF_8);
        Path current = root.resolve("current");
        try {
            Files.createSymbolicLink(current, release);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.abort("symbolic links are not permitted here: " + e.getMessage());
        }

        ZcodeBuilder.deleteRecursively(current);

        assertFalse(Files.exists(current, java.nio.file.LinkOption.NOFOLLOW_LINKS), "the pointer is gone");
        assertTrue(Files.isRegularFile(release.resolve("bin/zcode.mjs")),
                "the release the pointer named must survive the pointer's deletion");
    }

    @Test
    void cleaningATextPointerLeavesTheReleaseAlone() throws Exception {
        Path root = dir.resolve("root");
        Path release = root.resolve("releases").resolve("9.9.9");
        Files.createDirectories(release.resolve("bin"));
        Files.writeString(release.resolve("bin/zcode.mjs"), "// stub\n", StandardCharsets.UTF_8);
        Path current = root.resolve("current");
        Files.writeString(current, release.toAbsolutePath().normalize() + System.lineSeparator(),
                StandardCharsets.UTF_8);

        ZcodeBuilder.deleteRecursively(current);

        assertFalse(Files.exists(current, java.nio.file.LinkOption.NOFOLLOW_LINKS), "the pointer file is gone");
        assertTrue(Files.isRegularFile(release.resolve("bin/zcode.mjs")),
                "the release the pointer named must survive the pointer file's deletion");
    }

    /// The stub `pnpm`, copied somewhere writable with its executable bit set:
    /// a test must not depend on the checkout's own mode — this repository has
    /// `core.fileMode` off, so the bit is not even recorded in git.
    ///
    /// The whole fixture directory is copied, not just the POSIX script: the stub
    /// lives in `pnpm.mjs` with a one-line wrapper per platform, and on Windows the
    /// launcher resolves `pnpm` to the `pnpm.cmd` wrapper beside it. Copying only
    /// `pnpm` would leave Windows nothing to resolve, and the build would fail on a
    /// stub it could not start rather than on anything the test is about.
    private String fakePnpm() throws IOException {
        Path target = dir.resolve("fake-pnpm");
        Files.createDirectories(target);
        copy(Path.of("src/test/resources/fake-zcode-build"), target);
        Path stub = target.resolve("pnpm");
        stub.toFile().setExecutable(true, false);
        return stub.toAbsolutePath().toString();
    }

    private static ZcodeBuilder.Status awaitTerminal(long seconds) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < deadline) {
            ZcodeBuilder.Status status = ZcodeBuilder.status();
            if (status.state() == ZcodeBuilder.State.DONE || status.state() == ZcodeBuilder.State.ERROR) {
                return status;
            }
            Thread.sleep(50);
        }
        return ZcodeBuilder.status();
    }

    private static void run(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
        assertTrue(process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0,
                "command failed: " + String.join(" ", command));
    }

    private static void copy(Path from, Path to) throws IOException {
        try (var walk = Files.walk(from)) {
            for (Path path : walk.toList()) {
                Path target = to.resolve(from.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
