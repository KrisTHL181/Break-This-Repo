package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

public class MinecraftBanListServiceImpl implements MinecraftBanListService {
   private final MinecraftServer server;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftBanListServiceImpl(MinecraftServer p_424510_, JsonRpcLogger p_430124_) {
      this.server = p_424510_;
      this.jsonrpcLogger = p_430124_;
   }

   @Override
   public void addUserBan(UserBanListEntry p_427549_, ClientInfo p_425903_) {
      this.jsonrpcLogger.log(p_425903_, "Add player '{}' to banlist. Reason: '{}'", p_427549_.getDisplayName(), p_427549_.getReasonMessage().getString());
      this.server.getPlayerList().getBans().add(p_427549_);
   }

   @Override
   public void removeUserBan(NameAndId p_422799_, ClientInfo p_429887_) {
      this.jsonrpcLogger.log(p_429887_, "Remove player '{}' from banlist", p_422799_);
      this.server.getPlayerList().getBans().remove(p_422799_);
   }

   @Override
   public void clearUserBans(ClientInfo p_428601_) {
      this.server.getPlayerList().getBans().clear();
   }

   @Override
   public Collection<UserBanListEntry> getUserBanEntries() {
      return this.server.getPlayerList().getBans().getEntries();
   }

   @Override
   public Collection<IpBanListEntry> getIpBanEntries() {
      return this.server.getPlayerList().getIpBans().getEntries();
   }

   @Override
   public void addIpBan(IpBanListEntry p_426890_, ClientInfo p_425842_) {
      this.jsonrpcLogger.log(p_425842_, "Add ip '{}' to ban list", p_426890_.getUser());
      this.server.getPlayerList().getIpBans().add(p_426890_);
   }

   @Override
   public void clearIpBans(ClientInfo p_429672_) {
      this.jsonrpcLogger.log(p_429672_, "Clear ip ban list");
      this.server.getPlayerList().getIpBans().clear();
   }

   @Override
   public void removeIpBan(String p_430586_, ClientInfo p_428355_) {
      this.jsonrpcLogger.log(p_428355_, "Remove ip '{}' from ban list", p_430586_);
      this.server.getPlayerList().getIpBans().remove(p_430586_);
   }
}
