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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jackhuang.hmcl.util.platform.Privileges;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// What a launch put into a home's own `settings.yaml`, and what was there before it.
///
/// An account's route is written into the profile's patch layer, and a patch says what the harness is
/// offered, not what it uses. The harness keeps its own answers in `settings.yaml` — the default
/// model it was told to use, and the route's own settings, which the harness writes when the person
/// edits that supplier — and those answers outlive the process. So a launch with a key leaves two
/// things behind in a file that is not the launcher's:
///
/// - `agent-default-model.provider` naming the route the launcher built, which the launcher wrote
///   itself for a supplier of the person's own;
/// - `llm-pi-ai.providers.<route>`, which the launcher puts there only so the harness's models page
///   has something to draw that route under, and which the harness then keeps — models whose route
///   may belong to an account that is no longer launched.
///
/// Left alone, the next launch with no account starts on that route instead of on the harness's own
/// supplier and its own question for a key. Which is why a launch **stashes** what it is about
/// to disturb, and why both the end of that launch and the beginning of the next one put it back.
///
/// **Putting back means restoring, not deleting.** Whatever was there before the launch is what
/// returns; content that was not there and is now is taken away. A value the person changed while
/// the harness was up is theirs and is left exactly as they left it — only the two shapes above are
/// touched at all, and the default model only while it still names the route this launch built.
@NotNullByDefault
public final class DshInjectedSettings {
    private DshInjectedSettings() {
    }

    /// The harness's own settings, in the home it is given.
    private static final String FILE = "settings.yaml";

    /// The section holding the model the harness starts on.
    private static final String DEFAULT_MODEL = "agent-default-model";

    /// The note recording what a launch disturbed, written beside the settings it is about.
    private static final String NOTE = ".hdsl-injected.json";

    /// Where the ledger of routes this home has had built for it is written.
    private static final String LEDGER = ".hdsl-injected-routes.json";

    /// Every route this home has had built for it, kept long after the launch that built it.
    ///
    /// A note answers "what did the launch that is over disturb?" and is gone once it has been
    /// answered. That leaves the launcher unable to recognise its own work the moment a note is
    /// missing — a launch killed before it wrote one, a home carried over from before there were
    /// notes at all — and a route it cannot recognise is a route it will not take away. This is the
    /// part that does not go: a name per line, no secrets, and the answer to "did I make this?".

    /// The section the harness keeps suppliers in, and the key under it.
    private static final String PROVIDERS = "llm-pi-ai";
    private static final String PROVIDERS_KEY = "providers";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /// The ledger's own shape: one list, so a person reading it sees names and nothing else.
    private record Ledger(List<String> routes) {
    }

    /// What was in a home's settings before a launch, kept until that launch is over.
    ///
    /// @param instance      the instance the launch was for
    /// @param route         the supplier route the launch built
    /// @param account       the account's key, or `null`
    /// @param defaultModel  the `agent-default-model` block as it was, or `null` when there was none
    /// @param provider      the route's block under `providers` as it was, or `null` when there was none
    /// @param patchProfile  the profile whose patch layer the route was written into, or `null` for a
    ///                      note from a version that wrote no profile patch
    /// @param writtenAt     when the launch started, for a person reading the file
    public record Stash(String instance, String route, @Nullable String account,
                        @Nullable String defaultModel, @Nullable String provider,
                        @Nullable String patchProfile, String writtenAt) {
    }

    /// Records what a launch is about to disturb, before it disturbs it.
    ///
    /// @param instance     the instance being launched
    /// @param route        the supplier route this launch builds
    /// @param account      the account's key, or `null`
    /// @param patchProfile the profile whose patch layer this launch writes the route into
    /// @throws DshException when the note cannot be written
    public static void capture(DshInstance instance, String route, @Nullable String account,
                               String patchProfile) throws DshException {
        Path settings = settingsOf(instance);
        String text = read(settings);
        Stash stash = new Stash(instance.id(), route, account,
                sectionBlock(text, DEFAULT_MODEL),
                routeBlock(text, PROVIDERS, PROVIDERS_KEY, route),
                patchProfile,
                Instant.now().toString());
        write(noteOf(instance), GSON.toJson(stash));

        java.util.LinkedHashSet<String> routes = new java.util.LinkedHashSet<>(routesOf(instance));
        if (routes.add(route)) {
            write(ledgerOf(instance), GSON.toJson(new Ledger(List.copyOf(routes))));
        }
    }

    /// Takes away the routes this launcher made, and a default model that names one of them.
    ///
    /// Run at the start of every launch, before that launch decides what to make of its own, and
    /// **without asking**: these are the launcher's own doings, not the person's, and a route that is
    /// left is a route the harness will offer with no address and no key behind it — which is how a
    /// launch that wanted no supplier came to start on the last one's.
    ///
    /// Two things say what is the launcher's:
    ///
    /// - the ledger, which is what it has actually built for this home;
    /// - the names of the accounts that carry a key, because those are the names it builds routes
    ///   under, and because a home may hold work from before there was a ledger.
    ///
    /// A default model naming one of those routes is taken away too — unless it names the route this
    /// very launch is about to build, which is a pointer that will be good again in a moment. A model
    /// the person chose for a supplier of their own is not in either set and is never touched.
    ///
    /// The **profile patch** is cleaned first and by the ledger alone, because a route there is only
    /// ever written by the launcher under a name the ledger holds: a supplier the person made
    /// themselves is never in it, whatever they called it. This is also what takes back a route an
    /// earlier launch left behind — a home whose harness was killed, or one that ran a version whose
    /// routes stayed in the file.
    ///
    /// @param instance      the instance about to be launched
    /// @param patchProfile  the profile whose patch layer this launch boots
    /// @param accountRoutes the routes the launcher's accounts are named after
    /// @param injecting     the route this launch will build, or `null` when it builds none
    /// @return whether anything was changed
    /// @throws DshException when a file cannot be read or written
    public static boolean clean(DshInstance instance, String patchProfile,
                                java.util.Collection<String> accountRoutes, @Nullable String injecting)
            throws DshException {
        boolean changed = false;
        for (String route : routesOf(instance)) {
            if (!route.equals(injecting)) {
                changed |= takeRouteBack(instance, patchProfile, route);
            }
        }

        java.util.LinkedHashSet<String> ours = new java.util.LinkedHashSet<>(routesOf(instance));
        ours.addAll(accountRoutes);
        if (injecting != null) {
            // The route this launch is about to build is the launcher's by definition, ledger or no
            // ledger: anything already sitting under that name is this launcher's earlier work.
            ours.add(injecting);
        }
        ours.removeIf(route -> route == null || route.isBlank());
        Path settings = settingsOf(instance);
        String text = read(settings);
        if (ours.isEmpty() || text.isEmpty()) {
            return changed;
        }
        String updated = text;
        for (String route : ours) {
            updated = withRouteBlock(updated, PROVIDERS, PROVIDERS_KEY, route, null);
        }
        String current = sectionBlock(updated, DEFAULT_MODEL);
        String named = current == null ? null : scalarOf(current, "provider");
        if (named != null && ours.contains(named) && !named.equals(injecting)) {
            updated = withSectionBlock(updated, DEFAULT_MODEL, null);
        }
        if (!updated.equals(text)) {
            write(settings, updated);
            changed = true;
        }
        return changed;
    }

    /// Takes one of this launcher's routes back out of a profile's own patch layer.
    ///
    /// @param instance the instance
    /// @param profile  the profile whose patch layer it is
    /// @param route    the route
    /// @return whether the file was changed
    /// @throws DshException when the file cannot be read or written
    private static boolean takeRouteBack(DshInstance instance, String profile, String route)
            throws DshException {
        return DshAccountRoute.remove(instance.homeDirectory(), profile, route);
    }

    /// Returns the routes this home has had built for it.
    ///
    /// @param instance the instance
    /// @return the routes, in the order they were first built
    /// @throws DshException when the home cannot be resolved
    private static List<String> routesOf(DshInstance instance) throws DshException {
        Path ledger = ledgerOf(instance);
        if (!Files.isRegularFile(ledger)) {
            return List.of();
        }
        try {
            Ledger read = GSON.fromJson(Files.readString(ledger, StandardCharsets.UTF_8), Ledger.class);
            return read == null || read.routes() == null ? List.of() : read.routes();
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read " + ledger, e);
            return List.of();
        }
    }

    private static Path ledgerOf(DshInstance instance) throws DshException {
        return settingsOf(instance).resolveSibling(LEDGER);
    }

    /// Makes a route visible to the harness's own configuration surfaces, for as long as it runs.
    ///
    /// The models page lists what the **settings** layer configures, and draws a group per settings
    /// path. A route the patch layer declares has no settings document of its own until one is
    /// written, and a route with no settings path is not rendered at all — it is live and usable and
    /// appears nowhere on that page. Writing the profile object is what puts it in the list,
    /// editable, for as long as the launch lasts; the note this launch has already written is what
    /// takes it away again.
    ///
    /// **Only the profile object and the credential reference.** What the route *is* — protocol,
    /// address, models — is the profile patch's to say, and repeating it here would be a second copy
    /// of the same fact, which is one copy more than the cleanup can keep straight.
    ///
    /// @param instance  the instance being launched
    /// @param route     the route this launch built
    /// @param apiKeyEnv the environment variable its key travels in
    /// @return whether the settings were changed
    /// @throws DshException when the settings cannot be read or written
    public static boolean publish(DshInstance instance, String route, String apiKeyEnv)
            throws DshException {
        Path settings = settingsOf(instance);
        String text = read(settings);
        String block = YamlScalar.of(route) + ":\n  apiKeyEnv: " + YamlScalar.of(apiKeyEnv) + "\n";
        String updated = withRouteBlock(text, PROVIDERS, PROVIDERS_KEY, route, block);
        if (updated.equals(text)) {
            return false;
        }
        write(settings, updated);
        return true;
    }

    /// Puts back what a launch disturbed, and forgets it did.
    ///
    /// Called before a launch — a note left behind is a launch that was killed before it could tidy
    /// up — and again when one ends. Both callers do the same thing, so an instance that was killed
    /// outright is cleaned by the next launch of any kind, **including one with no account at all**.
    ///
    /// Doing nothing is the normal answer: most launches leave no note because most launches have
    /// nothing to put back.
    ///
    /// @param instance the instance about to be, or no longer being, launched
    /// @return whether the settings were changed
    /// @throws DshException when the settings cannot be read or written
    public static boolean settle(DshInstance instance) throws DshException {
        Path note = noteOf(instance);
        if (!Files.isRegularFile(note)) {
            return false;
        }
        Stash stash = readStash(note);
        if (stash == null) {
            // A note that cannot be read is a note that cannot be honoured. Leaving it would fail
            // the launch it is checked by for ever, so it goes.
            delete(note);
            return false;
        }

        // The route goes first, and whatever the home's own settings look like. It belongs to the
        // launch that wrote it — its key travelled in a variable that launch set — so a launch is
        // over, so it goes. This cannot be skipped by an empty `settings.yaml`: the harness *imports*
        // that file into the profile patch when it boots (see `publish`), so by the time a launch
        // ends there is usually no settings file left to read.
        boolean changed = stash.patchProfile() != null
                && takeRouteBack(instance, stash.patchProfile(), stash.route());

        Path settings = settingsOf(instance);
        String text = read(settings);
        if (text.isEmpty()) {
            delete(note);
            return changed;
        }
        String updated = text;

        // The route was not there before the launch and is there now: it appeared under this
        // launcher's watch, so it goes. A route that **was** there is left exactly as it is — the
        // launcher never wrote that block, so what is in it is the harness's or the person's, and
        // putting an old copy back could only lose an edit made since.
        if (stash.provider() == null) {
            updated = withRouteBlock(updated, PROVIDERS, PROVIDERS_KEY, stash.route(), null);
        }

        // The default model is the launcher's own writing, and only while it still names the route
        // this launcher built: one the person has since pointed somewhere else is their answer.
        String current = sectionBlock(updated, DEFAULT_MODEL);
        if (current != null && Objects.equals(scalarOf(current, "provider"), stash.route())) {
            updated = withSectionBlock(updated, DEFAULT_MODEL, stash.defaultModel());
        }

        if (!updated.equals(text)) {
            write(settings, updated);
            changed = true;
        }
        delete(note);
        return changed;
    }

    /// Returns where a home's note lives: beside the file it describes.
    ///
    /// Not in the launcher's own directory, because what a note is about is **this home's** settings
    /// — a home that is moved, copied or deleted takes its settings and its note together, and a
    /// home that never had anything injected never grows one.
    ///
    /// @param instance the instance
    /// @return the file, which may not exist
    private static Path noteOf(DshInstance instance) throws DshException {
        return settingsOf(instance).resolveSibling(NOTE);
    }

    private static Path settingsOf(DshInstance instance) throws DshException {
        return instance.homeDirectory().resolve(FILE);
    }

    private static @Nullable Stash readStash(Path note) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(note, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            return new Stash(string(json, "instance"), string(json, "route"), nullableString(json, "account"),
                    nullableString(json, "defaultModel"), nullableString(json, "provider"),
                    nullableString(json, "patchProfile"),
                    string(json, "writtenAt"));
        } catch (IOException | RuntimeException e) {
            LOG.warning("Could not read " + note, e);
            return null;
        }
    }

    private static String string(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }

    private static @Nullable String nullableString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : null;
    }

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

    /// Writes a file the harness may be reading, the way the harness writes it: beside the file and
    /// then into place, so an interrupted write cannot leave the settings truncated.
    ///
    /// @param file the file to write
    /// @param text what to put in it
    /// @throws DshException when it cannot be written
    private static void write(Path file, String text) throws DshException {
        try {
            Files.createDirectories(file.getParent());
            Path staging = file.resolveSibling(file.getFileName() + ".hdsl-injecting");
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

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOG.info("Could not remove " + file, e);
        }
    }

    // ---- The file, edited a line at a time ------------------------------------------------------
    //
    // The harness reads this file and writes it itself while it runs, so it is never re-emitted from
    // a parse: every edit is a splice of whole lines, and everything not named here — comments,
    // order, formatting — survives untouched.

    /// Returns a top-level section's lines, or `null` when the file has none.
    ///
    /// @param text the file
    /// @param name the key, without its colon
    /// @return the block, ending in a newline
    static @Nullable String sectionBlock(String text, String name) {
        List<String> lines = lines(text);
        for (int i = 0; i < lines.size(); i++) {
            if (indentOf(lines.get(i)) == 0 && isKey(lines.get(i), name)) {
                return join(lines, i, endOfBlock(lines, i));
            }
        }
        return null;
    }

    /// Replaces a top-level section with the given lines, or takes it away.
    ///
    /// @param text  the file
    /// @param name  the key, without its colon
    /// @param block the lines to put in its place, or `null` to remove the section
    /// @return the file
    static String withSectionBlock(String text, String name, @Nullable String block) {
        List<String> lines = lines(text);
        for (int i = 0; i < lines.size(); i++) {
            if (indentOf(lines.get(i)) == 0 && isKey(lines.get(i), name)) {
                return splice(lines, i, endOfBlock(lines, i), block);
            }
        }
        if (block == null) {
            return text;
        }
        // Not there: a section that belongs at the end, after whatever the file already has.
        String base = text.isEmpty() || text.endsWith("\n") ? text : text + "\n";
        return base + block;
    }

    /// Returns a route's block under a nested key, or `null` when the file has none.
    ///
    /// @param text  the file
    /// @param owner the top-level key holding the suppliers
    /// @param key   the key under it holding the routes
    /// @param route the route's name
    /// @return the block, ending in a newline
    static @Nullable String routeBlock(String text, String owner, String key, String route) {
        List<String> lines = lines(text);
        int ownerAt = keyAt(lines, 0, lines.size(), 0, owner);
        if (ownerAt < 0) {
            return null;
        }
        int ownerEnd = endOfBlock(lines, ownerAt);
        int keyAt = nestedKeyAt(lines, ownerAt + 1, ownerEnd, indentOf(lines.get(ownerAt)), key);
        if (keyAt < 0) {
            return null;
        }
        int keyEnd = endOfBlock(lines, keyAt);
        int routeAt = nestedKeyAt(lines, keyAt + 1, keyEnd, indentOf(lines.get(keyAt)), route);
        return routeAt < 0 ? null : join(lines, routeAt, endOfBlock(lines, routeAt));
    }

    /// Replaces a route's block with the given lines, or takes it away.
    ///
    /// @param text  the file
    /// @param owner the top-level key holding the suppliers
    /// @param key   the key under it holding the routes
    /// @param route the route's name
    /// @param block the lines to put in its place, or `null` to remove the route
    /// @return the file
    static String withRouteBlock(String text, String owner, String key, String route,
                                 @Nullable String block) {
        List<String> lines = lines(text);
        int ownerAt = keyAt(lines, 0, lines.size(), 0, owner);
        if (ownerAt < 0) {
            if (block == null) {
                return text;
            }
            String base = text.isEmpty() || text.endsWith("\n") ? text : text + "\n";
            return base + owner + ":\n  " + key + ":\n" + indent(block, 4);
        }
        int ownerIndent = indentOf(lines.get(ownerAt));
        int ownerEnd = endOfBlock(lines, ownerAt);
        int keyAt = nestedKeyAt(lines, ownerAt + 1, ownerEnd, ownerIndent, key);
        if (keyAt < 0) {
            if (block == null) {
                return text;
            }
            String grown = " ".repeat(ownerIndent + 2) + key + ":\n" + indent(block, ownerIndent + 4);
            return String.join("\n", insertAfter(lines, ownerAt, grown));
        }
        int keyEnd = endOfBlock(lines, keyAt);
        int routeAt = nestedKeyAt(lines, keyAt + 1, keyEnd, indentOf(lines.get(keyAt)), route);
        if (routeAt < 0) {
            // Nothing to replace: a block with something in it is a profile being added, and a route
            // the file has never held goes in as the first child of the routes it belongs to.
            return block == null
                    ? text
                    : String.join("\n", insertAfter(lines, keyAt, indent(block, indentOf(lines.get(keyAt)) + 2)));
        }
        return splice(lines, routeAt, endOfBlock(lines, routeAt), block);
    }

    /// Returns the lines with a block inserted after one of them.
    ///
    /// @param lines the file's lines
    /// @param after the line to insert after
    /// @param block the block's lines
    /// @return the file's lines
    private static List<String> insertAfter(List<String> lines, int after, String block) {
        List<String> result = new ArrayList<>(lines.subList(0, after + 1));
        List<String> inserted = lines(block);
        if (!inserted.isEmpty() && inserted.get(inserted.size() - 1).isEmpty()) {
            inserted.remove(inserted.size() - 1);
        }
        result.addAll(inserted);
        result.addAll(lines.subList(after + 1, lines.size()));
        return result;
    }

    /// Returns a block's lines, each indented by the given number of spaces.
    ///
    /// @param block  the block
    /// @param spaces the indentation
    /// @return the indented block
    private static String indent(String block, int spaces) {
        String padding = " ".repeat(spaces);
        StringBuilder indented = new StringBuilder();
        for (String line : lines(block)) {
            indented.append(line.isBlank() ? line : padding + line).append('\n');
        }
        return indented.toString();
    }

    /// Returns the value of a `key: value` line inside a block, or `null`.
    ///
    /// @param block the block's lines
    /// @param name  the key
    /// @return the value, unquoted
    static @Nullable String scalarOf(String block, String name) {
        for (String line : lines(block)) {
            if (isKey(line.trim(), name)) {
                String value = line.trim().substring(name.length() + 1).trim();
                int comment = commentAt(value);
                if (comment >= 0) {
                    value = value.substring(0, comment).trim();
                }
                if (value.length() >= 2 && (value.charAt(0) == '"' || value.charAt(0) == '\'')
                        && value.charAt(value.length() - 1) == value.charAt(0)) {
                    value = value.substring(1, value.length() - 1);
                }
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    /// Finds a key's line inside a range, at exactly the indentation given.
    ///
    /// @param lines  the file's lines
    /// @param from   the first line to look at
    /// @param to     the line to stop before
    /// @param indent the indentation wanted
    /// @param name   the key, without its colon
    /// @return the line's index, or `-1`
    private static int keyAt(List<String> lines, int from, int to, int indent, String name) {
        for (int i = from; i < to && i < lines.size(); i++) {
            if (!lines.get(i).isBlank() && indentOf(lines.get(i)) == indent
                    && isKey(lines.get(i).trim(), name)) {
                return i;
            }
        }
        return -1;
    }

    /// Finds a key's line inside a range, at any indentation deeper than its parent's.
    ///
    /// The indentation is not known in advance — a `providers:` may be written at two spaces or at
    /// four — so what is looked for is a key deeper than the block that should hold it, which is
    /// what nesting means in this file.
    ///
    /// @param lines        the file's lines
    /// @param from         the first line to look at
    /// @param to           the line to stop before
    /// @param parentIndent the indentation of the block that should hold the key
    /// @param name         the key, without its colon
    /// @return the line's index, or `-1`
    private static int nestedKeyAt(List<String> lines, int from, int to, int parentIndent, String name) {
        for (int i = from; i < to && i < lines.size(); i++) {
            if (!lines.get(i).isBlank() && indentOf(lines.get(i)) > parentIndent
                    && isKey(lines.get(i).trim(), name)) {
                return i;
            }
        }
        return -1;
    }

    /// Reports whether a trimmed line opens the given key.
    ///
    /// `providers:` opens `providers`; `providers-x:` does not.
    ///
    /// @param trimmed the line, without its indentation
    /// @param name    the key
    /// @return whether it opens that key
    private static boolean isKey(String trimmed, String name) {
        if (!trimmed.startsWith(name + ":")) {
            return false;
        }
        return trimmed.length() == name.length() + 1
                || Character.isWhitespace(trimmed.charAt(name.length() + 1));
    }

    /// Returns where a comment starts in a value, or `-1`.
    ///
    /// @param value the text after a colon
    /// @return the index of the `#`, or `-1`
    private static int commentAt(String value) {
        char quote = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '#' && (i == 0 || Character.isWhitespace(value.charAt(i - 1)))) {
                return i;
            }
        }
        return -1;
    }

    /// Returns where a block ends: the first following line that is not blank and not indented
    /// further than the block's own opening line.
    ///
    /// @param lines the file's lines
    /// @param start the opening line
    /// @return the first line after the block
    private static int endOfBlock(List<String> lines, int start) {
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

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return i;
    }

    private static List<String> lines(String text) {
        return new ArrayList<>(List.of(text.split("\n", -1)));
    }

    private static String join(List<String> lines, int from, int to) {
        StringBuilder block = new StringBuilder();
        for (int i = from; i < to && i < lines.size(); i++) {
            block.append(lines.get(i)).append('\n');
        }
        return block.toString();
    }

    /// Replaces the lines in `[from, to)` with the given block's lines.
    ///
    /// @param lines the file's lines
    /// @param from  the first line to replace
    /// @param to    the line to stop before
    /// @param block the replacement, or `null` to remove the range
    /// @return the file
    private static String splice(List<String> lines, int from, int to, @Nullable String block) {
        List<String> result = new ArrayList<>(lines.subList(0, from));
        if (block != null) {
            result.addAll(lines(block));
            // A block always ends in a newline, which the split turns into a trailing empty line.
            if (!result.isEmpty() && result.get(result.size() - 1).isEmpty()) {
                result.remove(result.size() - 1);
            }
        }
        result.addAll(lines.subList(Math.min(to, lines.size()), lines.size()));
        // A file that ended in a newline still does. Removing a block that ran to the end of the file
        // takes the last, empty line with it, and a settings file that stops mid-line is not one the
        // harness should have to read.
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()
                && (result.isEmpty() || !result.get(result.size() - 1).isEmpty())) {
            result.add("");
        }
        return String.join("\n", result);
    }

}
