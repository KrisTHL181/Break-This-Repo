package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record RandomBooleanSelectorFeature(Holder<PlacedFeature> featureTrue, Holder<PlacedFeature> featureFalse) implements Feature {
   public static final MapCodec<RandomBooleanSelectorFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            PlacedFeature.CODEC.fieldOf("feature_true").forGetter(RandomBooleanSelectorFeature::featureTrue),
            PlacedFeature.CODEC.fieldOf("feature_false").forGetter(RandomBooleanSelectorFeature::featureFalse)
         )
         .apply(i, RandomBooleanSelectorFeature::new)
   );

   @Override
   public MapCodec<RandomBooleanSelectorFeature> codec() {
      return CODEC;
   }

   @Override
   public Stream<Holder<Feature>> getSubFeatures() {
      return Stream.concat(this.featureTrue.value().getFeatures(), this.featureFalse.value().getFeatures());
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      boolean result = random.nextBoolean();
      return (result ? this.featureTrue : this.featureFalse).value().place(level, chunkGenerator, random, origin);
   }
}
