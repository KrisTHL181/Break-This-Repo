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

   @Nullable ServerPlayer getPlayer(UUID uuid);

   default CompletableFuture<Optional<NameAndId>> getUser(final Optional<UUID> id, final Optional<String> name) {
      if (id.isPresent()) {
         Optional<NameAndId> nameAndId = this.getCachedUserById(id.get());
         return nameAndId.isPresent()
            ? CompletableFuture.completedFuture(nameAndId)
            : CompletableFuture.supplyAsync(() -> this.fetchUserById(id.get()), Util.nonCriticalIoPool());
      } else {
         return name.isPresent()
            ? CompletableFuture.supplyAsync(() -> this.fetchUserByName(name.get()), Util.nonCriticalIoPool())
            : CompletableFuture.completedFuture(Optional.empty());
      }
   }

   Optional<NameAndId> fetchUserByName(String name);

   Optional<NameAndId> fetchUserById(UUID id);

   Optional<NameAndId> getCachedUserById(UUID id);

   Optional<ServerPlayer> getPlayer(Optional<UUID> id, Optional<String> name);

   List<ServerPlayer> getPlayersWithAddress(String ip);

   @Nullable ServerPlayer getPlayerByName(String name);

   void remove(ServerPlayer player, ClientInfo clientInfo);
}
