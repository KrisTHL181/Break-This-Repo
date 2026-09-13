package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

public interface MinecraftAllowListService {
   Collection<UserWhiteListEntry> getEntries();

   boolean add(UserWhiteListEntry var1, ClientInfo var2);

   void clear(ClientInfo var1);

   void remove(NameAndId var1, ClientInfo var2);

   void kickUnlistedPlayers(ClientInfo var1);
}
