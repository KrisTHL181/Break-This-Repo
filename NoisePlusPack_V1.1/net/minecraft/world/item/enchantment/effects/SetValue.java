package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record SetValue(LevelBasedValue value) implements EnchantmentValueEffect {
   public static final MapCodec<SetValue> CODEC = RecordCodecBuilder.mapCodec(
      p_344143_ -> p_344143_.group(LevelBasedValue.CODEC.fieldOf("value").forGetter(SetValue::value)).apply(p_344143_, SetValue::new)
   );

   @Override
   public float process(int p_343689_, RandomSource p_343573_, float p_342308_) {
      return this.value.calculate(p_343689_);
   }

   @Override
   public MapCodec<SetValue> codec() {
      return CODEC;
   }
}
