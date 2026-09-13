package net.minecraft.core.component;

import org.jspecify.annotations.Nullable;

public interface DataComponentGetter {
   <T> @Nullable T get(DataComponentType<? extends T> type);

   default <T> T getOrDefault(final DataComponentType<? extends T> type, final T defaultValue) {
      T value = this.get(type);
      return value != null ? value : defaultValue;
   }

   default <T> @Nullable TypedDataComponent<T> getTyped(final DataComponentType<T> type) {
      T value = this.get(type);
      return value != null ? new TypedDataComponent<>(type, value) : null;
   }
}
