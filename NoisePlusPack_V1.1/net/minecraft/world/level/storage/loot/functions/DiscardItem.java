package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DiscardItem extends LootItemConditionalFunction {
   public static final MapCodec<DiscardItem> CODEC = RecordCodecBuilder.mapCodec(p_458987_ -> commonFields(p_458987_).apply(p_458987_, DiscardItem::new));

   protected DiscardItem(List<LootItemCondition> p_450473_) {
      super(p_450473_);
   }

   @Override
   public LootItemFunctionType<DiscardItem> getType() {
      return LootItemFunctions.DISCARD;
   }

   @Override
   protected ItemStack run(ItemStack p_460147_, LootContext p_458480_) {
      return ItemStack.EMPTY;
   }

   public static LootItemConditionalFunction.Builder<?> discardItem() {
      return simpleBuilder(DiscardItem::new);
   }
}
