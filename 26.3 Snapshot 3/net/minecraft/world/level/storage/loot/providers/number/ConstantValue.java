package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;

public record ConstantValue(float value) implements NumberProvider {
   public static final MapCodec<ConstantValue> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.FLOAT.fieldOf("value").forGetter(ConstantValue::value)).apply(i, ConstantValue::new)
   );
   public static final Codec<ConstantValue> INLINE_CODEC = Codec.FLOAT.xmap(ConstantValue::new, ConstantValue::value);

   @Override
   public MapCodec<ConstantValue> codec() {
      return MAP_CODEC;
   }

   @Override
   public float getFloat(final LootContext random) {
      return this.value;
   }

   public static ConstantValue exactly(final float value) {
      return new ConstantValue(value);
   }
}
