package net.minecraft.server.jsonrpc.internalapi;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.notifications.NotificationManager;

public class MinecraftExecutorServiceImpl implements MinecraftExecutorService {
   private final NotificationManager notificationManager;

   public MinecraftExecutorServiceImpl(final NotificationManager notificationManager) {
      this.notificationManager = notificationManager;
   }

   private DedicatedServer server() {
      return Objects.requireNonNull(this.notificationManager.server());
   }

   @Override
   public <V> CompletableFuture<V> submit(final Supplier<V> supplier) {
      return this.server().submit(supplier);
   }

   @Override
   public CompletableFuture<Void> submit(final Runnable runnable) {
      return this.server().submit(runnable);
   }
}
