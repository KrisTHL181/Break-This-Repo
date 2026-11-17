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
import org.jackhuang.hmcl.util.platform.Architecture;
import org.jackhuang.hmcl.util.platform.Executables;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/// Claude Web's interactive terminal **is** the Claude Code CLI it drives.
///
/// The workbench is a front end: the agent runs as a CLI child process, either
/// its own engine (`claude-web-engine`, a Claude Code fork that also speaks
/// OpenAI/Gemini providers) or the official binary. The two are not
/// interchangeable in practice — a provider configured in the browser only
/// works on the engine — so the caller picks one and this adapter never
/// substitutes the other.
///
/// The entry points mirror upstream `server/src/claude-exe.ts`:
///
/// - engine: `<release>/node_modules/claude-web-engine/dist/cli-node.js`, run
///   with `node` (the package ships a JS entry, not a `bin`) — the one on
///   `PATH`, else the runtime the panel installed (see [requireNode]);
/// - official: the SDK's platform package
///   `@anthropic-ai/claude-agent-sdk-<platform>-<arch>/claude[.exe]`, else a
///   `claude` on PATH (a global npm install).
@NotNullByDefault
public final class ClaudeWebTui implements BrandTui {

    /// The brand id used by the protocol, tickets and the audit trail.
    public static final String BRAND = "claude-web";

    /// The community engine bundled with the workbench. Preferred default: the
    /// browser's provider configuration was written for it.
    public static final String COMMUNITY = "community";

    /// The official Claude Code binary (Anthropic account only).
    public static final String OFFICIAL = "official";

    /// The engine's entry inside its package.
    static final String ENGINE_ENTRY = "node_modules/claude-web-engine/dist/cli-node.js";

    /// Where Claude Code looks for the bash its own tools run through on Windows:
    /// `CLAUDE_CODE_GIT_BASH_PATH` (its own error message names it). The panel can
    /// see where Git for Windows is, so it says so — the same service the Kimi
    /// terminal does under `KIMI_SHELL_PATH`.
    static final String BASH_VARIABLE = "CLAUDE_CODE_GIT_BASH_PATH";

    /// Points Claude Code at a Git Bash, unless the operator already said which
    /// shell to use (their own `CLAUDE_CODE_GIT_BASH_PATH`, from the panel's
    /// environment or the instance's).
    ///
    /// A no-op on every other platform, and on a Windows machine without Git for
    /// Windows: there is nothing to point at, and guessing at some other bash is
    /// how an agent ends up running WSL.
    ///
    /// @param environment the child's environment, modified in place
    public static void applyShell(Map<String, String> environment) {
        org.jackhuang.hmcl.util.platform.GitBash.applyTo(environment, BASH_VARIABLE);
    }

    /// One kernel the instance can be started on.
    ///
    /// @param id        `community` or `official`
    /// @param label     what the picker shows
    /// @param available whether an entry point was found
    /// @param detail    the resolved path, or why it is missing
    public record Kernel(String id, String label, boolean available, @Nullable String detail) {
    }

    private final @Nullable String kernel;
    private final @Nullable String baseUrl;

    public ClaudeWebTui(@Nullable String kernel) {
        this(kernel, null);
    }

    /// @param kernel  `community`, `official`, or `null` for the default
    /// @param baseUrl the instance's endpoint override, as `ANTHROPIC_BASE_URL`
    public ClaudeWebTui(@Nullable String kernel, @Nullable String baseUrl) {
        this.kernel = kernel;
        this.baseUrl = baseUrl;
    }

    /// Puts an instance's endpoint override into a child environment.
    ///
    /// Claude Web is the only brand that stores one: Anthropic refuses the
    /// official kernel outright in unsupported regions, and `ANTHROPIC_BASE_URL`
    /// is the documented way to point the CLI at a reachable gateway instead.
    /// A blank value changes nothing, so the tools keep their own default.
    ///
    /// @param environment the child environment to amend
    /// @param baseUrl     the configured endpoint, or `null`/blank
    public static void applyBaseUrl(Map<String, String> environment, @Nullable String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            environment.put("ANTHROPIC_BASE_URL", baseUrl.trim());
        }
    }

    /// Whether an endpoint's answer means "this region is not served".
    ///
    /// Anthropic answers **403** to requests from unsupported countries; a
    /// supported region without a key answers **401**. Only the 403 is turned
    /// into a refusal, and it is worded so it also fits a gateway that rejects
    /// the caller for its own reasons.
    ///
    /// @param status the HTTP status the probe saw
    /// @param body   the response body, may be blank
    /// @return whether the official kernel should not be started here
    public static boolean regionBlocked(int status, @Nullable String body) {
        if (status != 403) {
            return false;
        }
        String text = body == null ? "" : body.toLowerCase(java.util.Locale.ROOT);
        return text.contains("not available in your country")
                || text.contains("supported countries")
                || text.contains("unsupported")
                || text.contains("region")
                // No body, or nothing recognisable: a 403 from the API edge is
                // still a refusal, and the message below covers both readings.
                || text.isBlank();
    }

    /// The kernels this installation can start, community first.
    public static List<Kernel> kernels(Path packageDir) {
        List<Kernel> kernels = new ArrayList<>(2);
        kernels.add(probe(COMMUNITY, "社区引擎（claude-web-engine）", packageDir));
        kernels.add(probe(OFFICIAL, "官方 Claude Code", packageDir));
        return List.copyOf(kernels);
    }

    /// The kernel to use when the request names none: the community engine when
    /// it is there (providers configured in the browser keep working), else the
    /// official binary.
    public static @Nullable String defaultKernel(Path packageDir) {
        for (Kernel kernel : kernels(packageDir)) {
            if (kernel.available()) {
                return kernel.id();
            }
        }
        return null;
    }

    @Override
    public TerminalTarget target(Path packageDir, Path home, Path workspace,
                                 String instanceId, @Nullable String resumeSession) throws IOException {
        String requested = kernel == null || kernel.isBlank() ? defaultKernel(packageDir) : kernel;
        if (requested == null) {
            throw new IOException("Claude Code is not installed in this release: neither claude-web-engine"
                    + " nor the official binary was found (release " + packageDir + ")");
        }
        Kernel chosen = null;
        for (Kernel candidate : kernels(packageDir)) {
            if (candidate.id().equals(requested)) {
                chosen = candidate;
                break;
            }
        }
        if (chosen == null) {
            throw new IOException("unknown Claude Code kernel \"" + requested + "\" (expected "
                    + COMMUNITY + " or " + OFFICIAL + ")");
        }
        if (!chosen.available() || chosen.detail() == null) {
            throw new IOException(chosen.label() + " is not available: " + chosen.detail());
        }
        Path entry = Path.of(chosen.detail());

        Map<String, String> environment = new HashMap<>(BrandTui.environment(home));
        List<String> command = new ArrayList<>();
        if (isJavaScript(entry)) {
            // The engine is a JS entry: without node there is nothing to run,
            // so a missing runtime fails here rather than inside the PTY.
            Path node = requireNode();
            command.add(node.toString());
            environment.put("PATH", node.getParent() + File.pathSeparator
                    + environment.getOrDefault("PATH", ""));
        } else {
            // A native binary (the official one) does not need node; put one on
            // the child's PATH when there is any — the panel's managed runtime
            // counts — but never fail over it.
            putNodeOnPath(environment);
        }
        command.add(entry.toString());
        applyBaseUrl(environment, baseUrl);
        // Claude Code runs its own shell tools through a bash on Windows and
        // refuses the session without one ("Claude Code on Windows requires
        // git-bash"), naming this variable in the refusal.
        applyShell(environment);
        return new TerminalTarget(BRAND, instanceId, command, workspace, Map.copyOf(environment));
    }

    /// What an endpoint answered when asked whether it serves this panel.
    ///
    /// @param status the HTTP status
    /// @param body   the response body, possibly blank
    public record Probe(int status, String body) {
    }

    /// Asks the endpoint whether it serves this panel at all: `GET <base>/v1/models`
    /// without a key. An unsupported region answers **403**; a supported one
    /// answers **401**, which is the healthy answer for a panel with no key.
    ///
    /// @param baseUrl the instance's endpoint, or `null`/blank for api.anthropic.com
    /// @return what it answered, or `null` when the request never completed
    public static @Nullable Probe probeAnthropic(@Nullable String baseUrl) {
        String base = baseUrl == null || baseUrl.isBlank() ? "https://api.anthropic.com" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();
            java.net.http.HttpRequest request = java.net.http.HttpRequest
                    .newBuilder(java.net.URI.create(base + "/v1/models"))
                    .timeout(java.time.Duration.ofSeconds(4))
                    .header("anthropic-version", "2023-06-01")
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> response =
                    client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body() == null ? "" : response.body();
            // The region note is at the front; the rest is not worth keeping.
            return new Probe(response.statusCode(), body.length() > 4096 ? body.substring(0, 4096) : body);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (RuntimeException | java.io.IOException e) {
            // Offline, DNS, TLS or timeout: unknown, and the CLI explains it.
            return null;
        }
    }

    // ------------------------------------------------------------------ probe --

    private static Kernel probe(String id, String label, Path packageDir) {
        try {
            Path entry = id.equals(COMMUNITY) ? communityEntry(packageDir) : officialEntry(packageDir);
            return new Kernel(id, label, true, entry.toAbsolutePath().normalize().toString());
        } catch (IOException e) {
            return new Kernel(id, label, false, e.getMessage());
        }
    }

    /// `node_modules/claude-web-engine/dist/cli-node.js`, exactly as upstream
    /// resolves it — the package has no `bin` field to read.
    private static Path communityEntry(Path packageDir) throws IOException {
        Path entry = packageDir.resolve(ENGINE_ENTRY);
        if (!Files.isRegularFile(entry)) {
            throw new IOException("claude-web-engine is not installed (missing " + entry + ")");
        }
        return entry;
    }

    /// The official binary: the SDK's platform package inside the release, else
    /// a `claude` on PATH.
    private static Path officialEntry(Path packageDir) throws IOException {
        Path bundled = packageDir.resolve("node_modules")
                .resolve(officialPackageName())
                .resolve(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS ? "claude.exe" : "claude");
        if (Files.isRegularFile(bundled)) {
            return bundled;
        }
        return Executables.resolve("claude")
                .orElseThrow(() -> new IOException("no " + officialPackageName() + " in the release and no"
                        + " `claude` on PATH (install it with: npm i -g @anthropic-ai/claude-code)"));
    }

    /// The npm package that carries the official binary for this platform:
    /// `@anthropic-ai/claude-agent-sdk-<platform>-<arch>`.
    static String officialPackageName() {
        return "@anthropic-ai/claude-agent-sdk-" + nodePlatform() + "-" + nodeArch();
    }

    static String nodePlatform() {
        return switch (OperatingSystem.CURRENT_OS) {
            case WINDOWS -> "win32";
            case MACOS -> "darwin";
            default -> "linux";
        };
    }

    static String nodeArch() {
        return switch (Architecture.CURRENT_ARCH) {
            case X86_64 -> "x64";
            case ARM64 -> "arm64";
            case X86 -> "ia32";
            default -> "arm";
        };
    }

    private static boolean isJavaScript(Path entry) {
        String name = entry.getFileName().toString();
        return name.endsWith(".js") || name.endsWith(".mjs") || name.endsWith(".cjs");
    }

    private static Path requireNode() throws IOException {
        return requireNode(DshNodeRuntime::detect, DshNodeRuntime::detectManaged);
    }

    /// The same decision with its two lookups passed in, so a machine's own state
    /// (does it have a Node on `PATH`? does the panel have a managed one?) is not
    /// what a test depends on.
    ///
    /// The order is `PATH` first, then the runtimes the panel installed under
    /// `<data>/hdsl/runtimes`. A bare Windows deployment has only the second — the
    /// panel installs it so nobody has to install Node — and asking `PATH` alone
    /// is why the community engine failed with "node is not on PATH" while a
    /// perfectly good runtime sat in the data directory, next to the one the Kimi
    /// and ZCode terminals already used.
    ///
    /// @param onPath  what `node` on `PATH` resolves to
    /// @param managed the runtimes the panel installed
    /// @return a program that can actually be started
    /// @throws IOException when neither lookup has one
    static Path requireNode(Supplier<Optional<DshNodeRuntime>> onPath,
                            Supplier<Optional<DshNodeRuntime>> managed) throws IOException {
        DshNodeRuntime runtime = onPath.get().or(managed).orElse(null);
        if (runtime == null) {
            throw new IOException("no Node runtime: none on PATH and none installed by the panel"
                    + " (Settings → Node.js installs one, and the community engine needs it)");
        }
        return runtime.node();
    }

    /// Puts whichever Node the child could use on its `PATH`, without requiring
    /// one: the official kernel runs as a native binary, but the tools it shells
    /// out to still look for `node`.
    ///
    /// @param environment the child environment to amend
    private static void putNodeOnPath(Map<String, String> environment) {
        DshNodeRuntime.detectSupported().ifPresent(runtime ->
                environment.put("PATH", runtime.node().getParent() + File.pathSeparator
                        + environment.getOrDefault("PATH", "")));
    }
}