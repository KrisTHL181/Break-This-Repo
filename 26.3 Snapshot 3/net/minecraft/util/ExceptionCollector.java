package net.minecraft.util;

import org.jspecify.annotations.Nullable;

public class ExceptionCollector<T extends Throwable> {
   private @Nullable T result;

   public void add(final T throwable) {
      if (this.result == null) {
         this.result = throwable;
      } else {
         this.result.addSuppressed(throwable);
      }
   }

   public void throwIfPresent() throws T {
      if (this.result != null) {
         throw this.result;
      }
   }
}
