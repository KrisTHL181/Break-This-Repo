package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record RemoveBinomial(LevelBasedValue chance) implements EnchantmentValueEffect {
   public static final MapCodec<RemoveBinomial> CODEC = RecordCodecBuilder.mapCodec(
      p_345282_ -> p_345282_.group(LevelBasedValue.CODEC.fieldOf("chance").forGetter(RemoveBinomial::chance)).apply(p_345282_, RemoveBinomial::new)
   );

   @Override
   public float process(int p_345007_, RandomSource p_342090_, float p_344829_) {
      float f = this.chance.calculate(p_345007_);
      int i = 0;
      if (!(p_344829_ <= 128.0F) && !(p_344829_ * f < 20.0F) && !(p_344829_ * (1.0F - f) < 20.0F)) {
         double d1 = Math.floor(p_344829_ * f);
         double d0 = Math.sqrt(p_344829_ * f * (1.0F - f));
         i = (int)Math.round(d1 + p_342090_.nextGaussian() * d0);
         i = Math.clamp(i, 0, (int)p_344829_);
      } else {
         for (int j = 0; j < p_344829_; j++) {
            if (p_342090_.nextFloat() < f) {
               i++;
            }
         }
      }

      return p_344829_ - i;
   }

   @Override
   public MapCodec<RemoveBinomial> codec() {
      return CODEC;
   }
}
