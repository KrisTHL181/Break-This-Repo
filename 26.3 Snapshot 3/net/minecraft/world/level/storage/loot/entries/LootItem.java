package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class LootItem extends LootPoolSingletonContainer {
   public static final MapCodec<LootItem> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Item.CODEC.fieldOf("name").forGetter(e -> e.item)).and(singletonFields(i)).apply(i, LootItem::new)
   );
   private final Holder<Item> item;

   private LootItem(
      final Holder<Item> item, final int weight, final int quality, final List<LootItemCondition> conditions, final List<LootItemFunction> functions
   ) {
      super(weight, quality, conditions, functions);
      this.item = item;
   }

   @Override
   public MapCodec<LootItem> codec() {
      return MAP_CODEC;
   }

   @Override
   public void createItemStack(final Consumer<ItemStack> output, final LootContext context) {
      output.accept(new ItemStack(this.item));
   }

   public static LootPoolSingletonContainer.Builder<?> lootTableItem(final ItemLike item) {
      return simpleBuilder(
         (weight, quality, conditions, functions) -> new LootItem(item.asItem().builtInRegistryHolder(), weight, quality, conditions, functions)
      );
   }
}
