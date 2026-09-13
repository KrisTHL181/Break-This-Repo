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
      ResourceManager p_395154_, List<PreparableReloadListener> p_396622_, Executor p_396395_, Executor p_391662_, CompletableFuture<Unit> p_394230_
   ) {
      ProfiledReloadInstance profiledreloadinstance = new ProfiledReloadInstance(p_396622_);
      profiledreloadinstance.startTasks(
         p_396395_,
         p_391662_,
         p_395154_,
         p_396622_,
         (p_421516_, p_421517_, p_421518_, p_421519_, p_421520_) -> {
            AtomicLong atomiclong = new AtomicLong();
            AtomicLong atomiclong1 = new AtomicLong();
            AtomicLong atomiclong2 = new AtomicLong();
            AtomicLong atomiclong3 = new AtomicLong();
            CompletableFuture<Void> completablefuture = p_421518_.reload(
               p_421516_,
               profiledExecutor(p_421519_, atomiclong, atomiclong1, p_421518_.getName()),
               p_421517_,
               profiledExecutor(p_421520_, atomiclong2, atomiclong3, p_421518_.getName())
            );
            return completablefuture.thenApplyAsync(p_390170_ -> {
               LOGGER.debug("Finished reloading {}", p_421518_.getName());
               return new ProfiledReloadInstance.State(p_421518_.getName(), atomiclong, atomiclong1, atomiclong2, atomiclong3);
            }, p_391662_);
         },
         p_394230_
      );
      return profiledreloadinstance;
   }

   private ProfiledReloadInstance(List<PreparableReloadListener> p_10650_) {
      super(p_10650_);
      this.total.start();
   }

   @Override
   protected CompletableFuture<List<ProfiledReloadInstance.State>> prepareTasks(
      Executor p_396171_,
      Executor p_394309_,
      ResourceManager p_397859_,
      List<PreparableReloadListener> p_394859_,
      SimpleReloadInstance.StateFactory<ProfiledReloadInstance.State> p_391634_,
      CompletableFuture<?> p_397690_
   ) {
      return super.prepareTasks(p_396171_, p_394309_, p_397859_, p_394859_, p_391634_, p_397690_).thenApplyAsync(this::finish, p_394309_);
   }

   private static Executor profiledExecutor(Executor p_364914_, AtomicLong p_362781_, AtomicLong p_397335_, String p_364822_) {
      return p_390164_ -> p_364914_.execute(() -> {
         ProfilerFiller profilerfiller = Profiler.get();
         profilerfiller.push(p_364822_);
         long i = Util.getNanos();
         p_390164_.run();
         p_362781_.addAndGet(Util.getNanos() - i);
         p_397335_.incrementAndGet();
         profilerfiller.pop();
      });
   }

   private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> p_215484_) {
      this.total.stop();
      long i = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

      for (ProfiledReloadInstance.State profiledreloadinstance$state : p_215484_) {
         long j = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.preparationNanos.get());
         long k = profiledreloadinstance$state.preparationCount.get();
         long l = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.reloadNanos.get());
         long i1 = profiledreloadinstance$state.reloadCount.get();
         long j1 = j + l;
         long k1 = k + i1;
         String s = profiledreloadinstance$state.name;
         LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", new Object[]{s, k1, j1, k, j, i1, l});
         i += l;
      }

      LOGGER.info("Total blocking time: {} ms", i);
      return p_215484_;
   }

   public record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
   }
}
