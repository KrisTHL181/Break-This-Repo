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

import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Installs a plugin from the source a person names: a repository, or a directory on this machine.
///
/// A plugin published to a registry is installed by name and version, which is what the plugin
/// market does. One that lives only in a repository — a fork, a branch somebody is working on, a
/// package nobody ever published — is installed from the repository and a revision instead, and one
/// somebody is still writing is installed from the directory it is in. pnpm takes all of those as a
/// single specification, so the work here is turning what a person types into that specification,
/// and handing it to the installer that already exists ([DshPluginInstaller#installSpecs]).
///
/// **Repositories.** Four addresses are understood, and they are the ones pnpm understands:
///
/// - a GitHub link or shorthand — `owner/name`, `github:owner/name`, `https://github.com/owner/name`
///   — becomes `github:owner/name`, which is what the ecosystem writes into a profile;
/// - another host's link becomes `git+<address>`, and the prefix is not decoration: to npm a bare
///   `https://` address is a **tarball**, not a repository;
/// - the `gitlab:` / `bitbucket:` / `gist:` shorthands and the scp-like `git@host:owner/name` are
///   passed through, because they already say which host to fetch from;
/// - a tarball over HTTP is passed through as it stands, which is what it is.
///
/// The revision is optional and may be a branch, a tag or a commit — pnpm resolves all three — and
/// it may be written where a browser writes it, as `.../tree/<branch>` in the address or
/// `#<revision>` after it. What the revision field says wins over what the address says, on the
/// grounds that somebody who fills the field in has just said which revision they mean.
///
/// **Directories.** `file:<path>`, `link:<path>` and a bare absolute path are the three ways of
/// writing the same thing, and pnpm reads all three as a local dependency. The path must be
/// absolute, which is the same rule the plugin manager itself applies and for the same reason: the
/// working directory means nothing to somebody typing into a dialog, and a relative path resolved
/// against the profile would point inside it. `link:` is the one to use while working on a plugin —
/// it is a live link, so an edit lands on the next boot — and `file:` installs a snapshot of the
/// directory as it is now. What a directory is *not* is a revision: a local path is installed as it
/// stands, so a branch, tag or commit alongside it is refused rather than ignored.
@NotNullByDefault
public final class DshPluginSources {
    /// The shape of a GitHub `owner/name`.
    private static final Pattern REPOSITORY = Pattern.compile("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$");

    /// The shorthands pnpm resolves through a host of its own.
    private static final Pattern SHORTHAND = Pattern.compile("^(?:github|gitlab|bitbucket|gist):",
            Pattern.CASE_INSENSITIVE);

    /// The scp-like form, `git@host:owner/name`.
    private static final Pattern SCP = Pattern.compile("^[^@/\\s]+@[^:/\\s]+:.+$");

    /// A packed plugin, which is an archive rather than a directory.
    ///
    /// Shared with [DshPluginBundle], which has to tell the two apart for the same reason: an
    /// archive travels in a pack and a directory does not.
    static final Pattern ARCHIVE = Pattern.compile(".*\\.(?:tgz|tar\\.gz)$", Pattern.CASE_INSENSITIVE);

    /// The schemes a repository is fetched over.
    private static final List<String> SCHEMES = List.of("http", "https", "ssh", "git");

    private DshPluginSources() {
    }

    /// Installs the plugin an address and a revision name.
    ///
    /// @param instance  the instance to install into
    /// @param address   the repository, the tarball, or the directory the plugin is in
    /// @param reference the branch, tag or commit, or `null`/blank for the default branch
    /// @param onLine    receives everything the package manager prints, or `null`
    /// @return the specification that was installed, which is what the profile recorded
    /// @throws DshException when the address or the revision cannot be written as a
    ///                      specification, the directory holds no package, or the installation fails
    public static String install(DshInstance instance, @Nullable String address, @Nullable String reference,
                                 @Nullable Consumer<String> onLine) throws DshException {
        String spec = specOf(address, reference);
        requirePackage(spec);
        DshPluginInstaller.installSpecs(instance, List.of(spec), onLine);
        LOG.info("Installed " + spec + " into " + instance.id());
        return spec;
    }

    /// Builds the specification `dsh plugin add` is handed.
    ///
    /// @param address   the repository, the tarball, or the directory the plugin is in
    /// @param reference the branch, tag or commit, or `null`/blank for the default branch
    /// @return the specification, for example `github:owner/name#feat/thing` or `link:/src/thing`
    /// @throws DshException when the address or the revision cannot be written as one
    static String specOf(@Nullable String address, @Nullable String reference) throws DshException {
        String text = address == null ? "" : address.trim();
        if (text.isEmpty()) {
            throw new DshException("No address was given for the plugin");
        }

        if (isLocalPath(text)) {
            String wanted = referenceOf(reference);
            if (!wanted.isEmpty()) {
                throw new DshException("A local directory is installed as it stands, so it takes no"
                        + " branch, tag or commit: " + wanted);
            }
            return localSpec(text);
        }

        String base = sourceOf(text);

        String revision = referenceOf(reference);
        if (revision.isEmpty()) {
            // Nothing was filled in, so whatever the address itself names stands: a link to a
            // branch is a revision somebody chose, and taking the default branch instead would
            // install a different commit than the one the link shows.
            revision = referenceOf(revisionInLink(text));
        }
        return revision.isEmpty() ? base : base + "#" + revision;
    }

    /// Reports whether an address names something on this machine rather than something to fetch.
    ///
    /// The two prefixes pnpm reads are `file:` and `link:`, and a bare absolute path is the third way
    /// of writing the same thing. A path written the way a shell writes one — `./thing`, `../thing` —
    /// counts as well, so that it is refused for not being absolute rather than mistaken for npm's
    /// `owner/name` shorthand. Nothing about the path itself is decided here: whether it exists, and
    /// whether it holds a package, is [#requirePackage]'s business.
    ///
    /// @param address the address, as it was typed
    /// @return whether it is a local path
    static boolean isLocalPath(@Nullable String address) {
        String text = address == null ? "" : address.trim();
        return text.startsWith("file:")
                || text.startsWith("link:")
                || text.startsWith("/")
                || text.startsWith("./")
                || text.startsWith("../");
    }

    /// Reports whether a revision can be written into a specification.
    ///
    /// @param reference the revision, as it was typed
    /// @return whether it is one the specification can carry
    public static boolean isUsableReference(@Nullable String reference) {
        try {
            referenceOf(reference);
            return true;
        } catch (DshException e) {
            return false;
        }
    }

    /// Returns the source as the part of a specification pnpm takes, without any revision.
    ///
    /// @param address the address, as it was typed
    /// @return the source, for example `github:owner/name` or `git+https://host/name`
    /// @throws DshException when the address names no source this can install from
    static String sourceOf(String address) throws DshException {
        String text = withoutRevision(address);
        if (text.isEmpty()) {
            throw new DshException("The address names nothing to install from");
        }

        if (isGitHubLink(text)) {
            String subpath = DshPluginCatalog.subpathSuffix(text);
            if (!subpath.isEmpty()) {
                // A link to a directory inside a repository is not a repository, and taking the
                // repository root instead would install something other than what the link names.
                throw new DshException("That address points inside a repository ("
                        + subpath.substring("#path:/".length()) + "); give the repository's own address");
            }
            return githubSpec(DshPluginCatalog.repoOf(text));
        }
        if (text.toLowerCase(Locale.ROOT).startsWith("github:")) {
            return githubSpec(text.substring("github:".length()));
        }
        if (SHORTHAND.matcher(text).find()) {
            // `gitlab:`, `bitbucket:` and `gist:` name their host themselves, and this launcher has
            // nothing to check their owner/name against, so they are handed over as they stand.
            return text;
        }
        if (text.regionMatches(true, 0, "git+", 0, "git+".length())) {
            // Already a specification: the prefix says which of npm's git forms this is, and what
            // follows it is read as the address it names.
            return fetchSpec(text.substring("git+".length()));
        }
        if (SCP.matcher(text).matches()) {
            return text;
        }
        if (REPOSITORY.matcher(text).matches()) {
            // `owner/name` on its own is npm's GitHub shorthand, and the marketplace writes it
            // that way too, so it is read as one rather than refused.
            return githubSpec(text);
        }
        return fetchSpec(text);
    }

    /// Reads the revision field, refusing what cannot be written into a specification.
    ///
    /// @param reference the revision, as it was typed
    /// @return the revision, or an empty string when none was given
    /// @throws DshException when it is written in a way a specification cannot carry
    static String referenceOf(@Nullable String reference) throws DshException {
        String text = reference == null ? "" : reference.trim();
        if (text.startsWith("#")) {
            text = text.substring(1).trim();
        }
        if (text.isEmpty()) {
            return "";
        }

        if (text.startsWith("-")) {
            // A package manager reads a leading hyphen as an option, so this would install
            // something else entirely rather than fail.
            throw new DshException("A revision cannot begin with a hyphen: " + text);
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                throw new DshException("A revision cannot contain a space: " + text);
            }
            if (c == '#') {
                throw new DshException("A revision cannot contain '#', which is what separates it"
                        + " from the repository: " + text);
            }
        }
        return text;
    }

    /// Returns the revision an address names, or an empty string when it names none.
    ///
    /// @param address the address
    /// @return the revision
    static String revisionInLink(String address) {
        String text = address.trim();

        int hash = text.indexOf('#');
        if (hash >= 0) {
            String fragment = text.substring(hash + 1).trim();
            // `#path:/packages/thing` names a directory rather than a revision.
            return fragment.startsWith("path:") ? "" : fragment;
        }

        int tree = text.indexOf("/tree/");
        if (tree < 0) {
            return "";
        }
        String rest = text.substring(tree + "/tree/".length()).trim();
        while (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        // A path after the branch names a directory inside the repository, which is refused
        // when the address is read, so nothing is taken from it here.
        return rest.contains("/") ? "" : rest;
    }

    /// Returns an address without the revision written after it.
    ///
    /// @param address the address
    /// @return the address
    private static String withoutRevision(String address) {
        String text = address.trim();
        int hash = text.indexOf('#');
        return (hash < 0 ? text : text.substring(0, hash)).trim();
    }

    /// Builds the specification for a local directory.
    ///
    /// @param address the address, which begins with `file:`, `link:` or `/`
    /// @return the specification
    /// @throws DshException when the path is not absolute, or is an archive
    private static String localSpec(String address) throws DshException {
        boolean linked = address.startsWith("link:");
        // `file:///srv/thing` is the same directory as `file:/srv/thing`, and the profile is written
        // with one spelling of it rather than with whatever was pasted.
        String path = address.replaceFirst("^(?:file|link):", "").trim().replaceFirst("^/+", "/");

        if (!path.startsWith("/")) {
            // The same rule the plugin manager applies: the working directory means nothing to
            // somebody typing into a dialog, and what the profile records has to keep pointing
            // somewhere after the dialog is gone.
            throw new DshException("A local path must be absolute, and " + path + " is not");
        }
        if (ARCHIVE.matcher(path).matches()) {
            throw new DshException("A packed plugin is installed with the file button, which copies it"
                    + " into the instance; this installs a directory: " + path);
        }
        return (linked ? "link:" : "file:") + path;
    }

    /// Refuses a local path that holds no package.
    ///
    /// @param spec the specification
    /// @throws DshException when the directory is not a package
    private static void requirePackage(String spec) throws DshException {
        if (!spec.startsWith("file:") && !spec.startsWith("link:")) {
            return;
        }
        String path = spec.replaceFirst("^(?:file|link):", "");
        Path directory = Path.of(path);
        if (!Files.isDirectory(directory)) {
            throw new DshException("There is no directory at " + path);
        }
        if (!Files.isRegularFile(directory.resolve("package.json"))) {
            throw new DshException("There is no package.json in " + path + ", so it is not a plugin");
        }
    }

    /// Builds the specification for something fetched over a network.
    ///
    /// @param address the address
    /// @return the specification
    /// @throws DshException when the address names nothing this can fetch
    private static String fetchSpec(String address) throws DshException {
        String text = address.trim();
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }

        if (ARCHIVE.matcher(text).matches() && text.toLowerCase(Locale.ROOT).startsWith("http")) {
            // A tarball is a source of its own, and one that is already a specification: it is
            // handed to the package manager exactly as it was typed.
            return text;
        }
        if (!text.contains("://")) {
            // The `git@host:owner/name` form is handled before this, and a line with no scheme at
            // all is a typo rather than a repository.
            throw new DshException(text + " is not an address this installs from: give a repository,"
                    + " an https:// tarball, or the absolute path of a directory");
        }
        String scheme = text.substring(0, text.indexOf("://")).toLowerCase(Locale.ROOT);
        if (!SCHEMES.contains(scheme)) {
            throw new DshException("A repository is fetched over " + String.join(", ", SCHEMES)
                    + ", and " + text + " is not one of them");
        }
        return "git+" + text;
    }

    /// Builds the specification for a GitHub repository.
    ///
    /// @param repository the `owner/name`
    /// @return the specification
    /// @throws DshException when the text is not an `owner/name`
    private static String githubSpec(String repository) throws DshException {
        String name = repository.trim();
        while (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - ".git".length());
        }
        if (!REPOSITORY.matcher(name).matches()) {
            throw new DshException(name + " is not the owner/name of a GitHub repository");
        }
        return "github:" + name;
    }

    /// Reports whether an address is a link to GitHub itself.
    ///
    /// The host is read rather than searched for in the text: `https://notgithub.com/owner/name`
    /// contains `github.com/` and is not GitHub, and taking it as GitHub would install from the
    /// wrong host.
    ///
    /// @param address the address
    /// @return whether it is a GitHub link
    private static boolean isGitHubLink(String address) {
        if (!address.contains("://")) {
            return false;
        }
        try {
            String host = java.net.URI.create(address).getHost();
            return host != null
                    && (host.equalsIgnoreCase("github.com") || host.equalsIgnoreCase("www.github.com"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
