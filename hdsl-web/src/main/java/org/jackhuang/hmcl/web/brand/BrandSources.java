/*
 * HDSL-web
 * Copyright (C) 2026  HDSL-web contributors
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
package org.jackhuang.hmcl.web.brand;

import org.jackhuang.hmcl.setting.LauncherSettings;
import org.jackhuang.hmcl.setting.SettingsManager;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Where a brand install downloads from: the PyPI index (for `uv` itself and `uv sync`), the
/// CPython build (`UV_PYTHON_INSTALL_MIRROR`), and — per brand — the GitHub release base.
///
/// The shapes are the same as [org.jackhuang.hmcl.dsh.NpmRegistry]: presets measured from this
/// machine, an `environment` preset that follows the deployment, and a hand-typed address that is
/// validated before it can reach a child process. The setting beats the environment, which beats
/// the upstream the tool publishes, because the setting is the one a person can change and see
/// change.
///
/// **Measured on the deployment this was written for**: `pypi.org` / `files.pythonhosted.org` at
/// about 40 KB/s, `github.com` unreachable (20 s connect timeouts) while `api.github.com` answers;
/// `mirrors.aliyun.com/pypi` at 6.6 MB/s and the NJU GitHub-release mirror at 6.45 MB/s. The notes
/// in the presets are those numbers, so the settings page can say why it recommends one.
@NotNullByDefault
public final class BrandSources {

    /// The preset id meaning "use whatever the deployment says", which is the default.
    public static final String ENVIRONMENT = "environment";

    /// The preset id the settings API sends for a hand-typed address.
    public static final String CUSTOM = "custom";

    /// The longest address accepted; long enough for a mirror path, short enough that nothing
    /// interesting can hide in it.
    private static final int MAX_LENGTH = 200;

    /// Characters refused outright, on top of whitespace and control characters: the values are
    /// handed to `npm`/`uv` in their environment, so quoting, backslashes, YAML comments, shell
    /// metacharacters and userinfo (`user:pass@host`) are all shapes nobody meant to type.
    private static final String REFUSED = "\"'`\\#$;|&@";

    /// Where PyPI packages come from when nothing else says.
    public static final String PYPI_DEFAULT = "https://pypi.org/simple";

    /// Where the CPython build comes from when nothing else says.
    public static final String PYTHON_DEFAULT =
            "https://github.com/astral-sh/python-build-standalone/releases/download";

    /// Where version source archives come from when nothing else says: GitHub's codeload, which
    /// serves a tag's zip directly and does not need the git transport.
    public static final String ARCHIVE_DEFAULT = "https://codeload.github.com";

    /// One published mirror.
    ///
    /// @param id    the name used in settings files
    /// @param label what the settings page shows
    /// @param url   the address, empty for [#ENVIRONMENT], which names none of its own
    /// @param note  one line about the source, for the settings page
    public record Preset(String id, String label, String url, String note) {
    }

    /// An address in force, and where it came from.
    ///
    /// @param url    the address, normalised, empty when the kind has no source at all
    /// @param source `setting`, `environment` or `default` — what the settings page reports, so
    ///               that "I changed it and nothing happened" is answerable at a glance
    public record Effective(String url, String source) {
    }

    private static final List<Preset> PYPI_PRESETS = List.of(
            new Preset(ENVIRONMENT, "跟随部署环境（默认）", "",
                    "用 UV_DEFAULT_INDEX / PIP_INDEX_URL，都没有就用官方 PyPI"),
            new Preset("official", "PyPI 官方", PYPI_DEFAULT,
                    "海外源；本机实测约 40 KB/s"),
            new Preset("aliyun", "阿里云", "https://mirrors.aliyun.com/pypi/simple",
                    "本机实测约 6.6 MB/s"),
            new Preset("tuna", "清华 TUNA", "https://pypi.tuna.tsinghua.edu.cn/simple",
                    "教育网常用；JSON API 与 simple 索引都通"),
            new Preset("ustc", "中科大 USTC", "https://mirrors.ustc.edu.cn/pypi/simple",
                    "教育网常用"));

    private static final List<Preset> PYTHON_PRESETS = List.of(
            new Preset(ENVIRONMENT, "跟随部署环境（默认）", "",
                    "用 UV_PYTHON_INSTALL_MIRROR，没有就用 GitHub 官方"),
            new Preset("github", "GitHub 官方", PYTHON_DEFAULT,
                    "本机实测不可用（连接超时）"),
            new Preset("nju", "南京大学", "https://mirror.nju.edu.cn/github-release/astral-sh/python-build-standalone",
                    "本机实测约 6.45 MB/s；含 2025 年以来各 tag"));

    /// Version source archives: `codeload` serves `<base>/<slug>/zip/refs/tags/<tag>` directly, so a
    /// mirror only has to proxy one host — no git transport, no API.
    private static final List<Preset> ARCHIVE_PRESETS = List.of(
            new Preset(ENVIRONMENT, "跟随部署环境（默认）", "",
                    "用 CODELOAD_BASE，没有就用 GitHub 的 codeload"),
            new Preset("codeload", "GitHub codeload 官方", ARCHIVE_DEFAULT,
                    "版本源码 zip 的官方地址；本机实测 11 MB / 1.5 秒"));

    private BrandSources() {
    }

    /// Returns the PyPI mirrors offered, in the order the settings page shows them.
    public static List<Preset> pypiPresets() {
        return PYPI_PRESETS;
    }

    /// Returns the CPython mirrors offered.
    public static List<Preset> pythonPresets() {
        return PYTHON_PRESETS;
    }

    /// Returns the source-archive mirrors offered.
    public static List<Preset> archivePresets() {
        return ARCHIVE_PRESETS;
    }

    /// Returns the release bases offered for one brand.
    ///
    /// @param brand the brand
    /// @return `environment`, the upstream base, and nothing else — `custom` is the page's label
    ///         for a hand-typed address rather than a preset
    public static List<Preset> releasePresets(Brand brand) {
        return List.of(
                new Preset(ENVIRONMENT, "跟随部署环境（默认）", "",
                        brand == Brand.OCTOP
                                ? "用 OCTOP_RELEASE_BASE，没有就用 GitHub Releases"
                                : "没有环境变量时用 GitHub Releases"),
                new Preset("github", "GitHub 官方", defaultReleaseBase(brand),
                        brand == Brand.OCTOP
                                ? "海外源；portable 包约 260 MB，本机实测不可用"
                                : "海外源；本机实测不可用"));
    }

    /// The upstream release base for a brand: where its GitHub release assets live.
    ///
    /// @param brand the brand
    /// @return `<https://github.com/><slug>/releases/download`, empty for npm brands
    public static String defaultReleaseBase(Brand brand) {
        String slug = brand.githubSlug();
        return slug.isEmpty() ? "" : "https://github.com/" + slug + "/releases/download";
    }

    /// Finds a preset by its id.
    ///
    /// @param presets the list to search
    /// @param id      the id, or `null`
    /// @return the preset, or `null` when there is no such preset (including [#CUSTOM])
    public static @Nullable Preset preset(List<Preset> presets, @Nullable String id) {
        if (id == null) {
            return null;
        }
        String wanted = id.trim();
        for (Preset preset : presets) {
            if (preset.id().equals(wanted) && !preset.url().isEmpty()) {
                return preset;
            }
        }
        return null;
    }

    /// Where the PyPI index in force is.
    public static Effective pypi() {
        LauncherSettings settings = settingsOrNull();
        if (settings != null) {
            Effective chosen = fromSetting(
                    preset(PYPI_PRESETS, settings.getBrandPypiIndexPreset()),
                    settings.getBrandPypiIndexPreset(), settings.getBrandPypiIndex());
            if (chosen != null) {
                return chosen;
            }
        }
        return fromEnvironment(new String[]{"UV_DEFAULT_INDEX", "PIP_INDEX_URL"}, PYPI_DEFAULT);
    }

    /// Where the CPython build in force is.
    public static Effective python() {
        LauncherSettings settings = settingsOrNull();
        if (settings != null) {
            Effective chosen = fromSetting(
                    preset(PYTHON_PRESETS, settings.getBrandPythonMirrorPreset()),
                    settings.getBrandPythonMirrorPreset(), settings.getBrandPythonMirror());
            if (chosen != null) {
                return chosen;
            }
        }
        return fromEnvironment(new String[]{"UV_PYTHON_INSTALL_MIRROR"}, PYTHON_DEFAULT);
    }

    /// Where version source archives in force are.
    ///
    /// @return the base to append `/<slug>/zip/refs/tags/<tag>` to, and where it came from
    public static Effective archive() {
        LauncherSettings settings = settingsOrNull();
        if (settings != null) {
            Effective chosen = fromSetting(
                    preset(ARCHIVE_PRESETS, settings.getBrandArchivePreset()),
                    settings.getBrandArchivePreset(), settings.getBrandArchiveBase());
            if (chosen != null) {
                return chosen;
            }
        }
        return fromEnvironment(new String[]{"CODELOAD_BASE", "HDSL_CODELOAD_BASE"}, ARCHIVE_DEFAULT);
    }

    /// Where a brand's release assets in force are.
    ///
    /// @param brand the brand
    /// @return the base to append `<tag>/<asset>` to, and where it came from; empty for npm brands
    public static Effective release(Brand brand) {
        String fallback = defaultReleaseBase(brand);
        LauncherSettings settings = settingsOrNull();
        if (settings != null) {
            String presetId = settings.getBrandReleasePreset(brand.id());
            Effective chosen = fromSetting(
                    preset(releasePresets(brand), presetId),
                    presetId, settings.getBrandReleaseBase(brand.id()));
            if (chosen != null) {
                return chosen;
            }
        }
        String environment = brand == Brand.OCTOP ? System.getenv("OCTOP_RELEASE_BASE") : null;
        if (environment != null && !environment.isBlank()) {
            try {
                return new Effective(normalize(environment), "environment");
            } catch (Invalid e) {
                LOG.warning("OCTOP_RELEASE_BASE is not usable (" + e.getMessage() + "); falling back to "
                        + fallback);
            }
        }
        return new Effective(fallback, "default");
    }

    /// Reports whether an address may be used.
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
    /// @throws Invalid when the value is not an address this launcher will hand to uv or npm
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

        // Every consumer appends its own path segment (`/uv/`, `/uv-<version>-…whl`,
        // `/<tag>/<asset>`), so a trailing slash would make that two and mirrors answer 404.
        String normalized = text;
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new Invalid("地址格式不合法");
        }
        return normalized;
    }

    /// Resolves a setting: a published preset wins, then a hand-typed address.
    ///
    /// @return the address, or `null` when the setting names nothing usable and the caller should
    ///         fall through to the environment
    private static @Nullable Effective fromSetting(@Nullable Preset preset, @Nullable String presetId,
                                                   @Nullable String custom) {
        if (preset != null) {
            return new Effective(preset.url(), "setting");
        }
        if (presetId != null && CUSTOM.equals(presetId.trim())) {
            try {
                return new Effective(normalize(custom), "setting");
            } catch (Invalid e) {
                // Kept rather than corrected: the settings API refuses these, so reaching here
                // means the file was edited by hand, and a wrong address is worse than none.
                LOG.warning("A brand download source in the settings is not usable ("
                        + e.getMessage() + "); falling back to the environment");
            }
        }
        return null;
    }

    /// Resolves the environment, then the upstream default.
    private static Effective fromEnvironment(String[] names, String fallback) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                try {
                    return new Effective(normalize(value), "environment");
                } catch (Invalid e) {
                    LOG.warning(name + " is not usable (" + e.getMessage() + "); ignoring it");
                }
            }
        }
        return new Effective(fallback, "default");
    }

    private static @Nullable LauncherSettings settingsOrNull() {
        try {
            return SettingsManager.settings();
        } catch (RuntimeException e) {
            // Settings that cannot be read are settings that were never chosen: the environment
            // and the upstream default still answer, which is what an unconfigured panel does.
            LOG.warning("Could not read the brand download-source settings", e);
            return null;
        }
    }

    /// An address this launcher refuses to hand to a child process.
    public static final class Invalid extends Exception {
        private static final long serialVersionUID = 1L;

        Invalid(String message) {
            super(message);
        }
    }
}