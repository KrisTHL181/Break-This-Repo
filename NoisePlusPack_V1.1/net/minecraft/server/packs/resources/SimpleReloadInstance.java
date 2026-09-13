package net.minecraft.server.packs.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class SimpleReloadInstance<S> implements ReloadInstance {
   private static final int PREPARATION_PROGRESS_WEIGHT = 2;
   private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
   private static final int LISTENER_PROGRESS_WEIGHT = 1;
   final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
   private @Nullable CompletableFuture<List<S>> allDone;
   final Set<PreparableReloadListener> preparingListeners;
   private final int listenerCount;
   private final AtomicInteger startedTasks = new AtomicInteger();
   private final AtomicInteger finishedTasks = new AtomicInteger();
   private final AtomicInteger startedReloads = new AtomicInteger();
   private final AtomicInteger finishedReloads = new AtomicInteger();

   public static ReloadInstance of(
      ResourceManager p_10816_, List<PreparableReloadListener> p_10817_, Executor p_10818_, Executor p_10819_, CompletableFuture<Unit> p_10820_
   ) {
      SimpleReloadInstance<Void> simplereloadinstance = new SimpleReloadInstance<>(p_10817_);
      simplereloadinstance.startTasks(p_10818_, p_10819_, p_10816_, p_10817_, SimpleReloadInstance.StateFactory.SIMPLE, p_10820_);
      return simplereloadinstance;
   }

   protected SimpleReloadInstance(List<PreparableReloadListener> p_10811_) {
      this.listenerCount = p_10811_.size();
      this.preparingListeners = new HashSet<>(p_10811_);
   }

   protected void startTasks(
      Executor p_396228_,
      Executor p_395769_,
      ResourceManager p_391611_,
      List<PreparableReloadListener> p_395372_,
      SimpleReloadInstance.StateFactory<S> p_391348_,
      CompletableFuture<?> p_394852_
   ) {
      this.allDone = this.prepareTasks(p_396228_, p_395769_, p_391611_, p_395372_, p_391348_, p_394852_);
   }

   protected CompletableFuture<List<S>> prepareTasks(
      Executor p_393184_,
      Executor p_395239_,
      ResourceManager p_394670_,
      List<PreparableReloadListener> p_397834_,
      SimpleReloadInstance.StateFactory<S> p_391359_,
      CompletableFuture<?> p_393207_
   ) {
      Executor executor = p_390185_ -> {
         this.startedTasks.incrementAndGet();
         p_393184_.execute(() -> {
            p_390185_.run();
            this.finishedTasks.incrementAndGet();
         });
      };
      Executor executor1 = p_390183_ -> {
         this.startedReloads.incrementAndGet();
         p_395239_.execute(() -> {
            p_390183_.run();
            this.finishedReloads.incrementAndGet();
         });
      };
      this.startedTasks.incrementAndGet();
      p_393207_.thenRun(this.finishedTasks::incrementAndGet);
      PreparableReloadListener.SharedState preparablereloadlistener$sharedstate = new PreparableReloadListener.SharedState(p_394670_);
      p_397834_.forEach(p_421522_ -> p_421522_.prepareSharedState(preparablereloadlistener$sharedstate));
      CompletableFuture<?> completablefuture = p_393207_;
      List<CompletableFuture<S>> list = new ArrayList<>();

      for (PreparableReloadListener preparablereloadlistener : p_397834_) {
         PreparableReloadListener.PreparationBarrier preparablereloadlistener$preparationbarrier = this.createBarrierForListener(
            preparablereloadlistener, completablefuture, p_395239_
         );
         CompletableFuture<S> completablefuture1 = p_391359_.create(
            preparablereloadlistener$sharedstate, preparablereloadlistener$preparationbarrier, preparablereloadlistener, executor, executor1
         );
         list.add(completablefuture1);
         completablefuture = completablefuture1;
      }

      return Util.sequenceFailFast(list);
   }

   private PreparableReloadListener.PreparationBarrier createBarrierForListener(
      final PreparableReloadListener p_396536_, final CompletableFuture<?> p_394372_, final Executor p_391253_
   ) {
      return new PreparableReloadListener.PreparationBarrier() {
         @Override
         public <T> CompletableFuture<T> wait(T p_10858_) {
            p_391253_.execute(() -> {
               SimpleReloadInstance.this.preparingListeners.remove(p_396536_);
               if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                  SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
               }
            });
            return SimpleReloadInstance.this.allPreparations.thenCombine((CompletionStage<? extends T>)p_394372_, (p_10861_, p_10862_) -> p_10858_);
         }
      };
   }

   @Override
   public CompletableFuture<?> done() {
      return Objects.requireNonNull(this.allDone, "not started");
   }

   @Override
   public float getActualProgress() {
      int i = this.listenerCount - this.preparingListeners.size();
      float f = weightProgress(this.finishedTasks.get(), this.finishedReloads.get(), i);
      float f1 = weightProgress(this.startedTasks.get(), this.startedReloads.get(), this.listenerCount);
      return f / f1;
   }

   private static int weightProgress(int p_396182_, int p_395893_, int p_394193_) {
      return p_396182_ * 2 + p_395893_ * 2 + p_394193_ * 1;
   }

   public static ReloadInstance create(
      ResourceManager p_203835_,
      List<PreparableReloadListener> p_203836_,
      Executor p_203837_,
      Executor p_203838_,
      CompletableFuture<Unit> p_203839_,
      boolean p_203840_
   ) {
      return p_203840_
         ? ProfiledReloadInstance.of(p_203835_, p_203836_, p_203837_, p_203838_, p_203839_)
         : of(p_203835_, p_203836_, p_203837_, p_203838_, p_203839_);
   }

   @FunctionalInterface
   protected interface StateFactory<S> {
      SimpleReloadInstance.StateFactory<Void> SIMPLE = (p_421523_, p_421524_, p_421525_, p_421526_, p_421527_) -> p_421525_.reload(
         p_421523_, p_421526_, p_421524_, p_421527_
      );

      CompletableFuture<S> create(
         PreparableReloadListener.SharedState var1,
         PreparableReloadListener.PreparationBarrier var2,
         PreparableReloadListener var3,
         Executor var4,
         Executor var5
      );
   }
}
