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
package org.jackhuang.hmcl.web.terminal;

import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Claude Web runs the Claude Code CLI it bundles; the community engine and the
/// official binary are separate kernels instead of one silently standing in
/// for the other. The entry points mirror upstream `server/src/claude-exe.ts`.
class ClaudeWebTuiTest {

    @TempDir
    Path packageDir;

    /// `node_modules/claude-web-engine/dist/cli-node.js` — the package ships a
    /// JS entry and no `bin` field, so the path is the contract.
    private Path stageEngine() throws IOException {
        Path entry = packageDir.resolve(ClaudeWebTui.ENGINE_ENTRY);
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// engine\n");
        return entry;
    }

    /// The SDK's platform package carrying the official binary.
    private Path stageOfficial() throws IOException {
        Path entry = packageDir.resolve("node_modules")
                .resolve(ClaudeWebTui.officialPackageName())
                .resolve(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? "claude.exe" : "claude");
        Files.createDirectories(entry.getParent());
        Files.writeString(entry, "// official\n");
        return entry;
    }

    @Test
    void kernelsReportWhatIsActuallyInstalled() throws IOException {
        stageEngine();
        List<ClaudeWebTui.Kernel> kernels = ClaudeWebTui.kernels(packageDir);
        assertEquals(List.of(ClaudeWebTui.COMMUNITY, ClaudeWebTui.OFFICIAL),
                kernels.stream().map(ClaudeWebTui.Kernel::id).toList());
        assertTrue(kernels.get(0).available(), kernels.get(0).detail());
        // The official kernel also resolves a `claude` on PATH, so on a machine
        // that has one installed it is available even without the SDK package.
        assertEquals(claudeOnPath(), kernels.get(1).available(), kernels.get(1).detail());
    }

    @Test
    void aBundledOfficialBinaryIsFoundWithoutPath() throws IOException {
        Path official = stageOfficial();
        ClaudeWebTui.Kernel kernel = ClaudeWebTui.kernels(packageDir).get(1);
        assertTrue(kernel.available(), kernel.detail());
        assertEquals(official.toString(), kernel.detail());
    }

    @Test
    void theCommunityEngineIsTheDefault() throws IOException {
        stageEngine();
        stageOfficial();
        assertEquals(ClaudeWebTui.COMMUNITY, ClaudeWebTui.defaultKernel(packageDir));
    }

    @Test
    void anUnavailableKernelIsRefusedInsteadOfSubstituted() throws IOException {
        assumeTrue(!claudeOnPath(), "the official kernel resolves to a PATH binary on this machine");
        stageEngine();

        IOException expected = assertThrows(IOException.class,
                () -> new ClaudeWebTui(ClaudeWebTui.OFFICIAL).target(
                        packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "c1", null));
        assertTrue(expected.getMessage().contains("not available"), expected.getMessage());
    }

    @Test
    void anUnknownKernelIsRefused() {
        IOException expected = assertThrows(IOException.class,
                () -> new ClaudeWebTui("turbo").target(
                        packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "c1", null));
        assertTrue(expected.getMessage().contains("unknown Claude Code kernel"), expected.getMessage());
    }

    @Test
    void noKernelAtAllSaysWhatIsMissing() {
        assumeTrue(!claudeOnPath(), "the official kernel resolves to a PATH binary on this machine");
        IOException expected = assertThrows(IOException.class,
                () -> new ClaudeWebTui(null).target(
                        packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "c1", null));
        assertTrue(expected.getMessage().contains("claude-web-engine"), expected.getMessage());
    }

    /// A JS engine is started with `node <file>` and the instance home as
    /// `HOME` — the same isolation the web process gets.
    @Test
    void theCommunityEngineStartsThroughNode() throws IOException {
        assumeTrue(nodeOnPath(), "needs a node on PATH, like the panel does");
        Path entry = stageEngine();

        TerminalTarget target = new ClaudeWebTui(ClaudeWebTui.COMMUNITY).target(
                packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "c1", null);

        assertEquals(ClaudeWebTui.BRAND, target.brand());
        assertEquals(2, target.command().size(), target.command().toString());
        String node = Path.of(target.command().get(0)).getFileName().toString().toLowerCase();
        assertTrue(node.startsWith("node"), target.command().get(0));
        assertEquals(entry.toAbsolutePath().normalize().toString(), target.command().get(1));
        assertEquals(packageDir.resolve("workspace"), target.cwd());
        assertEquals(packageDir.resolve("home").toString(), target.environment().get("HOME"));
        assertEquals("xterm-256color", target.environment().get("TERM"));
    }

    /// A bare Windows deployment has no `node` on `PATH`; the panel installs one
    /// under `<data>/hdsl/runtimes`, and that runtime is what the engine has to
    /// start from. Asking `PATH` alone failed the community terminal with
    /// "node is not on PATH" while the Kimi and ZCode terminals already fell back
    /// to it — the panel had a perfectly good Node the whole time.
    @Test
    void theCommunityEngineFallsBackToTheManagedRuntime(@TempDir Path managed) throws IOException {
        Path node = managed.resolve("bin").resolve(
                OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? "node.exe" : "node");
        Files.createDirectories(node.getParent());
        Files.writeString(node, "");
        DshNodeRuntime runtime = new DshNodeRuntime(node, "22.0.0", null, null, null, null);

        assertEquals(node, ClaudeWebTui.requireNode(Optional::empty, () -> Optional.of(runtime)));
        // And `PATH` wins when it has one: the machine's own Node is the one the
        // operator expects, the same order the ZCode terminal resolves it in.
        assertEquals(node, ClaudeWebTui.requireNode(() -> Optional.of(runtime), Optional::empty));
    }

    /// Neither place has one: the message names both, so the answer is not a bare
    /// "install Node" on a panel that could have installed it itself.
    @Test
    void noNodeAnywhereSaysWhereOneComesFrom() {
        IOException expected = assertThrows(IOException.class,
                () -> ClaudeWebTui.requireNode(Optional::empty, Optional::empty));
        assertTrue(expected.getMessage().contains("Node.js"), expected.getMessage());
        assertTrue(expected.getMessage().contains("PATH"), expected.getMessage());
        assertFalse(expected.getMessage().startsWith("node is not on PATH"), expected.getMessage());
    }

    /// Claude Code runs its own shell tools through a bash on Windows and refuses
    /// the session without one ("Claude Code on Windows requires git-bash"), naming
    /// `CLAUDE_CODE_GIT_BASH_PATH` in the refusal. The panel is the one that can see
    /// where Git for Windows is — the same service the Kimi terminal performs.
    @Test
    void thePanelNamesTheGitBashClaudeCodeNeeds() {
        assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS, "the hint is a Windows matter");
        assumeTrue(!System.getenv().containsKey(ClaudeWebTui.BASH_VARIABLE),
                "this test is about the case where the operator said nothing");
        Optional<Path> bash = org.jackhuang.hmcl.util.platform.GitBash.find();
        assumeTrue(bash.isPresent(), "Git for Windows is not installed on this machine");

        Map<String, String> environment = new java.util.HashMap<>();
        ClaudeWebTui.applyShell(environment);

        assertEquals(bash.get().toString(), environment.get(ClaudeWebTui.BASH_VARIABLE));
        String normalized = environment.get(ClaudeWebTui.BASH_VARIABLE)
                .toLowerCase(java.util.Locale.ROOT).replace('\\', '/');
        assertFalse(normalized.contains("/windows/system32/"), environment.toString());
    }

    /// An address the operator named is the one that is used: pointing the agent at
    /// a different bash than the one they chose is not the panel's call.
    @Test
    void anOperatorsOwnShellIsNotOverridden() {
        Map<String, String> environment = new java.util.HashMap<>();
        environment.put(ClaudeWebTui.BASH_VARIABLE, "C:\\chosen\\bash.exe");

        ClaudeWebTui.applyShell(environment);

        assertEquals("C:\\chosen\\bash.exe", environment.get(ClaudeWebTui.BASH_VARIABLE));
    }

    /// A native binary runs as itself: node is not required to start it.
    @Test
    void theOfficialBinaryRunsDirectly() throws IOException {
        Path official = stageOfficial();

        TerminalTarget target = new ClaudeWebTui(ClaudeWebTui.OFFICIAL).target(
                packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "c1", null);

        assertEquals(1, target.command().size(), target.command().toString());
        assertEquals(official.toString(), target.command().get(0));
        assertEquals(packageDir.resolve("home").toString(), target.environment().get("HOME"));
    }

    /// The instance's endpoint override travels as `ANTHROPIC_BASE_URL` — the
    /// documented way around a region Anthropic does not serve — and only when
    /// it is actually set.
    @Test
    void anEndpointOverrideTravelsInTheEnvironment() throws IOException {
        assumeTrue(nodeOnPath(), "the engine is a JS entry and needs node");
        stageEngine();

        TerminalTarget withOverride = new ClaudeWebTui(ClaudeWebTui.COMMUNITY, "https://gateway.example.com")
                .target(packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "c1", null);
        assertEquals("https://gateway.example.com",
                withOverride.environment().get("ANTHROPIC_BASE_URL"));

        TerminalTarget without = new ClaudeWebTui(ClaudeWebTui.COMMUNITY).target(
                packageDir, packageDir.resolve("home"), packageDir.resolve("workspace"), "c1", null);
        assertFalse(without.environment().containsKey("ANTHROPIC_BASE_URL"),
                "an unset override must not shadow the tool's own default");
    }

    /// Anthropic answers 403 to unsupported regions and 401 to a supported one
    /// without a key; only the first is a refusal, and a missing body is still
    /// a refusal rather than "unknown".
    @Test
    void onlyAForbiddenAnswerMeansTheRegionIsNotServed() {
        assertTrue(ClaudeWebTui.regionBlocked(403,
                "{\"error\":\"Claude Code is not available in your country\"}"));
        assertTrue(ClaudeWebTui.regionBlocked(403, ""));
        assertFalse(ClaudeWebTui.regionBlocked(401,
                "{\"error\":{\"type\":\"authentication_error\"}}"));
        assertFalse(ClaudeWebTui.regionBlocked(200, "{}"));
    }

    /// The probe reports what the endpoint said, and gives up quietly when
    /// nothing answers: an unreachable endpoint must not become a refusal.
    @Test
    void theProbeReportsTheAnswerAndGivesUpQuietly() throws IOException {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer
                .create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/models", exchange -> {
            byte[] body = "Claude Code is not available in your country"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ClaudeWebTui.Probe probe = ClaudeWebTui.probeAnthropic(
                    "http://127.0.0.1:" + server.getAddress().getPort());
            assertEquals(403, probe.status(), probe.body());
            assertTrue(ClaudeWebTui.regionBlocked(probe.status(), probe.body()));
        } finally {
            server.stop(0);
        }

        // Nothing listening: the answer is "unknown", never a refusal.
        org.junit.jupiter.api.Assertions.assertNull(ClaudeWebTui.probeAnthropic("http://127.0.0.1:1"));
    }

    private static boolean nodeOnPath() {
        return resolves("node");
    }

    private static boolean claudeOnPath() {
        return resolves("claude");
    }

    private static boolean resolves(String program) {
        try {
            return Executables.resolve(program).isPresent();
        } catch (RuntimeException e) {
            return false;
        }
    }
}