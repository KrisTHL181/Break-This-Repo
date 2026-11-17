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
import org.jetbrains.annotations.Unmodifiable;
import org.jackhuang.hmcl.util.platform.Privileges;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/// Reads and edits the skill packs in a DSH home.
///
/// The harness's own provider scans {@code <dshHome>/skills} one level deep:
/// {@code <name>/SKILL.md} is a directory bundle and {@code <name>.md} is a flat skill,
/// while a nested {@code SKILL.md} and every other kind of entry are not skills at all.
/// Everything here works on that same one level, so the list the launcher shows is the
/// list the harness will offer.
///
/// A skill is switched off for the agent with the harness's own frontmatter key,
/// {@code disable-model-invocation: true}. Switching off writes that one line into the
/// frontmatter and switching on takes it away again; every other line of the file the
/// author wrote comes back out exactly as it went in.
@NotNullByDefault
public final class DshSkills {
    /// The directory under a DSH home the harness reads skills from.
    private static final String DIRECTORY = "skills";

    /// The file a directory bundle describes itself in.
    private static final String INSTRUCTION = "SKILL.md";

    /// The frontmatter key that hides a skill from the agent.
    private static final String DISABLE = "disable-model-invocation";

    /// The line that opens and closes a frontmatter block.
    private static final String DELIMITER = "---";

    /// The suffix a rewrite is staged under before it replaces the file.
    private static final String STAGING = ".hdsl-skills";
    /// The directory the harness keeps its own shipped skills in.
    ///
    /// It is a container one level above the packs rather than a pack, so it is not a skill
    /// itself — but a pack must never carry it: those are the harness own copies, and
    /// installing one machine over another is not a thing anybody asked for.
    private static final String SYSTEM = ".system";

    /// The prefix a pack writes its skill entries under.
    ///
    /// The layout beneath it is the home own: a directory bundle travels as its name and
    /// then its files, a flat skill as its one .md, so restoring a pack is a copy rather
    /// than a translation.
    public static final String PREFIX = DIRECTORY + "/";

    private DshSkills() {
    }

    /// Returns the directory an instance's skills live in.
    ///
    /// @param home the instance's DSH_HOME
    /// @return the directory, which may not exist
    public static Path directory(Path home) {
        return home.resolve(DIRECTORY);
    }
    /// Returns the skill packs a pack should carry.
    ///
    /// The harness own `.system` container is never one of them, whatever is asked for: it holds
    /// the skills the harness ships rather than the person s, and carrying it would install one
    /// machine s copies over another s.
    ///
    /// @param home   the instance s DSH_HOME
    /// @param wanted the names to carry, matched against both the pack s own name and the name
    ///               it has on disk
    /// @return the packs, in the order [#list] returns them
    /// @throws DshException when the skills cannot be read
    public static @Unmodifiable List<DshSkill> packable(Path home, Set<String> wanted) throws DshException {
        if (wanted.isEmpty()) {
            return List.of();
        }
        List<DshSkill> chosen = new ArrayList<>();
        for (DshSkill skill : list(home)) {
            if (SYSTEM.equals(skill.fileName())) {
                continue;
            }
            if (wanted.contains(skill.name()) || wanted.contains(skill.fileName())) {
                chosen.add(skill);
            }
        }
        return List.copyOf(chosen);
    }

    /// Returns the on-disk name of every skill a pack could carry.
    ///
    /// @param home the instance s DSH_HOME
    /// @return the names, ordered as [#list] orders them
    /// @throws DshException when the skills cannot be read
    public static @Unmodifiable List<String> packableNames(Path home) throws DshException {
        List<String> names = new ArrayList<>();
        for (DshSkill skill : list(home)) {
            if (!SYSTEM.equals(skill.fileName())) {
                names.add(skill.fileName());
            }
        }
        return List.copyOf(names);
    }

    /// Writes skill packs into an archive that is already open.
    ///
    /// A directory bundle travels as every file under it; a flat skill travels as its one `.md`.
    /// Members carry [#PREFIX], which is what tells a pack s skills from its sessions.
    ///
    /// @param zip     the archive
    /// @param skills  the packs to write
    /// @param onStage receives progress lines, or `null`
    /// @return how many files were written
    /// @throws DshException when a pack cannot be read
    /// @throws IOException  when the archive cannot be written
    public static int writeInto(ZipOutputStream zip, List<DshSkill> skills,
                                @Nullable Consumer<String> onStage) throws DshException, IOException {
        int files = 0;
        for (DshSkill skill : skills) {
            if (!skill.bundle()) {
                copyInto(zip, skill.instructions(), PREFIX + skill.fileName());
                files++;
                continue;
            }
            List<Path> tree;
            try (Stream<Path> walk = Files.walk(skill.entry())) {
                tree = walk.filter(Files::isRegularFile).toList();
            } catch (IOException e) {
                throw new DshException("Failed to read " + skill.entry(), e);
            }
            for (Path file : tree) {
                String relative = skill.entry().relativize(file).toString()
                        .replace(java.io.File.separatorChar, '/');
                copyInto(zip, file, PREFIX + skill.fileName() + "/" + relative);
                files++;
            }
        }
        if (!skills.isEmpty()) {
            report(onStage, "Wrote " + skills.size() + " skill pack(s), " + files + " file(s)");
        }
        return files;
    }

    /// Copies a pack s skill entries into a home.
    ///
    /// A skill the home already has is replaced: the pack is the thing being installed, and a
    /// half-old, half-new pack is worse than either. The harness own `.system` container is
    /// neither written nor read, and an entry that would land above the skills directory is
    /// dropped rather than sanitised — the rule the archive import already uses.
    ///
    /// @param archive the archive
    /// @param home    the DSH_HOME to read into
    /// @param onStage receives progress lines, or `null`
    /// @return how many files were written
    /// @throws DshException when the archive cannot be read
    public static int restoreInto(Path archive, Path home, @Nullable Consumer<String> onStage)
            throws DshException {
        Path directory = directory(home);
        int files = 0;
        Set<String> packs = new TreeSet<>();
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                if (!name.startsWith(PREFIX)) {
                    continue;
                }
                String relative = name.substring(PREFIX.length());
                if (relative.isEmpty() || relative.equals(SYSTEM) || relative.startsWith(SYSTEM + "/")) {
                    continue;
                }
                if (relative.startsWith("/") || relative.contains("..")) {
                    // An archive is something somebody handed over; one that tries to write above
                    // where it was unpacked is not one to be clever about.
                    continue;
                }
                Path target = directory.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                packs.add(relative.contains("/") ? relative.substring(0, relative.indexOf('/')) : relative);
                files++;
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + archive, e);
        }
        if (files > 0) {
            report(onStage, "Restored " + packs.size() + " skill pack(s), " + files + " file(s)");
        }
        return files;
    }

    /// Writes one file into an open archive under a member name.
    ///
    /// @param zip  the archive
    /// @param file the file
    /// @param name the member name
    /// @throws IOException when the archive cannot be written
    private static void copyInto(ZipOutputStream zip, Path file, String name) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        Files.copy(file, zip);
        zip.closeEntry();
    }

    /// Reports a line of progress to whoever is watching.
    ///
    /// @param onStage the sink, or `null`
    /// @param line    the line
    private static void report(@Nullable Consumer<String> onStage, String line) {
        if (onStage != null) {
            onStage.accept(line);
        }
    }


    /// Lists the skill packs in a home, ordered by name.
    ///
    /// A missing directory is an empty list rather than an error: an instance that has
    /// never had a skill installed is the ordinary state, not a failure.
    ///
    /// @param home the instance's DSH_HOME
    /// @return the packs, each carrying its own frontmatter
    /// @throws DshException when the directory or one of the packs cannot be read
    public static @Unmodifiable List<DshSkill> list(Path home) throws DshException {
        Path directory = directory(home);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<DshSkill> skills = new ArrayList<>();
        try (Stream<Path> entries = Files.list(directory)) {
            for (Path entry : entries.toList()) {
                DshSkill skill = read(entry);
                if (skill != null) {
                    skills.add(skill);
                }
            }
        } catch (IOException e) {
            throw new DshException("Failed to read " + directory, e);
        }
        skills.sort(Comparator.comparing(DshSkill::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(skills);
    }

    /// Reads one entry as a skill, or reports that it is not one.
    ///
    /// @param entry the entry directly under the skills directory
    /// @return the skill, or {@code null} when the entry is not a skill pack
    /// @throws DshException when the entry is a pack but cannot be read
    private static @Nullable DshSkill read(Path entry) throws DshException {
        String fileName = entry.getFileName().toString();
        // The harness skips the user root's {@code .system} child; anything else hidden is
        // not a skill either, and offering it would offer a row the harness ignores.
        if (fileName.startsWith(".")) {
            return null;
        }

        boolean bundle = Files.isDirectory(entry);
        Path instructions;
        if (bundle) {
            instructions = entry.resolve(INSTRUCTION);
            if (!Files.isRegularFile(instructions)) {
                return null;
            }
        } else if (Files.isRegularFile(entry) && fileName.endsWith(".md")) {
            instructions = entry;
        } else {
            return null;
        }

        Map<String, String> frontmatter = frontmatter(readLines(instructions));
        String name = frontmatter.get("name");
        if (name == null || name.isBlank()) {
            name = bundle ? fileName : fileName.substring(0, fileName.length() - ".md".length());
        }
        String description = collapse(frontmatter.getOrDefault("description", ""));
        boolean disabled = "true".equalsIgnoreCase(frontmatter.getOrDefault(DISABLE, "").trim());

        long size;
        long modified;
        try {
            size = bundle ? sizeOf(entry) : Files.size(instructions);
            modified = Files.getLastModifiedTime(instructions).toMillis();
        } catch (IOException e) {
            throw new DshException("Failed to measure " + entry, e);
        }

        return new DshSkill(entry, instructions, name, description, bundle, !disabled, size, modified);
    }

    /// Switches a skill off for the agent, or back on.
    ///
    /// Switching off writes {@code disable-model-invocation: true} into the frontmatter,
    /// adding the line when the author did not write one. Switching on takes the line
    /// away, which is the absence that means enabled. Nothing else in the file is touched.
    ///
    /// @param skill   the skill to switch
    /// @param enabled whether the agent should be able to invoke it
    /// @throws DshException when the file cannot be read or written, or has no frontmatter
    public static void setEnabled(DshSkill skill, boolean enabled) throws DshException {
        Path file = skill.instructions();
        List<String> lines = readLines(file);

        int[] block = frontmatterRange(lines);
        if (block == null) {
            throw new DshException(file + " has no frontmatter, so " + skill.name()
                    + " cannot be switched");
        }

        int existing = -1;
        for (int i = block[0] + 1; i < block[1]; i++) {
            if (DISABLE.equals(keyOf(lines.get(i)))) {
                existing = i;
                break;
            }
        }

        if (enabled) {
            if (existing < 0) {
                return;
            }
            lines.remove(existing);
        } else {
            if (existing >= 0) {
                lines.set(existing, DISABLE + ": true");
            } else {
                lines.add(block[1], DISABLE + ": true");
            }
        }
        write(file, lines);
    }

    /// Removes a skill pack from the home.
    ///
    /// @param skill the skill to remove
    /// @throws DshException when the entry cannot be deleted
    public static void remove(DshSkill skill) throws DshException {
        try {
            if (skill.bundle()) {
                deleteTree(skill.entry());
            } else {
                Files.deleteIfExists(skill.entry());
            }
        } catch (IOException e) {
            throw new DshException("Failed to remove " + skill.entry(), e);
        }
    }

    /// Copies a skill pack the user picked into the home.
    ///
    /// The pack is validated before it is copied: the harness requires a {@code name} and a
    /// {@code description} in the frontmatter and silently ignores a pack without them, so
    /// installing one would look like nothing happened. A name already taken is refused
    /// rather than overwritten — the pack that is there is the user's.
    ///
    /// @param home   the instance's DSH_HOME
    /// @param source the directory bundle or {@code .md} file to copy
    /// @return the installed skill
    /// @throws DshException when the source is not a pack, has no name or description, or
    ///                       cannot be copied
    public static DshSkill install(Path home, Path source) throws DshException {
        // An archive is what the market hands out, so what it hands out has to be
        // something this can take back: unpacked first, then treated as the directory
        // it contains.
        if (Files.isRegularFile(source) && source.getFileName().toString().endsWith(".zip")) {
            return installArchive(home, source);
        }
        Path fileName = source.getFileName();
        boolean bundle = Files.isDirectory(source);
        Path instructions;
        if (bundle) {
            instructions = source.resolve(INSTRUCTION);
            if (!Files.isRegularFile(instructions)) {
                throw new DshException(source + " is not a skill pack: it holds no " + INSTRUCTION);
            }
        } else if (Files.isRegularFile(source) && fileName.toString().endsWith(".md")) {
            instructions = source;
        } else {
            throw new DshException(source + " is not a skill pack: pick a " + INSTRUCTION
                    + " folder or a .md file");
        }

        Map<String, String> frontmatter = frontmatter(readLines(instructions));
        String name = frontmatter.get("name");
        if (name == null || name.isBlank()) {
            throw new DshException(instructions + " names no skill: its frontmatter has no name");
        }
        String description = frontmatter.get("description");
        if (description == null || description.isBlank()) {
            throw new DshException(instructions + " describes no skill: it has no description");
        }

        Path directory = directory(home);
        Path target = directory.resolve(fileName.toString());
        if (Files.exists(target)) {
            throw new DshException("A skill pack called " + fileName + " is already installed");
        }
        try {
            Files.createDirectories(directory);
            // `home/skills` is the agent's: the harness renames, disables and deletes what lives
            // there. The copy below runs as the panel (root inside the container), so the directory
            // and everything copied into it change hands right after — a root-owned pack is one the
            // harness can read but never maintain.
            Privileges.prepareAgentDirectory(directory, Privileges.AGENT_SHARED_DIRECTORY);
            if (bundle) {
                copyTree(source, target);
            } else {
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (IOException e) {
            throw new DshException("Failed to install " + source, e);
        }
        Privileges.handTreeToAgent(target);
        if (!bundle) {
            Privileges.handWrittenFile(target, DshPaths.ROOT);
        }

        DshSkill installed = read(target);
        if (installed == null) {
            throw new DshException("The skill pack " + target + " was copied but is not readable");
        }
        return installed;
    }

    /// Unpacks an archive and installs the pack inside it.
    ///
    /// What the market downloads is an archive, so an archive has to be something this
    /// can take back: the files are unpacked into a temporary directory and then treated
    /// as the pack directory they are, which keeps one set of rules for every way a pack
    /// can arrive.
    ///
    /// @param home   the instance's DSH_HOME
    /// @param source the archive
    /// @return the installed skill
    /// @throws DshException when the archive cannot be read or holds no pack
    private static DshSkill installArchive(Path home, Path source) throws DshException {
        Path staging;
        try {
            staging = Files.createTempDirectory("hdsl-skill-zip-");
        } catch (IOException e) {
            throw new DshException("Failed to make somewhere to unpack " + source, e);
        }
        try {
            unzip(source, staging);
            Path pack = packIn(staging);
            if (pack == null) {
                throw new DshException(source + " holds no skill pack: no SKILL.md in it");
            }
            return install(home, pack);
        } catch (IOException e) {
            throw new DshException("Failed to unpack " + source, e);
        } finally {
            try {
                deleteTree(staging);
            } catch (IOException ignored) {
                // A temporary directory that cannot be removed is untidy, not a failure.
            }
        }
    }

    /// Writes a zip's entries under a directory.
    ///
    /// Entries that would land outside it are dropped rather than sanitised: an archive
    /// is something the person downloaded, and one that tries to write above where it was
    /// unpacked is not one to be clever about.
    ///
    /// @param source the archive
    /// @param target the directory to unpack into
    /// @throws IOException when it cannot be read or written
    private static void unzip(Path source, Path target) throws IOException {
        try (java.util.zip.ZipInputStream zip = new java.util.zip.ZipInputStream(
                new java.io.BufferedInputStream(Files.newInputStream(source)))) {
            for (java.util.zip.ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName().replace('\\', '/');
                if (name.isEmpty() || name.startsWith("/") || name.contains("..")) {
                    continue;
                }
                Path file = target.resolve(name);
                Files.createDirectories(file.getParent());
                Files.copy(zip, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /// Returns the shallowest pack in a tree.
    ///
    /// An archive of one skill holds one SKILL.md; an archive of a whole collection could
    /// hold several. The shallowest one is the pack that was asked for, because the
    /// others are inside it.
    ///
    /// @param directory the unpacked tree
    /// @return the directory holding the SKILL.md, or null when there is none
    /// @throws IOException when the tree cannot be walked
    private static @Nullable Path packIn(Path directory) throws IOException {
        Path found = null;
        int depth = Integer.MAX_VALUE;
        try (Stream<Path> walk = Files.walk(directory)) {
            for (Path path : walk.toList()) {
                if (!Files.isRegularFile(path) || !INSTRUCTION.equals(path.getFileName().toString())) {
                    continue;
                }
                int at = directory.relativize(path).getNameCount();
                if (at < depth) {
                    depth = at;
                    found = path.getParent();
                }
            }
        }
        return found;
    }

    // ---- frontmatter ---------------------------------------------------------------------------

    /// Reads the frontmatter of an instruction file.
    ///
    /// Only the subset the harness itself reads is understood: scalar keys, and the folded
    /// and literal block scalars a long description is written as.
    ///
    /// @param lines the file's lines
    /// @return the keys and their values
    private static Map<String, String> frontmatter(List<String> lines) {
        int[] block = frontmatterRange(lines);
        if (block == null) {
            return Map.of();
        }

        Map<String, String> values = new LinkedHashMap<>();
        for (int i = block[0] + 1; i < block[1]; i++) {
            String line = lines.get(i);
            if (line.isBlank() || Character.isWhitespace(line.charAt(0))
                    || line.stripLeading().startsWith("#")) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (value.isEmpty() || value.startsWith(">") || value.startsWith("|")) {
                String separator = value.startsWith("|") ? "\n" : " ";
                StringBuilder folded = new StringBuilder();
                while (i + 1 < block[1] && !lines.get(i + 1).isBlank()
                        && Character.isWhitespace(lines.get(i + 1).charAt(0))) {
                    if (folded.length() > 0) {
                        folded.append(separator);
                    }
                    folded.append(lines.get(++i).trim());
                }
                values.put(key, folded.toString());
            } else {
                values.put(key, unquote(value));
            }
        }
        return Map.copyOf(values);
    }

    /// Returns where the frontmatter block is, when there is one.
    ///
    /// @param lines the file's lines
    /// @return the opening line's index and the closing line's, or {@code null}
    private static int @Nullable [] frontmatterRange(List<String> lines) {
        int open = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                open = i;
                break;
            }
        }
        if (open < 0 || !DELIMITER.equals(lines.get(open).trim())) {
            return null;
        }
        for (int i = open + 1; i < lines.size(); i++) {
            if (DELIMITER.equals(lines.get(i).trim())) {
                return new int[]{open, i};
            }
        }
        return null;
    }

    /// Returns the key a frontmatter line declares, or an empty string.
    ///
    /// @param line one frontmatter line
    /// @return the text before the colon
    private static String keyOf(String line) {
        int colon = line.indexOf(':');
        return colon < 0 ? "" : line.substring(0, colon).trim();
    }

    /// Strips the quotes a YAML string may be wrapped in.
    ///
    /// @param value the raw scalar
    /// @return the scalar without its wrapping quotes
    private static String unquote(String value) {
        char quote = value.isEmpty() ? '\0' : value.charAt(0);
        if (value.length() >= 2 && (quote == '"' || quote == '\'')
                && value.charAt(value.length() - 1) == quote) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /// Collapses every run of whitespace into one space.
    ///
    /// A description is written across lines as often as not, and the row that shows it
    /// has one line to show it in.
    ///
    /// @param text the raw text
    /// @return the text on one line
    private static String collapse(String text) {
        StringBuilder collapsed = new StringBuilder(text.length());
        boolean pendingSpace = false;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.isWhitespace(character)) {
                pendingSpace = collapsed.length() > 0;
                continue;
            }
            if (pendingSpace) {
                collapsed.append(' ');
                pendingSpace = false;
            }
            collapsed.append(character);
        }
        return collapsed.toString();
    }

    // ---- files ---------------------------------------------------------------------------------

    /// Reads a file as lines.
    ///
    /// @param file the file
    /// @return its lines, without terminators
    /// @throws DshException when it cannot be read
    private static List<String> readLines(Path file) throws DshException {
        try {
            return new ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new DshException("Failed to read " + file, e);
        }
    }

    /// Replaces a file with the given lines.
    ///
    /// Written beside the file and moved onto it, so a reader never sees a half-written
    /// frontmatter: the harness watches these directories and reads them the moment they
    /// change.
    ///
    /// @param file  the file
    /// @param lines what it should say
    /// @throws DshException when it cannot be written
    private static void write(Path file, List<String> lines) throws DshException {
        Path staging = file.resolveSibling(file.getFileName() + STAGING);
        try {
            Files.writeString(staging, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING);
            }
            Privileges.handWrittenFile(file, DshPaths.ROOT);
        } catch (IOException e) {
            throw new DshException("Failed to write " + file, e);
        }
    }

    /// Sums the bytes of a bundle's files.
    ///
    /// @param directory the bundle directory
    /// @return its size in bytes
    /// @throws IOException when it cannot be walked
    private static long sizeOf(Path directory) throws IOException {
        long total = 0L;
        try (Stream<Path> walk = Files.walk(directory)) {
            for (Path path : walk.toList()) {
                if (Files.isRegularFile(path)) {
                    total += Files.size(path);
                }
            }
        }
        return total;
    }

    /// Copies a directory tree.
    ///
    /// @param source the directory to copy
    /// @param target where it should land
    /// @throws IOException when it cannot be copied
    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path path : walk.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    /// Deletes a directory and everything under it.
    ///
    /// @param directory the directory
    /// @throws IOException when something cannot be deleted
    private static void deleteTree(Path directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
