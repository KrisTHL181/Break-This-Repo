package net.minecraft.world.level.storage.loot.entries;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.world.level.storage.loot.LootContext;

@FunctionalInterface
public interface ComposableEntryContainer {
   ComposableEntryContainer ALWAYS_FALSE = (context, output) -> false;
   ComposableEntryContainer ALWAYS_TRUE = (context, output) -> true;

   boolean expand(final LootContext context, final Consumer<LootPoolEntry> output);

   default ComposableEntryContainer and(final ComposableEntryContainer other) {
      Objects.requireNonNull(other);
      return (context, output) -> this.expand(context, output) && other.expand(context, output);
   }

   default ComposableEntryContainer or(final ComposableEntryContainer other) {
      Objects.requireNonNull(other);
      return (context, output) -> this.expand(context, output) || other.expand(context, output);
   }
}
