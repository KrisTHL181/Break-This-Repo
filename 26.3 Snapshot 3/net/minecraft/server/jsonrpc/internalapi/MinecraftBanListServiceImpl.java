package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Objects;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.jsonrpc.JsonRpcLogger;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.notifications.NotificationManager;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

public class MinecraftBanListServiceImpl implements MinecraftBanListService {
   private final NotificationManager notificationManager;
   private final JsonRpcLogger jsonrpcLogger;

   public MinecraftBanListServiceImpl(final NotificationManager notificationManager, final JsonRpcLogger jsonrpcLogger) {
      this.notificationManager = notificationManager;
      this.jsonrpcLogger = jsonrpcLogger;
   }

   private DedicatedServer server() {
      return Objects.requireNonNull(this.notificationManager.server());
   }

   @Override
   public void addUserBan(final UserBanListEntry ban, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Add player '{}' to banlist. Reason: '{}'", ban.getDisplayName(), ban.getReasonMessage().getString());
      this.server().getPlayerList().getBans().add(ban);
   }

   @Override
   public void removeUserBan(final NameAndId nameAndId, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Remove player '{}' from banlist", nameAndId);
      this.server().getPlayerList().getBans().remove(nameAndId);
   }

   @Override
   public void clearUserBans(final ClientInfo clientInfo) {
      this.server().getPlayerList().getBans().clear();
   }

   @Override
   public Collection<UserBanListEntry> getUserBanEntries() {
      return this.server().getPlayerList().getBans().getEntries();
   }

   @Override
   public Collection<IpBanListEntry> getIpBanEntries() {
      return this.server().getPlayerList().getIpBans().getEntries();
   }

   @Override
   public void addIpBan(final IpBanListEntry ipBanEntry, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Add ip '{}' to ban list", ipBanEntry.getUser());
      this.server().getPlayerList().getIpBans().add(ipBanEntry);
   }

   @Override
   public void clearIpBans(final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Clear ip ban list");
      this.server().getPlayerList().getIpBans().clear();
   }

   @Override
   public void removeIpBan(final String ip, final ClientInfo clientInfo) {
      this.jsonrpcLogger.log(clientInfo, "Remove ip '{}' from ban list", ip);
      this.server().getPlayerList().getIpBans().remove(ip);
   }
}
