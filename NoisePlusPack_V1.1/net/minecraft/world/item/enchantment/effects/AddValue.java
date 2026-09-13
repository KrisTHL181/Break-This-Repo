package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record AddValue(LevelBasedValue value) implements EnchantmentValueEffect {
   public static final MapCodec<AddValue> CODEC = RecordCodecBuilder.mapCodec(
      p_342444_ -> p_342444_.group(LevelBasedValue.CODEC.fieldOf("value").forGetter(AddValue::value)).apply(p_342444_, AddValue::new)
   );

   @Override
   public float process(int p_342885_, RandomSource p_342107_, float p_343617_) {
      return p_343617_ + this.value.calculate(p_342885_);
   }

   @Override
   public MapCodec<AddValue> codec() {
      return CODEC;
   }
}
