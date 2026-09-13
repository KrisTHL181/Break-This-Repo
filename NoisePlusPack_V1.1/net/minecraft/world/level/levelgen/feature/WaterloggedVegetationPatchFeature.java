package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class WaterloggedVegetationPatchFeature extends VegetationPatchFeature {
   public WaterloggedVegetationPatchFeature(Codec<VegetationPatchConfiguration> p_160635_) {
      super(p_160635_);
   }

   @Override
   protected Set<BlockPos> placeGroundPatch(
      WorldGenLevel p_225339_,
      VegetationPatchConfiguration p_225340_,
      RandomSource p_225341_,
      BlockPos p_225342_,
      Predicate<BlockState> p_225343_,
      int p_225344_,
      int p_225345_
   ) {
      Set<BlockPos> set = super.placeGroundPatch(p_225339_, p_225340_, p_225341_, p_225342_, p_225343_, p_225344_, p_225345_);
      Set<BlockPos> set1 = new HashSet<>();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for (BlockPos blockpos : set) {
         if (!isExposed(p_225339_, set, blockpos, blockpos$mutableblockpos)) {
            set1.add(blockpos);
         }
      }

      for (BlockPos blockpos1 : set1) {
         p_225339_.setBlock(blockpos1, Blocks.WATER.defaultBlockState(), 2);
      }

      return set1;
   }

   private static boolean isExposed(WorldGenLevel p_160656_, Set<BlockPos> p_160657_, BlockPos p_160658_, BlockPos.MutableBlockPos p_160659_) {
      return isExposedDirection(p_160656_, p_160658_, p_160659_, Direction.NORTH)
         || isExposedDirection(p_160656_, p_160658_, p_160659_, Direction.EAST)
         || isExposedDirection(p_160656_, p_160658_, p_160659_, Direction.SOUTH)
         || isExposedDirection(p_160656_, p_160658_, p_160659_, Direction.WEST)
         || isExposedDirection(p_160656_, p_160658_, p_160659_, Direction.DOWN);
   }

   private static boolean isExposedDirection(WorldGenLevel p_160651_, BlockPos p_160652_, BlockPos.MutableBlockPos p_160653_, Direction p_160654_) {
      p_160653_.setWithOffset(p_160652_, p_160654_);
      return !p_160651_.getBlockState(p_160653_).isFaceSturdy(p_160651_, p_160653_, p_160654_.getOpposite());
   }

   @Override
   protected boolean placeVegetation(
      WorldGenLevel p_225347_, VegetationPatchConfiguration p_225348_, ChunkGenerator p_225349_, RandomSource p_225350_, BlockPos p_225351_
   ) {
      if (super.placeVegetation(p_225347_, p_225348_, p_225349_, p_225350_, p_225351_.below())) {
         BlockState blockstate = p_225347_.getBlockState(p_225351_);
         if (blockstate.hasProperty(BlockStateProperties.WATERLOGGED) && !blockstate.getValue(BlockStateProperties.WATERLOGGED)) {
            p_225347_.setBlock(p_225351_, blockstate.setValue(BlockStateProperties.WATERLOGGED, true), 2);
         }

         return true;
      } else {
         return false;
      }
   }
}
