package net.minecraft.world.level.storage.loot.functions;

import java.util.Arrays;
import java.util.function.Function;

public interface FunctionUserBuilder<T extends FunctionUserBuilder<T>> {
   T apply(LootItemFunction.Builder builder);

   default <E> T apply(final Iterable<E> collection, final Function<E, LootItemFunction.Builder> functionProvider) {
      T result = this.unwrap();

      for (E value : collection) {
         result = result.apply(functionProvider.apply(value));
      }

      return result;
   }

   default <E> T apply(final E[] collection, final Function<E, LootItemFunction.Builder> functionProvider) {
      return this.apply(Arrays.asList(collection), functionProvider);
   }

   T unwrap();
}
