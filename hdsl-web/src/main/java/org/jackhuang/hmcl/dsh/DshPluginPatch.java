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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jackhuang.hmcl.util.platform.Privileges;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Switches an installed plugin on and off in a profile's own patch layer.
///
/// DeepSeek Harness composes a profile from its bundles' layers and then from
/// the profile's `cordis.patch.yml`. A bundle switches **itself** off by a row
/// there —
///
/// ```yaml
/// - id: better-sidebar
///   disabled: true
/// ```
///
/// — which the loader re-applies on every boot. The row id is the loader entry
/// the package inserts, not the package name, so the package's own bundle patch
/// is what says which rows are its; a bundle patch may also carry rows that only
/// **reconfigure** other plugins, and writing `disabled: true` onto those would
/// take a neighbour down with it. That distinction is why [insertedIds] reads
/// only rows nested under an `insert:` block.
///
/// The patch file is the person's, so it is edited by appending and by removing
/// exactly the block this class wrote, never by reparsing and re-emitting it: a
/// hand-written `!!js` expression or comment survives intact.
@NotNullByDefault
public final class DshPluginPatch {
    private DshPluginPatch() {
    }

    /// The profile's own patch layer, the file a toggle writes.
    public static final String PATCH_NAME = "cordis.patch.yml";

    /// The conventional bundle patch a package may ship without declaring it.
    private static final String CONVENTIONAL_PATCH = "cordis.patch.yml";

    /// Row ids this class is willing to write: plain unquoted YAML scalars.
    private static final Pattern ROW_ID = Pattern.compile("^[A-Za-z0-9_.-]+$");

    /// Matches an `insert:` key, with or without a list marker.
    private static final Pattern INSERT_LINE = Pattern.compile("^\\s*-?\\s*insert:\\s*$");

    /// Matches a row key that keeps an open insert block open.
    private static final Pattern ROW_KEY = Pattern.compile("^\\s*-?\\s*(?:id|name|config):");

    /// Captures the id of a row, with or without a list marker.
    private static final Pattern ID_LINE = Pattern.compile("^\\s*-?\\s*id:\\s*['\"]?([^'\"\\s]+)");

    /// Captures a top-level disable row.
    private static final Pattern DISABLE_ROW = Pattern.compile("^- id: ([A-Za-z0-9_.-]+)\\s*$");

    /// Matches the disabled flag that follows a disable row.
    private static final Pattern DISABLED_NEXT = Pattern.compile("^ {2}disabled: true\\s*$");

    /// Matches a whole `- id: X` + `disabled: true` block for one id.
    private static Pattern disableBlock(String id) {
        return Pattern.compile("(?m)^- id: ['\"]?" + Pattern.quote(id) + "['\"]?\\r?\\n  disabled: true\\r?\\n");
    }

    /// Returns the profile's directory inside a home.
    ///
    /// @param home    the `DSH_HOME`
    /// @param profile the profile's name
    /// @return the directory
    public static Path profileDirectory(Path home, String profile) {
        return home.resolve("profiles").resolve(profile);
    }

    /// Returns the file a toggle writes.
    ///
    /// @param home    the `DSH_HOME`
    /// @param profile the profile's name
    /// @return the profile's own patch layer
    public static Path patchFile(Path home, String profile) {
        return profileDirectory(home, profile).resolve(PATCH_NAME);
    }

    /// Returns the loader entry ids a package brings into the tree.
    ///
    /// Both the patch the package declares through `dsh.bundle.patch` and the
    /// conventional `cordis.patch.yml` at its root are read, because the loader
    /// probes both. Rows the package only reconfigures are deliberately left out.
    ///
    /// @param home        the `DSH_HOME`
    /// @param profile     the profile's name
    /// @param packageName the package, as it is named in the profile manifest
    /// @return the ids, or an empty list when the package inserts none
    public static List<String> insertedIds(Path home, String profile, String packageName) {
        Path directory = profileDirectory(home, profile).resolve("node_modules").resolve(packageName);
        Set<String> ids = new LinkedHashSet<>();

        Path declared = declaredPatch(directory);
        if (declared != null) {
            addInsertedIds(ids, readIfFile(declared));
        }
        Path conventional = directory.resolve(CONVENTIONAL_PATCH);
        if (declared == null || !declared.equals(conventional)) {
            addInsertedIds(ids, readIfFile(conventional));
        }
        return List.copyOf(ids);
    }

    /// Returns the entry ids the profile's own patch layer disables.
    ///
    /// @param home    the `DSH_HOME`
    /// @param profile the profile's name
    /// @return the disabled ids, or an empty set when there is no patch file
    /// @throws DshException when the patch file cannot be read
    public static Set<String> disabledIds(Path home, String profile) throws DshException {
        return disabledIdsIn(read(patchFile(home, profile)));
    }

    /// Switches every row a package inserts on or off.
    ///
    /// Disabling appends a `disabled: true` block per row; enabling removes the
    /// block again, so a row the package itself holds down with an expression
    /// (an aggregate guard, for instance) is left exactly as the package wrote
    /// it.
    ///
    /// @param home        the `DSH_HOME`
    /// @param profile     the profile's name
    /// @param packageName the package to switch
    /// @param enabled     whether it should run
    /// @throws DshException when the package has no rows, or the patch file cannot be written
    public static void setEnabled(Path home, String profile, String packageName, boolean enabled) throws DshException {
        List<String> ids = insertedIds(home, profile, packageName);
        if (ids.isEmpty()) {
            throw new DshException("This plugin declares no loader rows that can be switched");
        }

        Path file = patchFile(home, profile);
        String text = read(file);
        for (String id : ids) {
            if (!ROW_ID.matcher(id).matches()) {
                throw new DshException("Loader row " + id + " cannot be written to the patch layer");
            }
            text = enabled ? removeDisable(text, id) : appendDisable(text, id);
        }
        write(file, text);
    }

    /// Returns the rows a package declares through `dsh.bundle.patch`.
    ///
    /// @param directory the package directory
    /// @return the declared patch file, or `null` when the manifest names none
    private static @Nullable Path declaredPatch(Path directory) {
        try {
            JsonObject manifest = JsonParser.parseString(
                    Files.readString(directory.resolve("package.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!manifest.has("dsh") || !manifest.get("dsh").isJsonObject()) {
                return null;
            }
            JsonObject bundle = manifest.getAsJsonObject("dsh").getAsJsonObject("bundle");
            if (bundle == null || !bundle.has("patch") || !bundle.get("patch").isJsonPrimitive()) {
                return null;
            }
            String declared = bundle.get("patch").getAsString();
            return declared == null || declared.isBlank() ? null : directory.resolve(declared).normalize();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /// Adds the ids one patch file inserts to a set.
    ///
    /// @param ids  the set to add to
    /// @param text the patch text, or `null` when there is no file
    private static void addInsertedIds(Set<String> ids, @Nullable String text) {
        if (text != null) {
            ids.addAll(insertedIdsIn(text));
        }
    }

    /// Reads a patch file, tolerating absence.
    ///
    /// @param file the file
    /// @return the text, or `null` when it is not a readable regular file
    private static @Nullable String readIfFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warning("Could not read " + file, e);
            return null;
        }
    }

    /// Reads the ids nested under `insert:` blocks.
    ///
    /// Line-wise rather than by parsing YAML, because a bundle patch may carry
    /// expressions and structures the loader accepts and a strict parser would
    /// not; a row at or above the `insert:` indentation closes the block, which
    /// is what separates a package's own rows from the neighbours it tunes.
    ///
    /// @param text the patch text
    /// @return the inserted ids, in order and without duplicates
    static List<String> insertedIdsIn(String text) {
        List<String> inserted = new ArrayList<>();
        @Nullable Integer insertIndent = null;
        for (String raw : text.split("\\r?\\n", -1)) {
            String line = raw.replaceFirst("#.*$", "");
            if (line.trim().isEmpty()) {
                continue;
            }
            int indent = line.length() - line.stripLeading().length();
            if (insertIndent != null && indent <= insertIndent && !ROW_KEY.matcher(line).find()) {
                insertIndent = null;
            }
            if (INSERT_LINE.matcher(line).find()) {
                insertIndent = indent;
                continue;
            }
            Matcher id = ID_LINE.matcher(line);
            if (!id.find()) {
                continue;
            }
            if (insertIndent != null && indent > insertIndent) {
                if (!inserted.contains(id.group(1))) {
                    inserted.add(id.group(1));
                }
            } else {
                insertIndent = null;
            }
        }
        return inserted;
    }

    /// Reads the ids a patch layer disables.
    ///
    /// @param text the patch text
    /// @return the ids carrying a `disabled: true` flag
    static Set<String> disabledIdsIn(String text) {
        Set<String> disabled = new LinkedHashSet<>();
        String[] lines = text.split("\\r?\\n", -1);
        boolean inInsert = false;
        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (line.matches("^- insert:\\s*$")) {
                inInsert = true;
                continue;
            }
            if (line.startsWith("- ")) {
                inInsert = false;
            }
            if (inInsert) {
                continue;
            }
            Matcher row = DISABLE_ROW.matcher(line);
            if (!row.find()) {
                continue;
            }
            String next = index + 1 < lines.length ? lines[index + 1] : "";
            if (DISABLED_NEXT.matcher(next).matches()) {
                disabled.add(row.group(1));
            }
        }
        return disabled;
    }

    /// Appends a disable block unless the id is disabled already.
    ///
    /// @param text the patch text
    /// @param id   the row id
    /// @return the updated text
    /// @throws DshException when the file cannot take another entry
    private static String appendDisable(String text, String id) throws DshException {
        if (disabledIdsIn(text).contains(id)) {
            return text;
        }
        return appendEntry(text, "- id: " + id + "\n  disabled: true\n");
    }

    /// Removes the disable block for an id, restoring the empty-list placeholder.
    ///
    /// @param text the patch text
    /// @param id   the row id
    /// @return the updated text
    private static String removeDisable(String text, String id) {
        String without = disableBlock(id).matcher(text).replaceFirst("");
        return without.equals(text) ? text : withPlaceholderRestored(without);
    }

    /// Appends one top-level entry, keeping the file a single YAML array.
    ///
    /// The profile template ships an empty `[]` placeholder, and appending after
    /// it would make two top-level elements in one document — the exact YAML
    /// error a hand-edit runs into. The placeholder is commented out instead,
    /// and [withPlaceholderRestored] puts it back when the last entry goes.
    ///
    /// Shared with [DshProfilePatch], which appends a row for an entry the file
    /// does not mention yet: the placeholder is the same trap for both.
    ///
    /// @param text  the patch text
    /// @param block the entry to append
    /// @return the updated text
    /// @throws DshException when the file is not an entry list
    static String appendEntry(String text, String block) throws DshException {
        if (text.trim().isEmpty()) {
            return block;
        }
        String withoutComments = text.replaceAll("(?m)^[ \\t]*#.*$", "").trim();
        if (withoutComments.isEmpty()) {
            return endWithNewline(text) + block;
        }
        if (withoutComments.equals("[]") || withoutComments.equals("[ ]")) {
            String commented = text.replaceFirst("(?m)^[ \\t]*\\[[ \\t]*\\][ \\t]*(?:#.*)?(?:\\r?\\n|$)", "# []\n");
            return endWithNewline(commented) + block;
        }
        String lastContent = "";
        for (String line : text.split("\\r?\\n", -1)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                lastContent = trimmed;
            }
        }
        if (lastContent.startsWith("[") || lastContent.startsWith("{")) {
            throw new DshException("The patch layer ends in a top-level flow structure; refusing to append");
        }
        return endWithNewline(text) + block;
    }

    /// Puts the empty-list placeholder back when nothing else is left.
    ///
    /// Appending the first entry comments the template's `[]` out, so removing
    /// the last one leaves a file of pure comments — not a top-level array, and
    /// a profile the loader refuses to boot. Switching a plugin off and on again
    /// would otherwise brick the profile.
    ///
    /// @param text the patch text
    /// @return the text, with a placeholder when it holds no entries
    static String withPlaceholderRestored(String text) {
        if (!text.replaceAll("(?m)^[ \\t]*#.*$", "").trim().isEmpty()) {
            return text;
        }
        String uncommented = text.replaceFirst("(?m)^[ \\t]*#[ \\t]*\\[[ \\t]*\\][ \\t]*(?:\\r?\\n|$)", "[]\n");
        if (!uncommented.equals(text)) {
            return uncommented;
        }
        return endWithNewline(text) + "[]\n";
    }

    /// Returns text that ends in exactly one newline.
    ///
    /// @param text the text
    /// @return the text with a trailing newline
    private static String endWithNewline(String text) {
        return text.endsWith("\n") ? text : text + "\n";
    }

    /// Reads a patch file, treating absence as an empty file.
    ///
    /// @param file the file
    /// @return the text, or an empty string
    /// @throws DshException when the file exists but cannot be read
    private static String read(Path file) throws DshException {
        if (!Files.isRegularFile(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DshException("Could not read " + file, e);
        }
    }

    /// Writes a file the harness may be reading, through a staging file.
    ///
    /// @param file the file
    /// @param text what to put in it
    /// @throws DshException when it cannot be written
    private static void write(Path file, String text) throws DshException {
        try {
            Files.createDirectories(file.getParent());
            Path staging = file.resolveSibling(file.getFileName() + ".hdsl-toggling");
            Files.writeString(staging, text, StandardCharsets.UTF_8);
            try {
                Files.move(staging, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(staging, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Privileges.handWrittenFile(file, DshPaths.ROOT);
        } catch (IOException e) {
            throw new DshException("Could not write " + file, e);
        }
    }
}
