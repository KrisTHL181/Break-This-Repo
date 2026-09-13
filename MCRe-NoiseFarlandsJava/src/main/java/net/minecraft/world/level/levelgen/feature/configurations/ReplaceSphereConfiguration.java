package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceSphereConfiguration implements FeatureConfiguration {
    public static final Codec<ReplaceSphereConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockState.CODEC.fieldOf("target").forGetter(c -> c.targetState),
                BlockState.CODEC.fieldOf("state").forGetter(c -> c.replaceState),
                IntProviders.codec(0, 12).fieldOf("radius").forGetter(c -> c.radius)
            )
            .apply(i, ReplaceSphereConfiguration::new)
    );
    public final BlockState targetState;
    public final BlockState replaceState;
    private final IntProvider radius;

    public ReplaceSphereConfiguration(final BlockState targetState, final BlockState replaceState, final IntProvider radius) {
        this.targetState = targetState;
        this.replaceState = replaceState;
        this.radius = radius;
    }

    public IntProvider radius() {
        return this.radius;
    }
}