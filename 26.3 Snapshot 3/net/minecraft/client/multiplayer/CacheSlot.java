package net.minecraft.client.multiplayer;

import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public class CacheSlot<C extends CacheSlot.Cleaner<C>, D> {
   private final Function<C, D> operation;
   private @Nullable C context;
   private @Nullable D value;

   public CacheSlot(final Function<C, D> operation) {
      this.operation = operation;
   }

   public D compute(final C context) {
      if (context == this.context && this.value != null) {
         return this.value;
      }

      D newValue = this.operation.apply(context);
      this.value = newValue;
      this.context = context;
      context.registerForCleaning(this);
      return newValue;
   }

   public void clear() {
      this.value = null;
      this.context = null;
   }

   @FunctionalInterface
   public interface Cleaner<C extends CacheSlot.Cleaner<C>> {
      void registerForCleaning(CacheSlot<C, ?> slot);
   }
}
