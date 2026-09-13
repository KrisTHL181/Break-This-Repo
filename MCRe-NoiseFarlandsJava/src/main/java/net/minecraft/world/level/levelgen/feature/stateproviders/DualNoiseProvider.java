package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class DualNoiseProvider extends NoiseProvider {
    public static final MapCodec<DualNoiseProvider> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                InclusiveRange.codec(Codec.INT, 1, 64).fieldOf("variety").forGetter(p -> p.variety),
                NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("slow_noise").forGetter(p -> p.slowNoiseParameters),
                ExtraCodecs.POSITIVE_FLOAT.fieldOf("slow_scale").forGetter(p -> p.slowScale)
            )
            .and(noiseProviderCodec(i))
            .apply(i, DualNoiseProvider::new)
    );
    private final InclusiveRange<Integer> variety;
    private final NormalNoise.NoiseParameters slowNoiseParameters;
    private final float slowScale;
    private final NormalNoise slowNoise;

    public DualNoiseProvider(
        final InclusiveRange<Integer> variety,
        final NormalNoise.NoiseParameters slowNoiseParameters,
        final float slowScale,
        final long seed,
        final NormalNoise.NoiseParameters parameters,
        final float scale,
        final List<BlockState> states
    ) {
        super(seed, parameters, scale, states);
        this.variety = variety;
        this.slowNoiseParameters = slowNoiseParameters;
        this.slowScale = slowScale;
        this.slowNoise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(seed)), slowNoiseParameters);
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.DUAL_NOISE_PROVIDER;
    }

    @Override
    public BlockState getState(final WorldGenLevel level, final RandomSource random, final BlockPos pos) {
        double varietyNoise = this.getSlowNoiseValue(pos);
        int localVariety = (int)Mth.clampedMap(varietyNoise, -1.0, 1.0, this.variety.minInclusive().intValue(), this.variety.maxInclusive() + 1);
        List<BlockState> possibleStates = Lists.newArrayListWithCapacity(localVariety);

        for (int i = 0; i < localVariety; i++) {
            possibleStates.add(this.getRandomState(this.states, this.getSlowNoiseValue(pos.offset(i * 54545, 0, i * 34234))));
        }

        return this.getRandomState(possibleStates, pos, this.scale);
    }

    protected double getSlowNoiseValue(final BlockPos pos) {
        return this.slowNoise.getValue(pos.getX() * this.slowScale, pos.getY() * this.slowScale, pos.getZ() * this.slowScale);
    }
}