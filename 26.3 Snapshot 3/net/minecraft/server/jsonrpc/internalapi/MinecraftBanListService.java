package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;

public interface MinecraftBanListService {
   void addUserBan(UserBanListEntry ban, ClientInfo clientInfo);

   void removeUserBan(NameAndId nameAndId, ClientInfo clientInfo);

   Collection<UserBanListEntry> getUserBanEntries();

   Collection<IpBanListEntry> getIpBanEntries();

   void addIpBan(IpBanListEntry ipBanEntry, ClientInfo clientInfo);

   void clearIpBans(ClientInfo clientInfo);

   void removeIpBan(String ip, ClientInfo clientInfo);

   void clearUserBans(ClientInfo clientInfo);
}
