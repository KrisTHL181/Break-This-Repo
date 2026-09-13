package net.minecraft.world.level.levelgen.heightproviders;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public class WeightedListHeight extends HeightProvider {
   public static final MapCodec<WeightedListHeight> CODEC = RecordCodecBuilder.mapCodec(
      p_391047_ -> p_391047_.group(WeightedList.nonEmptyCodec(HeightProvider.CODEC).fieldOf("distribution").forGetter(p_391046_ -> p_391046_.distribution))
         .apply(p_391047_, WeightedListHeight::new)
   );
   private final WeightedList<HeightProvider> distribution;

   public WeightedListHeight(WeightedList<HeightProvider> p_396498_) {
      this.distribution = p_396498_;
   }

   @Override
   public int sample(RandomSource p_226314_, WorldGenerationContext p_226315_) {
      return this.distribution.getRandomOrThrow(p_226314_).sample(p_226314_, p_226315_);
   }

   @Override
   public HeightProviderType<?> getType() {
      return HeightProviderType.WEIGHTED_LIST;
   }
}
