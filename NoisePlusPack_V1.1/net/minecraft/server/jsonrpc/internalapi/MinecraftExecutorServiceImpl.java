package net.minecraft.server.jsonrpc.internalapi;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.dedicated.DedicatedServer;

public class MinecraftExecutorServiceImpl implements MinecraftExecutorService {
   private final DedicatedServer server;

   public MinecraftExecutorServiceImpl(DedicatedServer p_422428_) {
      this.server = p_422428_;
   }

   @Override
   public <V> CompletableFuture<V> submit(Supplier<V> p_425802_) {
      return this.server.submit(p_425802_);
   }

   @Override
   public CompletableFuture<Void> submit(Runnable p_429561_) {
      return this.server.submit(p_429561_);
   }
}
