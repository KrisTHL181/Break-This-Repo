package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public interface ResourceManagerReloadListener extends PreparableReloadListener {
   @Override
   default CompletableFuture<Void> reload(
      final PreparableReloadListener.SharedState currentReload,
      final Executor taskExecutor,
      final PreparableReloadListener.PreparationBarrier preparationBarrier,
      final Executor reloadExecutor
   ) {
      ResourceManager manager = currentReload.resourceManager();
      return preparationBarrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
         ProfilerFiller reloadProfiler = Profiler.get();
         reloadProfiler.push("listener");
         this.onResourceManagerReload(manager);
         reloadProfiler.pop();
      }, reloadExecutor);
   }

   void onResourceManagerReload(ResourceManager resourceManager);
}
