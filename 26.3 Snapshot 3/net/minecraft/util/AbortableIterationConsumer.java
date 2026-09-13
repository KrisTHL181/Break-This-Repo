package net.minecraft.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface AbortableIterationConsumer<T> {
   Continuation accept(T entry);

   static <T> AbortableIterationConsumer<T> forConsumer(final Consumer<T> consumer) {
      return e -> {
         consumer.accept(e);
         return Continuation.CONTINUE;
      };
   }
}
