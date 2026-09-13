package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class LootItemConditionalFunction implements LootItemFunction {
   protected final List<LootItemCondition> predicates;
   private final Predicate<LootContext> compositePredicates;

   protected LootItemConditionalFunction(final List<LootItemCondition> predicates) {
      this.predicates = predicates;
      this.compositePredicates = Util.allOf(predicates);
   }

   @Override
   public abstract MapCodec<? extends LootItemConditionalFunction> codec();

   protected static <T extends LootItemConditionalFunction> P1<Mu<T>, List<LootItemCondition>> commonFields(final Instance<T> i) {
      return i.group(LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(f -> f.predicates));
   }

   public final ItemStack apply(final ItemStack itemStack, final LootContext context) {
      return this.compositePredicates.test(context) ? this.run(itemStack, context) : itemStack;
   }

   protected abstract ItemStack run(final ItemStack itemStack, final LootContext context);

   @Override
   public void validate(final ValidationContext context) {
      LootItemFunction.super.validate(context);
      Validatable.validate(context, "conditions", this.predicates);
   }

   protected static LootItemConditionalFunction.Builder<?> simpleBuilder(final Function<List<LootItemCondition>, LootItemFunction> constructor) {
      return new LootItemConditionalFunction.DummyBuilder(constructor);
   }

   public abstract static class Builder<T extends LootItemConditionalFunction.Builder<T>> implements LootItemFunction.Builder, ConditionUserBuilder<T> {
      private final com.google.common.collect.ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();

      public T when(final LootItemCondition.Builder condition) {
         this.conditions.add(condition.build());
         return this.getThis();
      }

      public final T unwrap() {
         return this.getThis();
      }

      protected abstract T getThis();

      protected List<LootItemCondition> getConditions() {
         return this.conditions.build();
      }
   }

   private static final class DummyBuilder extends LootItemConditionalFunction.Builder<LootItemConditionalFunction.DummyBuilder> {
      private final Function<List<LootItemCondition>, LootItemFunction> constructor;

      public DummyBuilder(final Function<List<LootItemCondition>, LootItemFunction> constructor) {
         this.constructor = constructor;
      }

      protected LootItemConditionalFunction.DummyBuilder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return this.constructor.apply(this.getConditions());
      }
   }
}
