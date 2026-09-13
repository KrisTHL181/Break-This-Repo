package net.minecraft.world.level.levelgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class FlatLevelSource extends ChunkGenerator {
    public static final MapCodec<FlatLevelSource> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings)).apply(i, i.stable(FlatLevelSource::new))
    );
    private final FlatLevelGeneratorSettings settings;

    public FlatLevelSource(final FlatLevelGeneratorSettings generatorSettings) {
        super(new FixedBiomeSource(generatorSettings.getBiome()), Util.memoize(generatorSettings::adjustGenerationSettings));
        this.settings = generatorSettings;
    }

    @Override
    public ChunkGeneratorStructureState createState(final HolderLookup<StructureSet> structureSets, final RandomState randomState, final long levelSeed) {
        Stream<Holder<StructureSet>> structures = this.settings
            .structureOverrides()
            .map(HolderSet::stream)
            .orElseGet(() -> structureSets.listElements().map(e -> (Holder<StructureSet>)e));
        return ChunkGeneratorStructureState.createForFlat(randomState, levelSeed, this.biomeSource, structures);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public FlatLevelGeneratorSettings settings() {
        return this.settings;
    }

    @Override
    public void buildSurface(final WorldGenRegion level, final StructureManager structureManager, final RandomState randomState, final ChunkAccess protoChunk) {
    }

    @Override
    public int getSpawnHeight(final LevelHeightAccessor heightAccessor) {
        return heightAccessor.getMinY() + Math.min(heightAccessor.getHeight(), this.settings.getLayers().size());
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
        final Blender blender, final RandomState randomState, final StructureManager structureManager, final ChunkAccess centerChunk
    ) {
        List<BlockState> layers = this.settings.getLayers();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = centerChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        for (int layerIndex = 0; layerIndex < Math.min(centerChunk.getHeight(), layers.size()); layerIndex++) {
            BlockState blockState = layers.get(layerIndex);
            if (blockState != null) {
                int y = centerChunk.getMinY() + layerIndex;

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        centerChunk.setBlockState(blockPos.set(x, y, z), blockState);
                        oceanFloor.update(x, y, z, blockState);
                        worldSurface.update(x, y, z, blockState);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(centerChunk);
    }

    @Override
    public int getBaseHeight(final int x, final int z, final Heightmap.Types type, final LevelHeightAccessor heightAccessor, final RandomState randomState) {
        List<BlockState> layers = this.settings.getLayers();

        for (int layerIndex = Math.min(layers.size() - 1, heightAccessor.getMaxY()); layerIndex >= 0; layerIndex--) {
            BlockState state = layers.get(layerIndex);
            if (state != null && type.isOpaque().test(state)) {
                return heightAccessor.getMinY() + layerIndex + 1;
            }
        }

        return heightAccessor.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(final int x, final int z, final LevelHeightAccessor heightAccessor, final RandomState randomState) {
        return new NoiseColumn(
            heightAccessor.getMinY(),
            this.settings
                .getLayers()
                .stream()
                .limit(heightAccessor.getHeight())
                .map(state -> state == null ? Blocks.AIR.defaultBlockState() : state)
                .toArray(BlockState[]::new)
        );
    }

    @Override
    public void addDebugScreenInfo(final List<String> result, final RandomState randomState, final BlockPos feetPos) {
    }

    @Override
    public void applyCarvers(
        final WorldGenRegion region,
        final long seed,
        final RandomState randomState,
        final BiomeManager biomeManager,
        final StructureManager structureManager,
        final ChunkAccess chunk
    ) {
    }

    @Override
    public void spawnOriginalMobs(final WorldGenRegion worldGenRegion) {
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }
}