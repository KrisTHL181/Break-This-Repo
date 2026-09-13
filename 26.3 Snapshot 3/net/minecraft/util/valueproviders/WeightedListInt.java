package net.minecraft.util.valueproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

public class WeightedListInt implements IntProvider {
   public static final MapCodec<WeightedListInt> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(WeightedList.nonEmptyCodec(IntProviders.CODEC).fieldOf("distribution").forGetter(c -> c.distribution)).apply(i, WeightedListInt::new)
   );
   private final WeightedList<IntProvider> distribution;
   private final int minValue;
   private final int maxValue;

   public WeightedListInt(final WeightedList<IntProvider> distribution) {
      this.distribution = distribution;
      int min = Integer.MAX_VALUE;
      int max = Integer.MIN_VALUE;

      for (Weighted<IntProvider> value : distribution.unwrap()) {
         int entryMin = value.value().minInclusive();
         int entryMax = value.value().maxInclusive();
         min = Math.min(min, entryMin);
         max = Math.max(max, entryMax);
      }

      this.minValue = min;
      this.maxValue = max;
   }

   @Override
   public int sample(final RandomSource random) {
      return this.distribution.getRandomOrThrow(random).sample(random);
   }

   @Override
   public int minInclusive() {
      return this.minValue;
   }

   @Override
   public int maxInclusive() {
      return this.maxValue;
   }

   @Override
   public MapCodec<WeightedListInt> codec() {
      return MAP_CODEC;
   }
}
