package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;

import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;

public class NormalNoise {
    private static final double INPUT_FACTOR = 1.0181268882175227;
    private static final double TARGET_DEVIATION = 0.3333333333333333;
    private final double valueFactor;
    private final PerlinNoise first;
    private final PerlinNoise second;
    private final double maxValue;
    private final NormalNoise.NoiseParameters parameters;

    private static boolean isBedrockMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && ("Bedrock-Edition".equals(config.farlandsStyle));
    }

    @Deprecated
    public static NormalNoise createLegacyNetherBiome(final RandomSource random, final NormalNoise.NoiseParameters parameters) {
        return new NormalNoise(random, parameters, false);
    }

    public static NormalNoise create(final RandomSource random, final int firstOctave, final double
                    ... amplitudes) {
        return create(random, new NormalNoise.NoiseParameters(firstOctave, new DoubleArrayList(amplitudes)));
    }

    public static NormalNoise create(final RandomSource random, final NormalNoise.NoiseParameters parameters) {
        return new NormalNoise(random, parameters, true);
    }

    private NormalNoise(final RandomSource random, final NormalNoise.NoiseParameters parameters, final boolean useNewInitialization) {
        int firstOctave = parameters.firstOctave;
        DoubleList amplitudes = parameters.amplitudes;
        this.parameters = parameters;

        boolean isBedrock = isBedrockMode();

        // 在 Bedrock 模式下，截断振幅列表，用于计算 octave 范围和 valueFactor
        DoubleList effectiveAmplitudes = amplitudes;
        if (isBedrock) {
            DoubleArrayList truncated = new DoubleArrayList(amplitudes.size());
            for (int i = 0; i < amplitudes.size(); i++) {
                truncated.add((float) amplitudes.getDouble(i));
            }
            effectiveAmplitudes = truncated;
        }

        // 使用 effectiveAmplitudes 创建 PerlinNoise（create 方法内部会再次截断，保持一致性）
        if (useNewInitialization) {
            this.first = PerlinNoise.create(random, firstOctave, effectiveAmplitudes);
            this.second = PerlinNoise.create(random, firstOctave, effectiveAmplitudes);
        } else {
            this.first = PerlinNoise.createLegacyForLegacyNetherBiome(random, firstOctave, effectiveAmplitudes);
            this.second = PerlinNoise.createLegacyForLegacyNetherBiome(random, firstOctave, effectiveAmplitudes);
        }

        int minOctave = Integer.MAX_VALUE;
        int maxOctave = Integer.MIN_VALUE;
        DoubleListIterator iterator = effectiveAmplitudes.iterator();

        while (iterator.hasNext()) {
            int i = iterator.nextIndex();
            double amplitude = iterator.nextDouble();
            if (amplitude != 0.0) {
                minOctave = Math.min(minOctave, i);
                maxOctave = Math.max(maxOctave, i);
            }
        }

        double rawExpectedDeviation = expectedDeviation(maxOctave - minOctave);
        double rawValueFactor = 0.16666666666666666 / rawExpectedDeviation;
        if (isBedrock) {
            this.valueFactor = (float) rawValueFactor;
        } else {
            this.valueFactor = rawValueFactor;
        }

        double rawMaxValue = (this.first.maxValue() + this.second.maxValue()) * this.valueFactor;
        if (isBedrock) {
            this.maxValue = (float) rawMaxValue;
        } else {
            this.maxValue = rawMaxValue;
        }
    }

    public double maxValue() {
        double result = this.maxValue;
        if (isBedrockMode()) {
            return (float) result;
        }
        return result;
    }

    private static double expectedDeviation(final int octaveSpan) {
        if (isBedrockMode()) {
            // === Bedrock 模式：全 float 计算 ===
            float fOctaveSpan = (float) octaveSpan;
            float result = 0.1f * (1.0f + 1.0f / (fOctaveSpan + 1.0f));
            return (float) result;
        }
        // === 原 double 实现 ===
        return 0.1 * (1.0 + 1.0 / (octaveSpan + 1));
    }

    public double getValue(final double x, final double y, final double z) {
        if (isBedrockMode()) {
            // === Bedrock 模式：全 float 精度 ===
            float fx = (float) x;
            float fy = (float) y;
            float fz = (float) z;

            // INPUT_FACTOR 转 float 后再乘，模拟单精度
            float factor = (float) INPUT_FACTOR;
            float x2 = fx * factor;
            float y2 = fy * factor;
            float z2 = fz * factor;

            // 调用 PerlinNoise.getValue（内部会根据模式切换精度）
            // 但为了确保输入精度已被截断，我们将 float 转回 double 传入（PerlinNoise 内部会再次判断）
            float val1 = (float) this.first.getValue((double) fx, (double) fy, (double) fz);
            float val2 = (float) this.second.getValue((double) x2, (double) y2, (double) z2);

            float result = (val1 + val2) * (float) this.valueFactor;
            return (float) result;
        }

        // === 原 double 实现 ===
        double x2 = x * 1.0181268882175227;
        double y2 = y * 1.0181268882175227;
        double z2 = z * 1.0181268882175227;
        double result = (this.first.getValue(x, y, z) + this.second.getValue(x2, y2, z2)) * this.valueFactor;
        return result;
    }

    public NormalNoise.NoiseParameters parameters() {
        return this.parameters;
    }

    @VisibleForTesting
    public void parityConfigString(final StringBuilder sb) {
        sb.append("NormalNoise {");
        sb.append("first: ");
        this.first.parityConfigString(sb);
        sb.append(", second: ");
        this.second.parityConfigString(sb);
        sb.append("}");
    }

    public record NoiseParameters(int firstOctave, DoubleList amplitudes) {
        public static final Codec<
                NormalNoise.NoiseParameters> DIRECT_CODEC = RecordCodecBuilder.create(
                i -> i.group(
                        Codec.INT.fieldOf("firstOctave").forGetter(NormalNoise.NoiseParameters
                                ::firstOctave),
                        Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(NormalNoise.NoiseParameters
                                ::amplitudes)
                )
                        .apply(i, NormalNoise.NoiseParameters::new)
        );
        public static final Codec<
                Holder<
                        NormalNoise.NoiseParameters>> CODEC = RegistryFileCodec.create(Registries.NOISE, DIRECT_CODEC);

        public NoiseParameters(final int firstOctave, final List<Double> amplitudes) {
            this(firstOctave, new DoubleArrayList(amplitudes));
        }

        public NoiseParameters(final int firstOctave, final double firstAmplitude, final double
                        ... amplitudes) {
            this(firstOctave, Util.make(new DoubleArrayList(amplitudes), list -> list.add(0, firstAmplitude)));
        }
    }
}