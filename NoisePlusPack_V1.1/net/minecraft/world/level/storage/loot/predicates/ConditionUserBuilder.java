package net.minecraft.world.level.storage.loot.predicates;

import java.util.function.Function;

public interface ConditionUserBuilder<T extends ConditionUserBuilder<T>> {
   T when(LootItemCondition.Builder var1);

   default <E> T when(Iterable<E> p_231041_, Function<E, LootItemCondition.Builder> p_231042_) {
      T t = this.unwrap();

      for (E e : p_231041_) {
         t = t.when(p_231042_.apply(e));
      }

      return t;
   }

   T unwrap();
}
