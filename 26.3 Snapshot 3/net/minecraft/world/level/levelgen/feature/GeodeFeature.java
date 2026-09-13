package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;

public record GeodeFeature(
   GeodeBlockSettings blockSettings,
   GeodeLayerSettings layerSettings,
   GeodeCrackSettings crackSettings,
   double usePotentialPlacementsChance,
   double useAlternateLayer0Chance,
   boolean placementsRequireLayer0Alternate,
   IntProvider outerWallDistance,
   IntProvider distributionPoints,
   IntProvider pointOffset,
   int minGenOffset,
   int maxGenOffset,
   double noiseMultiplier,
   int invalidBlocksThreshold
) implements Feature {
   public static final Codec<Double> CHANCE_RANGE = Codec.doubleRange(0.0, 1.0);
   public static final MapCodec<GeodeFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            GeodeBlockSettings.CODEC.fieldOf("blocks").forGetter(GeodeFeature::blockSettings),
            GeodeLayerSettings.CODEC.fieldOf("layers").forGetter(GeodeFeature::layerSettings),
            GeodeCrackSettings.CODEC.fieldOf("crack").forGetter(GeodeFeature::crackSettings),
            CHANCE_RANGE.optionalFieldOf("use_potential_placements_chance", 0.35).forGetter(GeodeFeature::usePotentialPlacementsChance),
            CHANCE_RANGE.optionalFieldOf("use_alternate_layer0_chance", 0.0).forGetter(GeodeFeature::useAlternateLayer0Chance),
            Codec.BOOL.optionalFieldOf("placements_require_layer0_alternate", true).forGetter(GeodeFeature::placementsRequireLayer0Alternate),
            IntProviders.codec(1, 20).optionalFieldOf("outer_wall_distance", UniformInt.of(4, 5)).forGetter(GeodeFeature::outerWallDistance),
            IntProviders.codec(1, 20).optionalFieldOf("distribution_points", UniformInt.of(3, 4)).forGetter(GeodeFeature::distributionPoints),
            IntProviders.codec(0, 10).optionalFieldOf("point_offset", UniformInt.of(1, 2)).forGetter(GeodeFeature::pointOffset),
            Codec.INT.optionalFieldOf("min_gen_offset", -16).forGetter(GeodeFeature::minGenOffset),
            Codec.INT.optionalFieldOf("max_gen_offset", 16).forGetter(GeodeFeature::maxGenOffset),
            CHANCE_RANGE.optionalFieldOf("noise_multiplier", 0.05).forGetter(GeodeFeature::noiseMultiplier),
            Codec.INT.fieldOf("invalid_blocks_threshold").forGetter(GeodeFeature::invalidBlocksThreshold)
         )
         .apply(i, GeodeFeature::new)
   );
   private static final Direction[] DIRECTIONS = Direction.values();

   @Override
   public MapCodec<GeodeFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      List<Pair<BlockPos, Integer>> points = Lists.newLinkedList();
      int numPoints = this.distributionPoints.sample(random);
      WorldgenRandom random1 = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));
      NormalNoise noise = NormalNoise.create(random1, -4, 1.0);
      List<BlockPos> crackPoints = Lists.newLinkedList();
      double crackSizeAdjustment = (double)numPoints / this.outerWallDistance.maxInclusive();
      double innerAir = 1.0 / Math.sqrt(this.layerSettings.filling);
      double innermostBlockLayer = 1.0 / Math.sqrt(this.layerSettings.innerLayer + crackSizeAdjustment);
      double innerCrust = 1.0 / Math.sqrt(this.layerSettings.middleLayer + crackSizeAdjustment);
      double outerCrust = 1.0 / Math.sqrt(this.layerSettings.outerLayer + crackSizeAdjustment);
      double crackSize = 1.0 / Math.sqrt(this.crackSettings.baseCrackSize + random.nextDouble() / 2.0 + (numPoints > 3 ? crackSizeAdjustment : 0.0));
      boolean shouldGenerateCrack = random.nextFloat() < this.crackSettings.generateCrackChance;
      int numInvalidPoints = 0;

      for (int i = 0; i < numPoints; i++) {
         int x = this.outerWallDistance.sample(random);
         int y = this.outerWallDistance.sample(random);
         int z = this.outerWallDistance.sample(random);
         BlockPos pos = origin.offset(x, y, z);
         BlockState state = level.getBlockState(pos);
         if (state.isAir() || state.is(this.blockSettings.invalidBlocks())) {
            if (++numInvalidPoints > this.invalidBlocksThreshold) {
               return false;
            }
         }

         points.add(Pair.of(pos, this.pointOffset.sample(random)));
      }

      if (shouldGenerateCrack) {
         int offsetIndex = random.nextInt(4);
         int crackOffset = numPoints * 2 + 1;
         if (offsetIndex == 0) {
            crackPoints.add(origin.offset(crackOffset, 7, 0));
            crackPoints.add(origin.offset(crackOffset, 5, 0));
            crackPoints.add(origin.offset(crackOffset, 1, 0));
         } else if (offsetIndex == 1) {
            crackPoints.add(origin.offset(0, 7, crackOffset));
            crackPoints.add(origin.offset(0, 5, crackOffset));
            crackPoints.add(origin.offset(0, 1, crackOffset));
         } else if (offsetIndex == 2) {
            crackPoints.add(origin.offset(crackOffset, 7, crackOffset));
            crackPoints.add(origin.offset(crackOffset, 5, crackOffset));
            crackPoints.add(origin.offset(crackOffset, 1, crackOffset));
         } else {
            crackPoints.add(origin.offset(0, 7, 0));
            crackPoints.add(origin.offset(0, 5, 0));
            crackPoints.add(origin.offset(0, 1, 0));
         }
      }

      List<BlockPos> potentialCrystalPlacements = Lists.newArrayList();
      HolderSet<Block> cantReplace = this.blockSettings.cannotReplace();
      Predicate<BlockState> canReplace = s -> !s.is(cantReplace);

      for (BlockPos pointInside : BlockPos.betweenClosed(
         origin.offset(this.minGenOffset, this.minGenOffset, this.minGenOffset), origin.offset(this.maxGenOffset, this.maxGenOffset, this.maxGenOffset)
      )) {
         double noiseOffset = noise.getValue(pointInside.getX(), pointInside.getY(), pointInside.getZ()) * this.noiseMultiplier;
         double distSumShell = 0.0;

         for (Pair<BlockPos, Integer> point : points) {
            distSumShell += Mth.invSqrt(pointInside.distSqr((Vec3i)point.getFirst()) + ((Integer)point.getSecond()).intValue()) + noiseOffset;
         }

         if (!(distSumShell < outerCrust)) {
            if (distSumShell >= innerAir) {
               this.safeSetBlock(level, pointInside, this.blockSettings.fillingProvider().getState(level, random, pointInside), canReplace);
            } else {
               double distSumCrack = 0.0;

               for (BlockPos point : crackPoints) {
                  distSumCrack += Mth.invSqrt(pointInside.distSqr(point) + this.crackSettings.crackPointOffset) + noiseOffset;
               }

               if (shouldGenerateCrack && distSumCrack >= crackSize) {
                  this.safeSetBlock(level, pointInside, Blocks.AIR.defaultBlockState(), canReplace);

                  for (Direction direction : DIRECTIONS) {
                     BlockPos adjacentPos = pointInside.relative(direction);
                     FluidState adjacentFluidState = level.getFluidState(adjacentPos);
                     if (!adjacentFluidState.isEmpty()) {
                        level.scheduleTick(adjacentPos, adjacentFluidState.getType(), 0);
                     }
                  }
               } else if (distSumShell >= innermostBlockLayer) {
                  boolean useAlternateLayer = random.nextFloat() < this.useAlternateLayer0Chance;
                  if (useAlternateLayer) {
                     this.safeSetBlock(level, pointInside, this.blockSettings.alternateInnerLayerProvider().getState(level, random, pointInside), canReplace);
                  } else {
                     this.safeSetBlock(level, pointInside, this.blockSettings.innerLayerProvider().getState(level, random, pointInside), canReplace);
                  }

                  if ((!this.placementsRequireLayer0Alternate || useAlternateLayer) && random.nextFloat() < this.usePotentialPlacementsChance) {
                     potentialCrystalPlacements.add(pointInside.immutable());
                  }
               } else if (distSumShell >= innerCrust) {
                  this.safeSetBlock(level, pointInside, this.blockSettings.middleLayerProvider().getState(level, random, pointInside), canReplace);
               } else if (distSumShell >= outerCrust) {
                  this.safeSetBlock(level, pointInside, this.blockSettings.outerLayerProvider().getState(level, random, pointInside), canReplace);
               }
            }
         }
      }

      List<BlockState> innerPlacements = this.blockSettings.innerPlacements();

      for (BlockPos crystalPos : potentialCrystalPlacements) {
         BlockState blockState = Util.getRandom(innerPlacements, random);

         for (Direction direction : DIRECTIONS) {
            if (blockState.hasProperty(BlockStateProperties.FACING)) {
               blockState = blockState.setValue(BlockStateProperties.FACING, direction);
            }

            BlockPos placePos = crystalPos.relative(direction);
            BlockState placeState = level.getBlockState(placePos);
            if (blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
               blockState = blockState.setValue(BlockStateProperties.WATERLOGGED, placeState.getFluidState().isSource());
            }

            if (BuddingAmethystBlock.canClusterGrowAtState(placeState)) {
               this.safeSetBlock(level, placePos, blockState, canReplace);
               break;
            }
         }
      }

      return true;
   }
}
