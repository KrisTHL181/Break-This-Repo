/*
 * HDSL
 * Copyright (C) 2026  HDSL contributors
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
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the specification an installed-from-a-source plugin is written as.
///
/// This is the string the profile ends up carrying, so it is what decides whether a plugin installed
/// from a fork of a branch, or from a directory somebody is still editing, is still there on the next
/// boot — and whether it is the same plugin. The refusals matter as much as the readings: an address
/// this cannot name correctly has to say so, because the alternative is a package manager quietly
/// installing the default branch (or, for a bare `https://` address, a tarball) instead.
class DshPluginSourcesTest {
    @Test
    void aGitHubLinkBecomesTheSpecificationTheProfileWrites() throws Exception {
        assertEquals("github:dsh-market/dsh-market#main",
                DshPluginSources.specOf("https://github.com/dsh-market/dsh-market", "main"));
        assertEquals("github:dsh-market/dsh-market#180c3144da8eb4229cacb843aced229ea55912fc",
                DshPluginSources.specOf("https://github.com/dsh-market/dsh-market",
                        "180c3144da8eb4229cacb843aced229ea55912fc"),
                "a commit is a revision like any other, and is what pins an installation to one");
    }

    @Test
    void aLinkThatCarriesItsRevisionIsTakenAsItIs() throws Exception {
        // Which is how a link is copied out of a browser, so it has to work with the revision
        // field left alone.
        assertEquals("github:dsh-market/dsh-market#main",
                DshPluginSources.specOf("https://github.com/dsh-market/dsh-market#main", ""));
        assertEquals("github:dsh-market/dsh-market#main",
                DshPluginSources.specOf("https://github.com/dsh-market/dsh-market#main", null));
    }

    @Test
    void whatTheRevisionFieldSaysWinsOverTheLink() throws Exception {
        assertEquals("github:owner/name#v1.2.3", DshPluginSources.specOf("https://github.com/owner/name#main", "v1.2.3"),
                "somebody who fills the field in has just said which revision they mean");
    }

    @Test
    void aBranchNamedInALinkToItIsRead() throws Exception {
        assertEquals("github:owner/name#main", DshPluginSources.specOf("https://github.com/owner/name/tree/main", ""));
    }

    @Test
    void aRepositoryWithNoRevisionFollowsItsDefaultBranch() throws Exception {
        assertEquals("github:owner/name", DshPluginSources.specOf("https://github.com/owner/name", ""));
        assertEquals("github:owner/name", DshPluginSources.specOf("https://github.com/owner/name", "   "));
        assertEquals("github:owner/name", DshPluginSources.specOf("https://github.com/owner/name/", null));
        assertEquals("github:owner/name", DshPluginSources.specOf("https://github.com/owner/name.git", null));
        assertEquals("github:owner/name", DshPluginSources.specOf("github:owner/name", null),
                "the shorthand the marketplace writes is one of the addresses this takes");
        assertEquals("github:owner/name", DshPluginSources.specOf("  owner/name  ", null),
                "and so is the bare owner/name npm reads as GitHub");
    }

    @Test
    void aRepositoryOnAnotherHostIsInstalledAsGit() throws Exception {
        // The `git+` prefix is not decoration: npm reads a bare https:// address as a tarball.
        assertEquals("git+https://gitlab.com/owner/name#main",
                DshPluginSources.specOf("https://gitlab.com/owner/name", "main"));
        assertEquals("git+https://git.example.com/owner/name",
                DshPluginSources.specOf("git+https://git.example.com/owner/name", ""));
        assertEquals("git+ssh://git@example.com/owner/name.git#v1",
                DshPluginSources.specOf("ssh://git@example.com/owner/name.git", "v1"));
        assertEquals("git+https://notgithub.com/owner/name",
                DshPluginSources.specOf("https://notgithub.com/owner/name", ""),
                "a host whose name merely contains github.com is not GitHub");
    }

    @Test
    void theFormsPnpmNamesHostsWithAreHandedOverAsTheyStand() throws Exception {
        // The plugin manager reads all of these itself, so this has nothing to add to them.
        assertEquals("gitlab:owner/name#main", DshPluginSources.specOf("gitlab:owner/name", "main"));
        assertEquals("bitbucket:owner/name", DshPluginSources.specOf("bitbucket:owner/name", ""));
        assertEquals("gist:0123456789abcdef", DshPluginSources.specOf("gist:0123456789abcdef", ""));
        assertEquals("git@github.com:owner/name.git", DshPluginSources.specOf("git@github.com:owner/name.git", ""));
        assertEquals("https://example.com/thing-1.0.0.tgz",
                DshPluginSources.specOf("https://example.com/thing-1.0.0.tgz", ""),
                "a tarball over HTTP is a source of its own, not a repository to prefix");
    }

    @Test
    void aDirectoryIsInstalledFromThePathItIsAt() throws Exception {
        // The three ways of writing a local dependency, and the one pnpm is handed.
        assertEquals("file:/srv/plugins/thing", DshPluginSources.specOf("file:/srv/plugins/thing", ""));
        assertEquals("link:/srv/plugins/thing", DshPluginSources.specOf("link:/srv/plugins/thing", ""),
                "`link:` is the live one, and is kept as the prefix that says so");
        assertEquals("file:/srv/plugins/thing", DshPluginSources.specOf("/srv/plugins/thing", ""),
                "a bare path is installed as `file:`, which is what npm reads a path as");
        assertEquals("file:/srv/plugins/thing", DshPluginSources.specOf("file:///srv/plugins/thing", ""),
                "one directory has one spelling in the profile");
    }

    @Test
    void aLocalDirectoryTakesNoRevision() {
        assertThrows(DshException.class, () -> DshPluginSources.specOf("link:/srv/plugins/thing", "main"),
                "a directory is installed as it stands, so a revision beside it is a mistake worth saying");
        assertThrows(DshException.class, () -> DshPluginSources.specOf("file:/srv/plugins/thing", "v1.0.0"));
    }

    @Test
    void aLocalPathMustBeAbsolute() {
        assertThrows(DshException.class, () -> DshPluginSources.specOf("./thing", ""));
        assertThrows(DshException.class, () -> DshPluginSources.specOf("../thing", ""));
        assertThrows(DshException.class, () -> DshPluginSources.specOf("file:thing", ""));
        assertThrows(DshException.class, () -> DshPluginSources.specOf("link:plugins/thing", ""),
                "which is the rule the plugin manager applies too: the working directory means nothing"
                        + " to somebody typing into a dialog");
    }

    @Test
    void aPackedPluginIsPointedAtTheButtonThatInstallsOne() {
        assertThrows(DshException.class, () -> DshPluginSources.specOf("file:/tmp/thing-1.0.0.tgz", ""),
                "the file button copies an archive into the instance, which a path install would not");
        assertThrows(DshException.class, () -> DshPluginSources.specOf("/tmp/thing.tar.gz", ""));
    }

    @Test
    void whatCannotBeInstalledFromIsRefused() {
        assertThrows(DshException.class, () -> DshPluginSources.specOf(null, "main"));
        assertThrows(DshException.class, () -> DshPluginSources.specOf("", ""));
        assertThrows(DshException.class, () -> DshPluginSources.specOf("   ", ""));
        assertThrows(DshException.class, () -> DshPluginSources.specOf("not an address", ""));
        assertThrows(DshException.class, () -> DshPluginSources.specOf("ftp://example.com/owner/name", ""),
                "a repository is fetched over http, https, ssh or git");
        assertThrows(DshException.class, () -> DshPluginSources.specOf("https://github.com/owner", ""),
                "a link to an account is not a repository");
        assertThrows(DshException.class, () -> DshPluginSources.specOf("https://github.com/owner/name/tree/main/packages/thing", ""),
                "a link that points inside a repository names a directory, not a plugin to install");
        assertThrows(DshException.class, () -> DshPluginSources.specOf("#feat/thing", ""),
                "a revision with no repository in front of it names nothing");
    }

    @Test
    void aRevisionThatCannotBeWrittenIntoASpecificationIsRefused() {
        assertThrows(DshException.class, () -> DshPluginSources.specOf("https://github.com/owner/name", "feat thing"),
                "a revision with a space in it is a typo, not a branch");
        assertThrows(DshException.class, () -> DshPluginSources.specOf("https://github.com/owner/name", "feat#thing"),
                "the '#' is what separates the revision from the repository, so it cannot be inside one");
        assertThrows(DshException.class, () -> DshPluginSources.specOf("https://github.com/owner/name", "--depth=1"),
                "a leading hyphen is read as an option by the package manager, which would install something else");
    }

    @Test
    void aRevisionWrittenWithItsOwnHashIsRead() throws Exception {
        assertEquals("github:owner/name#main", DshPluginSources.specOf("https://github.com/owner/name", "#main"));
        assertEquals("github:owner/name#v1.0.0", DshPluginSources.specOf("https://github.com/owner/name", "  v1.0.0  "),
                "what was pasted with spaces around it is the revision that was meant");
    }

    @Test
    void whatTheRevisionFieldAcceptsIsTheRuleTheSpecificationIsBuiltBy() {
        // The revision is checked as it is typed rather than when the button is pressed, and its
        // empty value is the default branch — the one thing in the dialog that may be left alone.
        assertTrue(DshPluginSources.isUsableReference(null));
        assertTrue(DshPluginSources.isUsableReference(""));
        assertTrue(DshPluginSources.isUsableReference("   "));
        assertTrue(DshPluginSources.isUsableReference("#main"));
        assertTrue(DshPluginSources.isUsableReference("feat/claude-flavor-bundle"));
        assertTrue(DshPluginSources.isUsableReference("180c3144da8eb4229cacb843aced229ea55912fc"));

        assertFalse(DshPluginSources.isUsableReference("feat thing"));
        assertFalse(DshPluginSources.isUsableReference("feat#thing"));
        assertFalse(DshPluginSources.isUsableReference("--depth=1"));
    }

    @Test
    void aLocalAddressIsRecognisedAsOne() {
        assertTrue(DshPluginSources.isLocalPath("/srv/plugins/thing"));
        assertTrue(DshPluginSources.isLocalPath("file:/srv/plugins/thing"));
        assertTrue(DshPluginSources.isLocalPath("link:/srv/plugins/thing"));
        assertTrue(DshPluginSources.isLocalPath("./thing"));
        assertTrue(DshPluginSources.isLocalPath("../thing"));

        assertFalse(DshPluginSources.isLocalPath("https://github.com/owner/name"));
        assertFalse(DshPluginSources.isLocalPath("owner/name"));
        assertFalse(DshPluginSources.isLocalPath(""));
        assertFalse(DshPluginSources.isLocalPath(null));
    }
}
