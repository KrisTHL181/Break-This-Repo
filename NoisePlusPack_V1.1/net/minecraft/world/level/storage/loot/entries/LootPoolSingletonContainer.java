package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public abstract class LootPoolSingletonContainer extends LootPoolEntryContainer {
   public static final int DEFAULT_WEIGHT = 1;
   public static final int DEFAULT_QUALITY = 0;
   protected final int weight;
   protected final int quality;
   protected final List<LootItemFunction> functions;
   final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
   private final LootPoolEntry entry = new LootPoolSingletonContainer.EntryBase() {
      @Override
      public void createItemStack(Consumer<ItemStack> p_79700_, LootContext p_79701_) {
         LootPoolSingletonContainer.this.createItemStack(
            LootItemFunction.decorate(LootPoolSingletonContainer.this.compositeFunction, p_79700_, p_79701_), p_79701_
         );
      }
   };

   protected LootPoolSingletonContainer(int p_79681_, int p_79682_, List<LootItemCondition> p_297957_, List<LootItemFunction> p_300940_) {
      super(p_297957_);
      this.weight = p_79681_;
      this.quality = p_79682_;
      this.functions = p_300940_;
      this.compositeFunction = LootItemFunctions.compose(p_300940_);
   }

   protected static <T extends LootPoolSingletonContainer> P4<Mu<T>, Integer, Integer, List<LootItemCondition>, List<LootItemFunction>> singletonFields(
      Instance<T> p_299133_
   ) {
      return p_299133_.group(
            Codec.INT.optionalFieldOf("weight", 1).forGetter(p_300354_ -> p_300354_.weight),
            Codec.INT.optionalFieldOf("quality", 0).forGetter(p_297680_ -> p_297680_.quality)
         )
         .and(commonFields(p_299133_).t1())
         .and(LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(p_301387_ -> p_301387_.functions));
   }

   @Override
   public void validate(ValidationContext p_79686_) {
      super.validate(p_79686_);

      for (int i = 0; i < this.functions.size(); i++) {
         this.functions.get(i).validate(p_79686_.forChild(new ProblemReporter.IndexedFieldPathElement("functions", i)));
      }
   }

   protected abstract void createItemStack(Consumer<ItemStack> var1, LootContext var2);

   @Override
   public boolean expand(LootContext p_79694_, Consumer<LootPoolEntry> p_79695_) {
      if (this.canRun(p_79694_)) {
         p_79695_.accept(this.entry);
         return true;
      } else {
         return false;
      }
   }

   public static LootPoolSingletonContainer.Builder<?> simpleBuilder(LootPoolSingletonContainer.EntryConstructor p_79688_) {
      return new LootPoolSingletonContainer.DummyBuilder(p_79688_);
   }

   public abstract static class Builder<T extends LootPoolSingletonContainer.Builder<T>>
      extends LootPoolEntryContainer.Builder<T>
      implements FunctionUserBuilder<T> {
      protected int weight = 1;
      protected int quality = 0;
      private final com.google.common.collect.ImmutableList.Builder<LootItemFunction> functions = ImmutableList.builder();

      public T apply(LootItemFunction.Builder p_79710_) {
         this.functions.add(p_79710_.build());
         return this.getThis();
      }

      protected List<LootItemFunction> getFunctions() {
         return this.functions.build();
      }

      public T setWeight(int p_79708_) {
         this.weight = p_79708_;
         return this.getThis();
      }

      public T setQuality(int p_79712_) {
         this.quality = p_79712_;
         return this.getThis();
      }
   }

   static class DummyBuilder extends LootPoolSingletonContainer.Builder<LootPoolSingletonContainer.DummyBuilder> {
      private final LootPoolSingletonContainer.EntryConstructor constructor;

      public DummyBuilder(LootPoolSingletonContainer.EntryConstructor p_79717_) {
         this.constructor = p_79717_;
      }

      protected LootPoolSingletonContainer.DummyBuilder getThis() {
         return this;
      }

      @Override
      public LootPoolEntryContainer build() {
         return this.constructor.build(this.weight, this.quality, this.getConditions(), this.getFunctions());
      }
   }

   protected abstract class EntryBase implements LootPoolEntry {
      @Override
      public int getWeight(float p_79725_) {
         return Math.max(Mth.floor(LootPoolSingletonContainer.this.weight + LootPoolSingletonContainer.this.quality * p_79725_), 0);
      }
   }

   @FunctionalInterface
   protected interface EntryConstructor {
      LootPoolSingletonContainer build(int var1, int var2, List<LootItemCondition> var3, List<LootItemFunction> var4);
   }
}
