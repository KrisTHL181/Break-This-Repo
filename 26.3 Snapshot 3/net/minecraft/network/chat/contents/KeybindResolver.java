package net.minecraft.network.chat.contents;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public class KeybindResolver {
   static Function<String, Supplier<Component>> keyResolver = name -> () -> Component.literal(name);

   public static void setKeyResolver(final Function<String, Supplier<Component>> resolver) {
      keyResolver = resolver;
   }
}
