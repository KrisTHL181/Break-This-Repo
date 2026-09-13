package net.minecraft.server.jsonrpc.internalapi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import org.jspecify.annotations.Nullable;

public class MinecraftPlayerListServiceImpl implements MinecraftPlayerListService {
   private final JsonRpcLogger jsonRpcLogger;
   private final DedicatedServer server;

   public MinecraftPlayerListServiceImpl(DedicatedServer p_425359_, JsonRpcLogger p_424652_) {
      this.jsonRpcLogger = p_424652_;
      this.server = p_425359_;
   }

   @Override
   public List<ServerPlayer> getPlayers() {
      return this.server.getPlayerList().getPlayers();
   }

   @Override
   public @Nullable ServerPlayer getPlayer(UUID p_430433_) {
      return this.server.getPlayerList().getPlayer(p_430433_);
   }

   @Override
   public Optional<NameAndId> fetchUserByName(String p_423028_) {
      return this.server.services().nameToIdCache().get(p_423028_);
   }

   @Override
   public Optional<NameAndId> fetchUserById(UUID p_422686_) {
      return Optional.ofNullable(this.server.services().sessionService().fetchProfile(p_422686_, true)).map(p_431716_ -> new NameAndId(p_431716_.profile()));
   }

   @Override
   public Optional<NameAndId> getCachedUserById(UUID p_423982_) {
      return this.server.services().nameToIdCache().get(p_423982_);
   }

   @Override
   public Optional<ServerPlayer> getPlayer(Optional<UUID> p_431094_, Optional<String> p_424603_) {
      if (p_431094_.isPresent()) {
         return Optional.ofNullable(this.server.getPlayerList().getPlayer(p_431094_.get()));
      } else {
         return p_424603_.isPresent() ? Optional.ofNullable(this.server.getPlayerList().getPlayerByName(p_424603_.get())) : Optional.empty();
      }
   }

   @Override
   public List<ServerPlayer> getPlayersWithAddress(String p_423879_) {
      return this.server.getPlayerList().getPlayersWithAddress(p_423879_);
   }

   @Override
   public void remove(ServerPlayer p_422778_, ClientInfo p_424616_) {
      this.server.getPlayerList().remove(p_422778_);
      this.jsonRpcLogger.log(p_424616_, "Remove player '{}'", p_422778_.getPlainTextName());
   }

   @Override
   public @Nullable ServerPlayer getPlayerByName(String p_430613_) {
      return this.server.getPlayerList().getPlayerByName(p_430613_);
   }
}
