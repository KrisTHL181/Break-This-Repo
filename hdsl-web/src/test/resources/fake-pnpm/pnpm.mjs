#!/usr/bin/env node
// Fake pnpm for the HDSL-web test JVM (wired in build.gradle.kts).
//
// It intercepts exactly one call shape — `pnpm install --dir <staging>`, the
// harness install DshVersionManager drives — and only while the marker file
// named by HDSL_FAKE_PNPM_MARKER exists. Instead of downloading
// @deepseek-ai/dsh from the registry it lays down a stub harness whose bin.js
// speaks the one stdout contract the launcher relies on (the readiness line),
// so pack installs run end-to-end in tests with no network and no 400 MB
// download. Every other call, and every call while the marker is absent, is
// delegated to the real pnpm found further down PATH.
//
// The stub reports the version the manifest asked for, exactly as pnpm would:
// the panel reads it back from the installed package.json and refuses to
// record anything else. Two versions are special, because a stub is the only
// way to stage what a misbehaving registry would do:
//
//   9.9.9-slow      waits three seconds, so a test can watch INSTALLING
//   9.9.9-mismatch  lays down 0.0.0-other, so a test can watch the read-back
//                   guard refuse the version
//
// This is JavaScript rather than a shell script because the stub has to be
// spawnable on Windows too, where the only thing on PATH under the name `pnpm`
// is a `.cmd` — and where an extensionless `#!/bin/sh` file is refused outright
// by CreateProcess. The POSIX `pnpm` and the Windows `pnpm.cmd` beside this
// file are one-line wrappers around it, so there is one implementation rather
// than two that drift apart.

import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const marker = process.env.HDSL_FAKE_PNPM_MARKER;

// The stub harness's `bin.js`: reads a port from its own argv and prints the
// readiness line the launcher parses, then stays up like a server does.
const STUB_BIN = `const [, , ...args] = process.argv;
if (args.includes("--help")) {
  console.log("Usage: dsh [--profile p] [--no-open] [--port p] [--trusted-host h]");
  process.exit(0);
}
if (args[0] === "plugin") {
  process.exit(0);
}
console.error("stub argv: " + args.join(" "));
const portIndex = args.indexOf("--port");
const port = portIndex >= 0 ? parseInt(args[portIndex + 1], 10) : 0;
console.log("dsh web: http://127.0.0.1:" + port + "/?token=packstubtoken");
setInterval(() => {}, 1 << 30);
`;

/// One argument, read from the flag that introduces it.
function argumentOf(flag) {
    const index = args.indexOf(flag);
    return index >= 0 && index + 1 < args.length ? args[index + 1] : null;
}

if (marker && existsSync(marker) && args[0] === "install") {
    const dir = argumentOf("--dir");
    if (dir) {
        let version = "0.0.0-stub";
        try {
            // Read out of the raw text rather than a parsed document: what the launcher
            // writes is a package.json with an `overrides` entry, not a dependency, and
            // the version is the only thing wanted from it.
            const declared = /"@deepseek-ai\/dsh"\s*:\s*"([^"]*)"/.exec(
                readFileSync(join(dir, "package.json"), "utf8"));
            if (declared) {
                version = declared[1];
            }
        } catch {
            // No manifest to read; the stub version stands.
        }

        if (version === "9.9.9-slow") {
            spawnSync(process.execPath, ["-e", "setTimeout(() => {}, 3000)"]);
        } else if (version === "9.9.9-mismatch") {
            version = "0.0.0-other";
        }

        const pkg = join(dir, "node_modules", "@deepseek-ai", "dsh");
        const boot = join(dir, "node_modules", "@deepseek-ai", "dsh-app-boot");
        mkdirSync(join(pkg, "lib"), { recursive: true });
        mkdirSync(join(boot, "lib"), { recursive: true });
        writeFileSync(join(pkg, "package.json"),
            JSON.stringify({ name: "@deepseek-ai/dsh", version, private: true }) + "\n");
        writeFileSync(join(boot, "package.json"),
            JSON.stringify({ name: "@deepseek-ai/dsh-app-boot", version, private: true }) + "\n");
        writeFileSync(join(pkg, "lib", "bin.js"), STUB_BIN);
        console.log("fake pnpm: stub harness laid down in " + dir);
        process.exit(0);
    }
}

// Delegate: remove this directory from PATH and re-resolve pnpm, so the real one
// runs instead of this stub calling itself.
const windows = process.platform === "win32";
const names = windows ? ["pnpm.cmd", "pnpm.exe", "pnpm.bat", "pnpm"] : ["pnpm"];
for (const entry of (process.env.PATH ?? "").split(windows ? ";" : ":")) {
    if (!entry || entry === here) {
        continue;
    }
    for (const name of names) {
        const candidate = join(entry, name);
        if (existsSync(candidate)) {
            const result = spawnSync(candidate, args, { stdio: "inherit" });
            process.exit(result.status ?? 1);
        }
    }
}
console.error("fake pnpm: no real pnpm below this one on PATH");
process.exit(127);
