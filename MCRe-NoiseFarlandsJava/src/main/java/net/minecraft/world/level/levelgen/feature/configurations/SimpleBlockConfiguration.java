package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SimpleBlockConfiguration(BlockStateProvider toPlace, boolean scheduleTick) implements FeatureConfiguration {
    public static final Codec<SimpleBlockConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockStateProvider.CODEC.fieldOf("to_place").forGetter(c -> c.toPlace),
                Codec.BOOL.optionalFieldOf("schedule_tick", false).forGetter(c -> c.scheduleTick)
            )
            .apply(i, SimpleBlockConfiguration::new)
    );

    public SimpleBlockConfiguration(final BlockStateProvider toPlace) {
        this(toPlace, false);
    }
}