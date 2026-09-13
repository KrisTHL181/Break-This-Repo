package net.minecraft.util.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public class ServerDebugSubscribers {
   private final MinecraftServer server;
   private final Map<DebugSubscription<?>, List<ServerPlayer>> enabledSubscriptions = new HashMap<>();

   public ServerDebugSubscribers(MinecraftServer p_428570_) {
      this.server = p_428570_;
   }

   private List<ServerPlayer> getSubscribersFor(DebugSubscription<?> p_431226_) {
      return this.enabledSubscriptions.getOrDefault(p_431226_, List.of());
   }

   public void tick() {
      this.enabledSubscriptions.values().forEach(List::clear);

      for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
         for (DebugSubscription<?> debugsubscription : serverplayer.debugSubscriptions()) {
            this.enabledSubscriptions.computeIfAbsent(debugsubscription, p_427534_ -> new ArrayList<>()).add(serverplayer);
         }
      }

      this.enabledSubscriptions.values().removeIf(List::isEmpty);
   }

   public void broadcastToAll(DebugSubscription<?> p_425191_, Packet<?> p_427581_) {
      for (ServerPlayer serverplayer : this.getSubscribersFor(p_425191_)) {
         serverplayer.connection.send(p_427581_);
      }
   }

   public Set<DebugSubscription<?>> enabledSubscriptions() {
      return Set.copyOf(this.enabledSubscriptions.keySet());
   }

   public boolean hasAnySubscriberFor(DebugSubscription<?> p_429963_) {
      return !this.getSubscribersFor(p_429963_).isEmpty();
   }

   public boolean hasRequiredPermissions(ServerPlayer p_431332_) {
      NameAndId nameandid = p_431332_.nameAndId();
      return SharedConstants.IS_RUNNING_IN_IDE && this.server.isSingleplayerOwner(nameandid) ? true : this.server.getPlayerList().isOp(nameandid);
   }
}
