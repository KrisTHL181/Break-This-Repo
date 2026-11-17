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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Where skill packs can be fetched from: the GitHub repositories that publish them.
///
/// A skill is a directory with a {@code SKILL.md} in it, so a collection of skills is
/// already a git repository and needs no registry of its own. That is the whole reason
/// this source is GitHub and not a market: the ecosystem's skills are published as
/// repositories, GitHub already answers "which repositories are about agent skills" with
/// its topic search, and a repository already answers "what files is this skill made of"
/// with one tree listing.
///
/// Two calls reach a bundle and one more fetches it. The count matters because GitHub
/// answers an unauthenticated caller sixty times an hour, which is why nothing here is
/// called in a loop over a list.
@NotNullByDefault
public final class DshSkillSource {
    /// The topic the community publishes agent skills under.
    public static final String TOPIC = "agent-skills";

    private static final String API = "https://api.github.com";
    private static final String RAW = "https://raw.githubusercontent.com";
    private static final int OK = 200;

    private DshSkillSource() {
    }

    /// One repository that publishes skills.
    ///
    /// @param fullName    the {@code owner/name} GitHub addresses it by
    /// @param description what the repository says about itself, or an empty string
    /// @param stars       its star count, which is the only ordering the topic gives
    /// @param branch      its default branch, which is where its files are read from
    public record Repo(String fullName, String description, int stars, String branch) {
    }

    /// One skill pack inside a repository.
    ///
    /// @param repo the repository it belongs to
    /// @param path the directory holding the {@code SKILL.md}, or an empty string when
    ///             the repository is a single skill at its root
    /// @param name the directory's own name, which is what the pack is called
    public record Bundle(Repo repo, String path, String name) {
    }

    /// One skill as a catalogue lists it.
    ///
    /// The catalogues do not agree on what they publish, so this is their union: the
    /// things that always exist (which catalogue, and the repository and name that
    /// identify the skill) and the ones each fills in as it can. A catalogue that knows
    /// where in the repository the skill sits saves the launcher the two GitHub calls a
    /// fetch would otherwise spend finding out; one that does not is resolved then.
    ///
    /// @param catalog     which catalogue found it
    /// @param id          the catalogue's own identifier
    /// @param source      the repository, as {@code owner/name}
    /// @param name        the skill's directory in that repository
    /// @param description what the skill says it does, or an empty string
    /// @param ref         the branch or tag it was indexed at, or null when unknown
    /// @param path        the directory holding the skill, or null when unknown
    /// @param popularity  the count the catalogue publishes: installs, or stars
    public record Offering(String catalog, String id, String source, String name,
                           String description, @Nullable String ref, @Nullable String path,
                           long popularity) {
    }

    /// What a search across the catalogues found.
    ///
    /// @param skills   the skills, most popular first and without duplicates
    /// @param failures the catalogues that could not be read, by name
    public record Found(@Unmodifiable List<Offering> skills, @Unmodifiable List<String> failures) {
    }

    /// The shortest query the catalogues answer.
    private static final int SHORTEST_QUERY = 2;

    /// Searches every catalogue and merges what they answer.
    ///
    /// One catalogue failing is not a failed search: they are separate services with
    /// separate quotas and separate outages, and somebody looking for a skill is better
    /// served by the part of the answer that arrived than by an error. The names of the
    /// ones that did not answer come back with the result so the page can say so.
    ///
    /// A duplicate is the same repository and the same skill name. The first catalogue to
    /// name one keeps it, because the order is fixed and a skill listed twice is one
    /// skill; sorting by the count each publishes is what orders the merged list.
    ///
    /// @param query what to search for
    /// @param limit the greatest number of skills each catalogue is asked for
    /// @return the merged skills, and the catalogues that failed
    public static Found find(String query, int limit) {
        if (query == null || query.trim().length() < SHORTEST_QUERY) {
            return new Found(List.of(), List.of());
        }
        List<Offering> merged = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (Catalog catalog : CATALOGS) {
            List<Offering> found;
            try {
                found = catalog.search(query.trim(), limit);
            } catch (DshException | RuntimeException e) {
                LOG.warning(catalog.name() + " could not be searched", e);
                failures.add(catalog.name());
                continue;
            }
            for (Offering offering : found) {
                if (seen.add(offering.source() + "/" + offering.name())) {
                    merged.add(offering);
                }
            }
        }
        merged.sort(java.util.Comparator.comparingLong(Offering::popularity).reversed());
        LOG.info("The catalogues answered " + merged.size() + " skills for " + query);
        return new Found(List.copyOf(merged), List.copyOf(failures));
    }

    /// One catalogue of published skills.
    private interface Catalog {
        /// Returns the name this catalogue is shown and reported by.
        ///
        /// @return the name
        String name();

        /// Searches it.
        ///
        /// @param query what to search for
        /// @param limit the greatest number of skills to return
        /// @return the skills it lists
        /// @throws DshException when it cannot be read
        List<Offering> search(String query, int limit) throws DshException;
    }

    /// The catalogues searched, in the order their claims on a duplicate win.
    private static final List<Catalog> CATALOGS = List.of(new SkillsSh(), new SkillsMp());

    /// The community's own index, which counts installs.
    private static final class SkillsSh implements Catalog {
        /// Where it lives.
        private static final String BASE = "https://skills.sh";

        @Override
        public String name() {
            return "skills.sh";
        }

        @Override
        public List<Offering> search(String query, int limit) throws DshException {
            JsonObject root = get(BASE + "/api/search?q=" + encode(query) + "&limit=" + limit);
            JsonArray skills = root.getAsJsonArray("skills");
            if (skills == null) {
                throw new DshException(BASE + " answered with no list", null);
            }
            List<Offering> offerings = new ArrayList<>();
            for (JsonElement element : skills) {
                JsonObject object = element.getAsJsonObject();
                String id = string(object, "id");
                String source = string(object, "source");
                String name = orEmpty(string(object, "skillId"));
                if (id == null || source == null || name.isEmpty()) {
                    continue;
                }
                offerings.add(new Offering(name(), id, source, name, "", null, null,
                        integer(object, "installs")));
            }
            return offerings;
        }
    }

    /// The marketplace that indexes repositories and says where each skill sits.
    private static final class SkillsMp implements Catalog {
        /// Where it lives.
        private static final String BASE = "https://skillsmp.com";

        @Override
        public String name() {
            return "SkillsMP";
        }

        @Override
        public List<Offering> search(String query, int limit) throws DshException {
            JsonObject root = get(BASE + "/api/v1/skills/search?q=" + encode(query)
                    + "&limit=" + limit);
            JsonObject data = root.getAsJsonObject("data");
            JsonArray skills = data == null ? null : data.getAsJsonArray("skills");
            if (skills == null) {
                throw new DshException(BASE + " answered with no list", null);
            }
            List<Offering> offerings = new ArrayList<>();
            for (JsonElement element : skills) {
                JsonObject object = element.getAsJsonObject();
                GithubAddress where = parseGithubUrl(string(object, "githubUrl"));
                String id = string(object, "id");
                String name = orEmpty(string(object, "name"));
                if (id == null || name.isEmpty() || where == null) {
                    continue;
                }
                offerings.add(new Offering(name(), id, where.source(), name,
                        orEmpty(string(object, "description")), where.ref(), where.path(),
                        integer(object, "stars")));
            }
            return offerings;
        }
    }

    /// Where a GitHub tree address points.
    ///
    /// @param source the repository, as {@code owner/name}
    /// @param ref    the branch or tag
    /// @param path   the directory in it
    private record GithubAddress(String source, String ref, String path) {
    }

    /// Reads the repository, ref and path out of a github.com tree address.
    ///
    /// The marketplace publishes one, and it is worth more than it looks: it is the two
    /// GitHub calls a fetch would otherwise spend finding out the same thing, and those
    /// calls are limited to sixty an hour for an unauthenticated caller.
    ///
    /// @param url the address, or null
    /// @return where it points, or null when it is not a tree address
    private static @Nullable GithubAddress parseGithubUrl(@Nullable String url) {
        if (url == null || !url.startsWith("https://github.com/")) {
            return null;
        }
        String rest = url.substring("https://github.com/".length());
        int tree = rest.indexOf("/tree/");
        if (tree < 0) {
            return null;
        }
        String source = rest.substring(0, tree);
        String tail = rest.substring(tree + "/tree/".length());
        int slash = tail.indexOf('/');
        if (source.isEmpty() || slash <= 0 || slash == tail.length() - 1) {
            return null;
        }
        return new GithubAddress(source, tail.substring(0, slash), tail.substring(slash + 1));
    }

    /// Finds where in its repository a registry entry lives.
    ///
    /// The registry names the repository and the skill's directory but not the path to
    /// it, because a collection is free to file its skills wherever it likes. One tree
    /// listing settles it, asked at HEAD so that the default branch does not have to be
    /// looked up first.
    ///
    /// @param offering the entry to locate
    /// @return the pack
    /// @throws DshException when the repository cannot be listed, or holds no such skill
    public static Bundle resolve(Offering offering) throws DshException {
        // The default branch has to be the real name and not HEAD: the tree listing
        // accepts HEAD, but the archive host does not, and a bundle that carries a branch
        // nothing can download is a trap for whoever holds it next.
        JsonObject repo = get(API + "/repos/" + offering.source());
        String branch = string(repo, "default_branch");
        if (branch == null) {
            throw new DshException("GitHub did not say which branch " + offering.source() + " uses", null);
        }
        // A catalogue that already said where the skill sits needs no looking up.
        if (offering.ref() != null && offering.path() != null) {
            return new Bundle(new Repo(offering.source(), "", 0, offering.ref()),
                    offering.path(), offering.name());
        }
        List<Bundle> bundles = bundles(new Repo(offering.source(), "", 0, branch));
        for (Bundle bundle : bundles) {
            if (bundle.name().equals(offering.name())) {
                return bundle;
            }
        }
        for (Bundle bundle : bundles) {
            if (bundle.path().endsWith("/" + offering.name())) {
                return bundle;
            }
        }
        throw new DshException(offering.source() + " holds no skill called " + offering.name());
    }

    /// Searches the repositories published under the agent-skills topic.
    ///
    /// @param query extra words to narrow the search, or an empty string for the topic
    /// @param limit the greatest number of repositories to return
    /// @return the repositories, most-starred first
    /// @throws DshException when GitHub cannot be read
    public static @Unmodifiable List<Repo> search(String query, int limit) throws DshException {
        StringBuilder q = new StringBuilder("topic:").append(TOPIC);
        if (!query.isBlank()) {
            q.append(' ').append(query.trim());
        }
        String url = API + "/search/repositories?q=" + encode(q.toString())
                + "&sort=stars&order=desc&per_page=" + limit;
        JsonObject root = get(url);
        JsonArray items = root.getAsJsonArray("items");
        if (items == null) {
            throw new DshException("GitHub returned no repository list", null);
        }

        List<Repo> repos = new ArrayList<>();
        for (JsonElement item : items) {
            JsonObject object = item.getAsJsonObject();
            String fullName = string(object, "full_name");
            String branch = string(object, "default_branch");
            if (fullName == null || branch == null) {
                continue;
            }
            repos.add(new Repo(fullName, orEmpty(string(object, "description")),
                    integer(object, "stargazers_count"), branch));
        }
        LOG.info("Found " + repos.size() + " skill repositories for " + q);
        return List.copyOf(repos);
    }

    /// Lists the skill packs a repository holds.
    ///
    /// A pack is a directory with a {@code SKILL.md} in it, at any depth: a collection
    /// puts them under {@code skills/}, a repository that is one skill puts it at the
    /// root. Nothing is fetched to answer this — one tree listing carries every path.
    ///
    /// @param repo the repository
    /// @return the packs, ordered by name
    /// @throws DshException when the repository cannot be listed, or is too large for
    ///                       GitHub to list in one answer
    public static @Unmodifiable List<Bundle> bundles(Repo repo) throws DshException {
        JsonObject tree = tree(repo);
        List<Bundle> bundles = new ArrayList<>();
        for (JsonElement element : tree.getAsJsonArray("tree")) {
            JsonObject entry = element.getAsJsonObject();
            if (!"blob".equals(string(entry, "type"))) {
                continue;
            }
            String path = string(entry, "path");
            if (path == null || !(path.equals("SKILL.md") || path.endsWith("/SKILL.md"))) {
                continue;
            }
            String directory = path.equals("SKILL.md")
                    ? "" : path.substring(0, path.length() - "/SKILL.md".length());
            String name = directory.isEmpty()
                    ? repo.fullName().substring(repo.fullName().indexOf('/') + 1)
                    : directory.substring(directory.lastIndexOf('/') + 1);
            bundles.add(new Bundle(repo, directory, name));
        }
        bundles.sort(Comparator.comparing(Bundle::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(bundles);
    }

    /// Fetches one pack out of a repository and installs it into a home.
    ///
    /// The repository is downloaded as one zip and the pack is taken out of it, rather
    /// than fetched file by file. Three reasons, all of them measured: GitHub's raw file
    /// host is not reachable from every network while its archive host is; the archive
    /// is one request instead of one per file, which matters because an unauthenticated
    /// caller gets sixty API calls an hour and a pack can be a hundred files; and an
    /// archive carries bytes, so an icon or a font arrives intact.
    ///
    /// The pack is gathered into a temporary directory and handed to
    /// {@link DshSkills#install}, so a download that stops halfway leaves nothing in the
    /// instance, and the frontmatter and same-name rules stay the ones the local add
    /// already enforces — a pack is a pack however it arrived.
    ///
    /// @param home   the instance's DSH_HOME
    /// @param bundle the pack to fetch
    /// @param report told the name of each file as it is unpacked
    /// @return the installed skill
    /// @throws DshException when the pack cannot be fetched, or is not one
    public static DshSkill install(Path home, Bundle bundle, Consumer<String> report) throws DshException {
        Path staging = null;
        try {
            staging = Files.createTempDirectory("hdsl-skill-");
            Path pack = staging.resolve(bundle.name());
            unpack(bundle, pack, report);
            return DshSkills.install(home, pack);
        } catch (IOException e) {
            throw new DshException("Failed to fetch " + bundle.name() + " from "
                    + bundle.repo().fullName(), e);
        } finally {
            if (staging != null) {
                deleteTree(staging);
            }
        }
    }

    /// Saves one pack as an archive the user keeps.
    ///
    /// The same fetch as an install and a different ending: the files are packed back up
    /// under one directory named after the skill, so unpacking the archive into a skills
    /// directory is all it takes to have the skill, and a person who would rather keep it
    /// somewhere else, read it first, or hand it to another tool can. Nothing is written
    /// into an instance.
    ///
    /// @param bundle the pack to fetch
    /// @param target the file to write
    /// @param report told the name of each file as it is unpacked
    /// @throws DshException when the pack cannot be fetched or the archive written
    public static void download(Bundle bundle, Path target, Consumer<String> report) throws DshException {
        Path staging = null;
        try {
            staging = Files.createTempDirectory("hdsl-skill-");
            Path pack = staging.resolve(bundle.name());
            unpack(bundle, pack, report);
            archive(pack, bundle.name(), target);
        } catch (IOException e) {
            throw new DshException("Failed to save " + bundle.name() + " to " + target, e);
        } finally {
            if (staging != null) {
                deleteTree(staging);
            }
        }
    }

    /// Unpacks one pack out of its repository's archive.
    ///
    /// @param bundle the pack
    /// @param pack   where its files should land
    /// @param report told the name of each file
    /// @throws DshException when the archive cannot be read, or holds no such pack
    /// @throws IOException  when the archive cannot be downloaded or written out
    private static void unpack(Bundle bundle, Path pack, Consumer<String> report)
            throws DshException, IOException {
        Repo repo = bundle.repo();
        String prefix = bundle.path().isEmpty() ? "" : bundle.path() + "/";
        // A repository that is one skill at its root may hold whole packs under it too;
        // their files belong to them, not to the root pack.
        List<String> otherPacks = new ArrayList<>();
        if (prefix.isEmpty()) {
            for (Bundle other : bundles(repo)) {
                if (!other.path().isEmpty()) {
                    otherPacks.add(other.path() + "/");
                }
            }
        }

        Path archive = Files.createTempFile("hdsl-skill-", ".zip");
        try {
            report.accept(repo.fullName());
            Files.write(archive, getBytes("https://codeload.github.com/" + repo.fullName()
                    + "/zip/refs/heads/" + encode(repo.branch())));

            Files.createDirectories(pack);
            int files = 0;
            try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                    new java.io.BufferedInputStream(Files.newInputStream(archive)))) {
                for (java.util.zip.ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    // Every entry is under one directory named after the repository and
                    // the branch; what is wanted is the path inside it. What that
                    // directory is called is not worth predicting — a branch name may
                    // hold slashes, and GitHub spells those its own way.
                    int root = entry.getName().indexOf('/');
                    if (root < 0) {
                        continue;
                    }
                    String within = entry.getName().substring(root + 1);
                    if (!within.startsWith(prefix)) {
                        continue;
                    }
                    String relative = within.substring(prefix.length());
                    if (relative.isEmpty() || relative.contains("..")
                            || otherPacks.stream().anyMatch(relative::startsWith)) {
                        continue;
                    }
                    report.accept(relative);
                    Path target = pack.resolve(relative);
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    files++;
                }
            }
            if (files == 0 || !Files.isRegularFile(pack.resolve("SKILL.md"))) {
                throw new DshException("The pack " + bundle.name() + " in " + repo.fullName()
                        + " holds no SKILL.md");
            }
        } finally {
            deleteTree(archive);
        }
    }

    /// Writes a directory into an archive whose entries all sit under one name.
    ///
    /// @param pack   the directory
    /// @param name   the directory name every entry is prefixed with
    /// @param target the file to write
    /// @throws IOException when the archive cannot be written
    private static void archive(Path pack, String name, Path target) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(
                new java.io.BufferedOutputStream(Files.newOutputStream(target)))) {
            try (java.util.stream.Stream<Path> walk = Files.walk(pack)) {
                for (Path path : walk.sorted().toList()) {
                    if (Files.isDirectory(path)) {
                        continue;
                    }
                    zip.putNextEntry(new java.util.zip.ZipEntry(
                            name + "/" + pack.relativize(path).toString().replace('\\', '/')));
                    Files.copy(path, zip);
                    zip.closeEntry();
                }
            }
        }
    }

    /// Lists a repository's files.
    ///
    /// @param repo the repository
    /// @return GitHub's tree answer
    /// @throws DshException when it cannot be read, or was cut short
    private static JsonObject tree(Repo repo) throws DshException {
        JsonObject root = get(API + "/repos/" + repo.fullName() + "/git/trees/"
                + encode(repo.branch()) + "?recursive=1");
        JsonElement truncated = root.get("truncated");
        if (truncated != null && !truncated.isJsonNull() && truncated.getAsBoolean()) {
            throw new DshException(repo.fullName() + " has more files than GitHub lists at once");
        }
        if (root.getAsJsonArray("tree") == null) {
            throw new DshException("GitHub listed no files for " + repo.fullName(), null);
        }
        return root;
    }

    /// Reads one of GitHub's JSON answers.
    ///
    /// @param url the address
    /// @return the object it answered with
    /// @throws DshException when it cannot be read or is not a JSON object
    private static JsonObject get(String url) throws DshException {
        String body = new String(getBytes(url), StandardCharsets.UTF_8);
        try {
            JsonElement parsed = JsonParser.parseString(body);
            if (parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
            throw new DshException(url + " answered with something that is not a JSON object", null);
        } catch (RuntimeException e) {
            throw new DshException(url + " answered with something that is not JSON", e);
        }
    }

    /// Fetches an address.
    ///
    /// Bytes rather than text, because a pack's files are whatever the pack is made of:
    /// a SKILL.md and an icon take the same path here, and decoding one as text first
    /// would corrupt the other.
    ///
    /// @param url the address
    /// @return what it answered with
    /// @throws DshException when the request fails or is refused
    private static byte[] getBytes(String url) throws DshException {
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "HDSL")
                .header("Accept", "application/vnd.github+json")
                .timeout(java.time.Duration.ofSeconds(60))
                .GET()
                .build();
        try {
            java.net.http.HttpResponse<byte[]> response = CLIENT.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != OK) {
                throw new DshException("The server answered " + response.statusCode() + " for " + url, null);
            }
            return response.body();
        } catch (IOException e) {
            throw new DshException("Failed to read " + url, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DshException("Interrupted while reading " + url, e);
        }
    }

    /// The client every request goes through.
    private static final java.net.http.HttpClient CLIENT = java.net.http.HttpClient.newHttpClient();

    /// Encodes one query-string value.
    ///
    /// @param value the value
    /// @return it, percent-encoded
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /// Reads a string field.
    ///
    /// @param object the object
    /// @param key    the field
    /// @return its value, or null when it is absent or JSON null
    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    /// Reads an integer field.
    ///
    /// @param object the object
    /// @param key    the field
    /// @return its value, or zero when it is absent
    private static int integer(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    /// Replaces null with an empty string.
    ///
    /// @param value the value, or null
    /// @return the value, or an empty string
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /// Deletes a directory and everything under it.
    ///
    /// @param directory the directory
    private static void deleteTree(Path directory) {
        try (java.util.stream.Stream<Path> walk = Files.walk(directory)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            LOG.warning("Could not remove " + directory, e);
        }
    }
}
