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

import java.util.List;
import java.util.Locale;

/// One of the application surfaces DeepSeek Harness can boot a profile into.
///
/// The surface is inferred from the profile name, because that is how upstream
/// models it: the shipped profile templates are named after the app they boot,
/// and the app plugin adds its own flags once the launcher hands over control.
@NotNullByDefault
public enum DshSurface {
    /// The browser interface. Served over local HTTP until stopped.
    WEB("web", "dsh-web-app", List.of("--no-open")),

    /// Answers one task and exits. Output is NDJSON when `--json` is passed.
    HEADLESS("headless", "dsh-headless", List.of()),

    /// Speaks the Agent Client Protocol over stdio.
    ACP("acp", "dsh-acp-app", List.of()),

    /// Exposes the programmatic SDK over stdio.
    SDK("sdk", "dsh-sdk-app", List.of()),

    /// A profile that does not match any shipped surface.
    OTHER("", "", List.of());

    private final String profileName;
    private final String appBundle;
    private final List<String> arguments;

    DshSurface(String profileName, String appBundle, List<String> arguments) {
        this.profileName = profileName;
        this.appBundle = appBundle;
        this.arguments = arguments;
    }

    /// Returns the bundle whose presence means a profile boots this surface.
    ///
    /// This is what a profile *is*, as opposed to what it is called: the app a profile boots is one
    /// of its bundles, and the bundle is named by the package rather than by whoever wrote the
    /// profile.
    ///
    /// @return the package name
    public String appBundle() {
        return appBundle;
    }

    /// Returns the profile template this surface boots.
    ///
    /// @return the profile name
    public String profileName() {
        return profileName;
    }

    /// Returns the flags the launcher must add for this surface.
    ///
    /// The browser surface is always started with `--no-open`: the launcher
    /// opens the browser itself once the readiness line arrives.
    ///
    /// @return the extra arguments, never `null`
    public List<String> arguments() {
        return arguments;
    }

    /// Returns the flags for this surface on a specific port.
    ///
    /// The port is always supplied rather than left to the kernel. DeepSeek
    /// Harness exits with status 1 when its port is taken instead of probing
    /// for another one, so the launcher has to choose a free port itself — and
    /// it has to keep choosing the same one, because the browser interface keys
    /// session state by origin.
    ///
    /// @param port the port to bind, or `0` to let the kernel choose
    /// @return the extra arguments, never `null`
    public List<String> arguments(int port) {
        if (this != WEB) {
            return arguments;
        }
        List<String> result = new java.util.ArrayList<>(arguments);
        result.add("--port");
        result.add(Integer.toString(Math.max(port, 0)));
        return List.copyOf(result);
    }

    /// Reports whether this surface serves a local HTTP endpoint.
    ///
    /// @return whether a readiness URL line is expected on stdout
    public boolean isWeb() {
        return this == WEB;
    }

    /// Infers the surface from a profile name.
    ///
    /// @param profile the profile name
    /// @return the matching surface, or [DshSurface#OTHER]
    public static DshSurface ofProfile(String profile) {
        String normalized = profile == null ? "" : profile.trim().toLowerCase(Locale.ROOT);
        for (DshSurface surface : values()) {
            if (!surface.profileName.isEmpty() && surface.profileName.equals(normalized)) {
                return surface;
            }
        }
        return OTHER;
    }

    /// Infers the surface from the bundles a profile layers.
    ///
    /// **This is the question that matters**, and asking it by name is what broke a pack. A profile
    /// is a directory of layers, and the app it boots is one of them: an author who writes a pack
    /// around the browser interface names their profile after the pack — `pokemon`, not `web` — and
    /// every name-based answer about it is then wrong. The launcher treated such a profile as an
    /// unknown surface, which meant no port, no `--no-open`, no readiness line and no browser: the
    /// instance started perfectly and nothing appeared, which is exactly what was reported.
    ///
    /// The bundle is matched on its last path segment, because the same app is addressed both as
    /// `@deepseek-ai/dsh-web-app` and as `dsh-web-app` depending on where it came from.
    ///
    /// @param bundles the bundle package names, in load order
    /// @return the surface, or [DshSurface#OTHER] when none of them is an app
    public static DshSurface ofBundles(@Nullable List<String> bundles) {
        if (bundles == null) {
            return OTHER;
        }
        for (String bundle : bundles) {
            if (bundle == null) {
                continue;
            }
            String name = bundle.substring(bundle.lastIndexOf('/') + 1).trim().toLowerCase(Locale.ROOT);
            for (DshSurface surface : values()) {
                if (!surface.appBundle.isEmpty() && surface.appBundle.equals(name)) {
                    return surface;
                }
            }
        }
        return OTHER;
    }

    /// Infers the surface of an instance's profile.
    ///
    /// The profile's own manifest first, and its name only as a fallback. The order is the whole
    /// point: a profile is named by whoever made it and layers what it actually boots, so the layers
    /// answer the question and the name is a guess. The fallback still exists for a profile whose
    /// manifest cannot be read — an instance made but never installed into, which has no bundles yet
    /// — where the name is all there is and `web` is at least a reasonable guess.
    ///
    /// @param instance the instance
    /// @return the surface
    public static DshSurface of(DshInstance instance) {
        try {
            DshSurface byBundles = ofBundles(
                    DshPluginInstaller.readBundles(instance.homeDirectory(), instance.profile()));
            if (byBundles != OTHER) {
                return byBundles;
            }
        } catch (DshException | RuntimeException e) {
            // A profile that cannot be read is not a reason to refuse to start it.
            org.jackhuang.hmcl.util.logging.Logger.LOG.info("Could not read the bundles of profile " + instance.profile()
                    + "; falling back to its name: " + e.getMessage());
        }
        return ofProfile(instance.profile());
    }
}
