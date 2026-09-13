package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record VoidStartPlatformFeature() implements Feature {
   private static final BlockPos PLATFORM_OFFSET = new BlockPos(8, 3, 8);
   private static final ChunkPos PLATFORM_ORIGIN_CHUNK = ChunkPos.containing(PLATFORM_OFFSET);
   private static final int PLATFORM_RADIUS = 16;
   private static final int PLATFORM_RADIUS_CHUNKS = 1;
   public static final MapCodec<VoidStartPlatformFeature> CODEC = MapCodec.unit(VoidStartPlatformFeature::new);

   @Override
   public MapCodec<VoidStartPlatformFeature> codec() {
      return CODEC;
   }

   private static int checkerboardDistance(final int xa, final int za, final int xb, final int zb) {
      return Math.max(Math.abs(xa - xb), Math.abs(za - zb));
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      ChunkPos currentChunkPos = ChunkPos.containing(origin);
      if (checkerboardDistance(currentChunkPos.x(), currentChunkPos.z(), PLATFORM_ORIGIN_CHUNK.x(), PLATFORM_ORIGIN_CHUNK.z()) > 1) {
         return true;
      }

      BlockPos platformOrigin = PLATFORM_OFFSET.atY(origin.getY() + PLATFORM_OFFSET.getY());
      BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

      for (int z = currentChunkPos.getMinBlockZ(); z <= currentChunkPos.getMaxBlockZ(); z++) {
         for (int x = currentChunkPos.getMinBlockX(); x <= currentChunkPos.getMaxBlockX(); x++) {
            if (checkerboardDistance(platformOrigin.getX(), platformOrigin.getZ(), x, z) <= 16) {
               blockPos.set(x, platformOrigin.getY(), z);
               if (blockPos.equals(platformOrigin)) {
                  level.setBlock(blockPos, Blocks.COBBLESTONE.defaultBlockState(), 2);
               } else {
                  level.setBlock(blockPos, Blocks.STONE.defaultBlockState(), 2);
               }
            }
         }
      }

      return true;
   }
}
