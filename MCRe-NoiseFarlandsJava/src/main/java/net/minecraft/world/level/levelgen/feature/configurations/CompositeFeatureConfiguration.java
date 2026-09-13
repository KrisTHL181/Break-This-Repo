package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record CompositeFeatureConfiguration(HolderSet<PlacedFeature> features) implements FeatureConfiguration {
    public static final Codec<CompositeFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(ExtraCodecs.nonEmptyHolderSet(PlacedFeature.LIST_CODEC).fieldOf("features").forGetter(CompositeFeatureConfiguration::features))
            .apply(i, CompositeFeatureConfiguration::new)
    );

    @Override
    public Stream<Holder<ConfiguredFeature<?, ?>>> getSubFeatures() {
        return this.features.stream().flatMap(f -> f.value().getFeatures());
    }
}