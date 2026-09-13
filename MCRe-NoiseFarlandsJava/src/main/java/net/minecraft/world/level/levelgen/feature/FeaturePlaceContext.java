package net.minecraft.world.level.levelgen.feature;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class FeaturePlaceContext<FC extends FeatureConfiguration> {
    private final Optional<ConfiguredFeature<?, ?>> topFeature;
    private final WorldGenLevel level;
    private final ChunkGenerator chunkGenerator;
    private final RandomSource random;
    private final BlockPos origin;
    private final FC config;

    public FeaturePlaceContext(
        final Optional<ConfiguredFeature<?, ?>> topFeature,
        final WorldGenLevel level,
        final ChunkGenerator chunkGenerator,
        final RandomSource random,
        final BlockPos origin,
        final FC config
    ) {
        this.topFeature = topFeature;
        this.level = level;
        this.chunkGenerator = chunkGenerator;
        this.random = random;
        this.origin = origin;
        this.config = config;
    }

    public Optional<ConfiguredFeature<?, ?>> topFeature() {
        return this.topFeature;
    }

    public WorldGenLevel level() {
        return this.level;
    }

    public ChunkGenerator chunkGenerator() {
        return this.chunkGenerator;
    }

    public RandomSource random() {
        return this.random;
    }

    public BlockPos origin() {
        return this.origin;
    }

    public FC config() {
        return this.config;
    }
}