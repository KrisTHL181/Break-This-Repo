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
package org.jackhuang.hmcl;

import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.platform.OperatingSystem;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.file.Path;

/// Stores metadata about the Hello DeepSeek! Launcher application and the
/// directories it owns.
///
/// All launcher-owned data lives under a single user home so that HDSL can
/// be installed next to a real HMCL without either application seeing the
/// other's settings.
@NotNullByDefault
public final class Metadata {
    private Metadata() {
    }

    /// The short product name.
    public static final String NAME = "HDSL";

    /// The full product name shown in window titles.
    public static final String FULL_NAME = "Hello DeepSeek! Launcher";

    /// The running version.
    ///
    /// The build script decides what this string says: a release carries the tag's
    /// version, and anything else carries the commit it was built from, as in
    /// `0.1.0+g1a613c5`. A package built here is therefore never mistaken for one a tag
    /// published, which is what the window title shows.
    ///
    /// Resolution order: an explicit override (used by `gradlew run`, which has
    /// no packaged manifest to read), then the jar manifest written by the
    /// shadow task, then a development placeholder.
    public static final String VERSION = resolveVersion();

    /// Resolves the running version.
    ///
    /// Order: an explicit override, which `gradlew run` passes because a class
    /// directory has no manifest; then the jar manifest written by the shadow
    /// task; then a development placeholder.
    ///
    /// @return the version string
    private static String resolveVersion() {
        String override = System.getProperty("hdsl.version.override");
        if (StringUtils.isNotBlank(override)) {
            return override;
        }
        String fromManifest = Metadata.class.getPackage().getImplementationVersion();
        return StringUtils.isNotBlank(fromManifest) ? fromManifest : "0.1.0-dev";
    }

    /// The window title including the version.
    public static final String TITLE = NAME + " " + VERSION;

    /// The full window title including the version.
    public static final String FULL_TITLE = FULL_NAME + " v" + VERSION;

    /// The HDSL project page. The title bar's help button, the crash window's help
    /// button and the About page's product row all open it.
    public static final String HOMEPAGE_URL = "https://github.com/MCXCC303/HDSL";

    /// The identifier used for Linux desktop integration and window grouping.
    public static final String APPLICATION_ID = "run.hdsl.HDSL";

    /// The identifier used for Windows taskbar grouping.
    ///
    /// Retained only so the transplanted window code keeps compiling; HDSL
    /// does not target Windows.
    public static final String WINDOWS_APP_USER_MODEL_ID = APPLICATION_ID;

    /// The directory the launcher was started from.
    public static final Path CURRENT_DIRECTORY = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

    /// The per-user directory holding shared HDSL data.
    public static final Path HMCL_USER_HOME;

    /// The per-workspace directory holding HDSL configuration and state.
    public static final Path HMCL_LOCAL_HOME;

    /// The directory holding runtime dependencies downloaded by the launcher.
    public static final Path DEPENDENCIES_DIRECTORY;

    static {
        String userHome = System.getProperty("hdsl.home", System.getenv("HDSL_USER_HOME"));
        if (StringUtils.isBlank(userHome)) {
            if (OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                String xdgData = System.getenv("XDG_DATA_HOME");
                HMCL_USER_HOME = StringUtils.isNotBlank(xdgData)
                        ? Path.of(xdgData, "hdsl").toAbsolutePath().normalize()
                        : Path.of(System.getProperty("user.home"), ".local", "share", "hdsl").toAbsolutePath().normalize();
            } else {
                HMCL_USER_HOME = OperatingSystem.getWorkingDirectory("hdsl");
            }
        } else {
            HMCL_USER_HOME = Path.of(userHome).toAbsolutePath().normalize();
        }

        String localHome = System.getProperty("hdsl.dir", System.getenv("HDSL_LOCAL_HOME"));
        HMCL_LOCAL_HOME = StringUtils.isNotBlank(localHome)
                ? Path.of(localHome).toAbsolutePath().normalize()
                : HMCL_USER_HOME;

        String dependencies = System.getProperty("hdsl.dependencies.dir", System.getenv("HDSL_DEPENDENCIES_DIR"));
        DEPENDENCIES_DIRECTORY = StringUtils.isNotBlank(dependencies)
                ? Path.of(dependencies).toAbsolutePath().normalize()
                : HMCL_USER_HOME.resolve("dependencies");
    }
}
