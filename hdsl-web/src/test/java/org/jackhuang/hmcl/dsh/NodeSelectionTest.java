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
package org.jackhuang.hmcl.dsh;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The selector grammar and the resolution rules the Node page and the launch
/// path share.
///
/// Every input is injected: the installed list, the disabled set and the PATH
/// probe. A test that read the machine's data directory or PATH would pass on
/// one host and fail on the next.
class NodeSelectionTest {

    private static NodeRuntime runtime(String version) {
        return new NodeRuntime(version, Path.of("/nonexistent", "node-" + version));
    }

    private static DshNodeRuntime pathRuntime(String version, boolean pnpm) {
        return new DshNodeRuntime(
                Path.of("/usr/bin/node"), version,
                null, null,
                pnpm ? Path.of("/usr/bin/pnpm") : null,
                pnpm ? "10.9.8" : null);
    }

    private static NodeSelection.Resolution resolve(String selector, List<NodeRuntime> installed,
                                                    Predicate<String> disabled,
                                                    Supplier<Optional<DshNodeRuntime>> onPath) {
        return NodeSelection.resolve(NodeSelection.parse(selector), null, false, installed, disabled, onPath);
    }

    @Test
    void parsesEveryFormTheUiCanSend() {
        assertEquals(NodeSelection.Kind.INHERIT, NodeSelection.parse(null).kind());
        assertEquals(NodeSelection.Kind.INHERIT, NodeSelection.parse("  ").kind());
        assertEquals(NodeSelection.Kind.INHERIT, NodeSelection.parse("global").kind());
        assertEquals(NodeSelection.Kind.INHERIT, NodeSelection.parse("inherit").kind());
        assertEquals(NodeSelection.Kind.AUTO, NodeSelection.parse("auto").kind());
        assertEquals(NodeSelection.Kind.AUTO, NodeSelection.parse("system").kind());
        assertEquals(NodeSelection.Kind.CHANNEL, NodeSelection.parse("22").kind());
        assertEquals("22", NodeSelection.parse("22").value());
        assertEquals(NodeSelection.Kind.CHANNEL, NodeSelection.parse("lts").kind());
        assertEquals(NodeSelection.Kind.CHANNEL, NodeSelection.parse("current").kind());
        assertEquals(NodeSelection.Kind.EXACT, NodeSelection.parse("22.23.1").kind());
        assertEquals(NodeSelection.Kind.INVALID, NodeSelection.parse("banana").kind());
        assertEquals(NodeSelection.Kind.INVALID, NodeSelection.parse("22.x").kind());
    }

    @Test
    void aChannelPicksTheHighestInstalledVersionOnThatLine() {
        List<NodeRuntime> installed = List.of(runtime("24.14.0"), runtime("22.23.1"), runtime("22.20.0"));
        NodeSelection.Resolution resolution = resolve("22", installed, version -> false, Optional::empty);

        assertEquals("22.23.1", resolution.version());
        assertEquals(NodeSelection.Source.CHANNEL, resolution.source());
        assertTrue(resolution.compatible());
    }

    @Test
    void aDisabledVersionIsSkippedByAutoAndChannelButNotByAnExactPin() {
        List<NodeRuntime> installed = List.of(runtime("22.23.1"), runtime("22.20.0"));
        Predicate<String> disabled = Set.of("22.23.1")::contains;

        assertEquals("22.20.0", resolve("22", installed, disabled, Optional::empty).version());
        assertEquals("22.20.0", resolve("auto", installed, disabled, Optional::empty).version());

        NodeSelection.Resolution pinned = resolve("22.23.1", installed, disabled, Optional::empty);
        assertEquals("22.23.1", pinned.version());
        assertEquals(NodeSelection.Source.EXACT, pinned.source());
        assertTrue(pinned.reason().contains("禁用"));
    }

    @Test
    void autoPrefersAUsablePathRuntime() {
        NodeSelection.Resolution resolution = resolve("auto",
                List.of(runtime("22.23.1")), version -> false,
                () -> Optional.of(pathRuntime("24.14.0", true)));

        assertEquals("24.14.0", resolution.version());
        assertEquals(NodeSelection.Source.SYSTEM, resolution.source());
    }

    @Test
    void autoFallsBackToTheNewestEnabledManagedRuntime() {
        NodeSelection.Resolution resolution = resolve("auto",
                List.of(runtime("24.14.0"), runtime("22.23.1")), version -> false, Optional::empty);

        assertEquals("24.14.0", resolution.version());
        assertEquals(NodeSelection.Source.MANAGED, resolution.source());
    }

    @Test
    void autoWithoutAnythingUsableResolvesToNothing() {
        NodeSelection.Resolution resolution = resolve("auto", List.of(), version -> false, Optional::empty);

        assertFalse(resolution.present());
        assertNull(resolution.version());
        assertEquals(NodeSelection.Source.NONE, resolution.source());
        assertFalse(resolution.reason().isBlank());
    }

    @Test
    void inheritFollowsTheNextLayer() {
        List<NodeRuntime> installed = List.of(runtime("22.23.1"));
        NodeSelection.Resolution resolution = NodeSelection.resolve(
                NodeSelection.parse("inherit"), "22", false, installed, version -> false, Optional::empty);

        assertEquals("22.23.1", resolution.version());
        assertEquals(NodeSelection.Source.CHANNEL, resolution.source());
    }

    @Test
    void anInheritInTheNextLayerFallsThroughToAuto() {
        List<NodeRuntime> installed = List.of(runtime("22.23.1"));
        NodeSelection.Resolution resolution = NodeSelection.resolve(
                NodeSelection.parse("inherit"), "inherit", false, installed, version -> false, Optional::empty);

        assertEquals("22.23.1", resolution.version());
        assertEquals(NodeSelection.Source.MANAGED, resolution.source());
    }

    @Test
    void anExactVersionThatIsNotInstalledResolvesToNothing() {
        NodeSelection.Resolution resolution = resolve("22.99.0",
                List.of(runtime("22.23.1")), version -> false, Optional::empty);

        assertFalse(resolution.present());
        assertEquals(NodeSelection.Source.EXACT, resolution.source());
    }

    @Test
    void ltsMeansTheEvenMajorLineAndCurrentMeansEveryLine() {
        List<NodeRuntime> installed = List.of(runtime("25.0.0"), runtime("24.14.0"), runtime("22.23.1"));
        assertEquals("24.14.0", resolve("lts", installed, version -> false, Optional::empty).version());
        assertEquals("25.0.0", resolve("current", installed, version -> false, Optional::empty).version());
    }

    @Test
    void aChannelPointerWinsOverTheHighestInstalled() {
        List<NodeRuntime> installed = List.of(runtime("22.23.1"), runtime("22.20.0"));
        NodeSelection.Resolution resolution = NodeSelection.resolve(
                NodeSelection.parse("22"), null, false, installed, version -> false,
                Optional::empty, name -> "22".equals(name) ? "22.20.0" : null);

        assertEquals("22.20.0", resolution.version());
        assertEquals(NodeSelection.Source.CHANNEL, resolution.source());
    }

    @Test
    void aPointerToAnUninstalledVersionFallsBackToTheLine() {
        List<NodeRuntime> installed = List.of(runtime("22.23.1"));
        NodeSelection.Resolution resolution = NodeSelection.resolve(
                NodeSelection.parse("22"), null, false, installed, version -> false,
                Optional::empty, name -> "22.99.0");

        assertEquals("22.23.1", resolution.version());
    }

    @Test
    void anInvalidSelectorIsRefusedRatherThanTreatedAsSystem() {
        NodeSelection.Resolution resolution = resolve("banana",
                List.of(runtime("22.23.1")), version -> false, Optional::empty);

        assertFalse(resolution.present());
        assertEquals(NodeSelection.Source.NONE, resolution.source());
    }
}
