package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseThresholdProvider extends NoiseBasedStateProvider {
    public static final MapCodec<NoiseThresholdProvider> CODEC = RecordCodecBuilder.mapCodec(
        i -> noiseCodec(i)
            .and(
                i.group(
                    Codec.floatRange(-1.0F, 1.0F).fieldOf("threshold").forGetter(p -> p.threshold),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("high_chance").forGetter(p -> p.highChance),
                    BlockState.CODEC.fieldOf("default_state").forGetter(p -> p.defaultState),
                    ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("low_states").forGetter(p -> p.lowStates),
                    ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("high_states").forGetter(p -> p.highStates)
                )
            )
            .apply(i, NoiseThresholdProvider::new)
    );
    private final float threshold;
    private final float highChance;
    private final BlockState defaultState;
    private final List<BlockState> lowStates;
    private final List<BlockState> highStates;

    public NoiseThresholdProvider(
        final long seed,
        final NormalNoise.NoiseParameters parameters,
        final float scale,
        final float threshold,
        final float highChance,
        final BlockState defaultState,
        final List<BlockState> lowStates,
        final List<BlockState> highStates
    ) {
        super(seed, parameters, scale);
        this.threshold = threshold;
        this.highChance = highChance;
        this.defaultState = defaultState;
        this.lowStates = lowStates;
        this.highStates = highStates;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.NOISE_THRESHOLD_PROVIDER;
    }

    @Override
    public BlockState getState(final WorldGenLevel level, final RandomSource random, final BlockPos pos) {
        double localValue = this.getNoiseValue(pos, this.scale);
        if (localValue < this.threshold) {
            return Util.getRandom(this.lowStates, random);
        } else {
            return random.nextFloat() < this.highChance ? Util.getRandom(this.highStates, random) : this.defaultState;
        }
    }
}