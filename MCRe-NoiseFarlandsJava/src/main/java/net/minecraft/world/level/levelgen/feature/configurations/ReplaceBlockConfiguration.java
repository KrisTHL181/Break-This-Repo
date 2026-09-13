package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;

public class ReplaceBlockConfiguration implements FeatureConfiguration {
    public static final Codec<ReplaceBlockConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(Codec.list(OreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter(c -> c.targetStates))
            .apply(i, ReplaceBlockConfiguration::new)
    );
    public final List<OreConfiguration.TargetBlockState> targetStates;

    public ReplaceBlockConfiguration(final BlockState targetState, final BlockState state) {
        this(ImmutableList.of(OreConfiguration.target(new BlockStateMatchTest(targetState), state)));
    }

    public ReplaceBlockConfiguration(final List<OreConfiguration.TargetBlockState> targetBlockStates) {
        this.targetStates = targetBlockStates;
    }
}