package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.WorldReposition;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.core.Direction;

public class BlendedNoise implements DensityFunction.SimpleFunction {
    private static final Codec<Double> SCALE_RANGE = Codec.doubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    private static final MapCodec<BlendedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    SCALE_RANGE.fieldOf("xz_scale").forGetter(n -> n.xzScale),
                    SCALE_RANGE.fieldOf("y_scale").forGetter(n -> n.yScale),
                    SCALE_RANGE.fieldOf("xz_factor").forGetter(n -> n.xzFactor),
                    SCALE_RANGE.fieldOf("y_factor").forGetter(n -> n.yFactor),
                    Codec.doubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).fieldOf("smear_scale_multiplier").forGetter(n -> n.smearScaleMultiplier)
            )
                    .apply(i, BlendedNoise::createUnseeded)
    );
    public static final KeyDispatchDataCodec<BlendedNoise> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
    private final PerlinNoise minLimitNoise;
    private final PerlinNoise maxLimitNoise;
    private final PerlinNoise mainNoise;
    private final double xzMultiplier;
    private final double yMultiplier;
    private final double xzFactor;
    private final double yFactor;
    private final double smearScaleMultiplier;
    private final double maxValue;
    private final double xzScale;
    private final double yScale;

    private static boolean isBedrockMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && ("Bedrock-Edition".equals(config.farlandsStyle));
    }

    private static boolean is1_18Exp4Mode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && ("1.18-exp-32bit".equals(config.precisionMode) || "1.18-exp-64bit".equals(config.precisionMode));
    }

    private static PerlinNoise createLimitNoiseExp4(RandomSource random) {
        DoubleArrayList amplitudes = new DoubleArrayList(new double[16]);
        for (int i = 0; i < 7; i++) amplitudes.set(i, 1.0);
        for (int i = 7; i < 16; i++) amplitudes.set(i, 0.0);
        return new PerlinNoise(random, Pair.of(-15, amplitudes), true);
    }

    public static BlendedNoise createUnseeded(
            final double xzScale, final double yScale, final double xzFactor, final double yFactor, final double smearScaleMultiplier) {
        return new BlendedNoise(new XoroshiroRandomSource(0L), xzScale, yScale, xzFactor, yFactor, smearScaleMultiplier);
    }

    private BlendedNoise(
            final PerlinNoise minLimitNoise,
            final PerlinNoise maxLimitNoise,
            final PerlinNoise mainNoise,
            final double xzScale,
            final double yScale,
            final double xzFactor,
            final double yFactor,
            final double smearScaleMultiplier) {
        this.minLimitNoise = minLimitNoise;
        this.maxLimitNoise = maxLimitNoise;
        this.mainNoise = mainNoise;
        this.xzScale = xzScale;
        this.yScale = yScale;
        this.xzFactor = xzFactor;
        this.yFactor = yFactor;
        this.smearScaleMultiplier = smearScaleMultiplier;

        boolean isBedrock = isBedrockMode();

        // 修改：在 Bedrock 模式下对乘数因子进行 float 截断
        double rawXzMultiplier = 684.412 * this.xzScale;
        double rawYMultiplier = 684.412 * this.yScale;
        if (isBedrock) {
            this.xzMultiplier = (float) rawXzMultiplier;
            this.yMultiplier = (float) rawYMultiplier;
        } else {
            this.xzMultiplier = rawXzMultiplier;
            this.yMultiplier = rawYMultiplier;
        }

        // maxValue 通过 minLimitNoise.maxBrokenValue 计算，该方法内部已根据 isBedrockMode() 切换精度
        // 若当前为 Bedrock 模式，maxBrokenValue 会返回截断后的值
        this.maxValue = minLimitNoise.maxBrokenValue(this.yMultiplier);
    }

    @VisibleForTesting
    public BlendedNoise(
            final RandomSource random, final double xzScale, final double yScale, final double xzFactor, final double yFactor, final double smearScaleMultiplier) {
        boolean exp4 = is1_18Exp4Mode();
        double actualXzScale = exp4 ? 1.0 : xzScale;

        this(
                exp4 ? createLimitNoiseExp4(random) : PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)),
                exp4 ? createLimitNoiseExp4(random) : PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-15, 0)),
                exp4 ? PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-7, 0))
                        : PerlinNoise.createLegacyForBlendedNoise(random, IntStream.rangeClosed(-7, 0)),
                actualXzScale, yScale, xzFactor, yFactor, smearScaleMultiplier
        );
    }

    public BlendedNoise withNewRandom(final RandomSource terrainRandom) {
        return new BlendedNoise(terrainRandom, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
    }

    @Override
    public double compute(final DensityFunction.FunctionContext context) {
        if (isBedrockMode()) {
            // === Bedrock 模式：全部使用 float 精度计算 ===
            // 🔧 MCRe：先施加 WorldReposition 偏移（newPos = pos * scale + shift），再乘 xzMultiplier/yMultiplier
            float limitX = (float)(WorldReposition.reposition(context.blockX(), Direction.Axis.X) * this.xzMultiplier);
            float limitY = (float)(WorldReposition.reposition(context.blockY(), Direction.Axis.Y) * this.yMultiplier);
            float limitZ = (float)(WorldReposition.reposition(context.blockZ(), Direction.Axis.Z) * this.xzMultiplier);
            float mainX = limitX / (float)this.xzFactor;
            float mainY = limitY / (float)this.yFactor;
            float mainZ = limitZ / (float)this.xzFactor;
            float limitSmear = (float)(this.yMultiplier * this.smearScaleMultiplier);
            float mainSmear = limitSmear / (float)this.yFactor;

            float mainNoiseValue = 0.0f;
            float pow = 1.0f;

            // mainNoise 循环（8 次）
            for (int i = 0; i < 8; i++) {
                ImprovedNoise noise = this.mainNoise.getOctaveNoise(i);
                if (noise != null) {
                    float wx = (float) PerlinNoise.wrap(mainX * pow);
                    float wy = (float) PerlinNoise.wrap(mainY * pow);
                    float wz = (float) PerlinNoise.wrap(mainZ * pow);
                    float noiseVal = (float) noise.noise(wx, wy, wz, mainSmear * pow, mainY * pow);
                    mainNoiseValue += noiseVal / pow;
                }
                pow /= 2.0f;
            }

            float factor = (mainNoiseValue / 10.0f + 1.0f) / 2.0f;
            boolean isMax = factor >= 1.0f;
            boolean isMin = factor <= 0.0f;
            pow = 1.0f;

            float blendMin = 0.0f;
            float blendMax = 0.0f;

            // limit noise 循环（16 次）
            for (int i = 0; i < 16; i++) {
                float wx = (float) PerlinNoise.wrap(limitX * pow);
                float wy = (float) PerlinNoise.wrap(limitY * pow);
                float wz = (float) PerlinNoise.wrap(limitZ * pow);
                float yScalePow = limitSmear * pow;
                float limitYPow = limitY * pow;

                if (!isMax) {
                    ImprovedNoise minNoise = this.minLimitNoise.getOctaveNoise(i);
                    if (minNoise != null) {
                        float noiseVal = (float) minNoise.noise(wx, wy, wz, limitSmear * pow, limitY * pow);
                        blendMin += noiseVal / pow;
                    }
                }

                if (!isMin) {
                    ImprovedNoise maxNoise = this.maxLimitNoise.getOctaveNoise(i);
                    if (maxNoise != null) {
                        float noiseVal = (float) maxNoise.noise(wx, wy, wz, limitSmear * pow, limitY * pow);
                        blendMax += noiseVal / pow;
                    }
                }

                pow /= 2.0f;
            }

            float result = Mth.clampedLerp(factor, blendMin / 512.0f, blendMax / 512.0f) / 128.0f;
            return (float) result;
        }

        // === 原 double 实现 ===
        // 🔧 MCRe：先施加 WorldReposition 偏移（newPos = pos * scale + shift），再乘 xzMultiplier/yMultiplier
        double limitX = WorldReposition.reposition(context.blockX(), Direction.Axis.X) * this.xzMultiplier;
        double limitY = WorldReposition.reposition(context.blockY(), Direction.Axis.Y) * this.yMultiplier;
        double limitZ = WorldReposition.reposition(context.blockZ(), Direction.Axis.Z) * this.xzMultiplier;
        double mainX = limitX / this.xzFactor;
        double mainY = limitY / this.yFactor;
        double mainZ = limitZ / this.xzFactor;
        double limitSmear = this.yMultiplier * this.smearScaleMultiplier;
        double mainSmear = limitSmear / this.yFactor;
        double blendMin = 0.0;
        double blendMax = 0.0;
        double mainNoiseValue = 0.0;
        boolean optimizeLoop = true;
        double pow = 1.0;

        for (int i = 0; i < 8; i++) {
            ImprovedNoise noise = this.mainNoise.getOctaveNoise(i);
            if (noise != null) {
                mainNoiseValue +=
                        noise.noise(
                                PerlinNoise.wrap(mainX * pow), PerlinNoise.wrap(mainY * pow), PerlinNoise.wrap(mainZ * pow), mainSmear * pow, mainY * pow
                        ) / pow;
            }
            pow /= 2.0;
        }

        double factor = (mainNoiseValue / 10.0 + 1.0) / 2.0;
        boolean isMax = factor >= 1.0;
        boolean isMin = factor <= 0.0;
        pow = 1.0;

        for (int i = 0; i < 16; i++) {
            double wx = PerlinNoise.wrap(limitX * pow);
            double wy = PerlinNoise.wrap(limitY * pow);
            double wz = PerlinNoise.wrap(limitZ * pow);
            double yScalePow = limitSmear * pow;
            double limitYPow = limitY * pow;

            if (!isMax) {
                ImprovedNoise minNoise = this.minLimitNoise.getOctaveNoise(i);
                if (minNoise != null) {
                    blendMin += minNoise.noise(wx, wy, wz, limitSmear * pow, limitY * pow) / pow;
                }
            }

            if (!isMin) {
                ImprovedNoise maxNoise = this.maxLimitNoise.getOctaveNoise(i);
                if (maxNoise != null) {
                    blendMax += maxNoise.noise(wx, wy, wz, limitSmear * pow, limitY * pow) / pow;
                }
            }

            pow /= 2.0;
        }

        double result = Mth.clampedLerp(factor, blendMin / 512.0, blendMax / 512.0) / 128.0;
        return result;
    }

    @Override
    public double minValue() {
        double result = -this.maxValue();
        if (isBedrockMode()) {
            return (float) result;
        }
        return result;
    }

    @Override
    public double maxValue() {
        double result = this.maxValue;
        if (isBedrockMode()) {
            return (float) result;
        }
        return result;
    }

    @VisibleForTesting
    public void parityConfigString(final StringBuilder sb) {
        sb.append("BlendedNoise{minLimitNoise=");
        this.minLimitNoise.parityConfigString(sb);
        sb.append(", maxLimitNoise=");
        this.maxLimitNoise.parityConfigString(sb);
        sb.append(", mainNoise=");
        this.mainNoise.parityConfigString(sb);
        sb.append(
                String.format(
                        Locale.ROOT,
                        ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8",
                        684.412,
                        684.412,
                        8.555150000000001,
                        4.277575000000001
                )
        )
                .append('}');
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}