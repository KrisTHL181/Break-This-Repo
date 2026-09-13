package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;

public class FutureChain implements TaskChainer, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private CompletableFuture<?> head = CompletableFuture.completedFuture(null);
   private final Executor executor;
   private volatile boolean closed;

   public FutureChain(final Executor executor) {
      this.executor = executor;
   }

   @Override
   public <T> void append(final CompletableFuture<T> preparation, final Consumer<T> chainedTask) {
      this.head = this.head.<T, Object>thenCombine(preparation, (ignored, value) -> value).thenAcceptAsync(value -> {
         if (!this.closed) {
            chainedTask.accept((T)value);
         }
      }, this.executor).exceptionally(t -> {
         if (t instanceof CompletionException c) {
            t = c.getCause();
         }

         if (t instanceof CancellationException c) {
            throw c;
         } else {
            LOGGER.error("Chain link failed, continuing to next one", t);
            return null;
         }
      });
   }

   @Override
   public void close() {
      this.closed = true;
   }
}
