package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface MinecraftExecutorService {
   <V> CompletableFuture<V> submit(Supplier<V> var1);

   CompletableFuture<Void> submit(Runnable var1);
}
