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

import org.jackhuang.hmcl.dsh.DshAccount;
import org.jackhuang.hmcl.dsh.DshBuildScriptPolicy;
import org.jackhuang.hmcl.dsh.DshDependencyPolicy;
import org.jackhuang.hmcl.dsh.DshHomeMode;
import org.jackhuang.hmcl.dsh.DshInstance;
import org.jackhuang.hmcl.dsh.DshInstanceSettings;
import org.jackhuang.hmcl.dsh.DshInstanceManager;
import org.jackhuang.hmcl.dsh.DshIsolationPolicy;
import org.jackhuang.hmcl.dsh.DshLauncherVisibility;
import org.jackhuang.hmcl.dsh.DshNodeRuntime;
import org.jackhuang.hmcl.dsh.DshProxyMode;
import org.jackhuang.hmcl.dsh.DshVendor;
import org.jackhuang.hmcl.dsh.NodeSource;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/// Holds the launcher settings HDSL-web persists and serves.
///
/// The desktop counterpart exposed every field as a toolkit property; the web
/// copy is a plain object with getters and setters, plus a simple
/// addListener/removeListener change notification every setter fires. Theme,
/// font and window-geometry fields do not exist here — the server has no
/// chrome to theme.
///
/// Persistence is the explicit snapshot in [SettingsManager], so the on-disk
/// format stays stable and easy to migrate as HDSL-web grows.
@NotNullByDefault
public final class LauncherSettings {
    /// The key for the folders the launcher looks for instances in.
    public static final String GAME_DIRECTORIES = "gameDirectories";

    /// The key for the folder whose instances are shown.
    public static final String SELECTED_GAME_DIRECTORY = "selectedGameDirectoryId";

    /// The key for the instance each game directory has chosen.
    public static final String SELECTED_INSTANCE = "selectedInstance";

    /// The Node runtime new instances are given.
    ///
    /// An instance may pin its own; this is what "follow the global setting"
    /// resolves to, and what a new instance starts with.
    private String defaultNodeRuntime = DshNodeRuntime.SYSTEM;

    /// The DSH_HOME policy new instances are given.
    private DshHomeMode defaultHomeMode = DshHomeMode.ISOLATED;

    /// The folders the launcher looks for instances in.
    private List<GameDirectory> gameDirectories = List.of();

    /// The folder whose instances the list shows.
    private @Nullable String selectedGameDirectoryId = GameDirectory.DEFAULT_ID;

    /// What happens when a plugin wants to run an install script.
    ///
    /// An install script runs with the user's own rights and nobody has read it, which
    /// is why a package manager stops and asks. Answering is a decision, so the
    /// launcher asks by default and an instance can be told otherwise.
    private DshBuildScriptPolicy buildScriptPolicy = DshBuildScriptPolicy.MANUAL;

    /// Whether the launcher writes debug lines to its log.
    private boolean debugLog;

    /// Where the plugin catalogue is read from, or empty for the built-in address.
    private String pluginCatalogUrl = "";

    /// Which npm registry the panel was set to, or `environment` to use the deployment's own.
    ///
    /// Kept as the preset's id rather than as an address so that a mirror whose URL changes can be
    /// followed by a release rather than by everybody who chose it.
    private String npmRegistryPreset = org.jackhuang.hmcl.dsh.NpmRegistry.ENVIRONMENT;

    /// The registry somebody typed by hand, or empty. Read only while the preset is `custom`.
    private String npmRegistry = "";

    /// Which PyPI index the brand installers use (uv itself and `uv sync`).
    ///
    /// Kept as a preset id for the same reason as the npm registry: a mirror whose
    /// address changes should be followed by a release, not by every deployment.
    private String brandPypiIndexPreset = org.jackhuang.hmcl.web.brand.BrandSources.ENVIRONMENT;

    /// The PyPI index typed by hand, or empty. Read only while the preset is `custom`.
    private String brandPypiIndex = "";

    /// Where the CPython build (`UV_PYTHON_INSTALL_MIRROR`) is fetched from.
    private String brandPythonMirrorPreset = org.jackhuang.hmcl.web.brand.BrandSources.ENVIRONMENT;

    /// The CPython mirror typed by hand, or empty. Read only while the preset is `custom`.
    private String brandPythonMirror = "";

    /// Where version source archives are fetched from (GitHub's codeload by default).
    private String brandArchivePreset = org.jackhuang.hmcl.web.brand.BrandSources.ENVIRONMENT;

    /// The source-archive base typed by hand, or empty. Read only while the preset is `custom`.
    private String brandArchiveBase = "";

    /// Per-brand release bases: brand id → preset id. A brand with no entry follows the
    /// deployment's environment and then the upstream GitHub releases.
    private final Map<String, String> brandReleasePresets = new LinkedHashMap<>();

    /// Per-brand release bases typed by hand: brand id → address, read only while that
    /// brand's preset is `custom`.
    private final Map<String, String> brandReleaseBases = new LinkedHashMap<>();

    /// Where the modpack market's index is read from, or empty for the one the ecosystem publishes.
    private String packMarketUrl = "";

    /// How much of a new instance's dependency tree is held to the versions the harness declares.
    ///
    /// Defaults to [DshDependencyPolicy#CORE_PINNED]: the harness's own ranges let a
    /// package the launcher never chose arrive underneath it, which has twice stopped
    /// an instance from starting.
    private DshDependencyPolicy dependencyPolicy = DshDependencyPolicy.CORE_PINNED;

    /// Whether a new instance keeps its own home.
    private DshIsolationPolicy isolationPolicy = DshIsolationPolicy.WITH_PLUGINS;

    /// What the launcher does with itself once an instance is running.
    private DshLauncherVisibility launcherVisibility = DshLauncherVisibility.KEEP;

    /// Whether an instance's log window opens when it launches.
    private boolean showLogs;

    /// A command to run before an instance starts, or an empty string for none.
    private String preLaunchCommand = "";

    /// A command to run after an instance has ended, or an empty string for none.
    private String postExitCommand = "";

    /// The variables every instance runs with, unless it sets its own.
    private Map<String, String> globalEnvironment = Map.of();

    /// How the launcher reaches the network.
    private DshProxyMode proxyMode = DshProxyMode.SYSTEM;

    /// The proxy's host.
    private String proxyHost = "";

    /// The proxy's port.
    private String proxyPort = "";

    /// Whether the proxy wants a name and a password.
    private boolean proxyAuthenticated;

    /// The name the proxy wants, when it wants one.
    private String proxyUser = "";

    /// The password the proxy wants, when it wants one.
    private String proxyPassword = "";

    /// Whether the cache folder was chosen rather than left where the launcher puts it.
    private boolean cacheDirectoryCustom;

    /// Where the launcher keeps what it fetched, or empty for the default place.
    private String cacheDirectory = "";

    /// Whether the launcher chooses how many downloads happen at once.
    private boolean autoDownloadThreads = true;

    /// How many downloads may happen at once, when the launcher is not choosing.
    private @Nullable Integer downloadConcurrency = 64;

    /// The accounts this machine has been given.
    ///
    /// Kept in the launcher's own settings — which already holds this machine's
    /// proxy password — and never written into an instance, a profile or a pack.
    private final List<DshAccount> accounts = new ArrayList<>();

    /// The suppliers this machine has been told about, beside the ones the launcher offers.
    private final List<DshVendor> customVendors = new ArrayList<>();

    /// Which account the launcher uses, by its key, or empty.
    private String activeAccountKey = "";

    /// The instance the launch button targets, keyed by game directory id.
    private final Map<String, String> selectedInstance = new LinkedHashMap<>();

    /// Where Node.js distributions are fetched from.
    private NodeSource nodeSource = NodeSource.OFFICIAL;

    /// Whether the browser is opened automatically once an instance is ready.
    private boolean openBrowserOnLaunch = true;

    /// The arguments a new instance is launched with, as one line.
    private String defaultLaunchArguments = "";

    /// Who is told when a setting changes.
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /// Registers a listener called on the setting thread whenever a setting changes.
    ///
    /// @param listener the listener
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /// Removes a listener registered through [#addListener].
    ///
    /// @param listener the listener
    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /// Tells every listener a setting changed.
    private void fireChanged() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public String defaultNodeRuntime() {
        return defaultNodeRuntime;
    }

    public void setDefaultNodeRuntime(String defaultNodeRuntime) {
        this.defaultNodeRuntime = defaultNodeRuntime;
        fireChanged();
    }

    public DshHomeMode defaultHomeMode() {
        return defaultHomeMode;
    }

    public void setDefaultHomeMode(DshHomeMode defaultHomeMode) {
        this.defaultHomeMode = defaultHomeMode;
        fireChanged();
    }

    /// Returns the folders the launcher looks for instances in.
    ///
    /// @return the folders, possibly empty; the default folder needs no entry
    public List<GameDirectory> getGameDirectories() {
        return gameDirectories;
    }

    public void setGameDirectories(List<GameDirectory> gameDirectories) {
        this.gameDirectories = List.copyOf(gameDirectories);
        fireChanged();
    }

    public @Nullable String getSelectedGameDirectoryId() {
        return selectedGameDirectoryId;
    }

    public void setSelectedGameDirectoryId(@Nullable String selectedGameDirectoryId) {
        this.selectedGameDirectoryId = selectedGameDirectoryId;
        fireChanged();
    }

    /// Returns the launcher's policy.
    ///
    /// @return the policy, never `null`
    public DshBuildScriptPolicy buildScriptPolicy() {
        return buildScriptPolicy == null ? DshBuildScriptPolicy.MANUAL : buildScriptPolicy;
    }

    public void setBuildScriptPolicy(DshBuildScriptPolicy buildScriptPolicy) {
        this.buildScriptPolicy = buildScriptPolicy;
        fireChanged();
    }

    public boolean isDebugLog() {
        return debugLog;
    }

    public void setDebugLog(boolean debugLog) {
        this.debugLog = debugLog;
        fireChanged();
    }

    /// Returns the address the plugin catalogue is read from.
    ///
    /// @return the address, empty for the built-in one
    public String getPluginCatalogUrl() {
        return pluginCatalogUrl;
    }

    public void setPluginCatalogUrl(String pluginCatalogUrl) {
        this.pluginCatalogUrl = pluginCatalogUrl;
        fireChanged();
    }

    /// Returns which npm registry the panel was set to.
    ///
    /// @return the preset's id, or `environment` when the deployment's own is to be used
    public String getNpmRegistryPreset() {
        return npmRegistryPreset == null || npmRegistryPreset.isBlank()
                ? org.jackhuang.hmcl.dsh.NpmRegistry.ENVIRONMENT : npmRegistryPreset;
    }

    public void setNpmRegistryPreset(String npmRegistryPreset) {
        this.npmRegistryPreset = npmRegistryPreset;
        fireChanged();
    }

    /// Returns the registry typed by hand.
    ///
    /// @return the address, empty when none was typed; meaningless unless the preset is `custom`
    public String getNpmRegistry() {
        return npmRegistry == null ? "" : npmRegistry;
    }

    public void setNpmRegistry(String npmRegistry) {
        this.npmRegistry = npmRegistry;
        fireChanged();
    }

    /// Returns which PyPI index the brand installers use.
    ///
    /// @return the preset's id, or `environment` when the deployment's own is to be used
    public String getBrandPypiIndexPreset() {
        return brandPypiIndexPreset == null || brandPypiIndexPreset.isBlank()
                ? org.jackhuang.hmcl.web.brand.BrandSources.ENVIRONMENT : brandPypiIndexPreset;
    }

    public void setBrandPypiIndexPreset(String preset) {
        this.brandPypiIndexPreset = preset;
        fireChanged();
    }

    /// Returns the PyPI index typed by hand.
    ///
    /// @return the address, empty when none was typed; meaningless unless the preset is `custom`
    public String getBrandPypiIndex() {
        return brandPypiIndex == null ? "" : brandPypiIndex;
    }

    public void setBrandPypiIndex(String index) {
        this.brandPypiIndex = index == null ? "" : index;
        fireChanged();
    }

    /// Returns where the CPython build is fetched from.
    ///
    /// @return the preset's id, or `environment` when the deployment's own is to be used
    public String getBrandPythonMirrorPreset() {
        return brandPythonMirrorPreset == null || brandPythonMirrorPreset.isBlank()
                ? org.jackhuang.hmcl.web.brand.BrandSources.ENVIRONMENT : brandPythonMirrorPreset;
    }

    public void setBrandPythonMirrorPreset(String preset) {
        this.brandPythonMirrorPreset = preset;
        fireChanged();
    }

    /// Returns the CPython mirror typed by hand.
    ///
    /// @return the address, empty when none was typed; meaningless unless the preset is `custom`
    public String getBrandPythonMirror() {
        return brandPythonMirror == null ? "" : brandPythonMirror;
    }

    public void setBrandPythonMirror(String mirror) {
        this.brandPythonMirror = mirror == null ? "" : mirror;
        fireChanged();
    }

    /// Returns where version source archives are fetched from.
    ///
    /// @return the preset's id, or `environment` when the deployment's own is to be used
    public String getBrandArchivePreset() {
        return brandArchivePreset == null || brandArchivePreset.isBlank()
                ? org.jackhuang.hmcl.web.brand.BrandSources.ENVIRONMENT : brandArchivePreset;
    }

    public void setBrandArchivePreset(String preset) {
        this.brandArchivePreset = preset;
        fireChanged();
    }

    /// Returns the source-archive base typed by hand.
    ///
    /// @return the address, empty when none was typed; meaningless unless the preset is `custom`
    public String getBrandArchiveBase() {
        return brandArchiveBase == null ? "" : brandArchiveBase;
    }

    public void setBrandArchiveBase(String base) {
        this.brandArchiveBase = base == null ? "" : base;
        fireChanged();
    }

    /// Returns which release base a brand was set to.
    ///
    /// @param brandId the brand's id, e.g. `octop`
    /// @return the preset's id, or `environment` when that brand has no setting
    public String getBrandReleasePreset(String brandId) {
        String preset = brandReleasePresets.get(brandId);
        return preset == null || preset.isBlank()
                ? org.jackhuang.hmcl.web.brand.BrandSources.ENVIRONMENT : preset;
    }

    /// Returns every per-brand release preset, by brand id.
    ///
    /// @return an unmodifiable view; brands with no entry are absent
    public Map<String, String> getBrandReleasePresets() {
        return java.util.Collections.unmodifiableMap(brandReleasePresets);
    }

    /// Returns every hand-typed release base, by brand id.
    public Map<String, String> getBrandReleaseBases() {
        return java.util.Collections.unmodifiableMap(brandReleaseBases);
    }

    public void setBrandReleasePreset(String brandId, String preset) {
        brandReleasePresets.put(brandId, preset);
        fireChanged();
    }

    /// Returns the release base typed by hand for a brand.
    ///
    /// @param brandId the brand's id
    /// @return the address, empty when none was typed; meaningless unless the preset is `custom`
    public String getBrandReleaseBase(String brandId) {
        String base = brandReleaseBases.get(brandId);
        return base == null ? "" : base;
    }

    public void setBrandReleaseBase(String brandId, String base) {
        brandReleaseBases.put(brandId, base == null ? "" : base);
        fireChanged();
    }

    /// Returns where the modpack market's index is read from.
    ///
    /// @return the address, empty for the published one
    public String getPackMarketUrl() {
        return packMarketUrl;
    }

    public void setPackMarketUrl(String packMarketUrl) {
        this.packMarketUrl = packMarketUrl;
        fireChanged();
    }

    /// Returns how much of a new instance's dependency tree is held.
    ///
    /// @return the policy, never `null`
    public DshDependencyPolicy dependencyPolicy() {
        return dependencyPolicy == null ? DshDependencyPolicy.CORE_PINNED : dependencyPolicy;
    }

    public void setDependencyPolicy(DshDependencyPolicy dependencyPolicy) {
        this.dependencyPolicy = dependencyPolicy;
        fireChanged();
    }

    /// Returns the policy a new instance is created under.
    ///
    /// @return the policy, never `null`
    public DshIsolationPolicy isolationPolicy() {
        return isolationPolicy == null ? DshIsolationPolicy.WITH_PLUGINS : isolationPolicy;
    }

    public void setIsolationPolicy(DshIsolationPolicy isolationPolicy) {
        this.isolationPolicy = isolationPolicy;
        fireChanged();
    }

    /// Returns what the launcher does with itself once an instance is running.
    ///
    /// @return the choice, never `null`
    public DshLauncherVisibility launcherVisibility() {
        return launcherVisibility == null ? DshLauncherVisibility.KEEP : launcherVisibility;
    }

    public void setLauncherVisibility(DshLauncherVisibility launcherVisibility) {
        this.launcherVisibility = launcherVisibility;
        fireChanged();
    }

    public boolean isShowLogs() {
        return showLogs;
    }

    public void setShowLogs(boolean showLogs) {
        this.showLogs = showLogs;
        fireChanged();
    }

    public String getPreLaunchCommand() {
        return preLaunchCommand == null ? "" : preLaunchCommand;
    }

    public void setPreLaunchCommand(String preLaunchCommand) {
        this.preLaunchCommand = preLaunchCommand;
        fireChanged();
    }

    public String getPostExitCommand() {
        return postExitCommand == null ? "" : postExitCommand;
    }

    public void setPostExitCommand(String postExitCommand) {
        this.postExitCommand = postExitCommand;
        fireChanged();
    }

    /// Returns the variables every instance runs with.
    ///
    /// @return the variables, never `null`
    public Map<String, String> globalEnvironment() {
        return globalEnvironment == null ? Map.of() : globalEnvironment;
    }

    public void setGlobalEnvironment(Map<String, String> globalEnvironment) {
        this.globalEnvironment = new LinkedHashMap<>(globalEnvironment);
        fireChanged();
    }

    public DshProxyMode proxyMode() {
        return proxyMode == null ? DshProxyMode.SYSTEM : proxyMode;
    }

    public void setProxyMode(DshProxyMode proxyMode) {
        this.proxyMode = proxyMode;
        fireChanged();
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
        fireChanged();
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
        fireChanged();
    }

    public boolean isProxyAuthenticated() {
        return proxyAuthenticated;
    }

    public void setProxyAuthenticated(boolean proxyAuthenticated) {
        this.proxyAuthenticated = proxyAuthenticated;
        fireChanged();
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
        fireChanged();
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
        fireChanged();
    }

    public boolean isCacheDirectoryCustom() {
        return cacheDirectoryCustom;
    }

    public void setCacheDirectoryCustom(boolean cacheDirectoryCustom) {
        this.cacheDirectoryCustom = cacheDirectoryCustom;
        fireChanged();
    }

    /// Returns where the launcher keeps what it fetched.
    ///
    /// @return the folder, empty for the default place
    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
        fireChanged();
    }

    public boolean isAutoDownloadThreads() {
        return autoDownloadThreads;
    }

    public void setAutoDownloadThreads(boolean autoDownloadThreads) {
        this.autoDownloadThreads = autoDownloadThreads;
        fireChanged();
    }

    public @Nullable Integer getDownloadConcurrency() {
        return downloadConcurrency;
    }

    public void setDownloadConcurrency(@Nullable Integer downloadConcurrency) {
        this.downloadConcurrency = downloadConcurrency;
        fireChanged();
    }

    /// Returns the accounts.
    ///
    /// The list itself is mutable; [SettingsManager] re-saves after changes
    /// made through it.
    ///
    /// @return the list
    public List<DshAccount> getAccounts() {
        return accounts;
    }

    /// Returns the suppliers this machine has been told about.
    ///
    /// @return the list
    public List<DshVendor> getCustomVendors() {
        return customVendors;
    }

    /// Returns every supplier the interface should offer: the launcher's own, then the added ones.
    ///
    /// @return the list
    public List<DshVendor> allVendors() {
        List<DshVendor> all = new ArrayList<>(DshVendor.offered());
        for (DshVendor vendor : customVendors) {
            boolean known = all.stream().anyMatch(known0 -> known0.id().equalsIgnoreCase(vendor.id()));
            if (!known) {
                all.add(vendor);
            }
        }
        return List.copyOf(all);
    }

    /// Returns which account the launcher uses, or empty when none is chosen.
    ///
    /// @return the key
    public @Nullable String activeAccountKey() {
        return activeAccountKey == null || activeAccountKey.isEmpty() ? null : activeAccountKey;
    }

    public void setActiveAccountKey(String activeAccountKey) {
        this.activeAccountKey = activeAccountKey;
        fireChanged();
    }

    /// Returns the account the launcher uses.
    ///
    /// A chosen account that is gone falls back to the first one, because a launcher with exactly one
    /// account means it for everything and does not need to be told so.
    ///
    /// @return the account, or `null` when there are none
    public @Nullable DshAccount activeAccount() {
        List<DshAccount> all = getAccounts();
        if (all.isEmpty()) {
            return null;
        }
        String chosen = activeAccountKey();
        if (chosen != null) {
            for (DshAccount account : all) {
                if (account.matchesKey(chosen)) {
                    return account;
                }
            }
        }
        return all.get(0);
    }

    /// Returns the selected instance of every game directory.
    ///
    /// Owned by [GameDirectoryManager]; code outside it should not write here.
    ///
    /// @return the map from game directory id to instance id
    public Map<String, String> getSelectedInstance() {
        return selectedInstance;
    }

    /// Returns the instance selected in a game directory.
    ///
    /// @param gameDirectoryId the game directory id, or `null`
    /// @return the instance id, or `null` when nothing is chosen there
    public @Nullable String getSelectedInstance(@Nullable String gameDirectoryId) {
        return gameDirectoryId == null ? null : selectedInstance.get(gameDirectoryId);
    }

    /// Records the instance selected in a game directory.
    ///
    /// A `null` instance clears the entry, which is what an empty directory
    /// leaves behind.
    ///
    /// @param gameDirectoryId the game directory id, or `null` to do nothing
    /// @param instanceId      the instance id, or `null` to clear
    public void setSelectedInstance(@Nullable String gameDirectoryId, @Nullable String instanceId) {
        if (gameDirectoryId == null) {
            return;
        }
        if (instanceId != null) {
            selectedInstance.put(gameDirectoryId, instanceId);
        } else {
            selectedInstance.remove(gameDirectoryId);
        }
        fireChanged();
    }

    public NodeSource nodeSource() {
        return nodeSource == null ? NodeSource.OFFICIAL : nodeSource;
    }

    public void setNodeSource(NodeSource nodeSource) {
        this.nodeSource = nodeSource;
        fireChanged();
    }

    public boolean isOpenBrowserOnLaunch() {
        return openBrowserOnLaunch;
    }

    public void setOpenBrowserOnLaunch(boolean openBrowserOnLaunch) {
        this.openBrowserOnLaunch = openBrowserOnLaunch;
        fireChanged();
    }

    /// Returns the arguments a new instance is launched with.
    ///
    /// @return the line, never `null`
    public String defaultLaunchArguments() {
        return defaultLaunchArguments == null ? "" : defaultLaunchArguments;
    }

    public void setDefaultLaunchArguments(String defaultLaunchArguments) {
        this.defaultLaunchArguments = defaultLaunchArguments;
        fireChanged();
    }

    /// Returns the command that runs before an instance starts.
    ///
    /// The instance's own command wins; without one, the launcher's applies.
    ///
    /// @param instanceId the instance
    /// @return the command, or an empty string for none
    public String preLaunchCommandFor(String instanceId) {
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance != null) {
            String own = DshInstanceSettings.preLaunchCommand(instance);
            if (own != null) {
                return own;
            }
        }
        return getPreLaunchCommand();
    }

    /// Returns the command that runs after an instance has ended.
    ///
    /// @param instanceId the instance
    /// @return the command, or an empty string for none
    public String postExitCommandFor(String instanceId) {
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance != null) {
            String own = DshInstanceSettings.postExitCommand(instance);
            if (own != null) {
                return own;
            }
        }
        return getPostExitCommand();
    }

    /// Returns whether an instance's log window opens when it launches.
    ///
    /// The instance's own answer wins; without one, the launcher's applies.
    ///
    /// @param instanceId the instance
    /// @return whether the window opens
    public boolean showLogsFor(String instanceId) {
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance != null) {
            Boolean own = DshInstanceSettings.showLogs(instance);
            if (own != null) {
                return own;
            }
        }
        return isShowLogs();
    }

    /// Returns whether debug lines are written for an instance.
    ///
    /// The instance's own answer wins; without one, the launcher's applies. One method, so the
    /// interface and the launch cannot disagree about which is in force.
    ///
    /// @param instanceId the instance
    /// @return whether debug lines are written
    public boolean debugLogFor(String instanceId) {
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance != null) {
            Boolean own = DshInstanceSettings.debugLog(instance);
            if (own != null) {
                return own;
            }
        }
        return isDebugLog();
    }

    /// Returns what the launcher does with itself while an instance runs.
    ///
    /// The instance's own choice wins; without one, the launcher's applies. One method, so
    /// the interface and the launch cannot disagree about which is in force.
    ///
    /// @param instanceId the instance
    /// @return the choice
    public DshLauncherVisibility launcherVisibilityFor(String instanceId) {
        DshInstance instance = DshInstanceManager.find(instanceId);
        if (instance != null) {
            String own = DshInstanceSettings.launcherVisibility(instance);
            if (own != null) {
                return DshLauncherVisibility.of(own);
            }
        }
        return launcherVisibility();
    }
}
