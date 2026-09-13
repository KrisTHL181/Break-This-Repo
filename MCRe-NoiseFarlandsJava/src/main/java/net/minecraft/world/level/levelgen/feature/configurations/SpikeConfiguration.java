package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public record SpikeConfiguration(BlockState state, BlockPredicate canPlaceOn, BlockPredicate canReplace) implements FeatureConfiguration {
    public static final Codec<SpikeConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(SpikeConfiguration::state),
                BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(SpikeConfiguration::canPlaceOn),
                BlockPredicate.CODEC.fieldOf("can_replace").forGetter(SpikeConfiguration::canReplace)
            )
            .apply(i, SpikeConfiguration::new)
    );
}