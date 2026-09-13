package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class EntryGroup extends CompositeEntryBase {
   public static final MapCodec<EntryGroup> MAP_CODEC = createCodec(EntryGroup::new);

   public EntryGroup(final List<LootPoolEntryContainer> children, final List<LootItemCondition> conditions) {
      super(children, conditions);
   }

   @Override
   public MapCodec<EntryGroup> codec() {
      return MAP_CODEC;
   }

   @Override
   protected ComposableEntryContainer compose(final List<? extends ComposableEntryContainer> entries) {
      return switch (entries.size()) {
         case 0 -> ALWAYS_TRUE;
         case 1 -> (ComposableEntryContainer)entries.get(0);
         case 2 -> {
            ComposableEntryContainer first = entries.get(0);
            ComposableEntryContainer second = entries.get(1);
            yield (context, output) -> {
               first.expand(context, output);
               second.expand(context, output);
               return true;
            };
         }
         default -> (context, output) -> {
            for (ComposableEntryContainer entry : entries) {
               entry.expand(context, output);
            }

            return true;
         };
      };
   }

   public static EntryGroup.Builder list(final LootPoolEntryContainer.Builder<?>... entries) {
      return new EntryGroup.Builder(entries);
   }

   public static class Builder extends LootPoolEntryContainer.Builder<EntryGroup.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

      public Builder(final LootPoolEntryContainer.Builder<?>... entries) {
         for (LootPoolEntryContainer.Builder<?> entry : entries) {
            this.entries.add(entry.build());
         }
      }

      protected EntryGroup.Builder getThis() {
         return this;
      }

      @Override
      public EntryGroup.Builder append(final LootPoolEntryContainer.Builder<?> other) {
         this.entries.add(other.build());
         return this;
      }

      @Override
      public LootPoolEntryContainer build() {
         return new EntryGroup(this.entries.build(), this.getConditions());
      }
   }
}
