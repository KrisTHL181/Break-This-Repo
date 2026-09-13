package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;

public class RandomSelectorFeature extends Feature<RandomFeatureConfiguration> {
    public RandomSelectorFeature(final Codec<RandomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<RandomFeatureConfiguration> context) {
        RandomFeatureConfiguration config = context.config();
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        BlockPos origin = context.origin();

        for (WeightedPlacedFeature feature : config.features()) {
            if (random.nextFloat() < feature.chance()) {
                return feature.place(level, chunkGenerator, random, origin);
            }
        }

        return config.defaultFeature().value().place(level, chunkGenerator, random, origin);
    }
}