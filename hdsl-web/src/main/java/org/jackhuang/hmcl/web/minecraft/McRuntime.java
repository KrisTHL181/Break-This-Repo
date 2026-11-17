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
package org.jackhuang.hmcl.web.minecraft;

import org.jackhuang.hmcl.util.ProcessTree;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.util.platform.Privileges;
import org.jackhuang.hmcl.util.platform.VirtualThreads;
import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.jackhuang.hmcl.web.terminal.TerminalSession;
import org.jackhuang.hmcl.web.terminal.TerminalSink;
import org.jackhuang.hmcl.web.terminal.TerminalTarget;
import org.jackhuang.hmcl.web.terminal.pty.PtySize;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Starts, stops and watches one Minecraft server per instance.
///
/// The process is a **terminal session**, not a bare `Process`: the console is
/// the existing PTY, so the browser gets history, resize and multi-viewer
/// attach for free, and the panel's shutdown takes the server with it. The only
/// thing added here is the Minecraft-specific part — the argv, the readiness
/// line, and the state the UI polls.
///
/// The console is attach-only: [console] never starts a server. That split is
/// what keeps "open the console" from becoming "start a service" (see
/// `docs/minecraft.md` §6.2.1).
@NotNullByDefault
public final class McRuntime {

    /// The terminal brand every Minecraft session is registered under.
    public static final String BRAND = "minecraft";

    /// How long a launch may take to print its readiness line.
    ///
    /// The first launch unpacks the bundler's libraries and generates the world;
    /// on a NAS's disks that is minutes, not seconds — and the marker (`Done (`)
    /// only appears after world generation. Five minutes is the ceiling before
    /// the panel decides the server is not coming up.
    public static final int READY_TIMEOUT_SECONDS = 300;
    /// How long `stop` waits for the server to exit before it is killed.
    public static final int STOP_GRACE_SECONDS = 30;

    /// What the panel shows beside an instance.
    public enum State {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        FAILED
    }

    /// The state and the reason it is not running.
    ///
    /// @param state the state
    /// @param error the last failure, or `null`
    public record Status(State state, @Nullable String error) {
    }

    /// Builds the argv for one instance. Production adds the heap and `nogui`;
    /// tests substitute a fake server.
    @FunctionalInterface
    public interface CommandFactory {
        List<String> command(McInstance instance);
    }

    private static final org.jackhuang.hmcl.util.logging.Logger LOG =
            org.jackhuang.hmcl.util.logging.Logger.LOG;

    private final McInstanceManager manager;
    private final TerminalManager terminals;
    private final McFeatureState feature;
    private final CommandFactory commandFactory;
    private final @Nullable String javaBin;
    private final @Nullable Path versionsRoot;
    /// Where the panel's own JREs live (`<data>/minecraft/java`), or `null` when
    /// this wiring cannot manage them (tests, embedded).
    private final @Nullable Path javaRoot;
    private final boolean ptySupported;
    private final String javaSource;

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final Map<String, String> errors = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> readiness = new ConcurrentHashMap<>();
    /// Windows has no PTY; those servers run as a plain child process with
    /// piped stdio. The console is then the log + a command box, not a terminal
    /// (docs/minecraft.md §15.2). Kept separate from [ptySupported] because
    /// these two maps are the authoritative record of what is actually running.
    private final Map<String, PipeServer> pipes = new ConcurrentHashMap<>();
    private static final Map<String, JavaProbe> JAVA_PROBES = new ConcurrentHashMap<>();

    /// What the panel knows about the `java` it will start servers with.
    ///
    /// @param path    the resolved executable, or `""` when none could be resolved
    /// @param version the `java -version` string, or `null` when it could not be read
    /// @param major   the parsed major version (8 for `1.8.0_…`), or `0`
    /// @param source  `panel`, `configured`, `installed`, or `none`
    /// @param ok      whether `-version` actually ran
    /// @param error   why it is not ok, or `null`
    public record JavaInfo(String path, @Nullable String version, int major,
                           String source, boolean ok, @Nullable String error) {
    }

    private record JavaProbe(@Nullable String version, int major, @Nullable String error) {
    }

    /// Whether this platform's console can be a real PTY.
    ///
    /// Windows deliberately stays on the pipe console in this round: ConPTY is
    /// implemented for instance terminals, but switching Minecraft's console is
    /// a separate behaviour change that needs a real-Windows pass
    /// (`docs/minecraft.md` §15.2). Tests inject `false` to exercise the pipe
    /// path on Linux.
    static boolean ptySupported() {
        return OperatingSystem.CURRENT_OS == OperatingSystem.LINUX
                || OperatingSystem.CURRENT_OS == OperatingSystem.MACOS;
    }

    /// The console shape this platform can offer, for the feature card and the
    /// console panel to say before a server is even running.
    ///
    /// @return `"pty"` or `"pipe"`
    public static String consoleKind() {
        return ptySupported() ? "pty" : "pipe";
    }

    /// The production wiring: `java -Xmx<memory>M -jar server.jar nogui`.
    ///
    /// @param manager   the instance manager
    /// @param terminals the terminal registry
    /// @param feature   the feature switch (start is refused while disabled or locked)
    /// @param javaBin   the java executable
    public McRuntime(McInstanceManager manager, TerminalManager terminals,
                     McFeatureState feature, String javaBin) {
        this(manager, terminals, feature, javaBin, null);
    }

    /// The production wiring plus the version metadata directory, so a launch
    /// can refuse a Java too old for the version before it starts.
    ///
    /// @param manager      the instance manager
    /// @param terminals    the terminal registry
    /// @param feature      the feature switch
    /// @param javaBin      the java executable (resolved by [resolveJava])
    /// @param versionsRoot the shared versions directory, or `null` to skip the check
    public McRuntime(McInstanceManager manager, TerminalManager terminals,
                     McFeatureState feature, String javaBin, @Nullable Path versionsRoot) {
        this(manager, terminals, feature, javaBin, versionsRoot, null);
    }

    /// The production wiring plus the version metadata directory and the
    /// panel-owned Java runtimes, so a launch can pick a JRE the panel
    /// installed instead of only whatever the host happens to carry.
    ///
    /// @param manager      the instance manager
    /// @param terminals    the terminal registry
    /// @param feature      the feature switch
    /// @param javaBin      the java executable (resolved by [resolveJava])
    /// @param versionsRoot the shared versions directory, or `null` to skip the check
    /// @param javaRoot     the managed JRE directory, or `null` to only use the host's
    public McRuntime(McInstanceManager manager, TerminalManager terminals,
                     McFeatureState feature, String javaBin, @Nullable Path versionsRoot,
                     @Nullable Path javaRoot) {
        this(manager, terminals, feature, null, resolveJava(javaBin), versionsRoot,
                ptySupported(), javaSource(javaBin), javaRoot);
    }

    /// The same, with an injectable argv for tests.
    ///
    /// @param manager        the instance manager
    /// @param terminals      the terminal registry
    /// @param feature        the feature switch
    /// @param commandFactory the argv builder
    public McRuntime(McInstanceManager manager, TerminalManager terminals,
                     McFeatureState feature, CommandFactory commandFactory) {
        this(manager, terminals, feature, commandFactory, null, null, ptySupported(), "none", null);
    }

    /// Test wiring that forces the pipe console on any platform, so the
    /// Windows path is covered by the Linux test suite.
    ///
    /// @param manager        the instance manager
    /// @param terminals      the terminal registry
    /// @param feature        the feature switch
    /// @param commandFactory the argv builder
    /// @param ptySupported   `false` to exercise the pipe console
    McRuntime(McInstanceManager manager, TerminalManager terminals,
              McFeatureState feature, CommandFactory commandFactory, boolean ptySupported) {
        this(manager, terminals, feature, commandFactory, null, null, ptySupported, "none", null);
    }

    private McRuntime(McInstanceManager manager, TerminalManager terminals, McFeatureState feature,
                      @Nullable CommandFactory commandFactory, @Nullable String javaBin,
                      @Nullable Path versionsRoot, boolean ptySupported, String javaSource,
                      @Nullable Path javaRoot) {
        this.manager = manager;
        this.terminals = terminals;
        this.feature = feature;
        this.javaBin = javaBin;
        this.versionsRoot = versionsRoot;
        this.javaRoot = javaRoot;
        this.ptySupported = ptySupported;
        this.javaSource = javaSource;
        CommandFactory factory = commandFactory;
        if (factory == null && javaBin != null) {
            factory = instance -> {
                List<String> command = new ArrayList<>();
                command.add(javaFor(instance));
                command.addAll(heapArgs(instance.memoryMb()));
                // The pipe pump decodes UTF-8. On JDK 17 + a Chinese Windows
                // code page the child would otherwise write GBK (JEP 400 made
                // UTF-8 the default only in 18). Keep both sides on one charset
                // instead of decoding differently in different places.
                command.add("-Dfile.encoding=UTF-8");
                command.add("-Dsun.stdout.encoding=UTF-8");
                command.add("-Dsun.stderr.encoding=UTF-8");
                // Forge and NeoForge do not run from a jar: their installer leaves
                // a Java argument file (`@libraries/…/unix_args.txt`) or, before
                // 1.17, a launcher jar. Both are discovered per instance, so a
                // core that was never installed falls through to the jar path and
                // fails there with the usual "server.jar is missing".
                java.util.List<String> launch = McForge.launchArguments(
                        manager.instanceDirectory(instance.id()));
                if (launch != null) {
                    command.addAll(launch);
                    return command;
                }
                command.add("-jar");
                // Absolute: a command line that names the instance directory is
                // what `ProcessTree.killReferencing` uses to reap an orphan on
                // Windows after a panel crash.
                command.add(manager.serverJar(instance).toString());
                command.add("nogui");
                return command;
            };
        }
        if (factory == null) {
            throw new IllegalArgumentException("a command factory is required when no java executable is configured");
        }
        this.commandFactory = factory;
    }

    /// `-Xms` must never exceed `-Xmx`: the create form accepts 256 MB, and a
    /// fixed `-Xms512M` made every such server die with "Initial heap size set
    /// to a larger value than the maximum heap size" before Minecraft printed
    /// anything.
    ///
    /// @param memoryMb the instance's configured heap ceiling
    /// @return the two heap arguments
    static List<String> heapArgs(int memoryMb) {
        int maximum = Math.max(256, memoryMb);
        int initial = Math.min(512, maximum);
        return List.of("-Xms" + initial + "M", "-Xmx" + maximum + "M");
    }

    private static String javaSource(@Nullable String configured) {
        if (configured == null || configured.isBlank()
                || "java".equals(configured) || "java.exe".equals(configured)) {
            return "panel";
        }
        return "configured";
    }

    /// Turns the configured `mc.java_path` into something `posix_spawn` accepts.
    ///
    /// The console is a PTY on POSIX, and the PTY spawn hands the first argv
    /// element to `posix_spawn` as-is: unlike a shell or `ProcessBuilder` it does
    /// **no** `PATH` lookup (glibc's `posix_spawn` is `execve`; the searching
    /// variant is `posix_spawnp`). The default `mc.java_path: java` therefore
    /// died with ENOENT before the server printed a byte — the UI could only say
    /// "failed to start the server process".
    ///
    /// Order: an explicit path is used as given; `java` means the JVM this panel
    /// is running on; anything else is looked up through `JAVA_HOME` and then
    /// `PATH`. When nothing is found the value is returned unchanged, so the
    /// error still names what the operator configured.
    ///
    /// @param configured `mc.java_path`, or `null`/blank for the default
    /// @return an absolute path when one exists, otherwise the input
    static String resolveJava(@Nullable String configured) {
        String program = configured == null || configured.isBlank() ? "java" : configured;
        if (program.indexOf('/') >= 0 || program.indexOf('\\') >= 0) {
            return Path.of(program).toAbsolutePath().toString();
        }
        if ("java".equals(program) || "java.exe".equals(program)) {
            String home = System.getProperty("java.home");
            if (home != null && !home.isBlank()) {
                for (String name : List.of("java", "java.exe")) {
                    Path candidate = Path.of(home, "bin", name);
                    if (Files.isRegularFile(candidate)) {
                        return candidate.toString();
                    }
                }
            }
        }
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            for (String name : executableNames(program)) {
                Path candidate = Path.of(javaHome, "bin", name);
                if (Files.isRegularFile(candidate)) {
                    return candidate.toAbsolutePath().toString();
                }
            }
        }
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(Pattern.quote(java.io.File.pathSeparator))) {
                if (dir.isBlank()) {
                    continue;
                }
                for (String name : executableNames(program)) {
                    Path candidate = Path.of(dir, name);
                    if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                        return candidate.toAbsolutePath().toString();
                    }
                }
            }
        }
        return program;
    }

    private static List<String> executableNames(String program) {
        if (program.toLowerCase(java.util.Locale.ROOT).endsWith(".exe")) {
            return List.of(program);
        }
        return List.of(program, program + ".exe");
    }

    /// The Java this runtime will start servers with, or why it cannot.
    ///
    /// @return the probe result; `-version` is cached per executable
    public JavaInfo javaInfo() {
        if (javaBin == null) {
            return new JavaInfo("", null, 0, javaSource, false,
                    "no Java executable is configured for Minecraft servers");
        }
        JavaProbe probe = probe(javaBin);
        return new JavaInfo(javaBin, probe.version(), probe.major(), javaSource,
                probe.error() == null, probe.error());
    }

    /// The `java` on `PATH`, whatever Minecraft is configured to use.
    ///
    /// Display only: the settings page shows it beside the panel's own JVM so an
    /// operator can see what the host carries, and neither is ever installed,
    /// removed or pointed at by this panel.
    ///
    /// @return the PATH java, or a probe that says why there is none
    public static JavaInfo pathJavaInfo() {
        JavaProbe probe = probe("java");
        return new JavaInfo("java", probe.version(), probe.major(), "installed",
                probe.error() == null, probe.error());
    }

    /// The Java an instance will actually use, probed, with where it came from.
    ///
    /// @param instance the instance
    /// @return the probe result
    public JavaInfo javaInfoFor(McInstance instance) {
        String program = javaFor(instance);
        if (program == null) {
            return new JavaInfo("", null, 0, "none", false,
                    "no Java executable is configured for Minecraft servers");
        }
        JavaProbe probe = probe(program);
        return new JavaInfo(program, probe.version(), probe.major(),
                sourceOf(instance, program), probe.error() == null, probe.error());
    }

    /// Every Java the advanced settings may pin an instance to.
    ///
    /// The panel's own JVM first (the combination this build was tested with),
    /// then the JREs the panel installed, then the host's JDKs. Paths are unique;
    /// the list is only a menu, not a trust boundary — the caller still names one
    /// of these paths, and the probe above is what decides whether it runs.
    ///
    /// @return the candidates, panel first
    List<JavaInfo> javaOptions() {
        Map<String, JavaInfo> byPath = new LinkedHashMap<>();
        if (javaBin != null) {
            JavaInfo own = javaInfo();
            byPath.put(own.path(), own);
        }
        for (Path candidate : managedCandidates(javaRoot)) {
            String path = candidate.toString();
            byPath.computeIfAbsent(path, ignored -> probeInfo(path, "managed"));
        }
        for (Path candidate : javaCandidates()) {
            String path = candidate.toString();
            byPath.computeIfAbsent(path, ignored -> probeInfo(path, "installed"));
        }
        return List.copyOf(byPath.values());
    }

    private static JavaInfo probeInfo(String program, String source) {
        JavaProbe probe = probe(program);
        return new JavaInfo(program, probe.version(), probe.major(), source,
                probe.error() == null, probe.error());
    }

    /// The `java` an instance is actually launched with.
    ///
    /// An instance's own pin wins outright — that is what the advanced setting is
    /// for. Next is `mc.java_path` (an operator who named a JDK meant it), then a
    /// JVM that already satisfies the version. The default is the panel's own JVM
    /// (the combination this panel was tested with), except when the instance's
    /// version asks for a newer major than that JVM offers **and** a suitable
    /// JRE/JDK is available: first the ones this panel installed itself
    /// (`<data>/minecraft/java`), then the ones the host carries. Since 26.x
    /// Minecraft requires Java 25 while the official image's default JVM is 21:
    /// "install one and point mc.java_path at it" is not advice a container user
    /// can follow, and refusing every newer version on an image that already
    /// ships the JDK is the wrong answer.
    ///
    /// @param instance the instance about to start
    /// @return the java executable
    String javaFor(McInstance instance) {
        String pinned = instance.javaPath();
        if (pinned != null && !pinned.isBlank()) {
            return pinned;
        }
        if (javaBin == null || !"panel".equals(javaSource)) {
            return javaBin;
        }
        int required = requiredJava(instance.version());
        if (required <= 0) {
            return javaBin;
        }
        List<Path> candidates = new ArrayList<>();
        candidates.addAll(managedCandidates(javaRoot));
        candidates.addAll(javaCandidates());
        return selectJava(javaBin, javaInfo().major(), required, candidates);
    }

    /// The JREs the panel installed under `<data>/minecraft/java`, as executables.
    ///
    /// @param javaRoot the managed directory, or `null`
    /// @return the `bin/java` executables, oldest feature release first
    static List<Path> managedCandidates(@Nullable Path javaRoot) {
        if (javaRoot == null || !Files.isDirectory(javaRoot)) {
            return List.of();
        }
        List<Path> found = new ArrayList<>();
        try (var entries = Files.list(javaRoot)) {
            for (Path directory : entries.filter(Files::isDirectory).toList()) {
                if (!directory.getFileName().toString().matches("\\d+")) {
                    continue;
                }
                for (String name : List.of("java", "java.exe")) {
                    Path candidate = directory.resolve("bin").resolve(name);
                    if (Files.isRegularFile(candidate)) {
                        found.add(candidate);
                        break;
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not scan " + javaRoot + " for managed JREs: " + e.getMessage());
        }
        found.sort(Comparator.comparing(path -> path.toString()));
        return List.copyOf(found);
    }

    /// The selection itself, with the installed JDKs passed in so a test can drive it.
    ///
    /// The **lowest** JDK that satisfies the version is chosen, not the newest: a
    /// version that asks for 25 is not asking for 29.
    ///
    /// @param chosen      the panel's own java
    /// @param chosenMajor its major version, or `0` when unknown
    /// @param required    what the Minecraft version asks for
    /// @param candidates  installed JDKs to consider
    /// @return the java to run
    static String selectJava(String chosen, int chosenMajor, int required, List<Path> candidates) {
        if (chosenMajor > 0 && chosenMajor >= required) {
            return chosen;
        }
        Path best = null;
        int bestMajor = Integer.MAX_VALUE;
        for (Path candidate : candidates) {
            String program = candidate.toString();
            if (program.equals(chosen)) {
                continue;
            }
            int major = probe(program).major();
            if (major >= required && major < bestMajor) {
                best = candidate;
                bestMajor = major;
            }
        }
        return best == null ? chosen : best.toString();
    }

    /// The JDKs installed beside the panel, for the versions that need a newer one.
    ///
    /// Linux keeps them under `/usr/lib/jvm` (Debian, RHEL) or `/usr/lib64/jvm`
    /// (SUSE); macOS under `/Library/Java/JavaVirtualMachines`. Windows is not
    /// scanned: a JDK there is a directory somebody unzipped somewhere, and
    /// `mc.java_path` is the documented way to name it.
    ///
    /// @return every `bin/java` found, at most one JVM directory below each root
    static List<Path> javaCandidates() {
        List<Path> found = new ArrayList<>();
        switch (OperatingSystem.CURRENT_OS) {
            case LINUX -> {
                // <root>/<jdk>/bin/java — depth 3, counting the root as 0.
                for (String root : List.of("/usr/lib/jvm", "/usr/lib64/jvm")) {
                    collectJava(Path.of(root), 3, found);
                }
            }
            // <root>/<jdk>/Contents/Home/bin/java
            case MACOS -> collectJava(Path.of("/Library/Java/JavaVirtualMachines"), 5, found);
            default -> {
            }
        }
        found.sort(java.util.Comparator.comparing(Path::toString));
        return List.copyOf(found);
    }

    private static void collectJava(Path root, int depth, List<Path> found) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var stream = Files.walk(root, depth)) {
            stream.filter(McRuntime::isJavaExecutable).forEach(path -> found.add(path.toAbsolutePath()));
        } catch (IOException | RuntimeException e) {
            // An unreadable JVM directory is not worth failing a start over; the
            // panel's own java still works for everything that does not need 25.
            LOG.warning("Could not scan " + root + " for JDKs: " + e.getMessage());
        }
    }

    private static boolean isJavaExecutable(Path path) {
        Path name = path.getFileName();
        Path parent = path.getParent();
        return name != null && "java".equals(name.toString())
                && parent != null && parent.getFileName() != null
                && "bin".equals(parent.getFileName().toString())
                && Files.isRegularFile(path);
    }

    private static JavaProbe probe(String program) {
        return JAVA_PROBES.computeIfAbsent(program, McRuntime::probeJava);
    }

    private static JavaProbe probeJava(String program) {
        boolean pathLike = program.indexOf('/') >= 0 || program.indexOf('\\') >= 0;
        if (pathLike && !Files.isRegularFile(Path.of(program))) {
            return new JavaProbe(null, 0, "the file does not exist");
        }
        try {
            Process process = new ProcessBuilder(program, "-version")
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (var in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new JavaProbe(null, 0, "`java -version` did not finish within 10 seconds");
            }
            String version = parseJavaVersion(output);
            int major = parseJavaMajor(output);
            if (major <= 0) {
                String tail = output.strip().lines().findFirst().orElse("");
                return new JavaProbe(version, 0, "`java -version` printed no version"
                        + (tail.isEmpty() ? "" : ": " + tail));
            }
            return new JavaProbe(version, major, null);
        } catch (Exception e) {
            String message = e.getMessage();
            return new JavaProbe(null, 0, message == null || message.isBlank()
                    ? e.getClass().getSimpleName() : message);
        }
    }

    /// The quoted version from `java -version`, or `null`.
    ///
    /// @param output the merged stdout/stderr of `java -version`
    /// @return the version string
    static @Nullable String parseJavaVersion(@Nullable String output) {
        if (output == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("(?:java|openjdk) version \"([^\"]+)\"").matcher(output);
        return matcher.find() ? matcher.group(1) : null;
    }

    /// The major version from `java -version`, with the Java 8 `1.8.0_x` form
    /// handled explicitly.
    ///
    /// @param output the merged stdout/stderr of `java -version`
    /// @return the major version, or `0` when it cannot be read
    static int parseJavaMajor(@Nullable String output) {
        String version = parseJavaVersion(output);
        if (version == null) {
            return 0;
        }
        String[] parts = version.split("[._+\\-]");
        try {
            int first = Integer.parseInt(parts[0]);
            if (first == 1 && parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }
            return first;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /// The state of an instance.
    ///
    /// @param id the instance id
    /// @return the status
    public Status status(String id) {
        TerminalSession session = terminals.find(BRAND, id).orElse(null);
        boolean alive = session != null || pipes.containsKey(id);
        State state = states.getOrDefault(id, State.STOPPED);
        if (!alive && (state == State.STARTING || state == State.RUNNING || state == State.STOPPING)) {
            // The registry drops a session that exited on its own; the maps follow.
            state = State.STOPPED;
        }
        return new Status(state, errors.get(id));
    }

    /// Whether the instance's server process is alive.
    ///
    /// @param id the instance id
    /// @return whether a session or a pipe process exists
    public boolean isRunning(String id) {
        return terminals.find(BRAND, id).isPresent() || pipes.containsKey(id);
    }

    /// The console of a running server. Never starts one.
    ///
    /// @param id the instance id
    /// @return the session, or empty when the server is not running
    public Optional<TerminalSession> console(String id) {
        return terminals.find(BRAND, id);
    }

    /// Starts an instance's server.
    ///
    /// @param id the instance id
    /// @throws McException when the feature is off or locked, the instance is
    ///                      unknown, not installed, or already running
    public void launch(String id) throws McException {
        if (!feature.enabled()) {
            throw new McException("the Minecraft feature is disabled");
        }
        if (feature.locked()) {
            throw new McException("the Minecraft feature is locked for another "
                    + feature.lockRemainingSeconds() + " second(s)");
        }
        McInstance instance = manager.find(id);
        if (instance == null) {
            throw new McException("Minecraft instance " + id + " does not exist");
        }
        if (terminals.find(BRAND, id).isPresent()) {
            throw new McException("the server for " + id + " is already running");
        }
        if (pipes.containsKey(id)) {
            throw new McException("the server for " + id + " is already running");
        }
        if (!installed(instance)) {
            // Neither a jar nor a Forge/NeoForge launch profile: say what to do instead of
            // naming one file that the installed core may never have.
            throw new McException("nothing to start for this instance: install "
                    + McInstanceManager.SERVER_JAR_NAME + " or a Forge/NeoForge core first");
        }
        if (!eulaAccepted(instance)) {
            throw new McException("the EULA has not been accepted for this instance");
        }
        checkJava(instance);
        // The server runs as the agent (see the PTY spawn's `Privileges.asAgent`), and the
        // panel is the one that created the instance directory and wrote `server.jar` into it.
        // Same rule as a brand instance: whatever the panel made for an agent's process has to
        // be the agent's. Without it the child can start and then die before Minecraft writes
        // its first log line — which reads as "exited before it was ready (code 1)" with no
        // output at all.
        prepareDirectories(instance);
        checkPorts(instance);

        if (!ptySupported) {
            launchPipe(instance);
            return;
        }

        Map<String, String> environment = childEnvironment(System.getenv());
        TerminalTarget target = new TerminalTarget(BRAND, id,
                commandFactory.command(instance), manager.instanceDirectory(id), environment);

        CompletableFuture<Void> ready = new CompletableFuture<>();
        readiness.put(id, ready);
        states.put(id, State.STARTING);
        errors.remove(id);

        TerminalSession session;
        try {
            session = terminals.open(target, PtySize.DEFAULT);
        } catch (IOException e) {
            readiness.remove(id);
            states.put(id, State.FAILED);
            errors.put(id, "failed to start the server process: " + e.getMessage());
            throw new McException("Failed to start the server for " + id, e);
        }

        session.attach(new ReadinessSink(ready));
        session.onExit(() -> {
            CompletableFuture<Void> pending = readiness.remove(id);
            boolean readyReached = pending != null && pending.isDone() && !pending.isCompletedExceptionally();
            State previous = states.getOrDefault(id, State.STOPPED);
            if (previous == State.FAILED) {
                // The readiness timeout already recorded why this server failed.
                return;
            }
            if (!readyReached && previous != State.STOPPING) {
                states.put(id, State.FAILED);
                errors.put(id, "the server exited before it was ready (code " + session.exitCode() + ")"
                        + (session.silentFailure()
                                ? " — it printed nothing at all, which is what a child that could not"
                                  + " start looks like: check that the server's user can read "
                                  + McInstanceManager.SERVER_JAR_NAME + " and write the instance directory"
                                : ""));
            } else {
                states.put(id, State.STOPPED);
            }
        });

        ready.orTimeout(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
            if (error != null) {
                states.put(id, State.FAILED);
                errors.put(id, "the server did not become ready within "
                        + READY_TIMEOUT_SECONDS + " seconds");
                session.terminate(Duration.ofSeconds(2));
                return;
            }
            states.put(id, State.RUNNING);
            LOG.info("Minecraft instance " + id + " is ready");
        });
    }

    /// Asks the server to stop (`stop` on stdin) and kills it when it hangs.
    ///
    /// @param id the instance id
    /// @throws McException when it is not running
    public void stop(String id) throws McException {
        PipeServer pipe = pipes.get(id);
        if (pipe != null) {
            states.put(id, State.STOPPING);
            writePipe(pipe, "stop\n");
            CompletableFuture<Void> exited = new CompletableFuture<>();
            pipe.process.onExit().thenRun(() -> exited.complete(null));
            exited.orTimeout(STOP_GRACE_SECONDS, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
                if (error != null) {
                    LOG.warning("Minecraft instance " + id + " did not stop within "
                            + STOP_GRACE_SECONDS + "s; killing it");
                    ProcessTree.terminate(pipe.process, 2);
                }
            });
            return;
        }
        TerminalSession session = terminals.find(BRAND, id)
                .orElseThrow(() -> new McException("the server for " + id + " is not running"));
        states.put(id, State.STOPPING);
        try {
            OutputStream input = session.input();
            synchronized (input) {
                input.write("stop\n".getBytes(StandardCharsets.US_ASCII));
                input.flush();
            }
        } catch (IOException e) {
            throw new McException("Failed to write `stop` to the server for " + id, e);
        }
        CompletableFuture<Void> exited = new CompletableFuture<>();
        session.onExit(() -> exited.complete(null));
        exited.orTimeout(STOP_GRACE_SECONDS, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
            if (error != null) {
                LOG.warning("Minecraft instance " + id + " did not stop within "
                        + STOP_GRACE_SECONDS + "s; killing it");
                session.terminate(Duration.ofSeconds(2));
            }
        });
    }

    /// Kills the server process group.
    ///
    /// @param id the instance id
    /// @throws McException when it is not running
    public void kill(String id) throws McException {
        PipeServer pipe = pipes.get(id);
        if (pipe != null) {
            states.put(id, State.STOPPING);
            ProcessTree.terminate(pipe.process, 2);
            return;
        }
        TerminalSession session = terminals.find(BRAND, id)
                .orElseThrow(() -> new McException("the server for " + id + " is not running"));
        states.put(id, State.STOPPING);
        session.terminate(Duration.ofSeconds(2));
    }

    /// Stops every running server and waits up to `grace` for a clean exit.
    ///
    /// This is the panel's own shutdown path. Without it a pipe-console server
    /// (Windows) is a plain child process the JVM does not take with it: the
    /// panel exits, the server keeps the port and the world, and the next panel
    /// has no record of it. The same walk also protects POSIX sessions, where
    /// `terminals.close()` would eventually do it, by asking each server to
    /// `stop` first and only killing what stays behind.
    ///
    /// @param grace the total time to wait, shared by all instances
    public void stopAll(Duration grace) {
        List<String> ids = new ArrayList<>();
        for (McInstance instance : manager.list()) {
            if (isRunning(instance.id())) {
                ids.add(instance.id());
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        Map<String, CompletableFuture<Void>> exits = new HashMap<>();
        for (String id : ids) {
            states.put(id, State.STOPPING);
            PipeServer pipe = pipes.get(id);
            if (pipe != null) {
                try {
                    writePipe(pipe, "stop\n");
                } catch (McException e) {
                    LOG.warning("Could not ask Minecraft instance " + id + " to stop: " + e.getMessage());
                }
                CompletableFuture<Void> exited = new CompletableFuture<>();
                pipe.process.onExit().thenRun(() -> exited.complete(null));
                exits.put(id, exited);
                continue;
            }
            terminals.find(BRAND, id).ifPresent(session -> {
                try {
                    OutputStream input = session.input();
                    synchronized (input) {
                        input.write("stop\n".getBytes(StandardCharsets.US_ASCII));
                        input.flush();
                    }
                } catch (IOException e) {
                    LOG.warning("Could not ask Minecraft instance " + id + " to stop: " + e.getMessage());
                }
                CompletableFuture<Void> exited = new CompletableFuture<>();
                session.onExit(() -> exited.complete(null));
                exits.put(id, exited);
            });
        }
        long deadline = System.nanoTime() + grace.toNanos();
        for (Map.Entry<String, CompletableFuture<Void>> entry : exits.entrySet()) {
            long remaining = deadline - System.nanoTime();
            try {
                if (remaining > 0) {
                    entry.getValue().get(remaining, TimeUnit.NANOSECONDS);
                }
            } catch (Exception ignored) {
                // Fall through to the forceful pass below with what is left.
            }
        }
        for (String id : ids) {
            PipeServer pipe = pipes.get(id);
            if (pipe != null) {
                if (pipe.process.isAlive()) {
                    LOG.warning("Minecraft instance " + id + " did not stop in time; killing it");
                    ProcessTree.terminate(pipe.process, 2);
                }
                continue;
            }
            terminals.find(BRAND, id).ifPresent(session -> {
                LOG.warning("Minecraft instance " + id + " did not stop in time; killing it");
                session.terminate(Duration.ofSeconds(2));
            });
        }
    }

    /// The console shape of a running instance: a real terminal on POSIX, a
    /// log+command box on Windows, `null` when nothing is running.
    ///
    /// @param id the instance id
    /// @return `"pty"`, `"pipe"` or `null`
    public @Nullable String consoleMode(String id) {
        if (terminals.find(BRAND, id).isPresent()) {
            return "pty";
        }
        return pipes.containsKey(id) ? "pipe" : null;
    }

    /// Writes one console line to the running server.
    ///
    /// @param id   the instance id
    /// @param line the command (a newline is appended)
    /// @throws McException when the server is not running
    public void sendCommand(String id, String line) throws McException {
        TerminalSession session = terminals.find(BRAND, id).orElse(null);
        if (session != null) {
            try {
                OutputStream input = session.input();
                synchronized (input) {
                    input.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    input.flush();
                }
            } catch (IOException e) {
                throw new McException("Failed to write to the server console", e);
            }
            return;
        }
        PipeServer pipe = pipes.get(id);
        if (pipe == null) {
            throw new McException("the server for " + id + " is not running");
        }
        writePipe(pipe, line + "\n");
    }

    private void launchPipe(McInstance instance) throws McException {
        String id = instance.id();
        Process process;
        try {
            Path directory = manager.instanceDirectory(id);
            Files.createDirectories(directory);
            ProcessBuilder builder = new ProcessBuilder(
                    org.jackhuang.hmcl.util.platform.Privileges.asAgent(
                            commandFactory.command(instance)));
            builder.directory(directory.toFile());
            builder.redirectErrorStream(true);
            builder.environment().clear();
            builder.environment().putAll(childEnvironment(System.getenv()));
            process = builder.start();
        } catch (IOException e) {
            states.put(id, State.FAILED);
            errors.put(id, "failed to start the server process: " + e.getMessage());
            throw new McException("Failed to start the server for " + id, e);
        }
        CompletableFuture<Void> ready = new CompletableFuture<>();
        readiness.put(id, ready);
        states.put(id, State.STARTING);
        errors.remove(id);

        PipeServer pipe = new PipeServer(process, manager.logFile(instance));
        pipes.put(id, pipe);
        process.onExit().thenRun(() -> finish(id, process.exitValue(), ready));
        // The pipe pump blocks in a native read for as long as the server runs, so it is a
        // platform thread: a virtual one pins its carrier for that whole time, and one server
        // on a one-core machine would take the only carrier there is. See
        // VirtualThreads#startPlatform for the terminal that hit this first.
        VirtualThreads.startPlatform(() -> pumpPipe(pipe, ready), "hdsl-mc-pipe-" + id);
        ready.orTimeout(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
            if (error != null) {
                states.put(id, State.FAILED);
                errors.put(id, "the server did not become ready within "
                        + READY_TIMEOUT_SECONDS + " seconds");
                process.destroyForcibly();
                return;
            }
            states.put(id, State.RUNNING);
            LOG.info("Minecraft instance " + id + " is ready (pipe console)");
        });
    }

    private void pumpPipe(PipeServer pipe, CompletableFuture<Void> ready) {
        try {
            java.nio.file.Files.createDirectories(pipe.log.getParent());
            try (java.io.BufferedReader in = new java.io.BufferedReader(
                         new java.io.InputStreamReader(pipe.process.getInputStream(), StandardCharsets.UTF_8));
                 java.io.BufferedWriter log = java.nio.file.Files.newBufferedWriter(pipe.log,
                         StandardCharsets.UTF_8,
                         java.nio.file.StandardOpenOption.CREATE,
                         java.nio.file.StandardOpenOption.APPEND)) {
                String line;
                while ((line = in.readLine()) != null) {
                    log.write(line);
                    log.newLine();
                    log.flush();
                    if (!ready.isDone() && line.contains("Done (")) {
                        ready.complete(null);
                    }
                }
            }
        } catch (IOException e) {
            LOG.warning("Console pump ended for " + pipe.log, e);
        }
    }

    private void finish(String id, int exitCode, CompletableFuture<Void> ready) {
        pipes.remove(id);
        CompletableFuture<Void> pending = readiness.remove(id);
        boolean readyReached = pending != null && pending.isDone() && !pending.isCompletedExceptionally();
        State previous = states.getOrDefault(id, State.STOPPED);
        if (previous == State.FAILED) {
            return;
        }
        if (!readyReached && previous != State.STOPPING) {
            states.put(id, State.FAILED);
            errors.put(id, "the server exited before it was ready (code " + exitCode + ")");
        } else {
            states.put(id, State.STOPPED);
        }
    }

    private static void writePipe(PipeServer pipe, String text) throws McException {
        try {
            OutputStream stdin = pipe.process.getOutputStream();
            synchronized (stdin) {
                stdin.write(text.getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }
        } catch (IOException e) {
            throw new McException("Failed to write to the server console", e);
        }
    }

    private record PipeServer(Process process, Path log) {
    }

    /// Whether the instance's `eula.txt` records acceptance.
    ///
    /// Minecraft writes the file with a comment header and `eula=true` on its
    /// own line; the panel's own `acceptEula` writes the bare line. Both are
    /// valid. Comparing the whole file to `eula=true` rejected every server
    /// that had been started once before the panel adopted it.
    ///
    /// When the key appears more than once the **last** value wins: that is how
    /// Java's properties parser (which the server uses) reads it, so a file a
    /// server would accept must not be rejected here.
    boolean eulaAccepted(McInstance instance) {
        try {
            boolean accepted = false;
            for (String line : Files.readAllLines(manager.eulaFile(instance), StandardCharsets.UTF_8)) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator > 0 && trimmed.substring(0, separator).strip().equalsIgnoreCase("eula")) {
                    accepted = trimmed.substring(separator + 1).strip().equalsIgnoreCase("true");
                }
            }
            return accepted;
        } catch (IOException e) {
            return false;
        }
    }

    /// Hands the instance's directory and jar to the agent before a server is spawned.
    ///
    /// The panel is the one that created the instance directory and wrote `server.jar` into
    /// it, and the server itself runs as the dropped uid. On a host whose `/data` is a plain
    /// bind mount (no setgid, umask 022) those come out `root:root 0755`: the jar is readable,
    /// but the child cannot create `world/`, `logs/` or `crash-reports/`, and Java exits
    /// before Minecraft writes anything — the panel can then only report "exited before it was
    /// ready (code 1)" with an empty console.
    ///
    /// Cheap when there is nothing to do: one `stat` on the jar decides it, and everything
    /// below is skipped outside a container where children are not dropped.
    ///
    /// @param instance the instance about to be started
    /// @throws McException when the server's user cannot read the jar even after the hand-over
    private void prepareDirectories(McInstance instance) throws McException {
        if (!Privileges.dropsChildren()) {
            return;
        }
        Path directory = manager.instanceDirectory(instance.id());
        Path jar = manager.serverJar(instance);
        // Forge/NeoForge have **no** `server.jar`: the installer leaves a launch profile. A
        // marker chosen as "the jar, else launch.json" is what keeps the hand-over from
        // skipping exactly those instances — and then refusing them with "server.jar is not
        // readable", which is what an unconditional jar check did.
        Path marker = Files.isRegularFile(jar) ? jar : directory.resolve(McForge.LAUNCH_FILE);
        if (Privileges.ownerOf(directory) == Privileges.AGENT_UID
                && Privileges.ownerOf(marker) == Privileges.AGENT_UID) {
            return;
        }
        int handed = Privileges.handTreeToAgent(directory);
        LOG.info("Handed " + handed + " entries of the Minecraft instance " + instance.id()
                + " to the agent (it was created as root)");
        // The jar is the one file a refusal can be built on: without read access the server
        // cannot start at all, and saying that is better than watching java exit by itself.
        if (Files.isRegularFile(jar) && !Files.isReadable(jar)) {
            throw new McException(McInstanceManager.SERVER_JAR_NAME + " is not readable by the "
                    + "server's user: " + jar);
        }
    }

    /// Whether the instance has something to start.
    ///
    /// An installed core is either a `server.jar` (vanilla, Paper, Fabric) or a launch profile
    /// the Forge/NeoForge installer left behind. Requiring the jar refused to start exactly the
    /// cores that had just been installed successfully.
    ///
    /// @param instance the instance
    /// @return whether a launch target exists
    private boolean installed(McInstance instance) {
        if (Files.isRegularFile(manager.serverJar(instance))) {
            return true;
        }
        return McForge.launchArguments(manager.instanceDirectory(instance.id())) != null;
    }

    /// The environment a Minecraft child is given.
    ///
    /// A whitelist, not `System.getenv()`: the child loads third-party plugins and mods, and the
    /// panel's environment may hold `HDSL_ADMIN_PASSWORD`, `HDSL_AGENT_PASSWORD` or proxy
    /// credentials. The panel's own `HDSL_*` names never travel to a game server.
    ///
    /// @param source the panel's environment
    /// @return what the child may see
    static Map<String, String> childEnvironment(Map<String, String> source) {
        Map<String, String> environment = new HashMap<>();
        for (String key : List.of("PATH", "HOME", "USERPROFILE", "LANG", "LC_ALL", "LC_CTYPE",
                "TZ", "TEMP", "TMP", "TMPDIR", "SystemRoot", "windir", "COMSPEC", "PATHEXT",
                "JAVA_HOME")) {
            String value = source.get(key);
            if (value != null) {
                environment.put(key, value);
            }
        }
        return environment;
    }

    /// Refuses a Java too old for the version **before** spawning anything.
    ///
    /// The version metadata carries `javaVersion.majorVersion` (written into
    /// `version.json` by [McVersions]); the panel runs `<java> -version` once and
    /// caches the result. When either side is unknown the check is skipped —
    /// the server's own error is then the fallback, not a wrong refusal.
    /// Refuses to start when the game or RCON port is already bound.
    ///
    /// The most common cause on Windows is a server from a panel process that
    /// died before it could stop its children: the new panel has no record of
    /// it, but the port is still held. Saying that here is much cheaper than
    /// letting Java fail after the world is half-loaded.
    private void checkPorts(McInstance instance) throws McException {
        if (!McPorts.isFree(instance.port())) {
            throw new McException("The Minecraft game port " + instance.port()
                    + " is already in use — another server, or a leftover from a previous"
                    + " panel process, is listening. Stop it or change this instance's port.");
        }
        // RCON is off by default. Checking its port unconditionally would refuse
        // to start a server over a number the server will never bind.
        boolean rcon = "true".equalsIgnoreCase(manager.properties(instance).get("enable-rcon"));
        if (rcon && !McPorts.isFree(instance.rconPort())) {
            throw new McException("The Minecraft RCON port " + instance.rconPort()
                    + " is already in use — another server, or a leftover from a previous"
                    + " panel process, is listening. Stop it or change this instance's port.");
        }
    }

    private void checkJava(McInstance instance) throws McException {
        if (versionsRoot == null || javaBin == null) {
            return;
        }
        // The executable that will actually launch, not the panel's own: when the
        // version asks for 25 and the image ships one, the refusal below must not
        // be about the JVM nobody was going to use.
        JavaInfo java = javaInfoFor(instance);
        if (!java.ok()) {
            String hint = instance.javaPath() != null
                    ? "pick another JDK/JRE in this instance's advanced settings, or clear the"
                            + " selection to use the panel's own runtime"
                    : "point mc.java_path at a working JDK/JRE (Java 21+ for modern versions),"
                            + " or unset it to use the panel's own runtime";
            throw new McException("The Java used for Minecraft servers cannot be run: `"
                    + java.path() + "` — " + java.error() + "; " + hint);
        }
        int required = requiredJava(instance.version());
        if (required <= 0) {
            return;
        }
        if (java.major() > 0 && java.major() < required) {
            throw new McException("Minecraft " + instance.version() + " needs Java " + required
                    + " or newer, but `" + java.path() + "` is Java " + java.major()
                    + "; install a newer JDK/JRE (the container image ships JDK 25 under /usr/lib/jvm)"
                    + " or point mc.java_path at one");
        }
    }

    /// Where the instance's chosen Java came from, for the refusal text and the API.
    ///
    /// A pin can point at the panel JVM, a managed JRE or a host JDK; the source
    /// is what lets the UI say which menu entry the selection came from.
    ///
    /// @param instance the instance
    /// @param program  the executable that will run
    /// @return `configured`, `panel`, `managed`, or `installed`
    private String sourceOf(McInstance instance, String program) {
        if (instance.javaPath() != null && instance.javaPath().equals(program)) {
            if (javaBin != null && program.equals(javaBin)) {
                return javaSource;
            }
            if (javaRoot != null) {
                Path root = javaRoot.toAbsolutePath().normalize();
                if (Path.of(program).toAbsolutePath().normalize().startsWith(root)) {
                    return "managed";
                }
            }
            return "installed";
        }
        return sourceOf(program);
    }

    /// Where `program` came from, for the refusal text and the API.
    ///
    /// @param program the executable that will run
    /// @return `configured`, `panel`, `managed`, or `installed`
    private String sourceOf(String program) {
        if (!"panel".equals(javaSource)) {
            return javaSource;
        }
        if (program.equals(javaBin)) {
            return "panel";
        }
        if (javaRoot != null) {
            Path root = javaRoot.toAbsolutePath().normalize();
            Path candidate = Path.of(program).toAbsolutePath().normalize();
            if (candidate.startsWith(root)) {
                return "managed";
            }
        }
        return "installed";
    }

    /// The Java feature release an instance needs.
    ///
    /// The version metadata is authoritative; when it is absent — a
    /// third-party core, or a jar an operator dropped in by hand — the
    /// documented vanilla requirement table answers instead, so a launch is
    /// never attempted on a JVM that is certainly too old.
    private int requiredJava(String version) {
        if (versionsRoot == null) {
            return McJavaRuntimes.recommendFor(version);
        }
        Path metadata = versionsRoot.resolve(version).resolve("version.json");
        if (!Files.isRegularFile(metadata)) {
            return McJavaRuntimes.recommendFor(version);
        }
        try {
            com.google.gson.JsonObject record =
                    org.jackhuang.hmcl.util.gson.JsonUtils.fromJsonFile(metadata,
                            com.google.gson.JsonObject.class);
            return record != null && record.has("javaMajor") ? record.get("javaMajor").getAsInt() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /// Watches the output for the readiness line. Vanilla prints
    /// `Done (1.234s)! For help, type "help"`; a version that rewords it will
    /// time out rather than hang forever, and the error says so.
    private static final class ReadinessSink implements TerminalSink {
        private static final String MARKER = "Done (";
        private final CompletableFuture<Void> ready;
        private final StringBuilder partial = new StringBuilder();

        private ReadinessSink(CompletableFuture<Void> ready) {
            this.ready = ready;
        }

        @Override
        public void data(byte[] bytes, int offset, int length) {
            if (ready.isDone()) {
                return;
            }
            partial.append(new String(bytes, offset, length, StandardCharsets.UTF_8));
            int newline;
            while ((newline = partial.indexOf("\n")) >= 0) {
                String line = partial.substring(0, newline);
                partial.delete(0, newline + 1);
                if (line.contains(MARKER)) {
                    ready.complete(null);
                    return;
                }
            }
            // A server that prints without a newline must not grow this forever.
            if (partial.length() > 16 * 1024) {
                partial.delete(0, partial.length() - 4 * 1024);
            }
        }

        @Override
        public void exited(int code) {
            // The runtime's onExit listener decides what an early exit means.
        }
    }
}