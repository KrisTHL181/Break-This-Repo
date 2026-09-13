package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

@Deprecated
public record WeightedPlacedFeature(Holder<PlacedFeature> feature, float chance) {
   public static final Codec<WeightedPlacedFeature> CODEC = RecordCodecBuilder.create(
      i -> i.group(PlacedFeature.CODEC.fieldOf("feature").forGetter(f -> f.feature), Codec.floatRange(0.0F, 1.0F).fieldOf("chance").forGetter(f -> f.chance))
         .apply(i, WeightedPlacedFeature::new)
   );

   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      return this.feature.value().place(level, chunkGenerator, random, origin);
   }
}
