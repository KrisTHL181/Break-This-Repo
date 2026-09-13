package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.FriendlyByteBuf;

public interface ArgumentTypeInfo<A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> {
   void serializeToNetwork(T var1, FriendlyByteBuf var2);

   T deserializeFromNetwork(FriendlyByteBuf var1);

   void serializeToJson(T var1, JsonObject var2);

   T unpack(A var1);

   interface Template<A extends ArgumentType<?>> {
      A instantiate(CommandBuildContext var1);

      ArgumentTypeInfo<A, ?> type();
   }
}
