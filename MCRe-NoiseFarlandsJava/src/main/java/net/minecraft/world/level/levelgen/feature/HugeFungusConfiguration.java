package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class HugeFungusConfiguration implements FeatureConfiguration {
    public static final Codec<HugeFungusConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                BlockState.CODEC.fieldOf("valid_base_block").forGetter(c -> c.validBaseState),
                BlockState.CODEC.fieldOf("stem_state").forGetter(c -> c.stemState),
                BlockState.CODEC.fieldOf("hat_state").forGetter(c -> c.hatState),
                BlockState.CODEC.fieldOf("decor_state").forGetter(c -> c.decorState),
                BlockPredicate.CODEC.fieldOf("replaceable_blocks").forGetter(c -> c.replaceableBlocks),
                Codec.BOOL.optionalFieldOf("planted", false).forGetter(c -> c.planted)
            )
            .apply(i, HugeFungusConfiguration::new)
    );
    public final BlockState validBaseState;
    public final BlockState stemState;
    public final BlockState hatState;
    public final BlockState decorState;
    public final BlockPredicate replaceableBlocks;
    public final boolean planted;

    public HugeFungusConfiguration(
        final BlockState validBaseState,
        final BlockState stemState,
        final BlockState hatState,
        final BlockState decorState,
        final BlockPredicate replaceableBlocks,
        final boolean planted
    ) {
        this.validBaseState = validBaseState;
        this.stemState = stemState;
        this.hatState = hatState;
        this.decorState = decorState;
        this.replaceableBlocks = replaceableBlocks;
        this.planted = planted;
    }
}