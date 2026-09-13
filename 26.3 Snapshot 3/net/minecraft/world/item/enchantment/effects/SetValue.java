package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.LevelBasedValue;

public record SetValue(LevelBasedValue value) implements EnchantmentValueEffect {
   public static final MapCodec<SetValue> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(LevelBasedValue.CODEC.fieldOf("value").forGetter(SetValue::value)).apply(i, SetValue::new)
   );

   @Override
   public float process(final int enchantmentLevel, final RandomSource random, final float inputValue) {
      return this.value.calculate(enchantmentLevel);
   }

   @Override
   public MapCodec<SetValue> codec() {
      return CODEC;
   }
}
