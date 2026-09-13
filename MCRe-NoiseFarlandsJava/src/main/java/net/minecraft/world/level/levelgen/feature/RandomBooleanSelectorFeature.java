package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RandomBooleanFeatureConfiguration;

public class RandomBooleanSelectorFeature extends Feature<RandomBooleanFeatureConfiguration> {
    public RandomBooleanSelectorFeature(final Codec<RandomBooleanFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<RandomBooleanFeatureConfiguration> context) {
        RandomSource random = context.random();
        RandomBooleanFeatureConfiguration config = context.config();
        WorldGenLevel level = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        BlockPos origin = context.origin();
        boolean result = random.nextBoolean();
        return (result ? config.featureTrue : config.featureFalse).value().place(level, chunkGenerator, random, origin);
    }
}