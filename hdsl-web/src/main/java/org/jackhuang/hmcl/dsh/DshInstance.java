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

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/// One launcher instance: a pinned DeepSeek Harness version plus the state it
/// operates on.
///
/// An instance is deliberately thin. It names an installed [DshVersion], a
/// profile inside that home, the working directory sessions are scoped to, and
/// how the `DSH_HOME` is resolved. Everything else — plugins, sessions,
/// credentials — belongs to the home directory and is therefore owned by
/// DeepSeek Harness rather than mirrored here.
///
/// Instances are immutable; [DshInstanceManager] replaces them on edit.
@NotNullByDefault
public record DshInstance(
        @SerializedName("id") String id,
        @SerializedName("version") String version,
        @SerializedName("profile") String profile,
        @SerializedName("workspace") String workspace,
        @SerializedName("nodeRuntime") @Nullable String nodeRuntime,
        @SerializedName("homeMode") DshHomeMode homeMode,
        @SerializedName("customHome") @Nullable String customHome,
        @SerializedName("arguments") @Unmodifiable List<String> extraArguments,
        // Empty means this instance adds nothing of its own, so what it runs with is the launcher's
        // set — which is what "follow the launcher" comes to, and is why the instance page's row
        // reads its own state from emptiness. A set of its own is laid over the launcher's rather
        // than replacing it, so an empty one and no set at all are the same thing here.
        @SerializedName("environment") @Unmodifiable Map<String, String> environment,
        @SerializedName("icon") @Nullable String icon,
        @SerializedName("iconFile") @Nullable String iconFile,
        @SerializedName("portMode") @Nullable DshPortMode portMode,
        @SerializedName("port") int port,
        @SerializedName("createdAt") long createdAt) {

    /// Normalises the members a settings file may leave out.
    ///
    /// The instance file is written by this launcher but not only read from files this launcher
    /// wrote, and Gson fills a record through its fields rather than through its constructor — so a
    /// member that is missing arrives here as `null` even though the record says it is never null.
    /// An instance whose `environment` was absent therefore used to reach the interface as a `null`
    /// map, and the first row that asked it whether it was empty brought the whole launcher down.
    /// Doing it in the constructor is what makes every reader safe at once: there are a dozen
    /// places that take an instance apart and rebuild it, and they all go through here.
    public DshInstance {
        environment = java.util.Map.copyOf(
                environment == null ? java.util.Map.of() : environment);
    }

    /// The profile booted when this instance is launched.
    public static final String DEFAULT_PROFILE = "web";

    /// Returns the icon selected for this instance.
    ///
    /// @return the icon, never `null`
    public DshInstanceIcon iconOrDefault() {
        return DshInstanceIcon.of(icon);
    }

    /// Returns the custom icon file, when the instance uses one.
    ///
    /// A file takes precedence over the built-in icon, because choosing one is
    /// the more specific act.
    ///
    /// @return the file, or `null` when a built-in icon is in use
    public @Nullable Path iconFileOrDefault() {
        return iconFile == null || iconFile.isBlank() ? null : Path.of(iconFile);
    }

    /// Returns a copy that uses a custom icon file.
    ///
    /// @param file the image file
    /// @return the copy
    public DshInstance withIconFile(Path file) {
        return new DshInstance(id, version, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment,
                DshInstanceIcon.DEFAULT.id(), file.toAbsolutePath().normalize().toString(),
                portMode, port, createdAt);
    }

    /// Returns a copy that uses a built-in icon.
    ///
    /// @return the copy
    public DshInstance withNoIconFile() {
        return new DshInstance(id, version, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment, icon, null, portMode, port, createdAt);
    }

    /// Returns a copy under a different id.
    ///
    /// @param newId the new id
    /// @return the copy
    public DshInstance withId(String newId) {
        return new DshInstance(newId, version, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment, icon, iconFile, portMode, port, createdAt);
    }

    /// Returns a copy with a different icon.
    ///
    /// @param newIcon the icon
    /// @return the copy
    public DshInstance withIcon(DshInstanceIcon newIcon) {
        return new DshInstance(id, version, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment, newIcon.id(), iconFile, portMode, port, createdAt);
    }

    /// Returns the port policy, defaulting to automatic.
    ///
    /// @return the port mode, never `null`
    public DshPortMode portModeOrDefault() {
        // An instance that follows the launcher says so, and what it follows is the
        // launcher's own policy — which is auto unless a person changed it.
        DshPortMode mode = portMode();
        return mode == null || mode == DshPortMode.GLOBAL ? DshPortMode.AUTO : mode;
    }

    /// Reports whether this instance chooses its own port policy.
    ///
    /// @return whether it does not follow the launcher
    public boolean hasOwnPortMode() {
        return portMode() != null && portMode() != DshPortMode.GLOBAL;
    }

    /// Returns the port to pass to DeepSeek Harness.
    ///
    /// For a fixed instance this is the port the user chose. For an automatic
    /// one it is the port the instance was first given, or `0` before that has
    /// happened.
    ///
    /// @return the port, or `0` when none has been settled on yet
    public int portOrDefault() {
        return Math.max(port, 0);
    }

    /// Returns a copy with a different port.
    ///
    /// @param newPort the port
    /// @return the copy
    public DshInstance withPort(int newPort) {
        return new DshInstance(id, version, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment, icon, iconFile, portMode, newPort, createdAt);
    }

    /// Returns a copy with a different port policy.
    ///
    /// @param newMode the policy
    /// @param newPort the fixed port, ignored when the policy is automatic
    /// @return the copy
    public DshInstance withPortPolicy(DshPortMode newMode, int newPort) {
        return new DshInstance(id, version, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment, icon, iconFile, newMode, newPort, createdAt);
    }

    /// Returns the Node runtime this instance runs on.
    ///
    /// `system` means whatever `node` is on `PATH`; anything else names a runtime
    /// installed under `runtimes/<version>/`.
    ///
    /// @return the runtime selection, never `null`
    public String nodeRuntimeOrDefault() {
        String value = nodeRuntime;
        // An instance that never chose follows the launcher. Existing manifests
        // carry null, which used to mean the system runtime and still resolves to
        // it, because the launcher's own default is the system runtime.
        if (value == null || value.isBlank() || DshNodeRuntime.GLOBAL.equals(value)) {
            return DshEnvironment.nodeRuntimeDefault();
        }
        return value;
    }

    /// Returns the working directory sessions are scoped to.
    ///
    /// Paths are stored as strings rather than as [Path] values: the manifest is
    /// a user-visible file, and Gson cannot serialise [Path] inside a record
    /// without a custom adapter.
    ///
    /// @return the absolute workspace path
    public Path workspacePath() {
        return Path.of(workspace).toAbsolutePath().normalize();
    }

    /// Returns the configured custom home.
    ///
    /// @return the absolute custom home, or `null` when none is configured
    public @Nullable Path customHomePath() {
        String value = customHome;
        return value == null ? null : Path.of(value).toAbsolutePath().normalize();
    }

    /// Resolves the `DSH_HOME` this instance must be launched with.
    ///
    /// @return the absolute home directory for this instance
    /// @throws DshException when a custom home was requested but not configured,
    ///                       or when a path segment is unusable
    public Path homeDirectory() throws DshException {
        // GLOBAL is resolved through the launcher's default rather than here,
        // because the settings live a layer above this package.
        if (homeMode == DshHomeMode.GLOBAL) {
            return withHome(DshEnvironment.homeModeDefault(), customHomePath()).homeDirectory();
        }
        return switch (homeMode) {
            case GLOBAL -> throw new AssertionError("handled above");
            case ISOLATED -> DshPaths.instanceDirectory(id).resolve("home").toAbsolutePath().normalize();
            case VERSION_SHARED -> DshPaths.versionHomeDirectory(version).toAbsolutePath().normalize();
            case CUSTOM -> {
                Path home = customHomePath();
                if (home == null) {
                    throw new DshException("Instance " + id + " is set to use a custom DSH_HOME but none is configured");
                }
                yield home.toAbsolutePath().normalize();
            }
        };
    }

    /// Returns this instance's directory, which holds `instance.json` and the
    /// isolated home when [#homeMode] is [DshHomeMode#ISOLATED].
    ///
    /// @return the instance directory
    /// @throws DshException when the id cannot be used as a directory name
    public Path instanceDirectory() throws DshException {
        return DshPaths.instanceDirectory(id);
    }

    /// Returns the directory holding this instance's own DeepSeek Harness.
    ///
    /// @return the directory
    /// @throws DshException when the identifier is not usable as a path segment
    public Path dshDirectory() throws DshException {
        return DshPaths.instanceVersionDirectory(id);
    }

    /// Returns the entry script of this instance's own DeepSeek Harness.
    ///
    /// @return the script's path
    /// @throws DshException when the identifier is not usable as a path segment
    public Path dshEntryPoint() throws DshException {
        return dshDirectory().resolve(DshVersion.PACKAGE_PATH).resolve("lib/bin.js");
    }

    /// Reports whether this instance shares its home with other instances.
    ///
    /// The user interface uses this to show a warning next to the instance.
    ///
    /// @return whether the home is not private to this instance
    public boolean sharesHome() {
        return homeMode != DshHomeMode.ISOLATED;
    }

    /// Creates a new instance with a different profile.
    ///
    /// @param newProfile the profile to boot
    /// @return the updated instance
    public DshInstance withProfile(String newProfile) {
        return new DshInstance(id, version, newProfile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment, icon, iconFile, portMode, port, createdAt);
    }

    /// Creates a new instance pinned to a different version.
    ///
    /// @param newVersion the version to pin
    /// @return the updated instance
    public DshInstance withVersion(String newVersion) {
        return new DshInstance(id, newVersion, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments, environment, icon, iconFile, portMode, port, createdAt);
    }

    /// Creates a new instance pinned to a different Node runtime.
    ///
    /// @param runtime the runtime selection, or `null` for the system runtime
    /// @return the updated instance
    public DshInstance withNodeRuntime(@Nullable String runtime) {
        return new DshInstance(id, version, profile, workspace, runtime, homeMode, customHome,
                extraArguments, environment, icon, iconFile, portMode, port, createdAt);
    }

    /// Creates a new instance with a different home policy.
    ///
    /// @param mode the new policy
    /// @param home the custom home, required when `mode` is [DshHomeMode#CUSTOM]
    /// @return the updated instance
    /// Returns a copy of this instance with a different environment.
    ///
    /// The environment is how a per-instance secret is passed to what the instance runs —
    /// an API key for one instance and another key for the next — so it is a field somebody
    /// changes rather than one set at creation.
    ///
    /// @param environment the variables the instance runs with
    /// @return the copy
    public DshInstance withEnvironment(java.util.Map<@Nullable String, @Nullable String> environment) {
        // `null` is the state "this instance adds nothing of its own", which is what following the
        // launcher is; it is written as an absent member. The constructor puts the field back to an
        // empty map for readers, so nothing downstream has to know about it.
        return new DshInstance(id, version, profile, workspace, nodeRuntime, homeMode, customHome,
                extraArguments,
                environment == null ? null : java.util.Map.copyOf(environment),
                icon, iconFile, portMode, port, createdAt);
    }

    public DshInstance withHome(DshHomeMode mode, @Nullable Path home) {
        return new DshInstance(id, version, profile, workspace, nodeRuntime, mode,
                home == null ? null : home.toAbsolutePath().normalize().toString(),
                extraArguments, environment, icon, iconFile, portMode, port, createdAt);
    }

    /// Creates a new instance with different launch arguments and environment.
    ///
    /// @param arguments   the extra command-line arguments
    /// @param environment the extra environment variables
    /// @return the updated instance
    public DshInstance withLaunchOptions(@Unmodifiable List<String> arguments,
                                         @Unmodifiable Map<String, String> environment) {
        return new DshInstance(id, version, profile, workspace, nodeRuntime, homeMode, customHome,
                arguments, environment, icon, iconFile, portMode, port, createdAt);
    }
}
