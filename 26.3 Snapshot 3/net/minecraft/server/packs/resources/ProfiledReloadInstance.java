package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Stopwatch total = Stopwatch.createUnstarted();

   public static ReloadInstance of(
      final ResourceManager resourceManager,
      final List<PreparableReloadListener> listeners,
      final Executor taskExecutor,
      final Executor mainThreadExecutor,
      final CompletableFuture<Unit> initialTask
   ) {
      ProfiledReloadInstance result = new ProfiledReloadInstance(listeners);
      result.startTasks(
         taskExecutor,
         mainThreadExecutor,
         resourceManager,
         listeners,
         (currentReload, previousStep, listener, parentTaskExecutor, parentReloadExecutor) -> {
            AtomicLong preparationNanos = new AtomicLong();
            AtomicLong preparationCount = new AtomicLong();
            AtomicLong reloadNanos = new AtomicLong();
            AtomicLong reloadCount = new AtomicLong();
            CompletableFuture<Void> reload = listener.reload(
               currentReload,
               profiledExecutor(parentTaskExecutor, preparationNanos, preparationCount, listener.getName()),
               previousStep,
               profiledExecutor(parentReloadExecutor, reloadNanos, reloadCount, listener.getName())
            );
            return reload.thenApplyAsync(v -> {
               LOGGER.debug("Finished reloading {}", listener.getName());
               return new ProfiledReloadInstance.State(listener.getName(), preparationNanos, preparationCount, reloadNanos, reloadCount);
            }, mainThreadExecutor);
         },
         initialTask
      );
      return result;
   }

   private ProfiledReloadInstance(final List<PreparableReloadListener> listeners) {
      super(listeners);
      this.total.start();
   }

   @Override
   protected CompletableFuture<List<ProfiledReloadInstance.State>> prepareTasks(
      final Executor taskExecutor,
      final Executor mainThreadExecutor,
      final ResourceManager resourceManager,
      final List<PreparableReloadListener> listeners,
      final SimpleReloadInstance.StateFactory<ProfiledReloadInstance.State> stateFactory,
      final CompletableFuture<?> initialTask
   ) {
      return super.prepareTasks(taskExecutor, mainThreadExecutor, resourceManager, listeners, stateFactory, initialTask)
         .thenApplyAsync(this::finish, mainThreadExecutor);
   }

   private static Executor profiledExecutor(final Executor executor, final AtomicLong accumulatedNanos, final AtomicLong taskCount, final String name) {
      return r -> executor.execute(() -> {
         ProfilerFiller profiler = Profiler.get();
         profiler.push(name);
         long nanos = Util.getNanos();
         r.run();
         accumulatedNanos.addAndGet(Util.getNanos() - nanos);
         taskCount.incrementAndGet();
         profiler.pop();
      });
   }

   private List<ProfiledReloadInstance.State> finish(final List<ProfiledReloadInstance.State> result) {
      this.total.stop();
      long blockingTime = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

      for (ProfiledReloadInstance.State state : result) {
         long prepTime = TimeUnit.NANOSECONDS.toMillis(state.preparationNanos.get());
         long prepCount = state.preparationCount.get();
         long reloadTime = TimeUnit.NANOSECONDS.toMillis(state.reloadNanos.get());
         long reloadCount = state.reloadCount.get();
         long totalTime = prepTime + reloadTime;
         long totalCount = prepCount + reloadCount;
         String name = state.name;
         LOGGER.info(
            "{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)",
            new Object[]{name, totalCount, totalTime, prepCount, prepTime, reloadCount, reloadTime}
         );
         blockingTime += reloadTime;
      }

      LOGGER.info("Total blocking time: {} ms", blockingTime);
      return result;
   }

   public record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
   }
}
