package net.minecraft.server.notifications;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public interface NotificationService {
   void playerJoined(ServerPlayer var1);

   void playerLeft(ServerPlayer var1);

   void serverStarted();

   void serverShuttingDown();

   void serverSaveStarted();

   void serverSaveCompleted();

   void serverActivityOccured();

   void playerOped(ServerOpListEntry var1);

   void playerDeoped(ServerOpListEntry var1);

   void playerAddedToAllowlist(NameAndId var1);

   void playerRemovedFromAllowlist(NameAndId var1);

   void ipBanned(IpBanListEntry var1);

   void ipUnbanned(String var1);

   void playerBanned(UserBanListEntry var1);

   void playerUnbanned(NameAndId var1);

   <T> void onGameRuleChanged(GameRule<T> var1, T var2);

   void statusHeartbeat();
}
