package net.minecraft.server.notifications;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public interface NotificationService {
    void playerJoined(ServerPlayer player);

    void playerLeft(ServerPlayer player);

    void serverStarted();

    void serverShuttingDown();

    void serverSaveStarted();

    void serverSaveCompleted();

    void serverActivityOccured();

    void playerOped(ServerOpListEntry operator);

    void playerDeoped(ServerOpListEntry operator);

    void playerAddedToAllowlist(NameAndId player);

    void playerRemovedFromAllowlist(NameAndId player);

    void ipBanned(IpBanListEntry ban);

    void ipUnbanned(String ip);

    void playerBanned(UserBanListEntry ban);

    void playerUnbanned(NameAndId player);

    <T> void onGameRuleChanged(GameRule<T> gameRule, T value);

    void statusHeartbeat();
}