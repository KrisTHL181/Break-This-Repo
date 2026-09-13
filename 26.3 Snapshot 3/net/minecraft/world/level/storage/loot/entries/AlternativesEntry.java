package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class AlternativesEntry extends CompositeEntryBase {
   public static final MapCodec<AlternativesEntry> MAP_CODEC = createCodec(AlternativesEntry::new);
   public static final ProblemReporter.Problem UNREACHABLE_PROBLEM = new ProblemReporter.Problem() {
      @Override
      public String description() {
         return "Unreachable entry!";
      }
   };

   public AlternativesEntry(final List<LootPoolEntryContainer> children, final List<LootItemCondition> conditions) {
      super(children, conditions);
   }

   @Override
   public MapCodec<AlternativesEntry> codec() {
      return MAP_CODEC;
   }

   @Override
   protected ComposableEntryContainer compose(final List<? extends ComposableEntryContainer> entries) {
      return switch (entries.size()) {
         case 0 -> ALWAYS_FALSE;
         case 1 -> (ComposableEntryContainer)entries.get(0);
         case 2 -> entries.get(0).or(entries.get(1));
         default -> (context, output) -> {
            for (ComposableEntryContainer entry : entries) {
               if (entry.expand(context, output)) {
                  return true;
               }
            }

            return false;
         };
      };
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);

      for (int i = 0; i < this.children.size() - 1; i++) {
         if (this.children.get(i).conditions.isEmpty()) {
            context.reportProblem(UNREACHABLE_PROBLEM);
         }
      }
   }

   public static AlternativesEntry.Builder alternatives(final LootPoolEntryContainer.Builder<?>... entries) {
      return new AlternativesEntry.Builder(entries);
   }

   public static <E> AlternativesEntry.Builder alternatives(final Collection<E> items, final Function<E, LootPoolEntryContainer.Builder<?>> provider) {
      return new AlternativesEntry.Builder(items.stream().map(provider::apply).toArray(LootPoolEntryContainer.Builder[]::new));
   }

   public static class Builder extends LootPoolEntryContainer.Builder<AlternativesEntry.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

      public Builder(final LootPoolEntryContainer.Builder<?>... entries) {
         for (LootPoolEntryContainer.Builder<?> entry : entries) {
            this.entries.add(entry.build());
         }
      }

      protected AlternativesEntry.Builder getThis() {
         return this;
      }

      @Override
      public AlternativesEntry.Builder otherwise(final LootPoolEntryContainer.Builder<?> other) {
         this.entries.add(other.build());
         return this;
      }

      @Override
      public LootPoolEntryContainer build() {
         return new AlternativesEntry(this.entries.build(), this.getConditions());
      }
   }
}
