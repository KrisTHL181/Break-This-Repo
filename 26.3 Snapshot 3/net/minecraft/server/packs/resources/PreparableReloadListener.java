package net.minecraft.server.packs.resources;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@FunctionalInterface
public interface PreparableReloadListener {
   CompletableFuture<Void> reload(
      PreparableReloadListener.SharedState currentReload,
      Executor taskExecutor,
      PreparableReloadListener.PreparationBarrier preparationBarrier,
      Executor reloadExecutor
   );

   default void prepareSharedState(final PreparableReloadListener.SharedState currentReload) {
   }

   default String getName() {
      return this.getClass().getSimpleName();
   }

   @FunctionalInterface
   interface PreparationBarrier {
      <T> CompletableFuture<T> wait(T t);
   }

   final class SharedState {
      private final ResourceManager manager;
      private final Map<PreparableReloadListener.StateKey<?>, Object> state = new IdentityHashMap<>();

      public SharedState(final ResourceManager manager) {
         this.manager = manager;
      }

      public ResourceManager resourceManager() {
         return this.manager;
      }

      public <T> void set(final PreparableReloadListener.StateKey<T> key, final T value) {
         this.state.put(key, value);
      }

      public <T> T get(final PreparableReloadListener.StateKey<T> key) {
         return Objects.requireNonNull((T)this.state.get(key));
      }
   }

   final class StateKey<T> {
   }
}
