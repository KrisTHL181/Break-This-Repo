package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record WeepingVinesFeature() implements Feature {
   private static final Direction[] DIRECTIONS = Direction.values();
   public static final MapCodec<WeepingVinesFeature> CODEC = MapCodec.unit(WeepingVinesFeature::new);

   @Override
   public MapCodec<WeepingVinesFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (!level.isEmptyBlock(origin)) {
         return false;
      }

      BlockState stateAbove = level.getBlockState(origin.above());
      if (!stateAbove.is(Blocks.NETHERRACK) && !stateAbove.is(Blocks.NETHER_WART_BLOCK)) {
         return false;
      }

      this.placeRoofNetherWart(level, random, origin);
      this.placeRoofWeepingVines(level, random, origin);
      return true;
   }

   private void placeRoofNetherWart(final LevelAccessor level, final RandomSource random, final BlockPos origin) {
      level.setBlock(origin, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
      BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos neighbourPos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < 200; i++) {
         placePos.setWithOffset(origin, random.nextInt(6) - random.nextInt(6), random.nextInt(2) - random.nextInt(5), random.nextInt(6) - random.nextInt(6));
         if (level.isEmptyBlock(placePos)) {
            int neighbours = 0;

            for (Direction direction : DIRECTIONS) {
               BlockState neighbourBlockState = level.getBlockState(neighbourPos.setWithOffset(placePos, direction));
               if (neighbourBlockState.is(Blocks.NETHERRACK) || neighbourBlockState.is(Blocks.NETHER_WART_BLOCK)) {
                  neighbours++;
               }

               if (neighbours > 1) {
                  break;
               }
            }

            if (neighbours == 1) {
               level.setBlock(placePos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
            }
         }
      }
   }

   private void placeRoofWeepingVines(final LevelAccessor level, final RandomSource random, final BlockPos origin) {
      BlockPos.MutableBlockPos placePos = new BlockPos.MutableBlockPos();

      for (int i = 0; i < 100; i++) {
         placePos.setWithOffset(origin, random.nextInt(8) - random.nextInt(8), random.nextInt(2) - random.nextInt(7), random.nextInt(8) - random.nextInt(8));
         if (level.isEmptyBlock(placePos)) {
            BlockState stateAbove = level.getBlockState(placePos.above());
            if (stateAbove.is(Blocks.NETHERRACK) || stateAbove.is(Blocks.NETHER_WART_BLOCK)) {
               int vineHeight = Mth.nextInt(random, 1, 8);
               if (random.nextInt(6) == 0) {
                  vineHeight *= 2;
               }

               if (random.nextInt(5) == 0) {
                  vineHeight = 1;
               }

               int minVineAge = 17;
               int maxVineAge = 25;
               placeWeepingVinesColumn(level, random, placePos, vineHeight, 17, 25);
            }
         }
      }
   }

   public static void placeWeepingVinesColumn(
      final LevelAccessor level, final RandomSource random, final BlockPos.MutableBlockPos placePos, final int totalHeight, final int minAge, final int naxAge
   ) {
      for (int height = 0; height <= totalHeight; height++) {
         if (level.isEmptyBlock(placePos)) {
            if (height == totalHeight || !level.isEmptyBlock(placePos.below())) {
               level.setBlock(placePos, Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Mth.nextInt(random, minAge, naxAge)), 2);
               break;
            }

            level.setBlock(placePos, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
         }

         placePos.move(Direction.DOWN);
      }
   }
}
