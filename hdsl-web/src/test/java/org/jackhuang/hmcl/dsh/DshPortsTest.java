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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies the port policy that keeps an instance on one origin.
class DshPortsTest {
    /// Builds an instance with the given port policy.
    ///
    /// @param mode the policy
    /// @param port the remembered or fixed port
    /// @return the instance
    private static DshInstance instance(DshPortMode mode, int port) {
        return new DshInstance("test", "0.0.0", "web", "/tmp",
                DshNodeRuntime.SYSTEM, DshHomeMode.ISOLATED, null,
                List.of(), Map.of(), DshInstanceIcon.DEFAULT.id(), null, mode, port, 0L);
    }

    @Test
    void aFixedPortIsUsedAsGiven() throws Exception {
        assertEquals(34567, DshPorts.resolve(instance(DshPortMode.FIXED, 34567)));
    }

    @Test
    void anAutomaticInstanceKeepsThePortItWasGiven() throws Exception {
        // The probe socket must be released first: while it is bound, the port
        // is by definition not free and the allocator would rightly move on.
        int port;
        try (ServerSocket probe = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = probe.getLocalPort();
        }
        assertEquals(port, DshPorts.resolve(instance(DshPortMode.AUTO, port)),
                "a remembered port that is still free must be reused");
    }

    @Test
    void aTakenPortRefusesTheLaunchRatherThanMovingTheInstance() throws Exception {
        try (ServerSocket taken = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            int port = taken.getLocalPort();
            DshPorts.PortUnavailableException refused = assertThrows(
                    DshPorts.PortUnavailableException.class,
                    () -> DshPorts.resolve(instance(DshPortMode.AUTO, port)),
                    "the browser interface keys its state by origin, so an instance must not be moved");
            assertEquals(port, refused.port(), "the message has to name the port to free");
            assertEquals("test", refused.instanceId());
        }
    }

    @Test
    void aTakenFixedPortIsRefusedTheSameWay() throws Exception {
        try (ServerSocket taken = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            int port = taken.getLocalPort();
            assertThrows(DshPorts.PortUnavailableException.class,
                    () -> DshPorts.resolve(instance(DshPortMode.FIXED, port)));
        }
    }

    @Test
    void aReservedPortIsFreeAndInTheLaunchersOwnRange() throws Exception {
        int port = DshPorts.reserve("a-new-instance");
        assertTrue(port >= 3081 && port <= 4081, "reserved ports stay in the launcher's band, got " + port);
        assertTrue(DshPorts.isFree(port));
    }

    @Test
    void aReservationAvoidsThePortsOtherInstancesHold() throws Exception {
        // Another instance already holds a port; a new one must not be given it,
        // or the second instance to start would find its own port taken.
        DshInstance existing = DshInstanceManager.create("holds-a-port", "1.0.0",
                DshInstance.DEFAULT_PROFILE, java.nio.file.Path.of("/tmp"),
                DshHomeMode.ISOLATED, null, List.of(), Map.of());
        try {
            assertTrue(existing.portOrDefault() > 0, "creating an instance settles its port");
            assertNotEquals(existing.portOrDefault(), DshPorts.reserve("another-instance"),
                    "two instances must not be promised the same port");
        } finally {
            DshInstanceManager.delete(existing.id());
        }
    }

    @Test
    void anAutomaticInstanceWithNoPortGetsAFreeOne() throws Exception {
        int port = DshPorts.resolve(instance(DshPortMode.AUTO, 0));
        assertTrue(port > 0);
        try (ServerSocket probe = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            assertTrue(probe.isBound());
        }
    }

    @Test
    void namingAPortIsADetourAndAutomaticComesBackToTheLaunchersPort() throws Exception {
        // The whole point of the two numbers. The port in the record is the one **in effect**, so a
        // named port takes it over; the port the launcher gave the instance has to survive that,
        // because it is the origin this instance's browser state lives at.
        DshInstance instance = managedInstance("ports-detour");
        try {
            int given = instance.portOrDefault();
            assertTrue(given > 0, "creating an instance settles its port");

            // Naming one starts at the number the instance already has, so the field is an edit of a
            // real port rather than a blank; the launcher's own choice is written down on the way.
            DshInstance named = DshPorts.withMode(instance, DshPortMode.FIXED);
            assertEquals(given, named.portOrDefault(), "the field starts at the port it had");
            DshInstanceManager.update(named.withPortPolicy(DshPortMode.FIXED, 34567));

            DshInstance back = DshPorts.withMode(DshInstanceManager.find(instance.id()),
                    DshPortMode.AUTO);

            assertEquals(given, back.portOrDefault(),
                    "automatic gives back the port the launcher gave, not the named one");
            assertEquals(given, DshPorts.autoPort(DshInstanceManager.find(instance.id())),
                    "and it is written down, so the next switch survives a restart too");
        } finally {
            DshInstanceManager.delete(instance.id());
        }
    }

    @Test
    void anInstanceThatLostItsAutomaticPortIsGivenAFreshOne() throws Exception {
        // What an earlier version left behind: an instance switched to a named port without its own
        // ever being written down, so the launcher's number is gone. The best that can be done is a
        // new one from the launcher's own band — what must not happen is the named port coming back
        // wearing an automatic label, which is what this test is here for.
        DshInstance instance = managedInstance("ports-lost");
        try {
            DshInstanceManager.update(instance.withPortPolicy(DshPortMode.FIXED, 34567));

            DshInstance back = DshPorts.withMode(DshInstanceManager.find(instance.id()),
                    DshPortMode.AUTO);

            assertNotEquals(34567, back.portOrDefault(), "automatic is not the named port in disguise");
            assertTrue(back.portOrDefault() >= 3081 && back.portOrDefault() <= 4081,
                    "a fresh one comes from the launcher's band, got " + back.portOrDefault());
        } finally {
            DshInstanceManager.delete(instance.id());
        }
    }

    @Test
    void aPortANamedOnePutAsideIsNotGivenToAnotherInstance() throws Exception {
        DshInstance instance = managedInstance("ports-put-aside");
        try {
            int given = instance.portOrDefault();
            DshInstanceManager.update(DshPorts.withMode(instance, DshPortMode.FIXED)
                    .withPortPolicy(DshPortMode.FIXED, 34567));

            Set<Integer> claimed = DshPorts.claimedPorts(null);

            assertTrue(claimed.contains(34567), "the port in effect is claimed: " + claimed);
            assertTrue(claimed.contains(given),
                    "and so is the one put aside, which this instance comes back to: " + claimed);
        } finally {
            DshInstanceManager.delete(instance.id());
        }
    }

    /// Creates an instance that exists on disk, which is where the two port facts are kept.
    ///
    /// @param id the instance id
    /// @return the instance
    private static DshInstance managedInstance(String id) throws Exception {
        Path workspace = Files.createTempDirectory("ports-workspace");
        DshInstance existing = DshInstanceManager.find(id);
        if (existing != null) {
            DshInstanceManager.delete(id);
        }
        return DshInstanceManager.create(id, "0.1.6-alpha.2", DshInstance.DEFAULT_PROFILE,
                workspace, DshHomeMode.ISOLATED, null, List.of(), Map.of());
    }

    @Test
    void validationRejectsPrivilegedAndOutOfRangePorts() {
        assertNull(DshPorts.validate(25565));
        assertFalse(DshPorts.validate(80) == null, "ports below 1024 need root");
        assertFalse(DshPorts.validate(70000) == null);
        assertFalse(DshPorts.validate(null) == null);
    }

    @Test
    void theSurfacePassesTheResolvedPortThrough() {
        assertTrue(DshSurface.WEB.arguments(39000).contains("39000"));
        assertFalse(DshSurface.WEB.arguments(39000).contains("0"));
        // Only the browser surface serves HTTP, so only it takes a port.
        assertEquals(List.of(), DshSurface.ACP.arguments(39000));
    }

    @Test
    void thePortAnInstanceIsOnIsTheOneTheHarnessReported() {
        assertEquals(3200, DshPorts.observedPort(java.net.URI.create("http://127.0.0.1:3200/?token=abc"), 3100),
                "the printed address is what the server bound, whatever the launcher asked for");
        assertEquals(3100, DshPorts.observedPort(null, 3100), "no address is nothing to contradict");
        assertEquals(3100, DshPorts.observedPort(java.net.URI.create("http://127.0.0.1/"), 3100),
                "an address with no port says nothing about which port it is");
    }

    @Test
    void theBrowserIsSentToTheAddressTheInstanceIsRecordedAt() {
        DshInstance fixed = instance(DshPortMode.FIXED, 3100);

        assertEquals("http://127.0.0.1:3100/?token=abc",
                DshPorts.openAddress(fixed, java.net.URI.create("http://127.0.0.1:3100/?token=abc")).toString(),
                "the printed address carries the token, so it is used while it is the instance's own port");

        // A harness that came up on another port is a different origin. A browser sent there would
        // keep everything it stores under an address the instance is not recorded at, and lose it
        // the day the port moves back — so the instance's own address is opened instead, plainly.
        assertEquals("http://127.0.0.1:3100/",
                DshPorts.openAddress(fixed, java.net.URI.create("http://127.0.0.1:3200/?token=abc")).toString(),
                "a moved server is not where the instance is");
        assertEquals("http://127.0.0.1:3100/", DshPorts.openAddress(fixed, null).toString(),
                "a stopped instance has no printed address and is opened at its own");
    }
}
