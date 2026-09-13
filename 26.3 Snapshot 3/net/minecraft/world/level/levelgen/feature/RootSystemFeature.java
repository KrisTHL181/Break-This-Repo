package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record RootSystemFeature(
   Holder<PlacedFeature> treeFeature,
   int requiredVerticalSpaceForTree,
   int levelTestDistance,
   int maxLevelDeviation,
   int rootRadius,
   HolderSet<Block> rootReplaceable,
   BlockStateProvider rootStateProvider,
   int rootPlacementAttempts,
   int rootColumnMaxHeight,
   int hangingRootRadius,
   int hangingRootsVerticalSpan,
   BlockStateProvider hangingRootStateProvider,
   int hangingRootPlacementAttempts,
   int allowedVerticalWaterForTree,
   BlockPredicate allowedTreePosition
) implements Feature {
   public static final MapCodec<RootSystemFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            PlacedFeature.CODEC.fieldOf("feature").forGetter(RootSystemFeature::treeFeature),
            Codec.intRange(1, 64).fieldOf("required_vertical_space_for_tree").forGetter(RootSystemFeature::requiredVerticalSpaceForTree),
            Codec.intRange(0, 16).fieldOf("level_test_distance").forGetter(RootSystemFeature::levelTestDistance),
            Codec.intRange(0, 64).fieldOf("max_level_deviation").forGetter(RootSystemFeature::maxLevelDeviation),
            Codec.intRange(1, 64).fieldOf("root_radius").forGetter(RootSystemFeature::rootRadius),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("root_replaceable").forGetter(RootSystemFeature::rootReplaceable),
            BlockStateProvider.CODEC.fieldOf("root_state_provider").forGetter(RootSystemFeature::rootStateProvider),
            Codec.intRange(1, 256).fieldOf("root_placement_attempts").forGetter(RootSystemFeature::rootPlacementAttempts),
            Codec.intRange(1, 4096).fieldOf("root_column_max_height").forGetter(RootSystemFeature::rootColumnMaxHeight),
            Codec.intRange(1, 64).fieldOf("hanging_root_radius").forGetter(RootSystemFeature::hangingRootRadius),
            Codec.intRange(1, 16).fieldOf("hanging_roots_vertical_span").forGetter(RootSystemFeature::hangingRootsVerticalSpan),
            BlockStateProvider.CODEC.fieldOf("hanging_root_state_provider").forGetter(RootSystemFeature::hangingRootStateProvider),
            Codec.intRange(1, 256).fieldOf("hanging_root_placement_attempts").forGetter(RootSystemFeature::hangingRootPlacementAttempts),
            Codec.intRange(1, 64).fieldOf("allowed_vertical_water_for_tree").forGetter(RootSystemFeature::allowedVerticalWaterForTree),
            BlockPredicate.CODEC.fieldOf("allowed_tree_position").forGetter(RootSystemFeature::allowedTreePosition)
         )
         .apply(i, RootSystemFeature::new)
   );

   @Override
   public MapCodec<RootSystemFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (!level.getBlockState(origin).isAir()) {
         return false;
      }

      BlockPos.MutableBlockPos workingPos = origin.mutable();
      if (this.placeDirtAndTree(level, chunkGenerator, random, workingPos, origin)) {
         this.placeRoots(level, random, origin, workingPos);
      }

      return true;
   }

   private boolean spaceForTree(final WorldGenLevel level, final BlockPos pos) {
      BlockPos.MutableBlockPos columnUpPos = pos.mutable();

      for (int i = 1; i <= this.requiredVerticalSpaceForTree; i++) {
         columnUpPos.move(Direction.UP);
         BlockState state = level.getBlockState(columnUpPos);
         if (!isAllowedTreeSpace(state, i, this.allowedVerticalWaterForTree)) {
            return false;
         }
      }

      if (this.levelTestDistance > 0) {
         BlockPos.MutableBlockPos cornerPos = pos.mutable();

         for (int i = 0; i < 4; i++) {
            cornerPos.move(Direction.from2DDataValue(i), this.levelTestDistance);
            BlockState below = level.getBlockState(cornerPos.below(this.maxLevelDeviation));
            BlockState above = level.getBlockState(cornerPos.above(this.maxLevelDeviation));
            if (below.isAir() || !above.isAir()) {
               return false;
            }

            cornerPos.set(pos);
         }
      }

      return true;
   }

   private static boolean isAllowedTreeSpace(final BlockState state, final int blocksAboveOrigin, final int allowedVerticalWaterHeight) {
      if (state.isAir()) {
         return true;
      }

      int blocksAboveGround = blocksAboveOrigin + 1;
      return blocksAboveGround <= allowedVerticalWaterHeight && state.getFluidState().is(FluidTags.WATER);
   }

   private boolean placeDirtAndTree(
      final WorldGenLevel level, final ChunkGenerator generator, final RandomSource random, final BlockPos.MutableBlockPos workingPos, final BlockPos pos
   ) {
      for (int y = 0; y < this.rootColumnMaxHeight; y++) {
         workingPos.move(Direction.UP);
         if (level.getHeight(Heightmap.Types.WORLD_SURFACE, workingPos) < workingPos.getY()) {
            return false;
         }

         if (this.allowedTreePosition.test(level, workingPos) && this.spaceForTree(level, workingPos)) {
            BlockPos belowPos = workingPos.below();
            if (level.getFluidState(belowPos).is(FluidTags.LAVA) || !level.getBlockState(belowPos).isSolid()) {
               return false;
            }

            if (this.treeFeature.value().place(level, generator, random, workingPos)) {
               this.placeDirt(pos, pos.getY() + y, level, random);
               return true;
            }
         }
      }

      return false;
   }

   private void placeDirt(final BlockPos origin, final int targetHeight, final WorldGenLevel level, final RandomSource random) {
      int originX = origin.getX();
      int originZ = origin.getZ();
      BlockPos.MutableBlockPos workingPos = origin.mutable();

      for (int y = origin.getY(); y < targetHeight; y++) {
         this.placeRootedDirt(level, random, originX, originZ, workingPos.set(originX, y, originZ));
      }
   }

   private void placeRootedDirt(
      final WorldGenLevel level, final RandomSource random, final int originX, final int originZ, final BlockPos.MutableBlockPos workingPos
   ) {
      for (int i = 0; i < this.rootPlacementAttempts; i++) {
         workingPos.setWithOffset(
            workingPos, random.nextInt(this.rootRadius) - random.nextInt(this.rootRadius), 0, random.nextInt(this.rootRadius) - random.nextInt(this.rootRadius)
         );
         if (level.getBlockState(workingPos).is(this.rootReplaceable)) {
            level.setBlock(workingPos, this.rootStateProvider.getState(level, random, workingPos), 2);
         }

         workingPos.setX(originX);
         workingPos.setZ(originZ);
      }
   }

   private void placeRoots(final WorldGenLevel level, final RandomSource random, final BlockPos pos, final BlockPos.MutableBlockPos workingPos) {
      for (int i = 0; i < this.hangingRootPlacementAttempts; i++) {
         workingPos.setWithOffset(
            pos,
            random.nextInt(this.hangingRootRadius) - random.nextInt(this.hangingRootRadius),
            random.nextInt(this.hangingRootsVerticalSpan) - random.nextInt(this.hangingRootsVerticalSpan),
            random.nextInt(this.hangingRootRadius) - random.nextInt(this.hangingRootRadius)
         );
         if (level.isEmptyBlock(workingPos)) {
            BlockState targetState = this.hangingRootStateProvider.getState(level, random, workingPos);
            if (targetState.canSurvive(level, workingPos) && level.getBlockState(workingPos.above()).isFaceSturdy(level, workingPos, Direction.DOWN)) {
               level.setBlock(workingPos, targetState, 2);
            }
         }
      }
   }
}
