package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record BlueIceFeature() implements Feature {
   public static final MapCodec<BlueIceFeature> CODEC = MapCodec.unit(BlueIceFeature::new);

   @Override
   public MapCodec<BlueIceFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (origin.getY() > level.getSeaLevel() - 1) {
         return false;
      }

      if (!level.getBlockState(origin).is(Blocks.WATER) && !level.getBlockState(origin.below()).is(Blocks.WATER)) {
         return false;
      }

      boolean foundPackedIce = false;

      for (Direction direction : Direction.values()) {
         if (direction != Direction.DOWN && level.getBlockState(origin.relative(direction)).is(Blocks.PACKED_ICE)) {
            foundPackedIce = true;
            break;
         }
      }

      if (!foundPackedIce) {
         return false;
      }

      level.setBlock(origin, Blocks.BLUE_ICE.defaultBlockState(), 2);

      for (int i = 0; i < 200; i++) {
         int yOff = random.nextInt(5) - random.nextInt(6);
         int xzDiff = 3;
         if (yOff < 2) {
            xzDiff += yOff / 2;
         }

         if (xzDiff >= 1) {
            BlockPos placePos = origin.offset(random.nextInt(xzDiff) - random.nextInt(xzDiff), yOff, random.nextInt(xzDiff) - random.nextInt(xzDiff));
            BlockState placeState = level.getBlockState(placePos);
            if (placeState.isAir() || placeState.is(Blocks.WATER) || placeState.is(Blocks.PACKED_ICE) || placeState.is(Blocks.ICE)) {
               for (Direction direction : Direction.values()) {
                  BlockState relativeBlockState = level.getBlockState(placePos.relative(direction));
                  if (relativeBlockState.is(Blocks.BLUE_ICE)) {
                     level.setBlock(placePos, Blocks.BLUE_ICE.defaultBlockState(), 2);
                     break;
                  }
               }
            }
         }
      }

      return true;
   }
}
