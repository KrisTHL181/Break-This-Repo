package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeLootItemCondition implements LootItemCondition {
   protected final List<LootItemCondition> terms;
   private final Predicate<LootContext> composedPredicate;

   protected CompositeLootItemCondition(final List<LootItemCondition> terms, final Predicate<LootContext> composedPredicate) {
      this.terms = terms;
      this.composedPredicate = composedPredicate;
   }

   @Override
   public abstract MapCodec<? extends CompositeLootItemCondition> codec();

   protected static <T extends CompositeLootItemCondition> MapCodec<T> createCodec(final Function<List<LootItemCondition>, T> factory) {
      return RecordCodecBuilder.mapCodec(
         i -> i.group(LootItemCondition.DIRECT_CODEC.listOf().fieldOf("terms").forGetter(condition -> condition.terms)).apply(i, factory)
      );
   }

   protected static <T extends CompositeLootItemCondition> Codec<T> createInlineCodec(final Function<List<LootItemCondition>, T> factory) {
      return LootItemCondition.DIRECT_CODEC.listOf().xmap(factory, condition -> condition.terms);
   }

   public final boolean test(final LootContext context) {
      return this.composedPredicate.test(context);
   }

   @Override
   public void validate(final ValidationContext output) {
      LootItemCondition.super.validate(output);
      Validatable.validate(output, "terms", this.terms);
   }

   public abstract static class Builder implements LootItemCondition.Builder {
      private final com.google.common.collect.ImmutableList.Builder<LootItemCondition> terms = ImmutableList.builder();

      protected Builder(final LootItemCondition.Builder... terms) {
         for (LootItemCondition.Builder term : terms) {
            this.terms.add(term.build());
         }
      }

      public void addTerm(final LootItemCondition.Builder term) {
         this.terms.add(term.build());
      }

      @Override
      public LootItemCondition build() {
         return this.create(this.terms.build());
      }

      protected abstract LootItemCondition create(List<LootItemCondition> terms);
   }
}
