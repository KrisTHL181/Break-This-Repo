package net.minecraft.server.waypoints;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.common.collect.UnmodifiableIterator;
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

   public void trackWaypoint(WaypointTransmitter p_408241_) {
      this.waypoints.add(p_408241_);

      for (ServerPlayer serverplayer : this.players) {
         this.createConnection(serverplayer, p_408241_);
      }
   }

   public void updateWaypoint(WaypointTransmitter p_409897_) {
      if (this.waypoints.contains(p_409897_)) {
         Map<ServerPlayer, WaypointTransmitter.Connection> map = Tables.transpose(this.connections).row(p_409897_);
         SetView<ServerPlayer> setview = Sets.difference(this.players, map.keySet());
         UnmodifiableIterator var4 = ImmutableSet.copyOf(map.entrySet()).iterator();

         while (var4.hasNext()) {
            Entry<ServerPlayer, WaypointTransmitter.Connection> entry = (Entry<ServerPlayer, WaypointTransmitter.Connection>)var4.next();
            this.updateConnection(entry.getKey(), p_409897_, entry.getValue());
         }

         var4 = setview.iterator();

         while (var4.hasNext()) {
            ServerPlayer serverplayer = (ServerPlayer)var4.next();
            this.createConnection(serverplayer, p_409897_);
         }
      }
   }

   public void untrackWaypoint(WaypointTransmitter p_406555_) {
      this.connections.column(p_406555_).forEach((p_408654_, p_407919_) -> p_407919_.disconnect());
      Tables.transpose(this.connections).row(p_406555_).clear();
      this.waypoints.remove(p_406555_);
   }

   public void addPlayer(ServerPlayer p_407322_) {
      this.players.add(p_407322_);

      for (WaypointTransmitter waypointtransmitter : this.waypoints) {
         this.createConnection(p_407322_, waypointtransmitter);
      }

      if (p_407322_.isTransmittingWaypoint()) {
         this.trackWaypoint(p_407322_);
      }
   }

   public void updatePlayer(ServerPlayer p_406327_) {
      Map<WaypointTransmitter, WaypointTransmitter.Connection> map = this.connections.row(p_406327_);
      SetView<WaypointTransmitter> setview = Sets.difference(this.waypoints, map.keySet());
      UnmodifiableIterator var4 = ImmutableSet.copyOf(map.entrySet()).iterator();

      while (var4.hasNext()) {
         Entry<WaypointTransmitter, WaypointTransmitter.Connection> entry = (Entry<WaypointTransmitter, WaypointTransmitter.Connection>)var4.next();
         this.updateConnection(p_406327_, entry.getKey(), entry.getValue());
      }

      var4 = setview.iterator();

      while (var4.hasNext()) {
         WaypointTransmitter waypointtransmitter = (WaypointTransmitter)var4.next();
         this.createConnection(p_406327_, waypointtransmitter);
      }
   }

   public void removePlayer(ServerPlayer p_409853_) {
      this.connections.row(p_409853_).values().removeIf(p_409511_ -> {
         p_409511_.disconnect();
         return true;
      });
      this.untrackWaypoint(p_409853_);
      this.players.remove(p_409853_);
   }

   public void breakAllConnections() {
      this.connections.values().forEach(WaypointTransmitter.Connection::disconnect);
      this.connections.clear();
   }

   public void remakeConnections(WaypointTransmitter p_407306_) {
      for (ServerPlayer serverplayer : this.players) {
         this.createConnection(serverplayer, p_407306_);
      }
   }

   public Set<WaypointTransmitter> transmitters() {
      return this.waypoints;
   }

   private static boolean isLocatorBarEnabledFor(ServerPlayer p_405810_) {
      return p_405810_.level().getGameRules().get(GameRules.LOCATOR_BAR);
   }

   private void createConnection(ServerPlayer p_406768_, WaypointTransmitter p_406530_) {
      if (p_406768_ != p_406530_ && isLocatorBarEnabledFor(p_406768_)) {
         p_406530_.makeWaypointConnectionWith(p_406768_).ifPresentOrElse(p_407837_ -> {
            this.connections.put(p_406768_, p_406530_, p_407837_);
            p_407837_.connect();
         }, () -> {
            WaypointTransmitter.Connection waypointtransmitter$connection = (WaypointTransmitter.Connection)this.connections.remove(p_406768_, p_406530_);
            if (waypointtransmitter$connection != null) {
               waypointtransmitter$connection.disconnect();
            }
         });
      }
   }

   private void updateConnection(ServerPlayer p_409540_, WaypointTransmitter p_410271_, WaypointTransmitter.Connection p_409943_) {
      if (p_409540_ != p_410271_ && isLocatorBarEnabledFor(p_409540_)) {
         if (!p_409943_.isBroken()) {
            p_409943_.update();
         } else {
            p_410271_.makeWaypointConnectionWith(p_409540_).ifPresentOrElse(p_408633_ -> {
               p_408633_.connect();
               this.connections.put(p_409540_, p_410271_, p_408633_);
            }, () -> {
               p_409943_.disconnect();
               this.connections.remove(p_409540_, p_410271_);
            });
         }
      }
   }
}
