package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.util.valueproviders.FloatProviders;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Column;

public record SpeleothemClusterFeature(
   BlockState baseBlock,
   BlockState pointedBlock,
   HolderSet<Block> replaceableBlocks,
   int floorToCeilingSearchRange,
   IntProvider height,
   IntProvider radius,
   int maxStalagmiteStalactiteHeightDiff,
   int heightDeviation,
   IntProvider speleothemBlockLayerThickness,
   FloatProvider density,
   FloatProvider wetness,
   float chanceOfSpeleothemAtMaxDistanceFromCenter,
   int maxDistanceFromEdgeAffectingChanceOfSpeleothem,
   int maxDistanceFromCenterAffectingHeightBias
) implements Feature {
   public static final MapCodec<SpeleothemClusterFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockState.CODEC.fieldOf("base_block").forGetter(SpeleothemClusterFeature::baseBlock),
            BlockState.CODEC.fieldOf("pointed_block").forGetter(SpeleothemClusterFeature::pointedBlock),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("replaceable_blocks").forGetter(SpeleothemClusterFeature::replaceableBlocks),
            Codec.intRange(1, 512).fieldOf("floor_to_ceiling_search_range").forGetter(SpeleothemClusterFeature::floorToCeilingSearchRange),
            IntProviders.codec(1, 128).fieldOf("height").forGetter(SpeleothemClusterFeature::height),
            IntProviders.codec(1, 128).fieldOf("radius").forGetter(SpeleothemClusterFeature::radius),
            Codec.intRange(0, 64).fieldOf("max_stalagmite_stalactite_height_diff").forGetter(SpeleothemClusterFeature::maxStalagmiteStalactiteHeightDiff),
            Codec.intRange(1, 64).fieldOf("height_deviation").forGetter(SpeleothemClusterFeature::heightDeviation),
            IntProviders.codec(0, 128).fieldOf("speleothem_block_layer_thickness").forGetter(SpeleothemClusterFeature::speleothemBlockLayerThickness),
            FloatProviders.codec(0.0F, 2.0F).fieldOf("density").forGetter(SpeleothemClusterFeature::density),
            FloatProviders.codec(0.0F, 2.0F).fieldOf("wetness").forGetter(SpeleothemClusterFeature::wetness),
            Codec.floatRange(0.0F, 1.0F)
               .fieldOf("chance_of_speleothem_at_max_distance_from_center")
               .forGetter(SpeleothemClusterFeature::chanceOfSpeleothemAtMaxDistanceFromCenter),
            Codec.intRange(1, 64)
               .fieldOf("max_distance_from_edge_affecting_chance_of_speleothem")
               .forGetter(SpeleothemClusterFeature::maxDistanceFromEdgeAffectingChanceOfSpeleothem),
            Codec.intRange(1, 64)
               .fieldOf("max_distance_from_center_affecting_height_bias")
               .forGetter(SpeleothemClusterFeature::maxDistanceFromCenterAffectingHeightBias)
         )
         .apply(i, SpeleothemClusterFeature::new)
   );

   @Override
   public MapCodec<SpeleothemClusterFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (!SpeleothemUtils.isEmptyOrWater(level, origin)) {
         return false;
      }

      int height = this.height.sample(random);
      float wetness = this.wetness.sample(random);
      float density = this.density.sample(random);
      int xRadius = this.radius.sample(random);
      int zRadius = this.radius.sample(random);

      for (int dx = -xRadius; dx <= xRadius; dx++) {
         for (int dz = -zRadius; dz <= zRadius; dz++) {
            double chanceOfStalagmiteOrStalactite = this.getChanceOfStalagmiteOrStalactite(xRadius, zRadius, dx, dz);
            BlockPos pos = origin.offset(dx, 0, dz);
            this.placeColumn(level, random, pos, dx, dz, wetness, chanceOfStalagmiteOrStalactite, height, density);
         }
      }

      return true;
   }

   private void placeColumn(
      final WorldGenLevel level,
      final RandomSource random,
      final BlockPos pos,
      final int dx,
      final int dz,
      final float chanceOfWater,
      final double chanceOfStalagmiteOrStalactite,
      final int clusterHeight,
      final float density
   ) {
      Optional<Column> baseColumn = Column.scan(
         level, pos, this.floorToCeilingSearchRange, SpeleothemUtils::isEmptyOrWater, SpeleothemUtils::isNeitherEmptyNorWater
      );
      if (!baseColumn.isEmpty()) {
         OptionalInt ceiling = baseColumn.get().getCeiling();
         OptionalInt baseFloor = baseColumn.get().getFloor();
         if (!ceiling.isEmpty() || !baseFloor.isEmpty()) {
            boolean wantPool = random.nextFloat() < chanceOfWater;
            Column column;
            if (wantPool && baseFloor.isPresent() && this.canPlacePool(level, pos.atY(baseFloor.getAsInt()))) {
               int baseFloorY = baseFloor.getAsInt();
               column = baseColumn.get().withFloor(OptionalInt.of(baseFloorY - 1));
               level.setBlock(pos.atY(baseFloorY), Blocks.WATER.defaultBlockState(), 2);
            } else {
               column = baseColumn.get();
            }

            OptionalInt floor = column.getFloor();
            boolean wantStalactite = random.nextDouble() < chanceOfStalagmiteOrStalactite;
            int stalactiteHeight;
            if (ceiling.isPresent() && wantStalactite && !this.isLava(level, pos.atY(ceiling.getAsInt()))) {
               int ceilingThickness = this.speleothemBlockLayerThickness.sample(random);
               this.replaceBlocksWithBaseBlocks(level, pos.atY(ceiling.getAsInt()), ceilingThickness, Direction.UP);
               int maxHeightForThisColumn;
               if (floor.isPresent()) {
                  maxHeightForThisColumn = Math.min(clusterHeight, ceiling.getAsInt() - floor.getAsInt());
               } else {
                  maxHeightForThisColumn = clusterHeight;
               }

               stalactiteHeight = this.getSpeleothemHeight(random, dx, dz, density, maxHeightForThisColumn);
            } else {
               stalactiteHeight = 0;
            }

            boolean wantStalagmite = random.nextDouble() < chanceOfStalagmiteOrStalactite;
            int stalagmiteHeight;
            if (floor.isPresent() && wantStalagmite && !this.isLava(level, pos.atY(floor.getAsInt()))) {
               int floorThickness = this.speleothemBlockLayerThickness.sample(random);
               this.replaceBlocksWithBaseBlocks(level, pos.atY(floor.getAsInt()), floorThickness, Direction.DOWN);
               if (ceiling.isPresent()) {
                  stalagmiteHeight = Math.max(
                     0, stalactiteHeight + Mth.randomBetweenInclusive(random, -this.maxStalagmiteStalactiteHeightDiff, this.maxStalagmiteStalactiteHeightDiff)
                  );
               } else {
                  stalagmiteHeight = this.getSpeleothemHeight(random, dx, dz, density, clusterHeight);
               }
            } else {
               stalagmiteHeight = 0;
            }

            int actualStalagmiteHeight;
            int actualStalactiteHeight;
            if (ceiling.isPresent() && floor.isPresent() && ceiling.getAsInt() - stalactiteHeight <= floor.getAsInt() + stalagmiteHeight) {
               int floorY = floor.getAsInt();
               int ceilingY = ceiling.getAsInt();
               int lowestStalactiteBottom = Math.max(ceilingY - stalactiteHeight, floorY + 1);
               int highestStalagmiteTop = Math.min(floorY + stalagmiteHeight, ceilingY - 1);
               int actualStalactiteBottom = Mth.randomBetweenInclusive(random, lowestStalactiteBottom, highestStalagmiteTop + 1);
               int actualStalagmiteTop = actualStalactiteBottom - 1;
               actualStalactiteHeight = ceilingY - actualStalactiteBottom;
               actualStalagmiteHeight = actualStalagmiteTop - floorY;
            } else {
               actualStalactiteHeight = stalactiteHeight;
               actualStalagmiteHeight = stalagmiteHeight;
            }

            boolean mergeTips = random.nextBoolean()
               && actualStalactiteHeight > 0
               && actualStalagmiteHeight > 0
               && column.getHeight().isPresent()
               && actualStalactiteHeight + actualStalagmiteHeight == column.getHeight().getAsInt();
            if (ceiling.isPresent()) {
               SpeleothemUtils.growSpeleothem(
                  level,
                  pos.atY(ceiling.getAsInt() - 1),
                  Direction.DOWN,
                  actualStalactiteHeight,
                  mergeTips,
                  this.baseBlock.getBlock(),
                  this.pointedBlock.getBlock(),
                  this.replaceableBlocks
               );
            }

            if (floor.isPresent()) {
               SpeleothemUtils.growSpeleothem(
                  level,
                  pos.atY(floor.getAsInt() + 1),
                  Direction.UP,
                  actualStalagmiteHeight,
                  mergeTips,
                  this.baseBlock.getBlock(),
                  this.pointedBlock.getBlock(),
                  this.replaceableBlocks
               );
            }
         }
      }
   }

   private boolean isLava(final LevelReader level, final BlockPos pos) {
      return level.getBlockState(pos).is(Blocks.LAVA);
   }

   private int getSpeleothemHeight(final RandomSource random, final int dx, final int dz, final float density, final int maxHeight) {
      if (random.nextFloat() > density) {
         return 0;
      }

      int distanceFromCenter = Math.abs(dx) + Math.abs(dz);
      float heightMean = (float)Mth.clampedMap(distanceFromCenter, 0.0, this.maxDistanceFromCenterAffectingHeightBias, maxHeight / 2.0, 0.0);
      return (int)randomBetweenBiased(random, 0.0F, maxHeight, heightMean, this.heightDeviation);
   }

   private boolean canPlacePool(final WorldGenLevel level, final BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (!state.is(Blocks.WATER) && !state.is(this.baseBlock.getBlock()) && !state.is(this.pointedBlock.getBlock())) {
         if (level.getBlockState(pos.above()).getFluidState().is(FluidTags.WATER)) {
            return false;
         }

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!this.canBeAdjacentToWater(level, pos.relative(direction))) {
               return false;
            }
         }

         return this.canBeAdjacentToWater(level, pos.below());
      } else {
         return false;
      }
   }

   private boolean canBeAdjacentToWater(final LevelAccessor level, final BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.getFluidState().is(FluidTags.WATER);
   }

   private void replaceBlocksWithBaseBlocks(final WorldGenLevel level, final BlockPos firstPos, final int maxCount, final Direction direction) {
      BlockPos.MutableBlockPos pos = firstPos.mutable();

      for (int i = 0; i < maxCount; i++) {
         if (!SpeleothemUtils.placeBaseBlockIfPossible(level, pos, this.baseBlock.getBlock(), this.replaceableBlocks)) {
            return;
         }

         pos.move(direction);
      }
   }

   private double getChanceOfStalagmiteOrStalactite(final int xRadius, final int zRadius, final int dx, final int dz) {
      int xDistanceFromEdge = xRadius - Math.abs(dx);
      int zDistanceFromEdge = zRadius - Math.abs(dz);
      int distanceFromEdge = Math.min(xDistanceFromEdge, zDistanceFromEdge);
      return Mth.clampedMap(distanceFromEdge, 0.0F, this.maxDistanceFromEdgeAffectingChanceOfSpeleothem, this.chanceOfSpeleothemAtMaxDistanceFromCenter, 1.0F);
   }

   private static float randomBetweenBiased(final RandomSource random, final float min, final float maxExclusive, final float mean, final float deviation) {
      return ClampedNormalFloat.sample(random, mean, deviation, min, maxExclusive);
   }
}
