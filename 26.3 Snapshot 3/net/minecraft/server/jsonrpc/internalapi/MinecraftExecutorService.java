package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface MinecraftExecutorService {
   <V> CompletableFuture<V> submit(final Supplier<V> supplier);

   CompletableFuture<Void> submit(final Runnable runnable);
}
