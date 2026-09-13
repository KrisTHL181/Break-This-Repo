package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record EndIslandFeature() implements Feature {
   public static final MapCodec<EndIslandFeature> CODEC = MapCodec.unit(EndIslandFeature::new);

   @Override
   public MapCodec<EndIslandFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      float size = random.nextInt(3) + 4.0F;

      for (int y = 0; size > 0.5F; y--) {
         for (int x = Mth.floor(-size); x <= Mth.ceil(size); x++) {
            for (int z = Mth.floor(-size); z <= Mth.ceil(size); z++) {
               if (x * x + z * z <= (size + 1.0F) * (size + 1.0F)) {
                  this.setBlock(level, origin.offset(x, y, z), Blocks.END_STONE.defaultBlockState());
               }
            }
         }

         size -= random.nextInt(2) + 0.5F;
      }

      return true;
   }
}
