package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

public class MinecraftOperatorListServiceImpl implements MinecraftOperatorListService {
   private final MinecraftServer minecraftServer;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftOperatorListServiceImpl(MinecraftServer p_425195_, JsonRpcLogger p_426558_) {
      this.minecraftServer = p_425195_;
      this.jsonrpcLogger = p_426558_;
   }

   @Override
   public Collection<ServerOpListEntry> getEntries() {
      return this.minecraftServer.getPlayerList().getOps().getEntries();
   }

   @Override
   public void op(NameAndId p_424857_, Optional<PermissionLevel> p_423949_, Optional<Boolean> p_422334_, ClientInfo p_425067_) {
      this.jsonrpcLogger.log(p_425067_, "Op '{}'", p_424857_);
      this.minecraftServer.getPlayerList().op(p_424857_, p_423949_.map(LevelBasedPermissionSet::forLevel), p_422334_);
   }

   @Override
   public void op(NameAndId p_426139_, ClientInfo p_429350_) {
      this.jsonrpcLogger.log(p_429350_, "Op '{}'", p_426139_);
      this.minecraftServer.getPlayerList().op(p_426139_);
   }

   @Override
   public void deop(NameAndId p_424738_, ClientInfo p_423083_) {
      this.jsonrpcLogger.log(p_423083_, "Deop '{}'", p_424738_);
      this.minecraftServer.getPlayerList().deop(p_424738_);
   }

   @Override
   public void clear(ClientInfo p_427287_) {
      this.jsonrpcLogger.log(p_427287_, "Clear operator list");
      this.minecraftServer.getPlayerList().getOps().clear();
   }
}
