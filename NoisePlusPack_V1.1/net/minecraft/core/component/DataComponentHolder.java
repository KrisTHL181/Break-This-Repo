package net.minecraft.core.component;

import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public interface DataComponentHolder extends DataComponentGetter {
   DataComponentMap getComponents();

   @Override
   default <T> @Nullable T get(DataComponentType<? extends T> p_331483_) {
      return this.getComponents().get(p_331483_);
   }

   default <T> Stream<T> getAllOfType(Class<? extends T> p_361858_) {
      return this.getComponents()
         .stream()
         .map(TypedDataComponent::value)
         .filter(p_365228_ -> p_361858_.isAssignableFrom(p_365228_.getClass()))
         .map(p_365353_ -> (T)p_365353_);
   }

   @Override
   default <T> T getOrDefault(DataComponentType<? extends T> p_328483_, T p_333219_) {
      return this.getComponents().getOrDefault(p_328483_, p_333219_);
   }

   default boolean has(DataComponentType<?> p_333597_) {
      return this.getComponents().has(p_333597_);
   }
}
