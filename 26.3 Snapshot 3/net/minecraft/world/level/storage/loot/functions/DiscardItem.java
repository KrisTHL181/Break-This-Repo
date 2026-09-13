package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DiscardItem extends LootItemConditionalFunction {
   public static final MapCodec<DiscardItem> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).apply(i, DiscardItem::new));

   protected DiscardItem(final List<LootItemCondition> predicates) {
      super(predicates);
   }

   @Override
   public MapCodec<DiscardItem> codec() {
      return MAP_CODEC;
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      return ItemStack.EMPTY;
   }

   public static LootItemConditionalFunction.Builder<?> discardItem() {
      return simpleBuilder(DiscardItem::new);
   }
}
