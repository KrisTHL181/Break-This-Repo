package net.minecraft.core.component;

import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface DataComponentHolder extends DataComponentGetter {
   DataComponentMap getComponents();

   @Override
   default <T> @Nullable T get(final DataComponentType<? extends T> type) {
      return this.getComponents().get(type);
   }

   default <T> Stream<T> getAllOfType(final Class<? extends T> valueClass) {
      return this.getComponents().stream().map(TypedDataComponent::value).filter(value -> valueClass.isAssignableFrom(value.getClass())).map(value -> (T)value);
   }

   @Override
   default <T> T getOrDefault(final DataComponentType<? extends T> type, final T defaultValue) {
      return this.getComponents().getOrDefault(type, defaultValue);
   }

   default boolean has(final DataComponentType<?> type) {
      return this.getComponents().has(type);
   }
}
