package net.minecraft.world.level.levelgen.feature.configurations;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

public class OreConfiguration implements FeatureConfiguration {
    public static final Codec<OreConfiguration> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                Codec.list(OreConfiguration.TargetBlockState.CODEC).fieldOf("targets").forGetter(c -> c.targetStates),
                Codec.intRange(0, 64).fieldOf("size").forGetter(c -> c.size),
                Codec.floatRange(0.0F, 1.0F).fieldOf("discard_chance_on_air_exposure").forGetter(c -> c.discardChanceOnAirExposure)
            )
            .apply(i, OreConfiguration::new)
    );
    public final List<OreConfiguration.TargetBlockState> targetStates;
    public final int size;
    public final float discardChanceOnAirExposure;

    public OreConfiguration(final List<OreConfiguration.TargetBlockState> targetBlockStates, final int size, final float discardChanceOnAirExposure) {
        this.size = size;
        this.targetStates = targetBlockStates;
        this.discardChanceOnAirExposure = discardChanceOnAirExposure;
    }

    public OreConfiguration(final List<OreConfiguration.TargetBlockState> targetBlockStates, final int size) {
        this(targetBlockStates, size, 0.0F);
    }

    public OreConfiguration(final RuleTest target, final BlockState state, final int size, final float discardChanceOnAirExposure) {
        this(ImmutableList.of(new OreConfiguration.TargetBlockState(target, state)), size, discardChanceOnAirExposure);
    }

    public OreConfiguration(final RuleTest target, final BlockState state, final int size) {
        this(ImmutableList.of(new OreConfiguration.TargetBlockState(target, state)), size, 0.0F);
    }

    public static OreConfiguration.TargetBlockState target(final RuleTest rule, final BlockState state) {
        return new OreConfiguration.TargetBlockState(rule, state);
    }

    public static class TargetBlockState {
        public static final Codec<OreConfiguration.TargetBlockState> CODEC = RecordCodecBuilder.create(
            i -> i.group(RuleTest.CODEC.fieldOf("target").forGetter(c -> c.target), BlockState.CODEC.fieldOf("state").forGetter(c -> c.state))
                .apply(i, OreConfiguration.TargetBlockState::new)
        );
        public final RuleTest target;
        public final BlockState state;

        private TargetBlockState(final RuleTest target, final BlockState state) {
            this.target = target;
            this.state = state;
        }
    }
}