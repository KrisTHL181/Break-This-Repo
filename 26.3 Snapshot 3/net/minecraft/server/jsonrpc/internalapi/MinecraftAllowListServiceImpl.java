package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Objects;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

public class MinecraftAllowListServiceImpl implements MinecraftAllowListService {
   private final NotificationManager notificationManager;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftAllowListServiceImpl(final NotificationManager notificationManager, final JsonRpcLogger jsonrpcLogger) {
      this.notificationManager = notificationManager;
      this.jsonrpcLogger = jsonrpcLogger;
   }

   private DedicatedServer server() {
      return Objects.requireNonNull(this.notificationManager.server());
   }

   @Override
   public Collection<UserWhiteListEntry> getEntries() {
      return this.server().getPlayerList().getWhiteList().getEntries();
   }

   @Override
   public boolean add(final UserWhiteListEntry infos, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Add player '{}' to allowlist", infos.getUser());
      return this.server().getPlayerList().getWhiteList().add(infos);
   }

   @Override
   public void clear(final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Clear allowlist");
      this.server().getPlayerList().getWhiteList().clear();
   }

   @Override
   public void remove(final NameAndId nameAndId, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Remove player '{}' from allowlist", nameAndId);
      this.server().getPlayerList().getWhiteList().remove(nameAndId);
   }

   @Override
   public void kickUnlistedPlayers(final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Kick unlisted players");
      this.server().kickUnlistedPlayers();
   }
}
