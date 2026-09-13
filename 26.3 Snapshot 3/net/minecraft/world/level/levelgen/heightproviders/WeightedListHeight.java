package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class WeightedListHeight extends HeightProvider {
   public static final MapCodec<WeightedListHeight> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(WeightedList.nonEmptyCodec(HeightProvider.CODEC).fieldOf("distribution").forGetter(c -> c.distribution)).apply(i, WeightedListHeight::new)
   );
   private final WeightedList<HeightProvider> distribution;

   public WeightedListHeight(final WeightedList<HeightProvider> distribution) {
      this.distribution = distribution;
   }

   @Override
   public int sample(final RandomSource random, final WorldGenerationContext heightAccessor) {
      return this.distribution.getRandomOrThrow(random).sample(random, heightAccessor);
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.WEIGHTED_LIST;
   }
}
