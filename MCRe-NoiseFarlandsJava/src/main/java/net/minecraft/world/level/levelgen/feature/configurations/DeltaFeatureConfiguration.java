package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.state.BlockState;

public class DeltaFeatureConfiguration implements FeatureConfiguration {
    public static final Codec<DeltaFeatureConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockState.CODEC.fieldOf("contents").forGetter(c -> c.contents),
                BlockState.CODEC.fieldOf("rim").forGetter(c -> c.rim),
                IntProviders.codec(0, 16).fieldOf("size").forGetter(c -> c.size),
                IntProviders.codec(0, 16).fieldOf("rim_size").forGetter(c -> c.rimSize)
            )
            .apply(i, DeltaFeatureConfiguration::new)
    );
    private final BlockState contents;
    private final BlockState rim;
    private final IntProvider size;
    private final IntProvider rimSize;

    public DeltaFeatureConfiguration(final BlockState contents, final BlockState rim, final IntProvider size, final IntProvider rimSize) {
        this.contents = contents;
        this.rim = rim;
        this.size = size;
        this.rimSize = rimSize;
    }

    public BlockState contents() {
        return this.contents;
    }

    public BlockState rim() {
        return this.rim;
    }

    public IntProvider size() {
        return this.size;
    }

    public IntProvider rimSize() {
        return this.rimSize;
    }
}