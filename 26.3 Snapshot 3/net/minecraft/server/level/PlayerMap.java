package net.minecraft.server.level;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Set;

public final class PlayerMap {
   private final Object2BooleanMap<ServerPlayer> players = new Object2BooleanOpenHashMap();

   public Set<ServerPlayer> getAllPlayers() {
      return this.players.keySet();
   }

   public void addPlayer(final ServerPlayer player, final boolean ignored) {
      this.players.put(player, ignored);
   }

   public void removePlayer(final ServerPlayer player) {
      this.players.removeBoolean(player);
   }

   public void ignorePlayer(final ServerPlayer player) {
      this.players.replace(player, true);
   }

   public void unIgnorePlayer(final ServerPlayer player) {
      this.players.replace(player, false);
   }

   public boolean ignoredOrUnknown(final ServerPlayer player) {
      return this.players.getOrDefault(player, true);
   }

   public boolean ignored(final ServerPlayer player) {
      return this.players.getBoolean(player);
   }
}
