package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ApplyExplosionDecay extends LootItemConditionalFunction {
   public static final MapCodec<ApplyExplosionDecay> CODEC = RecordCodecBuilder.mapCodec(
      p_297802_ -> commonFields(p_297802_).apply(p_297802_, ApplyExplosionDecay::new)
   );

   private ApplyExplosionDecay(List<LootItemCondition> p_301217_) {
      super(p_301217_);
   }

   @Override
   public LootItemFunctionType<ApplyExplosionDecay> getType() {
      return LootItemFunctions.EXPLOSION_DECAY;
   }

   @Override
   public ItemStack run(ItemStack p_80034_, LootContext p_80035_) {
      Float f = p_80035_.getOptionalParameter(LootContextParams.EXPLOSION_RADIUS);
      if (f != null) {
         RandomSource randomsource = p_80035_.getRandom();
         float f1 = 1.0F / f;
         int i = p_80034_.getCount();
         int j = 0;

         for (int k = 0; k < i; k++) {
            if (randomsource.nextFloat() <= f1) {
               j++;
            }
         }

         p_80034_.setCount(j);
      }

      return p_80034_;
   }

   public static LootItemConditionalFunction.Builder<?> explosionDecay() {
      return simpleBuilder(ApplyExplosionDecay::new);
   }
}
