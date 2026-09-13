package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;

public class CountConfiguration implements FeatureConfiguration {
    public static final Codec<CountConfiguration> CODEC = IntProviders.codec(0, 256)
        .fieldOf("count")
        .xmap(CountConfiguration::new, CountConfiguration::count)
        .codec();
    private final IntProvider count;

    public CountConfiguration(final int count) {
        this.count = ConstantInt.of(count);
    }

    public CountConfiguration(final IntProvider count) {
        this.count = count;
    }

    public IntProvider count() {
        return this.count;
    }
}