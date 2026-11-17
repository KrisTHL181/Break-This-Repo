#!/usr/bin/env node
// Fake pnpm for the ZcodeBuilder tests. ZcodeBuilder runs three commands —
// `pnpm install`, `pnpm exec tsc -b packages/shared` and `pnpm build:zcode
// --out-dir <dir> --base-url <url>` — and this stub answers all three without
// network or a real monorepo: the first is a no-op, the second writes the
// `packages/shared/dist/index.js` the builder refuses to continue without, and
// the third lays down exactly what build-zcode.mjs promises (a release tarball
// below <out>/releases/<v>/, its sha256.txt, and latest.json naming the two), so
// the builder's verify-and-install path runs for real.
//
// FAKE_ZCODE_VERSION overrides the version the stub reports.
//
// Every invocation is recorded so a test can assert what the builder passed on
// the command line: one line per call, the argv joined by spaces, plus a line
// naming ELECTRON_MIRROR when the builder put one in the child's environment.
// The file is `$HDSL_FAKE_PNPM_ARGS_FILE` when set — the convention
// `src/test/resources/fake-zcode/bin/zcode.mjs` uses — and `pnpm.args` beside
// this file otherwise, because the builder passes the child no environment of
// its own.
//
// JavaScript, not a shell script, because the stub has to be spawnable on
// Windows too, where an extensionless `#!/bin/sh` file is refused outright by
// CreateProcess. The POSIX `pnpm` and the Windows `pnpm.cmd` beside this file
// are one-line wrappers around it, so there is one implementation rather than
// two that drift apart.

import { appendFileSync, mkdirSync, readFileSync, rmSync, writeFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
import { dirname, join, relative } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);

/// Stops the run with an error, the way `set -eu` and an explicit `exit 2` do.
function fail(message) {
    console.error(message);
    process.exit(2);
}

// Record the invocation. `pnpm.args` beside this file is the default because the
// builder gives the child no environment of its own; a test that wants the file
// elsewhere names it, which is the convention the zcode fixture already uses.
const argsFile = process.env.HDSL_FAKE_PNPM_ARGS_FILE || join(here, "pnpm.args");
appendFileSync(argsFile, args.join(" ") + "\n");
if (process.env.ELECTRON_MIRROR) {
    appendFileSync(argsFile, "ELECTRON_MIRROR=" + process.env.ELECTRON_MIRROR + "\n");
}

/// One argument, read from the flag that introduces it.
function argumentOf(flag) {
    const index = args.indexOf(flag);
    return index >= 0 && index + 1 < args.length ? args[index + 1] : null;
}

switch (args[0]) {
    case "install": {
        console.log("fake pnpm install (no-op)");
        process.exit(0);
    }
    case "exec": {
        // `pnpm exec tsc -b packages/shared`: the project-reference build that produces the
        // `packages/shared/dist/index.js` the builder checks for and upstream's own build
        // does not make. Written relative to the working directory, which is the sources tree.
        mkdirSync(join(process.cwd(), "packages", "shared", "dist"), { recursive: true });
        writeFileSync(join(process.cwd(), "packages", "shared", "dist", "index.js"), "export {};\n");
        console.log("fake pnpm exec (no-op)");
        process.exit(0);
    }
    case "build:zcode": {
        const out = argumentOf("--out-dir");
        if (!out) {
            fail("fake pnpm: build:zcode without --out-dir");
        }
        const version = process.env.FAKE_ZCODE_VERSION || "9.9.9";
        console.log("fake pnpm: building zcode " + version + " into " + out);

        const staging = join(out, "staging");
        const release = join(out, "releases", version);
        mkdirSync(join(staging, "zcode", "bin"), { recursive: true });
        mkdirSync(release, { recursive: true });
        writeFileSync(join(staging, "zcode", "bin", "zcode.mjs"),
            '#!/usr/bin/env node\nconsole.log("fake zcode web");\n');
        writeFileSync(join(staging, "zcode", "package.json"),
            JSON.stringify({ name: "zcode", version }) + "\n");

        // A real tarball, because the builder's next step runs the real `tar` over it.
        // Relative paths with a working directory rather than absolute ones: GNU tar — which
        // is what `tar` is wherever Git for Windows is installed, and it comes first on PATH
        // — reads `C:\…` as a *remote host* named `C` and refuses with "Cannot connect to
        // C: resolve failed". The same trap the builder itself has to avoid.
        const tarball = join(release, "zcode-" + version + ".tar.gz");
        const tar = spawnSync("tar",
            ["-czf", relative(out, tarball), "-C", relative(out, staging), "zcode"],
            { cwd: out, stdio: "inherit" });
        if (tar.error || tar.status !== 0) {
            fail("fake pnpm: tar failed: " + (tar.error ? tar.error.message : "exit " + tar.status));
        }
        rmSync(staging, { recursive: true, force: true });

        const sha = createHash("sha256").update(readFileSync(tarball)).digest("hex");
        writeFileSync(join(release, "sha256.txt"), sha + "\n");
        writeFileSync(join(out, "latest.json"), JSON.stringify({
            version,
            tarball: "zcode-" + version + ".tar.gz",
            sha256: sha,
            baseUrl: "https://example.invalid/zcode/",
        }) + "\n");
        console.log("fake pnpm: build done");
        process.exit(0);
    }
    default: {
        fail("fake pnpm: unsupported command: " + args.join(" "));
    }
}
