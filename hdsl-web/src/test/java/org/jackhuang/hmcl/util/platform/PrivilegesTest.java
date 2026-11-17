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
package org.jackhuang.hmcl.util.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// When the panel drops its children to the image's uid, and when it must not.
///
/// The rule that matters is the "must not" half: on a bare host uid 1000 belongs
/// to whoever the administrator made it, so dropping an agent to that number
/// would hand it somebody else's account. The decision therefore requires a
/// container **and** root, and both are named as inputs here so the tests do not
/// depend on the machine they run on.
class PrivilegesTest {

    @Test
    void aMarkerFileSaysContainer(@TempDir Path directory) throws Exception {
        Path marker = Files.createFile(directory.resolve("dockerenv"));
        assertTrue(Privileges.detect(marker, directory.resolve("absent"), directory.resolve("no-cgroup")));
    }

    @Test
    void podmansMarkerCountsToo(@TempDir Path directory) throws Exception {
        Path marker = Files.createFile(directory.resolve("containerenv"));
        assertTrue(Privileges.detect(directory.resolve("absent"), marker, directory.resolve("no-cgroup")));
    }

    @Test
    void aCgroupPathThatNamesARuntimeSaysContainer(@TempDir Path directory) throws Exception {
        Path cgroup = Files.writeString(directory.resolve("cgroup"),
                "0::/docker/0123456789abcdef\n");
        assertTrue(Privileges.detect(directory.resolve("absent"), directory.resolve("absent"), cgroup));

        Path systemd = Files.writeString(directory.resolve("cgroup-systemd"),
                "0::/system.slice/hdsl-web.service\n");
        assertFalse(Privileges.detect(directory.resolve("absent"), directory.resolve("absent"), systemd));
    }

    @Test
    void nothingReadableMeansNotAContainer(@TempDir Path directory) {
        assertFalse(Privileges.detect(directory.resolve("absent"), directory.resolve("absent"),
                directory.resolve("absent")));
    }

    @Test
    void droppingPrefixesSetprivAndKeepsTheCommandIntact() {
        List<String> command = List.of("/opt/node/bin/node", "--version");

        List<String> wrapped = Privileges.wrap(command, true);

        assertEquals("--reuid=" + Privileges.AGENT_UID, wrapped.get(1), wrapped.toString());
        assertEquals("--regid=" + Privileges.AGENT_UID, wrapped.get(2), wrapped.toString());
        assertEquals("--init-groups", wrapped.get(3), wrapped.toString());
        assertEquals("--", wrapped.get(4), wrapped.toString());
        assertEquals(command, wrapped.subList(5, wrapped.size()),
                "the agent's argv must arrive unchanged after the separator");
        assertTrue(wrapped.get(0).endsWith("setpriv"), wrapped.get(0));
    }

    @Test
    void notDroppingHandsTheCommandBackUntouched() {
        List<String> command = List.of("/opt/node/bin/node", "--version");
        assertEquals(command, Privileges.wrap(command, false));
        assertEquals(List.of(), Privileges.wrap(List.of(), true));
    }

    @Test
    void theAgentUidIsConfigurableBecauseSomeStorageProtectsOnlyOtherNumbers() {
        assertEquals(1000, Privileges.agentUid(null));
        assertEquals(1000, Privileges.agentUid("  "));
        assertEquals(1001, Privileges.agentUid("1001"));
        assertEquals(1000, Privileges.agentUid("not-a-number"), "a typo must not become uid 0");
        assertEquals(1000, Privileges.agentUid("0"));
        assertEquals(1000, Privileges.agentUid("70000"));
    }

    @Test
    void handingAFileToTheAgentDoesNothingWhereNobodyWasDropped(@TempDir Path directory) throws Exception {
        // The rule under test is the side effect nobody expects: on a bare deployment the panel
        // is the same person the agent is, so an ownership change here would be the panel taking
        // somebody's file. Handing over only has meaning where children were dropped.
        Path file = Files.writeString(directory.resolve("config.yaml"), "registry: https://a.example\n");
        java.util.Optional<java.util.Set<java.nio.file.attribute.PosixFilePermission>> before =
                org.jackhuang.hmcl.util.PosixPermissions.of(file);

        boolean handed = Privileges.handToAgent(
                file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-rw-r--"));

        if (Privileges.dropsChildren()) {
            assertTrue(handed, "inside the container the file goes to the agent");
            return;
        }
        assertFalse(handed, "outside the container nothing is handed over");
        assertEquals(before, org.jackhuang.hmcl.util.PosixPermissions.of(file),
                "and the mode is left as it was");
    }

    @Test
    void handingOverChangesTheModeAsWellAsTheOwner(@TempDir Path directory) throws Exception {
        // The container's numbers are out of a test's reach, but the two calls they turn into
        // are not: chown to a uid, then chmod. Driving it with the uid the test already is keeps
        // it legal without privileges and still covers the part that can be got wrong — the
        // order. `chown` clears the setgid bit, so a mode set before it is a mode lost.
        Path file = Files.writeString(directory.resolve("config.yaml"), "registry: https://a.example\n");
        if (org.jackhuang.hmcl.util.PosixPermissions.of(file).isEmpty()) {
            return; // no mode bits on this platform: nothing this test can assert
        }
        int uid = Privileges.euid();
        int gid = egid();
        if (uid < 0 || gid < 0) {
            return;
        }

        assertTrue(Privileges.handTo(file, java.nio.file.attribute.PosixFilePermissions.fromString("rw-rw-r--"),
                uid, gid));

        assertEquals(java.util.Set.of(
                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                        java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                        java.nio.file.attribute.PosixFilePermission.GROUP_WRITE,
                        java.nio.file.attribute.PosixFilePermission.OTHERS_READ),
                org.jackhuang.hmcl.util.PosixPermissions.of(file).orElseThrow(),
                "the agent's pnpm has to be able to read it, and `pnpm config set` to rewrite it");
        assertEquals(uid, ((Number) Files.getAttribute(file, "unix:uid")).intValue());
        assertEquals(gid, ((Number) Files.getAttribute(file, "unix:gid")).intValue());
    }

    /// The group id of this process, for the test above.
    ///
    /// @return the gid, or `-1` where the platform has no such notion
    private static int egid() {
        try {
            return (int) Files.getAttribute(Path.of("/proc/self"), "unix:gid");
        } catch (Exception e) {
            try {
                return (int) new com.sun.security.auth.module.UnixSystem().getGid();
            } catch (Exception | LinkageError ignored) {
                return -1;
            }
        }
    }

    /// The banner note exists for the two ways the UID split can be off, and
    /// they are different sentences. A machine that is neither says nothing —
    /// that is the case the note must not fire on, or operators learn to skip it.
    @Test
    void theBannerNoteSpeaksForBothWaysTheSplitCanBeOff() {
        String note = Privileges.bannerNote();
        if (Privileges.rootOutsideContainer()) {
            assertNotNull(note);
            assertTrue(note.contains("root"));
        } else if (Privileges.inContainer() && Privileges.euid() > 0) {
            // An image with `USER`, or a compose file with `user:`: everything
            // still starts, and every child shares the panel's uid.
            assertNotNull(note);
            assertTrue(note.contains("NOT active"), note);
        } else {
            assertNull(note);
        }
    }

    /// The "create it, then hand it over" pairing the three brands share. What a
    /// test can pin here is the part every caller relies on: the directory exists
    /// afterwards, nested parents included, and calling it again is a no-op rather
    /// than a failure — an instance is created once and launched many times.
    @Test
    void preparingAnAgentDirectoryCreatesItAndKeepsWorkingWhenCalledAgain(@TempDir Path directory)
            throws Exception {
        Path home = directory.resolve("instances/abc/home");
        Privileges.prepareAgentDirectory(home, Privileges.AGENT_SHARED_DIRECTORY);
        assertTrue(Files.isDirectory(home), "nested parents are created too: " + home);

        Path panel = home.resolve("panel");
        Privileges.prepareAgentDirectory(panel, Privileges.AGENT_PRIVATE_DIRECTORY);
        assertTrue(Files.isDirectory(panel));

        Path marker = Files.writeString(home.resolve("state.db"), "kept\n");
        Privileges.prepareAgentDirectory(home, Privileges.AGENT_SHARED_DIRECTORY);
        assertTrue(Files.isRegularFile(marker), "a second pass must not clear what is already there");

        // Inside the container the mode is the one asked for; outside nobody is
        // dropped, so nothing is changed (see handToAgent).
        if (Privileges.dropsChildren()) {
            assertEquals(java.util.Set.of(
                            java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                            java.nio.file.attribute.PosixFilePermission.OWNER_WRITE,
                            java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE,
                            java.nio.file.attribute.PosixFilePermission.GROUP_READ,
                            java.nio.file.attribute.PosixFilePermission.GROUP_WRITE,
                            java.nio.file.attribute.PosixFilePermission.GROUP_EXECUTE,
                            java.nio.file.attribute.PosixFilePermission.OTHERS_READ,
                            java.nio.file.attribute.PosixFilePermission.OTHERS_EXECUTE),
                    org.jackhuang.hmcl.util.PosixPermissions.of(home).orElseThrow(),
                    "0775: the agent writes inside it, the panel may too");
            assertEquals(Privileges.AGENT_UID,
                    ((Number) Files.getAttribute(home, "unix:uid")).intValue());
        }
    }

    /// A path that cannot be created is logged, not thrown: the caller is always a
    /// start-up path, and the tool that needs the directory reports the same thing
    /// better than an exception from here would.
    @Test
    void aDirectoryThatCannotBeCreatedDoesNotThrow(@TempDir Path directory) throws Exception {
        Path blocker = Files.writeString(directory.resolve("not-a-directory"), "x");
        Privileges.prepareAgentDirectory(blocker.resolve("below"), Privileges.AGENT_SHARED_DIRECTORY);
        assertTrue(Files.isRegularFile(blocker), "the blocker is untouched");
        assertFalse(Files.exists(blocker.resolve("below")), "and nothing was created under it");
    }

    /// Handing a whole tree over must not touch modes: such a tree holds executables
    /// (`.venv/bin/octop`), and a file mode applied to those takes the execute bit away. It
    /// must also skip symlinks — `chown` follows them, and a link inside an instance may point
    /// anywhere.
    @Test
    void handingATreeKeepsEveryModeAndFollowsNoLink(@TempDir Path directory) throws Exception {
        Path root = Files.createDirectories(directory.resolve("home"));
        Path script = Files.writeString(root.resolve("run.sh"), "#!/bin/sh\n");
        Files.setPosixFilePermissions(script, java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        Path data = Files.createDirectories(root.resolve("plugins"));
        Path outside = Files.writeString(directory.resolve("outside.txt"), "x");
        Path link = root.resolve("escape");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.io.IOException e) {
            link = null; // no symlinks on this filesystem: the rest still holds
        }
        var before = org.jackhuang.hmcl.util.PosixPermissions.of(script);

        Privileges.handTreeToAgent(root);

        assertEquals(before, org.jackhuang.hmcl.util.PosixPermissions.of(script),
                "a tree hand-over changes owners, never modes");
        assertTrue(Files.isDirectory(data));
        if (link != null) {
            assertTrue(Files.isSymbolicLink(link), "the link itself stays a link");
        }
        assertEquals(Files.exists(outside), true, "and the file it points at is untouched");
    }

    /// The file the panel just wrote has to change hands, together with the directory that holds
    /// it — the failure this repairs is `EACCES` on the file *beside* it: the harness composes
    /// `cordis.yml` next to the `cordis.patch.yml` the panel wrote, and it runs as the agent.
    @Test
    void aWrittenFileInsideTheDataRootGoesToTheAgent(@TempDir Path directory) throws Exception {
        Path dataRoot = Files.createDirectories(directory.resolve("data"));
        Path profile = Files.createDirectories(dataRoot.resolve("instances/abc/home/profiles/web"));
        Path patch = Files.writeString(profile.resolve("cordis.patch.yml"), "[]\n");
        int fileOwner = Privileges.ownerOf(patch);
        int directoryOwner = Privileges.ownerOf(profile);

        Privileges.handWrittenFile(patch, dataRoot);

        if (!Privileges.dropsChildren()) {
            // Outside the container nobody was dropped, so nothing may move: the panel and the
            // agent are the same person and an ownership change would be taking their file.
            assertEquals(fileOwner, Privileges.ownerOf(patch));
            assertEquals(directoryOwner, Privileges.ownerOf(profile));
            return;
        }
        assertEquals(Privileges.AGENT_UID, Privileges.ownerOf(patch),
                "the agent has to be able to rewrite the patch layer");
        assertEquals(Privileges.AGENT_UID, Privileges.ownerOf(profile),
                "and to write the composed profile beside it");
    }

    /// An instance may point its home at the operator's own directory (`DshHomeMode.CUSTOM`).
    /// The hand-over is therefore limited to the panel's data root, or the first launch would
    /// take ownership of someone's files.
    @Test
    void aWrittenFileOutsideTheDataRootIsLeftAlone(@TempDir Path directory) throws Exception {
        Path dataRoot = Files.createDirectories(directory.resolve("data"));
        Path operator = Files.createDirectories(directory.resolve("operator"));
        Path file = Files.writeString(operator.resolve("settings.yaml"), "provider: x\n");
        int owner = Privileges.ownerOf(file);
        int parentOwner = Privileges.ownerOf(operator);

        Privileges.handWrittenFile(file, dataRoot);

        assertEquals(owner, Privileges.ownerOf(file));
        assertEquals(parentOwner, Privileges.ownerOf(operator));
    }
}
