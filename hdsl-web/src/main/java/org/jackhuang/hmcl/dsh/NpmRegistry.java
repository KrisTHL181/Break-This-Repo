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

import java.net.URI;
import java.util.List;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Where npm packages are fetched from, and the mirrors the settings page offers.
///
/// A launcher that installs everything through npm is a launcher whose first bad minute is a
/// registry it cannot reach: the guest network here reaches `registry.npmjs.org` at tens of
/// kilobytes a second, which turns an install of two thousand packages into an apparent hang.
/// The mirror list below is the answer, and every entry was measured from this machine before it
/// was written down — the two whose numbers were bad (`registry.yarnpkg.com`, which mirrors the
/// slow registry anyway) or that no longer exist (`registry.npm.taobao.org`, `r.cnpmjs.org`) are
/// deliberately absent rather than offered as a trap.
///
/// **What is validated here is where the value ends up, not where it came from.** The registry is
/// written into pnpm's own `config.yaml` and passed to child processes on their command lines, so
/// a value carrying a newline could inject a second setting and one carrying a shell metacharacter
/// is a value nobody meant to type. [normalize] is that gate, and both the settings API and the
/// code that writes the file go through it — refusing at write time and normalising again at use
/// time, because a settings file outlives the build that wrote it and can also be edited by hand.
@NotNullByDefault
public final class NpmRegistry {

    /// The registry npm publishes itself, and the value an unconfigured launcher uses.
    public static final String DEFAULT = "https://registry.npmjs.org";

    /// The preset id meaning "use whatever the deployment says", which is the default.
    public static final String ENVIRONMENT = "environment";

    /// The preset id the settings API sends for a hand-typed address.
    public static final String CUSTOM = "custom";

    /// One published mirror.
    ///
    /// @param id              the name used in settings files
    /// @param label           what the settings page shows
    /// @param url             the registry, empty for [#ENVIRONMENT], which names none of its own
    /// @param electronMirror  where Electron's own binaries are published beside this registry, or
    ///                        `null` when nothing is known to be — the ZCode build downloads one,
    ///                        and GitHub's copy is unreadable from the network this list is for
    /// @param note            one line about the source, for the settings page
    public record Preset(String id, String label, String url, @Nullable String electronMirror,
                         String note) {
    }

    /// The mirrors offered, in the order the settings page shows them.
    private static final List<Preset> PRESETS = List.of(
            new Preset(ENVIRONMENT, "跟随部署环境（默认）", "", null,
                    "用容器启动时的 NPM_CONFIG_REGISTRY，没有就用官方源"),
            new Preset("npmjs", "npm 官方", DEFAULT, null,
                    "全球发布源；本机实测约 28 KB/s"),
            new Preset("npmmirror", "npmmirror（淘宝）", "https://registry.npmmirror.com",
                    "https://npmmirror.com/mirrors/electron/", "本机实测约 25 MB/s"),
            new Preset("ustc", "中科大 USTC", "https://npmreg.proxy.ustclug.org", null,
                    "本机实测约 18 MB/s"),
            new Preset("tencent", "腾讯云", "https://mirrors.cloud.tencent.com/npm", null,
                    "本机实测约 14 MB/s"),
            new Preset("huawei", "华为云", "https://repo.huaweicloud.com/repository/npm", null,
                    "本机实测约 11 MB/s"));

    /// The longest address accepted. Long enough for a Nexus path, short enough that nothing
    /// interesting can be hidden in it.
    private static final int MAX_LENGTH = 200;

    /// Characters refused outright, on top of whitespace and control characters.
    ///
    /// Quoting and backslashes because the value is written into YAML and handed to pnpm; `#`
    /// because YAML starts a comment with it; the shell metacharacters because one of these
    /// addresses may yet end up inside a command somebody pasted; `@` because a registry needs no
    /// userinfo and `https://user:pass@host/` is the shape of an address that leaks one.
    private static final String REFUSED = "\"'`\\#$;|&@";

    /// The registry in force, and where it came from.
    ///
    /// @param registry the address, normalised
    /// @param source   `setting`, `environment` or `default` — what the settings page reports, so
    ///                 that "I changed it and nothing happened" is answerable at a glance
    public record Effective(String registry, String source) {
    }

    private NpmRegistry() {
    }

    /// Returns the mirrors offered.
    ///
    /// @return the presets
    public static List<Preset> presets() {
        return PRESETS;
    }

    /// Finds a preset by its id.
    ///
    /// @param id the id, or `null`
    /// @return the preset, or `null` when there is no such preset (including [#CUSTOM])
    public static @Nullable Preset preset(@Nullable String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim();
        for (Preset preset : PRESETS) {
            if (preset.id().equals(wanted) && !preset.url().isEmpty()) {
                return preset;
            }
        }
        return null;
    }

    /// Returns the address in force, preferring the panel's setting over the deployment's.
    ///
    /// The order is the setting, then the environment, then the published registry. The setting
    /// wins because it is the one a person can change and see change; the environment is what the
    /// container was started with, which is what an operator without a panel would use; and the
    /// default is right for a network that is not this one.
    ///
    /// @return the address and where it came from
    public static Effective effective() {
        String presetId = null;
        String custom = null;
        try {
            org.jackhuang.hmcl.setting.LauncherSettings settings =
                    org.jackhuang.hmcl.setting.SettingsManager.settings();
            presetId = settings.getNpmRegistryPreset();
            custom = settings.getNpmRegistry();
        } catch (RuntimeException e) {
            // Settings that cannot be read are settings that were never chosen: the environment
            // and the default still answer, which is what an unconfigured launcher does.
            LOG.warning("Could not read the npm registry setting", e);
        }

        Preset chosen = preset(presetId);
        if (chosen != null) {
            return new Effective(chosen.url(), "setting");
        }
        if (CUSTOM.equals(presetId == null ? "" : presetId.trim())) {
            try {
                return new Effective(normalize(custom), "setting");
            } catch (Invalid e) {
                // Kept rather than corrected: the settings API refuses these, so reaching here
                // means the file was edited by hand, and a wrong address is worse than none.
                LOG.warning("The npm registry in the settings is not usable (" + e.getMessage()
                        + "); falling back to the environment");
            }
        }

        String environment = System.getenv("NPM_CONFIG_REGISTRY");
        if (environment != null && !environment.isBlank()) {
            try {
                return new Effective(normalize(environment), "environment");
            } catch (Invalid e) {
                LOG.warning("NPM_CONFIG_REGISTRY is not usable (" + e.getMessage()
                        + "); falling back to " + DEFAULT);
            }
        }
        return new Effective(DEFAULT, "default");
    }

    /// Where Electron's own binaries are published beside the registry in force.
    ///
    /// @return the mirror, or `null` when nothing is known about one
    public static @Nullable String electronMirror() {
        String presetId = null;
        try {
            presetId = org.jackhuang.hmcl.setting.SettingsManager.settings().getNpmRegistryPreset();
        } catch (RuntimeException e) {
            LOG.warning("Could not read the npm registry setting", e);
        }
        Preset preset = preset(presetId);
        return preset == null ? null : preset.electronMirror();
    }

    /// Reports whether an address may be used.
    ///
    /// @param value the address, or `null`
    /// @return whether [normalize] would accept it
    public static boolean isValid(@Nullable String value) {
        try {
            normalize(value);
            return true;
        } catch (Invalid e) {
            return false;
        }
    }

    /// Checks an address and returns it in the one spelling the rest of the launcher uses.
    ///
    /// @param value the address, or `null`
    /// @return the address, without its trailing slash
    /// @throws Invalid when the value is not an address this launcher will hand to pnpm
    public static String normalize(@Nullable String value) throws Invalid {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new Invalid("请填写下载源地址");
        }
        if (text.length() > MAX_LENGTH) {
            throw new Invalid("地址太长（最多 " + MAX_LENGTH + " 个字符）");
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                throw new Invalid("地址里不能有换行或控制字符");
            }
            if (Character.isWhitespace(c)) {
                throw new Invalid("地址里不能有空格");
            }
            if (REFUSED.indexOf(c) >= 0) {
                throw new Invalid("地址里不能有字符 " + c);
            }
        }

        URI uri;
        try {
            uri = URI.create(text);
        } catch (IllegalArgumentException e) {
            throw new Invalid("地址格式不合法");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new Invalid("只支持 http 或 https 地址");
        }
        if (uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new Invalid("地址里没有主机名");
        }
        if (uri.getUserInfo() != null) {
            throw new Invalid("地址里不能带用户名或密码");
        }
        if (uri.getQuery() != null || uri.getFragment() != null) {
            throw new Invalid("地址里不能带查询串或锚点");
        }

        // A registry is a base URL, and corepack joins its own `/pnpm/latest` to it: with a
        // trailing slash that is two, and mirrors answer those with 404.
        String normalized = text;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new Invalid("地址格式不合法");
        }
        return normalized;
    }

    /// An address this launcher will not use.
    ///
    /// The message is shown to whoever typed it, so it says what is wrong rather than what the
    /// rule is.
    public static final class Invalid extends Exception {
        private static final long serialVersionUID = 1L;

        Invalid(String message) {
            super(message);
        }
    }
}
