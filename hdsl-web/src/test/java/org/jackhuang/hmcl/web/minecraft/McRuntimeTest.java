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

import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jackhuang.hmcl.web.terminal.TerminalManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McRuntimeTest {

    @TempDir
    Path dir;

    private McInstanceManager manager;
    private McFeatureState feature;
    private TerminalManager terminals;

    @BeforeEach
    void setUp() throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"),
                "the PTY implementation is POSIX-only");
        manager = new McInstanceManager(dir.resolve("minecraft").resolve("instances"));
        feature = new McFeatureState(dir.resolve("feature.json"));
        feature.enable(consent());
        terminals = new TerminalManager();
    }

    @AfterEach
    void tearDown() {
        if (terminals != null) {
            terminals.close();
        }
    }

    private static McConsent consent() {
        return new McConsent(3, "sha256:terms", "sha256:eula",
                "2026-10-29T00:58:00Z", "2026-10-29T01:00:00Z", "2026-10-29T01:00:00Z",
                "tester", "127.0.0.1", "scroll+checkbox", true, "1.4.0", "UA", "abc1234", null);
    }

    private static String javaBin() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name).toString();
    }

    private McInstance installedInstance(String name, String version) throws Exception {
        // 端口必须是真的空闲的：`launch` 现在启动前会 bind 一次做探测，
        // 固定写 25565 的用例在“这台机器上已经有人在听 25565”时就必红
        // （面板自己发布了 MC 端口也是这样）。
        McPorts.Pair ports = freePorts();
        McInstance instance = manager.create(name, version, ports.game(), ports.rcon(), 1024, "world", null);
        Files.writeString(manager.serverJar(instance), "");
        manager.acceptEula(instance);
        return instance;
    }

    private McPorts.Pair freePorts() {
        return McPorts.allocate(port -> manager.portTaken(port) || !McPorts.isFree(port), 26000, 26999);
    }

    private McRuntime runtime(String... mode) {
        List<String> command = new java.util.ArrayList<>(List.of(
                javaBin(), "-cp", System.getProperty("java.class.path"), FakeMcServer.class.getName()));
        command.addAll(List.of(mode));
        return new McRuntime(manager, terminals, feature, instance -> command);
    }

    private static McRuntime.Status await(McRuntime runtime, String id, McRuntime.State expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        McRuntime.Status status = runtime.status(id);
        while (System.nanoTime() < deadline) {
            status = runtime.status(id);
            if (status.state() == expected) {
                return status;
            }
            Thread.sleep(50);
        }
        return status;
    }

    @Test
    void launchWaitsForTheReadinessLineAndStopEndsTheProcess() throws Exception {
        McInstance instance = installedInstance("survival", "1.21.4");
        McRuntime runtime = runtime();

        runtime.launch(instance.id());
        assertEquals(McRuntime.State.RUNNING, await(runtime, instance.id(), McRuntime.State.RUNNING).state());
        assertNotNull(runtime.console(instance.id()).orElse(null));

        runtime.stop(instance.id());
        assertEquals(McRuntime.State.STOPPED, await(runtime, instance.id(), McRuntime.State.STOPPED).state());
        assertTrue(runtime.console(instance.id()).isEmpty());
    }

    @Test
    void launchingWhileLockedIsRefused() throws Exception {
        McInstance instance = installedInstance("locked", "1.21.4");
        feature.stopFeature();

        McException refused = assertThrows(McException.class, () -> runtime().launch(instance.id()));
        assertTrue(refused.getMessage().contains("locked"), refused.getMessage());
    }

    @Test
    void aServerThatCrashesBeforeReadyIsMarkedFailed() throws Exception {
        McInstance instance = installedInstance("crashy", "1.21.4");
        McRuntime runtime = runtime("crash");

        runtime.launch(instance.id());
        McRuntime.Status status = await(runtime, instance.id(), McRuntime.State.FAILED);
        assertEquals(McRuntime.State.FAILED, status.state());
        assertNotNull(status.error());
    }

    /// A server that dies before it prints anything is the shape a permission or exec failure
    /// takes (the reported one: the instance directory was root's while the server ran as the
    /// agent). "code 1" alone sends people to look at Minecraft, so the message says the child
    /// never spoke and what to check.
    @Test
    void aServerThatDiesWithoutPrintingSaysSo() throws Exception {
        McInstance instance = installedInstance("mute", "1.21.4");
        McRuntime runtime = runtime("silent");

        runtime.launch(instance.id());
        McRuntime.Status status = await(runtime, instance.id(), McRuntime.State.FAILED);
        assertEquals(McRuntime.State.FAILED, status.state());
        assertNotNull(status.error());
        assertTrue(status.error().contains("printed nothing at all"), status.error());
        assertTrue(status.error().contains(McInstanceManager.SERVER_JAR_NAME), status.error());
    }

    @Test
    void aMissingJarOrEulaIsRefusedBeforeAnythingIsSpawned() throws Exception {
        McPorts.Pair ports = freePorts();
        McInstance instance = manager.create("bare", "1.21.4", ports.game(), ports.rcon(), 1024, "world", null);
        McRuntime runtime = runtime();

        McException noJar = assertThrows(McException.class, () -> runtime.launch(instance.id()));
        assertTrue(noJar.getMessage().contains("server.jar"), noJar.getMessage());

        Files.writeString(manager.serverJar(instance), "");
        McException noEula = assertThrows(McException.class, () -> runtime.launch(instance.id()));
        assertTrue(noEula.getMessage().contains("EULA"), noEula.getMessage());
        assertTrue(runtime.console(instance.id()).isEmpty());
    }

    @Test
    void anEulaFileWithMinecraftsCommentHeaderIsAccepted() throws Exception {
        McInstance instance = installedInstance("standard-eula", "1.21.4");
        Files.writeString(manager.eulaFile(instance),
                "#By changing the setting below to TRUE you are indicating your agreement to our EULA "
                        + "(https://account.mojang.com/documents/minecraft_eula).\n"
                        + "#Wed Feb 04 19:16:42 CST 2026\n"
                        + "eula=true\n");

        assertTrue(runtime().eulaAccepted(instance));
    }

    @Test
    void anEulaFileThatDeclinesIsRejected() throws Exception {
        McInstance instance = installedInstance("declined-eula", "1.21.4");
        Files.writeString(manager.eulaFile(instance),
                "#By changing the setting below to TRUE you are indicating your agreement to our EULA.\n"
                        + "eula=false\n");

        assertFalse(runtime().eulaAccepted(instance));
    }

    /// Java's properties parser keeps the last value of a repeated key; the
    /// server would accept this file, so the panel must not reject it.
    @Test
    void aRepeatedEulaKeyUsesTheLastValue() throws Exception {
        McInstance instance = installedInstance("repeated-eula", "1.21.4");
        Files.writeString(manager.eulaFile(instance),
                "eula=false\n"
                        + "# turned on later by hand\n"
                        + "eula=true\n");
        assertTrue(runtime().eulaAccepted(instance));

        Files.writeString(manager.eulaFile(instance),
                "eula=true\n"
                        + "eula=false\n");
        assertFalse(runtime().eulaAccepted(instance));
    }

    @Test
    void theConsoleIsAttachOnly() throws Exception {
        McInstance instance = installedInstance("console", "1.21.4");
        McRuntime runtime = runtime();
        assertTrue(runtime.console(instance.id()).isEmpty(), "opening the console must not start a server");

        runtime.launch(instance.id());
        await(runtime, instance.id(), McRuntime.State.RUNNING);
        // A second launch is refused instead of attaching a second process.
        assertThrows(McException.class, () -> runtime.launch(instance.id()));

        runtime.kill(instance.id());
        assertEquals(McRuntime.State.STOPPED, await(runtime, instance.id(), McRuntime.State.STOPPED).state());
    }

    @Test
    void theConfiguredJavaIsResolvedToAPathBecausePosixSpawnDoesNotSearchPath() {
        // `posix_spawn("java")` is ENOENT: the PTY spawn is execve, not a shell.
        String resolved = McRuntime.resolveJava("java");
        assertTrue(Path.of(resolved).isAbsolute(), resolved);
        assertTrue(Files.isRegularFile(Path.of(resolved)), resolved);

        // An explicit path is kept as given (only made absolute) rather than re-resolved.
        String explicit = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        assertEquals(Path.of(explicit).toAbsolutePath().toString(), McRuntime.resolveJava(explicit));

        // A name nobody has stays as given, so the failure can still name it.
        assertEquals("hdsl-no-such-java-binary", McRuntime.resolveJava("hdsl-no-such-java-binary"));
    }

    @Test
    void javaEightIsParsedAsMajorEightNotOne() {
        assertEquals(8, McRuntime.parseJavaMajor("java version \"1.8.0_401\"\nJava(TM) SE Runtime"));
        assertEquals(17, McRuntime.parseJavaMajor("openjdk version \"17.0.9\" 2023-10-17"));
        assertEquals(21, McRuntime.parseJavaMajor("java version \"21\" 2023-09-19 LTS"));
        assertEquals(11, McRuntime.parseJavaMajor("openjdk version \"11.0.2\" 2019-01-15"));
        assertEquals(0, McRuntime.parseJavaMajor("not a java version line"));
        assertEquals(0, McRuntime.parseJavaMajor(null));
    }

    @Test
    void javaInfoReportsThePanelRuntimeByDefault() {
        McRuntime runtime = new McRuntime(manager, terminals, feature, "java", null);
        McRuntime.JavaInfo info = runtime.javaInfo();
        assertTrue(info.ok(), info.error());
        assertEquals("panel", info.source());
        assertTrue(info.major() >= 17, "the panel itself requires JDK 17+: " + info.major());
        assertNotNull(info.version());
        assertTrue(Path.of(info.path()).isAbsolute(), info.path());
    }

    @Test
    void aConfiguredJavaThatDoesNotExistIsNotSilentlyAccepted() {
        McRuntime runtime = new McRuntime(manager, terminals, feature,
                Path.of(dir.toString(), "no-such-java").toString(), null);
        McRuntime.JavaInfo info = runtime.javaInfo();
        assertFalse(info.ok());
        assertNotNull(info.error());
        assertTrue(info.error().contains("does not exist"), info.error());
    }

    /// Since 26.x Minecraft requires Java 25. The official image ships JDK 21 as
    /// the default JVM and 25 beside it, so the panel picks the installed one per
    /// version — "install one and point `mc.java_path` at it" is not advice a
    /// container user can follow, and moving the whole machine's default to 25
    /// would take 1.16 off the combination it was tested on.
    @Test
    void aVersionThatNeedsANewerJavaGetsTheInstalledOne() throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake JDKs are shell scripts");
        Path java21 = fakeJava(dir.resolve("jdk21"), "21.0.9");
        Path java25 = fakeJava(dir.resolve("jdk25"), "25.0.1");

        assertEquals(java21.toString(),
                McRuntime.selectJava(java21.toString(), 21, 21, List.of(java21, java25)),
                "a JVM that already satisfies the version is left alone");
        assertEquals(java21.toString(),
                McRuntime.selectJava(java21.toString(), 21, 17, List.of(java21, java25)),
                "older versions keep the combination they were tested on");
        assertEquals(java25.toString(),
                McRuntime.selectJava(java21.toString(), 21, 25, List.of(java21, java25)),
                "a version that needs 25 gets the JDK that has it");
        assertEquals(java21.toString(),
                McRuntime.selectJava(java21.toString(), 21, 25, List.of(java21)),
                "with no suitable JDK the panel's own comes back, so the refusal can name it");
    }

    /// The advanced setting exists so one server can stay on a JDK the operator
    /// picked, even when the panel's own JVM or the version heuristic would pick
    /// another. A pin is not a hint: it is the executable that will run.
    @Test
    void anInstancesPinnedJavaWinsOverThePanelRuntime() throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake JDK is a shell script");
        Path java17 = fakeJava(dir.resolve("jdk17"), "17.0.9");
        McPorts.Pair ports = freePorts();
        McInstance instance = manager.create("pinned", "1.21.4", ports.game(), ports.rcon(),
                1024, "world", null).withJava(java17.toString());

        McRuntime runtime = new McRuntime(manager, terminals, feature, "java", null);
        assertEquals(java17.toString(), runtime.javaFor(instance));
        McRuntime.JavaInfo info = runtime.javaInfoFor(instance);
        assertTrue(info.ok(), info.error());
        assertEquals(17, info.major());
        assertEquals("installed", info.source());
        assertEquals(java17.toString(), info.path());
    }

    @Test
    void theLowestJdkThatSatisfiesTheVersionWins() throws Exception {
        Assumptions.assumeFalse(OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS,
                "the fake JDKs are shell scripts");
        Path java21 = fakeJava(dir.resolve("jdk21"), "21.0.9");
        Path java25 = fakeJava(dir.resolve("jdk25"), "25.0.1");
        Path java29 = fakeJava(dir.resolve("jdk29"), "29.0.2");

        assertEquals(java25.toString(),
                McRuntime.selectJava(java21.toString(), 21, 25, List.of(java21, java25, java29)),
                "a version that asks for 25 is not asking for 29");
    }

    /// The scan against whatever this machine actually has. The depth is the part
    /// that can silently break: `/usr/lib/jvm/<jdk>/bin/java` is three levels
    /// below the root, and an off-by-one finds nothing and reports "no suitable
    /// JDK" on a machine that has one.
    @Test
    void theInstalledJdksAreFoundWhereTheDistributionsPutThem() {
        Assumptions.assumeTrue(OperatingSystem.CURRENT_OS == OperatingSystem.LINUX,
                "the layout under test is the Linux one");
        Assumptions.assumeTrue(Files.isDirectory(Path.of("/usr/lib/jvm")));

        List<Path> candidates = McRuntime.javaCandidates();

        assertFalse(candidates.isEmpty(), "the JDK running this test sits in /usr/lib/jvm");
        for (Path candidate : candidates) {
            assertEquals("java", candidate.getFileName().toString(), candidate.toString());
            assertEquals("bin", candidate.getParent().getFileName().toString(), candidate.toString());
            assertTrue(Files.isRegularFile(candidate), candidate.toString());
        }
    }

    /// A JDK in a temp directory that answers `-version` like the real thing.
    ///
    /// @param home    the fake JDK root
    /// @param version the version it reports
    /// @return its `bin/java`
    private static Path fakeJava(Path home, String version) throws Exception {
        Path java = Files.createDirectories(home.resolve("bin")).resolve("java");
        Files.writeString(java, "#!/bin/sh\necho 'openjdk version \"" + version + "\" 2025-09-16' >&2\n");
        assertTrue(java.toFile().setExecutable(true), "the fake JDK has to be executable");
        return java;
    }

    @Test
    void initialHeapNeverExceedsTheConfiguredMaximum() {
        // The create form accepts 256 MB; a fixed -Xms512M made the JVM refuse
        // to start before Minecraft wrote its first line.
        assertEquals(List.of("-Xms256M", "-Xmx256M"), McRuntime.heapArgs(256));
        assertEquals(List.of("-Xms512M", "-Xmx2048M"), McRuntime.heapArgs(2048));
        assertEquals(List.of("-Xms512M", "-Xmx512M"), McRuntime.heapArgs(512));
    }

    /// A Minecraft server loads third-party plugins and mods, so it gets a whitelist of the
    /// panel's environment rather than a copy: `HDSL_ADMIN_PASSWORD` and friends stay behind.
    @Test
    void theChildEnvironmentDropsThePanelsSecrets() {
        java.util.Map<String, String> source = java.util.Map.of(
                "PATH", "/usr/bin", "HOME", "/home/hdsl", "LANG", "C.UTF-8", "TZ", "Asia/Shanghai",
                "HDSL_ADMIN_PASSWORD", "secret", "HDSL_AGENT_PASSWORD", "secret",
                "DEEPSEEK_API_KEY", "sk-x");
        java.util.Map<String, String> environment = McRuntime.childEnvironment(source);
        assertEquals("/usr/bin", environment.get("PATH"));
        assertEquals("/home/hdsl", environment.get("HOME"));
        assertEquals("C.UTF-8", environment.get("LANG"));
        assertEquals("Asia/Shanghai", environment.get("TZ"));
        assertFalse(environment.containsKey("HDSL_ADMIN_PASSWORD"));
        assertFalse(environment.containsKey("HDSL_AGENT_PASSWORD"));
        assertFalse(environment.containsKey("DEEPSEEK_API_KEY"));
    }
}
