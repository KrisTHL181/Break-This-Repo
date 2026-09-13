package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record ChorusPlantFeature() implements Feature {
   public static final MapCodec<ChorusPlantFeature> CODEC = MapCodec.unit(ChorusPlantFeature::new);

   @Override
   public MapCodec<ChorusPlantFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (level.isEmptyBlock(origin) && level.getBlockState(origin.below()).is(BlockTags.SUPPORTS_CHORUS_PLANT)) {
         ChorusFlowerBlock.generatePlant(level, origin, random, 8);
         return true;
      } else {
         return false;
      }
   }
}
