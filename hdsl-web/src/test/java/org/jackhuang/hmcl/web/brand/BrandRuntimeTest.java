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
package org.jackhuang.hmcl.web.brand;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Launch-command construction and the readiness-line handling of the brand
/// runtime — the two places where an upstream flag drift would break launching
/// silently.
class BrandRuntimeTest {

    @TempDir
    Path dir;

    @Test
    void kimiCommandCarriesTheProxyContract() throws IOException {
        Path pkg = prefix(Brand.KIMI);
        BrandInstance instance = new BrandInstance("abc", "kimi", "demo", "2.1.1", null, 0, 1, null);

        List<String> command = BrandRuntime.command(Brand.KIMI, pkg, instance, "dsh.example.com");

        assertEquals(pkg.resolve("node_modules/.bin/kimi").toRealPath().toString(), command.get(0));
        assertTrue(command.contains("web"), command.toString());
        assertTrue(command.contains("--no-open"), command.toString());
        // Kimi picks its own ephemeral port and announces it — port 0 here.
        assertEquals("0", command.get(command.indexOf("--port") + 1), command.toString());
        assertTrue(command.contains("--host"), command.toString());
        // The DNS-rebinding allowlist carries the browser-facing host.
        assertEquals("dsh.example.com", command.get(command.indexOf("--allowed-host") + 1), command.toString());
        assertTrue(command.contains("--dangerous-bypass-auth"), command.toString());
    }

    @Test
    void kimiCommandOmitsTheAllowlistWithoutAHost() throws IOException {
        Path pkg = prefix(Brand.KIMI);
        BrandInstance instance = new BrandInstance("abc", "kimi", "demo", "2.1.1", null, 0, 1, null);

        List<String> command = BrandRuntime.command(Brand.KIMI, pkg, instance, null);

        assertFalse(command.contains("--allowed-host"), command.toString());
    }

    @Test
    void opencodeCommandUsesAPanelAllocatedPort() throws IOException {
        Path pkg = prefix(Brand.OPENCODE);
        BrandInstance instance = new BrandInstance("abc", "opencode", "demo", "1.18.35", null, 0, 1, null);

        List<String> command = BrandRuntime.command(Brand.OPENCODE, pkg, instance, null);

        int port = Integer.parseInt(command.get(command.indexOf("--port") + 1));
        assertTrue(port > 0, "a real pre-allocated port, not 0: " + command);
        assertEquals("127.0.0.1", command.get(command.indexOf("--hostname") + 1), command.toString());
    }

    @Test
    void commandRefusesAPrefixWithoutTheExecutable() {
        BrandInstance instance = new BrandInstance("abc", "kimi", "demo", "2.1.1", null, 0, 1, null);

        assertThrows(IOException.class,
                () -> BrandRuntime.command(Brand.KIMI, dir, instance, null));
    }

    @Test
    void claudeWebPlanRunsTsxWithAPortEnvironment() throws IOException {
        Path pkg = Files.createDirectories(dir.resolve("claude-web-rel"));
        Path tsx = pkg.resolve("node_modules/.bin/tsx");
        Path entry = pkg.resolve("server/src/index.ts");
        Files.createDirectories(tsx.getParent());
        Files.createDirectories(entry.getParent());
        createExecutable(tsx);
        Files.createFile(entry);
        BrandInstance instance = new BrandInstance("abc", "claude-web", "demo", "0.2.1", null, 0, 1, null);

        BrandRuntime.LaunchPlan plan = BrandRuntime.plan(Brand.CLAUDE_WEB, pkg, instance, null);

        assertEquals(tsx.toRealPath().toString(), plan.command().get(0));
        assertEquals(entry.toString(), plan.command().get(1));
        assertEquals(2, plan.command().size());
        // The pre-allocated port travels in the environment — the server reads
        // PORT (verified in its source: PORT ?? 3090) and has no port flag.
        int port = Integer.parseInt(plan.environment().get("PORT"));
        assertTrue(port > 0, "a real pre-allocated port in the environment: " + plan.environment());
    }

    /// The instance's endpoint override reaches the browser mode too: the
    /// workbench drives the same CLI, so a gateway configured for the terminal
    /// has to serve the web mode as well.
    @Test
    void claudeWebPlanCarriesTheInstancesEndpointOverride() throws IOException {
        Path pkg = Files.createDirectories(dir.resolve("claude-web-rel"));
        Path tsx = pkg.resolve("node_modules/.bin/tsx");
        Path entry = pkg.resolve("server/src/index.ts");
        Files.createDirectories(tsx.getParent());
        Files.createDirectories(entry.getParent());
        createExecutable(tsx);
        Files.createFile(entry);
        BrandInstance instance = new BrandInstance("abc", "claude-web", "demo", "0.2.1",
                "https://gateway.example.com", 0, 1, null);

        BrandRuntime.LaunchPlan plan = BrandRuntime.plan(Brand.CLAUDE_WEB, pkg, instance, null);

        assertEquals("https://gateway.example.com", plan.environment().get("ANTHROPIC_BASE_URL"));
        assertTrue(plan.environment().containsKey("PORT"), plan.environment().toString());

        // Unset stays unset: the CLI keeps its own default.
        BrandInstance plain = new BrandInstance("abc", "claude-web", "demo", "0.2.1", null, 0, 1, null);
        assertFalse(BrandRuntime.plan(Brand.CLAUDE_WEB, pkg, plain, null)
                .environment().containsKey("ANTHROPIC_BASE_URL"));
    }

    @Test
    void claudeWebPlanRefusesAPrefixWithoutTheServerEntry() throws IOException {
        Path pkg = Files.createDirectories(dir.resolve("claude-web-rel"));
        Path tsx = pkg.resolve("node_modules/.bin/tsx");
        Files.createDirectories(tsx.getParent());
        createExecutable(tsx);
        BrandInstance instance = new BrandInstance("abc", "claude-web", "demo", "0.2.1", null, 0, 1, null);

        assertThrows(IOException.class,
                () -> BrandRuntime.plan(Brand.CLAUDE_WEB, pkg, instance, null));
    }

    @Test
    void hermesPlanPassesTheWrapperItsFiveArguments() throws IOException {
        Path brandRoot = Files.createDirectories(dir.resolve("hermes"));
        BrandInstanceManager manager = new BrandInstanceManager(Brand.HERMES, brandRoot.resolve("instances"));
        BrandInstance instance = new BrandInstance("abc", "hermes", "demo", "0.22.5", null, 0, 1, null);
        Path pkg = Files.createDirectories(dir.resolve("hermes-rel"));

        BrandRuntime.LaunchPlan plan = BrandRuntime.hermesPlan(manager, instance, pkg);

        List<String> command = plan.command();
        assertEquals("/bin/sh", command.get(0));
        assertEquals(manager.instanceDirectory(instance.id()).resolve("panel/wrapper.sh").toString(),
                command.get(1));
        assertEquals(manager.homeDirectory(instance).toString(), command.get(2));
        assertTrue(command.get(3).matches("\\d+"), command.toString());
        assertTrue(command.get(4).matches("\\d+"), command.toString());
        assertEquals(pkg.resolve("hermes_cli/web_dist").toString(), command.get(5));
        assertEquals(pkg.resolve(".venv").toString(), command.get(6));
        assertEquals(7, command.size(), command.toString());
    }

    @Test
    void npmBrandPlansCarryNoEnvironment() throws IOException {
        Path pkg = prefix(Brand.KIMI);
        BrandInstance instance = new BrandInstance("abc", "kimi", "demo", "2.1.1", null, 0, 1, null);

        // The one thing a plan may carry is where kimi's shell is: on Windows it does
        // not look for a bash itself, and without the hint its first tool call fails.
        // Nothing else belongs here — no port, no base url: those are flags or npm's
        // own business, and an environment key added here outlives its reason.
        Map<String, String> environment = BrandRuntime.plan(Brand.KIMI, pkg, instance, null).environment();
        Set<String> unexpected = new TreeSet<>(environment.keySet());
        unexpected.remove("KIMI_SHELL_PATH");
        assertTrue(unexpected.isEmpty(), "the plan carries an environment key it has no reason to: "
                + environment);
        if (environment.containsKey("KIMI_SHELL_PATH")) {
            assertTrue(environment.get("KIMI_SHELL_PATH").toLowerCase(java.util.Locale.ROOT)
                    .endsWith("bash.exe"), environment.toString());
        }
    }

    /// The workspace registration is the tool's own wire format — an envelope
    /// wrapper this test pins, because getting it wrong makes the request a
    /// silent no-op on the instance's side.
    @Test
    void workspaceRegistrationUsesTheToolsEnvelope() {
        String json = BrandRuntime.workspaceAddRequest("/data/instances/abc/workspace");

        com.google.gson.JsonObject envelope = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        assertEquals("request", envelope.get("type").getAsString());
        com.google.gson.JsonObject request = envelope.getAsJsonObject("request");
        assertTrue(!request.get("id").getAsString().isEmpty(), json);
        com.google.gson.JsonObject req = request.getAsJsonObject("req");
        assertEquals("workspaces.add", req.get("kind").getAsString());
        assertEquals("/data/instances/abc/workspace", req.get("path").getAsString());
    }

    @Test
    void ansiEscapesAreStrippedBeforeMatching() {
        String banner = "\u001B[94m\u001B[1m  Web interface:     \u001B[0m http://127.0.0.1:45678/";
        String stripped = BrandRuntime.stripAnsi(banner);

        assertEquals("  Web interface:      http://127.0.0.1:45678/", stripped);
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("Web interface:\\s+http://127\\.0\\.0\\.1:(\\d+)/")
                        .matcher(stripped);
        assertTrue(matcher.find());
        assertEquals("45678", matcher.group(1));
    }

    @Test
    void stripAnsiLeavesPlainLinesAlone() {
        assertEquals("Local: http://127.0.0.1:35849/",
                BrandRuntime.stripAnsi("Local: http://127.0.0.1:35849/"));
    }

    /// Creates a fake binary and gives it the execute bit a real pnpm install gives it.
    ///
    /// On POSIX a program is a file with the execute bit — the resolver checks for it (as
    /// `which` always did), so a fixture created without one is not a program at all.
    /// Windows has no mode bits; what can be started there is decided by extension.
    private static Path createExecutable(Path path) throws IOException {
        Files.createFile(path);
        try {
            Files.setPosixFilePermissions(path,
                    java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        } catch (UnsupportedOperationException e) {
            // Windows: the extension is the whole story.
        }
        return path;
    }

    /// Creates a fake installed prefix holding only the executable the command
    /// builder looks for.
    private Path prefix(Brand brand) throws IOException {
        Path pkg = Files.createDirectories(dir.resolve(brand.id() + "-rel"));
        Path bin = pkg.resolve("node_modules/.bin/" + brand.binName());
        Files.createDirectories(bin.getParent());
        createExecutable(bin);
        return pkg;
    }
}
