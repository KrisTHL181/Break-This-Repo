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

   public ServerScoreboard(MinecraftServer p_136197_) {
      this.server = p_136197_;
   }

   public void load(ScoreboardSaveData.Packed p_452163_) {
      p_452163_.objectives().forEach(p_448862_ -> this.loadObjective(p_448862_));
      p_452163_.scores().forEach(p_448858_ -> this.loadPlayerScore(p_448858_));
      p_452163_.displaySlots().forEach((p_448855_, p_448856_) -> {
         Objective objective = this.getObjective(p_448856_);
         this.setDisplayObjective(p_448855_, objective);
      });
      p_452163_.teams().forEach(p_448860_ -> this.loadPlayerTeam(p_448860_));
   }

   private ScoreboardSaveData.Packed store() {
      return new ScoreboardSaveData.Packed(this.packObjectives(), this.packPlayerScores(), this.packDisplaySlots(), this.packPlayerTeams());
   }

   @Override
   protected void onScoreChanged(ScoreHolder p_311591_, Objective p_310366_, Score p_136206_) {
      super.onScoreChanged(p_311591_, p_310366_, p_136206_);
      if (this.trackedObjectives.contains(p_310366_)) {
         this.server
            .getPlayerList()
            .broadcastAll(
               new ClientboundSetScorePacket(
                  p_311591_.getScoreboardName(),
                  p_310366_.getName(),
                  p_136206_.value(),
                  Optional.ofNullable(p_136206_.display()),
                  Optional.ofNullable(p_136206_.numberFormat())
               )
            );
      }

      this.setDirty();
   }

   @Override
   protected void onScoreLockChanged(ScoreHolder p_309548_, Objective p_312571_) {
      super.onScoreLockChanged(p_309548_, p_312571_);
      this.setDirty();
   }

   @Override
   public void onPlayerRemoved(ScoreHolder p_310662_) {
      super.onPlayerRemoved(p_310662_);
      this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_310662_.getScoreboardName(), null));
      this.setDirty();
   }

   @Override
   public void onPlayerScoreRemoved(ScoreHolder p_310122_, Objective p_136213_) {
      super.onPlayerScoreRemoved(p_310122_, p_136213_);
      if (this.trackedObjectives.contains(p_136213_)) {
         this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(p_310122_.getScoreboardName(), p_136213_.getName()));
      }

      this.setDirty();
   }

   @Override
   public void setDisplayObjective(DisplaySlot p_297629_, @Nullable Objective p_136200_) {
      Objective objective = this.getDisplayObjective(p_297629_);
      super.setDisplayObjective(p_297629_, p_136200_);
      if (objective != p_136200_ && objective != null) {
         if (this.getObjectiveDisplaySlotCount(objective) > 0) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_297629_, p_136200_));
         } else {
            this.stopTrackingObjective(objective);
         }
      }

      if (p_136200_ != null) {
         if (this.trackedObjectives.contains(p_136200_)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(p_297629_, p_136200_));
         } else {
            this.startTrackingObjective(p_136200_);
         }
      }

      this.setDirty();
   }

   @Override
   public boolean addPlayerToTeam(String p_136215_, PlayerTeam p_136216_) {
      if (super.addPlayerToTeam(p_136215_, p_136216_)) {
         this.server
            .getPlayerList()
            .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(p_136216_, p_136215_, ClientboundSetPlayerTeamPacket.Action.ADD));
         this.updatePlayerWaypoint(p_136215_);
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public void removePlayerFromTeam(String p_136223_, PlayerTeam p_136224_) {
      super.removePlayerFromTeam(p_136223_, p_136224_);
      this.server
         .getPlayerList()
         .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(p_136224_, p_136223_, ClientboundSetPlayerTeamPacket.Action.REMOVE));
      this.updatePlayerWaypoint(p_136223_);
      this.setDirty();
   }

   @Override
   public void onObjectiveAdded(Objective p_136202_) {
      super.onObjectiveAdded(p_136202_);
      this.setDirty();
   }

   @Override
   public void onObjectiveChanged(Objective p_136219_) {
      super.onObjectiveChanged(p_136219_);
      if (this.trackedObjectives.contains(p_136219_)) {
         this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(p_136219_, 2));
      }

      this.setDirty();
   }

   @Override
   public void onObjectiveRemoved(Objective p_136226_) {
      super.onObjectiveRemoved(p_136226_);
      if (this.trackedObjectives.contains(p_136226_)) {
         this.stopTrackingObjective(p_136226_);
      }

      this.setDirty();
   }

   @Override
   public void onTeamAdded(PlayerTeam p_136204_) {
      super.onTeamAdded(p_136204_);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(p_136204_, true));
      this.setDirty();
   }

   @Override
   public void onTeamChanged(PlayerTeam p_136221_) {
      super.onTeamChanged(p_136221_);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(p_136221_, false));
      this.updateTeamWaypoints(p_136221_);
      this.setDirty();
   }

   @Override
   public void onTeamRemoved(PlayerTeam p_136228_) {
      super.onTeamRemoved(p_136228_);
      this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(p_136228_));
      this.updateTeamWaypoints(p_136228_);
      this.setDirty();
   }

   protected void setDirty() {
      this.dirty = true;
   }

   public void storeToSaveDataIfDirty(ScoreboardSaveData p_454601_) {
      if (this.dirty) {
         this.dirty = false;
         p_454601_.setData(this.store());
      }
   }

   public List<Packet<?>> getStartTrackingPackets(Objective p_136230_) {
      List<Packet<?>> list = Lists.newArrayList();
      list.add(new ClientboundSetObjectivePacket(p_136230_, 0));

      for (DisplaySlot displayslot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displayslot) == p_136230_) {
            list.add(new ClientboundSetDisplayObjectivePacket(displayslot, p_136230_));
         }
      }

      for (PlayerScoreEntry playerscoreentry : this.listPlayerScores(p_136230_)) {
         list.add(
            new ClientboundSetScorePacket(
               playerscoreentry.owner(),
               p_136230_.getName(),
               playerscoreentry.value(),
               Optional.ofNullable(playerscoreentry.display()),
               Optional.ofNullable(playerscoreentry.numberFormatOverride())
            )
         );
      }

      return list;
   }

   public void startTrackingObjective(Objective p_136232_) {
      List<Packet<?>> list = this.getStartTrackingPackets(p_136232_);

      for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
         for (Packet<?> packet : list) {
            serverplayer.connection.send(packet);
         }
      }

      this.trackedObjectives.add(p_136232_);
   }

   public List<Packet<?>> getStopTrackingPackets(Objective p_136234_) {
      List<Packet<?>> list = Lists.newArrayList();
      list.add(new ClientboundSetObjectivePacket(p_136234_, 1));

      for (DisplaySlot displayslot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displayslot) == p_136234_) {
            list.add(new ClientboundSetDisplayObjectivePacket(displayslot, p_136234_));
         }
      }

      return list;
   }

   public void stopTrackingObjective(Objective p_136236_) {
      List<Packet<?>> list = this.getStopTrackingPackets(p_136236_);

      for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
         for (Packet<?> packet : list) {
            serverplayer.connection.send(packet);
         }
      }

      this.trackedObjectives.remove(p_136236_);
   }

   public int getObjectiveDisplaySlotCount(Objective p_136238_) {
      int i = 0;

      for (DisplaySlot displayslot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displayslot) == p_136238_) {
            i++;
         }
      }

      return i;
   }

   private void updatePlayerWaypoint(String p_407437_) {
      ServerPlayer serverplayer = this.server.getPlayerList().getPlayerByName(p_407437_);
      if (serverplayer != null) {
         serverplayer.level().getWaypointManager().remakeConnections(serverplayer);
      }
   }

   private void updateTeamWaypoints(PlayerTeam p_408233_) {
      for (ServerLevel serverlevel : this.server.getAllLevels()) {
         p_408233_.getPlayers()
            .stream()
            .map(p_405130_ -> this.server.getPlayerList().getPlayerByName(p_405130_))
            .filter(Objects::nonNull)
            .forEach(p_405129_ -> serverlevel.getWaypointManager().remakeConnections(p_405129_));
      }
   }
}
