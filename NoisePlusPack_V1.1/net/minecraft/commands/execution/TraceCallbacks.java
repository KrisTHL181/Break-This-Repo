package net.minecraft.commands.execution;

import net.minecraft.resources.Identifier;

public interface TraceCallbacks extends AutoCloseable {
   void onCommand(int var1, String var2);

   void onReturn(int var1, String var2, int var3);

   void onError(String var1);

   void onCall(int var1, Identifier var2, int var3);

   @Override
   void close();
}
