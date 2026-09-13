package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeLootItemCondition implements LootItemCondition {
   protected final List<LootItemCondition> terms;
   private final Predicate<LootContext> composedPredicate;

   protected CompositeLootItemCondition(List<LootItemCondition> p_299430_, Predicate<LootContext> p_286771_) {
      this.terms = p_299430_;
      this.composedPredicate = p_286771_;
   }

   protected static <T extends CompositeLootItemCondition> MapCodec<T> createCodec(Function<List<LootItemCondition>, T> p_297590_) {
      return RecordCodecBuilder.mapCodec(
         p_342025_ -> p_342025_.group(LootItemCondition.DIRECT_CODEC.listOf().fieldOf("terms").forGetter(p_297812_ -> p_297812_.terms))
            .apply(p_342025_, p_297590_)
      );
   }

   protected static <T extends CompositeLootItemCondition> Codec<T> createInlineCodec(Function<List<LootItemCondition>, T> p_298800_) {
      return LootItemCondition.DIRECT_CODEC.listOf().xmap(p_298800_, p_300100_ -> p_300100_.terms);
   }

   public final boolean test(LootContext p_286298_) {
      return this.composedPredicate.test(p_286298_);
   }

   @Override
   public void validate(ValidationContext p_286819_) {
      LootItemCondition.super.validate(p_286819_);

      for (int i = 0; i < this.terms.size(); i++) {
         this.terms.get(i).validate(p_286819_.forChild(new ProblemReporter.IndexedFieldPathElement("terms", i)));
      }
   }

   public abstract static class Builder implements LootItemCondition.Builder {
      private final com.google.common.collect.ImmutableList.Builder<LootItemCondition> terms = ImmutableList.builder();

      protected Builder(LootItemCondition.Builder... p_286619_) {
         for (LootItemCondition.Builder lootitemcondition$builder : p_286619_) {
            this.terms.add(lootitemcondition$builder.build());
         }
      }

      public void addTerm(LootItemCondition.Builder p_286677_) {
         this.terms.add(p_286677_.build());
      }

      @Override
      public LootItemCondition build() {
         return this.create(this.terms.build());
      }

      protected abstract LootItemCondition create(List<LootItemCondition> var1);
   }
}
