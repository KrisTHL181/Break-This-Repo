package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class WaterloggedVegetationPatchFeature extends VegetationPatchFeature {
   public static final MapCodec<WaterloggedVegetationPatchFeature> CODEC = makeCodec(WaterloggedVegetationPatchFeature::new);

   public WaterloggedVegetationPatchFeature(
      final HolderSet<Block> replaceable,
      final BlockStateProvider groundState,
      final Holder<PlacedFeature> vegetationFeature,
      final CaveSurface surface,
      final IntProvider depth,
      final float extraBottomBlockChance,
      final int verticalRange,
      final float vegetationChance,
      final IntProvider xzRadius,
      final float extraEdgeColumnChance
   ) {
      super(
         replaceable, groundState, vegetationFeature, surface, depth, extraBottomBlockChance, verticalRange, vegetationChance, xzRadius, extraEdgeColumnChance
      );
   }

   @Override
   public MapCodec<WaterloggedVegetationPatchFeature> codec() {
      return CODEC;
   }

   @Override
   public Set<BlockPos> placeGroundPatch(
      final WorldGenLevel level,
      final RandomSource random,
      final BlockPos origin,
      final Predicate<BlockState> replaceable,
      final int xRadius,
      final int zRadius
   ) {
      Set<BlockPos> surface = super.placeGroundPatch(level, random, origin, replaceable, xRadius, zRadius);
      Set<BlockPos> waterSurface = new HashSet<>();
      BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos();

      for (BlockPos surfacePos : surface) {
         if (!isExposed(level, surface, surfacePos, testPos)) {
            waterSurface.add(surfacePos);
         }
      }

      for (BlockPos surfacePos : waterSurface) {
         level.setBlock(surfacePos, Blocks.WATER.defaultBlockState(), 2);
      }

      return waterSurface;
   }

   private static boolean isExposed(final WorldGenLevel level, final Set<BlockPos> surface, final BlockPos pos, final BlockPos.MutableBlockPos testPos) {
      return isExposedDirection(level, pos, testPos, Direction.NORTH)
         || isExposedDirection(level, pos, testPos, Direction.EAST)
         || isExposedDirection(level, pos, testPos, Direction.SOUTH)
         || isExposedDirection(level, pos, testPos, Direction.WEST)
         || isExposedDirection(level, pos, testPos, Direction.DOWN);
   }

   private static boolean isExposedDirection(final WorldGenLevel level, final BlockPos pos, final BlockPos.MutableBlockPos testPos, final Direction direction) {
      testPos.setWithOffset(pos, direction);
      return !level.getBlockState(testPos).isFaceSturdy(level, testPos, direction.getOpposite());
   }

   @Override
   protected boolean placeVegetation(final WorldGenLevel level, final ChunkGenerator generator, final RandomSource random, final BlockPos placementPos) {
      if (super.placeVegetation(level, generator, random, placementPos.below())) {
         BlockState placed = level.getBlockState(placementPos);
         if (placed.hasProperty(BlockStateProperties.WATERLOGGED) && !placed.getValue(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(placementPos, placed.setValue(BlockStateProperties.WATERLOGGED, true), 2);
         }

         return true;
      } else {
         return false;
      }
   }
}
