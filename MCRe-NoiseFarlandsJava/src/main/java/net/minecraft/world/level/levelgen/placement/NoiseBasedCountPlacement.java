package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public class NoiseBasedCountPlacement extends RepeatingPlacement {
    public static final MapCodec<NoiseBasedCountPlacement> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
                Codec.INT.fieldOf("noise_to_count_ratio").forGetter(c -> c.noiseToCountRatio),
                Codec.DOUBLE.fieldOf("noise_factor").forGetter(c -> c.noiseFactor),
                Codec.DOUBLE.optionalFieldOf("noise_offset", 0.0).forGetter(c -> c.noiseOffset)
            )
            .apply(i, NoiseBasedCountPlacement::new)
    );
    private final int noiseToCountRatio;
    private final double noiseFactor;
    private final double noiseOffset;

    private NoiseBasedCountPlacement(final int noiseToCountRatio, final double noiseFactor, final double noiseOffset) {
        this.noiseToCountRatio = noiseToCountRatio;
        this.noiseFactor = noiseFactor;
        this.noiseOffset = noiseOffset;
    }

    public static NoiseBasedCountPlacement of(final int noiseToCountRatio, final double noiseFactor, final double noiseOffset) {
        return new NoiseBasedCountPlacement(noiseToCountRatio, noiseFactor, noiseOffset);
    }

    @Override
    protected int count(final RandomSource random, final BlockPos origin) {
        double flowerNoise = Biome.BIOME_INFO_NOISE.getValue(origin.getX() / this.noiseFactor, origin.getZ() / this.noiseFactor, false);
        return (int)Math.ceil((flowerNoise + this.noiseOffset) * this.noiseToCountRatio);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.NOISE_BASED_COUNT;
    }
}