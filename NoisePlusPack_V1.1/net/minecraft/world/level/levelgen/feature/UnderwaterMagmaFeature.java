package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.UnderwaterMagmaConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaConfiguration> {
   public UnderwaterMagmaFeature(Codec<UnderwaterMagmaConfiguration> p_160560_) {
      super(p_160560_);
   }

   @Override
   public boolean place(FeaturePlaceContext<UnderwaterMagmaConfiguration> p_160569_) {
      WorldGenLevel worldgenlevel = p_160569_.level();
      BlockPos blockpos = p_160569_.origin();
      UnderwaterMagmaConfiguration underwatermagmaconfiguration = p_160569_.config();
      RandomSource randomsource = p_160569_.random();
      OptionalInt optionalint = getFloorY(worldgenlevel, blockpos, underwatermagmaconfiguration);
      if (optionalint.isEmpty()) {
         return false;
      }

      BlockPos blockpos1 = blockpos.atY(optionalint.getAsInt());
      Vec3i vec3i = new Vec3i(
         underwatermagmaconfiguration.placementRadiusAroundFloor,
         underwatermagmaconfiguration.placementRadiusAroundFloor,
         underwatermagmaconfiguration.placementRadiusAroundFloor
      );
      BoundingBox boundingbox = BoundingBox.fromCorners(blockpos1.subtract(vec3i), blockpos1.offset(vec3i));
      return BlockPos.betweenClosedStream(boundingbox)
            .filter(p_225310_ -> randomsource.nextFloat() < underwatermagmaconfiguration.placementProbabilityPerValidPosition)
            .filter(p_160584_ -> this.isValidPlacement(worldgenlevel, p_160584_))
            .mapToInt(p_160579_ -> {
               worldgenlevel.setBlock(p_160579_, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
               return 1;
            })
            .sum()
         > 0;
   }

   private static OptionalInt getFloorY(WorldGenLevel p_160565_, BlockPos p_160566_, UnderwaterMagmaConfiguration p_160567_) {
      Predicate<BlockState> predicate = p_160586_ -> p_160586_.is(Blocks.WATER);
      Predicate<BlockState> predicate1 = p_160581_ -> !p_160581_.is(Blocks.WATER);
      Optional<Column> optional = Column.scan(p_160565_, p_160566_, p_160567_.floorSearchRange, predicate, predicate1);
      return optional.map(Column::getFloor).orElseGet(OptionalInt::empty);
   }

   private boolean isValidPlacement(WorldGenLevel p_160575_, BlockPos p_160576_) {
      if (!isWaterOrAir(p_160575_.getBlockState(p_160576_)) && !this.isVisibleFromOutside(p_160575_, p_160576_.below(), Direction.UP)) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.isVisibleFromOutside(p_160575_, p_160576_.relative(direction), direction.getOpposite())) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static boolean isWaterOrAir(BlockState p_452532_) {
      return p_452532_.is(Blocks.WATER) || p_452532_.isAir();
   }

   private boolean isVisibleFromOutside(LevelAccessor p_460779_, BlockPos p_451516_, Direction p_454887_) {
      BlockState blockstate = p_460779_.getBlockState(p_451516_);
      VoxelShape voxelshape = blockstate.getFaceOcclusionShape(p_454887_);
      return voxelshape == Shapes.empty() || !Block.isShapeFullBlock(voxelshape);
   }
}
