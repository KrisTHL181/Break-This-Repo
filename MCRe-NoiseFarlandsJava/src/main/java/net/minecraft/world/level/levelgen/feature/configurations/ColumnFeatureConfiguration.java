package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;

public class ColumnFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<ColumnFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(IntProviders.codec(0, 3).fieldOf("reach").forGetter(c -> c.reach), IntProviders.codec(1, 10).fieldOf("height").forGetter(c -> c.height))
            .apply(i, ColumnFeatureConfiguration::new)
    );
    private final IntProvider reach;
    private final IntProvider height;

    public ColumnFeatureConfiguration(final IntProvider reach, final IntProvider height) {
        this.reach = reach;
        this.height = height;
    }

    public IntProvider reach() {
        return this.reach;
    }

    public IntProvider height() {
        return this.height;
    }
}