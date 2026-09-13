package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetPotionFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetPotionFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i).and(Potion.CODEC.fieldOf("id").forGetter(f -> f.potion)).apply(i, SetPotionFunction::new)
   );
   private final Holder<Potion> potion;

   private SetPotionFunction(final List<LootItemCondition> predicates, final Holder<Potion> potion) {
      super(predicates);
      this.potion = potion;
   }

   @Override
   public MapCodec<SetPotionFunction> codec() {
      return MAP_CODEC;
   }

   @Override
   public ItemStack run(final ItemStack itemStack, final LootContext context) {
      itemStack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, this.potion, PotionContents::withPotion);
      return itemStack;
   }

   public static LootItemConditionalFunction.Builder<?> setPotion(final Holder<Potion> value) {
      return simpleBuilder(conditions -> new SetPotionFunction(conditions, value));
   }
}
