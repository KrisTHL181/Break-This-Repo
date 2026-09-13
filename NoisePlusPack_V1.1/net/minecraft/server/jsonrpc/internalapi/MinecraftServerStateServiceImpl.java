package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class MinecraftServerStateServiceImpl implements MinecraftServerStateService {
   private final DedicatedServer server;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftServerStateServiceImpl(DedicatedServer p_430408_, JsonRpcLogger p_430048_) {
      this.server = p_430408_;
      this.jsonrpcLogger = p_430048_;
   }

   @Override
   public boolean isReady() {
      return this.server.isReady();
   }

   @Override
   public boolean saveEverything(boolean p_425613_, boolean p_426815_, boolean p_429631_, ClientInfo p_425496_) {
      this.jsonrpcLogger.log(p_425496_, "Save everything. SuppressLogs: {}, flush: {}, force: {}", p_425613_, p_426815_, p_429631_);
      return this.server.saveEverything(p_425613_, p_426815_, p_429631_);
   }

   @Override
   public void halt(boolean p_430716_, ClientInfo p_426982_) {
      this.jsonrpcLogger.log(p_426982_, "Halt server. WaitForShutdown: {}", p_430716_);
      this.server.halt(p_430716_);
   }

   @Override
   public void sendSystemMessage(Component p_426049_, ClientInfo p_428370_) {
      this.jsonrpcLogger.log(p_428370_, "Send system message: '{}'", p_426049_.getString());
      this.server.sendSystemMessage(p_426049_);
   }

   @Override
   public void sendSystemMessage(Component p_427472_, boolean p_431684_, Collection<ServerPlayer> p_424018_, ClientInfo p_424617_) {
      List<String> list = p_424018_.stream().map(Player::getPlainTextName).toList();
      this.jsonrpcLogger.log(p_424617_, "Send system message to '{}' players (overlay: {}): '{}'", list.size(), p_431684_, p_427472_.getString());

      for (ServerPlayer serverplayer : p_424018_) {
         if (p_431684_) {
            serverplayer.sendSystemMessage(p_427472_, true);
         } else {
            serverplayer.sendSystemMessage(p_427472_);
         }
      }
   }

   @Override
   public void broadcastSystemMessage(Component p_430601_, boolean p_430520_, ClientInfo p_428432_) {
      this.jsonrpcLogger.log(p_428432_, "Broadcast system message (overlay: {}): '{}'", p_430520_, p_430601_.getString());

      for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
         if (p_430520_) {
            serverplayer.sendSystemMessage(p_430601_, true);
         } else {
            serverplayer.sendSystemMessage(p_430601_);
         }
      }
   }
}
