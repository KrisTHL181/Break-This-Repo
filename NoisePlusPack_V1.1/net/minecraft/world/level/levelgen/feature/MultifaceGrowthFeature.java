package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;

public class MultifaceGrowthFeature extends Feature<MultifaceGrowthConfiguration> {
   public MultifaceGrowthFeature(Codec<MultifaceGrowthConfiguration> p_225156_) {
      super(p_225156_);
   }

   @Override
   public boolean place(FeaturePlaceContext<MultifaceGrowthConfiguration> p_225165_) {
      WorldGenLevel worldgenlevel = p_225165_.level();
      BlockPos blockpos = p_225165_.origin();
      RandomSource randomsource = p_225165_.random();
      MultifaceGrowthConfiguration multifacegrowthconfiguration = p_225165_.config();
      if (!isAirOrWater(worldgenlevel.getBlockState(blockpos))) {
         return false;
      }

      List<Direction> list = multifacegrowthconfiguration.getShuffledDirections(randomsource);
      if (placeGrowthIfPossible(worldgenlevel, blockpos, worldgenlevel.getBlockState(blockpos), multifacegrowthconfiguration, randomsource, list)) {
         return true;
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos = blockpos.mutable();

      for (Direction direction : list) {
         blockpos$mutableblockpos.set(blockpos);
         List<Direction> list1 = multifacegrowthconfiguration.getShuffledDirectionsExcept(randomsource, direction.getOpposite());

         for (int i = 0; i < multifacegrowthconfiguration.searchRange; i++) {
            blockpos$mutableblockpos.setWithOffset(blockpos, direction);
            BlockState blockstate = worldgenlevel.getBlockState(blockpos$mutableblockpos);
            if (!isAirOrWater(blockstate) && !blockstate.is(multifacegrowthconfiguration.placeBlock)) {
               break;
            }

            if (placeGrowthIfPossible(worldgenlevel, blockpos$mutableblockpos, blockstate, multifacegrowthconfiguration, randomsource, list1)) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean placeGrowthIfPossible(
      WorldGenLevel p_225158_,
      BlockPos p_225159_,
      BlockState p_225160_,
      MultifaceGrowthConfiguration p_225161_,
      RandomSource p_225162_,
      List<Direction> p_225163_
   ) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_225159_.mutable();

      for (Direction direction : p_225163_) {
         BlockState blockstate = p_225158_.getBlockState(blockpos$mutableblockpos.setWithOffset(p_225159_, direction));
         if (blockstate.is(p_225161_.canBePlacedOn)) {
            BlockState blockstate1 = p_225161_.placeBlock.getStateForPlacement(p_225160_, p_225158_, p_225159_, direction);
            if (blockstate1 == null) {
               return false;
            }

            p_225158_.setBlock(p_225159_, blockstate1, 3);
            p_225158_.getChunk(p_225159_).markPosForPostprocessing(p_225159_);
            if (p_225162_.nextFloat() < p_225161_.chanceOfSpreading) {
               p_225161_.placeBlock.getSpreader().spreadFromFaceTowardRandomDirection(blockstate1, p_225158_, p_225159_, direction, p_225162_, true);
            }

            return true;
         }
      }

      return false;
   }

   private static boolean isAirOrWater(BlockState p_225167_) {
      return p_225167_.isAir() || p_225167_.is(Blocks.WATER);
   }
}
