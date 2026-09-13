package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public record BlockBlobConfiguration(BlockState state, BlockPredicate canPlaceOn) implements FeatureConfiguration {
    public static final Codec<BlockBlobConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(BlockBlobConfiguration::state),
                BlockPredicate.CODEC.fieldOf("can_place_on").forGetter(BlockBlobConfiguration::canPlaceOn)
            )
            .apply(i, BlockBlobConfiguration::new)
    );
}