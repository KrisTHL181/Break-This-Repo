package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;

public class LayerConfiguration implements FeatureConfiguration {
    public static final Codec<LayerConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                Codec.intRange(0, DimensionType.Y_SIZE).fieldOf("height").forGetter(c -> c.height), BlockState.CODEC.fieldOf("state").forGetter(c -> c.state)
            )
            .apply(i, LayerConfiguration::new)
    );
    public final int height;
    public final BlockState state;

    public LayerConfiguration(final int height, final BlockState state) {
        this.height = height;
        this.state = state;
    }
}