package net.minecraft.server.jsonrpc.internalapi;

import java.util.Collection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.jsonrpc.methods.ClientInfo;
import net.minecraft.server.level.ServerPlayer;

public interface MinecraftServerStateService {
   boolean isReady();

   boolean saveEverything(boolean var1, boolean var2, boolean var3, ClientInfo var4);

   void halt(boolean var1, ClientInfo var2);

   void sendSystemMessage(Component var1, ClientInfo var2);

   void sendSystemMessage(Component var1, boolean var2, Collection<ServerPlayer> var3, ClientInfo var4);

   void broadcastSystemMessage(Component var1, boolean var2, ClientInfo var3);
}
