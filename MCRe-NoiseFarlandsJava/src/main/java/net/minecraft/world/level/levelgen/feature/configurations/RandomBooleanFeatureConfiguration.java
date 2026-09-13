package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class RandomBooleanFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<RandomBooleanFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                PlacedFeature.CODEC.fieldOf("feature_true").forGetter(c -> c.featureTrue),
                PlacedFeature.CODEC.fieldOf("feature_false").forGetter(c -> c.featureFalse)
            )
            .apply(i, RandomBooleanFeatureConfiguration::new)
    );
    public final Holder<PlacedFeature> featureTrue;
    public final Holder<PlacedFeature> featureFalse;

    public RandomBooleanFeatureConfiguration(final Holder<PlacedFeature> featureTrue, final Holder<PlacedFeature> featureFalse) {
        this.featureTrue = featureTrue;
        this.featureFalse = featureFalse;
    }

    @Override
    public Stream<Holder<ConfiguredFeature<?, ?>>> getSubFeatures() {
        return Stream.concat(this.featureTrue.value().getFeatures(), this.featureFalse.value().getFeatures());
    }
}