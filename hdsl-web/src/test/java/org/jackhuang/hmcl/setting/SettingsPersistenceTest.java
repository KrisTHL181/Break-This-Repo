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
import org.jackhuang.hmcl.dsh.DshIsolationPolicy;
import org.jackhuang.hmcl.dsh.DshLauncherVisibility;
import org.jackhuang.hmcl.dsh.DshProxyMode;
import org.jackhuang.hmcl.dsh.DshVendor;
import org.jackhuang.hmcl.dsh.NodeSource;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Verifies that every setting the server can change is one the settings file
/// holds.
///
/// The desktop build checked this at source level: it grepped the settings
/// class for `…Property()` accessors and required each name to appear in both
/// halves of the persistence mirror, because loading the class needed a UI
/// toolkit a headless test run did not have. The HDSL-web settings are plain
/// objects, so the same guarantee is checked the direct way: put a distinctive
/// value in every setting, run it through the same snapshot the persistence
/// layer writes, and read it back into a fresh object. A setting the snapshot
/// forgets — in either direction — comes home wrong.
///
/// The mistake this guards is the one that lost seventeen settings at once:
/// a value somebody can change, watch take effect, and lose at the next start,
/// with nothing said about it.
class SettingsPersistenceTest {
    @Test
    void everySettingSurvivesTheSnapshotRoundTrip() {
        LauncherSettings written = new LauncherSettings();
        written.setDefaultNodeRuntime("managed:22.19.0");
        written.setDefaultHomeMode(DshHomeMode.VERSION_SHARED);
        written.setGameDirectories(List.of(GameDirectory.defaultDirectory()));
        written.setSelectedGameDirectoryId("default");
        written.setBuildScriptPolicy(DshBuildScriptPolicy.AUTO);
        written.setDebugLog(true);
        written.setPluginCatalogUrl("https://plugins.example.com/index.json");
        written.setPackMarketUrl("https://packs.example.com/index.json");
        written.setDependencyPolicy(DshDependencyPolicy.LOCKED);
        written.setIsolationPolicy(DshIsolationPolicy.ALWAYS);
        written.setLauncherVisibility(DshLauncherVisibility.HIDE);
        written.setShowLogs(true);
        written.setPreLaunchCommand("echo before");
        written.setPostExitCommand("echo after");
        written.setGlobalEnvironment(Map.of("A", "1", "B", "2"));
        written.setProxyMode(DshProxyMode.SOCKS);
        written.setProxyHost("proxy.example.com");
        written.setProxyPort("1080");
        written.setProxyAuthenticated(true);
        written.setProxyUser("user");
        written.setProxyPassword("secret");
        written.setCacheDirectoryCustom(true);
        written.setCacheDirectory("/tmp/hdsl-cache");
        written.setAutoDownloadThreads(false);
        written.setDownloadConcurrency(16);
        written.getAccounts().add(new DshAccount("deepseek", "sk-test", null, "mine"));
        written.getCustomVendors().add(DshVendor.discovered("agg", "Aggregator", "https://agg.example.com"));
        written.setActiveAccountKey("key-1");
        written.setSelectedInstance("default", "an-instance");
        written.setNodeSource(NodeSource.MIRROR);
        written.setOpenBrowserOnLaunch(false);
        written.setDefaultLaunchArguments("--verbose");
        written.setNpmRegistryPreset("npmmirror");
        written.setNpmRegistry("https://nexus.example.com/repository/npm");

        // The same JSON the persistence layer writes: a snapshot serialised
        // with the shared Gson configuration, then read back field by field.
        String json = JsonUtils.GSON.toJson(SettingsManager.Snapshot.of(written));
        SettingsManager.Snapshot snapshot =
                JsonUtils.fromNonNullJson(json, SettingsManager.Snapshot.class);

        LauncherSettings read = new LauncherSettings();
        snapshot.applyTo(read);

        assertEquals(written.defaultNodeRuntime(), read.defaultNodeRuntime());
        assertEquals(written.defaultHomeMode(), read.defaultHomeMode());
        assertEquals(written.getGameDirectories(), read.getGameDirectories());
        assertEquals(written.getSelectedGameDirectoryId(), read.getSelectedGameDirectoryId());
        assertEquals(written.buildScriptPolicy(), read.buildScriptPolicy());
        assertEquals(written.isDebugLog(), read.isDebugLog());
        assertEquals(written.getPluginCatalogUrl(), read.getPluginCatalogUrl());
        assertEquals(written.getPackMarketUrl(), read.getPackMarketUrl());
        assertEquals(written.dependencyPolicy(), read.dependencyPolicy());
        assertEquals(written.isolationPolicy(), read.isolationPolicy());
        assertEquals(written.launcherVisibility(), read.launcherVisibility());
        assertEquals(written.isShowLogs(), read.isShowLogs());
        assertEquals(written.getPreLaunchCommand(), read.getPreLaunchCommand());
        assertEquals(written.getPostExitCommand(), read.getPostExitCommand());
        assertEquals(written.globalEnvironment(), read.globalEnvironment());
        assertEquals(written.proxyMode(), read.proxyMode());
        assertEquals(written.getProxyHost(), read.getProxyHost());
        assertEquals(written.getProxyPort(), read.getProxyPort());
        assertEquals(written.isProxyAuthenticated(), read.isProxyAuthenticated());
        assertEquals(written.getProxyUser(), read.getProxyUser());
        assertEquals(written.getProxyPassword(), read.getProxyPassword());
        assertEquals(written.isCacheDirectoryCustom(), read.isCacheDirectoryCustom());
        assertEquals(written.getCacheDirectory(), read.getCacheDirectory());
        assertEquals(written.isAutoDownloadThreads(), read.isAutoDownloadThreads());
        assertEquals(written.getDownloadConcurrency(), read.getDownloadConcurrency());
        assertEquals(written.getAccounts().size(), read.getAccounts().size());
        assertEquals(written.getAccounts().get(0).key(), read.getAccounts().get(0).key());
        assertEquals(written.getCustomVendors().size(), read.getCustomVendors().size());
        assertEquals(written.getCustomVendors().get(0).id(), read.getCustomVendors().get(0).id());
        assertEquals(written.activeAccountKey(), read.activeAccountKey());
        assertEquals(Map.of("default", "an-instance"), read.getSelectedInstance());
        assertEquals(written.nodeSource(), read.nodeSource());
        assertEquals(written.isOpenBrowserOnLaunch(), read.isOpenBrowserOnLaunch());
        assertEquals(written.defaultLaunchArguments(), read.defaultLaunchArguments());
        // The download source: a mirror chosen in the panel that did not survive a restart would be
        // a choice that looks applied and is silently back to npmjs.org at the next start.
        assertEquals(written.getNpmRegistryPreset(), read.getNpmRegistryPreset());
        assertEquals(written.getNpmRegistry(), read.getNpmRegistry());
    }

    /// The selection written before selections were kept per folder is still
    /// honoured, and is not written back over a choice made since.
    @Test
    void theLegacySelectionStillCounts() {
        LauncherSettings settings = new LauncherSettings();
        SettingsManager.applyLegacySelection(settings, "an-instance-from-an-older-launcher");

        assertEquals("an-instance-from-an-older-launcher",
                settings.getSelectedInstance(GameDirectory.DEFAULT_ID),
                "the one selection an older launcher kept belonged to the folder it owns");

        LauncherSettings migrated = new LauncherSettings();
        migrated.setSelectedInstance(GameDirectory.DEFAULT_ID, "chosen-since");
        SettingsManager.applyLegacySelection(migrated, "an-instance-from-an-older-launcher");
        assertEquals("chosen-since", migrated.getSelectedInstance(GameDirectory.DEFAULT_ID));
    }
}
