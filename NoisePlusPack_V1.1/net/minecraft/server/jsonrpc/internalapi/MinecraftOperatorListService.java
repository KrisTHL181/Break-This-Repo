package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;

public interface MinecraftOperatorListService {
   Collection<ServerOpListEntry> getEntries();

   void op(NameAndId var1, Optional<PermissionLevel> var2, Optional<Boolean> var3, ClientInfo var4);

   void op(NameAndId var1, ClientInfo var2);

   void deop(NameAndId var1, ClientInfo var2);

   void clear(ClientInfo var1);
}
