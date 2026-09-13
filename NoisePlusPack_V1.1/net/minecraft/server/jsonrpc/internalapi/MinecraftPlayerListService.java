package net.minecraft.server.jsonrpc.internalapi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public interface MinecraftPlayerListService {
   List<ServerPlayer> getPlayers();

   @Nullable ServerPlayer getPlayer(UUID var1);

   default CompletableFuture<Optional<NameAndId>> getUser(Optional<UUID> p_423451_, Optional<String> p_426430_) {
      if (p_423451_.isPresent()) {
         Optional<NameAndId> optional = this.getCachedUserById(p_423451_.get());
         return optional.isPresent()
            ? CompletableFuture.completedFuture(optional)
            : CompletableFuture.supplyAsync(() -> this.fetchUserById(p_423451_.get()), Util.nonCriticalIoPool());
      } else {
         return p_426430_.isPresent()
            ? CompletableFuture.supplyAsync(() -> this.fetchUserByName(p_426430_.get()), Util.nonCriticalIoPool())
            : CompletableFuture.completedFuture(Optional.empty());
      }
   }

   Optional<NameAndId> fetchUserByName(String var1);

   Optional<NameAndId> fetchUserById(UUID var1);

   Optional<NameAndId> getCachedUserById(UUID var1);

   Optional<ServerPlayer> getPlayer(Optional<UUID> var1, Optional<String> var2);

   List<ServerPlayer> getPlayersWithAddress(String var1);

   @Nullable ServerPlayer getPlayerByName(String var1);

   void remove(ServerPlayer var1, ClientInfo var2);
}
