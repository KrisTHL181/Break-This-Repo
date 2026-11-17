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
package org.jackhuang.hmcl.setting;

import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.Metadata;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Owns the single [LauncherSettings] instance and persists it to disk.
///
/// Persistence is deliberately explicit: the settings object is a plain POJO,
/// and what is written is a plain serialisable snapshot of it. That keeps the
/// on-disk format stable, easy to migrate, and readable by any hand that opens
/// the file. Field names follow the desktop format where the field survived
/// the desktop UI removal, so a settings file written by either build loads in the
/// other for every setting they both have.
///
/// The desktop build also snapshotted window geometry and the theme state;
/// neither exists here — the server has no window and no theme engine.
@NotNullByDefault
public final class SettingsManager {
    private SettingsManager() {
    }

    /// The settings file inside the per-user HDSL home.
    private static final Path SETTINGS_PATH = Metadata.HMCL_USER_HOME.resolve("launcher-settings.json");

    /// The lazily created settings singleton.
    private static @Nullable LauncherSettings launcherSettings;

    /// Returns the process-wide launcher settings, loading them on first use.
    ///
    /// @return the launcher settings singleton
    public static synchronized LauncherSettings settings() {
        LauncherSettings settings = launcherSettings;
        if (settings == null) {
            settings = load();
            settings.addListener(SettingsManager::save);
            launcherSettings = settings;
        }
        return settings;
    }

    /// Loads settings from disk, returning defaults when the file is absent or unreadable.
    ///
    /// @return the loaded settings
    private static LauncherSettings load() {
        LauncherSettings settings = new LauncherSettings();
        if (!Files.isRegularFile(SETTINGS_PATH)) {
            return settings;
        }
        try {
            Snapshot snapshot = JsonUtils.fromJsonFile(SETTINGS_PATH, Snapshot.class);
            if (snapshot != null) {
                snapshot.applyTo(settings);
            }
        } catch (Exception e) {
            LOG.warning("Failed to load launcher settings from " + SETTINGS_PATH, e);
        }
        return settings;
    }

    /// Writes the current launcher settings to disk.
    ///
    /// Failures are logged rather than propagated so that a read-only home
    /// directory cannot make the launcher unusable.
    // glm-5.2: synchronized so the snapshot read in Snapshot.of runs under the
    // same class-level lock as settings(); without it two concurrent save()
    // calls could interleave their reads of every field and produce a file
    // whose fields come from different points in time.
    public static synchronized void save() {
        writeSnapshot(Snapshot.of(settings()));
    }

    /// Writes a snapshot to the settings file.
    ///
    /// Through a file beside it and a move, rather than over it. Writing over it means truncating it
    /// first, and a reader that arrives in between — another process reading the settings at
    /// start-up, which is the usual case — opens a file that is empty or half written: for this file
    /// that is no accounts, no API keys, nothing. Through a move, what a reader opens is either the
    /// file as it was or the file as it is, whole either way.
    ///
    /// @param snapshot the snapshot to write
    private static void writeSnapshot(Snapshot snapshot) {
        try {
            Path directory = SETTINGS_PATH.getParent();
            if (directory == null) {
                // A path with no parent is the root of a filesystem, which this file is never in.
                throw new IOException("The settings file has no directory: " + SETTINGS_PATH);
            }
            Files.createDirectories(directory);

            Path written = Files.createTempFile(directory, "launcher-settings", ".json");
            try {
                JsonUtils.writeToJsonFile(written, snapshot);
                restrictPermissions(written);
                move(written, SETTINGS_PATH);
            } finally {
                Files.deleteIfExists(written);
            }
        } catch (IOException e) {
            LOG.warning("Failed to save launcher settings to " + SETTINGS_PATH, e);
        }
    }

    /// Puts a written file in place of the settings file.
    ///
    /// @param written the file to move
    /// @param target  where it goes
    /// @throws IOException when it cannot be moved
    private static void move(Path written, Path target) throws IOException {
        try {
            Files.move(written, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // A filesystem that cannot do it atomically — a network mount, some Windows
            // configurations — still gets the replacement, and with it the part that matters most:
            // the reader opens one file or the other, never a half-written one.
            LOG.info("Moving " + written + " into place atomically is not supported here", e);
            Files.move(written, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /// Makes the settings file readable only by its owner.
    ///
    /// The file holds the accounts' API keys, and the harness keeps its own credentials in a file
    /// with the same restriction for the same reason. A default `umask` writes it `0644`, which on a
    /// machine with more than one person on it is a key given away — and the key is not a preference
    /// that can be reset, it is a credential that bills somebody.
    ///
    /// A filesystem that cannot express the mode (Windows, some mounts) fails here and is ignored:
    /// the key still works, and refusing to save settings over an unavailable permission bit would
    /// be trading the whole file for a hardening step.
    ///
    /// The file it is applied to is the one being written, before it is moved into place: the mode
    /// travels with the move, so restricting it afterwards would leave the window this write exists
    /// to close.
    ///
    /// @param file the file to restrict
    private static void restrictPermissions(Path file) {
        try {
            java.nio.file.attribute.PosixFileAttributeView view = Files.getFileAttributeView(
                    file, java.nio.file.attribute.PosixFileAttributeView.class);
            if (view != null) {
                view.setPermissions(java.util.EnumSet.of(
                        java.nio.file.attribute.PosixFilePermission.OWNER_READ,
                        java.nio.file.attribute.PosixFilePermission.OWNER_WRITE));
            }
        } catch (IOException | UnsupportedOperationException e) {
            LOG.info("Could not restrict the permissions of " + file, e);
        }
    }

    /// Applies the selection written before selections were kept per folder.
    ///
    /// A launcher that kept one selection for the whole program was showing one
    /// folder, so the value belongs to the folder it owns. A value already chosen
    /// since is left alone: it is the newer answer.
    ///
    /// @param settings the settings to update
    /// @param legacyId the instance id an older launcher stored, or `null`
    static void applyLegacySelection(LauncherSettings settings, @Nullable String legacyId) {
        if (legacyId == null || settings.getSelectedInstance(GameDirectory.DEFAULT_ID) != null) {
            return;
        }
        settings.setSelectedInstance(GameDirectory.DEFAULT_ID, legacyId);
    }

    /// Parses an enum value written by the shared Gson configuration.
    ///
    /// [JsonUtils#GSON] registers a lowercase enum adapter, so a value read back
    /// is `custom` rather than `CUSTOM`; `Enum.valueOf` would reject it and, if
    /// the failure were allowed to propagate, would abort the whole restore.
    ///
    /// @param type     the enum class
    /// @param value    the stored value, possibly lowercase
    /// @param fallback the value to use when the name is unknown
    /// @param <E>      the enum type
    /// @return the parsed constant, or the fallback
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOG.warning("Unknown " + type.getSimpleName() + " value in settings: " + value);
            return fallback;
        }
    }

    /// The serialisable view of [LauncherSettings].
    ///
    /// Every field is nullable so that a partially written or older file still
    /// loads; missing values keep the defaults already present in
    /// [LauncherSettings]. Package-visible so the persistence test can run a
    /// value through the same mirror the file is written with.
    static final class Snapshot {
        @SerializedName("defaultNodeRuntime")
        private @Nullable String defaultNodeRuntime;

        @SerializedName("defaultHomeMode")
        private @Nullable String defaultHomeMode;

        @SerializedName("gameDirectories")
        private @Nullable java.util.List<GameDirectory> gameDirectories;

        @SerializedName("selectedGameDirectoryId")
        private @Nullable String selectedGameDirectoryId;

        @SerializedName("defaultLaunchArguments")
        private @Nullable String defaultLaunchArguments;

        @SerializedName("accounts")
        private @Nullable java.util.List<AccountSnapshot> accounts;

        /// The suppliers added by address, in the order they were added.
        private @Nullable java.util.List<VendorSnapshot> vendors;

        /// Which account the launcher uses, by its key. Written to the snapshot because a choice with
        /// no field here is a choice that silently reverts on the next start — the trap that lost
        /// seventeen settings once already.
        @SerializedName("activeAccountKey")
        private @Nullable String activeAccountKey;

        @SerializedName("nodeSource")
        private @Nullable String nodeSource;

        @SerializedName("selectedInstance")
        private @Nullable java.util.Map<String, String> selectedInstance;

        /// The single selection written by launchers that kept one for the whole
        /// launcher rather than one per folder.
        ///
        /// Only ever read, and only while nothing per folder has been stored: it
        /// belonged to the folder the launcher owns.
        @SerializedName("selectedInstanceId")
        private @Nullable String legacySelectedInstanceId;

        @SerializedName("openBrowserOnLaunch")
        private @Nullable Boolean openBrowserOnLaunch;

        // Everything below belongs to a control on one of the settings pages,
        // and each one is written here because the snapshot is what `save()`
        // writes and the only thing `load()` reads: a setting with no field here
        // is a setting somebody can change and lose at the next start.

        // Stored as the lowercase id the enum itself publishes, the way `nodeSource` is:
        // Gson's enum adapter would write `MANUAL`, so a file written by the interface and
        // one written by hand would disagree about the spelling of the same value.
        @SerializedName("buildScriptPolicy")
        private @Nullable String buildScriptPolicy;

        @SerializedName("launcherVisibility")
        private @Nullable String launcherVisibility;

        @SerializedName("isolationPolicy")
        private @Nullable String isolationPolicy;

        @SerializedName("showLogs")
        private @Nullable Boolean showLogs;

        @SerializedName("debugLog")
        private @Nullable Boolean debugLog;

        @SerializedName("preLaunchCommand")
        private @Nullable String preLaunchCommand;

        @SerializedName("postExitCommand")
        private @Nullable String postExitCommand;

        @SerializedName("globalEnvironment")
        private @Nullable java.util.Map<String, String> globalEnvironment;

        @SerializedName("pluginCatalogUrl")
        private @Nullable String pluginCatalogUrl;

        /// Which npm registry the panel was set to, or absent for the deployment's own.
        @SerializedName("npmRegistryPreset")
        private @Nullable String npmRegistryPreset;

        /// The registry typed by hand, read only while the preset is `custom`.
        @SerializedName("npmRegistry")
        private @Nullable String npmRegistry;

        /// Which PyPI index the brand installers use, or absent for the deployment's own.
        @SerializedName("brandPypiIndexPreset")
        private @Nullable String brandPypiIndexPreset;

        /// The PyPI index typed by hand, read only while the preset is `custom`.
        @SerializedName("brandPypiIndex")
        private @Nullable String brandPypiIndex;

        /// Where the CPython build is fetched from, or absent for the deployment's own.
        @SerializedName("brandPythonMirrorPreset")
        private @Nullable String brandPythonMirrorPreset;

        /// The CPython mirror typed by hand, read only while the preset is `custom`.
        @SerializedName("brandPythonMirror")
        private @Nullable String brandPythonMirror;

        /// Where version source archives are fetched from, or absent for the deployment's own.
        @SerializedName("brandArchivePreset")
        private @Nullable String brandArchivePreset;

        /// The source-archive base typed by hand, read only while the preset is `custom`.
        @SerializedName("brandArchiveBase")
        private @Nullable String brandArchiveBase;

        /// Per-brand release bases: brand id → preset id.
        @SerializedName("brandReleasePresets")
        private @Nullable java.util.Map<String, String> brandReleasePresets;

        /// Per-brand release bases typed by hand: brand id → address.
        @SerializedName("brandReleaseBases")
        private @Nullable java.util.Map<String, String> brandReleaseBases;

        /// Where the modpack market's index is read from, or absent for the published one.
        @SerializedName("packMarketUrl")
        private @Nullable String packMarketUrl;

        /// How much of a new instance's dependency tree is held, by the enum's own name.
        @SerializedName("dependencyPolicy")
        private @Nullable String dependencyPolicy;

        @SerializedName("cacheDirectory")
        private @Nullable String cacheDirectory;

        @SerializedName("cacheDirectoryCustom")
        private @Nullable Boolean cacheDirectoryCustom;

        @SerializedName("autoDownloadThreads")
        private @Nullable Boolean autoDownloadThreads;

        @SerializedName("downloadConcurrency")
        private @Nullable Integer downloadConcurrency;

        @SerializedName("proxyMode")
        private @Nullable String proxyMode;

        @SerializedName("proxyHost")
        private @Nullable String proxyHost;

        @SerializedName("proxyPort")
        private @Nullable String proxyPort;

        @SerializedName("proxyAuthenticated")
        private @Nullable Boolean proxyAuthenticated;

        @SerializedName("proxyUser")
        private @Nullable String proxyUser;

        @SerializedName("proxyPassword")
        private @Nullable String proxyPassword;

        /// One account as it is stored.
        ///
        /// @param vendorId the vendor's id
        /// @param apiKey   the key
        /// @param baseUrl  the endpoint, for a vendor whose address is per account
        /// @param label    what the person calls it
        private record AccountSnapshot(
                @Nullable org.jackhuang.hmcl.dsh.DshAccount.AccountKind kind,
                String vendorId, String apiKey,
                @Nullable String baseUrl, @Nullable String label,
                @Nullable String model,
                @Nullable String skinType,
                @Nullable String skinModel,
                @Nullable String skinPath,
                @Nullable String skinCapePath) {
        }

        /// A supplier somebody added by address.
        ///
        /// All five fields, because all five are what the harness needs to route it: the id becomes
        /// the route name, the address is where the calls go, the protocol is what the route
        /// declares, and the variable is where the key is read from.
        private record VendorSnapshot(
                String id, String displayName, String apiKeyEnv, String api, String baseUrl) {
        }

        /// Captures the current settings into a serialisable snapshot.
        ///
        /// @param settings the settings to capture
        /// @return the snapshot
        static Snapshot of(LauncherSettings settings) {
            Snapshot snapshot = new Snapshot();
            snapshot.defaultNodeRuntime = settings.defaultNodeRuntime();
            snapshot.defaultHomeMode = settings.defaultHomeMode().name();
            snapshot.gameDirectories = java.util.List.copyOf(settings.getGameDirectories());
            snapshot.selectedGameDirectoryId = settings.getSelectedGameDirectoryId();
            snapshot.defaultLaunchArguments = settings.defaultLaunchArguments();
            snapshot.activeAccountKey = settings.activeAccountKey();
            snapshot.accounts = settings.getAccounts().stream()
                    .map(account -> new AccountSnapshot(account.kind(), account.vendorId(),
                            account.apiKey(), account.baseUrl(), account.label(), account.model(),
                            account.skinOrDefault().type().name(),
                            account.skinOrDefault().model().modelName,
                            account.skinOrDefault().localSkinPath(),
                            account.skinOrDefault().localCapePath()))
                    .toList();
            snapshot.vendors = settings.getCustomVendors().stream()
                    .map(vendor -> new VendorSnapshot(vendor.id(), vendor.displayName(),
                            vendor.apiKeyEnv(), vendor.api(), vendor.baseUrl()))
                    .toList();
            snapshot.nodeSource = settings.nodeSource().id();
            snapshot.selectedInstance = new java.util.LinkedHashMap<>(settings.getSelectedInstance());
            snapshot.openBrowserOnLaunch = settings.isOpenBrowserOnLaunch();
            snapshot.buildScriptPolicy = settings.buildScriptPolicy().id();
            snapshot.launcherVisibility = settings.launcherVisibility().id();
            snapshot.isolationPolicy = settings.isolationPolicy().id();
            snapshot.showLogs = settings.isShowLogs();
            snapshot.debugLog = settings.isDebugLog();
            snapshot.preLaunchCommand = settings.getPreLaunchCommand();
            snapshot.postExitCommand = settings.getPostExitCommand();
            snapshot.globalEnvironment = new java.util.LinkedHashMap<>(settings.globalEnvironment());
            snapshot.pluginCatalogUrl = settings.getPluginCatalogUrl();
            snapshot.packMarketUrl = settings.getPackMarketUrl();
            snapshot.npmRegistryPreset = settings.getNpmRegistryPreset();
            snapshot.npmRegistry = settings.getNpmRegistry();
            snapshot.brandPypiIndexPreset = settings.getBrandPypiIndexPreset();
            snapshot.brandPypiIndex = settings.getBrandPypiIndex();
            snapshot.brandPythonMirrorPreset = settings.getBrandPythonMirrorPreset();
            snapshot.brandPythonMirror = settings.getBrandPythonMirror();
            snapshot.brandArchivePreset = settings.getBrandArchivePreset();
            snapshot.brandArchiveBase = settings.getBrandArchiveBase();
            snapshot.brandReleasePresets = new java.util.LinkedHashMap<>(settings.getBrandReleasePresets());
            snapshot.brandReleaseBases = new java.util.LinkedHashMap<>(settings.getBrandReleaseBases());
            snapshot.dependencyPolicy = settings.dependencyPolicy().name();
            snapshot.cacheDirectory = settings.getCacheDirectory();
            snapshot.cacheDirectoryCustom = settings.isCacheDirectoryCustom();
            snapshot.autoDownloadThreads = settings.isAutoDownloadThreads();
            snapshot.downloadConcurrency = settings.getDownloadConcurrency();
            snapshot.proxyMode = settings.proxyMode().id();
            snapshot.proxyHost = settings.getProxyHost();
            snapshot.proxyPort = settings.getProxyPort();
            snapshot.proxyAuthenticated = settings.isProxyAuthenticated();
            snapshot.proxyUser = settings.getProxyUser();
            snapshot.proxyPassword = settings.getProxyPassword();
            return snapshot;
        }

        /// Applies this snapshot onto a settings object, ignoring unset fields.
        ///
        /// @param settings the settings to update
        void applyTo(LauncherSettings settings) {
            // Each field is applied on its own: a single unreadable value must
            // not silently discard every setting that follows it.
            if (defaultNodeRuntime != null) {
                settings.setDefaultNodeRuntime(defaultNodeRuntime);
            }
            if (defaultHomeMode != null) {
                settings.setDefaultHomeMode(parseEnum(
                        org.jackhuang.hmcl.dsh.DshHomeMode.class, defaultHomeMode,
                        org.jackhuang.hmcl.dsh.DshHomeMode.ISOLATED));
            }
            if (gameDirectories != null) {
                settings.setGameDirectories(java.util.List.copyOf(gameDirectories));
            }
            if (selectedGameDirectoryId != null) {
                settings.setSelectedGameDirectoryId(selectedGameDirectoryId);
            }
            if (defaultLaunchArguments != null) {
                settings.setDefaultLaunchArguments(defaultLaunchArguments);
            }
            if (activeAccountKey != null) {
                settings.setActiveAccountKey(activeAccountKey);
            }
            if (accounts != null) {
                for (AccountSnapshot account : accounts) {
                    settings.getAccounts().add(new org.jackhuang.hmcl.dsh.DshAccount(
                            account.kind() == null
                                    ? org.jackhuang.hmcl.dsh.DshAccount.AccountKind.THIRD_PARTY
                                    : account.kind(),
                            account.vendorId(), account.apiKey(), account.baseUrl(),
                            account.label(), account.model(),
                            new org.jackhuang.hmcl.dsh.skin.DshSkinChoice(
                                    // Read through the forgiving reader rather than the enum's own:
                                    // a name this launcher does not know — a file written by a later
                                    // version, or one edited by hand — must not stop the whole
                                    // settings file from loading.
                                    java.util.Objects.requireNonNullElse(
                                            org.jackhuang.hmcl.dsh.skin.DshSkinChoice.Type
                                                    .fromStorage(account.skinType()),
                                            org.jackhuang.hmcl.dsh.skin.DshSkinChoice.Type.DEFAULT),
                                    org.jackhuang.hmcl.dsh.skin.DshSkinChoice.TextureModel
                                            .fromStorage(account.skinModel()),
                                    account.skinPath(),
                                    account.skinCapePath())));
                }
            }
            if (vendors != null) {
                for (VendorSnapshot vendor : vendors) {
                    // A supplier with no address cannot be routed, so a row that has lost one — a
                    // hand-edited file, or a version that wrote it differently — is dropped rather
                    // than offered as something that cannot work.
                    if (vendor.id() == null || vendor.baseUrl() == null || vendor.baseUrl().isBlank()) {
                        continue;
                    }
                    settings.getCustomVendors().add(new org.jackhuang.hmcl.dsh.DshVendor(
                            vendor.id(),
                            vendor.displayName() == null ? vendor.id() : vendor.displayName(),
                            vendor.apiKeyEnv() == null ? "" : vendor.apiKeyEnv(),
                            vendor.api() == null ? org.jackhuang.hmcl.dsh.DshVendor.APIS.get(0) : vendor.api(),
                            vendor.baseUrl(), false));
                }
            }
            if (nodeSource != null) {
                settings.setNodeSource(org.jackhuang.hmcl.dsh.NodeSource.of(nodeSource));
            }
            if (selectedInstance != null) {
                settings.getSelectedInstance().clear();
                settings.getSelectedInstance().putAll(selectedInstance);
            } else {
                applyLegacySelection(settings, legacySelectedInstanceId);
            }
            if (openBrowserOnLaunch != null) {
                settings.setOpenBrowserOnLaunch(openBrowserOnLaunch);
            }
            if (buildScriptPolicy != null) {
                settings.setBuildScriptPolicy(org.jackhuang.hmcl.dsh.DshBuildScriptPolicy.of(buildScriptPolicy));
            }
            if (launcherVisibility != null) {
                settings.setLauncherVisibility(org.jackhuang.hmcl.dsh.DshLauncherVisibility.of(launcherVisibility));
            }
            if (isolationPolicy != null) {
                settings.setIsolationPolicy(org.jackhuang.hmcl.dsh.DshIsolationPolicy.of(isolationPolicy));
            }
            if (showLogs != null) {
                settings.setShowLogs(showLogs);
            }
            if (debugLog != null) {
                settings.setDebugLog(debugLog);
                // The switch decides whether lines are written, so the logger has to be
                // told as well: without this it stays as the interface left it, which
                // after a restart is off.
                org.jackhuang.hmcl.util.logging.Logger.setDebugEnabled(debugLog);
            }
            if (preLaunchCommand != null) {
                settings.setPreLaunchCommand(preLaunchCommand);
            }
            if (postExitCommand != null) {
                settings.setPostExitCommand(postExitCommand);
            }
            if (globalEnvironment != null) {
                settings.setGlobalEnvironment(new java.util.LinkedHashMap<>(globalEnvironment));
            }
            if (packMarketUrl != null) {
                settings.setPackMarketUrl(packMarketUrl);
            }
            if (dependencyPolicy != null) {
                // A name this launcher does not know is left at the default rather than refusing the
                // whole settings file: a policy is a preference, and losing it costs one choice.
                try {
                    settings.setDependencyPolicy(
                            org.jackhuang.hmcl.dsh.DshDependencyPolicy.valueOf(dependencyPolicy));
                } catch (IllegalArgumentException e) {
                    LOG.warning("Unknown dependency policy in the settings: " + dependencyPolicy);
                }
            }
            if (pluginCatalogUrl != null) {
                settings.setPluginCatalogUrl(pluginCatalogUrl);
            }
            if (npmRegistryPreset != null) {
                settings.setNpmRegistryPreset(npmRegistryPreset);
            }
            if (npmRegistry != null) {
                settings.setNpmRegistry(npmRegistry);
            }
            if (brandPypiIndexPreset != null) {
                settings.setBrandPypiIndexPreset(brandPypiIndexPreset);
            }
            if (brandPypiIndex != null) {
                settings.setBrandPypiIndex(brandPypiIndex);
            }
            if (brandPythonMirrorPreset != null) {
                settings.setBrandPythonMirrorPreset(brandPythonMirrorPreset);
            }
            if (brandPythonMirror != null) {
                settings.setBrandPythonMirror(brandPythonMirror);
            }
            if (brandArchivePreset != null) {
                settings.setBrandArchivePreset(brandArchivePreset);
            }
            if (brandArchiveBase != null) {
                settings.setBrandArchiveBase(brandArchiveBase);
            }
            if (brandReleasePresets != null) {
                for (java.util.Map.Entry<String, String> entry : brandReleasePresets.entrySet()) {
                    settings.setBrandReleasePreset(entry.getKey(), entry.getValue());
                }
            }
            if (brandReleaseBases != null) {
                for (java.util.Map.Entry<String, String> entry : brandReleaseBases.entrySet()) {
                    settings.setBrandReleaseBase(entry.getKey(), entry.getValue());
                }
            }
            if (cacheDirectory != null) {
                settings.setCacheDirectory(cacheDirectory);
            }
            if (cacheDirectoryCustom != null) {
                settings.setCacheDirectoryCustom(cacheDirectoryCustom);
            }
            if (autoDownloadThreads != null) {
                settings.setAutoDownloadThreads(autoDownloadThreads);
            }
            if (downloadConcurrency != null) {
                settings.setDownloadConcurrency(downloadConcurrency);
            }
            if (proxyMode != null) {
                settings.setProxyMode(org.jackhuang.hmcl.dsh.DshProxyMode.of(proxyMode));
            }
            if (proxyHost != null) {
                settings.setProxyHost(proxyHost);
            }
            if (proxyPort != null) {
                settings.setProxyPort(proxyPort);
            }
            if (proxyAuthenticated != null) {
                settings.setProxyAuthenticated(proxyAuthenticated);
            }
            if (proxyUser != null) {
                settings.setProxyUser(proxyUser);
            }
            if (proxyPassword != null) {
                settings.setProxyPassword(proxyPassword);
            }
        }
    }
}
