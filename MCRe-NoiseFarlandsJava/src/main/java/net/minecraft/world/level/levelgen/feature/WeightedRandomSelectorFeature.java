package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.WeightedRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class WeightedRandomSelectorFeature extends Feature<WeightedRandomFeatureConfiguration> {
    public WeightedRandomSelectorFeature(final Codec<WeightedRandomFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<WeightedRandomFeatureConfiguration> context) {
        WeightedRandomFeatureConfiguration config = context.config();
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        BlockPos origin = context.origin();
        Optional<Holder<PlacedFeature>> featureToPlace = config.features().getRandom(random);
        return featureToPlace.<Boolean>map(placedFeatureHolder -> placedFeatureHolder.value().place(level, chunkGenerator, random, origin)).orElse(false);
    }
}