package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.FriendlyByteBuf;

public class SingletonArgumentInfo<A extends ArgumentType<?>> implements ArgumentTypeInfo<A, SingletonArgumentInfo<A>.Template> {
   private final SingletonArgumentInfo<A>.Template template;

   private SingletonArgumentInfo(final Function<CommandBuildContext, A> constructor) {
      this.template = new SingletonArgumentInfo.Template(constructor);
   }

   public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextFree(final Supplier<T> constructor) {
      return new SingletonArgumentInfo<>(context -> constructor.get());
   }

   public static <T extends ArgumentType<?>> SingletonArgumentInfo<T> contextAware(final Function<CommandBuildContext, T> constructor) {
      return new SingletonArgumentInfo<>(constructor);
   }

   public void serializeToNetwork(final SingletonArgumentInfo<A>.Template template, final FriendlyByteBuf out) {
   }

   public void serializeToJson(final SingletonArgumentInfo<A>.Template template, final JsonObject out) {
   }

   public SingletonArgumentInfo<A>.Template deserializeFromNetwork(final FriendlyByteBuf in) {
      return this.template;
   }

   public SingletonArgumentInfo<A>.Template unpack(final A argument) {
      return this.template;
   }

   public final class Template implements ArgumentTypeInfo.Template<A> {
      private final Function<CommandBuildContext, A> constructor;

      public Template(final Function<CommandBuildContext, A> constructor) {
         this.constructor = constructor;
      }

      @Override
      public A instantiate(final CommandBuildContext context) {
         return this.constructor.apply(context);
      }

      @Override
      public ArgumentTypeInfo<A, ?> type() {
         return SingletonArgumentInfo.this;
      }
   }
}
