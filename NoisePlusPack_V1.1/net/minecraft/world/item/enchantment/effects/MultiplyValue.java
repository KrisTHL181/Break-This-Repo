package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record MultiplyValue(LevelBasedValue factor) implements EnchantmentValueEffect {
   public static final MapCodec<MultiplyValue> CODEC = RecordCodecBuilder.mapCodec(
      p_342244_ -> p_342244_.group(LevelBasedValue.CODEC.fieldOf("factor").forGetter(MultiplyValue::factor)).apply(p_342244_, MultiplyValue::new)
   );

   @Override
   public float process(int p_344987_, RandomSource p_344070_, float p_343762_) {
      return p_343762_ * this.factor.calculate(p_344987_);
   }

   @Override
   public MapCodec<MultiplyValue> codec() {
      return CODEC;
   }
}
