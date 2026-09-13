package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Function;

public interface ConditionUserBuilder<T extends ConditionUserBuilder<T>> {
   T when(final LootItemCondition.Builder builder);

   default <E> T when(final Iterable<E> collection, final Function<E, LootItemCondition.Builder> conditionProvider) {
      T result = this.unwrap();

      for (E value : collection) {
         result = result.when(conditionProvider.apply(value));
      }

      return result;
   }

   T unwrap();
}
