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
package org.jackhuang.hmcl.web;

import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.web.auth.AuthService;
import org.jackhuang.hmcl.web.auth.UserStore;
import org.jackhuang.hmcl.web.config.ServerConfig;
import org.jackhuang.hmcl.web.config.ServerConfigLoader;
import org.jackhuang.hmcl.web.config.WorkspaceDirectory;
import org.jackhuang.hmcl.web.logs.LogRetention;
import org.jackhuang.hmcl.web.server.HdslServer;
import org.jackhuang.hmcl.web.terminal.pty.ConPty;
import org.jackhuang.hmcl.web.tls.CertificateManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

/// The HDSL-web server entry point.
///
/// `hdsl-web` starts the server from `$HDSL_DATA/server.yaml` (defaults to
/// `./data`), prints a banner naming where it listens, under which protocol,
/// whether authentication is on, and where the TLS identity comes from, then
/// blocks until a shutdown signal stops Jetty gracefully.
///
/// `hdsl-web -version` prints the banner and exits.
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        // The jar this process runs from, recorded before anything reads a resource:
        // replacing that file under a running panel is what turns every later
        // resource read into "ZipException: invalid LOC header", and the messages
        // that fail should be able to say so.
        org.jackhuang.hmcl.util.PanelJar.remember(Main.class);
        // Must be the very first thing: the JDK freezes the restricted-header
        // allow-list when the first HttpClient is constructed, and any static
        // client in a class we happen to touch would do that first (this bit us:
        // a brand-installer initialiser ran before the proxy's own static block,
        // and every /i/* mount answered 500 "restricted header name: Host").
        org.jackhuang.hmcl.web.http.HostHeaderSupport.allow();
        for (String arg : args) {
            if ("-version".equals(arg) || "--version".equals(arg)) {
                System.out.println("HDSL-web " + Metadata.VERSION);
                return;
            }
        }
        if (args.length > 0) {
            System.err.println("Unknown argument(s): " + String.join(" ", args));
            System.err.println("Usage: hdsl-web [-version]");
            System.exit(2);
        }

        // Before anything that touches a file: from here on the panel may spend a while
        // cleaning up after a failed delete (a leftover `node_modules` tree takes 20–40 s on
        // Windows) before the banner appears, and a console with no output at all for a minute
        // and a half reads as "it hung". The pid is enough to go look at the process.
        //
        // Deliberately no version here: `Metadata`'s static initialiser reads `hdsl.home`,
        // which is only set once the configuration is loaded — touching it now would freeze the
        // per-user home at the OS default and the panel would look for instances outside its own
        // data directory. The banner below prints the version properly.
        System.out.println("HDSL-web starting (pid " + ProcessHandle.current().pid() + ")");

        try {
            Map<String, String> env = System.getenv();
            ServerConfig config = ServerConfigLoader.load(env, Logger.LOG);

            // The launcher's per-user home lives inside the data directory, so
            // a deployment mounts exactly one volume (`/data`, see the plan).
            // An explicit -Dhdsl.home wins, and the property must be set before
            // the first touch of Metadata, whose static initializer reads it —
            // which is why nothing below may run earlier: a single call into
            // DshPaths freezes the home at the OS default and the panel then
            // looks for instances outside its own data directory.
            if (System.getProperty("hdsl.home") == null) {
                System.setProperty("hdsl.home", config.dataDir.resolve("hdsl").toString());
            }

            // The outbound-TLS window an operator acknowledged last time, if its
            // hour has not passed. Nothing here widens trust on its own: strict is
            // the default, and the failure that leads to an acknowledgement is
            // explained where the empty version list is.
            org.jackhuang.hmcl.util.platform.OutboundTls.accept(config.network.acceptUnverifiedUntil);

            // Directories a delete could not remove (something held a file inside) were
            // renamed aside; at startup whatever held them is most likely gone. The brand and
            // ZCode categories sweep their own roots where their managers are built
            // (BrandApiServlet / ZcodeApiServlet), which is also this startup path.
            //
            // Timed, because this is the one step that can block the banner by minutes: a
            // leftover tree is a whole `node_modules`, and deleting one takes 20–40 s on
            // Windows. Silence during it is what made a slow start look like a hang, so a slow
            // sweep says so (a fast one stays quiet — this prints nothing on the normal path).
            long sweepStarted = System.nanoTime();
            org.jackhuang.hmcl.dsh.DshInstanceManager.sweepTombstones();
            long sweepMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - sweepStarted);
            if (sweepMillis >= 1000) {
                System.out.println("  startup: cleaned up leftover directories in " + sweepMillis + " ms");
            }

            Logger.LOG.start(config.dataDir.resolve("logs"));

            // 日志保留：启动清理一次，之后每天一次。审计按文件名里的日期算年龄，
            // 面板日志按 mtime；保底 180 天、上限四年，面板接口可往上调（见 LogRetention）。
            LogRetention.install(new LogRetention(
                    config.dataDir.resolve("logs"), config.logs.auditDays, config.logs.panelDays));

            // 升级迁移：v0.4.8 之前写入的 server.yaml 是 0644，里面躺着 p12 密码；certs/ 下的
            // 私钥/证书包同理。逐份收紧为仅所有者可读写，个别失败只告警、不挡启动。
            Path serverYaml = config.dataDir.resolve("server.yaml");
            if (Files.exists(serverYaml)) {
                try {
                    Files.setPosixFilePermissions(serverYaml, Set.of(
                            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                } catch (UnsupportedOperationException | IOException e) {
                    Logger.LOG.warning("Could not tighten permissions on " + serverYaml + ": " + e);
                }
            }
            try (var certs = Files.list(config.dataDir.resolve("certs"))) {
                certs.filter(path -> {
                    String name = path.getFileName().toString();
                    return name.endsWith(".key") || name.endsWith(".p12") || name.endsWith(".pem");
                }).forEach(path -> {
                    try {
                        Files.setPosixFilePermissions(path, Set.of(
                                PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                    } catch (UnsupportedOperationException | IOException e) {
                        Logger.LOG.warning("Could not tighten permissions on " + path + ": " + e);
                    }
                });
            } catch (UnsupportedOperationException | IOException e) {
                // certs/ 尚不存在（从未开过 TLS）或无法列出：没有要迁移的东西。
                Logger.LOG.warning("Could not scan the certs directory for permission migration: " + e);
            }

            // pnpm reads its registry from its own configuration file rather than from the
            // environment, so the source chosen in the panel is written there now as well as when
            // it changes: a container whose home directory was recreated still installs from the
            // mirror it was configured with.
            org.jackhuang.hmcl.dsh.PnpmConfigFile.apply();

            // The workspace is where a session's files live and where the
            // harness process runs: `<home>/workspace` (what the compose file
            // mounts), or HDSL_WORKSPACE when the operator named another
            // folder. Not the directory the launcher keeps its instances in,
            // which is what the domain defaults to.
            // 用 HOME 而不是 user.home：容器里面板是 root，user.home 是 /root，
            // 而 agent 在 uid 1000 下看到的家目录是 /home/hdsl（见 UserHome）。
            WorkspaceDirectory.install(env, org.jackhuang.hmcl.util.platform.UserHome.directory(env), Logger.LOG);

            UserStore users = UserStore.open(config.dataDir, env, Logger.LOG);
            AuthService auth = new AuthService(users, Duration.ofHours(config.auth.sessionTtlHours));
            CertificateManager certificates = CertificateManager.create(config, Logger.LOG);
            HdslServer server = HdslServer.build(config, auth, certificates, Logger.LOG);

            server.start();
            printBanner(config, server, certificates, env);
            server.join();
        } catch (ServerConfigLoader.ConfigException e) {
            System.err.println("Invalid configuration: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            Logger.LOG.error("Failed to start HDSL-web: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    private static void printBanner(ServerConfig config, HdslServer server, CertificateManager certificates,
                                    Map<String, String> env) {
        String scheme = config.https.enabled ? "https" : "http";
        System.out.println();
        System.out.println("HDSL-web " + Metadata.VERSION);
        System.out.println("  listening: " + scheme + "://" + config.bindHost + ":" + server.getPort());
        System.out.println("  open     : " + scheme + "://localhost:" + server.getPort()
                + (config.basePath.isEmpty() ? "/" : config.basePath + "/")
                + "  (first run: this page creates the admin account)");
        if (config.https.enabled && config.https.redirectHttp) {
            System.out.println("  redirect : http://" + config.bindHost + ":" + config.https.redirectPort + " -> https (308)");
        }
        if (config.https.enabled && config.https.redirectSamePort) {
            System.out.println("  redirect : http://<any address>:" + server.getPort()
                    + " -> https (308)  (cleartext on the TLS port)");
        }
        System.out.println("  data dir : " + config.dataDir);
        System.out.println("             (server.yaml / accounts / instances / pnpm store / certificates -");
        System.out.println("              back up or migrate by copying this folder; keep it off the public net)");
        // The bug class behind "the server/brand/store never started": the panel creates a
        // directory as root, the child that has to write it runs as the agent. Invisible on a
        // setgid volume, fatal on a plain bind mount. One line per offender, at boot.
        org.jackhuang.hmcl.util.platform.Privileges.auditAgentRoots(config.dataDir);
        System.out.println("  workspace: " + WorkspaceDirectory.resolve(env,
                org.jackhuang.hmcl.util.platform.UserHome.directory(env)));
        System.out.println("  auth     : " + (config.auth.disabled
                ? "disabled (loopback bind only; do not expose this instance)"
                : "enabled (session cookie)"));
        System.out.println("  tls      : " + switch (certificates.getSource()) {
            case NONE -> "not enabled";
            case PKCS12 -> "PKCS#12 keystore";
            case PEM -> "PEM certificate";
            case SELF_SIGNED -> "self-signed (browsers will warn; configure https.cert_file/https.pkcs12_file to replace)";
        });
        if (!config.basePath.isEmpty()) {
            System.out.println("  base path: " + config.basePath + " (behind a path-stripping reverse proxy)");
        }

        // 身份：容器里面板是 root 并按进程降权（秘密文件 0700），裸部署不是。
        // 裸部署以 root 起面板时不拦，但这句必须说，因为它意味着 agent 也是 root。
        String privileges = org.jackhuang.hmcl.util.platform.Privileges.bannerNote();
        if (privileges != null) {
            System.out.println("  note     : " + privileges);
        }

        // Platform capabilities that change what the panel can offer. The same
        // sentences are shown on the brand tab / MC card; printing them once at
        // boot means a bare-deploy operator does not have to click through the
        // UI to find out why an install button is greyed out.
        for (org.jackhuang.hmcl.web.brand.Brand brand : org.jackhuang.hmcl.web.brand.Brand.values()) {
            String reason = brand.notInstallableReason(
                    org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS);
            if (reason != null) {
                System.out.println("  platform : " + brand.id() + " not installable - " + reason);
            }
        }
        System.out.println("  minecraft: console " + org.jackhuang.hmcl.web.minecraft.McRuntime.consoleKind()
                + (org.jackhuang.hmcl.web.minecraft.McRuntime.consoleKind().equals("pipe")
                    ? " (log + command box; PTY console not enabled on this platform)" : " (interactive terminal)"));

        // ---- 体检段：这两项缺了，面板能启动但"装不了东西 / 打不开页面"，
        // 裸部署（尤其 Windows）控制台是唯一窗口，必须在启动时说清楚 ----
        if (frontendPackaged()) {
            System.out.println("  frontend : packaged");
        } else {
            System.out.println("  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.out.println("  !! web frontend MISSING from this jar - every page    !!");
            System.out.println("  !! answers \"not found\". This build never packaged    !!");
            System.out.println("  !! the SPA (usually: built without Node.js on PATH).  !!");
            System.out.println("  !! Fix - either:                                      !!");
            System.out.println("  !!   1. download the release jar:                     !!");
            System.out.println("  !!      github.com/nas200-timmy/HDSL-WEB/releases     !!");
            System.out.println("  !!   2. or install Node.js LTS, then rebuild:         !!");
            System.out.println("  !!      gradlew shadowJar -PreleaseVersion=...        !!");
            System.out.println("  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
        String node = toolVersion("node", "--version");
        String pnpm = toolVersion("pnpm", "--version");
        if (node != null && pnpm != null) {
            System.out.println("  runtime  : node " + node + ", pnpm " + pnpm);
        } else {
            System.out.println("  runtime  : " + (node == null ? "node MISSING" : "node " + node)
                    + ", " + (pnpm == null ? "pnpm MISSING" : "pnpm " + pnpm));
            System.out.println("  !! installing dsh versions / plugins / brand tools needs");
            System.out.println("  !! node + pnpm. Either:");
            System.out.println("  !!   1. one-click a panel-managed runtime (Settings -> Node.js);");
            System.out.println("  !!   2. or install Node.js LTS yourself: https://nodejs.org");
        }
        if (node != null && !org.jackhuang.hmcl.dsh.DshNodeRuntime.isSupportedNodeVersion(node)) {
            // Worth a line of its own: a distribution package that is two majors behind is the
            // normal way to arrive here, and the two symptoms are "dsh refuses to launch" and
            // "a brand tool dies inside its own ESM loader", neither of which names the version.
            System.out.println("  !! that Node.js is outside the range the tools run on ("
                    + org.jackhuang.hmcl.dsh.DshNodeRuntime.NODE_RANGE + ")");
            System.out.println("  !! dsh instances will refuse to start and some brand tools will crash;");
            System.out.println("  !! the panel's managed runtime (Settings -> Node.js) fixes both");
        }
        if (org.jackhuang.hmcl.util.platform.OperatingSystem.CURRENT_OS
                == org.jackhuang.hmcl.util.platform.OperatingSystem.WINDOWS) {
            // POSIX 0600 has no NTFS equivalent: Java's setReadable maps onto
            // ACLs unpredictably, and the credential file can be readable by
            // other accounts on a multi-user machine. Worth saying out loud.
            System.out.println("  note     : Windows has no POSIX 0600 semantics - the credential file");
            System.out.println("             may be readable by other accounts on this machine. Single-");
            System.out.println("             user machines are fine; keep the data dir off the public net.");
            // Said here, once, because the alternative is a person discovering it from a
            // button that refuses: which terminal this build can run depends on the Windows
            // build, and the number is what they can act on.
            System.out.println("  terminal : " + ConPty.describe());
        }
        System.out.println();
    }

    /// Whether the SPA shell is on the classpath. A classes/dev build without
    /// the packaged frontend boots fine and then answers every page with a bare
    /// "not found" — this check is what turns that into a printed diagnosis.
    private static boolean frontendPackaged() {
        return Main.class.getClassLoader().getResource("web/index.html") != null;
    }

    /// Runs `tool --version`, returning the trimmed first line or `null`.
    /// Best effort: any failure means "not available", and the banner says so.
    private static @org.jetbrains.annotations.Nullable String toolVersion(String tool, String flag) {
        try {
            // `pnpm` is `pnpm.cmd` on Windows and `node` is `node.exe`: a bare name is
            // not a file there. The banner asks the same resolver as the rest of the
            // panel, so it cannot report a tool missing that the panel would go on to
            // use successfully — or the reverse, which is what it did for `pnpm`.
            Process process = new ProcessBuilder(
                    org.jackhuang.hmcl.util.platform.Executables.startableProgram(tool), flag)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) {
                return null;
            }
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                // 版本号不一定是第一行：空 /data 被宿主目录挂载时，corepack 的 shim
                // 会先打一句「Downloading the pnpm … binary...」再输出真正的版本。
                // 取第一行版本形状的输出，全都没有才退回第一行非空内容。
                String first = null;
                for (int i = 0; i < 20; i++) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (first == null) {
                        first = trimmed;
                    }
                    if (trimmed.matches("^v?\\d+\\.\\d+.*")) {
                        return trimmed;
                    }
                }
                return first;
            }
        } catch (java.io.IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }
}
