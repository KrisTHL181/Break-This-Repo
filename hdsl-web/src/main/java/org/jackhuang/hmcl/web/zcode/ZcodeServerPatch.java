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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// One edit to ZCode's own server entry, made once per release: the standalone
/// server owns its project store instead of pretending somebody else does.
///
/// Upstream builds a server that is always a *remote environment*: the project
/// store's owner id is `remote:server:<serverId>` (source:
/// `services/src/node.ts`, `createServerHostServices`), while every client-side
/// navigation defaults the host to `"local"` (`services/src/project-workspace-navigation`,
/// `hostId = request.hostId ?? "local"`). In the desktop app the two never meet —
/// the desktop's own host *is* `local`. In a browser tab it is two different
/// strings forever, and the consequences are exactly the ones this panel kept
/// hitting:
///
/// - `project.readDirectory` throws `project.host-offline` on every folder
///   browse, so the workbench shows no directory and the picker cannot open one;
/// - `project.prepareSession` without a project id throws
///   `project.remote-project-required`, so the initial tab cannot send a message
///   and a session can only exist under a project that the same broken picker
///   cannot create;
/// - the agent runtime is never started by either path, so
///   `subscribeSessionsIndexV4` answers `ZCode Agent runtime is not running`.
///
/// The panel's model is the one the patch restores: one process on the
/// operator's own machine, with the browser as its only client. Pinning the owner
/// to `local` makes the server's store look like the desktop's local store, which
/// is what a self-hosted panel is.
///
/// **Every platform, since v0.6.45.** This used to be Windows-only, on the theory
/// that a Linux deployment has a second intended client: the ZCode desktop
/// application can connect to the panel's server as its host, and
/// `remote:server:<id>` is the owner that model needs. That theory cost every
/// operator who does not use the desktop app — which is most of them — the three
/// symptoms above, on Linux exactly as on Windows. The browser is the client the
/// panel actually serves, so the owner is pinned everywhere; the one deployment
/// that does use the desktop as a host can set `HDSL_ZCODE_KEEP_REMOTE=1` and get
/// upstream's owner back, unpatched.
///
/// **This is a patch to upstream's shipped code.** The anchor is the exact
/// expression upstream writes today; when it is gone the panel says so once and
/// runs the release unpatched (the workbench then behaves as it did before, which
/// is a working page minus its project features) rather than refusing to start.
@NotNullByDefault
public final class ZcodeServerPatch {

    /// The server entry inside a release: `server/entry-http.js`, the file
    /// `bin/zcode.mjs` spawns for `--web`.
    static final String SERVER_ENTRY = "server/entry-http.js";

    /// The escape hatch for a deployment whose second client really is the ZCode
    /// desktop application: `1` keeps upstream's `remote:server:<id>` owner.
    static final String KEEP_REMOTE_ENV = "HDSL_ZCODE_KEEP_REMOTE";

    /// What upstream writes: the owner id derived from the server's identity.
    ///
    /// Matched by shape rather than by text because the argument names are the
    /// bundle's own (`options2` in 3.17.1); the statement itself is what has to
    /// hold. `[^;]*?` keeps it inside one statement.
    private static final Pattern OWNER_EXPRESSION = Pattern.compile(
            "const\\s+projectStorageHostId\\s*=\\s*remoteHostIdFromWorkspaceIdentity\\s*\\("
                    + "[^;]*?\\)\\s*;");

    /// What it becomes. The `throw` upstream keeps right after it stays satisfied:
    /// the literal is never falsy.
    private static final String OWNER_LOCAL = "const projectStorageHostId = \"local\";";

    private ZcodeServerPatch() {
    }

    /// Whether a release built on this machine gets the patch.
    ///
    /// Every platform, unless the operator says otherwise. The browser workbench
    /// is the only client a panel deployment serves, and with upstream's owner it
    /// cannot list a directory, create a project, or start a session — on Linux
    /// exactly as on Windows. A deployment that connects the ZCode desktop
    /// application as a host sets [#KEEP_REMOTE_ENV] and is left alone.
    ///
    /// @return whether a release built on this machine carries the patch
    public static boolean applies() {
        return !keepRemote(System.getenv(KEEP_REMOTE_ENV));
    }

    /// The opt-out's parsing, named so a test can drive it without an environment.
    ///
    /// @param value the raw variable, or `null`
    /// @return whether upstream's remote owner should be kept
    static boolean keepRemote(@Nullable String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && !"0".equals(trimmed) && !"false".equalsIgnoreCase(trimmed);
    }

    /// Patches a release in place. **Build time only** — this is the distro patch
    /// that ships in every release the panel produces, not something a launch does
    /// to somebody else's files.
    ///
    /// The file is read, checked and only written when the expression is really
    /// there; a release that is already patched is left byte-for-byte alone, and a
    /// deployment that opted out ([#KEEP_REMOTE_ENV]) is not touched at all
    /// ([Outcome#NOT_APPLICABLE]).
    ///
    /// @param release the release root (the directory holding `server/` and `bin/`)
    /// @return what happened; the builder fails the build on [Outcome#ANCHOR_MISSING]
    public static Outcome patch(Path release) {
        return applies() ? rewrite(release) : Outcome.NOT_APPLICABLE;
    }

    /// Reads a release and says whether it carries the patch — **without writing
    /// anything**.
    ///
    /// This is what a launch does: a release this panel built is already patched,
    /// and one that was dropped in by hand (or built elsewhere) is reported once
    /// and left alone. Editing an installed release at start would make the same
    /// bytes behave differently depending on who started them.
    ///
    /// @param release the release root (the directory holding `server/` and `bin/`)
    /// @return what the file says now
    public static Outcome verify(Path release) {
        return applies() ? inspect(release) : Outcome.NOT_APPLICABLE;
    }

    /// The mechanics of [patch], without the platform question — package-private so
    /// the rules stay testable on a machine the patch does not ship for.
    ///
    /// @param release the release root
    /// @return what the rewrite found
    static Outcome rewrite(Path release) {
        Outcome inspected = inspect(release);
        if (inspected != Outcome.NEEDS_PATCH) {
            return inspected;
        }
        Path entry = release.resolve(SERVER_ENTRY);
        try {
            String source = Files.readString(entry, StandardCharsets.UTF_8);
            String patched = OWNER_EXPRESSION.matcher(source)
                    .replaceFirst(Matcher.quoteReplacement(OWNER_LOCAL));
            Files.writeString(entry, patched, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warning("Could not write the project-owner patch into " + entry, e);
            return Outcome.UNREADABLE;
        }
        return Outcome.PATCHED;
    }

    /// The mechanics of [verify], without the platform question.
    ///
    /// @param release the release root
    /// @return what the file says
    static Outcome inspect(Path release) {
        Path entry = release.resolve(SERVER_ENTRY);
        if (!Files.isRegularFile(entry)) {
            return Outcome.NOT_A_SERVER;
        }
        String source;
        try {
            source = Files.readString(entry, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warning("Could not read " + entry + " to check the project-owner patch", e);
            return Outcome.UNREADABLE;
        }
        if (OWNER_EXPRESSION.matcher(source).find()) {
            return Outcome.NEEDS_PATCH;
        }
        // Either we patched it, or upstream changed the shape of the statement.
        // Only one of those is worth saying out loud at every launch.
        return source.contains(OWNER_LOCAL) ? Outcome.PATCHED_ALREADY : Outcome.ANCHOR_MISSING;
    }

    /// Reports the patch's state to the panel log, once per launch.
    ///
    /// @param release  the release that was checked
    /// @param outcome  what [verify] found
    static void report(Path release, @Nullable Outcome outcome) {
        if (outcome == null) {
            return;
        }
        switch (outcome) {
            case ANCHOR_MISSING -> LOG.warning("ZCode server in " + release.getFileName()
                    + " does not carry the project-owner expression this panel patches;"
                    + " the web workbench will show no workspace and cannot create projects."
                    + " The page still opens — see docs/zcode-experimental.md for the anchor");
            case NEEDS_PATCH -> LOG.warning("ZCode release " + release.getFileName()
                    + " was not built by this panel and still ships upstream's project-owner"
                    + " expression: the web workbench will show no workspace and cannot create"
                    + " projects. Rebuild it from the panel's version card to get the distro patch");
            case PATCHED, PATCHED_ALREADY, NOT_APPLICABLE -> {
                // Nothing to say at launch: a release is patched once, in the build,
                // and started many times; where the patch does not apply, nothing was
                // ever expected of it.
            }
            case UNREADABLE, NOT_A_SERVER -> LOG.warning("Could not check " + release
                    + " for the project-owner patch (" + outcome + ")");
        }
    }

    /// What [patch] and [verify] found.
    public enum Outcome {
        /// [patch] found the expression and wrote the replacement.
        PATCHED,
        /// The file already says `"local"`.
        PATCHED_ALREADY,
        /// The panel was told not to patch (see [applies]) — nothing was read and
        /// nothing was written.
        NOT_APPLICABLE,
        /// The expression is there and nothing was written — [verify]'s way of
        /// saying "this is not the patched bytes this panel builds".
        NEEDS_PATCH,
        /// The file is a server entry but the expression is gone: upstream changed
        /// it, and the workbench will behave as it did before the patch existed.
        ANCHOR_MISSING,
        /// The release has no `server/entry-http.js` (a TUI-only build).
        NOT_A_SERVER,
        /// The file could not be read or written.
        UNREADABLE
    }
}
