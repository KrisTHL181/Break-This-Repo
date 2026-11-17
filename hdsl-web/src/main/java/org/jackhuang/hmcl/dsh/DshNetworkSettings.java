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

import java.util.LinkedHashMap;
import java.util.Map;

/// The network settings every child process is given.
///
/// Downloads happen in whatever the package manager runs — reading a version list, resolving a
/// profile, installing a plugin, fetching a package — and a package manager is configured through
/// the environment it inherits. So the settings live here and are merged into every child this
/// launcher starts, in one place rather than at each call site: a proxy that applied to some
/// downloads and not others would be worse than no setting at all, because nothing would say which
/// ones went where.
///
/// Empty settings contribute nothing, so a launcher nobody configured starts its children with
/// exactly the environment it always did.
@NotNullByDefault
public final class DshNetworkSettings {

    /// Where node-gyp reads its headers from when the registry in force is not the published one.
    ///
    /// The same judgement the registry setting makes, applied to the other download a native build
    /// makes: nodejs.org publishes them at `v<version>/node-v<version>-headers.tar.gz` under this
    /// base, which is exactly how this mirror lays them out.
    private static final String NODE_HEADER_MIRROR = "https://npmmirror.com/mirrors/node";

    private DshNetworkSettings() {
    }

    /// Returns the variables the launcher's network settings contribute.
    ///
    /// @return the variables, empty when nothing is configured
    public static Map<String, String> environment() {
        Map<String, String> environment = new LinkedHashMap<>();
        try {
            org.jackhuang.hmcl.setting.LauncherSettings settings =
                    org.jackhuang.hmcl.setting.SettingsManager.settings();
            DshProxyMode mode = settings.proxyMode();
            switch (mode) {
                case SYSTEM -> {
                    // Nothing: whatever the system uses is what a child inherits anyway.
                }
                case NONE -> {
                    // Said out loud, because a child that inherits a proxy from the system would
                    // otherwise keep using one after being told not to.
                    environment.put("HTTP_PROXY", "");
                    environment.put("HTTPS_PROXY", "");
                    environment.put("NO_PROXY", "*");
                }
                case HTTP, SOCKS -> {
                    String address = address(settings, mode);
                    if (address != null) {
                        environment.put("HTTP_PROXY", address);
                        environment.put("HTTPS_PROXY", address);
                        // Some tools read only this one, and a SOCKS proxy is usually named here.
                        environment.put("ALL_PROXY", address);
                    }
                }
            }

            // npm reads its registry from the environment; pnpm does not — it reads the
            // config.yaml that PnpmConfigFile writes. Both are set: this half is what `npm view`
            // and `npm install --global` consult, and it is the value an operator looking at a
            // child's environment would expect to find here.
            String registry = NpmRegistry.effective().registry();
            environment.put("npm_config_registry", registry);
            environment.put("NPM_CONFIG_REGISTRY", registry);

            // Compiling a native module makes node-gyp download a ten-megabyte headers tarball from
            // nodejs.org — and download it again on every container that has not built one yet,
            // because the cache lives in the home directory, which a redeployed container does not
            // keep. That host is as slow from here as the registry this setting exists for
            // (measured: 104 KB/s against 5.7 MB/s from the mirror), so a machine told to use a
            // mirror is told to use the Node mirror beside it. The published registry keeps
            // node-gyp's own default: a machine that reaches npmjs.org reaches nodejs.org.
            if (!NpmRegistry.DEFAULT.equals(registry)) {
                environment.put("NODEJS_ORG_MIRROR", NODE_HEADER_MIRROR);
                environment.put("npm_config_disturl", NODE_HEADER_MIRROR);
            }

            // node-gyp runs `make` without `-j` unless it is told how many jobs it may start, and
            // it reads the count from here. This is the difference between compiling node-pty in
            // seconds and in minutes on a machine with twelve cores.
            String jobs = Integer.toString(Math.max(1, Runtime.getRuntime().availableProcessors()));
            environment.put("JOBS", jobs);
            environment.put("npm_config_jobs", jobs);

            Integer concurrency = settings.getDownloadConcurrency();
            if (concurrency != null && concurrency > 0) {
                // npm reads its configuration from the environment, and `maxsockets` is the key
                // that decides how many requests it makes at once. The name matters: npm answers an
                // unknown `npm_config_*` variable with a warning on **stdout**, which is the same
                // stream the answers come back on.
                environment.put("npm_config_maxsockets", Integer.toString(concurrency));
            }
        } catch (RuntimeException e) {
            // A launcher whose settings cannot be read is not a reason to fail every command: it
            // simply has no proxy, which is what an unconfigured one has too.
            org.jackhuang.hmcl.util.logging.Logger.LOG.warning("Could not read the network settings", e);
        }
        return environment;
    }

    /// Builds the address of the proxy the settings describe.
    ///
    /// @param settings the settings
    /// @param mode     the chosen mode
    /// @return the address, or `null` when there is no host to reach
    private static @org.jetbrains.annotations.Nullable String address(
            org.jackhuang.hmcl.setting.LauncherSettings settings, DshProxyMode mode) {
        String host = settings.getProxyHost();
        if (host == null || host.isBlank()) {
            return null;
        }
        String port = settings.getProxyPort();
        String credentials = "";
        if (settings.isProxyAuthenticated()) {
            String user = settings.getProxyUser();
            String password = settings.getProxyPassword();
            if (user != null && !user.isBlank()) {
                credentials = user + ":" + (password == null ? "" : password) + "@";
            }
        }
        String scheme = mode == DshProxyMode.SOCKS ? "socks5" : "http";
        return scheme + "://" + credentials + host.trim()
                + (port == null || port.isBlank() ? "" : ":" + port.trim());
    }

    /// Adds a variable when it holds something.
    ///
    /// @param environment the variables
    /// @param name        the variable's name
    /// @param value       its value, or `null` or blank for none
    private static void add(Map<String, String> environment, String name, String value) {
        if (value != null && !value.isBlank()) {
            environment.put(name, value.trim());
        }
    }
}
