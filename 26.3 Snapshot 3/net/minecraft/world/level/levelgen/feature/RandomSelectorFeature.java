package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

@Deprecated
public record RandomSelectorFeature(List<WeightedPlacedFeature> features, Holder<PlacedFeature> defaultFeature) implements Feature {
   public static final MapCodec<RandomSelectorFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            WeightedPlacedFeature.CODEC.listOf().fieldOf("features").forGetter(RandomSelectorFeature::features),
            PlacedFeature.CODEC.fieldOf("default").forGetter(RandomSelectorFeature::defaultFeature)
         )
         .apply(i, RandomSelectorFeature::new)
   );

   @Override
   public MapCodec<RandomSelectorFeature> codec() {
      return CODEC;
   }

   @Override
   public Stream<Holder<Feature>> getSubFeatures() {
      return Stream.concat(this.features.stream().flatMap(weighted -> weighted.feature().value().getFeatures()), this.defaultFeature.value().getFeatures());
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      for (WeightedPlacedFeature feature : this.features) {
         if (random.nextFloat() < feature.chance()) {
            return feature.place(level, chunkGenerator, random, origin);
         }
      }

      return this.defaultFeature.value().place(level, chunkGenerator, random, origin);
   }
}
