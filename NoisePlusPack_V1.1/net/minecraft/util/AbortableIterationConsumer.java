package net.minecraft.util;

import java.util.function.Consumer;

@FunctionalInterface
public interface AbortableIterationConsumer<T> {
   AbortableIterationConsumer.Continuation accept(T var1);

   static <T> AbortableIterationConsumer<T> forConsumer(Consumer<T> p_261477_) {
      return p_261916_ -> {
         p_261477_.accept(p_261916_);
         return AbortableIterationConsumer.Continuation.CONTINUE;
      };
   }

   enum Continuation {
      CONTINUE,
      ABORT;

      public boolean shouldAbort() {
         return this == ABORT;
      }
   }
}
