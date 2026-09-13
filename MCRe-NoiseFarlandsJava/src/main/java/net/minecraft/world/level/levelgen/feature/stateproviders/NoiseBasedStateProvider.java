package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public abstract class NoiseBasedStateProvider extends BlockStateProvider {
    protected final long seed;
    protected final NormalNoise.NoiseParameters parameters;
    protected final float scale;
    protected final NormalNoise noise;

    protected static <P extends NoiseBasedStateProvider> P3<Mu<P>, Long, NormalNoise.NoiseParameters, Float> noiseCodec(final Instance<P> instance) {
        return instance.group(
            Codec.LONG.fieldOf("seed").forGetter(p -> p.seed),
            NormalNoise.NoiseParameters.DIRECT_CODEC.fieldOf("noise").forGetter(p -> p.parameters),
            ExtraCodecs.POSITIVE_FLOAT.fieldOf("scale").forGetter(p -> p.scale)
        );
    }

    protected NoiseBasedStateProvider(final long seed, final NormalNoise.NoiseParameters parameters, final float scale) {
        this.seed = seed;
        this.parameters = parameters;
        this.scale = scale;
        this.noise = NormalNoise.create(new WorldgenRandom(new LegacyRandomSource(seed)), parameters);
    }

    protected double getNoiseValue(final BlockPos pos, final double scale) {
        return this.noise.getValue(pos.getX() * scale, pos.getY() * scale, pos.getZ() * scale);
    }
}