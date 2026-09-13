package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class RootSystemFeature extends Feature<RootSystemConfiguration> {
   public RootSystemFeature(Codec<RootSystemConfiguration> p_160218_) {
      super(p_160218_);
   }

   @Override
   public boolean place(FeaturePlaceContext<RootSystemConfiguration> p_160257_) {
      WorldGenLevel worldgenlevel = p_160257_.level();
      BlockPos blockpos = p_160257_.origin();
      if (!worldgenlevel.getBlockState(blockpos).isAir()) {
         return false;
      }

      RandomSource randomsource = p_160257_.random();
      BlockPos blockpos1 = p_160257_.origin();
      RootSystemConfiguration rootsystemconfiguration = p_160257_.config();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = blockpos1.mutable();
      if (placeDirtAndTree(worldgenlevel, p_160257_.chunkGenerator(), rootsystemconfiguration, randomsource, blockpos$mutableblockpos, blockpos1)) {
         placeRoots(worldgenlevel, rootsystemconfiguration, randomsource, blockpos1, blockpos$mutableblockpos);
      }

      return true;
   }

   private static boolean spaceForTree(WorldGenLevel p_160236_, RootSystemConfiguration p_160237_, BlockPos p_160238_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_160238_.mutable();

      for (int i = 1; i <= p_160237_.requiredVerticalSpaceForTree; i++) {
         blockpos$mutableblockpos.move(Direction.UP);
         BlockState blockstate = p_160236_.getBlockState(blockpos$mutableblockpos);
         if (!isAllowedTreeSpace(blockstate, i, p_160237_.allowedVerticalWaterForTree)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isAllowedTreeSpace(BlockState p_160253_, int p_160254_, int p_160255_) {
      if (p_160253_.isAir()) {
         return true;
      }

      int i = p_160254_ + 1;
      return i <= p_160255_ && p_160253_.getFluidState().is(FluidTags.WATER);
   }

   private static boolean placeDirtAndTree(
      WorldGenLevel p_225203_,
      ChunkGenerator p_225204_,
      RootSystemConfiguration p_225205_,
      RandomSource p_225206_,
      BlockPos.MutableBlockPos p_225207_,
      BlockPos p_225208_
   ) {
      for (int i = 0; i < p_225205_.rootColumnMaxHeight; i++) {
         p_225207_.move(Direction.UP);
         if (p_225205_.allowedTreePosition.test(p_225203_, p_225207_) && spaceForTree(p_225203_, p_225205_, p_225207_)) {
            BlockPos blockpos = p_225207_.below();
            if (p_225203_.getFluidState(blockpos).is(FluidTags.LAVA) || !p_225203_.getBlockState(blockpos).isSolid()) {
               return false;
            }

            if (p_225205_.treeFeature.value().place(p_225203_, p_225204_, p_225206_, p_225207_)) {
               placeDirt(p_225208_, p_225208_.getY() + i, p_225203_, p_225205_, p_225206_);
               return true;
            }
         }
      }

      return false;
   }

   private static void placeDirt(BlockPos p_225223_, int p_225224_, WorldGenLevel p_225225_, RootSystemConfiguration p_225226_, RandomSource p_225227_) {
      int i = p_225223_.getX();
      int j = p_225223_.getZ();
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_225223_.mutable();

      for (int k = p_225223_.getY(); k < p_225224_; k++) {
         placeRootedDirt(p_225225_, p_225226_, p_225227_, i, j, blockpos$mutableblockpos.set(i, k, j));
      }
   }

   private static void placeRootedDirt(
      WorldGenLevel p_225210_, RootSystemConfiguration p_225211_, RandomSource p_225212_, int p_225213_, int p_225214_, BlockPos.MutableBlockPos p_225215_
   ) {
      int i = p_225211_.rootRadius;
      Predicate<BlockState> predicate = p_204762_ -> p_204762_.is(p_225211_.rootReplaceable);

      for (int j = 0; j < p_225211_.rootPlacementAttempts; j++) {
         p_225215_.setWithOffset(p_225215_, p_225212_.nextInt(i) - p_225212_.nextInt(i), 0, p_225212_.nextInt(i) - p_225212_.nextInt(i));
         if (predicate.test(p_225210_.getBlockState(p_225215_))) {
            p_225210_.setBlock(p_225215_, p_225211_.rootStateProvider.getState(p_225212_, p_225215_), 2);
         }

         p_225215_.setX(p_225213_);
         p_225215_.setZ(p_225214_);
      }
   }

   private static void placeRoots(
      WorldGenLevel p_225217_, RootSystemConfiguration p_225218_, RandomSource p_225219_, BlockPos p_225220_, BlockPos.MutableBlockPos p_225221_
   ) {
      int i = p_225218_.hangingRootRadius;
      int j = p_225218_.hangingRootsVerticalSpan;

      for (int k = 0; k < p_225218_.hangingRootPlacementAttempts; k++) {
         p_225221_.setWithOffset(
            p_225220_, p_225219_.nextInt(i) - p_225219_.nextInt(i), p_225219_.nextInt(j) - p_225219_.nextInt(j), p_225219_.nextInt(i) - p_225219_.nextInt(i)
         );
         if (p_225217_.isEmptyBlock(p_225221_)) {
            BlockState blockstate = p_225218_.hangingRootStateProvider.getState(p_225219_, p_225221_);
            if (blockstate.canSurvive(p_225217_, p_225221_) && p_225217_.getBlockState(p_225221_.above()).isFaceSturdy(p_225217_, p_225221_, Direction.DOWN)) {
               p_225217_.setBlock(p_225221_, blockstate, 2);
            }
         }
      }
   }
}
