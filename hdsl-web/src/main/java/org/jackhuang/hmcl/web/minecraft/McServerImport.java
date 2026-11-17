/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web Contributors
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

import org.jetbrains.annotations.NotNullByDefault;

/// **Disabled skeleton** for importing a server's own `server.jar` and plugins
/// (docs/minecraft.md §7.4, "P2").
///
/// Nothing here runs: the class carries no behaviour, and every method below is
/// inside a comment block so the compiler cannot wire it in by accident. It
/// exists so a future change starts from the settled design rather than from
/// the security argument again.
///
/// ## Why it is off
///
/// A Minecraft server is a Java program. Importing one runs its author's code
/// with this panel's uid, which owns `/data`: `auth/users.json` (admin password
/// hashes), `developer.json`, the TLS private keys under `certs/`, every
/// instance's credentials and sessions. Reading a file is enough — no exploit
/// required. The panel has no usable sandbox either: the container has no root
/// and no docker socket, JDK 21 removed SecurityManager, and user namespaces
/// are unreliable on the NAS boxes this panel targets. So "isolated" cannot be
/// promised; the honest framing is "you are trusting the jar's author".
///
/// ## What enabling it would require
///
/// 1. Two doors: developer elevation **and** a typed confirmation phrase (not a
///    checkbox), recorded verbatim in the audit log.
/// 2. Verification that only claims what it can: a vanilla `server.jar` is
///    compared against Mojang's manifest SHA-1 (the same list
///    [McVersions] already uses); everything else (Paper/Forge/Fabric,
///    plugins, mods) only gets a recorded SHA-256 and the label
///    "unrecognised" — never "verified".
/// 3. A permanent marker on the instance ("unverified server program") that
///    survives restarts, with launching behind elevation again.
/// 4. An import root only: a read-only directory mounted into the container
///    (`HDSL_MC_IMPORT_DIR`), never an arbitrary path; no symlink escapes;
///    copy, never move; free-space check; refuse when `session.lock` says the
///    old server is still running.
/// 5. The documentation saying plainly: same uid, so the jar's author is
///    trusted with the whole panel.
///
/// The sections below are the implementation those points imply, kept as
/// comments on purpose. Uncommenting them without the UI/consent work above
/// would ship exactly the risk this class describes.
@NotNullByDefault
public final class McServerImport {

    /// Whether importing a server's own programs is enabled. Kept `false`; the
    /// constant is the single switch a future change would flip, after the
    /// consent and marker work exists.
    public static final boolean ENABLED = false;

    private McServerImport() {
    }

    // =========================================================================
    // Everything below is design, not code. To enable it, delete this marker
    // comment pair and implement the methods in this class, then wire the route
    // in McApiServlet (`case "import"` next to `case "install"`).
    //
    // -------------------------------------------------------------------------
    // Route (McApiServlet):
    //
    //   POST /api/mc/instances/{id}/import/server
    //     body: {"source":"/import/old-server","confirm":"我理解它将以面板身份运行"}
    //     202  {taskId}   — long copy + hash as a task, like `install`
    //     403  developer_elevation_required / confirm_phrase_mismatch
    //     400  import_root_unset / source_outside_root / server_running
    //     409  instance_running
    //
    // -------------------------------------------------------------------------
    // Verification:
    //
    //   record Verdict(String sha256, @Nullable String officialVersion,
    //                  boolean vanilla, String label) {}
    //
    //   static Verdict inspect(Path jar, McVersions versions) throws IOException {
    //       byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(jar));
    //       String sha256 = hex(MessageDigest.getInstance("SHA-256").digest(...));
    //       // Mojang's manifest lists every vanilla server jar as
    //       //   downloads.server.sha1
    //       // [McVersions] already downloads that manifest; match sha1 against
    //       // every version and report the version it belongs to.
    //       String vanilla = versions.versionOfServerSha1(hex(sha1));   // null when unknown
    //       return new Verdict(sha256, vanilla, vanilla != null,
    //               vanilla != null ? "official vanilla " + vanilla : "unrecognised");
    //   }
    //
    // -------------------------------------------------------------------------
    // Import (copy, never move; refuses while the old server runs):
    //
    //   static void importInto(McInstanceManager manager, McInstance instance,
    //                          Path source, Verdict verdict) throws McException {
    //       Path instanceDir = manager.instanceDirectory(instance.id());
    //       // 1. source must be inside HDSL_MC_IMPORT_DIR, realpath-checked;
    //       //    every entry must resolve under it (no symlink escapes).
    //       // 2. refuse when source/session.lock is held:
    //       //    try (FileChannel ch = FileChannel.open(lock, WRITE)) { ... }
    //       // 3. copy with Files.walkFileTree: skip logs/, caches, crash reports;
    //       //    count bytes against free space + reserve (docs §7.2).
    //       // 4. write instanceDir/server.jar = copy, plus a sidecar
    //       //    `server-import.json`:
    //       //      {"source": "...", "sha256": "...", "verdict": "unrecognised",
    //       //       "confirmedAt": "...", "confirmedBy": "...", "phrase": "..."}
    //       // 5. plugins/mods copied as-is; each jar gets its SHA-256 recorded,
    //       //    never "verified".
    //       // 6. EULA: delete the copied eula.txt and require the panel's own
    //       //    acceptance flow (the owner accepts for this instance, not the
    //       //    old one).
    //   }
    //
    // -------------------------------------------------------------------------
    // Marker + launch gate:
    //
    //   the sidecar above is the marker; McApiServlet's `start` refuses unless
    //   request elevation was granted, and instanceJson() adds
    //   {"unverifiedProgram": true} so the list and detail pages can render the
    //   banner. Deleting the instance is the only way to clear it.
    //
    // -------------------------------------------------------------------------
    // Tests that must exist before flipping ENABLED:
    //
    //   - vanilla sha1 matches a fake Mojang manifest entry -> label official;
    //   - unknown sha1 -> "unrecognised", never "verified";
    //   - source outside the import root -> refused;
    //   - symlink pointing outside -> refused;
    //   - session.lock held -> refused;
    //   - missing/incorrect confirm phrase -> 403 with the phrase quoted back;
    //   - launching an unverified instance without elevation -> 403.
    // =========================================================================
}