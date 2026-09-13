package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record OverlayFeature(HolderSet<PlacedFeature> features) implements Feature {
   public static final MapCodec<OverlayFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(ExtraCodecs.nonEmptyHolderSet(PlacedFeature.LIST_CODEC).fieldOf("features").forGetter(OverlayFeature::features))
         .apply(i, OverlayFeature::new)
   );

   @Override
   public MapCodec<OverlayFeature> codec() {
      return CODEC;
   }

   @Override
   public Stream<Holder<Feature>> getSubFeatures() {
      return this.features.stream().flatMap(f -> f.value().getFeatures());
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      FeaturePlacer placer = new FeaturePlacer(level, chunkGenerator);
      boolean placedAny = false;

      for (Holder<PlacedFeature> feature : this.features) {
         placedAny |= placer.place(feature.value(), random, origin);
      }

      return placedAny;
   }
}
