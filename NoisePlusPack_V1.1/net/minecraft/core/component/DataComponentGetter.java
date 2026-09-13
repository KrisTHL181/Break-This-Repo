package net.minecraft.core.component;

import org.jspecify.annotations.Nullable;

public interface DataComponentGetter {
   <T> @Nullable T get(DataComponentType<? extends T> var1);

   default <T> T getOrDefault(DataComponentType<? extends T> p_396161_, T p_396548_) {
      T t = this.get(p_396161_);
      return t != null ? t : p_396548_;
   }

   default <T> @Nullable TypedDataComponent<T> getTyped(DataComponentType<T> p_396283_) {
      T t = this.get(p_396283_);
      return t != null ? new TypedDataComponent<>(p_396283_, t) : null;
   }
}
