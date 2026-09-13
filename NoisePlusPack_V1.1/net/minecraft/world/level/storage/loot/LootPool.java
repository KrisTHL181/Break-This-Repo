package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import org.apache.commons.lang3.mutable.MutableInt;

public class LootPool {
   public static final Codec<LootPool> CODEC = RecordCodecBuilder.create(
      p_341975_ -> p_341975_.group(
            LootPoolEntries.CODEC.listOf().fieldOf("entries").forGetter(p_297007_ -> p_297007_.entries),
            LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("conditions", List.of()).forGetter(p_297008_ -> p_297008_.conditions),
            LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(p_297009_ -> p_297009_.functions),
            NumberProviders.CODEC.fieldOf("rolls").forGetter(p_297004_ -> p_297004_.rolls),
            NumberProviders.CODEC.fieldOf("bonus_rolls").orElse(ConstantValue.exactly(0.0F)).forGetter(p_297006_ -> p_297006_.bonusRolls)
         )
         .apply(p_341975_, LootPool::new)
   );
   private final List<LootPoolEntryContainer> entries;
   private final List<LootItemCondition> conditions;
   private final Predicate<LootContext> compositeCondition;
   private final List<LootItemFunction> functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
   private final NumberProvider rolls;
   private final NumberProvider bonusRolls;

   LootPool(
      List<LootPoolEntryContainer> p_298341_,
      List<LootItemCondition> p_297697_,
      List<LootItemFunction> p_299722_,
      NumberProvider p_165131_,
      NumberProvider p_165132_
   ) {
      this.entries = p_298341_;
      this.conditions = p_297697_;
      this.compositeCondition = Util.allOf(p_297697_);
      this.functions = p_299722_;
      this.compositeFunction = LootItemFunctions.compose(p_299722_);
      this.rolls = p_165131_;
      this.bonusRolls = p_165132_;
   }

   private void addRandomItem(Consumer<ItemStack> p_79059_, LootContext p_79060_) {
      RandomSource randomsource = p_79060_.getRandom();
      List<LootPoolEntry> list = Lists.newArrayList();
      MutableInt mutableint = new MutableInt();

      for (LootPoolEntryContainer lootpoolentrycontainer : this.entries) {
         lootpoolentrycontainer.expand(p_79060_, p_79048_ -> {
            int k = p_79048_.getWeight(p_79060_.getLuck());
            if (k > 0) {
               list.add(p_79048_);
               mutableint.add(k);
            }
         });
      }

      int i = list.size();
      if (mutableint.intValue() != 0 && i != 0) {
         if (i == 1) {
            list.get(0).createItemStack(p_79059_, p_79060_);
         } else {
            int j = randomsource.nextInt(mutableint.intValue());

            for (LootPoolEntry lootpoolentry : list) {
               j -= lootpoolentry.getWeight(p_79060_.getLuck());
               if (j < 0) {
                  lootpoolentry.createItemStack(p_79059_, p_79060_);
                  return;
               }
            }
         }
      }
   }

   public void addRandomItems(Consumer<ItemStack> p_79054_, LootContext p_79055_) {
      if (this.compositeCondition.test(p_79055_)) {
         Consumer<ItemStack> consumer = LootItemFunction.decorate(this.compositeFunction, p_79054_, p_79055_);
         int i = this.rolls.getInt(p_79055_) + Mth.floor(this.bonusRolls.getFloat(p_79055_) * p_79055_.getLuck());

         for (int j = 0; j < i; j++) {
            this.addRandomItem(consumer, p_79055_);
         }
      }
   }

   public void validate(ValidationContext p_79052_) {
      for (int i = 0; i < this.conditions.size(); i++) {
         this.conditions.get(i).validate(p_79052_.forChild(new ProblemReporter.IndexedFieldPathElement("conditions", i)));
      }

      for (int j = 0; j < this.functions.size(); j++) {
         this.functions.get(j).validate(p_79052_.forChild(new ProblemReporter.IndexedFieldPathElement("functions", j)));
      }

      for (int k = 0; k < this.entries.size(); k++) {
         this.entries.get(k).validate(p_79052_.forChild(new ProblemReporter.IndexedFieldPathElement("entries", k)));
      }

      this.rolls.validate(p_79052_.forChild(new ProblemReporter.FieldPathElement("rolls")));
      this.bonusRolls.validate(p_79052_.forChild(new ProblemReporter.FieldPathElement("bonus_rolls")));
   }

   public static LootPool.Builder lootPool() {
      return new LootPool.Builder();
   }

   public static class Builder implements FunctionUserBuilder<LootPool.Builder>, ConditionUserBuilder<LootPool.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();
      private final com.google.common.collect.ImmutableList.Builder<LootItemCondition> conditions = ImmutableList.builder();
      private final com.google.common.collect.ImmutableList.Builder<LootItemFunction> functions = ImmutableList.builder();
      private NumberProvider rolls = ConstantValue.exactly(1.0F);
      private NumberProvider bonusRolls = ConstantValue.exactly(0.0F);

      public LootPool.Builder setRolls(NumberProvider p_165134_) {
         this.rolls = p_165134_;
         return this;
      }

      public LootPool.Builder unwrap() {
         return this;
      }

      public LootPool.Builder setBonusRolls(NumberProvider p_165136_) {
         this.bonusRolls = p_165136_;
         return this;
      }

      public LootPool.Builder add(LootPoolEntryContainer.Builder<?> p_79077_) {
         this.entries.add(p_79077_.build());
         return this;
      }

      public LootPool.Builder when(LootItemCondition.Builder p_79081_) {
         this.conditions.add(p_79081_.build());
         return this;
      }

      public LootPool.Builder apply(LootItemFunction.Builder p_79079_) {
         this.functions.add(p_79079_.build());
         return this;
      }

      public LootPool build() {
         return new LootPool(this.entries.build(), this.conditions.build(), this.functions.build(), this.rolls, this.bonusRolls);
      }
   }
}
