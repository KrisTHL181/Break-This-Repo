package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.jspecify.annotations.Nullable;

public class ServerScoreboard extends Scoreboard {
    private final MinecraftServer server;
    private final Set<Objective> trackedObjectives = Sets.newHashSet();
    private boolean dirty;

    public ServerScoreboard(final MinecraftServer server) {
        this.server = server;
    }

    public void load(final ScoreboardSaveData.Packed data) {
        data.objectives().forEach(x$0 -> this.loadObjective(x$0));
        data.scores().forEach(x$0 -> this.loadPlayerScore(x$0));
        data.displaySlots().forEach((slot, name) -> {
            Objective objective = this.getObjective(name);
            this.setDisplayObjective(slot, objective);
        });
        data.teams().forEach(x$0 -> this.loadPlayerTeam(x$0));
    }

    private ScoreboardSaveData.Packed store() {
        return new ScoreboardSaveData.Packed(this.packObjectives(), this.packPlayerScores(), this.packDisplaySlots(), this.packPlayerTeams());
    }

    @Override
    protected void onScoreChanged(final ScoreHolder owner, final Objective objective, final Score score) {
        super.onScoreChanged(owner, objective, score);
        if (this.trackedObjectives.contains(objective)) {
            this.server
                .getPlayerList()
                .broadcastAll(
                    new ClientboundSetScorePacket(
                        owner.getScoreboardName(),
                        objective.getName(),
                        score.value(),
                        Optional.ofNullable(score.display()),
                        Optional.ofNullable(score.numberFormat())
                    )
                );
        }

        this.setDirty();
    }

    @Override
    protected void onScoreLockChanged(final ScoreHolder owner, final Objective objective) {
        super.onScoreLockChanged(owner, objective);
        this.setDirty();
    }

    @Override
    public void onPlayerRemoved(final ScoreHolder player) {
        super.onPlayerRemoved(player);
        this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(player.getScoreboardName(), null));
        this.setDirty();
    }

    @Override
    public void onPlayerScoreRemoved(final ScoreHolder player, final Objective objective) {
        super.onPlayerScoreRemoved(player, objective);
        if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(player.getScoreboardName(), objective.getName()));
        }

        this.setDirty();
    }

    @Override
    public void setDisplayObjective(final DisplaySlot slot, final @Nullable Objective objective) {
        Objective old = this.getDisplayObjective(slot);
        super.setDisplayObjective(slot, objective);
        if (old != objective && old != null) {
            if (this.getObjectiveDisplaySlotCount(old) > 0) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(slot, objective));
            } else {
                this.stopTrackingObjective(old);
            }
        }

        if (objective != null) {
            if (this.trackedObjectives.contains(objective)) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(slot, objective));
            } else {
                this.startTrackingObjective(objective);
            }
        }

        this.setDirty();
    }

    @Override
    public boolean addPlayerToTeam(final String player, final PlayerTeam team) {
        if (super.addPlayerToTeam(player, team)) {
            this.server
                .getPlayerList()
                .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, player, ClientboundSetPlayerTeamPacket.Action.ADD));
            this.updatePlayerWaypoint(player);
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void removePlayerFromTeam(final String player, final PlayerTeam team) {
        super.removePlayerFromTeam(player, team);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, player, ClientboundSetPlayerTeamPacket.Action.REMOVE));
        this.updatePlayerWaypoint(player);
        this.setDirty();
    }

    @Override
    public void onObjectiveAdded(final Objective objective) {
        super.onObjectiveAdded(objective);
        this.setDirty();
    }

    @Override
    public void onObjectiveChanged(final Objective objective) {
        super.onObjectiveChanged(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(objective, 2));
        }

        this.setDirty();
    }

    @Override
    public void onObjectiveRemoved(final Objective objective) {
        super.onObjectiveRemoved(objective);
        if (this.trackedObjectives.contains(objective)) {
            this.stopTrackingObjective(objective);
        }

        this.setDirty();
    }

    @Override
    public void onTeamAdded(final PlayerTeam team) {
        super.onTeamAdded(team);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
        this.setDirty();
    }

    @Override
    public void onTeamChanged(final PlayerTeam team) {
        super.onTeamChanged(team);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, false));
        this.updateTeamWaypoints(team);
        this.setDirty();
    }

    @Override
    public void onTeamRemoved(final PlayerTeam team) {
        super.onTeamRemoved(team);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
        this.updateTeamWaypoints(team);
        this.setDirty();
    }

    protected void setDirty() {
        this.dirty = true;
    }

    public void storeToSaveDataIfDirty(final ScoreboardSaveData saveData) {
        if (this.dirty) {
            this.dirty = false;
            saveData.setData(this.store());
        }
    }

    public List<Packet<?>> getStartTrackingPackets(final Objective objective) {
        List<Packet<?>> packets = Lists.newArrayList();
        packets.add(new ClientboundSetObjectivePacket(objective, 0));

        for (DisplaySlot slot : DisplaySlot.values()) {
            if (this.getDisplayObjective(slot) == objective) {
                packets.add(new ClientboundSetDisplayObjectivePacket(slot, objective));
            }
        }

        for (PlayerScoreEntry score : this.listPlayerScores(objective)) {
            packets.add(
                new ClientboundSetScorePacket(
                    score.owner(), objective.getName(), score.value(), Optional.ofNullable(score.display()), Optional.ofNullable(score.numberFormatOverride())
                )
            );
        }

        return packets;
    }

    public void startTrackingObjective(final Objective objective) {
        List<Packet<?>> packets = this.getStartTrackingPackets(objective);

        for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : packets) {
                player.connection.send(packet);
            }
        }

        this.trackedObjectives.add(objective);
    }

    public List<Packet<?>> getStopTrackingPackets(final Objective objective) {
        List<Packet<?>> packets = Lists.newArrayList();
        packets.add(new ClientboundSetObjectivePacket(objective, 1));

        for (DisplaySlot slot : DisplaySlot.values()) {
            if (this.getDisplayObjective(slot) == objective) {
                packets.add(new ClientboundSetDisplayObjectivePacket(slot, objective));
            }
        }

        return packets;
    }

    public void stopTrackingObjective(final Objective objective) {
        List<Packet<?>> packets = this.getStopTrackingPackets(objective);

        for (ServerPlayer player : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : packets) {
                player.connection.send(packet);
            }
        }

        this.trackedObjectives.remove(objective);
    }

    public int getObjectiveDisplaySlotCount(final Objective objective) {
        int count = 0;

        for (DisplaySlot slot : DisplaySlot.values()) {
            if (this.getDisplayObjective(slot) == objective) {
                count++;
            }
        }

        return count;
    }

    private void updatePlayerWaypoint(final String player) {
        ServerPlayer serverPlayer = this.server.getPlayerList().getPlayerByName(player);
        if (serverPlayer != null) {
            serverPlayer.level().getWaypointManager().remakeConnections(serverPlayer);
        }
    }

    private void updateTeamWaypoints(final PlayerTeam team) {
        for (ServerLevel level : this.server.getAllLevels()) {
            team.getPlayers()
                .stream()
                .map(name -> this.server.getPlayerList().getPlayerByName(name))
                .filter(Objects::nonNull)
                .forEach(player -> level.getWaypointManager().remakeConnections(player));
        }
    }
}