package net.minecraft.server.packs.resources;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface PreparableReloadListener {
   CompletableFuture<Void> reload(PreparableReloadListener.SharedState var1, Executor var2, PreparableReloadListener.PreparationBarrier var3, Executor var4);

   default void prepareSharedState(PreparableReloadListener.SharedState p_429550_) {
   }

   default String getName() {
      return this.getClass().getSimpleName();
   }

   @FunctionalInterface
   interface PreparationBarrier {
      <T> CompletableFuture<T> wait(T var1);
   }

   final class SharedState {
      private final ResourceManager manager;
      private final Map<PreparableReloadListener.StateKey<?>, Object> state = new IdentityHashMap<>();

      public SharedState(ResourceManager p_427415_) {
         this.manager = p_427415_;
      }

      public ResourceManager resourceManager() {
         return this.manager;
      }

      public <T> void set(PreparableReloadListener.StateKey<T> p_423258_, T p_426673_) {
         this.state.put(p_423258_, p_426673_);
      }

      public <T> T get(PreparableReloadListener.StateKey<T> p_422546_) {
         return Objects.requireNonNull((T)this.state.get(p_422546_));
      }
   }

   final class StateKey<T> {
   }
}
