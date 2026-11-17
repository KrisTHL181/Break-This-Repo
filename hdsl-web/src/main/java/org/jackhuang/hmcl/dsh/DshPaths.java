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

import org.jackhuang.hmcl.Metadata;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/// The directory layout HDSL owns on disk.
///
/// Two distinct kinds of state live here and must not be confused:
///
/// - [#VERSIONS] holds DeepSeek Harness *installations* (code). Several
///   versions coexist, each in its own npm prefix.
/// - [#INSTANCES] holds launcher *instances*. An instance owns a `DSH_HOME`
///   and a profile, and references one installed version.
///
/// Keeping them separate is what lets two instances share one version while
/// still keeping their profiles, sessions and credentials apart.
@NotNullByDefault
public final class DshPaths {
    private DshPaths() {
    }

    /// The root of all HDSL user data.
    public static final Path ROOT = Metadata.HMCL_USER_HOME;

    /// One directory per launcher instance.
    public static final Path INSTANCES = ROOT.resolve("instances");

    /// Holds the homes that are shared between instances of one version.
    public static final Path HOMES = ROOT.resolve("homes");

    /// One directory per Node.js runtime the launcher installed.
    ///
    /// Kept separate from [#VERSIONS] because a DeepSeek Harness version and a
    /// Node runtime are independent choices: many instances share one runtime,
    /// and one instance can be repinned without touching the other.
    public static final Path RUNTIMES = ROOT.resolve("runtimes");

    /// Cached copies of remote catalogues such as the quick-install presets.
    public static final Path CATALOG = ROOT.resolve("catalog");

    /// Where the logs a person exports from the log window are written.
    ///
    /// **Not** the process's working directory, which is what an export relative to it amounts to:
    /// what somebody exports is something they will look for again, and a file written into
    /// whatever directory the launcher happened to be started in is a file they cannot find.
    public static final Path LOGS = ROOT.resolve("logs");

    /// Returns the npm prefix directory for a DeepSeek Harness version.
    ///
    /// The directory is returned whether or not it exists.
    ///
    /// @param version the version string, which must be a safe path segment
    /// @return the version's prefix directory
    /// @throws DshException when the version string cannot be used as a directory name
    /// The home shared between every instance running one version.
    ///
    /// Outside the version's own files: each instance has its own copy of the
    /// runtime now, and a home that two instances share cannot live inside one
    /// of them.
    ///
    /// @param version the version
    /// @return the home directory
    /// @throws DshException when the version is not usable as a path segment
    public static Path versionHomeDirectory(String version) throws DshException {
        return HOMES.resolve(requireSafeSegment(version, "version"));
    }

    /// The directory holding an instance's own copy of DeepSeek Harness.
    ///
    /// Its own rather than shared between every instance that runs the same
    /// version. Sharing is cheaper on paper and worse in practice: the boot
    /// library is installed into the copy, so changing it for one instance
    /// changed it for all of them, and an instance that carries its own copy is
    /// an instance that can be moved, exported or thrown away whole. pnpm links
    /// a second copy of the same packages from a store it keeps, so the second
    /// one costs a fraction of the first.
    ///
    /// @param instanceId the instance
    /// @return the directory
    /// @throws DshException when the identifier is not usable as a path segment
    public static Path instanceVersionDirectory(String instanceId) throws DshException {
        return instanceDirectory(instanceId).resolve("dsh");
    }

    /// Returns the directory for a launcher-managed Node runtime.
    ///
    /// @param version the Node version, which must be a safe path segment
    /// @return the runtime directory
    /// @throws DshException when the version cannot be used as a directory name
    public static Path runtimeDirectory(String version) throws DshException {
        return RUNTIMES.resolve(requireSafeSegment(version, "runtime version"));
    }

    /// Returns the launcher instance directory for an instance id.
    ///
    /// The directory is returned whether or not it exists.
    ///
    /// @param instanceId the instance identifier
    /// @return the instance directory
    /// @throws DshException when the identifier cannot be used as a directory name
    public static Path instanceDirectory(String instanceId) throws DshException {
        return INSTANCES.resolve(requireSafeSegment(instanceId, "instance id"));
    }

    /// Validates a string for use as a single path segment.
    ///
    /// @param value the candidate segment
    /// @param what  a human-readable name for the value, used in the error message
    /// @return the trimmed segment
    /// @throws DshException when the value is empty or contains path separators
    /// Reports whether a value can be used as one segment of a path.
    ///
    /// The same rule [requireSafeSegment] enforces, asked in advance so that a
    /// page can refuse a name before it is used to make a directory.
    ///
    /// @param value the value to test
    /// @return whether it is usable
    public static boolean isUsableSegment(@Nullable String value) {
        String trimmed = value == null ? "" : value.trim();
        return !trimmed.isEmpty()
                && !trimmed.equals(".")
                && !trimmed.equals("..")
                && !trimmed.contains("/")
                && !trimmed.contains("\\")
                && trimmed.indexOf('\0') < 0;
    }

    private static String requireSafeSegment(String value, String what) throws DshException {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()
                || trimmed.equals(".")
                || trimmed.equals("..")
                || trimmed.contains("/")
                || trimmed.contains("\\")
                || trimmed.indexOf('\0') >= 0) {
            throw new DshException("Invalid " + what + ": " + value);
        }
        return trimmed;
    }
}
