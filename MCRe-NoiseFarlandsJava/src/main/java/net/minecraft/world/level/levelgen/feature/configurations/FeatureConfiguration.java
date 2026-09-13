package net.minecraft.world.level.levelgen.feature.configurations;

import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public interface FeatureConfiguration {
    NoneFeatureConfiguration NONE = NoneFeatureConfiguration.INSTANCE;

    default Stream<Holder<ConfiguredFeature<?, ?>>> getSubFeatures() {
        return Stream.empty();
    }
}