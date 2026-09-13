package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record EndPlatformFeature() implements Feature {
   public static final MapCodec<EndPlatformFeature> CODEC = MapCodec.unit(EndPlatformFeature::new);

   @Override
   public MapCodec<EndPlatformFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      createEndPlatform(level, origin, false);
      return true;
   }

   public static void createEndPlatform(final ServerLevelAccessor newLevel, final BlockPos origin, final boolean dropResources) {
      BlockPos.MutableBlockPos pos = origin.mutable();

      for (int dz = -2; dz <= 2; dz++) {
         for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy < 3; dy++) {
               BlockPos blockPos = pos.set(origin).move(dx, dy, dz);
               Block block = dy == -1 ? Blocks.OBSIDIAN : Blocks.AIR;
               if (!newLevel.getBlockState(blockPos).is(block)) {
                  if (dropResources) {
                     newLevel.destroyBlock(blockPos, true, null);
                  }

                  newLevel.setBlockAndUpdate(blockPos, block.defaultBlockState());
               }
            }
         }
      }
   }
}
