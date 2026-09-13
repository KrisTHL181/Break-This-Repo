package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.CompositeFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SimpleRandomSelectorFeature extends Feature<CompositeFeatureConfiguration> {
    public SimpleRandomSelectorFeature(final Codec<CompositeFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<CompositeFeatureConfiguration> context) {
        RandomSource random = context.random();
        CompositeFeatureConfiguration config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        int index = random.nextInt(config.features().size());
        PlacedFeature feature = config.features().get(index).value();
        return feature.place(level, chunkGenerator, random, origin);
    }
}