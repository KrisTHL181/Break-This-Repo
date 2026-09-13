package net.minecraft.world.level.levelgen.synth;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;

public class PerlinSimplexNoise {
    private final @Nullable SimplexNoise[] noiseLevels;
    private final double highestFreqValueFactor;
    private final double highestFreqInputFactor;

    private static boolean isBedrockMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && ("Bedrock-Edition".equals(config.farlandsStyle));
    }

    public PerlinSimplexNoise(final RandomSource random, final List<Integer> octaveSet) {
        this(random, new IntRBTreeSet(octaveSet));
    }

    private PerlinSimplexNoise(final RandomSource random, final IntSortedSet octaveSet) {
        if (octaveSet.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        }

        int lowFreqOctaves = -octaveSet.firstInt();
        int highFreqOctaves = octaveSet.lastInt();
        int octaves = lowFreqOctaves + highFreqOctaves + 1;
        if (octaves < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
        }

        SimplexNoise zeroOctave = new SimplexNoise(random);
        int zeroOctaveIndex = highFreqOctaves;
        this.noiseLevels = new SimplexNoise[octaves];
        if (zeroOctaveIndex >= 0 && zeroOctaveIndex < octaves && octaveSet.contains(0)) {
            this.noiseLevels[zeroOctaveIndex] = zeroOctave;
        }

        for (int i = zeroOctaveIndex + 1; i < octaves; i++) {
            if (i >= 0 && octaveSet.contains(zeroOctaveIndex - i)) {
                this.noiseLevels[i] = new SimplexNoise(random);
            } else {
                random.consumeCount(262);
            }
        }

        if (highFreqOctaves > 0) {
            long positiveOctaveSeed = (long)(zeroOctave.getValue(zeroOctave.xo, zeroOctave.yo, zeroOctave.zo) * 9.223372E18F);
            RandomSource highFreqRandom = new WorldgenRandom(new LegacyRandomSource(positiveOctaveSeed));

            for (int i = zeroOctaveIndex - 1; i >= 0; i--) {
                if (i < octaves && octaveSet.contains(zeroOctaveIndex - i)) {
                    this.noiseLevels[i] = new SimplexNoise(highFreqRandom);
                } else {
                    highFreqRandom.consumeCount(262);
                }
            }
        }

        boolean isBedrock = isBedrockMode();

        // 修改：计算因子时在 Bedrock 模式下进行 float 截断
        double rawInputFactor = Math.pow(2.0, highFreqOctaves);
        double rawValueFactor = 1.0 / (Math.pow(2.0, octaves) - 1.0);

        if (isBedrock) {
            this.highestFreqInputFactor = (float) rawInputFactor;
            this.highestFreqValueFactor = (float) rawValueFactor;
        } else {
            this.highestFreqInputFactor = rawInputFactor;
            this.highestFreqValueFactor = rawValueFactor;
        }
    }

    public double getValue(final double x, final double y, final boolean useNoiseStart) {
        if (isBedrockMode()) {
            // === Bedrock 模式：全 float 精度 ===
            float fx = (float) x;
            float fy = (float) y;

            float value = 0.0f;
            float factor = (float) this.highestFreqInputFactor;
            float valueFactor = (float) this.highestFreqValueFactor;

            for (SimplexNoise noiseLevel : this.noiseLevels) {
                if (noiseLevel != null) {
                    float offsetX = useNoiseStart ? (float) noiseLevel.xo : 0.0f;
                    float offsetY = useNoiseStart ? (float) noiseLevel.yo : 0.0f;
                    float noiseVal = (float) noiseLevel.getValue(fx * factor + offsetX, fy * factor + offsetY);
                    value += noiseVal * valueFactor;
                }
                factor /= 2.0f;
                valueFactor *= 2.0f;
            }

            return (float) value;
        }

        // === 原 double 实现 ===
        double value = 0.0;
        double factor = this.highestFreqInputFactor;
        double valueFactor = this.highestFreqValueFactor;

        for (SimplexNoise noiseLevel : this.noiseLevels) {
            if (noiseLevel != null) {
                value += noiseLevel.getValue(x * factor + (useNoiseStart ? noiseLevel.xo : 0.0), y * factor + (useNoiseStart ? noiseLevel.yo : 0.0))
                    * valueFactor;
            }

            factor /= 2.0;
            valueFactor *= 2.0;
        }

        return value;
    }
}