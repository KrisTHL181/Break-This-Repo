package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.GeodeBlockSettings;
import net.minecraft.world.level.levelgen.GeodeCrackSettings;
import net.minecraft.world.level.levelgen.GeodeLayerSettings;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.configurations.GeodeConfiguration;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.material.FluidState;

public class GeodeFeature extends Feature<GeodeConfiguration> {
    private static final Direction[] DIRECTIONS = Direction.values();

    public GeodeFeature(final Codec<GeodeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<GeodeConfiguration> context) {
        GeodeConfiguration config = context.config();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        WorldGenLevel level = context.level();
        int minGenOffset = config.minGenOffset();
        int maxGenOffset = config.maxGenOffset();
        List<Pair<BlockPos, Integer>> points = Lists.newLinkedList();
        int numPoints = config.distributionPoints().sample(random);
        WorldgenRandom random1 = new WorldgenRandom(new LegacyRandomSource(level.getSeed()));
        NormalNoise noise = NormalNoise.create(random1, -4, 1.0);
        List<BlockPos> crackPoints = Lists.newLinkedList();
        double crackSizeAdjustment = (double)numPoints / config.outerWallDistance().maxInclusive();
        GeodeLayerSettings layerSettings = config.geodeLayerSettings();
        GeodeBlockSettings blockSettings = config.geodeBlockSettings();
        GeodeCrackSettings crackSettings = config.geodeCrackSettings();
        double innerAir = 1.0 / Math.sqrt(layerSettings.filling);
        double innermostBlockLayer = 1.0 / Math.sqrt(layerSettings.innerLayer + crackSizeAdjustment);
        double innerCrust = 1.0 / Math.sqrt(layerSettings.middleLayer + crackSizeAdjustment);
        double outerCrust = 1.0 / Math.sqrt(layerSettings.outerLayer + crackSizeAdjustment);
        double crackSize = 1.0 / Math.sqrt(crackSettings.baseCrackSize + random.nextDouble() / 2.0 + (numPoints > 3 ? crackSizeAdjustment : 0.0));
        boolean shouldGenerateCrack = random.nextFloat() < crackSettings.generateCrackChance;
        int numInvalidPoints = 0;

        for (int i = 0; i < numPoints; i++) {
            int x = config.outerWallDistance().sample(random);
            int y = config.outerWallDistance().sample(random);
            int z = config.outerWallDistance().sample(random);
            BlockPos pos = origin.offset(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(blockSettings.invalidBlocks())) {
                if (++numInvalidPoints > config.invalidBlocksThreshold()) {
                    return false;
                }
            }

            points.add(Pair.of(pos, config.pointOffset().sample(random)));
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
        HolderSet<Block> cantReplace = config.geodeBlockSettings().cannotReplace();
        Predicate<BlockState> canReplace = s -> !s.is(cantReplace);

        for (BlockPos pointInside : BlockPos.betweenClosed(
            origin.offset(minGenOffset, minGenOffset, minGenOffset), origin.offset(maxGenOffset, maxGenOffset, maxGenOffset)
        )) {
            double noiseOffset = noise.getValue(pointInside.getX(), pointInside.getY(), pointInside.getZ()) * config.noiseMultiplier();
            double distSumShell = 0.0;
            double distSumCrack = 0.0;

            for (Pair<BlockPos, Integer> point : points) {
                distSumShell += Mth.invSqrt(pointInside.distSqr(point.getFirst()) + point.getSecond().intValue()) + noiseOffset;
            }

            for (BlockPos point : crackPoints) {
                distSumCrack += Mth.invSqrt(pointInside.distSqr(point) + crackSettings.crackPointOffset) + noiseOffset;
            }

            if (!(distSumShell < outerCrust)) {
                if (shouldGenerateCrack && distSumCrack >= crackSize && distSumShell < innerAir) {
                    this.safeSetBlock(level, pointInside, Blocks.AIR.defaultBlockState(), canReplace);

                    for (Direction direction : DIRECTIONS) {
                        BlockPos adjacentPos = pointInside.relative(direction);
                        FluidState adjacentFluidState = level.getFluidState(adjacentPos);
                        if (!adjacentFluidState.isEmpty()) {
                            level.scheduleTick(adjacentPos, adjacentFluidState.getType(), 0);
                        }
                    }
                } else if (distSumShell >= innerAir) {
                    this.safeSetBlock(level, pointInside, blockSettings.fillingProvider().getState(level, random, pointInside), canReplace);
                } else if (distSumShell >= innermostBlockLayer) {
                    boolean useAlternateLayer = random.nextFloat() < config.useAlternateLayer0Chance();
                    if (useAlternateLayer) {
                        this.safeSetBlock(level, pointInside, blockSettings.alternateInnerLayerProvider().getState(level, random, pointInside), canReplace);
                    } else {
                        this.safeSetBlock(level, pointInside, blockSettings.innerLayerProvider().getState(level, random, pointInside), canReplace);
                    }

                    if ((!config.placementsRequireLayer0Alternate() || useAlternateLayer) && random.nextFloat() < config.usePotentialPlacementsChance()) {
                        potentialCrystalPlacements.add(pointInside.immutable());
                    }
                } else if (distSumShell >= innerCrust) {
                    this.safeSetBlock(level, pointInside, blockSettings.middleLayerProvider().getState(level, random, pointInside), canReplace);
                } else if (distSumShell >= outerCrust) {
                    this.safeSetBlock(level, pointInside, blockSettings.outerLayerProvider().getState(level, random, pointInside), canReplace);
                }
            }
        }

        List<BlockState> innerPlacements = blockSettings.innerPlacements();

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