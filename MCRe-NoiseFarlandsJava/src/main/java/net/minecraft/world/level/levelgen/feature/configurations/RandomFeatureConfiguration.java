package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

@Deprecated
public record RandomFeatureConfiguration(List<WeightedPlacedFeature> features, Holder<PlacedFeature> defaultFeature) implements FeatureConfiguration {
    public static final Codec<RandomFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.apply2(
            RandomFeatureConfiguration::new,
            WeightedPlacedFeature.CODEC.listOf().fieldOf("features").forGetter(c -> c.features),
            PlacedFeature.CODEC.fieldOf("default").forGetter(c -> c.defaultFeature)
        )
    );

    @Override
    public Stream<Holder<ConfiguredFeature<?, ?>>> getSubFeatures() {
        return Stream.concat(this.features.stream().flatMap(weighted -> weighted.feature().value().getFeatures()), this.defaultFeature.value().getFeatures());
    }
}