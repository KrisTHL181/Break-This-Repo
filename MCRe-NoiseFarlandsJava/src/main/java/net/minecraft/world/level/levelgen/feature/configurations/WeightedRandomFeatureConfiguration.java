package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record WeightedRandomFeatureConfiguration(WeightedList<Holder<PlacedFeature>> features) implements FeatureConfiguration {
    public static final Codec<WeightedRandomFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(WeightedList.codec(PlacedFeature.CODEC).fieldOf("features").forGetter(WeightedRandomFeatureConfiguration::features))
            .apply(i, WeightedRandomFeatureConfiguration::new)
    );

    @Override
    public Stream<Holder<ConfiguredFeature<?, ?>>> getSubFeatures() {
        return this.features.unwrap().stream().flatMap(weighted -> weighted.value().value().getFeatures());
    }
}