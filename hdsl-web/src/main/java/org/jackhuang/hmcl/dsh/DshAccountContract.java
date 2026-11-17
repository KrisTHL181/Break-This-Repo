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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// What the launcher tells plugins about the account an instance is running as.
///
/// The harness already has the entry point this uses, and it is the harness's own rather than
/// this launcher's invention: {@code @deepseek-ai/dsh-launch-environment} freezes a snapshot of
/// the launch environment in three layers with a fixed trust order — the environment this
/// process inherited, the invoking directory's {@code .env}, and the harness home's
/// {@code .env} — and any plugin can resolve a value out of it and ask which layer supplied it:
///
/// {@snippet :
/// launchEnvironmentOf(ctx).getFrom("HDSL_ACCOUNT_NAME", ["process", "user-env"])?.value
/// }
///
/// So a plugin that wants to show who this instance is running as does not need the launcher to
/// know anything about that plugin: it reads a name the launcher publishes, in a layer the
/// harness already ranks. Two layers here, for two different lifetimes:
///
/// - the **process environment** of the launch, which is the layer the harness trusts most and
///   which exists exactly as long as the run does;
/// - the **harness home's {@code .env}**, which outlives the launch, so a person who starts the
///   harness by hand — or from the terminal this launcher opens on an instance — gets the same
///   answers the launcher's own launches give.
///
/// Two rules make this safe to publish, and both come from the harness rather than from taste:
///
/// - **No credentials.** The key stays in the process environment alone, under its own name, and
///   never reaches a file. The contract is metadata for showing and routing, nothing else.
/// - **No {@code DSH_} names.** The bootstrap refuses that prefix, and every other bootstrap
///   name, from any discovered {@code .env} — and refuses it by throwing, so one careless line
///   here would stop the harness from starting at all. Everything this writes is
///   {@code HDSL_}, which the harness leaves alone.
@NotNullByDefault
public final class DshAccountContract {
    /// The key a plugin reads first: the contract's version, and its existence.
    ///
    /// A plugin that does not recognise the version it finds should ignore the whole set rather
    /// than guess at the fields. The name has no value of its own to offer a plugin, which is
    /// why it is not called a version.
    public static final String CONTRACT = "HDSL_ACCOUNT_CONTRACT";

    /// The version this launcher publishes.
    public static final String VERSION = "1";

    /// The account's name, which is what a plugin would show: the same name a launch uses for
    /// the account's route.
    public static final String ACCOUNT_NAME = "HDSL_ACCOUNT_NAME";

    /// The route the account is offered to the harness under. Equal to the name today; a plugin
    /// that looks the route up in the harness's configuration wants this one.
    public static final String ACCOUNT_ROUTE = "HDSL_ACCOUNT_ROUTE";

    /// The vendor's id, such as {@code deepseek}.
    public static final String ACCOUNT_VENDOR = "HDSL_ACCOUNT_VENDOR";

    /// Which kind of account it is: {@code official}, {@code third-party} or {@code offline}.
    public static final String ACCOUNT_KIND = "HDSL_ACCOUNT_KIND";

    /// The endpoint the account talks to, when it has one of its own.
    public static final String ACCOUNT_ENDPOINT = "HDSL_ACCOUNT_ENDPOINT";

    /// The model the harness is told to start on, when the account names one.
    public static final String ACCOUNT_MODEL = "HDSL_ACCOUNT_MODEL";

    /// Which face the account wears: default, steve, alex, or local for a picture of its own.
    public static final String ACCOUNT_SKIN = "HDSL_ACCOUNT_SKIN";

    /// The body the skin is drawn on: default for the wide one, slim for the narrow one.
    public static final String ACCOUNT_SKIN_MODEL = "HDSL_ACCOUNT_SKIN_MODEL";

    /// The picture itself, when the account has one: an absolute path to a PNG.
    public static final String ACCOUNT_SKIN_FILE = "HDSL_ACCOUNT_SKIN_FILE";

    /// The cape's picture, when the account has one: an absolute path to a PNG.
    public static final String ACCOUNT_CAPE_FILE = "HDSL_ACCOUNT_CAPE_FILE";

    /// The instance's id.
    public static final String INSTANCE_ID = "HDSL_INSTANCE_ID";

    /// The dsh version the instance is pinned to.
    public static final String INSTANCE_VERSION = "HDSL_INSTANCE_VERSION";

    /// The profile the instance boots.
    public static final String INSTANCE_PROFILE = "HDSL_INSTANCE_PROFILE";

    /// The directory the instance works in.
    public static final String INSTANCE_WORKSPACE = "HDSL_INSTANCE_WORKSPACE";

    /// The launcher's own version, so a plugin can say which launcher told it.
    public static final String LAUNCHER_VERSION = "HDSL_LAUNCHER_VERSION";

    /// The prefixes the harness refuses from any discovered {@code .env}, and refuses by
    /// throwing.
    private static final List<String> REFUSED_PREFIXES = List.of("DSH_", "XDG_", "DYLD_", "BASH_FUNC_");

    /// The names the harness refuses from any discovered {@code .env}, whatever their prefix.
    private static final Set<String> REFUSED_NAMES = Set.of(
            "PATH", "HOME", "USERPROFILE", "SHELL", "NODE_OPTIONS", "NODE_PATH",
            "NODE_EXTRA_CA_CERTS", "LD_PRELOAD", "LD_LIBRARY_PATH", "LD_AUDIT");

    /// The markers the block is written between, so a person reading the file can see whose it
    /// is and where it ends.
    private static final String START = "# >>> HDSL account contract: written by the launcher, rewritten on every launch";
    private static final String END = "# <<< HDSL account contract";

    private DshAccountContract() {
    }

    /// Builds the values of the contract.
    ///
    /// The instance's own fields are always there; the account's only when there is one, because
    /// a launch with no account is a real arrangement and a plugin has to be able to tell that
    /// from an account it cannot see.
    ///
    /// @param instance the instance being launched
    /// @param account  the account it launches with, or null
    /// @return the values, in the order they are written
    /// @throws DshException when the instance's paths cannot be resolved
    public static @Unmodifiable Map<String, String> values(DshInstance instance, @Nullable DshAccount account)
            throws DshException {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(CONTRACT, VERSION);
        values.put(LAUNCHER_VERSION, org.jackhuang.hmcl.Metadata.VERSION);
        values.put(INSTANCE_ID, instance.id());
        values.put(INSTANCE_VERSION, instance.version());
        values.put(INSTANCE_PROFILE, instance.profile());
        values.put(INSTANCE_WORKSPACE, instance.workspacePath().toString());

        if (account != null) {
            values.put(ACCOUNT_NAME, account.displayName());
            values.put(ACCOUNT_ROUTE, account.displayName());
            values.put(ACCOUNT_VENDOR, account.vendorId());
            values.put(ACCOUNT_KIND, kindOf(account));
            String endpoint = account.endpoint();
            if (endpoint != null && !endpoint.isBlank()) {
                values.put(ACCOUNT_ENDPOINT, endpoint.trim());
            }
            String model = account.modelOrDefault();
            if (!model.isBlank()) {
                values.put(ACCOUNT_MODEL, model);
            }

            // The skin, because a plugin that draws the person rather than a generic head needs
            // it. The picture is the launcher's own copy and not the file it was imported from:
            // that one may have moved or been deleted, and the face has not.
            org.jackhuang.hmcl.dsh.skin.DshSkinChoice worn = account.skinOrDefault();
            values.put(ACCOUNT_SKIN, skinName(worn.type()));
            values.put(ACCOUNT_SKIN_MODEL, worn.model().modelName);
            if (org.jackhuang.hmcl.dsh.skin.DshSkinFiles.isSet(account.key())) {
                values.put(ACCOUNT_SKIN_FILE,
                        org.jackhuang.hmcl.dsh.skin.DshSkinFiles.file(account.key()).toString());
            }
            if (Files.isRegularFile(org.jackhuang.hmcl.dsh.skin.DshSkinFiles.capeFile(account.key()))) {
                values.put(ACCOUNT_CAPE_FILE,
                        org.jackhuang.hmcl.dsh.skin.DshSkinFiles.capeFile(account.key()).toString());
            }
        }
        // Unmodifiable but ordered: the file is read by people as well as by the harness,
        // and a contract whose lines move around on every launch is one nobody can diff.
        return java.util.Collections.unmodifiableMap(values);
    }

    /// Returns a skin choice's name as the contract spells it.
    ///
    /// @param type the choice
    /// @return its name
    private static String skinName(org.jackhuang.hmcl.dsh.skin.DshSkinChoice.Type type) {
        return switch (type) {
            case DEFAULT -> "default";
            case STEVE -> "steve";
            case ALEX -> "alex";
            case LOCAL_FILE -> "local";
        };
    }

    /// Returns the kind of an account under the spelling the launcher's own settings use.
    ///
    /// @param account the account
    /// @return its kind
    private static String kindOf(DshAccount account) {
        return switch (account.kind()) {
            case OFFICIAL -> "official";
            case THIRD_PARTY -> "third-party";
            case OFFLINE -> "offline";
        };
    }

    /// Writes the contract into an instance's harness home.
    ///
    /// The home's own {@code .env} rather than the profile's or the launcher's: that file is a
    /// layer the harness reads on every boot, whoever started it, and it belongs to the
    /// instance — so a hand-started {@code dsh web} in that home answers the same questions a
    /// launcher's own launch does, and so does the terminal this launcher opens on it.
    ///
    /// The launcher's lines live between two markers and are rewritten from scratch every time;
    /// every other line is left exactly as it was found, because the rest of the file is the
    /// person's.
    ///
    /// @param instance the instance
    /// @param account  the account it launches with, or null
    /// @throws DshException when the file cannot be read or written
    public static void publish(DshInstance instance, @Nullable DshAccount account) throws DshException {
        Path file = fileOf(instance);
        String existing;
        try {
            existing = Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            throw new DshException("Failed to read " + file, e);
        }

        String updated = withBlock(existing, values(instance, account));
        Path staging = file.resolveSibling(".env" + STAGING);
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Written beside the file and moved onto it: the harness reads this file while it
            // boots, and a half-written one would be read rather than skipped.
            Files.writeString(staging, updated, StandardCharsets.UTF_8);
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

    /// Replaces the launcher's block in a file's text, keeping everything else.
    ///
    /// Package-private and pure so it can be tested against the files a person actually has:
    /// a commented-out line of their own, a name they set themselves, a file with no trailing
    /// newline.
    ///
    /// @param text   the file as it is
    /// @param values the contract's values
    /// @return the file as it should be
    static String withBlock(String text, Map<String, String> values) {
        StringBuilder kept = new StringBuilder();
        boolean inside = false;
        for (String line : text.split("\n", -1)) {
            if (line.equals(START)) {
                inside = true;
                continue;
            }
            if (inside) {
                if (line.equals(END)) {
                    inside = false;
                }
                continue;
            }
            kept.append(line).append('\n');
        }

        StringBuilder out = new StringBuilder(kept.toString().stripTrailing());
        if (out.length() > 0) {
            out.append("\n\n");
        }
        out.append(START).append('\n');
        for (Map.Entry<String, String> value : values.entrySet()) {
            if (refused(value.getKey())) {
                // The harness refuses these from any .env by throwing, so one here would stop
                // the harness from starting at all. Nothing this class builds is refused; the
                // guard is here so that a name added later cannot make it so quietly.
                LOG.warning("Not writing " + value.getKey() + " to .env: the harness refuses it");
                continue;
            }
            out.append(value.getKey()).append('=').append(quoted(value.getValue())).append('\n');
        }
        out.append(END).append('\n');
        return out.toString();
    }

    /// Reports whether the harness refuses a name from a discovered {@code .env}.
    ///
    /// @param name the variable name
    /// @return whether it is bootstrap-only
    static boolean refused(String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        return REFUSED_NAMES.contains(upper)
                || REFUSED_PREFIXES.stream().anyMatch(upper::startsWith);
    }

    /// Quotes a value the way an {@code .env} reader expects, when it needs quoting.
    ///
    /// @param value the value
    /// @return the value, bare when it is unambiguous and in double quotes otherwise
    static String quoted(String value) {
        boolean bare = !value.isEmpty();
        for (int i = 0; bare && i < value.length(); i++) {
            bare = SAFE.indexOf(value.charAt(i)) >= 0;
        }
        if (bare) {
            return value;
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    /// Returns the file the contract is written to.
    ///
    /// @param instance the instance
    /// @return the harness home's {@code .env}
    /// @throws DshException when the home cannot be resolved
    static Path fileOf(DshInstance instance) throws DshException {
        return instance.homeDirectory().resolve(FILE);
    }

    /// What the file is called, and what the harness looks for.
    private static final String FILE = ".env";

    /// The suffix a rewrite is staged under before it replaces the file.
    private static final String STAGING = ".hdsl-contract";

    /// The characters a value may be written with bare.
    private static final String SAFE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789_./:@+-";
}
