package net.minecraft.world.level.storage.loot;

import java.util.Set;
import net.minecraft.util.context.ContextKey;

public interface LootContextUser extends Validatable {
   default Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of();
   }

   @Override
   default void validate(final ValidationContext context) {
      context.validateContextUsage(this);
   }
}
