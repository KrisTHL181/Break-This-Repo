package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

public interface MinecraftBanListService {
   void addUserBan(UserBanListEntry var1, ClientInfo var2);

   void removeUserBan(NameAndId var1, ClientInfo var2);

   Collection<UserBanListEntry> getUserBanEntries();

   Collection<IpBanListEntry> getIpBanEntries();

   void addIpBan(IpBanListEntry var1, ClientInfo var2);

   void clearIpBans(ClientInfo var1);

   void removeIpBan(String var1, ClientInfo var2);

   void clearUserBans(ClientInfo var1);
}
