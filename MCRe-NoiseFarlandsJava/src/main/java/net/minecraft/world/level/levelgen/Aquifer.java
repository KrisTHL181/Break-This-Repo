package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.jspecify.annotations.Nullable;

public interface Aquifer {
    // 安全阈值（区块坐标），约 2.147e9 方块，略小于 Integer.MAX_VALUE，能捕获绝大多数溢出
    long SAFE_COORD_LIMIT = 134_217_500L;

    static Aquifer create(
            final NoiseChunk noiseChunk,
            final ChunkPos pos,
            final NoiseRouter router,
            final PositionalRandomFactory positionalRandomFactory,
            final int minBlockY,
            final int yBlockSize,
            final Aquifer.FluidPicker fluidRule) {
        // [检查 1] 工厂方法入口拦截
        if (Math.abs((long) pos.x()) > SAFE_COORD_LIMIT || Math.abs((long) pos.z()) > SAFE_COORD_LIMIT) {
            return Aquifer.createDisabled(fluidRule);
        }
        return new Aquifer.NoiseBasedAquifer(noiseChunk, pos, router, positionalRandomFactory, minBlockY, yBlockSize, fluidRule);
    }

    static Aquifer createDisabled(final Aquifer.FluidPicker fluidRule) {
        return new Aquifer() {
            @Override
            public @Nullable BlockState computeSubstance(final DensityFunction.FunctionContext context, final double density) {
                return density > 0.0 ? null : fluidRule.computeFluid(context.blockX(), context.blockY(), context.blockZ()).at(context.blockY());
            }

            @Override
            public boolean shouldScheduleFluidUpdate() {
                return false;
            }
        };
    }

    @Nullable BlockState computeSubstance(final DensityFunction.FunctionContext context, double density);

    boolean shouldScheduleFluidUpdate();

    interface FluidPicker {
        Aquifer.FluidStatus computeFluid(final int blockX, final int blockY, final int blockZ);
    }

    record FluidStatus(int fluidLevel, BlockState fluidType) {
        public BlockState at(final int blockY) {
            return blockY < this.fluidLevel ? this.fluidType : Blocks.AIR.defaultBlockState();
        }
    }

    class NoiseBasedAquifer implements Aquifer {
        private static final int X_RANGE = 10;
        private static final int Y_RANGE = 9;
        private static final int Z_RANGE = 10;
        private static final int X_SEPARATION = 6;
        private static final int Y_SEPARATION = 3;
        private static final int Z_SEPARATION = 6;
        private static final int X_SPACING = 16;
        private static final int Y_SPACING = 12;
        private static final int Z_SPACING = 16;
        private static final int X_SPACING_SHIFT = 4;
        private static final int Z_SPACING_SHIFT = 4;
        private static final int MAX_REASONABLE_DISTANCE_TO_AQUIFER_CENTER = 11;
        private static final double FLOWING_UPDATE_SIMULARITY = similarity(Mth.square(10), Mth.square(12));
        private static final int SAMPLE_OFFSET_X = -5;
        private static final int SAMPLE_OFFSET_Y = 1;
        private static final int SAMPLE_OFFSET_Z = -5;
        private static final int MIN_CELL_SAMPLE_X = 0;
        private static final int MIN_CELL_SAMPLE_Y = -1;
        private static final int MIN_CELL_SAMPLE_Z = 0;
        private static final int MAX_CELL_SAMPLE_X = 1;
        private static final int MAX_CELL_SAMPLE_Y = 1;
        private static final int MAX_CELL_SAMPLE_Z = 1;
        private final NoiseChunk noiseChunk;
        private final DensityFunction barrierNoise;
        private final DensityFunction fluidLevelFloodednessNoise;
        private final DensityFunction fluidLevelSpreadNoise;
        private final DensityFunction lavaNoise;
        private final PositionalRandomFactory positionalRandomFactory;
        private final Aquifer.@Nullable FluidStatus[] aquiferCache;
        private final BlockPos[] aquiferLocationCache;
        private final Aquifer.FluidPicker globalFluidPicker;
        private final DensityFunction erosion;
        private final DensityFunction depth;
        private boolean shouldScheduleFluidUpdate;
        private final int skipSamplingAboveY;
        private final int minGridX;
        private final int minGridY;
        private final int minGridZ;
        private final int gridSizeX;
        private final int gridSizeZ;
        private static final int[][] SURFACE_SAMPLING_OFFSETS_IN_CHUNKS = new int[][]{
                {0, 0}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {-3, 0}, {-2, 0}, {-1, 0}, {1, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}
        };

        private NoiseBasedAquifer(
                final NoiseChunk noiseChunk,
                final ChunkPos pos,
                final NoiseRouter router,
                final PositionalRandomFactory positionalRandomFactory,
                final int minBlockY,
                final int yBlockSize,
                final Aquifer.FluidPicker globalFluidPicker) {
            // [检查 2] 构造器内双重保险
            if (Math.abs((long) pos.x()) > SAFE_COORD_LIMIT || Math.abs((long) pos.z()) > SAFE_COORD_LIMIT) {
                // 禁用状态：空数组，所有后续计算跳过
                this.aquiferCache = new Aquifer.FluidStatus[0];
                this.aquiferLocationCache = new BlockPos[0];
                this.skipSamplingAboveY = Integer.MAX_VALUE;
                this.minGridX = 0;
                this.minGridY = 0;
                this.minGridZ = 0;
                this.gridSizeX = 0;
                this.gridSizeZ = 0;
                // 赋值以避免空引用（尽管不会被使用）
                this.noiseChunk = noiseChunk;
                this.barrierNoise = router.barrierNoise();
                this.fluidLevelFloodednessNoise = router.fluidLevelFloodednessNoise();
                this.fluidLevelSpreadNoise = router.fluidLevelSpreadNoise();
                this.lavaNoise = router.lavaNoise();
                this.erosion = router.erosion();
                this.depth = router.depth();
                this.positionalRandomFactory = positionalRandomFactory;
                this.globalFluidPicker = globalFluidPicker;
                this.shouldScheduleFluidUpdate = false;
                return;
            }

            // 正常初始化
            this.noiseChunk = noiseChunk;
            this.barrierNoise = router.barrierNoise();
            this.fluidLevelFloodednessNoise = router.fluidLevelFloodednessNoise();
            this.fluidLevelSpreadNoise = router.fluidLevelSpreadNoise();
            this.lavaNoise = router.lavaNoise();
            this.erosion = router.erosion();
            this.depth = router.depth();
            this.positionalRandomFactory = positionalRandomFactory;
            this.minGridX = gridX((int) pos.getMinBlockX() + -5) + 0;
            this.globalFluidPicker = globalFluidPicker;
            int maxGridX = gridX((int) pos.getMaxBlockX() + -5) + 1;
            this.gridSizeX = maxGridX - this.minGridX + 1;
            this.minGridY = gridY(minBlockY + 1) + -1;
            int maxGridY = gridY(minBlockY + yBlockSize + 1) + 1;
            int gridSizeY = maxGridY - this.minGridY + 1;
            this.minGridZ = gridZ((int) pos.getMinBlockZ() + -5) + 0;
            int maxGridZ = gridZ((int) pos.getMaxBlockZ() + -5) + 1;
            this.gridSizeZ = maxGridZ - this.minGridZ + 1;
            int totalGridSize = this.gridSizeX * gridSizeY * this.gridSizeZ;
            this.aquiferCache = new Aquifer.FluidStatus[totalGridSize];
            this.aquiferLocationCache = new BlockPos[totalGridSize];
            int maxAdjustedSurfaceLevel = this.adjustSurfaceLevel(
                    noiseChunk.maxPreliminarySurfaceLevel(fromGridX(this.minGridX, 0), fromGridZ(this.minGridZ, 0), fromGridX(maxGridX, 9), fromGridZ(maxGridZ, 9))
            );
            int skipSamplingAboveGridY = gridY(maxAdjustedSurfaceLevel + 12) - -1;
            this.skipSamplingAboveY = fromGridY(skipSamplingAboveGridY, 11) - 1;
        }

        private boolean isDisabled() {
            return this.aquiferCache.length == 0;
        }

        private int getIndex(final int gridX, final int gridY, final int gridZ) {
            if (this.isDisabled()) return -1;
            int x = gridX - this.minGridX;
            int y = gridY - this.minGridY;
            int z = gridZ - this.minGridZ;
            return (y * this.gridSizeZ + z) * this.gridSizeX + x;
        }

        @Override
        public @Nullable BlockState computeSubstance(final DensityFunction.FunctionContext context, final double density) {
            if (this.isDisabled()) {
                this.shouldScheduleFluidUpdate = false;
                return density > 0.0 ? null : this.globalFluidPicker.computeFluid(context.blockX(), context.blockY(), context.blockZ()).at(context.blockY());
            }

            if (density > 0.0) {
                this.shouldScheduleFluidUpdate = false;
                return null;
            }

            int posX = context.blockX();
            int posY = context.blockY();
            int posZ = context.blockZ();
            Aquifer.FluidStatus globalFluid = this.globalFluidPicker.computeFluid(posX, posY, posZ);
            if (posY > this.skipSamplingAboveY) {
                this.shouldScheduleFluidUpdate = false;
                return globalFluid.at(posY);
            }

            if (globalFluid.at(posY).is(Blocks.LAVA)) {
                this.shouldScheduleFluidUpdate = false;
                return SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : Blocks.LAVA.defaultBlockState();
            }

            int xAnchor = gridX(posX + -5);
            int yAnchor = gridY(posY + 1);
            int zAnchor = gridZ(posZ + -5);
            int distanceSqr1 = Integer.MAX_VALUE;
            int distanceSqr2 = Integer.MAX_VALUE;
            int distanceSqr3 = Integer.MAX_VALUE;
            int distanceSqr4 = Integer.MAX_VALUE;
            int closestIndex1 = 0;
            int closestIndex2 = 0;
            int closestIndex3 = 0;
            int closestIndex4 = 0;

            for (int x1 = 0; x1 <= 1; x1++) {
                for (int y1 = -1; y1 <= 1; y1++) {
                    for (int z1 = 0; z1 <= 1; z1++) {
                        int spacedGridX = xAnchor + x1;
                        int spacedGridY = yAnchor + y1;
                        int spacedGridZ = zAnchor + z1;
                        int index = this.getIndex(spacedGridX, spacedGridY, spacedGridZ);
                        if (index < 0) continue;
                        BlockPos existingLocation = this.aquiferLocationCache[index];
                        BlockPos location;
                        if (existingLocation != null) {
                            location = existingLocation;
                        } else {
                            RandomSource random = this.positionalRandomFactory.at(spacedGridX, spacedGridY, spacedGridZ);
                            location = new BlockPos(
                            fromGridX(spacedGridX, random.nextInt(10)),
                            fromGridY(spacedGridY, random.nextInt(9)),
                            fromGridZ(spacedGridZ, random.nextInt(10))
                            );
                            this.aquiferLocationCache[index] = location;
                        }

                        int dx = location.getX() - posX;
                        int dy = location.getY() - posY;
                        int dz = location.getZ() - posZ;
                        int newDistance = dx * dx + dy * dy + dz * dz;
                        if (distanceSqr1 >= newDistance) {
                            closestIndex4 = closestIndex3;
                            closestIndex3 = closestIndex2;
                            closestIndex2 = closestIndex1;
                            closestIndex1 = index;
                            distanceSqr4 = distanceSqr3;
                            distanceSqr3 = distanceSqr2;
                            distanceSqr2 = distanceSqr1;
                            distanceSqr1 = newDistance;
                        } else if (distanceSqr2 >= newDistance) {
                            closestIndex4 = closestIndex3;
                            closestIndex3 = closestIndex2;
                            closestIndex2 = index;
                            distanceSqr4 = distanceSqr3;
                            distanceSqr3 = distanceSqr2;
                            distanceSqr2 = newDistance;
                        } else if (distanceSqr3 >= newDistance) {
                            closestIndex4 = closestIndex3;
                            closestIndex3 = index;
                            distanceSqr4 = distanceSqr3;
                            distanceSqr3 = newDistance;
                        } else if (distanceSqr4 >= newDistance) {
                            closestIndex4 = index;
                            distanceSqr4 = newDistance;
                        }
                    }
                }
            }

            if (closestIndex1 < 0) {
                this.shouldScheduleFluidUpdate = false;
                return globalFluid.at(posY);
            }

            Aquifer.FluidStatus closestStatus1 = this.getAquiferStatus(closestIndex1);
            double similarity12 = similarity(distanceSqr1, distanceSqr2);
            BlockState fluidState = closestStatus1.at(posY);
            BlockState actualFluidState = SharedConstants.DEBUG_DISABLE_FLUID_GENERATION ? Blocks.AIR.defaultBlockState() : fluidState;
            if (similarity12 <= 0.0) {
                if (similarity12 >= FLOWING_UPDATE_SIMULARITY) {
                    Aquifer.FluidStatus closestStatus2 = this.getAquiferStatus(closestIndex2);
                    this.shouldScheduleFluidUpdate = !closestStatus1.equals(closestStatus2);
                } else {
                    this.shouldScheduleFluidUpdate = false;
                }
                return actualFluidState;
            } else {
                if (fluidState.is(Blocks.WATER) && this.globalFluidPicker.computeFluid(posX, posY - 1, posZ).at(posY - 1).is(Blocks.LAVA)) {
                    this.shouldScheduleFluidUpdate = true;
                    return actualFluidState;
                }

                MutableDouble barrierNoiseValue = new MutableDouble(Double.NaN);
                Aquifer.FluidStatus closestStatus2 = this.getAquiferStatus(closestIndex2);
                double barrier12 = similarity12 * this.calculatePressure(context, barrierNoiseValue, closestStatus1, closestStatus2);
                if (density + barrier12 > 0.0) {
                    this.shouldScheduleFluidUpdate = false;
                    return null;
                }

                Aquifer.FluidStatus closestStatus3 = this.getAquiferStatus(closestIndex3);
                double similarity13 = similarity(distanceSqr1, distanceSqr3);
                if (similarity13 > 0.0) {
                    double barrier13 = similarity12 * similarity13 * this.calculatePressure(context, barrierNoiseValue, closestStatus1, closestStatus3);
                    if (density + barrier13 > 0.0) {
                        this.shouldScheduleFluidUpdate = false;
                        return null;
                    }
                }

                double similarity23 = similarity(distanceSqr2, distanceSqr3);
                if (similarity23 > 0.0) {
                    double barrier23 = similarity12 * similarity23 * this.calculatePressure(context, barrierNoiseValue, closestStatus2, closestStatus3);
                    if (density + barrier23 > 0.0) {
                        this.shouldScheduleFluidUpdate = false;
                        return null;
                    }
                }

                boolean mayFlow12 = !closestStatus1.equals(closestStatus2);
                boolean mayFlow23 = similarity23 >= FLOWING_UPDATE_SIMULARITY && !closestStatus2.equals(closestStatus3);
                boolean mayFlow13 = similarity13 >= FLOWING_UPDATE_SIMULARITY && !closestStatus1.equals(closestStatus3);
                if (!mayFlow12 && !mayFlow23 && !mayFlow13) {
                    this.shouldScheduleFluidUpdate = similarity13 >= FLOWING_UPDATE_SIMULARITY
                            && similarity(distanceSqr1, distanceSqr4) >= FLOWING_UPDATE_SIMULARITY
                            && !closestStatus1.equals(this.getAquiferStatus(closestIndex4));
                } else {
                    this.shouldScheduleFluidUpdate = true;
                }
                return actualFluidState;
            }
        }

        @Override
        public boolean shouldScheduleFluidUpdate() {
            if (this.isDisabled()) return false;
            return this.shouldScheduleFluidUpdate;
        }

        private static double similarity(final int distanceSqr1, final int distanceSqr2) {
            return 1.0 - (distanceSqr2 - distanceSqr1) / 25.0;
        }

        private double calculatePressure(
                final DensityFunction.FunctionContext context,
                final MutableDouble barrierNoiseValue,
                final Aquifer.FluidStatus statusClosest1,
                final Aquifer.FluidStatus statusClosest2) {
            int posY = context.blockY();
            BlockState type1 = statusClosest1.at(posY);
            BlockState type2 = statusClosest2.at(posY);
            if ((!type1.is(Blocks.LAVA) || !type2.is(Blocks.WATER)) && (!type1.is(Blocks.WATER) || !type2.is(Blocks.LAVA))) {
                int fluidYDiff = Math.abs(statusClosest1.fluidLevel - statusClosest2.fluidLevel);
                if (fluidYDiff == 0) return 0.0;
                double averageFluidY = 0.5 * (statusClosest1.fluidLevel + statusClosest2.fluidLevel);
                double howFarAboveAverageFluidPoint = posY + 0.5 - averageFluidY;
                double distanceFromBarrierEdgeTowardsMiddle = (fluidYDiff / 2.0) - Math.abs(howFarAboveAverageFluidPoint);
                double gradient;
                if (howFarAboveAverageFluidPoint > 0.0) {
                    double centerPoint = 0.0 + distanceFromBarrierEdgeTowardsMiddle;
                    gradient = centerPoint > 0.0 ? centerPoint / 1.5 : centerPoint / 2.5;
                } else {
                    double centerPoint = 3.0 + distanceFromBarrierEdgeTowardsMiddle;
                    gradient = centerPoint > 0.0 ? centerPoint / 3.0 : centerPoint / 10.0;
                }
                double noiseValue;
                if (gradient < -2.0 || gradient > 2.0) {
                    noiseValue = 0.0;
                } else {
                    double current = barrierNoiseValue.doubleValue();
                    if (Double.isNaN(current)) {
                        double barrierNoise = this.barrierNoise.compute(context);
                        barrierNoiseValue.setValue(barrierNoise);
                        noiseValue = barrierNoise;
                    } else {
                        noiseValue = current;
                    }
                }
                return 2.0 * (noiseValue + gradient);
            } else {
                return 2.0;
            }
        }

        private static int gridX(final int blockCoord) {
            return blockCoord >> 4;
        }

        private static int fromGridX(final int gridCoord, final int blockOffset) {
            return (gridCoord << 4) + blockOffset;
        }

        private static int gridY(final int blockCoord) {
            return Math.floorDiv(blockCoord, 12);
        }

        private static int fromGridY(final int gridCoord, final int blockOffset) {
            return gridCoord * 12 + blockOffset;
        }

        private static int gridZ(final int blockCoord) {
            return blockCoord >> 4;
        }

        private static int fromGridZ(final int gridCoord, final int blockOffset) {
            return (gridCoord << 4) + blockOffset;
        }

        private Aquifer.FluidStatus getAquiferStatus(final int index) {
            if (index < 0 || this.isDisabled()) {
                // 安全回退：返回一个默认的流体状态（空气）
                return new Aquifer.FluidStatus(DimensionType.WAY_BELOW_MIN_Y, Blocks.AIR.defaultBlockState());
            }
            Aquifer.FluidStatus oldStatus = this.aquiferCache[index];
            if (oldStatus != null) return oldStatus;
            BlockPos location = this.aquiferLocationCache[index];
            Aquifer.FluidStatus status = this.computeFluid(location.getX(), location.getY(), location.getZ());
            this.aquiferCache[index] = status;
            return status;
        }

        private Aquifer.FluidStatus computeFluid(final int x, final int y, final int z) {
            Aquifer.FluidStatus globalFluid = this.globalFluidPicker.computeFluid(x, y, z);
            int lowestPreliminarySurface = Integer.MAX_VALUE;
            int topOfAquiferCell = y + 12;
            int bottomOfAquiferCell = y - 12;
            boolean surfaceAtCenterIsUnderGlobalFluidLevel = false;

            for (int[] offset : SURFACE_SAMPLING_OFFSETS_IN_CHUNKS) {
                int sampleX = x + SectionPos.sectionToBlockCoord(offset[0]);
                int sampleZ = z + SectionPos.sectionToBlockCoord(offset[1]);
                int preliminarySurfaceLevel = this.noiseChunk.preliminarySurfaceLevel(sampleX, sampleZ);
                int adjustedSurfaceLevel = this.adjustSurfaceLevel(preliminarySurfaceLevel);
                boolean start = offset[0] == 0 && offset[1] == 0;
                if (start && bottomOfAquiferCell > adjustedSurfaceLevel) {
                    return globalFluid;
                }
                boolean topPokesAbove = topOfAquiferCell > adjustedSurfaceLevel;
                if (topPokesAbove || start) {
                    Aquifer.FluidStatus surfaceFluid = this.globalFluidPicker.computeFluid(sampleX, adjustedSurfaceLevel, sampleZ);
                    if (!surfaceFluid.at(adjustedSurfaceLevel).isAir()) {
                        if (start) surfaceAtCenterIsUnderGlobalFluidLevel = true;
                        if (topPokesAbove) return surfaceFluid;
                    }
                }
                lowestPreliminarySurface = Math.min(lowestPreliminarySurface, preliminarySurfaceLevel);
            }

            int fluidSurfaceLevel = this.computeSurfaceLevel(x, y, z, globalFluid, lowestPreliminarySurface, surfaceAtCenterIsUnderGlobalFluidLevel);
            return new Aquifer.FluidStatus(fluidSurfaceLevel, this.computeFluidType(x, y, z, globalFluid, fluidSurfaceLevel));
        }

        private int adjustSurfaceLevel(final int preliminarySurfaceLevel) {
            return preliminarySurfaceLevel + 8;
        }

        private int computeSurfaceLevel(
                final int x,
                final int y,
                final int z,
                final Aquifer.FluidStatus globalFluid,
                final int lowestPreliminarySurface,
                final boolean surfaceAtCenterIsUnderGlobalFluidLevel) {
            DensityFunction.SinglePointContext context = new DensityFunction.SinglePointContext(x, y, z);
            double partiallyFloodedness;
            double fullyFloodidness;
            if (OverworldBiomeBuilder.isDeepDarkRegion(this.erosion, this.depth, context)) {
                partiallyFloodedness = -1.0;
                fullyFloodidness = -1.0;
            } else {
                int distanceBelowSurface = lowestPreliminarySurface + 8 - y;
                double floodednessFactor = surfaceAtCenterIsUnderGlobalFluidLevel ? Mth.clampedMap(distanceBelowSurface, 0.0, 64.0, 1.0, 0.0) : 0.0;
                double floodednessNoiseValue = Mth.clamp(this.fluidLevelFloodednessNoise.compute(context), -1.0, 1.0);
                double fullyFloodedThreshold = Mth.map(floodednessFactor, 1.0, 0.0, -0.3, 0.8);
                double partiallyFloodedThreshold = Mth.map(floodednessFactor, 1.0, 0.0, -0.8, 0.4);
                partiallyFloodedness = floodednessNoiseValue - partiallyFloodedThreshold;
                fullyFloodidness = floodednessNoiseValue - fullyFloodedThreshold;
            }

            int fluidSurfaceLevel;
            if (fullyFloodidness > 0.0) {
                fluidSurfaceLevel = globalFluid.fluidLevel;
            } else if (partiallyFloodedness > 0.0) {
                fluidSurfaceLevel = this.computeRandomizedFluidSurfaceLevel(x, y, z, lowestPreliminarySurface);
            } else {
                fluidSurfaceLevel = DimensionType.WAY_BELOW_MIN_Y;
            }
            return fluidSurfaceLevel;
        }

        private int computeRandomizedFluidSurfaceLevel(final int x, final int y, final int z, final int lowestPreliminarySurface) {
            int fluidLevelCellX = Math.floorDiv(x, 16);
            int fluidLevelCellY = Math.floorDiv(y, 40);
            int fluidLevelCellZ = Math.floorDiv(z, 16);
            int fluidCellMiddleY = fluidLevelCellY * 40 + 20;
            double fluidLevelSpread = this.fluidLevelSpreadNoise
                            .compute(new DensityFunction.SinglePointContext(fluidLevelCellX, fluidLevelCellY, fluidLevelCellZ))
                    * 10.0;
            int fluidLevelSpreadQuantized = Mth.quantize(fluidLevelSpread, 3);
            int targetFluidSurfaceLevel = fluidCellMiddleY + fluidLevelSpreadQuantized;
            return Math.min(lowestPreliminarySurface, targetFluidSurfaceLevel);
        }

        private BlockState computeFluidType(final int x, final int y, final int z, final Aquifer.FluidStatus globalFluid, final int fluidSurfaceLevel) {
            BlockState fluidType = globalFluid.fluidType;
            if (fluidSurfaceLevel <= -10 && fluidSurfaceLevel != DimensionType.WAY_BELOW_MIN_Y && globalFluid.fluidType != Blocks.LAVA.defaultBlockState()) {
                int fluidTypeCellX = Math.floorDiv(x, 64);
                int fluidTypeCellY = Math.floorDiv(y, 40);
                int fluidTypeCellZ = Math.floorDiv(z, 64);
                double lavaNoiseValue = this.lavaNoise.compute(new DensityFunction.SinglePointContext(fluidTypeCellX, fluidTypeCellY, fluidTypeCellZ));
                if (Math.abs(lavaNoiseValue) > 0.3) {
                    fluidType = Blocks.LAVA.defaultBlockState();
                }
            }
            return fluidType;
        }
    }
}