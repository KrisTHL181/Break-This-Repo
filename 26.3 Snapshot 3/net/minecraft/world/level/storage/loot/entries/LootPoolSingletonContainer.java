package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.Products.P4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
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
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;
   private final LootPoolEntry entry = new LootPoolSingletonContainer.EntryBase() {
      @Override
      public void createItemStack(final Consumer<ItemStack> output, final LootContext context) {
         LootPoolSingletonContainer.this.createItemStack(LootItemFunction.decorate(LootPoolSingletonContainer.this.compositeFunction, output, context), context);
      }
   };

   protected LootPoolSingletonContainer(final int weight, final int quality, final List<LootItemCondition> conditions, final List<LootItemFunction> functions) {
      super(conditions);
      this.weight = weight;
      this.quality = quality;
      this.functions = functions;
      this.compositeFunction = LootItemFunctions.compose(functions);
   }

   @Override
   public abstract MapCodec<? extends LootPoolSingletonContainer> codec();

   protected static <T extends LootPoolSingletonContainer> P4<Mu<T>, Integer, Integer, List<LootItemCondition>, List<LootItemFunction>> singletonFields(
      final Instance<T> i
   ) {
      return i.group(Codec.INT.optionalFieldOf("weight", 1).forGetter(e -> e.weight), Codec.INT.optionalFieldOf("quality", 0).forGetter(e -> e.quality))
         .and(commonFields(i).t1())
         .and(LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("functions", List.of()).forGetter(e -> e.functions));
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      Validatable.validate(context, "functions", this.functions);
   }

   protected abstract void createItemStack(Consumer<ItemStack> output, LootContext context);

   @Override
   public boolean expand(final LootContext context, final Consumer<LootPoolEntry> output) {
      if (this.canRun(context)) {
         output.accept(this.entry);
         return true;
      } else {
         return false;
      }
   }

   public static LootPoolSingletonContainer.Builder<?> simpleBuilder(final LootPoolSingletonContainer.EntryConstructor constructor) {
      return new LootPoolSingletonContainer.DummyBuilder(constructor);
   }

   public abstract static class Builder<T extends LootPoolSingletonContainer.Builder<T>>
      extends LootPoolEntryContainer.Builder<T>
      implements FunctionUserBuilder<T> {
      protected int weight = 1;
      protected int quality = 0;
      private final com.google.common.collect.ImmutableList.Builder<LootItemFunction> functions = ImmutableList.builder();

      public T apply(final LootItemFunction.Builder function) {
         this.functions.add(function.build());
         return this.getThis();
      }

      protected List<LootItemFunction> getFunctions() {
         return this.functions.build();
      }

      public T setWeight(final int weight) {
         this.weight = weight;
         return this.getThis();
      }

      public T setQuality(final int quality) {
         this.quality = quality;
         return this.getThis();
      }
   }

   private static class DummyBuilder extends LootPoolSingletonContainer.Builder<LootPoolSingletonContainer.DummyBuilder> {
      private final LootPoolSingletonContainer.EntryConstructor constructor;

      public DummyBuilder(final LootPoolSingletonContainer.EntryConstructor constructor) {
         this.constructor = constructor;
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
      public int getWeight(final float luck) {
         return Math.max(Mth.floor(LootPoolSingletonContainer.this.weight + LootPoolSingletonContainer.this.quality * luck), 0);
      }
   }

   @FunctionalInterface
   protected interface EntryConstructor {
      LootPoolSingletonContainer build(int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions);
   }
}
