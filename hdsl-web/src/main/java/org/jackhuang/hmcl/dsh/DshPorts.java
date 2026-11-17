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

import java.io.IOException;
import java.io.Serial;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/// Port selection for the browser surface.
///
/// DeepSeek Harness exits with status 1 when its port is taken rather than
/// probing for another one, so the launcher has to choose a free port itself.
/// It also has to keep choosing the *same* port for an instance: the browser
/// interface keys session state by origin, so reaching one history through two
/// ports lets two writers corrupt it.
///
/// The port is therefore settled once, when the instance is created, and written
/// into its manifest; every launch from then on asks for that same port. A port
/// that has since been taken is not worked around — moving the instance would
/// change the origin its state is keyed by — so the launch is refused and the
/// user is told which port to free or to change.
@NotNullByDefault
public final class DshPorts {
    private DshPorts() {
    }

    /// The lowest port the automatic allocator will hand out.
    private static final int AUTO_RANGE_START = 3081;

    /// The highest port the automatic allocator will hand out.
    private static final int AUTO_RANGE_END = 4081;

    /// How many random candidates are tried before the range is swept in order.
    private static final int RANDOM_ATTEMPTS = 64;

    /// The source of the candidates.
    ///
    /// Random rather than sequential so that two launchers, or two runs of the
    /// same one, do not both hand out the bottom of the range and collide the
    /// moment anything else on the machine takes a port.
    private static final java.util.Random RANDOM = new java.util.Random();

    /// Raised when the port an instance must bind is held by something else.
    ///
    /// Carries the port, because the interface names it when it explains what to
    /// do about it, and the domain layer is where the number is known.
    public static final class PortUnavailableException extends DshException {
        @Serial
        private static final long serialVersionUID = 1L;

        /// The instance that cannot have its port.
        private final String instanceId;

        /// The port that is taken.
        private final int port;

        /// Creates the exception.
        ///
        /// @param instanceId the instance
        /// @param port       the port it needs
        PortUnavailableException(String instanceId, int port) {
            super("Port " + port + " for instance " + instanceId + " is already in use");
            this.instanceId = instanceId;
            this.port = port;
        }

        /// Returns the instance that cannot be started.
        ///
        /// @return the instance id
        public String instanceId() {
            return instanceId;
        }

        /// Returns the port that is taken.
        ///
        /// @return the port
        public int port() {
            return port;
        }
    }

    /// Reserves a free port for an instance, avoiding the ones other instances hold.
    ///
    /// Called when an instance is created, so that the port it will use for its
    /// whole life is decided once and written down, rather than being discovered
    /// on the first launch — which is what left an instance without a port while
    /// it sat in the list, and let two instances be given the same one.
    ///
    /// @param instanceId the instance being created, excluded from the ports in use
    /// @return the reserved port
    /// @throws DshException when no free port can be found in the range
    public static int reserve(String instanceId) throws DshException {
        Set<Integer> claimed = claimedPorts(instanceId);
        int span = AUTO_RANGE_END - AUTO_RANGE_START + 1;

        for (int attempt = 0; attempt < RANDOM_ATTEMPTS; attempt++) {
            int candidate = AUTO_RANGE_START + RANDOM.nextInt(span);
            if (!claimed.contains(candidate) && isFree(candidate)) {
                return candidate;
            }
        }

        for (int port = AUTO_RANGE_START; port <= AUTO_RANGE_END; port++) {
            if (!claimed.contains(port) && isFree(port)) {
                return port;
            }
        }
        throw new DshException("No free port was found between " + AUTO_RANGE_START
                + " and " + AUTO_RANGE_END);
    }

    /// Resolves the port to launch an instance on.
    ///
    /// The instance's own port is used as it stands, whether the launcher picked
    /// it or the user did. An instance that has no port yet — one made before
    /// ports were reserved — is given one now, and keeps it from then on.
    ///
    /// @param instance the instance being launched
    /// @return the port to pass to DeepSeek Harness
    /// @throws DshException when no free port can be found, or the instance's own
    ///                       port is held by something else
    public static int resolve(DshInstance instance) throws DshException {
        int port = instance.portOrDefault();
        if (port <= 0) {
            LOG.info("Instance " + instance.id() + " has no port yet; reserving one");
            return reserve(instance.id());
        }
        if (!isFree(port)) {
            // The port is part of what the instance is: the browser interface
            // keys its state by origin, so an instance that came up somewhere
            // else would be an instance whose history is somewhere else. The
            // launcher says so instead of quietly changing it.
            throw new PortUnavailableException(instance.id(), port);
        }
        return port;
    }

    /// Returns the port an instance is really serving on.
    ///
    /// DeepSeek Harness prints the address it bound, so the printed one is the truth and the
    /// number the launcher asked for is only what it asked for. The two are the same thing until
    /// a patch layer restates the `webserver` row: `--port` reaches the server through that row's
    /// own `webStartup` expression, and a layer that replaces the row with a literal port takes
    /// that expression away. Nothing about a launch says which of the two happened, so the
    /// answer is read from the address rather than assumed from the request.
    ///
    /// @param reported the address the harness printed, or `null` when it printed none
    /// @param planned  the port the launcher asked for
    /// @return the port the instance is on
    public static int observedPort(@Nullable java.net.URI reported, int planned) {
        if (reported == null) {
            return planned;
        }
        int port = reported.getPort();
        return port > 0 ? port : planned;
    }

    /// Returns the address a browser is sent to for an instance.
    ///
    /// A browser keys the state it keeps by origin, and an instance's origin is the port it is
    /// recorded at. The address the harness printed is used while it is that port, because it
    /// carries the token a fenced release wants. When it is another port the instance's own
    /// address is returned instead, deliberately without a token: an address on the port the
    /// harness really bound belongs to an origin this instance is not recorded at, and a browser
    /// sent there would leave everything it stores behind the day the port moves back.
    ///
    /// @param instance the instance
    /// @param reported the address the harness printed, or `null` when it printed none
    /// @return the address to open
    public static java.net.URI openAddress(DshInstance instance, @Nullable java.net.URI reported) {
        int port = instance.portOrDefault();
        boolean servingWhereItIsRecorded = reported != null && (port <= 0 || observedPort(reported, port) == port);
        return servingWhereItIsRecorded ? reported : java.net.URI.create("http://127.0.0.1:" + Math.max(port, 0) + "/");
    }

    /// Records the port an instance settled on.
    ///
    /// Only an instance that had none is written to: a port the launcher or the
    /// user already chose is the one it keeps, and rewriting it would be exactly
    /// the silent move this class exists to prevent.
    ///
    /// @param instance the instance
    /// @param port     the port DeepSeek Harness is bound to
    /// @throws DshException when the instance cannot be written back
    public static void remember(DshInstance instance, int port) throws DshException {
        if (port <= 0 || instance.portOrDefault() == port) {
            return;
        }
        if (instance.portModeOrDefault() != DshPortMode.AUTO) {
            return;
        }
        DshInstanceManager.update(instance.withPort(port));
        if (DshInstanceSettings.autoPort(instance) <= 0) {
            DshInstanceSettings.setAutoPort(instance, port);
        }
    }

    /// Returns the port the launcher gave an instance, which is what its automatic policy means.
    ///
    /// An instance made before this was written down has it nowhere else, and while its policy is
    /// automatic the port in its record **is** the one the launcher gave it — that is what makes this
    /// work for the instances that already exist, without a migration step.
    ///
    /// @param instance the instance
    /// @return the port, or `0` when the launcher never gave this instance one
    public static int autoPort(DshInstance instance) {
        int recorded = DshInstanceSettings.autoPort(instance);
        if (recorded > 0) {
            return recorded;
        }
        return instance.portModeOrDefault() == DshPortMode.AUTO ? instance.portOrDefault() : 0;
    }

    /// Writes down the port the launcher gave an instance, before a named one takes over.
    ///
    /// Called when the policy is about to become a named port: the instance's record is going to hold
    /// the number the person typed, and a number that was never written down somewhere else is a
    /// number there is no going back to.
    ///
    /// @param instance the instance
    /// @throws DshException when the choice cannot be written
    public static void keepAutoPort(DshInstance instance) throws DshException {
        if (DshInstanceSettings.autoPort(instance) > 0
                || instance.portModeOrDefault() != DshPortMode.AUTO) {
            return;
        }
        int port = instance.portOrDefault();
        if (port > 0) {
            DshInstanceSettings.setAutoPort(instance, port);
        }
    }

    /// Returns an instance under a different port policy, keeping the port the launcher gave it.
    ///
    /// **A named port is a detour, not a replacement.** An instance's browser state is keyed to the
    /// origin it was first reached at, so the launcher's own port is written down before a named one
    /// takes over and given back when the person stops naming one. Only an instance that lost it
    /// before the launcher wrote such things down — one an earlier version switched to a named port —
    /// is given a new one, which is the most that can be done for it.
    ///
    /// @param instance the instance
    /// @param mode     the policy to record
    /// @return the instance to store
    /// @throws DshException when a port has to be reserved and none can be found
    public static DshInstance withMode(DshInstance instance, DshPortMode mode) throws DshException {
        if (mode == DshPortMode.FIXED) {
            keepAutoPort(instance);
            // The field starts at the number the instance already has, so naming a port is an edit of
            // a real one rather than a blank to fill in.
            return instance.withPortPolicy(mode, instance.portOrDefault());
        }
        int automatic = autoPort(instance);
        if (automatic <= 0) {
            automatic = reserve(instance.id());
            DshInstanceSettings.setAutoPort(instance, automatic);
        }
        return instance.withPortPolicy(mode, automatic);
    }

    /// Reports whether a port can be bound on the loopback interface.
    ///
    /// @param port the port to test
    /// @return whether it is free
    public static boolean isFree(int port) {
        if (port <= 0 || port > 65535) {
            return false;
        }
        try (ServerSocket socket = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            socket.setReuseAddress(false);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /// Returns the ports other instances have been given.
    ///
    /// @param exceptInstanceId the instance to leave out, or `null`
    /// @return the ports in use on paper
    static Set<Integer> claimedPorts(@Nullable String exceptInstanceId) {
        Set<Integer> claimed = new HashSet<>();
        for (DshInstance instance : List.copyOf(DshInstanceManager.list())) {
            if (instance.id().equals(exceptInstanceId)) {
                continue;
            }
            // Both numbers, when there are two: the one in effect, and the launcher's own — which a
            // named port has merely put aside, and which this instance is entitled to come back to.
            for (int port : new int[]{instance.portOrDefault(), autoPort(instance)}) {
                if (port > 0) {
                    claimed.add(port);
                }
            }
        }
        return claimed;
    }

    /// Validates a user-entered port.
    ///
    /// @param port the port, or `null`
    /// @return the complaint, or `null` when the port is usable
    public static @Nullable String validate(@Nullable Integer port) {
        if (port == null) {
            return "Enter a port number";
        }
        if (port < 1024 || port > 65535) {
            return "Use a port between 1024 and 65535";
        }
        return null;
    }
}
