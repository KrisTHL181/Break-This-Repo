package net.minecraft.server.packs.resources;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;

public abstract class SimplePreparableReloadListener<T> implements PreparableReloadListener {
   @Override
   public final CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState p_429663_, Executor p_10784_, PreparableReloadListener.PreparationBarrier p_10780_, Executor p_10785_
   ) {
      ResourceManager resourcemanager = p_429663_.resourceManager();
      return CompletableFuture.<T>supplyAsync(() -> this.prepare(resourcemanager, Profiler.get()), p_10784_)
         .thenCompose(p_10780_::wait)
         .thenAcceptAsync(p_358748_ -> this.apply((T)p_358748_, resourcemanager, Profiler.get()), p_10785_);
   }

   protected abstract T prepare(ResourceManager var1, ProfilerFiller var2);

   protected abstract void apply(T var1, ResourceManager var2, ProfilerFiller var3);
}
