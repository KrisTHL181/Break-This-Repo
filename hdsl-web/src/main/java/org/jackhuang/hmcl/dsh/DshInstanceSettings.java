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
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// The settings that belong to one instance rather than to the launcher.
///
/// The original keeps a game's own settings beside the game, with each one either
/// following the launcher's value or overriding it, and this is the same idea for an
/// instance: what is stored here is only what somebody chose *for this instance*, and
/// anything absent follows the launcher. Keeping it in the instance's own directory
/// rather than in a table keyed by id means it travels with the instance when the
/// directory is moved, which is what a per-instance setting should do.
@NotNullByDefault
public final class DshInstanceSettings {
    /// The file's name inside an instance directory.
    private static final String FILE = "settings.json";

    private DshInstanceSettings() {
    }

    /// Reads the policy this instance chose, if it chose one.
    ///
    /// @param instance the instance
    /// @return the stored value, or `null` when it follows the launcher
    public static @Nullable String buildScriptPolicy(DshInstance instance) {
        JsonObject root = read(instance);
        if (root == null || !root.has("buildScriptPolicy")
                || !root.get("buildScriptPolicy").isJsonPrimitive()) {
            return null;
        }
        try {
            return root.get("buildScriptPolicy").getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Records the policy this instance chose.
    ///
    /// @param instance the instance
    /// @param policy   the stored value, or `null` to follow the launcher
    /// @throws DshException when the file cannot be written
    public static void setBuildScriptPolicy(DshInstance instance, @Nullable String policy)
            throws DshException {
        write(instance, "buildScriptPolicy", policy == null ? null : new com.google.gson.JsonPrimitive(policy));
    }

    /// Reads the command that runs before this instance starts.
    ///
    /// @param instance the instance
    /// @return its own command, or `null` to follow the launcher
    public static @Nullable String preLaunchCommand(DshInstance instance) {
        return stringOf(instance, "preLaunchCommand");
    }

    /// Records the command that runs before this instance starts.
    ///
    /// @param instance the instance
    /// @param command  the command, or `null` to follow the launcher
    /// @throws DshException when the file cannot be written
    public static void setPreLaunchCommand(DshInstance instance, @Nullable String command) throws DshException {
        write(instance, "preLaunchCommand", command == null ? null : new com.google.gson.JsonPrimitive(command));
    }

    /// Reads the command that runs after this instance has ended.
    ///
    /// @param instance the instance
    /// @return its own command, or `null` to follow the launcher
    public static @Nullable String postExitCommand(DshInstance instance) {
        return stringOf(instance, "postExitCommand");
    }

    /// Records the command that runs after this instance has ended.
    ///
    /// @param instance the instance
    /// @param command  the command, or `null` to follow the launcher
    /// @throws DshException when the file cannot be written
    public static void setPostExitCommand(DshInstance instance, @Nullable String command) throws DshException {
        write(instance, "postExitCommand", command == null ? null : new com.google.gson.JsonPrimitive(command));
    }

    /// Reads the port the launcher gave this instance, which is what "automatic" means.
    ///
    /// Kept apart from the port in the instance's own record because the two are different facts. The
    /// record holds the port **in effect**, which a person who names one takes over; this is the one
    /// the launcher picked. It is the answer to "stop naming one": the instance goes back to the
    /// origin its browser state lives at, rather than to a number invented at that moment.
    ///
    /// @param instance the instance
    /// @return the port, or `0` when the launcher's choice was never written down
    public static int autoPort(DshInstance instance) {
        Integer port = intOf(instance, "autoPort");
        return port == null || port <= 0 ? 0 : port;
    }

    /// Writes down the port the launcher gave this instance.
    ///
    /// @param instance the instance
    /// @param port     the port, or `0` to forget it
    /// @throws DshException when the file cannot be written
    public static void setAutoPort(DshInstance instance, int port) throws DshException {
        write(instance, "autoPort", port <= 0 ? null : new com.google.gson.JsonPrimitive(port));
    }

    /// Reads an integer member.
    ///
    /// @param instance the instance
    /// @param name     the member
    /// @return the value, or `null` when it is absent or not a number
    private static @Nullable Integer intOf(DshInstance instance, String name) {
        JsonObject root = read(instance);
        if (root == null || !root.has(name) || !root.get(name).isJsonPrimitive()) {
            return null;
        }
        try {
            return root.get(name).getAsInt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Reads a string member.
    ///
    /// @param instance the instance
    /// @param name     the member
    /// @return the value, or `null` when it is absent or not a string
    private static @Nullable String stringOf(DshInstance instance, String name) {
        JsonObject root = read(instance);
        if (root == null || !root.has(name) || !root.get(name).isJsonPrimitive()) {
            return null;
        }
        try {
            return root.get(name).getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Reads whether this instance's log window opens when it launches.
    ///
    /// @param instance the instance
    /// @return the choice made for this instance, or `null` to follow the launcher
    public static @Nullable Boolean showLogs(DshInstance instance) {
        return booleanOf(instance, "showLogs");
    }

    /// Records whether this instance's log window opens when it launches.
    ///
    /// @param instance the instance
    /// @param value    the choice, or `null` to follow the launcher
    /// @throws DshException when the file cannot be written
    public static void setShowLogs(DshInstance instance, @Nullable Boolean value) throws DshException {
        write(instance, "showLogs", value == null ? null : new com.google.gson.JsonPrimitive(value));
    }

    /// Reads whether this instance writes the launcher's debug lines.
    ///
    /// @param instance the instance
    /// @return the choice made for this instance, or `null` to follow the launcher
    public static @Nullable Boolean debugLog(DshInstance instance) {
        return booleanOf(instance, "debugLog");
    }

    /// Records whether this instance writes the launcher's debug lines.
    ///
    /// @param instance the instance
    /// @param value    the choice, or `null` to follow the launcher
    /// @throws DshException when the file cannot be written
    public static void setDebugLog(DshInstance instance, @Nullable Boolean value) throws DshException {
        write(instance, "debugLog", value == null ? null : new com.google.gson.JsonPrimitive(value));
    }

    /// Reads which account this instance launches with.
    ///
    /// @param instance the instance
    /// @return the account's vendor id and label, or `null` to launch with no account
    public static @Nullable String accountKey(DshInstance instance) {
        return stringOf(instance, "accountKey");
    }

    /// Records which account this instance launches with.
    ///
    /// @param instance the instance
    /// @param key      the account's key, or `null` for none
    /// @throws DshException when the file cannot be written
    public static void setAccountKey(DshInstance instance, @Nullable String key) throws DshException {
        write(instance, "accountKey", key == null ? null : new com.google.gson.JsonPrimitive(key));
    }

    /// Reads whether this instance has an environment of its own.
    ///
    /// The instance file cannot say so: its `environment` member is never absent once read, because
    /// the record turns a missing one into an empty map so that no reader has to guard against a
    /// null. This is the same shape the other per-instance answers here have — a member that is
    /// there when somebody chose something and absent when they did not.
    ///
    /// @param instance the instance
    /// @return whether it has one
    public static boolean environmentIsOwn(DshInstance instance) {
        JsonObject root = read(instance);
        return root != null && root.has("environmentIsOwn")
                && root.get("environmentIsOwn").isJsonPrimitive()
                && root.get("environmentIsOwn").getAsBoolean();
    }

    /// Records whether this instance has an environment of its own.
    ///
    /// @param instance the instance
    /// @param value    whether it has one
    /// @throws DshException when the file cannot be written
    public static void setEnvironmentIsOwn(DshInstance instance, boolean value) throws DshException {
        write(instance, "environmentIsOwn", new com.google.gson.JsonPrimitive(value));
    }

    /// Reads what this instance does with the launcher while it runs.
    ///
    /// @param instance the instance
    /// @return the choice made for this instance, or `null` to follow the launcher
    public static @Nullable String launcherVisibility(DshInstance instance) {
        return stringOf(instance, "launcherVisibility");
    }

    /// Records what this instance does with the launcher while it runs.
    ///
    /// @param instance the instance
    /// @param value    the choice, or `null` to follow the launcher
    /// @throws DshException when the file cannot be written
    public static void setLauncherVisibility(DshInstance instance, @Nullable String value) throws DshException {
        write(instance, "launcherVisibility", value == null ? null : new com.google.gson.JsonPrimitive(value));
    }

    /// Reads a boolean member.
    ///
    /// @param instance the instance
    /// @param name     the member
    /// @return the value, or `null` when it is absent or not a boolean
    private static @Nullable Boolean booleanOf(DshInstance instance, String name) {
        JsonObject root = read(instance);
        if (root == null || !root.has(name) || !root.get(name).isJsonPrimitive()) {
            return null;
        }
        try {
            return root.get(name).getAsBoolean();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Reads whether install scripts may run without being asked about.
    ///
    /// @param instance the instance
    /// @return the choice made for this instance, or `null` to follow the launcher
    public static @Nullable Boolean approveBuildScripts(DshInstance instance) {
        JsonObject root = read(instance);
        if (root == null || !root.has("approveBuildScripts")
                || !root.get("approveBuildScripts").isJsonPrimitive()) {
            return null;
        }
        try {
            return root.get("approveBuildScripts").getAsBoolean();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /// Records whether install scripts may run without being asked about.
    ///
    /// @param instance the instance
    /// @param value    the choice, or `null` to follow the launcher again
    /// @throws DshException when the file cannot be written
    public static void setApproveBuildScripts(DshInstance instance, @Nullable Boolean value)
            throws DshException {
        write(instance, "approveBuildScripts", value == null ? null : new com.google.gson.JsonPrimitive(value));
    }

    /// Writes one member of the instance's settings.
    ///
    /// @param instance the instance
    /// @param name     the member
    /// @param value    the value, or `null` to remove it
    /// @throws DshException when the file cannot be written
    private static void write(DshInstance instance, String name,
                              @Nullable com.google.gson.JsonElement value) throws DshException {
        Path file = fileOf(instance);
        JsonObject root = read(instance);
        if (root == null) {
            root = new JsonObject();
        }
        if (value == null) {
            root.remove(name);
        } else {
            root.add(name, value);
        }

        Path staging = file.resolveSibling(FILE + ".hdsl");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(staging, JsonUtils.GSON.toJson(root));
            Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new DshException("Failed to write " + file, e);
        }
    }

    /// Reads the instance's settings file.
    ///
    /// @param instance the instance
    /// @return the object, or `null` when there is none or it cannot be read
    private static @Nullable JsonObject read(DshInstance instance) {
        Path file = fileOf(instance);
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            JsonObject root = JsonUtils.fromJsonFile(file, JsonObject.class);
            return root;
        } catch (Exception e) {
            LOG.warning("Failed to read " + file, e);
            return null;
        }
    }

    /// Returns an instance's settings file.
    ///
    /// @param instance the instance
    /// @return the file
    private static Path fileOf(DshInstance instance) {
        try {
            return instance.instanceDirectory().resolve(FILE);
        } catch (DshException e) {
            // An instance whose directory cannot be resolved has nowhere to keep
            // settings; a path that will fail to be written is the honest answer.
            return Path.of(FILE);
        }
    }
}
