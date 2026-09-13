package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

public class MinecraftAllowListServiceImpl implements MinecraftAllowListService {
   private final DedicatedServer server;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftAllowListServiceImpl(DedicatedServer p_426754_, JsonRpcLogger p_426058_) {
      this.server = p_426754_;
      this.jsonrpcLogger = p_426058_;
   }

   @Override
   public Collection<UserWhiteListEntry> getEntries() {
      return this.server.getPlayerList().getWhiteList().getEntries();
   }

   @Override
   public boolean add(UserWhiteListEntry p_426781_, ClientInfo p_429072_) {
      this.jsonrpcLogger.log(p_429072_, "Add player '{}' to allowlist", p_426781_.getUser());
      return this.server.getPlayerList().getWhiteList().add(p_426781_);
   }

   @Override
   public void clear(ClientInfo p_425321_) {
      this.jsonrpcLogger.log(p_425321_, "Clear allowlist");
      this.server.getPlayerList().getWhiteList().clear();
   }

   @Override
   public void remove(NameAndId p_428940_, ClientInfo p_430665_) {
      this.jsonrpcLogger.log(p_430665_, "Remove player '{}' from allowlist", p_428940_);
      this.server.getPlayerList().getWhiteList().remove(p_428940_);
   }

   @Override
   public void kickUnlistedPlayers(ClientInfo p_422540_) {
      this.jsonrpcLogger.log(p_422540_, "Kick unlisted players");
      this.server.kickUnlistedPlayers();
   }
}
