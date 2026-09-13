package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.Sets.SetView;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.waypoints.WaypointManager;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class ServerWaypointManager implements WaypointManager<WaypointTransmitter> {
    private final Set<WaypointTransmitter> waypoints = new HashSet<>();
    private final Set<ServerPlayer> players = new HashSet<>();
    private final Table<ServerPlayer, WaypointTransmitter, WaypointTransmitter.Connection> connections = HashBasedTable.create();

    public void trackWaypoint(final WaypointTransmitter waypoint) {
        this.waypoints.add(waypoint);

        for (ServerPlayer player : this.players) {
            this.createConnection(player, waypoint);
        }
    }

    public void updateWaypoint(final WaypointTransmitter waypoint) {
        if (this.waypoints.contains(waypoint)) {
            Map<ServerPlayer, WaypointTransmitter.Connection> playerConnection = Tables.transpose(this.connections).row(waypoint);
            SetView<ServerPlayer> potentialPlayers = Sets.difference(this.players, playerConnection.keySet());

            for (Entry<ServerPlayer, WaypointTransmitter.Connection> waypointConnection : ImmutableSet.copyOf(playerConnection.entrySet())) {
                this.updateConnection(waypointConnection.getKey(), waypoint, waypointConnection.getValue());
            }

            for (ServerPlayer player : potentialPlayers) {
                this.createConnection(player, waypoint);
            }
        }
    }

    public void untrackWaypoint(final WaypointTransmitter waypoint) {
        this.connections.column(waypoint).forEach((player, connection) -> connection.disconnect());
        Tables.transpose(this.connections).row(waypoint).clear();
        this.waypoints.remove(waypoint);
    }

    public void addPlayer(final ServerPlayer player) {
        this.players.add(player);

        for (WaypointTransmitter waypoint : this.waypoints) {
            this.createConnection(player, waypoint);
        }

        if (player.isTransmittingWaypoint()) {
            this.trackWaypoint(player);
        }
    }

    public void updatePlayer(final ServerPlayer player) {
        Map<WaypointTransmitter, WaypointTransmitter.Connection> waypointConnections = this.connections.row(player);
        SetView<WaypointTransmitter> potentialWaypoints = Sets.difference(this.waypoints, waypointConnections.keySet());

        for (Entry<WaypointTransmitter, WaypointTransmitter.Connection> waypointConnection : ImmutableSet.copyOf(waypointConnections.entrySet())) {
            this.updateConnection(player, waypointConnection.getKey(), waypointConnection.getValue());
        }

        for (WaypointTransmitter waypoint : potentialWaypoints) {
            this.createConnection(player, waypoint);
        }
    }

    public void removePlayer(final ServerPlayer player) {
        this.connections.row(player).values().removeIf(connection -> {
            connection.disconnect();
            return true;
        });
        this.untrackWaypoint(player);
        this.players.remove(player);
    }

    public void breakAllConnections() {
        this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
        this.connections.clear();
    }

    public void remakeConnections(final WaypointTransmitter waypoint) {
        for (ServerPlayer player : this.players) {
            this.createConnection(player, waypoint);
        }
    }

    public Set<WaypointTransmitter> transmitters() {
        return this.waypoints;
    }

    private static boolean isLocatorBarEnabledFor(final ServerPlayer player) {
        return player.level().getGameRules().get(GameRules.LOCATOR_BAR);
    }

    private void createConnection(final ServerPlayer player, final WaypointTransmitter waypoint) {
        if (player != waypoint) {
            if (isLocatorBarEnabledFor(player)) {
                waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(connection -> {
                    this.connections.put(player, waypoint, connection);
                    connection.connect();
                }, () -> {
                    WaypointTransmitter.Connection connection = this.connections.remove(player, waypoint);
                    if (connection != null) {
                        connection.disconnect();
                    }
                });
            }
        }
    }

    private void updateConnection(final ServerPlayer player, final WaypointTransmitter waypoint, final WaypointTransmitter.Connection connection) {
        if (player != waypoint) {
            if (isLocatorBarEnabledFor(player)) {
                if (!connection.isBroken()) {
                    connection.update();
                } else {
                    waypoint.makeWaypointConnectionWith(player).ifPresentOrElse(newConnection -> {
                        newConnection.connect();
                        this.connections.put(player, waypoint, newConnection);
                    }, () -> {
                        connection.disconnect();
                        this.connections.remove(player, waypoint);
                    });
                }
            }
        }
    }
}