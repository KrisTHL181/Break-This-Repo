/*
 * HDSL-web
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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which toolchain a caller gets when `PATH`'s is not one it can use.
///
/// Both failures this pins were real, and they share a shape — **"something is on PATH" is not
/// "something that can do the job"**:
///
/// - a machine with a system Node.js and no pnpm reported "pnpm was not found on PATH" on every
///   DeepSeek Harness install, while the panel's managed runtime (installed *with* pnpm) sat in
///   the data directory and was never asked;
/// - a machine with a distribution Node.js 20 launched brand tools that then died inside their
///   own ESM loader (`node:zlib has no export named createZstdDecompress`), and refused every dsh
///   launch with a message about a range — again while a supported managed runtime was available.
class DshNodeRuntimeChoiceTest {

    /// What a pnpm-needing caller asks for: a supported Node.js that carries pnpm.
    private static final Predicate<DshNodeRuntime> NEEDS_PNPM =
            runtime -> runtime.isNodeSupported() && runtime.canManagePlugins();

    /// What a launch asks for: a supported Node.js.
    private static final Predicate<DshNodeRuntime> NEEDS_SUPPORTED_NODE =
            DshNodeRuntime::isNodeSupported;

    /// What a registry read asks for: a Node.js that can run `npm`.
    private static final Predicate<DshNodeRuntime> NEEDS_NPM = DshNodeRuntime::canInstall;

    /// Debian and Ubuntu ship `nodejs` and `npm` as **separate packages**, so a `PATH` Node
    /// without npm is the normal state on those hosts rather than a broken one. Reading the
    /// registry used to take that runtime and refuse with "npm was not found on PATH" while
    /// the managed runtime — installed with npm — sat unused in the data directory.
    @Test
    void aPathRuntimeWithoutNpmYieldsToTheManagedOne() {
        DshNodeRuntime system = nodeWithoutNpm("/usr/bin/node", "22.19.0");
        DshNodeRuntime managed = runtime("/data/hdsl/runtimes/22.19.0/bin/node", "22.19.0", null);

        assertEquals(Optional.of(managed),
                DshNodeRuntime.prefer(Optional.of(system), Optional.of(managed), NEEDS_NPM),
                "the managed runtime is the one that can reach the registry");
    }

    @Test
    void aPathRuntimeWithNpmIsKeptForRegistryReads() {
        DshNodeRuntime system = runtime("/usr/bin/node", "22.19.0", null);
        DshNodeRuntime managed = runtime("/data/hdsl/runtimes/22.19.0/bin/node", "22.19.0", null);

        assertEquals(Optional.of(system),
                DshNodeRuntime.prefer(Optional.of(system), Optional.of(managed), NEEDS_NPM),
                "what the operator put on PATH wins when it can do the job");
    }

    @Test
    void aPathRuntimeWithoutPnpmYieldsToTheManagedOne() {
        DshNodeRuntime system = runtime("/usr/bin/node", "22.19.0", null);
        DshNodeRuntime managed = runtime("/data/hdsl/runtimes/22.19.0/bin/node", "22.19.0",
                "/data/hdsl/runtimes/22.19.0/bin/pnpm");

        assertEquals(Optional.of(managed),
                DshNodeRuntime.prefer(Optional.of(system), Optional.of(managed), NEEDS_PNPM),
                "the managed runtime is the one that can run pnpm");
    }

    @Test
    void aPathRuntimeThatIsTooOldYieldsToTheManagedOne() {
        // Node 20 with pnpm: it can install, and what it installs cannot run. The version
        // check is what tells the two apart.
        DshNodeRuntime system = runtime("/usr/bin/node", "20.19.2", "/usr/lib/node_modules/pnpm");
        DshNodeRuntime managed = runtime("/data/hdsl/runtimes/22.19.0/bin/node", "22.19.0",
                "/data/hdsl/runtimes/22.19.0/bin/pnpm");

        assertEquals(Optional.of(managed),
                DshNodeRuntime.prefer(Optional.of(system), Optional.of(managed), NEEDS_PNPM),
                "a supported Node.js beats an older one that happens to have pnpm");
        assertEquals(Optional.of(managed),
                DshNodeRuntime.prefer(Optional.of(system), Optional.of(managed), NEEDS_SUPPORTED_NODE),
                "and a launch that only needs Node.js makes the same choice");
    }

    @Test
    void aPathRuntimeWithPnpmIsKept() {
        DshNodeRuntime system = runtime("/opt/node/bin/node", "22.19.0", "/opt/node/bin/pnpm");
        DshNodeRuntime managed = runtime("/data/hdsl/runtimes/22.19.0/bin/node", "22.19.0",
                "/data/hdsl/runtimes/22.19.0/bin/pnpm");

        assertEquals(Optional.of(system),
                DshNodeRuntime.prefer(Optional.of(system), Optional.of(managed), NEEDS_PNPM),
                "what the operator put on PATH wins when it can do the job");
    }

    @Test
    void thePathRuntimeIsStillReturnedWhenNothingQualifies() {
        DshNodeRuntime system = runtime("/usr/bin/node", "22.19.0", null);

        assertEquals(Optional.of(system),
                DshNodeRuntime.prefer(Optional.of(system), Optional.empty(), NEEDS_PNPM),
                "returning it lets the caller name the runtime it looked at, which is the whole diagnosis");
        assertEquals(Optional.empty(),
                DshNodeRuntime.prefer(Optional.empty(), Optional.empty(), NEEDS_PNPM),
                "no Node.js at all is a different answer, and a different message");
    }

    @Test
    void theRefusalNamesTheRuntimeTheFloorAndBothWaysOut() {
        DshNodeRuntime system = runtime("/usr/bin/node", "22.19.0", null);

        String message = system.pnpmMissing("installing DeepSeek Harness").getMessage();

        assertTrue(message.contains("/usr/bin/node"),
                "the runtime that was found is the missing piece: " + message);
        assertTrue(message.contains(DshNodeRuntime.MINIMUM_PNPM), message);
        assertTrue(message.contains("npm install -g pnpm"), message);
        assertTrue(message.contains("Settings -> Node.js"),
                "and the managed runtime that brings pnpm: " + message);
    }

    @Test
    void aNodeThatIsTooOldIsRefusedBeforeTheInstallInsteadOfAfterIt() {
        DshNodeRuntime system = runtime("/usr/bin/node", "20.19.2", "/usr/lib/node_modules/pnpm");

        DshException refused = assertThrows(DshException.class,
                () -> DshVersionManager.requireSupportedNode(system),
                "an install under Node 20 would succeed and leave a harness that cannot launch");

        assertTrue(refused.getMessage().contains("20.19.2"), refused.getMessage());
        assertTrue(refused.getMessage().contains("/usr/bin/node"), refused.getMessage());
        assertTrue(refused.getMessage().contains(DshNodeRuntime.requirement()),
                "the refusal says what is supported: " + refused.getMessage());
    }

    /// A runtime descriptor, without spawning anything.
    ///
    /// @param node     the node path
    /// @param version  the node version
    /// @param pnpm     the pnpm path, or `null` when the runtime has none
    /// @return the descriptor
    private static DshNodeRuntime runtime(String node, String version, String pnpm) {
        return new DshNodeRuntime(Path.of(node), version, Path.of("/usr/bin/npm"), "10.9.9",
                pnpm == null ? null : Path.of(pnpm), pnpm == null ? null : "11.7.0");
    }

    /// The Debian/Ubuntu shape: `node` is installed, `npm` is not.
    ///
    /// @param node    the node path
    /// @param version the node version
    /// @return the descriptor
    private static DshNodeRuntime nodeWithoutNpm(String node, String version) {
        return new DshNodeRuntime(Path.of(node), version, null, null, null, null);
    }
}
