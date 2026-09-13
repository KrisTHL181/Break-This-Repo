package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.FriendlyByteBuf;

public interface ArgumentTypeInfo<A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> {
   void serializeToNetwork(T template, FriendlyByteBuf out);

   T deserializeFromNetwork(FriendlyByteBuf in);

   void serializeToJson(T template, JsonObject out);

   T unpack(final A argument);

   interface Template<A extends ArgumentType<?>> {
      A instantiate(CommandBuildContext context);

      ArgumentTypeInfo<A, ?> type();
   }
}
