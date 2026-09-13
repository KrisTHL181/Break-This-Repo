package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;
import org.jspecify.annotations.Nullable;

public class PerlinNoise {
    private boolean isBedrockMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && ("Bedrock-Edition".equals(config.farlandsStyle));
    }

    private boolean is1_18Exp4Mode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && ("1.18-exp-32bit".equals(config.precisionMode) || "1.18-exp-64bit".equals(config.precisionMode));
    }
    
    private static boolean limitReturnValueMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.limitReturnValue;
    }

    private static final int ROUND_OFF = 33554432;
    private final @Nullable ImprovedNoise[] noiseLevels;
    private final int firstOctave;
    private final DoubleList amplitudes;
    private final double lowestFreqValueFactor;
    private final double lowestFreqInputFactor;
    private final double maxValue;

    @Deprecated
    public static PerlinNoise createLegacyForBlendedNoise(final RandomSource random, final IntStream octaves) {
        return new PerlinNoise(random, makeAmplitudes(new IntRBTreeSet(octaves.boxed().collect(ImmutableList.toImmutableList()))), false);
    }

    @Deprecated
    public static PerlinNoise createLegacyForLegacyNetherBiome(final RandomSource random, final int firstOctave, final DoubleList amplitudes) {
        return new PerlinNoise(random, Pair.of(firstOctave, amplitudes), false);
    }

    public static PerlinNoise create(final RandomSource random, final IntStream octaves) {
        return create(random, octaves.boxed().collect(ImmutableList.toImmutableList()));
    }

    public static PerlinNoise create(final RandomSource random, final List<Integer> octaveSet) {
        return new PerlinNoise(random, makeAmplitudes(new IntRBTreeSet(octaveSet)), true);
    }

    // 修改：在 Bedrock 模式下截断振幅为 float 精度
    public static PerlinNoise create(final RandomSource random, final int firstOctave, final double firstAmplitude, final double amplitudes) {
        DoubleArrayList amplitudeList = new DoubleArrayList();
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        boolean isBedrock = config != null && "Bedrock-Edition".equals(config.farlandsStyle);
        if (isBedrock) {
            // 强制截断为 32 位 float，模拟单精度输入
            amplitudeList.add((float) firstAmplitude);
            amplitudeList.add((float) amplitudes);
        } else {
            amplitudeList.add(firstAmplitude);
            amplitudeList.add(amplitudes);
        }
        return new PerlinNoise(random, Pair.of(firstOctave, amplitudeList), true);
    }

    // 修改：在 Bedrock 模式下截断已有的 DoubleList 中的每个元素
    public static PerlinNoise create(final RandomSource random, final int firstOctave, final DoubleList amplitudes) {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        boolean isBedrock = config != null && "Bedrock-Edition".equals(config.farlandsStyle);
        DoubleList finalAmplitudes = amplitudes;
        if (isBedrock) {
            DoubleArrayList truncated = new DoubleArrayList(amplitudes.size());
            for (int i = 0; i < amplitudes.size(); i++) {
                truncated.add((float) amplitudes.getDouble(i));
            }
            finalAmplitudes = truncated;
        }
        return new PerlinNoise(random, Pair.of(firstOctave, finalAmplitudes), true);
    }

    private static Pair<Integer, DoubleList> makeAmplitudes(final IntSortedSet octaveSet) {
        if (octaveSet.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        }

        int lowFreqOctaves = -octaveSet.firstInt();
        int highFreqOctaves = octaveSet.lastInt();
        int octaves = lowFreqOctaves + highFreqOctaves + 1;
        if (octaves < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
        }

        DoubleList amplitudes = new DoubleArrayList(new double[octaves]);
        IntBidirectionalIterator iterator = octaveSet.iterator();

        while (iterator.hasNext()) {
            int octave = iterator.nextInt();
            amplitudes.set(octave + lowFreqOctaves, 1.0);
        }

        return Pair.of(-lowFreqOctaves, amplitudes);
    }

    protected PerlinNoise(final RandomSource random, final Pair<
                    Integer, DoubleList> pair, final boolean useNewInitialization) {
        this.firstOctave = pair.getFirst();
        this.amplitudes = pair.getSecond();
        int octaves = this.amplitudes.size();
        int zeroOctaveIndex = -this.firstOctave;
        this.noiseLevels = new ImprovedNoise[octaves];

        // --- 提前获取 Bedrock 模式状态（避免在后续循环中重复调用实例方法） ---
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        boolean isBedrock = config != null && "Bedrock-Edition".equals(config.farlandsStyle);

        if (useNewInitialization) {
            PositionalRandomFactory positional = random.forkPositional();
            for (int i = 0; i < octaves; i++) {
                if (this.amplitudes.getDouble(i) != 0.0) {
                    int octave = this.firstOctave + i;
                    this.noiseLevels[
                    i] = new ImprovedNoise(positional.fromHashOf("octave_" + octave));
                }
            }
        } else {
            // === 旧版初始化逻辑（保持不变） ===
            ImprovedNoise zeroOctave = new ImprovedNoise(random);
            if (zeroOctaveIndex >= 0 && zeroOctaveIndex < octaves) {
                double zeroOctaveAmplitude = this.amplitudes.getDouble(zeroOctaveIndex);
                if (zeroOctaveAmplitude != 0.0) {
                    this.noiseLevels[zeroOctaveIndex] = zeroOctave;
                }
            }

            for (int i = zeroOctaveIndex - 1; i >= 0; i--) {
                if (i < octaves) {
                    double amplitude = this.amplitudes.getDouble(i);
                    if (amplitude != 0.0) {
                        this.noiseLevels[i] = new ImprovedNoise(random);
                    } else {
                        skipOctave(random);
                    }
                } else {
                    skipOctave(random);
                }
            }

            if (Arrays.stream(this.noiseLevels).filter(Objects
                            ::nonNull).count() != this.amplitudes.stream().filter(a -> a != 0.0).count()) {
                throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
            }

            if (zeroOctaveIndex < octaves - 1) {
                throw new IllegalArgumentException("Positive octaves are temporarily disabled");
            }
        }

        // === 核心修改：计算倍频因子时强制截断为 float 精度 ===
        double rawInputFactor = Math.pow(2.0, -zeroOctaveIndex);
        double rawValueFactor = Math.pow(2.0, octaves - 1) / (Math.pow(2.0, octaves) - 1.0);

        if (isBedrock) {
            // 模拟 26.3 快照：构造时就将因子截断为 32 位 float 残值，再存入 double 字段
            this.lowestFreqInputFactor = (float) rawInputFactor;
            this.lowestFreqValueFactor = (float) rawValueFactor;
        } else {
            this.lowestFreqInputFactor = rawInputFactor;
            this.lowestFreqValueFactor = rawValueFactor;
        }

        // maxValue 通过 edgeValue 计算，而 edgeValue 内部已根据 isBedrockMode() 自动切换精度
        // 因此此处无需额外强转，其结果自然符合 Bedrock 模式的截断规则
        this.maxValue = this.edgeValue(2.0);
    }

    // 修改：Bedrock 模式下重新计算并截断
    protected double maxValue() {
        if (isBedrockMode()) {
            return (float) edgeValue(2.0);
        }
        return this.maxValue;
    }

    private static void skipOctave(final RandomSource random) {
        random.consumeCount(262);
    }

    public double getValue(final double x, final double y, final double z) {
        return this.getValue(x, y, z, 0.0, 0.0);
    }

    @Deprecated
    public double getValue(final double x, final double y, final double z, final double yScale, final double yFudge) {
        if (isBedrockMode()) {
            // === Bedrock 模式：全部使用 float 精度计算 ===
            float fx = (float) x;
            float fy = (float) y;
            float fz = (float) z;
            float fyScale = (float) yScale;
            float fyFudge = (float) yFudge;

            float value = 0.0f;
            float factor = (float) this.lowestFreqInputFactor;
            float valueFactor = (float) this.lowestFreqValueFactor;

            for (int i = 0; i < this.noiseLevels.length; i++) {
                ImprovedNoise noise = this.noiseLevels[i];
                if (noise != null) {
                    // 注意 wrap 返回 double，但此处我们强制转为 float 参与乘法，模拟单精度计算
                    float wrapX = (float) wrap(fx * factor);
                    float wrapY = (float) wrap(fy * factor);
                    float wrapZ = (float) wrap(fz * factor);
                    float noiseVal = (float) noise.noise(wrapX, wrapY, wrapZ, fyScale * factor, fyFudge * factor);
                    value += (float) (this.amplitudes.getDouble(i) * noiseVal * valueFactor);
                }
                factor *= 2.0f;
                valueFactor /= 2.0f;
            }
            return (float) value;
        }

        // === 原 double 实现 ===
        double value = 0.0;
        double factor = this.lowestFreqInputFactor;
        double valueFactor = this.lowestFreqValueFactor;

        for (int i = 0; i < this.noiseLevels.length; i++) {
            ImprovedNoise noise = this.noiseLevels[i];
            if (noise != null) {
                double noiseVal = noise.noise(wrap(x * factor), wrap(y * factor), wrap(z * factor), yScale * factor, yFudge * factor);
                value += this.amplitudes.getDouble(i) * noiseVal * valueFactor;
            }

            factor *= 2.0;
            valueFactor /= 2.0;
        }

        return value;
    }

    public double maxBrokenValue(final double yScale) {
        if (isBedrockMode()) {
            float fYScale = (float) yScale;
            float val = (float) edgeValue(fYScale + 2.0f);
            return (float) val;
        }
        return edgeValue(yScale + 2.0);
    }

    private double edgeValue(final double noiseValue) {
        if (isBedrockMode()) {
            float fNoise = (float) noiseValue;
            float value = 0.0f;
            float valueFactor = (float) this.lowestFreqValueFactor;
            for (int i = 0; i < this.noiseLevels.length; i++) {
                ImprovedNoise noise = this.noiseLevels[i];
                if (noise != null) {
                    value += (float) (this.amplitudes.getDouble(i) * fNoise * valueFactor);
                }
                valueFactor /= 2.0f;
            }
            return (float) value;
        }

        double value = 0.0;
        double valueFactor = this.lowestFreqValueFactor;
        for (int i = 0; i < this.noiseLevels.length; i++) {
            ImprovedNoise noise = this.noiseLevels[i];
            if (noise != null) {
                value += this.amplitudes.getDouble(i) * noiseValue * valueFactor;
            }
            valueFactor /= 2.0;
        }
        return value;
    }

    public @Nullable ImprovedNoise getOctaveNoise(final int i) {
        return this.noiseLevels[this.noiseLevels.length - 1 - i];
    }

    // === 坐标折叠函数（保持不变） ===
    public static double computeReleaseValue(double x) {
        long l = Mth.lfloor(x);
        x -= l;
        l %= 16777216L;
        return x + l;
    }

    public static double wrap(final double x) {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        if (config == null) {
            return x;
        }
        double limitNoiseValue = config.limitReturnValueValue;
        String mode = config.precisionMode;
        double folded;
        switch (mode) {
            case "64bit":
            case "1.18-exp-64bit":
                folded = x - Mth.lfloor(x / 3.3554432E7 + 0.5) * 3.3554432E7;
                break;
            case "Release":
                folded = computeReleaseValue(x);
                break;
            default:
                folded = x;
                break;
        }
        // 🔧 限制逻辑：限制输入坐标量级（对数域折叠），支持任意实数等级（含小数、0）
        if (limitReturnValueMode()) {
            double abs = Math.abs(folded);
            // log10(0) = -Infinity，恒不满足 > limit，天然跳过，无需特判 0
            if (Math.log10(abs) > limitNoiseValue) {
                double logAbs = Math.log10(abs);
                folded = Math.pow(10, logAbs - Math.floor(logAbs - limitNoiseValue)) * Math.signum(folded);
            }
        }
        return folded;
    }

    protected int firstOctave() {
        return this.firstOctave;
    }

    protected DoubleList amplitudes() {
        return this.amplitudes;
    }

    @VisibleForTesting
    public void parityConfigString(final StringBuilder sb) {
        sb.append("PerlinNoise{");
        List<
                String> amplitudeStrings = this.amplitudes.stream().map(d -> String.format(Locale.ROOT, "%.2f", d)).toList();
        sb.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append(amplitudeStrings).append(", noise levels: [");

        for (int i = 0; i < this.noiseLevels.length; i++) {
            sb.append(i).append(": ");
            ImprovedNoise noiseLevel = this.noiseLevels[i];
            if (noiseLevel == null) {
                sb.append("null");
            } else {
                noiseLevel.parityConfigString(sb);
            }

            sb.append(", ");
        }

        sb.append("]");
        sb.append("}");
    }
}