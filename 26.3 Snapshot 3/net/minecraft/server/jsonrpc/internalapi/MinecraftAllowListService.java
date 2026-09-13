package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;

public interface MinecraftAllowListService {
   Collection<UserWhiteListEntry> getEntries();

   boolean add(UserWhiteListEntry infos, ClientInfo clientInfo);

   void clear(ClientInfo clientInfo);

   void remove(NameAndId nameAndId, ClientInfo clientInfo);

   void kickUnlistedPlayers(ClientInfo clientInfo);
}
