package net.minecraft.core.component.predicates;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;

public record AnyValue(DataComponentType<?> type) implements DataComponentPredicate {
   @Override
   public boolean matches(DataComponentGetter p_453685_) {
      return p_453685_.get(this.type) != null;
   }
}
