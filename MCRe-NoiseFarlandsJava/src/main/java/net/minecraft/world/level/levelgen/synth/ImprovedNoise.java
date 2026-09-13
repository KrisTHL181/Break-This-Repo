package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;

public final class ImprovedNoise {
    private static final float SHIFT_UP_EPSILON = 1.0E-7F;
    private final byte[] p;
    public final double xo;
    public final double yo;
    public final double zo;

    private static boolean isBedrockMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && ("Bedrock-Edition".equals(config.farlandsStyle));
    }

    private static boolean isProgressiveFarlands() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.progressiveFarlands;
    }

    private static boolean simulatedWraparoundOverflowMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.simulatedWraparoundOverflow;
    }

    public ImprovedNoise(final RandomSource random) {
        this.xo = random.nextDouble() * 256.0;
        this.yo = random.nextDouble() * 256.0;
        this.zo = random.nextDouble() * 256.0;
        this.p = new byte[256];

        for (int i = 0; i < 256; i++) {
            this.p[i] = (byte) i;
        }

        for (int i = 0; i < 256; i++) {
            int offset = random.nextInt(256 - i);
            byte tmp = this.p[i];
            this.p[i] = this.p[i + offset];
            this.p[i + offset] = tmp;
        }
    }

    public double noise(final double _x, final double _y, final double _z) {
        if (isBedrockMode()) {
            return (float) this.noise(_x, _y, _z, 0.0, 0.0);
        }
        return this.noise(_x, _y, _z, 0.0, 0.0);
    }

    public double noise(final double _x, final double _y, final double _z, final double yScale, final double yFudge) {
        if (isBedrockMode()) {
            // === Bedrock 模式：全部使用 float 精度 ===
            float x = (float) (_x + this.xo);
            float y = (float) (_y + this.yo);
            float z = (float) (_z + this.zo);

            int xf, yf, zf;
            if (simulatedWraparoundOverflowMode()) {
                xf = floorToIntWithWrap(x);
                yf = floorToIntWithWrap(y);
                zf = floorToIntWithWrap(z);
            } else {
                xf = Mth.floor(x);
                yf = Mth.floor(y);
                zf = Mth.floor(z);
            }

            float xr = x - xf;
            float yr = y - yf;
            float zr = z - zf;
            float yrFudge;
            if (yScale != 0.0) {
                float fYScale = (float) yScale;
                float fYFudge = (float) yFudge;
                float fudgeLimit;
                if (fYFudge >= 0.0f && fYFudge < yr) {
                    fudgeLimit = fYFudge;
                } else {
                    fudgeLimit = yr;
                }
                yrFudge = Mth.floor(fudgeLimit / fYScale + 1.0E-7F) * fYScale;
            } else {
                yrFudge = 0.0f;
            }

            float result = (float) this.sampleAndLerp(xf, yf, zf, xr, yr - yrFudge, zr, yr);
            return (float) result;
        }

        // === 原 double 实现 ===
        double x = _x + this.xo;
        double y = _y + this.yo;
        double z = _z + this.zo;

        int xf, yf, zf;
        if (simulatedWraparoundOverflowMode()) {
            xf = floorToIntWithWrap(x);
            yf = floorToIntWithWrap(y);
            zf = floorToIntWithWrap(z);
        } else {
            xf = Mth.floor(x);
            yf = Mth.floor(y);
            zf = Mth.floor(z);
        }

        double xr = x - xf;
        double yr = y - yf;
        double zr = z - zf;
        double yrFudge;
        if (yScale != 0.0) {
            double fudgeLimit;
            if (yFudge >= 0.0 && yFudge < yr) {
                fudgeLimit = yFudge;
            } else {
                fudgeLimit = yr;
            }
            yrFudge = Mth.floor(fudgeLimit / yScale + 1.0E-7F) * yScale;
        } else {
            yrFudge = 0.0;
        }

        double result = this.sampleAndLerp(xf, yf, zf, xr, yr - yrFudge, zr, yr);
        return result;
    }

    public double noiseWithDerivative(final double _x, final double _y, final double _z, final double[] derivativeOut) {
        if (isBedrockMode()) {
            // Bedrock 模式：全部使用 float，但 derivativeOut 保留 double 数组签名（外部可能期望 double）
            // 内部计算用 float，最后赋值时强转回 double
            float x = (float) (_x + this.xo);
            float y = (float) (_y + this.yo);
            float z = (float) (_z + this.zo);
            int xf = Mth.floor(x);
            int yf = Mth.floor(y);
            int zf = Mth.floor(z);
            float xr = x - xf;
            float yr = y - yf;
            float zr = z - zf;

            // 临时 float 数组用于内部计算
            double[] derivTemp = new double[3];
            float result = (float) this.sampleWithDerivative(xf, yf, zf, xr, yr, zr, derivTemp);
            derivativeOut[0] += derivTemp[0];
            derivativeOut[1] += derivTemp[1];
            derivativeOut[2] += derivTemp[2];
            return (float) result;
        }

        // 原 double 实现
        double x = _x + this.xo;
        double y = _y + this.yo;
        double z = _z + this.zo;
        int xf = Mth.floor(x);
        int yf = Mth.floor(y);
        int zf = Mth.floor(z);
        double xr = x - xf;
        double yr = y - yf;
        double zr = z - zf;
        return this.sampleWithDerivative(xf, yf, zf, xr, yr, zr, derivativeOut);
    }

    private static double gradDot(final int hash, final double x, final double y, final double z) {
        if (isBedrockMode()) {
            return (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    hash & 15], (float) x, (float) y, (float) z);
        }
        return SimplexNoise.dot(SimplexNoise.GRADIENT[hash & 15], x, y, z);
    }

    private int p(final int x) {
        return this.p[x & 0xFF] & 0xFF;
    }

    // ========== 插值工具 ==========
    private static double lerp(double delta, double start, double end) {
        if (isProgressiveFarlands()) {
            return start;
        }
        if (isBedrockMode()) {
            float fDelta = (float) delta;
            float fStart = (float) start;
            float fEnd = (float) end;
            return (float) (fStart + fDelta * (fEnd - fStart));
        }
        return start + delta * (end - start);
    }

    private static double lerp2(double delta1, double delta2,
            double start1, double end1,
            double start2, double end2) {
        if (isBedrockMode()) {
            float fDelta1 = (float) delta1;
            float fDelta2 = (float) delta2;
            float mid1 = (float) lerp(fDelta1, start1, end1);
            float mid2 = (float) lerp(fDelta1, start2, end2);
            return (float) lerp(fDelta2, mid1, mid2);
        }
        double mid1 = lerp(delta1, start1, end1);
        double mid2 = lerp(delta1, start2, end2);
        return lerp(delta2, mid1, mid2);
    }

    private static double lerp3(double delta1, double delta2, double delta3,
            double v000, double v100, double v010, double v110,
            double v001, double v101, double v011, double v111) {
        if (isBedrockMode()) {
            float fDelta1 = (float) delta1;
            float fDelta2 = (float) delta2;
            float fDelta3 = (float) delta3;
            float x00 = (float) lerp(fDelta1, v000, v100);
            float x10 = (float) lerp(fDelta1, v010, v110);
            float x01 = (float) lerp(fDelta1, v001, v101);
            float x11 = (float) lerp(fDelta1, v011, v111);
            float y0 = (float) lerp(fDelta2, x00, x10);
            float y1 = (float) lerp(fDelta2, x01, x11);
            return (float) lerp(fDelta3, y0, y1);
        }
        double x00 = lerp(delta1, v000, v100);
        double x10 = lerp(delta1, v010, v110);
        double x01 = lerp(delta1, v001, v101);
        double x11 = lerp(delta1, v011, v111);
        double y0 = lerp(delta2, x00, x10);
        double y1 = lerp(delta2, x01, x11);
        return lerp(delta3, y0, y1);
    }

    private double sampleAndLerp(final int x, final int y, final int z, final double xr, final double yr, final double zr, final double yrOriginal) {
        int x0 = this.p(x);
        int x1 = this.p(x + 1);
        int xy00 = this.p(x0 + y);
        int xy01 = this.p(x0 + y + 1);
        int xy10 = this.p(x1 + y);
        int xy11 = this.p(x1 + y + 1);

        double d000 = gradDot(this.p(xy00 + z), xr, yr, zr);
        double d100 = gradDot(this.p(xy10 + z), xr - 1.0, yr, zr);
        double d010 = gradDot(this.p(xy01 + z), xr, yr - 1.0, zr);
        double d110 = gradDot(this.p(xy11 + z), xr - 1.0, yr - 1.0, zr);
        double d001 = gradDot(this.p(xy00 + z + 1), xr, yr, zr - 1.0);
        double d101 = gradDot(this.p(xy10 + z + 1), xr - 1.0, yr, zr - 1.0);
        double d011 = gradDot(this.p(xy01 + z + 1), xr, yr - 1.0, zr - 1.0);
        double d111 = gradDot(this.p(xy11 + z + 1), xr - 1.0, yr - 1.0, zr - 1.0);

        double xAlpha = Mth.smoothstep(xr);
        double yAlpha = Mth.smoothstep(yrOriginal);
        double zAlpha = Mth.smoothstep(zr);

        if (isBedrockMode()) {
            return (float) ImprovedNoise.lerp3(
                            (float) xAlpha, (float) yAlpha, (float) zAlpha,
                            (float) d000, (float) d100, (float) d010, (float) d110,
                            (float) d001, (float) d101, (float) d011, (float) d111
                    );
        }
        return ImprovedNoise.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    private double sampleWithDerivative(final int x, final int y, final int z, final double xr, final double yr, final double zr, final double[] derivativeOut) {
        int x0 = this.p(x);
        int x1 = this.p(x + 1);
        int xy00 = this.p(x0 + y);
        int xy01 = this.p(x0 + y + 1);
        int xy10 = this.p(x1 + y);
        int xy11 = this.p(x1 + y + 1);
        int p000 = this.p(xy00 + z);
        int p100 = this.p(xy10 + z);
        int p010 = this.p(xy01 + z);
        int p110 = this.p(xy11 + z);
        int p001 = this.p(xy00 + z + 1);
        int p101 = this.p(xy10 + z + 1);
        int p011 = this.p(xy01 + z + 1);
        int p111 = this.p(xy11 + z + 1);

        if (isBedrockMode()) {
            // === Bedrock 模式：全 float 计算导数 ===
            float fxr = (float) xr;
            float fyr = (float) yr;
            float fzr = (float) zr;

            float d000 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[p000 & 15], fxr, fyr, fzr);
            float d100 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    p100 & 15], fxr - 1.0f, fyr, fzr);
            float d010 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    p010 & 15], fxr, fyr - 1.0f, fzr);
            float d110 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    p110 & 15], fxr - 1.0f, fyr - 1.0f, fzr);
            float d001 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    p001 & 15], fxr, fyr, fzr - 1.0f);
            float d101 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    p101 & 15], fxr - 1.0f, fyr, fzr - 1.0f);
            float d011 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    p011 & 15], fxr, fyr - 1.0f, fzr - 1.0f);
            float d111 = (float) SimplexNoise.dot(SimplexNoise.GRADIENT[
                    p111 & 15], fxr - 1.0f, fyr - 1.0f, fzr - 1.0f);

            float xAlpha = (float) Mth.smoothstep(fxr);
            float yAlpha = (float) Mth.smoothstep(fyr);
            float zAlpha = (float) Mth.smoothstep(fzr);

            // 梯度向量（需手动构建 float 数组）
            float[] g000 = toFloatArray(SimplexNoise.GRADIENT[p000 & 15]);
            float[] g100 = toFloatArray(SimplexNoise.GRADIENT[p100 & 15]);
            float[] g010 = toFloatArray(SimplexNoise.GRADIENT[p010 & 15]);
            float[] g110 = toFloatArray(SimplexNoise.GRADIENT[p110 & 15]);
            float[] g001 = toFloatArray(SimplexNoise.GRADIENT[p001 & 15]);
            float[] g101 = toFloatArray(SimplexNoise.GRADIENT[p101 & 15]);
            float[] g011 = toFloatArray(SimplexNoise.GRADIENT[p011 & 15]);
            float[] g111 = toFloatArray(SimplexNoise.GRADIENT[p111 & 15]);

            float d1x = lerp3Float(xAlpha, yAlpha, zAlpha, g000[0], g100[0], g010[0], g110[0], g001[
            0], g101[0], g011[0], g111[0]);
            float d1y = lerp3Float(xAlpha, yAlpha, zAlpha, g000[1], g100[1], g010[1], g110[1], g001[
            1], g101[1], g011[1], g111[1]);
            float d1z = lerp3Float(xAlpha, yAlpha, zAlpha, g000[2], g100[2], g010[2], g110[2], g001[
            2], g101[2], g011[2], g111[2]);

            float d2x = lerp2Float(yAlpha, zAlpha, d100 - d000, d110 - d010, d101 - d001, d111 - d011);
            float d2y = lerp2Float(zAlpha, xAlpha, d010 - d000, d011 - d001, d110 - d100, d111 - d101);
            float d2z = lerp2Float(xAlpha, yAlpha, d001 - d000, d101 - d100, d011 - d010, d111 - d110);

            float xSD = (float) Mth.smoothstepDerivative(fxr);
            float ySD = (float) Mth.smoothstepDerivative(fyr);
            float zSD = (float) Mth.smoothstepDerivative(fzr);

            float dX = d1x + xSD * d2x;
            float dY = d1y + ySD * d2y;
            float dZ = d1z + zSD * d2z;

            derivativeOut[0] += dX;
            derivativeOut[1] += dY;
            derivativeOut[2] += dZ;

            float result = lerp3Float(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
            return (float) result;
        }

        // === 原 double 实现 ===
        int[] g000 = SimplexNoise.GRADIENT[p000 & 15];
        int[] g100 = SimplexNoise.GRADIENT[p100 & 15];
        int[] g010 = SimplexNoise.GRADIENT[p010 & 15];
        int[] g110 = SimplexNoise.GRADIENT[p110 & 15];
        int[] g001 = SimplexNoise.GRADIENT[p001 & 15];
        int[] g101 = SimplexNoise.GRADIENT[p101 & 15];
        int[] g011 = SimplexNoise.GRADIENT[p011 & 15];
        int[] g111 = SimplexNoise.GRADIENT[p111 & 15];

        double d000 = SimplexNoise.dot(g000, xr, yr, zr);
        double d100 = SimplexNoise.dot(g100, xr - 1.0, yr, zr);
        double d010 = SimplexNoise.dot(g010, xr, yr - 1.0, zr);
        double d110 = SimplexNoise.dot(g110, xr - 1.0, yr - 1.0, zr);
        double d001 = SimplexNoise.dot(g001, xr, yr, zr - 1.0);
        double d101 = SimplexNoise.dot(g101, xr - 1.0, yr, zr - 1.0);
        double d011 = SimplexNoise.dot(g011, xr, yr - 1.0, zr - 1.0);
        double d111 = SimplexNoise.dot(g111, xr - 1.0, yr - 1.0, zr - 1.0);

        double xAlpha = Mth.smoothstep(xr);
        double yAlpha = Mth.smoothstep(yr);
        double zAlpha = Mth.smoothstep(zr);

        double d1x = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[0], g100[0], g010[0], g110[0], g001[
        0], g101[0], g011[0], g111[0]);
        double d1y = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[1], g100[1], g010[1], g110[1], g001[
        1], g101[1], g011[1], g111[1]);
        double d1z = Mth.lerp3(xAlpha, yAlpha, zAlpha, g000[2], g100[2], g010[2], g110[2], g001[
        2], g101[2], g011[2], g111[2]);

        double d2x = Mth.lerp2(yAlpha, zAlpha, d100 - d000, d110 - d010, d101 - d001, d111 - d011);
        double d2y = Mth.lerp2(zAlpha, xAlpha, d010 - d000, d011 - d001, d110 - d100, d111 - d101);
        double d2z = Mth.lerp2(xAlpha, yAlpha, d001 - d000, d101 - d100, d011 - d010, d111 - d110);

        double xSD = Mth.smoothstepDerivative(xr);
        double ySD = Mth.smoothstepDerivative(yr);
        double zSD = Mth.smoothstepDerivative(zr);

        double dX = d1x + xSD * d2x;
        double dY = d1y + ySD * d2y;
        double dZ = d1z + zSD * d2z;

        derivativeOut[0] += dX;
        derivativeOut[1] += dY;
        derivativeOut[2] += dZ;

        return ImprovedNoise.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    // ========== Float 版插值辅助（仅 Bedrock 模式使用） ==========
    private static float lerpFloat(float delta, float start, float end) {
        if (isProgressiveFarlands()) {
            return start;
        }
        return start + delta * (end - start);
    }

    private static float lerp2Float(float delta1, float delta2,
            float start1, float end1,
            float start2, float end2) {
        float mid1 = lerpFloat(delta1, start1, end1);
        float mid2 = lerpFloat(delta1, start2, end2);
        return lerpFloat(delta2, mid1, mid2);
    }

    private static float lerp3Float(float delta1, float delta2, float delta3,
            float v000, float v100, float v010, float v110,
            float v001, float v101, float v011, float v111) {
        float x00 = lerpFloat(delta1, v000, v100);
        float x10 = lerpFloat(delta1, v010, v110);
        float x01 = lerpFloat(delta1, v001, v101);
        float x11 = lerpFloat(delta1, v011, v111);
        float y0 = lerpFloat(delta2, x00, x10);
        float y1 = lerpFloat(delta2, x01, x11);
        return lerpFloat(delta3, y0, y1);
    }

    private static float[] toFloatArray(int[] arr) {
        return new float[]{(float) arr[0], (float) arr[1], (float) arr[2]};
    }

    @VisibleForTesting
    public void parityConfigString(final StringBuilder sb) {
        NoiseUtils.parityNoiseOctaveConfigString(sb, this.xo, this.yo, this.zo, this.p);
    }

    private static int floorToIntWithWrap(double val) {
        if (val >= Integer.MAX_VALUE || val <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) Math.floor(val);
    }
}