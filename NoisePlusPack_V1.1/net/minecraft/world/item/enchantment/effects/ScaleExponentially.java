package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record ScaleExponentially(LevelBasedValue base, LevelBasedValue exponent) implements EnchantmentValueEffect {
   public static final MapCodec<ScaleExponentially> CODEC = RecordCodecBuilder.mapCodec(
      p_453029_ -> p_453029_.group(
            LevelBasedValue.CODEC.fieldOf("base").forGetter(ScaleExponentially::base),
            LevelBasedValue.CODEC.fieldOf("exponent").forGetter(ScaleExponentially::exponent)
         )
         .apply(p_453029_, ScaleExponentially::new)
   );

   @Override
   public float process(int p_451954_, RandomSource p_456903_, float p_457398_) {
      return (float)(p_457398_ * Math.pow(this.base.calculate(p_451954_), this.exponent.calculate(p_451954_)));
   }

   @Override
   public MapCodec<ScaleExponentially> codec() {
      return CODEC;
   }
}
