package net.minecraft.world.level.storage.loot.entries;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class NestedLootTable extends LootPoolSingletonContainer {
   public static final MapCodec<NestedLootTable> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.either(LootTable.KEY_CODEC, LootTable.DIRECT_CODEC).fieldOf("value").forGetter(e -> e.contents))
         .and(singletonFields(i))
         .apply(i, NestedLootTable::new)
   );
   public static final ProblemReporter.PathElement INLINE_LOOT_TABLE_PATH_ELEMENT = new ProblemReporter.PathElement() {
      @Override
      public String get() {
         return "->{inline}";
      }
   };
   private final Either<ResourceKey<LootTable>, LootTable> contents;

   private NestedLootTable(
      final Either<ResourceKey<LootTable>, LootTable> contents,
      final int weight,
      final int quality,
      final List<LootItemCondition> conditions,
      final List<LootItemFunction> functions
   ) {
      super(weight, quality, conditions, functions);
      this.contents = contents;
   }

   @Override
   public MapCodec<NestedLootTable> codec() {
      return MAP_CODEC;
   }

   @Override
   public void createItemStack(final Consumer<ItemStack> output, final LootContext context) {
      ((LootTable)this.contents.map(name -> context.getResolver().get(name).map(Holder::value).orElse(LootTable.EMPTY), table -> table))
         .getRandomItemsRaw(context, output);
   }

   @Override
   public void validate(final ValidationContext context) {
      super.validate(context);
      this.contents
         .ifLeft(id -> Validatable.validateReference(context, id))
         .ifRight(lootTable -> lootTable.validate(context.forChild(INLINE_LOOT_TABLE_PATH_ELEMENT)));
   }

   public static LootPoolSingletonContainer.Builder<?> lootTableReference(final ResourceKey<LootTable> name) {
      return simpleBuilder((weight, quality, conditions, functions) -> new NestedLootTable(Either.left(name), weight, quality, conditions, functions));
   }

   public static LootPoolSingletonContainer.Builder<?> inlineLootTable(final LootTable table) {
      return simpleBuilder((weight, quality, conditions, functions) -> new NestedLootTable(Either.right(table), weight, quality, conditions, functions));
   }
}
