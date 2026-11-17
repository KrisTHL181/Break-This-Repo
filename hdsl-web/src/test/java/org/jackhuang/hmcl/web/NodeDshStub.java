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

import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshVersion;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/// A fake dsh for tests that must launch a real process: a node script that
/// speaks the one stdout contract the launcher relies on — the readiness line
/// `dsh web: http://127.0.0.1:<port>/?token=…` — and, optionally, serves a
/// small HTTP stub: an echo endpoint for the header and body contracts, a
/// cookie endpoint, an SSE endpoint, a page shaped like the generated one
/// (gzipped) with a plain page beside it, and redirects rooted, loopback-absolute
/// and foreign.
///
/// The script doubles as the instance's `bin.js`, so it must also answer the
/// `--help` probe the launcher runs to decide about `--no-open`; it mentions
/// the flag, which is what a modern release does.
///
/// [#installPluginCapable] additionally answers the `plugin` subcommand the
/// plugin installer drives: `add`/`remove` edit the profile's `package.json` and
/// lay down (or take away) the package directory under `node_modules` the way
/// `dsh plugin` + pnpm would — a stub fetches nothing, but it does leave an
/// installed package behind, because the panel checks that an install really
/// landed before it calls it one — and, when asked, it plays the build-script
/// refusal pnpm prints until somebody answers its `pnpm-workspace.yaml`
/// placeholder.
public final class NodeDshStub {

    public static final String TOKEN = "testtoken123";

    private NodeDshStub() {
    }

    /// Skips the calling test when no usable node is on PATH.
    public static void assumeNode() {
        Assumptions.assumeTrue(nodeAvailable(), "node is required on PATH for this test");
    }

    public static boolean nodeAvailable() {
        try {
            Process process = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
            return process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /// Writes the stub into the instance's dsh directory so
    /// [org.jackhuang.hmcl.dsh.DshVersionManager#isInstalled] sees the
    /// instance as installed and [org.jackhuang.hmcl.dsh.DshLauncher] finds
    /// an entry script.
    ///
    /// @param instance the instance
    /// @param serve    whether the stub serves HTTP after announcing readiness;
    ///                 a passive stub only prints the readiness line and idles
    public static void install(DshInstance instance, boolean serve) throws IOException {
        Path bin;
        try {
            bin = instance.dshDirectory().resolve(DshVersion.PACKAGE_PATH).resolve("lib/bin.js");
        } catch (org.jackhuang.hmcl.dsh.DshException e) {
            throw new IOException("cannot resolve the stub path", e);
        }
        Files.createDirectories(bin.getParent());
        Files.writeString(bin, script(serve));
    }

    /// Writes a stub that also answers `dsh plugin --profile <p> add|remove|install`.
    ///
    /// @param instance      the instance
    /// @param needsApproval whether `add` refuses with pnpm's ignored-build
    ///                       output until the profile's `pnpm-workspace.yaml`
    ///                       answers `node-pty:` with true or false — the flow
    ///                       that parks the install task in `waiting_approval`
    public static void installPluginCapable(DshInstance instance, boolean needsApproval) throws IOException {
        writePluginScript(instance, pluginScript(needsApproval, true));
    }

    /// Writes a stub whose `plugin add` reports success **without** leaving the package
    /// behind.
    ///
    /// This is the shape a fetch that fails after the dependency was written leaves, and it
    /// is what the panel has to refuse at install time: the profile would otherwise be
    /// recorded as installed while `node_modules` holds nothing, which is how a terminal
    /// comes to die in a fraction of a second with an empty screen.
    ///
    /// @param instance the instance
    public static void installPluginCapableLayingNothing(DshInstance instance) throws IOException {
        writePluginScript(instance, pluginScript(false, false));
    }

    private static void writePluginScript(DshInstance instance, String script) throws IOException {
        Path bin;
        try {
            bin = instance.dshDirectory().resolve(DshVersion.PACKAGE_PATH).resolve("lib/bin.js");
        } catch (org.jackhuang.hmcl.dsh.DshException e) {
            throw new IOException("cannot resolve the stub path", e);
        }
        Files.createDirectories(bin.getParent());
        Files.writeString(bin, script);
    }

    /// The stub program.
    private static String script(boolean serve) {
        String serveBlock = serve ? """
                const http = require("http");
                const zlib = require("zlib");
                const server = http.createServer((req, res) => {
                  const url = new URL(req.url, "http://127.0.0.1");
                  if (url.pathname === "/echo") {
                    let body = "";
                    req.on("data", (chunk) => (body += chunk));
                    req.on("end", () => {
                      res.writeHead(200, {"content-type": "application/json"});
                      res.end(JSON.stringify({
                        path: url.pathname + url.search,
                        host: req.headers.host,
                        origin: req.headers.origin || null,
                        cookie: req.headers.cookie || null,
                        method: req.method,
                        body,
                      }));
                    });
                  } else if (url.pathname === "/cookie") {
                    res.writeHead(200, {"set-cookie": ["x=1; Path=/", "y=2; Path=/; HttpOnly"]});
                    res.end("cookie set");
                  } else if (url.pathname === "/sse") {
                    res.writeHead(200, {"content-type": "text/event-stream", "cache-control": "no-cache"});
                    res.write("data: one\\n\\n");
                    setTimeout(() => res.write("data: two\\n\\n"), 50);
                    setTimeout(() => res.end("data: three\\n\\n"), 100);
                  } else if (url.pathname === "/page") {
                    // A page shaped like the one dsh generates: the base fixes
                    // relative references at the origin, the two attributes and
                    // the boot payload name their plugins absolutely, and the
                    // manifest is asked for relatively. Sent gzipped, which is
                    // how dsh sends it.
                    const html = `<!doctype html><html><head>`
                      + `<base href="/">`
                      + `<link rel="manifest" href="./manifest.webmanifest" />`
                      + `<link rel="preload" href="/plugins/one/client.js" as="script">`
                      + `<script src="/plugins/two/client.js"></script>`
                      + `<script>globalThis["__DSH_BOOT__"]={"entries":[{"url":"/plugins/three/client.js"}]}</script>`
                      + `</head><body>the dsh page</body></html>`;
                    const gzipped = zlib.gzipSync(Buffer.from(html, "utf8"));
                    res.writeHead(200, {
                      "content-type": "text/html; charset=utf-8",
                      "content-encoding": "gzip",
                      "content-length": String(gzipped.length),
                    });
                    res.end(gzipped);
                  } else if (url.pathname === "/plain-page") {
                    // Must stay identical to the expectation in
                    // ProxyBehaviorTest#leavesAPageWithoutABaseHrefUntouched: the
                    // proxy hands a page it does not recognize over untouched.
                    res.writeHead(200, {"content-type": "text/html; charset=utf-8"});
                    res.end(`<!doctype html><html><head><title>plain</title></head><body>plain page</body></html>`);
                  } else if (url.pathname === "/redirect/root") {
                    res.writeHead(303, {"location": "/"});
                    res.end();
                  } else if (url.pathname === "/redirect/self") {
                    res.writeHead(303, {"location": "http://127.0.0.1:" + server.address().port + "/foo?x=1"});
                    res.end();
                  } else if (url.pathname === "/redirect/localhost") {
                    res.writeHead(303, {"location": "http://localhost:" + server.address().port + "/bar"});
                    res.end();
                  } else if (url.pathname === "/redirect/external") {
                    res.writeHead(302, {"location": "https://dsh.example.com/x"});
                    res.end();
                  } else {
                    res.writeHead(200, {"content-type": "text/plain"});
                    res.end("stub ok: " + req.url);
                  }
                });
                server.listen(port, "127.0.0.1", () => {
                  console.log("dsh web: http://127.0.0.1:" + server.address().port + "/?token=TOKEN");
                });
                """ : """
                console.log("dsh web: http://127.0.0.1:" + port + "/?token=TOKEN");
                """;
        return """
                const [, , ...args] = process.argv;
                if (args.includes("--help")) {
                  console.log("Usage: dsh [--profile p] [--no-open] [--port p] [--trusted-host h]");
                  process.exit(0);
                }
                console.error("stub argv: " + args.join(" "));
                const portIndex = args.indexOf("--port");
                const port = portIndex >= 0 ? parseInt(args[portIndex + 1], 10) : 0;
                %s
                setInterval(() => {}, 1 << 30);
                """.formatted(serveBlock).replace("TOKEN", TOKEN);
    }

    /// The plugin-capable stub program.
    ///
    /// `plugin --profile <p> add <spec…>` records each spec in the profile's
    /// `package.json` (`name@1.2.3` pins, a bare name is `*`, a local path is
    /// a `file:` dependency named after the archive). When `needsApproval` is
    /// set, `add` first writes pnpm's placeholder into `pnpm-workspace.yaml`
    /// and exits 1 with the ignored-build output, exactly as pnpm does, until
    /// the file answers the entry — which is what the launcher's approval
    /// dance reads and answers.
    private static String pluginScript(boolean needsApproval, boolean laysFiles) {
        String approvalBlock = needsApproval ? """
                  const wsFile = path.join(profileDir, "pnpm-workspace.yaml");
                  const wsText = fs.existsSync(wsFile) ? fs.readFileSync(wsFile, "utf8") : "";
                  const answered = /^\\s*node-pty: (true|false)\\s*$/m.test(wsText);
                  if (!answered) {
                    if (!wsText.includes("allowBuilds:")) {
                      fs.writeFileSync(wsFile, "allowBuilds:\\n  node-pty: set this to true or false\\n");
                    }
                    console.log(" ERR_PNPM_IGNORED_BUILDS  Ignored build scripts: node-pty");
                    console.log("Ignored build scripts: node-pty");
                    process.exit(1);
                  }
                """ : "";
        return """
                const [, , ...args] = process.argv;
                if (args.includes("--help")) {
                  console.log("Usage: dsh [--profile p] [--no-open] [--port p] [--trusted-host h]");
                  process.exit(0);
                }
                const fs = require("fs");
                const path = require("path");
                const home = process.env.DSH_HOME;
                // Whether an `add` really leaves the package behind. False is the variant a
                // fetch that fails after the dependency was written produces.
                const laysFiles = %s;
                if (args[0] === "plugin") {
                  const profile = args[args.indexOf("--profile") + 1];
                  const pnpmArgs = args.slice(args.indexOf("--profile") + 2);
                  const profileDir = path.join(home, "profiles", profile);
                  fs.mkdirSync(profileDir, { recursive: true });
                  const manifestFile = path.join(profileDir, "package.json");
                  let manifest = { dependencies: {}, dsh: { profile: { bundles: [] } } };
                  if (fs.existsSync(manifestFile)) {
                    manifest = JSON.parse(fs.readFileSync(manifestFile, "utf8"));
                  }
                  const op = pnpmArgs[0];
                  const write = () => fs.writeFileSync(manifestFile, JSON.stringify(manifest, null, 2) + "\\n");
                  // pnpm leaves the package's files behind, and the panel checks that an
                  // install really landed (DshPluginInstaller.requireLanded): a stub that
                  // only edited the manifest would be describing an install that never
                  // happened, and every install test would then be testing the failure.
                  // A variant that reports success without laying anything down is what the
                  // panel has to catch, and lives behind the flag below.
                  const lay = (name) => {
                    if (!laysFiles) {
                      return;
                    }
                    const dir = path.join(profileDir, "node_modules", name);
                    fs.mkdirSync(path.join(dir, "lib"), { recursive: true });
                    fs.writeFileSync(path.join(dir, "lib", "main.js"), "// " + name + "\\n");
                    const bin = name.split("/").pop();
                    fs.writeFileSync(path.join(dir, "package.json"), JSON.stringify(
                      { name: name, version: "0.0.0", bin: { [bin]: "lib/main.js" } }, null, 2) + "\\n");
                  };
                  const unlay = (name) => fs.rmSync(path.join(profileDir, "node_modules", name),
                    { recursive: true, force: true });
                  if (op === "add") {
                %s
                    for (const spec of pnpmArgs.slice(1)) {
                      const parsed = parseSpec(spec);
                      manifest.dependencies[parsed[0]] = parsed[1];
                      lay(parsed[0]);
                    }
                    write();
                    console.log("added " + (pnpmArgs.length - 1) + " package(s)");
                    process.exit(0);
                  }
                  if (op === "remove") {
                    for (const spec of pnpmArgs.slice(1)) {
                      delete manifest.dependencies[spec];
                      unlay(spec);
                    }
                    write();
                    console.log("removed " + (pnpmArgs.length - 1) + " package(s)");
                    process.exit(0);
                  }
                  if (op === "install") {
                    write();
                    process.exit(0);
                  }
                  console.error("stub: unknown plugin args " + pnpmArgs.join(" "));
                  process.exit(1);
                }
                function parseSpec(spec) {
                  if (spec.startsWith("/") || /^[A-Za-z]:/.test(spec)) {
                    const base = path.basename(spec).replace(/\\.tar\\.gz$|\\.tgz$/, "");
                    return [base, "file:" + spec];
                  }
                  const at = spec.lastIndexOf("@");
                  if (at > 0) {
                    return [spec.slice(0, at), spec.slice(at + 1)];
                  }
                  return [spec, "*"];
                }
                console.error("stub argv: " + args.join(" "));
                process.exit(1);
                """.formatted(laysFiles ? "true" : "false", approvalBlock);
    }
}
