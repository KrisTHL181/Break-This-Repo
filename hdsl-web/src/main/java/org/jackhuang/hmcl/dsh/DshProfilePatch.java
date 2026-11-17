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
import java.util.ArrayList;
import java.util.List;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// A profile's own patch layer, edited by lines.
///
/// A profile is composed from its bundles' layers and then from this file, and a plugin listed in
/// `dsh.profile.bundles` is applied **by that bundle's layer**. A pack that also inserts the same
/// plugin applies it twice, and a plugin that claims a route on the way up cannot be applied twice:
/// the profile then fails to load at all, with `webserver: duplicate exact route "…"` — which is how
/// one pack in the market came to be unlaunchable, and it is not obvious from the pack's contents,
/// because both halves look reasonable on their own.
///
/// So an insert that a bundle already applies is taken out. **An entry that carries configuration is
/// left alone and reported instead**: dropping it would drop the settings with it, and moving them
/// onto a row that targets the bundle's own id is a rewrite this does not do.
///
/// The other edit here is [`putProvider`][#putProvider], which is how an account's supplier reaches
/// the harness. Both are edits of a file that is the person's and that the harness itself rewrites,
/// so both are line edits: comments, `!!js` expressions, indentation and the file's own key order
/// all survive, and nothing outside the block being written is re-emitted.
@NotNullByDefault
public final class DshProfilePatch {
    private DshProfilePatch() {
    }

    /// Removes every insert that a bundle already applies.
    ///
    /// @param patch   the profile's patch layer
    /// @param bundles the bundle list the profile boots, in order
    /// @return the entry ids left alone because they carry configuration, never `null`
    /// @throws DshException when the file cannot be read or written
    public static List<String> dropRedundantInserts(Path patch, List<String> bundles)
            throws DshException {
        if (bundles.isEmpty() || !Files.isRegularFile(patch)) {
            return List.of();
        }
        String text;
        try {
            text = Files.readString(patch, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DshException("Could not read " + patch, e);
        }
        Edit edit = withoutRedundantInserts(text, bundles);
        if (edit.changed()) {
            try {
                Files.writeString(patch, edit.text(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new DshException("Could not write " + patch, e);
            }
            Privileges.handWrittenFile(patch, DshPaths.ROOT);
            LOG.info("Removed inserts from " + patch + " that its own bundles already apply");
        }
        return edit.withConfiguration();
    }

    /// What one pass over a patch produced.
    ///
    /// @param text             the patch without the redundant inserts
    /// @param changed          whether anything was taken out
    /// @param withConfiguration the entries left alone because they carry configuration
    public record Edit(String text, boolean changed, List<String> withConfiguration) {
    }

    /// Returns the patch without the inserts its bundles already apply, and what was left alone.
    ///
    /// The text form exists because a pack is written from a **copy**: the exporter puts this file
    /// into the archive, and the copy has to be repaired too — otherwise every pack made from an
    /// instance that carries such an insert hands the same broken profile to the next person.
    ///
    /// @param text    the patch
    /// @param bundles the bundle list the profile boots, in order
    /// @return what one pass produced
    public static Edit withoutRedundantInserts(String text, List<String> bundles) {
        if (bundles.isEmpty()) {
            return new Edit(text, false, List.of());
        }
        List<String> lines = List.of(text.split("\n", -1));

        List<String> kept = new ArrayList<>();
        List<String> withConfiguration = new ArrayList<>();
        boolean changed = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!isInsertItem(line)) {
                kept.add(line);
                continue;
            }
            int end = itemEnd(lines, i);
            String name = valueOf(lines, i, end, "name");
            if (name == null || !bundles.contains(unquote(name))) {
                kept.addAll(lines.subList(i, end));
                i = end - 1;
                continue;
            }
            if (hasKey(lines, i, end, "config")) {
                // Applied twice is wrong, but so is throwing the settings away: said out loud rather
                // than decided here.
                withConfiguration.add(valueOf(lines, i, end, "id") == null
                        ? unquote(name) : unquote(valueOf(lines, i, end, "id")));
                kept.addAll(lines.subList(i, end));
                i = end - 1;
                continue;
            }
            changed = true;
            i = end - 1;
        }

        return new Edit(String.join("\n", kept), changed, List.copyOf(withConfiguration));
    }

    /// Gives an entry one more provider route, under its `config.providers` mapping.
    ///
    /// This is where an account's supplier goes, and why it goes into this file rather than into a
    /// `--patch` overlay is argued in [DshAccountRoute]. What matters here is only the shape: the
    /// route has to land **inside the entry the configuration editor edits**, at
    /// `config.providers.<route>`, because a patch entry's `config` is replaced rather than merged —
    /// a second entry for the same id would take the person's own suppliers away instead of adding
    /// one.
    ///
    /// The entry written is the **last** one with this id, which is the entry the harness's editor
    /// edits (`findLastIndex` over the document's items) and therefore the one that wins the
    /// composition. Every other byte of the file is left where it is.
    ///
    /// @param patch the profile's patch layer
    /// @param entry the loader entry that holds the supplier routes
    /// @param route the route's name
    /// @param block the route's settings, one level in from the route's own key
    /// @return what the pass produced
    /// @throws DshException when the file cannot be read or written
    public static Edit putProvider(Path patch, String entry, String route, String block)
            throws DshException {
        String text = read(patch);
        Edit edit = putProvider(text, entry, route, block);
        if (edit.changed()) {
            write(patch, edit.text());
            LOG.info("Put the account route " + route + " into " + patch);
        }
        return edit;
    }

    /// Returns the patch with one provider route set, and whether anything changed.
    ///
    /// The text form is what is tested, and it is the whole operation: writing the file is the two
    /// lines around this call.
    ///
    /// @param text  the patch
    /// @param entry the loader entry that holds the supplier routes
    /// @param route the route's name
    /// @param block the route's settings, one level in from the route's own key
    /// @return what the pass produced
    /// @throws DshException when the entry is written in a shape this will not rewrite
    public static Edit putProvider(String text, String entry, String route, String block)
            throws DshException {
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        int item = lastEntryItem(lines, entry);
        if (item < 0) {
            // Nothing names the entry yet: a profile nobody has configured a supplier in.
            return new Edit(DshPluginPatch.appendEntry(text, newEntry(entry, route, block)), true,
                    List.of());
        }
        int end = itemEnd(lines, item);
        int body = childIndent(lines, item, end, indentOf(lines.get(item)));
        int config = keyAtIndent(lines, item, end, "config", body);
        if (config < 0) {
            // A row that only names the entry, or disables it: everything below is added.
            int at = beforeBlanks(lines, end);
            lines.addAll(at, List.of(pad(body) + "config:", pad(body + 2) + "providers:"));
            lines.addAll(at + 2, routeLines(route, block, body + 4, step(lines, item, body)));
            return changed(lines, text);
        }

        String configLine = contentOf(lines.get(config));
        if (!configLine.equals("config:") && !isEmptyMapping(configLine, "config")) {
            // A `config` written inline carries settings this will not parse and re-emit, and writing
            // a second entry for the same id would take them away rather than add to them. Saying so
            // is better than either: the launch stops with a sentence about the line to rewrite, and
            // the person's suppliers stay theirs.
            throw new DshException("The entry " + entry + " has its config written inline ("
                    + contentOf(lines.get(config)) + "); the providers of a supplier cannot be added"
                    + " to it. Write that entry's config as a block, or launch on a profile that has"
                    + " no such entry.");
        }
        int configIndent = indentOf(lines.get(config));
        if (!configLine.equals("config:")) {
            lines.set(config, pad(configIndent) + "config:");
        }
        int configEnd = blockEnd(lines, config + 1, end, configIndent);

        int providers = keyDeeperThan(lines, config + 1, configEnd, "providers", configIndent);
        if (providers < 0) {
            int child = childIndent(lines, config + 1, configEnd, configIndent);
            int at = beforeBlanks(lines, configEnd);
            lines.addAll(at, List.of(pad(child) + "providers:"));
            lines.addAll(at + 1, routeLines(route, block, child + 2, step(lines, item, body)));
            return changed(lines, text);
        }

        String providersLine = contentOf(lines.get(providers));
        if (!providersLine.equals("providers:") && !isEmptyMapping(providersLine, "providers")) {
            throw new DshException("The entry " + entry + " has its providers written inline ("
                    + contentOf(lines.get(providers)) + "); a supplier cannot be added to it. Write"
                    + " that mapping as a block, or launch on a profile that has no such entry.");
        }
        int providersIndent = indentOf(lines.get(providers));
        if (!providersLine.equals("providers:")) {
            lines.set(providers, pad(providersIndent) + "providers:");
        }
        int providersEnd = blockEnd(lines, providers + 1, end, providersIndent);

        int existing = routeKey(lines, providers + 1, providersEnd, route, providersIndent);
        if (existing < 0) {
            int child = childIndent(lines, providers + 1, providersEnd, providersIndent);
            lines.addAll(beforeBlanks(lines, providersEnd),
                    routeLines(route, block, child, step(lines, item, body)));
            return changed(lines, text);
        }
        int routeIndent = indentOf(lines.get(existing));
        int routeEnd = beforeBlanks(lines,
                blockEnd(lines, existing + 1, providersEnd, routeIndent));
        lines.subList(existing, routeEnd).clear();
        lines.addAll(existing, routeLines(route, block, routeIndent, step(lines, item, body)));
        return changed(lines, text);
    }

    /// Takes one provider route back out of an entry, leaving everything else where it is.
    ///
    /// A route the launcher wrote belongs to the launch that wrote it: its key travels in a variable
    /// that launch set, and nothing sets it afterwards. So the route goes when the launch ends —
    /// otherwise the harness keeps offering a supplier with no key behind it, and a launch that wants
    /// no supplier inherits the last one's. That is the contract the home's own settings have always
    /// had; this is it for the profile patch.
    ///
    /// Nothing else in the file is touched, and a route that is not there leaves the file alone.
    ///
    /// @param patch the profile's patch layer
    /// @param entry the loader entry that holds the supplier routes
    /// @param route the route's name
    /// @return what the pass produced
    /// @throws DshException when the file cannot be read or written
    public static Edit removeProvider(Path patch, String entry, String route) throws DshException {
        String text = read(patch);
        Edit edit = removeProvider(text, entry, route);
        if (edit.changed()) {
            write(patch, edit.text());
            LOG.info("Took the account route " + route + " back out of " + patch);
        }
        return edit;
    }

    /// Returns the patch without one provider route, and whether anything changed.
    ///
    /// @param text  the patch
    /// @param entry the loader entry that holds the supplier routes
    /// @param route the route's name
    /// @return what the pass produced
    public static Edit removeProvider(String text, String entry, String route) {
        List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
        int[] block = routeBlock(lines, entry, route);
        if (block == null) {
            return new Edit(text, false, List.of());
        }
        lines.subList(block[0], block[1]).clear();
        return changed(lines, text);
    }

    /// Returns where one route's block sits: its first line, and the line after its last.
    ///
    /// The route's key line is part of the block, and so is everything indented under it, up to the
    /// next sibling. Blank lines between the two are left to the file: taking them out would move the
    /// spacing the person chose for the routes around it.
    ///
    /// @param lines the file's lines
    /// @param entry the loader entry that holds the supplier routes
    /// @param route the route's name
    /// @return the two line numbers, or `null` when the file has no such route
    private static int @Nullable [] routeBlock(List<String> lines, String entry, String route) {
        int item = lastEntryItem(lines, entry);
        if (item < 0) {
            return null;
        }
        int end = itemEnd(lines, item);
        int body = childIndent(lines, item, end, indentOf(lines.get(item)));
        int config = keyAtIndent(lines, item, end, "config", body);
        if (config < 0) {
            return null;
        }
        int configIndent = indentOf(lines.get(config));
        int configEnd = blockEnd(lines, config + 1, end, configIndent);
        int providers = keyDeeperThan(lines, config + 1, configEnd, "providers", configIndent);
        if (providers < 0) {
            return null;
        }
        int providersIndent = indentOf(lines.get(providers));
        int providersEnd = blockEnd(lines, providers + 1, end, providersIndent);
        int at = routeKey(lines, providers + 1, providersEnd, route, providersIndent);
        if (at < 0) {
            return null;
        }
        return new int[]{at, beforeBlanks(lines,
                blockEnd(lines, at + 1, providersEnd, indentOf(lines.get(at))))};
    }

    /// Returns what a rewritten line list produced, and whether it differs from the text.
    ///
    /// Equality is the point: a launch that finds its route already written exactly as the account
    /// states it must not touch the file, so the file's modification time keeps meaning "the person
    /// or the harness changed this".
    ///
    /// @param lines the lines
    /// @param text  the text they came from
    /// @return what the pass produced
    private static Edit changed(List<String> lines, String text) {
        String updated = String.join("\n", lines);
        return new Edit(updated, !updated.equals(text), List.of());
    }

    /// Reports whether a line is a key whose value is an empty mapping.
    ///
    /// @param content the line without its indentation
    /// @param key     the key
    /// @return whether it is `key: {}`
    private static boolean isEmptyMapping(String content, String key) {
        String value = content.substring(key.length() + 1).trim();
        return value.equals("{}") || value.equals("{ }");
    }

    /// Returns the last entry with an id, as a line index, or `-1`.
    ///
    /// Top level only: an `insert` list holds entries of its own at a deeper indentation, and one of
    /// those is not a row of this file.
    ///
    /// @param lines the file's lines
    /// @param entry the id
    /// @return the line the entry opens on, or `-1`
    private static int lastEntryItem(List<String> lines, String entry) {
        int found = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (indentOf(lines.get(i)) != 0) {
                continue;
            }
            String content = contentOf(lines.get(i));
            if (content.startsWith("id:") && unquote(content.substring(3)).equals(entry)) {
                found = i;
            }
        }
        return found;
    }

    /// Renders a whole entry for an id that the file does not mention yet.
    ///
    /// @param entry the loader entry
    /// @param route the route's name
    /// @param block the route's settings
    /// @return the entry, ending in a newline
    private static String newEntry(String entry, String route, String block) {
        return "- id: " + YamlScalar.of(entry) + "\n  config:\n    providers:\n"
                + String.join("\n", routeLines(route, block, 6, 2)) + "\n";
    }

    /// Renders the route's own lines at an indentation, in the file's own steps.
    ///
    /// The block arrives two spaces per level, which is what the harness writes; a file indented four
    /// spaces at a time keeps four, because the levels are the file's and not this method's.
    ///
    /// @param route  the route's name
    /// @param block  the route's settings, one level in
    /// @param indent the indentation of the route's key
    /// @param step   the file's indentation step
    /// @return the lines
    private static List<String> routeLines(String route, String block, int indent, int step) {
        List<String> lines = new ArrayList<>();
        lines.add(pad(indent) + YamlScalar.of(route) + ":");
        for (String line : block.split("\n", -1)) {
            if (line.isEmpty()) {
                continue;
            }
            int level = indentOf(line) / 2 + 1;
            lines.add(pad(indent + step * level) + line.trim());
        }
        return lines;
    }

    /// Returns the file's indentation step, from an entry's own children.
    ///
    /// @param lines the file's lines
    /// @param item  the entry's first line
    /// @param body  the entry's children's indentation
    /// @return how far one level goes, two spaces when the file does not say
    private static int step(List<String> lines, int item, int body) {
        int own = indentOf(lines.get(item));
        return body > own ? body - own : 2;
    }

    /// Returns the indentation of a block's children.
    ///
    /// The file's own, taken from a child that is already there, and two spaces when there is none:
    /// the harness writes two, and a hand-written file that uses four keeps four.
    ///
    /// @param lines        the file's lines
    /// @param start        the block's first line
    /// @param end          the line after the block
    /// @param parentIndent the block's own indentation
    /// @return the children's indentation
    private static int childIndent(List<String> lines, int start, int end, int parentIndent) {
        for (int i = start; i < end && i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isBlank() && indentOf(line) > parentIndent) {
                return indentOf(line);
            }
        }
        return parentIndent + 2;
    }

    /// Returns where a block is written into: the index just before its last blank lines.
    ///
    /// A line list carries the text's final newline as an empty last line, and a mapping may be
    /// spaced out by hand. Both are blanks, and writing *after* one would leave a blank line inside a
    /// mapping — or move the file's final newline into the middle of it.
    ///
    /// @param lines the file's lines
    /// @param index the index a block would go at
    /// @return the index to write at
    private static int beforeBlanks(List<String> lines, int index) {
        int at = Math.min(index, lines.size());
        while (at > 0 && lines.get(at - 1).isBlank()) {
            at--;
        }
        return at;
    }

    /// Returns where a block ends: the next line at or above its own indentation.
    ///
    /// Blank lines are stepped over rather than treated as the end, because a mapping written by hand
    /// may be spaced out and stopping at the first blank would leave half of it behind.
    ///
    /// @param lines  the file's lines
    /// @param start  the first line after the block's opening
    /// @param end    the line the block cannot reach past
    /// @param indent the block's own indentation
    /// @return the first line after the block
    private static int blockEnd(List<String> lines, int start, int end, int indent) {
        for (int i = start; i < end && i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.isBlank() && indentOf(line) <= indent) {
                return i;
            }
        }
        return Math.min(end, lines.size());
    }

    /// Returns the line a key sits on at exactly one indentation, or `-1`.
    ///
    /// @param lines  the file's lines
    /// @param start  the first line to look at
    /// @param end    the line to stop before
    /// @param key    the key
    /// @param indent the indentation it has to have
    /// @return the line's index, or `-1`
    private static int keyAtIndent(List<String> lines, int start, int end, String key, int indent) {
        for (int i = start; i < end && i < lines.size(); i++) {
            if (indentOf(lines.get(i)) == indent && startsWithKey(lines.get(i), key)) {
                return i;
            }
        }
        return -1;
    }

    /// Returns the line a key sits on below an indentation, or `-1`.
    ///
    /// @param lines        the file's lines
    /// @param start        the first line to look at
    /// @param end          the line to stop before
    /// @param key          the key
    /// @param parentIndent the indentation it has to be inside
    /// @return the line's index, or `-1`
    private static int keyDeeperThan(List<String> lines, int start, int end, String key,
                                     int parentIndent) {
        for (int i = start; i < end && i < lines.size(); i++) {
            if (indentOf(lines.get(i)) > parentIndent && startsWithKey(lines.get(i), key)) {
                return i;
            }
        }
        return -1;
    }

    /// Returns the line a route's own key sits on, or `-1`.
    ///
    /// Matched by value rather than by text, because the harness quotes a name that needs it and the
    /// launcher may have written it unquoted.
    ///
    /// @param lines        the file's lines
    /// @param start        the first line to look at
    /// @param end          the line to stop before
    /// @param route        the route's name
    /// @param parentIndent the indentation it has to be inside
    /// @return the line's index, or `-1`
    private static int routeKey(List<String> lines, int start, int end, String route,
                                int parentIndent) {
        for (int i = start; i < end && i < lines.size(); i++) {
            if (indentOf(lines.get(i)) <= parentIndent) {
                continue;
            }
            String content = contentOf(lines.get(i));
            if (content.endsWith(":") && unquote(content.substring(0, content.length() - 1)).equals(route)) {
                return i;
            }
        }
        return -1;
    }

    /// Reports whether a line names a key, whatever it puts after it.
    ///
    /// The point of matching a key with a value is that a value this will not rewrite — an inline
    /// mapping — has to be *found*, so that it can be refused. Missing it would silently write a
    /// second key of the same name and shadow whatever the person had written.
    ///
    /// @param line the line
    /// @param key  the key
    /// @return whether the line names it
    private static boolean startsWithKey(String line, String key) {
        String content = contentOf(line);
        return content.startsWith(key + ":")
                && (content.length() == key.length() + 1
                        || Character.isWhitespace(content.charAt(key.length() + 1)));
    }

    /// Returns that many spaces.
    ///
    /// @param count how many
    /// @return the spaces
    private static String pad(int count) {
        return " ".repeat(Math.max(0, count));
    }

    /// Reads a patch file, treating absence as an empty file.
    ///
    /// @param patch the file
    /// @return the text, or an empty string
    /// @throws DshException when the file exists but cannot be read
    private static String read(Path patch) throws DshException {
        if (!Files.isRegularFile(patch)) {
            return "";
        }
        try {
            return Files.readString(patch, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DshException("Could not read " + patch, e);
        }
    }

    /// Writes a file the harness may be reading, through a staging file.
    ///
    /// @param patch the file
    /// @param text  what to put in it
    /// @throws DshException when it cannot be written
    private static void write(Path patch, String text) throws DshException {
        try {
            Files.createDirectories(patch.getParent());
            Path staging = patch.resolveSibling(patch.getFileName() + ".hdsl-route");
            Files.writeString(staging, text, StandardCharsets.UTF_8);
            try {
                Files.move(staging, patch, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(staging, patch, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Privileges.handWrittenFile(patch, DshPaths.ROOT);
        } catch (IOException e) {
            throw new DshException("Could not write " + patch, e);
        }
    }

    /// Reports whether a line opens an item of an `insert` list.
    ///
    /// The shape is the loader's: a patch entry is `- insert:` and the plugins it inserts are the
    /// lines under it that begin with `- `.
    ///
    /// @param line the line
    /// @return whether it opens an inserted plugin
    private static boolean isInsertItem(String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("- ")) {
            return false;
        }
        // An item that names a plugin, not the `- insert:` that holds the list: the latter sits at the
        // same indentation as the list's own items would at the file's top level.
        return trimmed.startsWith("- id:") || trimmed.startsWith("- name:");
    }

    /// Returns where the item starting at `start` ends.
    ///
    /// @param lines the file's lines
    /// @param start the item's first line
    /// @return the first line after the item
    private static int itemEnd(List<String> lines, int start) {
        int indent = indentOf(lines.get(start));
        int i = start + 1;
        for (; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            if (indentOf(line) <= indent) {
                break;
            }
        }
        return i;
    }

    /// Returns the value of a key inside an item, or `null`.
    ///
    /// @param lines the file's lines
    /// @param start the item's first line
    /// @param end   the line after the item
    /// @param key   the key
    /// @return the raw value, or `null` when the item does not name it
    private static String valueOf(List<String> lines, int start, int end, String key) {
        int at = keyAt(lines, start, end, key);
        return at < 0 ? null : contentOf(lines.get(at)).substring(key.length() + 1).trim();
    }

    /// Reports whether an item names a key.
    ///
    /// @param lines the file's lines
    /// @param start the item's first line
    /// @param end   the line after the item
    /// @param key   the key
    /// @return whether the item has it
    private static boolean hasKey(List<String> lines, int start, int end, String key) {
        return keyAt(lines, start, end, key) >= 0;
    }

    /// Returns the line an item names a key on, or `-1`.
    ///
    /// @param lines the file's lines
    /// @param start the item's first line
    /// @param end   the line after the item
    /// @param key   the key
    /// @return the line's index, or `-1`
    private static int keyAt(List<String> lines, int start, int end, String key) {
        for (int i = start; i < end && i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("- ")) {
                trimmed = trimmed.substring(2);
            }
            if (trimmed.startsWith(key + ":")
                    && (trimmed.length() == key.length() + 1
                            || Character.isWhitespace(trimmed.charAt(key.length() + 1)))) {
                return i;
            }
        }
        return -1;
    }

    /// Returns a line without its indentation or the list marker that opens it.
    ///
    /// An inserted plugin is written as `- id: …`, so the key is behind both.
    ///
    /// @param line the line
    /// @return what is left
    private static String contentOf(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("- ") ? trimmed.substring(2).trim() : trimmed;
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return i;
    }

    /// Returns a YAML scalar without the quotes a writer may have put around it.
    ///
    /// @param value the raw value
    /// @return the value itself
    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2
                && (trimmed.charAt(0) == '\'' || trimmed.charAt(0) == '"')
                && trimmed.charAt(trimmed.length() - 1) == trimmed.charAt(0)) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
