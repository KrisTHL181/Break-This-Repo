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
import org.jackhuang.hmcl.util.platform.Privileges;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The plugin settings a pack carries, and the ones it must not.
///
/// The harness keeps user settings in one file, `settings.yaml`, as one section per plugin: a
/// sidebar's custom CSS, a market's preferences, a workstation's switches. That file is most of what
/// "the same environment" means for a plugin — a pack that restored the plugin but not its settings
/// would hand somebody the same program configured differently — so a pack carries the sections and
/// merges them into the next home on install, leaving every section it does not carry untouched.
///
/// Three kinds of section are **not** carried, and the reasons differ:
///
/// - `agent-default-model` and `llm-pi-ai` belong to the launcher's own account plumbing. They name
///   provider routes this launcher built for *this* machine's account, and a route is not a setting
///   that travels: on the next machine it names a provider that is not there.
/// - `ui-onboarding` is the harness's record that somebody has been shown the tour, which a new
///   instance should be shown.
/// - Anything that looks like a credential is removed from the sections that do travel. A key in a
///   pack is a key handed to whoever opens the pack. The harness's own design keeps keys in
///   `.credentials.yaml`, which no pack reads — but a plugin can put a literal key in its own
///   settings, and `remote-web-ui` in fact keeps a tunnel token in one. A value that names a
///   credential rather than being one — `apiKeyCredentialRef: PUBCHEM_API_KEY` — is kept: that is the
///   portable half, and the key it names stays where it belongs.
///
/// The file is edited as lines rather than parsed as YAML, the way [DshDefaultModel] edits its own
/// section: everything outside the sections a pack carries has to come back unchanged, including
/// comments, ordering, and anything a harness version writes that this launcher does not know about.
@NotNullByDefault
public final class DshPluginSettings {
    /// The harness's settings file, in a home.
    private static final String FILE = "settings.yaml";

    /// The name the file travels under inside a pack.
    public static final String ENTRY = "settings.yaml";

    /// The sections that never travel, whatever a pack asks for.
    ///
    /// A list rather than a comment, so the reader and the writer cannot disagree: a pack a person
    /// edited by hand is a pack this has to refuse just the same.
    private static final Set<String> RESERVED = Set.of("agent-default-model", "llm-pi-ai", "ui-onboarding");

    /// A key whose value is a credential rather than a reference to one.
    ///
    /// Deliberately broad: carrying a secret by accident is a real harm, while leaving out a setting
    /// that merely sounds like one is a small loss, reported by name when the pack is written.
    private static final Pattern SECRET_KEY = Pattern.compile(
            "(?i).*(token|secret|password|passwd|api[-_]?key|apikey|credential|private[-_]?key|"
                    + "access[-_]?key|client[-_]?secret|auth[-_]?key|bearer).*");

    /// A key that names a credential rather than holding one.
    private static final Pattern REFERENCE_KEY = Pattern.compile(
            "(?i).*(ref|refs|name|env|id|path|file|store|provider)$");

    /// What a pack carries of the settings.
    ///
    /// @param text     the YAML to put in the pack, empty when nothing travels
    /// @param sections the section names carried, in the file's order
    /// @param omitted  the `section.key` paths left out because they look like credentials
    public record Carried(String text, List<String> sections, List<String> omitted) {
    }

    private DshPluginSettings() {
    }

    /// Returns the sections of a home's settings that a pack can carry.
    ///
    /// @param home the `DSH_HOME`
    /// @return the section names, in the file's order
    /// @throws DshException when the file cannot be read
    public static List<String> sectionsOf(Path home) throws DshException {
        List<String> names = new ArrayList<>();
        for (Section section : split(read(home)).sections()) {
            if (!RESERVED.contains(section.name())) {
                names.add(section.name());
            }
        }
        return List.copyOf(names);
    }

    /// Builds the settings a pack carries.
    ///
    /// @param home   the home to read
    /// @param wanted the sections to carry; a name that is not there is skipped
    /// @return what travels, with the credential-looking values left out
    /// @throws DshException when the file cannot be read
    public static Carried extract(Path home, Set<String> wanted) throws DshException {
        Parsed parsed = split(read(home));
        StringBuilder text = new StringBuilder();
        List<String> sections = new ArrayList<>();
        List<String> omitted = new ArrayList<>();
        for (Section section : parsed.sections()) {
            if (!wanted.contains(section.name()) || RESERVED.contains(section.name())) {
                continue;
            }
            sections.add(section.name());
            text.append("# ").append(section.name()).append('\n');
            text.append(parsed.lines().get(section.header())).append('\n');
            // The section's own keys are the ones at its shallowest indentation; anything deeper
            // belongs to them, and a block scalar's body is not read at all.
            int bodyIndent = -1;
            boolean scalar = false;
            for (String line : parsed.lines().subList(section.header() + 1, section.end())) {
                if (!line.isBlank()) {
                    int indent = indentOf(line);
                    if (scalar && indent <= bodyIndent) {
                        scalar = false;
                    } else if (scalar) {
                        text.append(line).append('\n');
                        continue;
                    }
                    if (bodyIndent < 0) {
                        bodyIndent = indent;
                    }
                    if (indent == bodyIndent && opensBlockScalar(line)) {
                        scalar = true;
                        text.append(line).append('\n');
                        continue;
                    }
                    if (indent == bodyIndent && isCredential(line)) {
                        omitted.add(section.name() + "." + keyOf(line));
                        continue;
                    }
                }
                text.append(line).append('\n');
            }
        }
        return new Carried(text.toString(), List.copyOf(sections), List.copyOf(omitted));
    }

    /// Merges a pack's settings into a home's own file.
    ///
    /// A section the pack carries is merged **key by key** into the home's section of that name: the
    /// pack's value wins for every key it has, and a key it does not have keeps the value the home
    /// had. That is not a detail. A key is missing from a pack either because the author did not have
    /// it or because it looked like a credential and was deliberately left out — and replacing the
    /// section wholesale would then delete *the recipient's* credential, which is the opposite of
    /// what leaving one out was for. A key the home does not have is added, and every other line of
    /// the home's file — its comments, its ordering, the sections for plugins the pack says nothing
    /// about — is left exactly as it was.
    ///
    /// @param home    the `DSH_HOME`
    /// @param carried what the pack holds
    /// @return the section names that were merged
    /// @throws DshException when the file cannot be read or written
    public static List<String> merge(Path home, String carried) throws DshException {
        Parsed incoming = split(carried);
        if (incoming.sections().isEmpty()) {
            return List.of();
        }

        String existing = read(home);
        Parsed current = split(existing);
        List<String> lines = new ArrayList<>(current.lines());
        List<Section> replaced = new ArrayList<>();
        List<Section> appended = new ArrayList<>();
        List<String> merged = new ArrayList<>();

        for (Section section : incoming.sections()) {
            if (RESERVED.contains(section.name())) {
                // A pack that names a section this refuses to carry is refused the same way on the
                // way in, because a pack is a file somebody can edit.
                LOG.warning("A pack carries the settings section " + section.name()
                        + ", which this launcher does not carry");
                continue;
            }
            merged.add(section.name());
            int index = current.indexOf(section.name());
            if (index < 0) {
                appended.add(section);
            } else {
                replaced.add(current.sections().get(index));
            }
        }

        // Back to front: replacing a block changes the indices of everything after it.
        List<Section> ordered = new ArrayList<>(replaced);
        ordered.sort((left, right) -> Integer.compare(right.start(), left.start()));
        for (Section mine : ordered) {
            Section section = incoming.section(mine.name());
            if (section == null) {
                continue;
            }
            List<String> block = new ArrayList<>(mine.preamble(current.lines()));
            // The home's own comment above its section is the person's; the pack brings content.
            block.add(current.lines().get(mine.header()));
            block.addAll(mergedBody(mine.body(current.lines()), section.body(incoming.lines())));
            lines.subList(mine.start(), mine.end()).clear();
            lines.addAll(mine.start(), block);
        }

        for (Section section : appended) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).isBlank()) {
                lines.add("");
            }
            lines.addAll(section.lines(incoming.lines()));
        }

        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        write(home.resolve(FILE), lines.isEmpty() ? "" : String.join("\n", lines) + "\n");
        return List.copyOf(merged);
    }

    /// Returns the sections a carried text holds.
    ///
    /// @param carried the carried text
    /// @return the section names
    public static List<String> sectionsIn(String carried) {
        Set<String> names = new LinkedHashSet<>();
        for (Section section : split(carried).sections()) {
            names.add(section.name());
        }
        return List.copyOf(names);
    }

    /// Merges one section's body into another, key by key.
    ///
    /// The pack's value wins for every key it declares; a key only the home has keeps the home's own
    /// lines, which is what keeps a credential that was left out of the pack from being deleted here.
    ///
    /// @param home   the home's body
    /// @param packed the pack's body
    /// @return the merged body
    private static List<String> mergedBody(List<String> home, List<String> packed) {
        int indent = bodyIndentOf(packed) < 0 ? bodyIndentOf(home) : bodyIndentOf(packed);
        if (indent < 0) {
            return List.copyOf(packed);
        }
        Map<String, List<String>> mine = entries(home, indent);
        Map<String, List<String>> theirs = entries(packed, indent);

        List<String> merged = new ArrayList<>(headOf(home, indent));
        for (Map.Entry<String, List<String>> entry : mine.entrySet()) {
            merged.addAll(theirs.getOrDefault(entry.getKey(), entry.getValue()));
        }
        for (Map.Entry<String, List<String>> entry : theirs.entrySet()) {
            if (!mine.containsKey(entry.getKey())) {
                merged.addAll(entry.getValue());
            }
        }
        return merged;
    }

    /// Reads a section body as one block of lines per declared key, in order.
    ///
    /// @param body   the lines
    /// @param indent the indentation the section's own keys are at
    /// @return the blocks, by key, in the order they are declared
    private static Map<String, List<String>> entries(List<String> body, int indent) {
        Map<String, List<String>> entries = new LinkedHashMap<>();
        String key = null;
        List<String> block = new ArrayList<>();
        for (String line : body) {
            String declared = !line.isBlank() && indentOf(line) == indent && keyOf(line.trim()) != null
                    ? keyOf(line.trim()) : null;
            if (declared != null) {
                if (key != null) {
                    entries.put(key, List.copyOf(block));
                }
                key = declared;
                block = new ArrayList<>();
            }
            if (key != null) {
                block.add(line);
            }
        }
        if (key != null) {
            entries.put(key, List.copyOf(block));
        }
        return entries;
    }

    /// Returns the lines of a section body that come before its first declared key.
    ///
    /// @param body   the lines
    /// @param indent the indentation the section's own keys are at
    /// @return the lines
    private static List<String> headOf(List<String> body, int indent) {
        List<String> head = new ArrayList<>();
        for (String line : body) {
            if (!line.isBlank() && indentOf(line) == indent && keyOf(line.trim()) != null) {
                break;
            }
            head.add(line);
        }
        return head;
    }

    /// Returns the indentation a section body's own keys are at.
    ///
    /// @param body the lines
    /// @return the indentation, or -1 when the body declares no key
    private static int bodyIndentOf(List<String> body) {
        for (String line : body) {
            if (!line.isBlank() && keyOf(line.trim()) != null
                    && (indentOf(line) > 0 || !line.startsWith("#"))) {
                return indentOf(line);
            }
        }
        return -1;
    }

    /// Reads a home's settings file.
    ///
    /// @param home the home
    /// @return the text, or an empty string when there is no file
    /// @throws DshException when the file cannot be read
    private static String read(Path home) throws DshException {
        Path file = home.resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DshException("Failed to read " + file, e);
        }
    }

    /// Replaces a home's settings file.
    ///
    /// Written beside the file and moved into place, so no reader — the harness, or another launcher —
    /// ever sees half of it.
    ///
    /// @param file the file
    /// @param text what to write
    /// @throws DshException when the file cannot be written
    private static void write(Path file, String text) throws DshException {
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path staging = file.resolveSibling(FILE + ".pack");
            Files.writeString(staging, text, StandardCharsets.UTF_8);
            Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING);
            Privileges.handWrittenFile(file, DshPaths.ROOT);
        } catch (IOException e) {
            throw new DshException("Failed to write " + file, e);
        }
    }

    /// Returns whether a line declares a value that must not travel.
    ///
    /// Only the section's own level is judged. A nested value is named by the key above it rather than
    /// by its own, so this cannot attribute one to a section confidently — and a mapping that holds a
    /// credential under a harmless name is a case the export reports rather than guesses at.
    ///
    /// @param line the line
    /// @return whether it looks like a credential
    private static boolean isCredential(String line) {
        String key = keyOf(line.trim());
        if (key == null || !SECRET_KEY.matcher(key).matches()) {
            return false;
        }
        // `apiKeyCredentialRef: PUBCHEM_API_KEY` is the portable half: the name of a key, not a key.
        if (REFERENCE_KEY.matcher(key).matches()) {
            return false;
        }
        String value = valueOf(line);
        if (value.isEmpty()) {
            return false;
        }
        // A value that is an interpolation or an environment reference holds no secret itself.
        return !value.startsWith("$") && !value.startsWith("env:");
    }

    /// Returns the key of a section-level `key: value` line.
    ///
    /// @param line the line
    /// @return the key, or `null` when the line does not declare one
    static @Nullable String keyOf(String line) {
        int colon = line.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        String key = line.substring(0, colon).trim();
        return key.isEmpty() || key.indexOf(' ') >= 0 || key.indexOf('\t') >= 0 ? null : key;
    }

    /// Returns the value of a section-level `key: value` line.
    ///
    /// @param line the line
    /// @return the value, trimmed, or an empty string
    private static String valueOf(String line) {
        String trimmed = line.trim();
        int colon = trimmed.indexOf(':');
        return colon < 0 ? "" : trimmed.substring(colon + 1).trim();
    }

    /// Returns how far a line is indented.
    ///
    /// @param line the line
    /// @return the number of leading spaces and tabs
    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    /// Reports whether a key's value is a block scalar, whose body is carried verbatim.
    ///
    /// @param line the line
    /// @return whether it opens one
    private static boolean opensBlockScalar(String line) {
        String value = valueOf(line);
        return value.equals("|") || value.equals("|-") || value.equals("|+")
                || value.equals(">") || value.equals(">-") || value.equals(">+");
    }

    /// The file, as sections and the lines they are cut out of.
    ///
    /// @param sections the sections, in the file's order
    /// @param lines    every line of the file, so a section can be replaced by index
    private record Parsed(List<Section> sections, List<String> lines) {

        /// Returns the index of a section in the list.
        ///
        /// @param name the section name
        /// @return the index, or -1 when there is no such section
        int indexOf(String name) {
            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).name().equals(name)) {
                    return i;
                }
            }
            return -1;
        }

        /// Returns a section by name.
        ///
        /// @param name the section name
        /// @return the section, or `null` when there is none
        @Nullable Section section(String name) {
            int index = indexOf(name);
            return index < 0 ? null : sections.get(index);
        }
    }

    /// One top-level section of the file.
    ///
    /// @param name   the section's key
    /// @param start  the index of the first line of its block, comment lines included
    /// @param header the index of the line that declares it
    /// @param end    the index after the section's last line
    private record Section(String name, int start, int header, int end) {

        /// Returns the comment lines that introduce the section.
        ///
        /// @param file the file's lines
        /// @return the preamble
        List<String> preamble(List<String> file) {
            return List.copyOf(file.subList(start, header));
        }

        /// Returns the section's own lines, the line that declares it included.
        ///
        /// @param file the file's lines
        /// @return the lines
        List<String> lines(List<String> file) {
            return List.copyOf(file.subList(header, end));
        }

        /// Returns the section's lines under the one that declares it.
        ///
        /// @param file the file's lines
        /// @return the lines
        List<String> body(List<String> file) {
            return List.copyOf(file.subList(header + 1, end));
        }
    }

    /// Splits a settings file into its top-level sections.
    ///
    /// A section starts at a line whose first character is not whitespace and which declares a key —
    /// which is what a top-level key is in YAML — and runs to the next one. Comments and blank lines
    /// before a section are part of its block, so replacing a section takes its heading with it, while
    /// a merge can put the person's own heading back. A block scalar's body is indented, so it belongs
    /// to the section that opened it and is never read as a declaration of its own: a stylesheet is
    /// full of lines that look like `padding: 2px`.
    ///
    /// @param text the file's contents
    /// @return the sections, and the file's lines
    private static Parsed split(String text) {
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            // The trailing newline is a terminator, not an empty last line: keeping it would make
            // every merge grow the file by one line per round.
            lines.remove(lines.size() - 1);
        }

        List<Integer> headers = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String key = declares(lines.get(i)) ? keyOf(lines.get(i)) : null;
            if (key != null) {
                headers.add(i);
                names.add(key);
            }
        }

        List<Section> sections = new ArrayList<>();
        int previousEnd = 0;
        for (int k = 0; k < headers.size(); k++) {
            int header = headers.get(k);
            int next = k + 1 < headers.size() ? headers.get(k + 1) : lines.size();
            // The content ends after the last line that is neither blank nor a comment: the run of
            // comments and blanks before the next section introduces *that* section.
            int end = header + 1;
            for (int i = next - 1; i > header; i--) {
                if (!introduced(lines.get(i))) {
                    end = i + 1;
                    break;
                }
            }
            // And this section's block starts at the run of comments and blanks before it.
            int start = header;
            while (start > previousEnd && introduced(lines.get(start - 1))) {
                start--;
            }
            sections.add(new Section(names.get(k), start, header, end));
            previousEnd = end;
        }
        return new Parsed(List.copyOf(sections), List.copyOf(lines));
    }

    /// Reports whether a line declares a top-level key.
    ///
    /// @param line the line
    /// @return whether it declares one
    private static boolean declares(String line) {
        return !line.isBlank() && !Character.isWhitespace(line.charAt(0))
                && !line.startsWith("#") && keyOf(line) != null;
    }

    /// Reports whether a line introduces the section that follows it.
    ///
    /// @param line the line
    /// @return whether it is a comment or blank
    private static boolean introduced(String line) {
        return line.isBlank() || line.startsWith("#");
    }
}
