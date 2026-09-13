package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SequentialEntry extends CompositeEntryBase {
   public static final MapCodec<SequentialEntry> MAP_CODEC = createCodec(SequentialEntry::new);

   public SequentialEntry(final List<LootPoolEntryContainer> children, final List<LootItemCondition> conditions) {
      super(children, conditions);
   }

   @Override
   public MapCodec<SequentialEntry> codec() {
      return MAP_CODEC;
   }

   @Override
   protected ComposableEntryContainer compose(final List<? extends ComposableEntryContainer> entries) {
      return switch (entries.size()) {
         case 0 -> ALWAYS_TRUE;
         case 1 -> (ComposableEntryContainer)entries.get(0);
         case 2 -> entries.get(0).and(entries.get(1));
         default -> (context, output) -> {
            for (ComposableEntryContainer entry : entries) {
               if (!entry.expand(context, output)) {
                  return false;
               }
            }

            return true;
         };
      };
   }

   public static SequentialEntry.Builder sequential(final LootPoolEntryContainer.Builder<?>... entries) {
      return new SequentialEntry.Builder(entries);
   }

   public static class Builder extends LootPoolEntryContainer.Builder<SequentialEntry.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

      public Builder(final LootPoolEntryContainer.Builder<?>... entries) {
         for (LootPoolEntryContainer.Builder<?> entry : entries) {
            this.entries.add(entry.build());
         }
      }

      protected SequentialEntry.Builder getThis() {
         return this;
      }

      @Override
      public SequentialEntry.Builder then(final LootPoolEntryContainer.Builder<?> other) {
         this.entries.add(other.build());
         return this;
      }

      @Override
      public LootPoolEntryContainer build() {
         return new SequentialEntry(this.entries.build(), this.getConditions());
      }
   }
}
