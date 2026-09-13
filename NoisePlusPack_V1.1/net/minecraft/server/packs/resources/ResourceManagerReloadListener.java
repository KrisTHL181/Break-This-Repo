package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public interface ResourceManagerReloadListener extends PreparableReloadListener {
   @Override
   default CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_422642_, Executor p_10756_, PreparableReloadListener.PreparationBarrier p_10752_, Executor p_10757_
   ) {
      ResourceManager resourcemanager = p_422642_.resourceManager();
      return p_10752_.wait(Unit.INSTANCE).thenRunAsync(() -> {
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push("listener");
         this.onResourceManagerReload(resourcemanager);
         profilerfiller.pop();
      }, p_10757_);
   }

   void onResourceManagerReload(ResourceManager var1);
}
