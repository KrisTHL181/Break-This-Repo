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
   public static final MapCodec<LootItem> CODEC = RecordCodecBuilder.mapCodec(
      p_360665_ -> p_360665_.group(Item.CODEC.fieldOf("name").forGetter(p_297028_ -> p_297028_.item))
         .and(singletonFields(p_360665_))
         .apply(p_360665_, LootItem::new)
   );
   private final Holder<Item> item;

   private LootItem(Holder<Item> p_298860_, int p_79567_, int p_79568_, List<LootItemCondition> p_299681_, List<LootItemFunction> p_298494_) {
      super(p_79567_, p_79568_, p_299681_, p_298494_);
      this.item = p_298860_;
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.ITEM;
   }

   @Override
   public void createItemStack(Consumer<ItemStack> p_79590_, LootContext p_79591_) {
      p_79590_.accept(new ItemStack(this.item));
   }

   public static LootPoolSingletonContainer.Builder<?> lootTableItem(ItemLike p_79580_) {
      return simpleBuilder(
         (p_297030_, p_297031_, p_297032_, p_297033_) -> new LootItem(p_79580_.asItem().builtInRegistryHolder(), p_297030_, p_297031_, p_297032_, p_297033_)
      );
   }
}
