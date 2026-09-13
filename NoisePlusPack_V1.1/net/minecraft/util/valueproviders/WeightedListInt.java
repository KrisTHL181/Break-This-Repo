package net.minecraft.util.valueproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;

public class WeightedListInt extends IntProvider {
   public static final MapCodec<WeightedListInt> CODEC = RecordCodecBuilder.mapCodec(
      p_390462_ -> p_390462_.group(WeightedList.nonEmptyCodec(IntProvider.CODEC).fieldOf("distribution").forGetter(p_390461_ -> p_390461_.distribution))
         .apply(p_390462_, WeightedListInt::new)
   );
   private final WeightedList<IntProvider> distribution;
   private final int minValue;
   private final int maxValue;

   public WeightedListInt(WeightedList<IntProvider> p_391951_) {
      this.distribution = p_391951_;
      int i = Integer.MAX_VALUE;
      int j = Integer.MIN_VALUE;

      for (Weighted<IntProvider> weighted : p_391951_.unwrap()) {
         int k = weighted.value().getMinValue();
         int l = weighted.value().getMaxValue();
         i = Math.min(i, k);
         j = Math.max(j, l);
      }

      this.minValue = i;
      this.maxValue = j;
   }

   @Override
   public int sample(RandomSource p_216870_) {
      return this.distribution.getRandomOrThrow(p_216870_).sample(p_216870_);
   }

   @Override
   public int getMinValue() {
      return this.minValue;
   }

   @Override
   public int getMaxValue() {
      return this.maxValue;
   }

   @Override
   public IntProviderType<?> getType() {
      return IntProviderType.WEIGHTED_LIST;
   }
}
