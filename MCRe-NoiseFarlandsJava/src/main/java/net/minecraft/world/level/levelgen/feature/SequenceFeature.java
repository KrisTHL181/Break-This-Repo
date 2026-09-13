package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.configurations.CompositeFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class SequenceFeature extends Feature<CompositeFeatureConfiguration> {
    public SequenceFeature(final Codec<CompositeFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<CompositeFeatureConfiguration> context) {
        for (Holder<PlacedFeature> feature : context.config().features()) {
            if (!feature.value().place(context.level(), context.chunkGenerator(), context.random(), context.origin())) {
                return false;
            }
        }

        return true;
    }
}