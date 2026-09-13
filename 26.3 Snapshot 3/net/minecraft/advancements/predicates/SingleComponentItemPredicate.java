package net.minecraft.advancements.predicates;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.predicates.DataComponentPredicate;

public interface SingleComponentItemPredicate<T> extends DataComponentPredicate {
   @Override
   default boolean matches(final DataComponentGetter components) {
      T value = components.get(this.componentType());
      return value != null && this.matches(value);
   }

   DataComponentType<T> componentType();

   boolean matches(T value);
}
