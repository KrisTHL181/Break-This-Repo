package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ProbabilityFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<ProbabilityFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(c -> c.probability)).apply(i, ProbabilityFeatureConfiguration::new)
    );
    public final float probability;

    public ProbabilityFeatureConfiguration(final float probability) {
        this.probability = probability;
    }
}