package net.minecraft.world.level.levelgen.blending;

import com.google.common.primitives.Doubles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrays;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

public class BlendingData {
    private static final double BLENDING_DENSITY_FACTOR = 0.1;
    protected static final int CELL_WIDTH = 4;
    protected static final int CELL_HEIGHT = 8;
    protected static final int CELL_RATIO = 2;
    private static final double SOLID_DENSITY = 1.0;
    private static final double AIR_DENSITY = -1.0;
    private static final int CELLS_PER_SECTION_Y = 2;
    private static final int QUARTS_PER_SECTION = QuartPos.fromBlock(16);
    private static final int CELL_HORIZONTAL_MAX_INDEX_INSIDE = QUARTS_PER_SECTION - 1;
    private static final int CELL_HORIZONTAL_MAX_INDEX_OUTSIDE = QUARTS_PER_SECTION;
    private static final int CELL_COLUMN_INSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_INSIDE + 1;
    private static final int CELL_COLUMN_OUTSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_OUTSIDE + 1;
    private static final int CELL_COLUMN_COUNT = CELL_COLUMN_INSIDE_COUNT + CELL_COLUMN_OUTSIDE_COUNT;
    private final LevelHeightAccessor areaWithOldGeneration;
    private static final List<Block> SURFACE_BLOCKS = List.of(
        Blocks.PODZOL,
        Blocks.GRAVEL,
        Blocks.GRASS_BLOCK,
        Blocks.STONE,
        Blocks.COARSE_DIRT,
        Blocks.SAND,
        Blocks.RED_SAND,
        Blocks.MYCELIUM,
        Blocks.SNOW_BLOCK,
        Blocks.TERRACOTTA,
        Blocks.DIRT
    );
    protected static final double NO_VALUE = Double.MAX_VALUE;
    private boolean hasCalculatedData;
    private final double[] heights;
    private final List<@Nullable List<@Nullable Holder<Biome>>> biomes;
    private final transient double[][] densities;

    private BlendingData(final int minSection, final int maxSection, final Optional<double[]> heights) {
        this.heights = heights.orElseGet(() -> Util.make(new double[CELL_COLUMN_COUNT], i -> Arrays.fill(i, Double.MAX_VALUE)));
        this.densities = new double[CELL_COLUMN_COUNT][];
        ObjectArrayList<List<Holder<Biome>>> biomes = new ObjectArrayList<>(CELL_COLUMN_COUNT);
        biomes.size(CELL_COLUMN_COUNT);
        this.biomes = biomes;
        int minY = SectionPos.sectionToBlockCoord(minSection);
        int height = SectionPos.sectionToBlockCoord(maxSection) - minY;
        this.areaWithOldGeneration = LevelHeightAccessor.create(minY, height);
    }

    public static @Nullable BlendingData unpack(final BlendingData.@Nullable Packed packed) {
        return packed == null ? null : new BlendingData(packed.minSection(), packed.maxSection(), packed.heights());
    }

    public BlendingData.Packed pack() {
        boolean hasHeight = false;

        for (double height : this.heights) {
            if (height != Double.MAX_VALUE) {
                hasHeight = true;
                break;
            }
        }

        return new BlendingData.Packed(
            this.areaWithOldGeneration.getMinSectionY(),
            this.areaWithOldGeneration.getMaxSectionY() + 1,
            hasHeight ? Optional.of(DoubleArrays.copy(this.heights)) : Optional.empty()
        );
    }

    public static @Nullable BlendingData getOrUpdateBlendingData(final WorldGenRegion region, final int chunkX, final int chunkZ) {
        ChunkAccess chunk = region.getChunk(chunkX, chunkZ);
        BlendingData blendingData = chunk.getBlendingData();
        if (blendingData != null && !chunk.getHighestGeneratedStatus().isBefore(ChunkStatus.BIOMES)) {
            blendingData.calculateData(chunk, sideByGenerationAge(region, chunkX, chunkZ, false));
            return blendingData;
        } else {
            return null;
        }
    }

    public static Set<Direction8> sideByGenerationAge(final WorldGenLevel region, final int chunkX, final int chunkZ, final boolean wantedOldGen) {
        Set<Direction8> sides = EnumSet.noneOf(Direction8.class);

        for (Direction8 direction8 : Direction8.values()) {
            int testChunkX = chunkX + direction8.getStepX();
            int testChunkZ = chunkZ + direction8.getStepZ();
            if (region.getChunk(testChunkX, testChunkZ).isOldNoiseGeneration() == wantedOldGen) {
                sides.add(direction8);
            }
        }

        return sides;
    }

    private void calculateData(final ChunkAccess chunk, final Set<Direction8> newSides) {
        if (!this.hasCalculatedData) {
            if (newSides.contains(Direction8.NORTH) || newSides.contains(Direction8.WEST) || newSides.contains(Direction8.NORTH_WEST)) {
                this.addValuesForColumn(getInsideIndex(0, 0), chunk, 0, 0);
            }

            if (newSides.contains(Direction8.NORTH)) {
                for (int i = 1; i < QUARTS_PER_SECTION; i++) {
                    this.addValuesForColumn(getInsideIndex(i, 0), chunk, 4 * i, 0);
                }
            }

            if (newSides.contains(Direction8.WEST)) {
                for (int i = 1; i < QUARTS_PER_SECTION; i++) {
                    this.addValuesForColumn(getInsideIndex(0, i), chunk, 0, 4 * i);
                }
            }

            if (newSides.contains(Direction8.EAST)) {
                for (int i = 1; i < QUARTS_PER_SECTION; i++) {
                    this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, i), chunk, 15, 4 * i);
                }
            }

            if (newSides.contains(Direction8.SOUTH)) {
                for (int i = 0; i < QUARTS_PER_SECTION; i++) {
                    this.addValuesForColumn(getOutsideIndex(i, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), chunk, 4 * i, 15);
                }
            }

            if (newSides.contains(Direction8.EAST) && newSides.contains(Direction8.NORTH_EAST)) {
                this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, 0), chunk, 15, 0);
            }

            if (newSides.contains(Direction8.EAST) && newSides.contains(Direction8.SOUTH) && newSides.contains(Direction8.SOUTH_EAST)) {
                this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), chunk, 15, 15);
            }

            this.hasCalculatedData = true;
        }
    }

    private void addValuesForColumn(final int index, final ChunkAccess chunk, final int blockX, final int blockZ) {
        if (this.heights[index] == Double.MAX_VALUE) {
            this.heights[index] = this.getHeightAtXZ(chunk, blockX, blockZ);
        }

        this.densities[index] = this.getDensityColumn(chunk, blockX, blockZ, Mth.floor(this.heights[index]));
        this.biomes.set(index, this.getBiomeColumn(chunk, blockX, blockZ));
    }

    private int getHeightAtXZ(final ChunkAccess chunk, final int blockX, final int blockZ) {
        int height;
        if (chunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE_WG)) {
            height = Math.min(chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockX, blockZ), this.areaWithOldGeneration.getMaxY());
        } else {
            height = this.areaWithOldGeneration.getMaxY();
        }

        int minY = this.areaWithOldGeneration.getMinY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(blockX, height, blockZ);

        while (pos.getY() > minY) {
            if (SURFACE_BLOCKS.contains(chunk.getBlockState(pos).getBlock())) {
                return pos.getY();
            }

            pos.move(Direction.DOWN);
        }

        return minY;
    }

    private static double read1(final ChunkAccess chunk, final BlockPos.MutableBlockPos pos) {
        return isGround(chunk, pos.move(Direction.DOWN)) ? 1.0 : -1.0;
    }

    private static double read7(final ChunkAccess chunk, final BlockPos.MutableBlockPos pos) {
        double sum = 0.0;

        for (int i = 0; i < 7; i++) {
            sum += read1(chunk, pos);
        }

        return sum;
    }

    private double[] getDensityColumn(final ChunkAccess chunk, final int x, final int z, final int height) {
        double[] densities = new double[this.cellCountPerColumn()];
        Arrays.fill(densities, -1.0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, this.areaWithOldGeneration.getMaxY() + 1, z);
        double last7 = read7(chunk, pos);

        for (int cellIndex = densities.length - 2; cellIndex >= 0; cellIndex--) {
            double one = read1(chunk, pos);
            double current7 = read7(chunk, pos);
            densities[cellIndex] = (last7 + one + current7) / 15.0;
            last7 = current7;
        }

        int highestCellWithSurfaceIndex = this.getCellYIndex(Mth.floorDiv(height, 8));
        if (highestCellWithSurfaceIndex >= 0 && highestCellWithSurfaceIndex < densities.length - 1) {
            double inCellIndex = (height + 0.5) % 8.0 / 8.0;
            double amplitudeAboveToMakeSurfaceBeAtHeight = (1.0 - inCellIndex) / inCellIndex;
            double max = Math.max(amplitudeAboveToMakeSurfaceBeAtHeight, 1.0) * 0.25;
            densities[highestCellWithSurfaceIndex + 1] = -amplitudeAboveToMakeSurfaceBeAtHeight / max;
            densities[highestCellWithSurfaceIndex] = 1.0 / max;
        }

        return densities;
    }

    private List<Holder<Biome>> getBiomeColumn(final ChunkAccess chunk, final int blockX, final int blockZ) {
        ObjectArrayList<Holder<Biome>> biomes = new ObjectArrayList<>(this.quartCountPerColumn());
        biomes.size(this.quartCountPerColumn());

        for (int quartIndex = 0; quartIndex < biomes.size(); quartIndex++) {
            int quartY = quartIndex + QuartPos.fromBlock(this.areaWithOldGeneration.getMinY());
            biomes.set(quartIndex, chunk.getNoiseBiome(QuartPos.fromBlock(blockX), quartY, QuartPos.fromBlock(blockZ)));
        }

        return biomes;
    }

    private static boolean isGround(final ChunkAccess chunk, final BlockPos pos) {
        BlockState state = chunk.getBlockState(pos);
        if (state.isAir()) {
            return false;
        } else if (state.is(BlockTags.LEAVES)) {
            return false;
        } else if (state.is(BlockTags.LOGS)) {
            return false;
        } else {
            return state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK) ? false : !state.getCollisionShape(chunk, pos).isEmpty();
        }
    }

    protected double getHeight(final int cellX, final int cellY, final int cellZ) {
        if (cellX == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE || cellZ == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
            return this.heights[getOutsideIndex(cellX, cellZ)];
        } else {
            return cellX != 0 && cellZ != 0 ? Double.MAX_VALUE : this.heights[getInsideIndex(cellX, cellZ)];
        }
    }

    private double getDensity(final double @Nullable [] densityColumn, final int cellY) {
        if (densityColumn == null) {
            return Double.MAX_VALUE;
        }

        int yIndex = this.getCellYIndex(cellY);
        return yIndex >= 0 && yIndex < densityColumn.length ? densityColumn[yIndex] * 0.1 : Double.MAX_VALUE;
    }

    protected double getDensity(final int cellX, final int cellY, final int cellZ) {
        if (cellY == this.getMinY()) {
            return 0.1;
        } else if (cellX == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE || cellZ == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
            return this.getDensity(this.densities[getOutsideIndex(cellX, cellZ)], cellY);
        } else {
            return cellX != 0 && cellZ != 0 ? Double.MAX_VALUE : this.getDensity(this.densities[getInsideIndex(cellX, cellZ)], cellY);
        }
    }

    protected void iterateBiomes(final int minCellX, final int quartY, final int minCellZ, final BlendingData.BiomeConsumer biomeConsumer) {
        if (quartY >= QuartPos.fromBlock(this.areaWithOldGeneration.getMinY()) && quartY <= QuartPos.fromBlock(this.areaWithOldGeneration.getMaxY())) {
            int quartIndex = quartY - QuartPos.fromBlock(this.areaWithOldGeneration.getMinY());

            for (int i = 0; i < this.biomes.size(); i++) {
                List<Holder<Biome>> biomeCell = this.biomes.get(i);
                if (biomeCell != null) {
                    Holder<Biome> value = biomeCell.get(quartIndex);
                    if (value != null) {
                        biomeConsumer.consume(minCellX + getX(i), minCellZ + getZ(i), value);
                    }
                }
            }
        }
    }

    protected void iterateHeights(final int minCellX, final int minCellZ, final BlendingData.HeightConsumer heightConsumer) {
        for (int i = 0; i < this.heights.length; i++) {
            double value = this.heights[i];
            if (value != Double.MAX_VALUE) {
                heightConsumer.consume(minCellX + getX(i), minCellZ + getZ(i), value);
            }
        }
    }

    protected void iterateDensities(
        final int minCellX, final int minCellZ, final int fromCellY, final int toCellY, final BlendingData.DensityConsumer densityConsumer
    ) {
        int minCellY = this.getColumnMinY();
        int minYIndex = Math.max(0, fromCellY - minCellY);
        int maxYIndex = Math.min(this.cellCountPerColumn(), toCellY - minCellY);

        for (int i = 0; i < this.densities.length; i++) {
            double[] densityColumn = this.densities[i];
            if (densityColumn != null) {
                int testCellX = minCellX + getX(i);
                int testCellZ = minCellZ + getZ(i);

                for (int yIndex = minYIndex; yIndex < maxYIndex; yIndex++) {
                    densityConsumer.consume(testCellX, yIndex + minCellY, testCellZ, densityColumn[yIndex] * 0.1);
                }
            }
        }
    }

    private int cellCountPerColumn() {
        return this.areaWithOldGeneration.getSectionsCount() * 2;
    }

    private int quartCountPerColumn() {
        return QuartPos.fromSection(this.areaWithOldGeneration.getSectionsCount());
    }

    private int getColumnMinY() {
        return this.getMinY() + 1;
    }

    private int getMinY() {
        return this.areaWithOldGeneration.getMinSectionY() * 2;
    }

    private int getCellYIndex(final int cellY) {
        return cellY - this.getColumnMinY();
    }

    private static int getInsideIndex(final int x, final int z) {
        return CELL_HORIZONTAL_MAX_INDEX_INSIDE - x + z;
    }

    private static int getOutsideIndex(final int x, final int z) {
        return CELL_COLUMN_INSIDE_COUNT + x + CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - z;
    }

    private static int getX(final int index) {
        if (index < CELL_COLUMN_INSIDE_COUNT) {
            return zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_INSIDE - index);
        }

        int offsetIndex = index - CELL_COLUMN_INSIDE_COUNT;
        return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - offsetIndex);
    }

    private static int getZ(final int index) {
        if (index < CELL_COLUMN_INSIDE_COUNT) {
            return zeroIfNegative(index - CELL_HORIZONTAL_MAX_INDEX_INSIDE);
        }

        int offsetIndex = index - CELL_COLUMN_INSIDE_COUNT;
        return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative(offsetIndex - CELL_HORIZONTAL_MAX_INDEX_OUTSIDE);
    }

    private static int zeroIfNegative(final int value) {
        return value & ~(value >> 31);
    }

    public LevelHeightAccessor getAreaWithOldGeneration() {
        return this.areaWithOldGeneration;
    }

    protected interface BiomeConsumer {
        void consume(final int cellX, final int cellZ, final Holder<Biome> biome);
    }

    protected interface DensityConsumer {
        void consume(final int cellX, final int cellY, final int cellZ, final double density);
    }

    protected interface HeightConsumer {
        void consume(final int cellX, final int cellZ, final double height);
    }

    public record Packed(int minSection, int maxSection, Optional<double[]> heights) {
        private static final Codec<double[]> DOUBLE_ARRAY_CODEC = Codec.DOUBLE.listOf().xmap(Doubles::toArray, Doubles::asList);
        public static final Codec<BlendingData.Packed> CODEC = RecordCodecBuilder.<BlendingData.Packed>create(
                i -> i.group(
                        Codec.INT.fieldOf("min_section").forGetter(BlendingData.Packed::minSection),
                        Codec.INT.fieldOf("max_section").forGetter(BlendingData.Packed::maxSection),
                        DOUBLE_ARRAY_CODEC.lenientOptionalFieldOf("heights").forGetter(BlendingData.Packed::heights)
                    )
                    .apply(i, BlendingData.Packed::new)
            )
            .validate(BlendingData.Packed::validateArraySize);

        private static DataResult<BlendingData.Packed> validateArraySize(final BlendingData.Packed blendingData) {
            return blendingData.heights.isPresent() && ((double[])blendingData.heights.get()).length != BlendingData.CELL_COLUMN_COUNT
                ? DataResult.error(() -> "heights has to be of length " + BlendingData.CELL_COLUMN_COUNT)
                : DataResult.success(blendingData);
        }
    }
}