package net.minecraft.world.level.levelgen;

import com.google.common.collect.Comparators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.core.BlockPos;
import net.minecraft.util.MathUtil;
import net.minecraft.client.gui.screens.worldselection.WorldMainSettingScreen;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.MinecraftTools.Math.DynamicAccuracy.BigDecimal;
import net.MinecraftTools.Math.DynamicAccuracy.BigInteger;
import org.slf4j.Logger;

public final class DensityFunctions {
    private static boolean isForceSkyGrid() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.forceSkyGrid;
    }

    private static boolean fixEndRingMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.fixEndRings;
    }

    private static boolean expandDatapackValueRangeMode() {
        WorldMainSettingScreen.FarLandsConfigData config = WorldMainSettingScreen.FarLandsConfigData.activeConfig;
        return config != null && config.expandDatapackValueRange;
    }

    private static final Codec<DensityFunction> CODEC = BuiltInRegistries.DENSITY_FUNCTION_TYPE
            .byNameCodec()
            .dispatch(function -> function.codec().codec(), Function.identity());
    static final double MAX_REASONABLE_NOISE_VALUE = Double.POSITIVE_INFINITY;
    private static Codec<Double> NOISE_VALUE_CODEC = createCodec();

    // 工厂方法：根据当前开关生成 Codec
    private static Codec<Double> createCodec() {
        if (expandDatapackValueRangeMode()) {
            return Codec.doubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        } else {
            // 使用原版范围，自行替换为实际值
            return Codec.doubleRange(-1000000, 1000000);
        }
    }

    // 当配置改变时，外部调用此方法刷新 Codec
    public static void refreshCodec() {
        NOISE_VALUE_CODEC = createCodec();
    }

    public static final Codec<DensityFunction> DIRECT_CODEC = Codec.either(NOISE_VALUE_CODEC, CODEC)
            .xmap(
                    either -> either.map(DensityFunctions::constant, Function.identity()),
                    function -> function
                                    instanceof
                                    DensityFunctions.Constant constant ? Either.left(constant.value()) : Either.right(function)
            );

    public static MapCodec<? extends DensityFunction> bootstrap(final Registry<
                    MapCodec<? extends DensityFunction>> registry) {
        register(registry, "blend_alpha", DensityFunctions.BlendAlpha.CODEC);
        register(registry, "blend_offset", DensityFunctions.BlendOffset.CODEC);
        register(registry, "beardifier", DensityFunctions.BeardifierMarker.CODEC);
        register(registry, "old_blended_noise", BlendedNoise.CODEC);

        for (DensityFunctions.Marker.Type value : DensityFunctions.Marker.Type.values()) {
            register(registry, value.getSerializedName(), value.codec);
        }

        register(registry, "noise", DensityFunctions.Noise.CODEC);
        register(registry, "end_islands", DensityFunctions.EndIslandDensityFunction.CODEC);
        register(registry, "shifted_noise", DensityFunctions.ShiftedNoise.CODEC);
        register(registry, "range_choice", DensityFunctions.RangeChoice.CODEC);
        register(registry, "interval_select", DensityFunctions.IntervalSelect.CODEC);
        register(registry, "shift_a", DensityFunctions.ShiftA.CODEC);
        register(registry, "shift_b", DensityFunctions.ShiftB.CODEC);
        register(registry, "shift", DensityFunctions.Shift.CODEC);
        register(registry, "clamp", DensityFunctions.Clamp.CODEC);

        for (DensityFunctions.Mapped.Type value : DensityFunctions.Mapped.Type.values()) {
            register(registry, value.getSerializedName(), value.codec);
        }

        for (DensityFunctions.TwoArgumentSimpleFunction.Type value : DensityFunctions.TwoArgumentSimpleFunction.Type.values()) {
            register(registry, value.getSerializedName(), value.codec);
        }

        register(registry, "spline", DensityFunctions.Spline.CODEC);
        register(registry, "constant", DensityFunctions.Constant.CODEC);
        register(registry, "y_clamped_gradient", DensityFunctions.YClampedGradient.CODEC);
        return register(registry, "find_top_surface", DensityFunctions.FindTopSurface.CODEC);
    }

    private static MapCodec<? extends DensityFunction> register(
            final Registry<
                    MapCodec<
                            ? extends
                                    DensityFunction>> registry, final String name, final KeyDispatchDataCodec<
                    ? extends DensityFunction> codec) {
        return Registry.register(registry, name, codec.codec());
    }

    private static <A, O> KeyDispatchDataCodec<O> singleArgumentCodec(
            final Codec<A> argumentCodec, final Function<A, O> constructor, final Function<
                    O, A> getter) {
        return KeyDispatchDataCodec.of(argumentCodec.fieldOf("argument").xmap(constructor, getter));
    }

    private static <O> KeyDispatchDataCodec<O> singleFunctionArgumentCodec(
            final Function<DensityFunction, O> constructor, final Function<
                    O, DensityFunction> getter) {
        return singleArgumentCodec(DensityFunction.CODEC, constructor, getter);
    }

    private static <O> KeyDispatchDataCodec<O> doubleFunctionArgumentCodec(
            final BiFunction<DensityFunction, DensityFunction, O> constructor,
            final Function<O, DensityFunction> firstArgumentGetter,
            final Function<O, DensityFunction> secondArgumentGetter) {
        return KeyDispatchDataCodec.of(
                RecordCodecBuilder.mapCodec(
                        i -> i.group(
                                DensityFunction.CODEC.fieldOf("argument1").forGetter(firstArgumentGetter),
                                DensityFunction.CODEC.fieldOf("argument2").forGetter(secondArgumentGetter)
                        )
                                .apply(i, constructor)
                )
        );
    }

    private static <O> KeyDispatchDataCodec<O> makeCodec(final MapCodec<O> dataCodec) {
        return KeyDispatchDataCodec.of(dataCodec);
    }

    private DensityFunctions() {}

    public static DensityFunction interpolated(final DensityFunction function) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Interpolated, function);
    }

    public static DensityFunction flatCache(final DensityFunction function) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.FlatCache, function);
    }

    public static DensityFunction cache2d(final DensityFunction function) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.Cache2D, function);
    }

    public static DensityFunction cacheOnce(final DensityFunction function) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheOnce, function);
    }

    public static DensityFunction cacheAllInCell(final DensityFunction function) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.CacheAllInCell, function);
    }

    public static DensityFunction mappedNoise(
            final Holder<NormalNoise.NoiseParameters> noiseData,
            @Deprecated final double xzScale,
            final double yScale,
            final double minTarget,
            final double maxTarget) {
        return mapFromUnitTo(new DensityFunctions.Noise(new DensityFunction.NoiseHolder(noiseData), xzScale, yScale), minTarget, maxTarget);
    }

    public static DensityFunction mappedNoise(
            final Holder<
                    NormalNoise.NoiseParameters> noiseData, final double yScale, final double minTarget, final double maxTarget) {
        return mappedNoise(noiseData, 1.0, yScale, minTarget, maxTarget);
    }

    public static DensityFunction mappedNoise(final Holder<
                    NormalNoise.NoiseParameters> noiseData, final double minTarget, final double maxTarget) {
        return mappedNoise(noiseData, 1.0, 1.0, minTarget, maxTarget);
    }

    public static DensityFunction shiftedNoise2d(
            final DensityFunction shiftX, final DensityFunction shiftZ, final double xzScale, final Holder<
                    NormalNoise.NoiseParameters> noiseData) {
        return new DensityFunctions.ShiftedNoise(shiftX, zero(), shiftZ, xzScale, 0.0, new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction noise(final Holder<NormalNoise.NoiseParameters> noiseData) {
        return noise(noiseData, 1.0, 1.0);
    }

    public static DensityFunction noise(final Holder<
                    NormalNoise.NoiseParameters> noiseData, final double xzScale, final double yScale) {
        return new DensityFunctions.Noise(new DensityFunction.NoiseHolder(noiseData), xzScale, yScale);
    }

    public static DensityFunction noise(final Holder<
                    NormalNoise.NoiseParameters> noiseData, final double yScale) {
        return noise(noiseData, 1.0, yScale);
    }

    public static DensityFunction rangeChoice(
            final DensityFunction input,
            final double minInclusive,
            final double maxExclusive,
            final DensityFunction whenInRange,
            final DensityFunction whenOutOfRange) {
        return new DensityFunctions.RangeChoice(input, minInclusive, maxExclusive, whenInRange, whenOutOfRange);
    }

    public static DensityFunction intervalSelect(final DensityFunction input, final DoubleList thresholds, final List<
                    DensityFunction> functions) {
        return new DensityFunctions.IntervalSelect(input, thresholds, functions);
    }

    public static DensityFunction shiftA(final Holder<NormalNoise.NoiseParameters> noiseData) {
        return new DensityFunctions.ShiftA(new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction shiftB(final Holder<NormalNoise.NoiseParameters> noiseData) {
        return new DensityFunctions.ShiftB(new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction shift(final Holder<NormalNoise.NoiseParameters> noiseData) {
        return new DensityFunctions.Shift(new DensityFunction.NoiseHolder(noiseData));
    }

    public static DensityFunction blendDensity(final DensityFunction input) {
        return new DensityFunctions.Marker(DensityFunctions.Marker.Type.BlendDensity, input);
    }

    public static DensityFunction endIslands(final long seed) {
        return new DensityFunctions.EndIslandDensityFunction(seed);
    }

    public static DensityFunction add(final DensityFunction f1, final DensityFunction f2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.ADD, f1, f2);
    }

    public static DensityFunction mul(final DensityFunction f1, final DensityFunction f2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MUL, f1, f2);
    }

    public static DensityFunction min(final DensityFunction f1, final DensityFunction f2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MIN, f1, f2);
    }

    public static DensityFunction max(final DensityFunction f1, final DensityFunction f2) {
        return DensityFunctions.TwoArgumentSimpleFunction.create(DensityFunctions.TwoArgumentSimpleFunction.Type.MAX, f1, f2);
    }

    public static DensityFunction spline(final CubicSpline<
                    DensityFunctions.Spline.Coordinate> spline) {
        return new DensityFunctions.Spline(spline);
    }

    public static DensityFunction zero() {
        return DensityFunctions.Constant.ZERO;
    }

    public static DensityFunction constant(final double value) {
        return new DensityFunctions.Constant(value);
    }

    public static DensityFunction yClampedGradient(final int fromY, final int toY, final double fromValue, final double toValue) {
        return new DensityFunctions.YClampedGradient(fromY, toY, fromValue, toValue);
    }

    public static DensityFunction map(final DensityFunction function, final DensityFunctions.Mapped.Type type) {
        return DensityFunctions.Mapped.create(type, function);
    }

    private static DensityFunction mapFromUnitTo(final DensityFunction function, final double min, final double max) {
        double middle = (min + max) * 0.5;
        double factor = (max - min) * 0.5;
        return add(constant(middle), mul(constant(factor), function));
    }

    public static DensityFunction blendAlpha() {
        return DensityFunctions.BlendAlpha.INSTANCE;
    }

    public static DensityFunction blendOffset() {
        return DensityFunctions.BlendOffset.INSTANCE;
    }

    public static DensityFunction lerp(final DensityFunction alpha, final DensityFunction first, final DensityFunction second) {
        if (first instanceof DensityFunctions.Constant constant) {
            return lerp(alpha, constant.value, second);
        } else {
            DensityFunction alphaCached = cacheOnce(alpha);
            DensityFunction oneMinusAlpha = add(mul(alphaCached, constant(-1.0)), constant(1.0));
            return add(mul(first, oneMinusAlpha), mul(second, alphaCached));
        }
    }

    public static DensityFunction lerp(final DensityFunction factor, final double first, final DensityFunction second) {
        return add(mul(factor, add(second, constant(-first))), constant(first));
    }

    public static DensityFunction findTopSurface(final DensityFunction density, final DensityFunction upperBound, final int lowerBound, final int stepSize) {
        return new DensityFunctions.FindTopSurface(density, upperBound, lowerBound, stepSize);
    }

    private record Ap2(
            DensityFunctions.TwoArgumentSimpleFunction.Type type, DensityFunction argument1, DensityFunction argument2, double minValue, double maxValue)
            implements DensityFunctions.TwoArgumentSimpleFunction {
        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            double v1 = this.argument1.compute(context);

            return switch (this.type) {
                case ADD -> v1 + this.argument2.compute(context);
                case MUL -> v1 == 0.0 ? 0.0 : v1 * this.argument2.compute(context);
                case MIN ->
                        v1 < this.argument2.minValue() ? v1 : Math.min(v1, this.argument2.compute(context));
                case MAX ->
                        v1 > this.argument2.maxValue() ? v1 : Math.max(v1, this.argument2.compute(context));
            };
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            this.argument1.fillArray(output, contextProvider);
            switch (this.type) {
                case ADD:
                    double[] v2 = new double[output.length];
                    this.argument2.fillArray(v2, contextProvider);

                    for (int i = 0; i < output.length; i++) {
                        output[i] += v2[i];
                    }
                    break;
                case MUL:
                    for (int i = 0; i < output.length; i++) {
                        double v = output[i];
                        output[
                        i] = v == 0.0 ? 0.0 : v * this.argument2.compute(contextProvider.forIndex(i));
                    }
                    break;
                case MIN:
                    double min = this.argument2.minValue();

                    for (int i = 0; i < output.length; i++) {
                        double v = output[i];
                        output[
                        i] = v < min ? v : Math.min(v, this.argument2.compute(contextProvider.forIndex(i)));
                    }
                    break;
                case MAX:
                    double max = this.argument2.maxValue();

                    for (int i = 0; i < output.length; i++) {
                        double v = output[i];
                        output[
                        i] = v > max ? v : Math.max(v, this.argument2.compute(contextProvider.forIndex(i)));
                    }
            }
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return DensityFunctions.TwoArgumentSimpleFunction.create(this.type, visitor.apply(this.argument1), visitor.apply(this.argument2));
        }
    }

    enum BeardifierMarker implements DensityFunctions.BeardifierOrMarker {
        INSTANCE;

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return 0.0;
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, 0.0);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }
    }

    public interface BeardifierOrMarker extends DensityFunction.SimpleFunction {
        KeyDispatchDataCodec<
                DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(DensityFunctions.BeardifierMarker.INSTANCE));

        @Override
        default KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    enum BlendAlpha implements DensityFunction.SimpleFunction {
        INSTANCE;

        public static final KeyDispatchDataCodec<
                DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return 1.0;
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, 1.0);
        }

        @Override
        public double minValue() {
            return 1.0;
        }

        @Override
        public double maxValue() {
            return 1.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    enum BlendOffset implements DensityFunction.SimpleFunction {
        INSTANCE;

        public static final KeyDispatchDataCodec<
                DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return 0.0;
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, 0.0);
        }

        @Override
        public double minValue() {
            return 0.0;
        }

        @Override
        public double maxValue() {
            return 0.0;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record Clamp(DensityFunction input, double minValue, double maxValue)
            implements DensityFunctions.PureTransformer {
        private static final MapCodec<
                DensityFunctions.Clamp> DATA_CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        DensityFunction.CODEC.fieldOf("input").forGetter(DensityFunctions.Clamp
                                ::input),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min").forGetter(DensityFunctions.Clamp
                                ::minValue),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max").forGetter(DensityFunctions.Clamp
                                ::maxValue)
                )
                        .apply(i, DensityFunctions.Clamp::new)
        );
        public static final KeyDispatchDataCodec<
                DensityFunctions.Clamp> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double transform(final double input) {
            if (isForceSkyGrid()) {
                return MathUtil.clamp(input, this.minValue, this.maxValue);
            }
            return Mth.clamp(input, this.minValue, this.maxValue);
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.Clamp(visitor.apply(this.input), this.minValue, this.maxValue);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private record Constant(double value) implements DensityFunction.SimpleFunction {
        private static final KeyDispatchDataCodec<
                DensityFunctions.Constant> CODEC = DensityFunctions.singleArgumentCodec(
                DensityFunctions.NOISE_VALUE_CODEC, DensityFunctions.Constant
                        ::new, DensityFunctions.Constant::value
        );
        private static final DensityFunctions.Constant ZERO = new DensityFunctions.Constant(0.0);

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return this.value;
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            Arrays.fill(output, this.value);
        }

        @Override
        public double minValue() {
            return this.value;
        }

        @Override
        public double maxValue() {
            return this.value;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected static final class EndIslandDensityFunction
            implements DensityFunction.SimpleFunction {
        public static final KeyDispatchDataCodec<
                DensityFunctions.EndIslandDensityFunction> CODEC = KeyDispatchDataCodec.of(
                MapCodec.unit(new DensityFunctions.EndIslandDensityFunction(0L))
        );
        private static final float ISLAND_THRESHOLD = -0.9F;
        private final SimplexNoise islandNoise;

        public EndIslandDensityFunction(final long seed) {
            RandomSource islandRandom = new LegacyRandomSource(seed);
            islandRandom.consumeCount(17292);
            this.islandNoise = new SimplexNoise(islandRandom);
        }

        private static float getHeightValue(final SimplexNoise islandNoise, final int sectionX, final int sectionZ) {
            // 🔧 MCRe NoiseFarlands：WorldReposition 偏移（自研 BigDecimal，无大小限制）
            // 公式：offsetX = sectionX * scaleX + shiftX（BigDecimal 精确）
            // 然后 /16 = 原始 /8 后 /2（chunkX），/8 后 % 2 = subSectionX
            final BigInteger offsetX = WorldReposition.reposition(BigDecimal.valueOf(sectionX), Direction.Axis.X).toBigInteger();
            final BigInteger offsetZ = WorldReposition.reposition(BigDecimal.valueOf(sectionZ), Direction.Axis.Z).toBigInteger();
            final BigInteger SIXTEEN = BigInteger.valueOf(16);
            final BigInteger EIGHT = BigInteger.valueOf(8);
            final BigInteger TWO = BigInteger.valueOf(2);
            int chunkX = offsetX.divide(SIXTEEN).intValue();
            int chunkZ = offsetZ.divide(SIXTEEN).intValue();
            int subSectionX = offsetX.divide(EIGHT).remainder(TWO).intValue();
            int subSectionZ = offsetZ.divide(EIGHT).remainder(TWO).intValue();
            float doffs;
            if (fixEndRingMode()) {
                // 🔧 MCRe：BigInteger 路径，无 int 溢出 + 应用 WorldReposition 偏移缩放
                // 公式：sqrtArg = (reposition(sectionX) / 8)² + (reposition(sectionZ) / 8)²
                // /8 还原 sectionX/8 域（与 chunkX*2 一致）；BigInteger.multiply 无溢出
                final BigInteger sqrtOffsetX = WorldReposition.reposition(BigDecimal.valueOf(sectionX), Direction.Axis.X).toBigInteger();
                final BigInteger sqrtOffsetZ = WorldReposition.reposition(BigDecimal.valueOf(sectionZ), Direction.Axis.Z).toBigInteger();
                final BigInteger sx8 = sqrtOffsetX.divide(EIGHT);
                final BigInteger sz8 = sqrtOffsetZ.divide(EIGHT);
                final BigInteger sumSq = sx8.multiply(sx8).add(sz8.multiply(sz8));
                doffs = 100.0F - Mth.sqrt(sumSq.floatValue()) * 8.0F;
            } else {
                doffs = 100.0F - Mth.sqrt(sectionX * sectionX + sectionZ * sectionZ) * 8.0F;
            }
            doffs = Mth.clamp(doffs, -100.0F, 80.0F);

            for (int xo = -12; xo <= 12; xo++) {
                for (int zo = -12; zo <= 12; zo++) {
                    long totalChunkX = chunkX + xo;
                    long totalChunkZ = chunkZ + zo;
                    if (totalChunkX * totalChunkX + totalChunkZ * totalChunkZ > 4096L && islandNoise.getValue(totalChunkX, totalChunkZ) < -0.9F) {
                        float islandSize = (Mth.abs((float) totalChunkX) * 3439.0F + Mth.abs((float) totalChunkZ) * 147.0F) % 13.0F + 9.0F;
                        float xd = subSectionX - xo * 2;
                        float zd = subSectionZ - zo * 2;
                        BlockPos.MutableBlockPos blockPos;
                        float newDoffs = 100.0F - Mth.sqrt(xd * xd + zd * zd) * islandSize;
                        newDoffs = Mth.clamp(newDoffs, -100.0F, 80.0F);
                        doffs = Math.max(doffs, newDoffs);
                    }
                }
            }

            return doffs;
        }

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            // 🔧 MCRe NoiseFarlands：传原始坐标进 getHeightValue，由内部 BigInteger 偏移后还原 /8
            return (getHeightValue(this.islandNoise, context.blockX(), context.blockZ()) - 8.0) / 128.0;
        }

        @Override
        public double minValue() {
            return -0.84375;
        }

        @Override
        public double maxValue() {
            return 0.5625;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private record FindTopSurface(DensityFunction density, DensityFunction upperBound, int lowerBound, int cellHeight)
            implements DensityFunction {
        private static final MapCodec<
                DensityFunctions.FindTopSurface> DATA_CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        DensityFunction.CODEC.fieldOf("density").forGetter(DensityFunctions.FindTopSurface
                                ::density),
                        DensityFunction.CODEC.fieldOf("upper_bound").forGetter(DensityFunctions.FindTopSurface
                                ::upperBound),
                        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y)
                                .fieldOf("lower_bound")
                                .forGetter(DensityFunctions.FindTopSurface::lowerBound),
                        ExtraCodecs.POSITIVE_INT.fieldOf("cell_height").forGetter(DensityFunctions.FindTopSurface
                                ::cellHeight)
                )
                        .apply(i, DensityFunctions.FindTopSurface::new)
        );
        public static final KeyDispatchDataCodec<
                DensityFunctions.FindTopSurface> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            int topY = Mth.floor(this.upperBound.compute(context) / this.cellHeight) * this.cellHeight;
            if (topY <= this.lowerBound) {
                return this.lowerBound;
            }

            for (int blockY = topY; blockY >= this.lowerBound; blockY -= this.cellHeight) {
                if (this.density.compute(new DensityFunction.SinglePointContext(context.blockX(), blockY, context.blockZ())) > 0.0) {
                    return blockY;
                }
            }

            return this.lowerBound;
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.FindTopSurface(visitor.apply(this.density), visitor.apply(this.upperBound), this.lowerBound, this.cellHeight);
        }

        @Override
        public double minValue() {
            return this.lowerBound;
        }

        @Override
        public double maxValue() {
            return Math.max(this.lowerBound, this.upperBound.maxValue());
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    @VisibleForDebug
    public record HolderHolder(Holder<DensityFunction> function) implements DensityFunction {
        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return this.function.value().compute(context);
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            this.function.value().fillArray(output, contextProvider);
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.HolderHolder(Holder.direct(visitor.apply(this.function.value())));
        }

        @Override
        public double minValue() {
            return this.function.isBound() ? this.function.value().minValue() : Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return this.function.isBound() ? this.function.value().maxValue() : Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("Calling .codec() on HolderHolder");
        }
    }

    private record IntervalSelect(DensityFunction input, DoubleList thresholds, List<
                    DensityFunction> functions)
            implements DensityFunction {
        private static final Codec<
                DoubleList> THRESHOLDS_CODEC = DensityFunctions.NOISE_VALUE_CODEC.listOf().xmap(DoubleArrayList
                ::new, Function.identity());
        public static final MapCodec<
                DensityFunctions.IntervalSelect> DATA_CODEC = RecordCodecBuilder.<DensityFunctions.IntervalSelect>
                mapCodec(
                        i -> i.group(
                                DensityFunction.CODEC.fieldOf("input").forGetter(DensityFunctions.IntervalSelect
                                        ::input),
                                THRESHOLDS_CODEC.fieldOf("thresholds").forGetter(DensityFunctions.IntervalSelect
                                        ::thresholds),
                                DensityFunction.CODEC.listOf(2, Integer.MAX_VALUE).fieldOf("functions").forGetter(DensityFunctions.IntervalSelect
                                        ::functions)
                        )
                                .apply(i, DensityFunctions.IntervalSelect::new)
                )
                .validate(DensityFunctions.IntervalSelect::validate);
        public static final KeyDispatchDataCodec<
                DensityFunctions.IntervalSelect> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        private DataResult<DensityFunctions.IntervalSelect> validate() {
            if (this.thresholds.size() != this.functions.size() - 1) {
                return DataResult.error(
                        () -> "Expected "
                                + (this.functions.size() - 1)
                                + " thresholds for "
                                + this.functions.size()
                                + " functions, but got "
                                + this.thresholds.size()
                );
            } else {
                return !Comparators.isInOrder(this.thresholds, Double::compare)
                        ? DataResult.error(() -> "Threshold values must be ordered from smallest to largest")
                        : DataResult.success(this);
            }
        }

        private double compute(final DensityFunction.FunctionContext context, final double input) {
            for (int i = 0; i < this.thresholds.size(); i++) {
                if (input < this.thresholds.getDouble(i)) {
                    return this.functions.get(i).compute(context);
                }
            }

            return this.functions.getLast().compute(context);
        }

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return this.compute(context, this.input.compute(context));
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            this.input.fillArray(output, contextProvider);

            for (int i = 0; i < output.length; i++) {
                output[i] = this.compute(contextProvider.forIndex(i), output[i]);
            }
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.IntervalSelect(visitor.apply(this.input), this.thresholds, List.copyOf(Lists.transform(this.functions, visitor
                    ::apply)));
        }

        @Override
        public double minValue() {
            double minValue = Double.MAX_VALUE;

            for (DensityFunction function : this.functions) {
                minValue = Math.min(function.minValue(), minValue);
            }

            return minValue;
        }

        @Override
        public double maxValue() {
            double maxValue = -Double.MAX_VALUE;

            for (DensityFunction function : this.functions) {
                maxValue = Math.max(function.maxValue(), maxValue);
            }

            return maxValue;
        }

        @Override
        public KeyDispatchDataCodec<DensityFunctions.IntervalSelect> codec() {
            return CODEC;
        }
    }

    protected record Mapped(DensityFunctions.Mapped.Type type, DensityFunction input, double minValue, double maxValue)
            implements DensityFunctions.PureTransformer {
        public static DensityFunctions.Mapped create(final DensityFunctions.Mapped.Type type, final DensityFunction input) {
            double minValue = input.minValue();
            double maxValue = input.maxValue();
            double minImage = transform(type, minValue);
            double maxImage = transform(type, maxValue);
            if (type == DensityFunctions.Mapped.Type.INVERT) {
                return minValue < 0.0 && maxValue > 0.0
                        ? new DensityFunctions.Mapped(type, input, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
                        : new DensityFunctions.Mapped(type, input, maxImage, minImage);
            } else {
                return type != DensityFunctions.Mapped.Type.ABS && type != DensityFunctions.Mapped.Type.SQUARE
                        ? new DensityFunctions.Mapped(type, input, minImage, maxImage)
                        : new DensityFunctions.Mapped(type, input, Math.max(0.0, minValue), Math.max(minImage, maxImage));
            }
        }

        private static double transform(final DensityFunctions.Mapped.Type type, final double input) {
            return switch (type) {
                case ABS -> Math.abs(input);
                case SQUARE -> input * input;
                case CUBE -> input * input * input;
                case HALF_NEGATIVE -> input > 0.0 ? input : input * 0.5;
                case QUARTER_NEGATIVE -> input > 0.0 ? input : input * 0.25;
                case INVERT -> 1.0 / input;
                case SQUEEZE -> {
                    double c = Mth.clamp(input, -1.0, 1.0);
                    yield c / 2.0 - c * c * c / 24.0;
                }
            };
        }

        @Override
        public double transform(final double input) {
            return transform(this.type, input);
        }

        public DensityFunctions.Mapped mapChildren(final DensityFunction.Visitor visitor) {
            return create(this.type, visitor.apply(this.input));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type.codec;
        }

        public enum Type implements StringRepresentable {
            ABS("abs"),
            SQUARE("square"),
            CUBE("cube"),
            HALF_NEGATIVE("half_negative"),
            QUARTER_NEGATIVE("quarter_negative"),
            INVERT("invert"),
            SQUEEZE("squeeze");

            private final String name;
            private final KeyDispatchDataCodec<
                    DensityFunctions.Mapped> codec = DensityFunctions.singleFunctionArgumentCodec(
                    input -> DensityFunctions.Mapped.create(this, input), DensityFunctions.Mapped
                            ::input
            );

            Type(final String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    record Marker(DensityFunctions.Marker.Type type, DensityFunction wrapped)
            implements DensityFunctions.MarkerOrMarked {
        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return this.wrapped.compute(context);
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            this.wrapped.fillArray(output, contextProvider);
        }

        @Override
        public double minValue() {
            return this.type == DensityFunctions.Marker.Type.BlendDensity ? Double.NEGATIVE_INFINITY : this.wrapped.minValue();
        }

        @Override
        public double maxValue() {
            return this.type == DensityFunctions.Marker.Type.BlendDensity ? Double.POSITIVE_INFINITY : this.wrapped.maxValue();
        }

        public enum Type implements StringRepresentable {
            Interpolated("interpolated"),
            FlatCache("flat_cache"),
            Cache2D("cache_2d"),
            CacheOnce("cache_once"),
            CacheAllInCell("cache_all_in_cell"),
            BlendDensity("blend_density");

            private final String name;
            private final KeyDispatchDataCodec<
                    DensityFunctions.MarkerOrMarked> codec = DensityFunctions.singleFunctionArgumentCodec(
                    input -> new DensityFunctions.Marker(this, input), DensityFunctions.MarkerOrMarked
                            ::wrapped
            );

            Type(final String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    public interface MarkerOrMarked extends DensityFunction {
        DensityFunctions.Marker.Type type();

        DensityFunction wrapped();

        @Override
        default KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type().codec;
        }

        @Override
        default DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.Marker(this.type(), visitor.apply(this.wrapped()));
        }
    }

    private record MulOrAdd(DensityFunctions.MulOrAdd.Type specificType, DensityFunction input, double minValue, double maxValue, double argument)
            implements DensityFunctions.TwoArgumentSimpleFunction,
                    DensityFunctions.PureTransformer {
        @Override
        public DensityFunctions.TwoArgumentSimpleFunction.Type type() {
            return this.specificType == DensityFunctions.MulOrAdd.Type.MUL
                    ? DensityFunctions.TwoArgumentSimpleFunction.Type.MUL
                    : DensityFunctions.TwoArgumentSimpleFunction.Type.ADD;
        }

        @Override
        public DensityFunction argument1() {
            return DensityFunctions.constant(this.argument);
        }

        @Override
        public DensityFunction argument2() {
            return this.input;
        }

        @Override
        public double transform(final double input) {
            return switch (this.specificType) {
                case MUL -> input * this.argument;
                case ADD -> input + this.argument;
            };
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            DensityFunction function = visitor.apply(this.input);
            double min = function.minValue();
            double max = function.maxValue();
            double minValue;
            double maxValue;
            if (this.specificType == DensityFunctions.MulOrAdd.Type.ADD) {
                minValue = min + this.argument;
                maxValue = max + this.argument;
            } else if (this.argument >= 0.0) {
                minValue = min * this.argument;
                maxValue = max * this.argument;
            } else {
                minValue = max * this.argument;
                maxValue = min * this.argument;
            }

            return new DensityFunctions.MulOrAdd(this.specificType, function, minValue, maxValue, this.argument);
        }

        public enum Type {
            MUL,
            ADD;
        }
    }

    protected record Noise(DensityFunction.NoiseHolder noise, @Deprecated
                    double xzScale, double yScale)
            implements DensityFunction {
        public static final MapCodec<
                DensityFunctions.Noise> DATA_CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.Noise
                                ::noise),
                        Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.Noise::xzScale),
                        Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.Noise::yScale)
                )
                        .apply(i, DensityFunctions.Noise::new)
        );
        public static final KeyDispatchDataCodec<
                DensityFunctions.Noise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            // 🔧 MCRe：先施加 WorldReposition 偏移（newPos = pos * scale + shift），再乘数据定义的 xzScale/yScale
            final double x = WorldReposition.reposition(context.blockX(), Direction.Axis.X) * this.xzScale;
            final double y = WorldReposition.reposition(context.blockY(), Direction.Axis.Y) * this.yScale;
            final double z = WorldReposition.reposition(context.blockZ(), Direction.Axis.Z) * this.xzScale;
            return this.noise.getValue(x, y, z);
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.Noise(visitor.visitNoise(this.noise), this.xzScale, this.yScale);
        }

        @Override
        public double minValue() {
            return -this.maxValue();
        }

        @Override
        public double maxValue() {
            return this.noise.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    private interface PureTransformer extends DensityFunction {
        DensityFunction input();

        @Override
        default double compute(final DensityFunction.FunctionContext context) {
            return this.transform(this.input().compute(context));
        }

        @Override
        default void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            this.input().fillArray(output, contextProvider);

            for (int i = 0; i < output.length; i++) {
                output[i] = this.transform(output[i]);
            }
        }

        double transform(final double input);
    }

    private record RangeChoice(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange)
            implements DensityFunction {
        public static final MapCodec<
                DensityFunctions.RangeChoice> DATA_CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        DensityFunction.CODEC.fieldOf("input").forGetter(DensityFunctions.RangeChoice
                                ::input),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min_inclusive").forGetter(DensityFunctions.RangeChoice
                                ::minInclusive),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max_exclusive").forGetter(DensityFunctions.RangeChoice
                                ::maxExclusive),
                        DensityFunction.CODEC.fieldOf("when_in_range").forGetter(DensityFunctions.RangeChoice
                                ::whenInRange),
                        DensityFunction.CODEC.fieldOf("when_out_of_range").forGetter(DensityFunctions.RangeChoice
                                ::whenOutOfRange)
                )
                        .apply(i, DensityFunctions.RangeChoice::new)
        );
        public static final KeyDispatchDataCodec<
                DensityFunctions.RangeChoice> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            double inputValue = this.input.compute(context);
            return inputValue >= this.minInclusive && inputValue < this.maxExclusive ? this.whenInRange.compute(context) : this.whenOutOfRange.compute(context);
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            this.input.fillArray(output, contextProvider);

            for (int i = 0; i < output.length; i++) {
                double v = output[i];
                if (v >= this.minInclusive && v < this.maxExclusive) {
                    output[i] = this.whenInRange.compute(contextProvider.forIndex(i));
                } else {
                    output[i] = this.whenOutOfRange.compute(contextProvider.forIndex(i));
                }
            }
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.RangeChoice(
            visitor.apply(this.input), this.minInclusive, this.maxExclusive, visitor.apply(this.whenInRange), visitor.apply(this.whenOutOfRange)
            );
        }

        @Override
        public double minValue() {
            return Math.min(this.whenInRange.minValue(), this.whenOutOfRange.minValue());
        }

        @Override
        public double maxValue() {
            return Math.max(this.whenInRange.maxValue(), this.whenOutOfRange.maxValue());
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record Shift(DensityFunction.NoiseHolder offsetNoise)
            implements DensityFunctions.ShiftNoise {
        private static final KeyDispatchDataCodec<
                DensityFunctions.Shift> CODEC = DensityFunctions.singleArgumentCodec(
                DensityFunction.NoiseHolder.CODEC, DensityFunctions.Shift
                        ::new, DensityFunctions.Shift::offsetNoise
        );

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            // 🔧 MCRe：先施加 WorldReposition 偏移，再调内部 compute(x, y, z)
            return this.compute(
                    WorldReposition.reposition(context.blockX(), Direction.Axis.X),
                    WorldReposition.reposition(context.blockY(), Direction.Axis.Y),
                    WorldReposition.reposition(context.blockZ(), Direction.Axis.Z)
            );
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.Shift(visitor.visitNoise(this.offsetNoise));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record ShiftA(DensityFunction.NoiseHolder offsetNoise)
            implements DensityFunctions.ShiftNoise {
        private static final KeyDispatchDataCodec<
                DensityFunctions.ShiftA> CODEC = DensityFunctions.singleArgumentCodec(
                DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftA
                        ::new, DensityFunctions.ShiftA::offsetNoise
        );

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            // 🔧 MCRe：先施加 WorldReposition 偏移（X/Z 轴，Y 强制 0），再调内部 compute
            return this.compute(
                    WorldReposition.reposition(context.blockX(), Direction.Axis.X),
                    0.0,
                    WorldReposition.reposition(context.blockZ(), Direction.Axis.Z)
            );
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.ShiftA(visitor.visitNoise(this.offsetNoise));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected record ShiftB(DensityFunction.NoiseHolder offsetNoise)
            implements DensityFunctions.ShiftNoise {
        private static final KeyDispatchDataCodec<
                DensityFunctions.ShiftB> CODEC = DensityFunctions.singleArgumentCodec(
                DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftB
                        ::new, DensityFunctions.ShiftB::offsetNoise
        );

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            // 🔧 MCRe：ShiftB 内部参数顺序是 (z, x, y=0)，施加 WorldReposition 偏移保持原版语义
            return this.compute(
                    WorldReposition.reposition(context.blockZ(), Direction.Axis.Z),
                    WorldReposition.reposition(context.blockX(), Direction.Axis.X),
                    0.0
            );
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.ShiftB(visitor.visitNoise(this.offsetNoise));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    protected interface ShiftNoise extends DensityFunction {
        DensityFunction.NoiseHolder offsetNoise();

        @Override
        default double minValue() {
            return -this.maxValue();
        }

        @Override
        default double maxValue() {
            return this.offsetNoise().maxValue() * 4.0;
        }

        default double compute(final double localX, final double localY, final double localZ) {
            return this.offsetNoise().getValue(localX * 0.25, localY * 0.25, localZ * 0.25) * 4.0;
        }

        @Override
        default void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }
    }

    protected record ShiftedNoise(
            DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise)
            implements DensityFunction {
        private static final MapCodec<
                DensityFunctions.ShiftedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        DensityFunction.CODEC.fieldOf("shift_x").forGetter(DensityFunctions.ShiftedNoise
                                ::shiftX),
                        DensityFunction.CODEC.fieldOf("shift_y").forGetter(DensityFunctions.ShiftedNoise
                                ::shiftY),
                        DensityFunction.CODEC.fieldOf("shift_z").forGetter(DensityFunctions.ShiftedNoise
                                ::shiftZ),
                        Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.ShiftedNoise
                                ::xzScale),
                        Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.ShiftedNoise
                                ::yScale),
                        DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.ShiftedNoise
                                ::noise)
                )
                        .apply(i, DensityFunctions.ShiftedNoise::new)
        );
        public static final KeyDispatchDataCodec<
                DensityFunctions.ShiftedNoise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            // 🔧 MCRe：先施加 WorldReposition 偏移，再乘 xzScale/yScale，最后加 shiftX/Y/Z（数据包定义的偏移）
            // 禁用 Offset 噪声开关开启时，跳过 shiftX/Y/Z 偏移（等同于 NoOffset 数据包将 shift_x/y/z 设为 constant(0)）
            final boolean disableOffset = WorldMainSettingScreen.FarLandsConfigData.activeConfig != null
                    && WorldMainSettingScreen.FarLandsConfigData.activeConfig.disableOffsetNoise;
            final double x = WorldReposition.reposition(context.blockX(), Direction.Axis.X) * this.xzScale
                    + (disableOffset ? 0.0 : this.shiftX.compute(context));
            final double y = WorldReposition.reposition(context.blockY(), Direction.Axis.Y) * this.yScale
                    + (disableOffset ? 0.0 : this.shiftY.compute(context));
            final double z = WorldReposition.reposition(context.blockZ(), Direction.Axis.Z) * this.xzScale
                    + (disableOffset ? 0.0 : this.shiftZ.compute(context));
            return this.noise.getValue(x, y, z);
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.ShiftedNoise(
            visitor.apply(this.shiftX), visitor.apply(this.shiftY), visitor.apply(this.shiftZ), this.xzScale, this.yScale, visitor.visitNoise(this.noise)
            );
        }

        @Override
        public double minValue() {
            return -this.maxValue();
        }

        @Override
        public double maxValue() {
            return this.noise.maxValue();
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }

    public static final class Spline implements DensityFunction {
        private static final Codec<
                CubicSpline<
                        DensityFunctions.Spline.Coordinate>> SPLINE_CODEC = CubicSpline.codec(DensityFunctions.Spline.Coordinate.CODEC);
        private static final MapCodec<
                DensityFunctions.Spline> DATA_CODEC = SPLINE_CODEC.fieldOf("spline")
                .xmap(DensityFunctions.Spline::new, DensityFunctions.Spline::spline);
        public static final KeyDispatchDataCodec<
                DensityFunctions.Spline> CODEC = DensityFunctions.makeCodec(DATA_CODEC);
        private final CubicSpline<DensityFunctions.Spline.Coordinate> spline;
        private final BoundedFloatFunction<DensityFunctions.Spline.Point> sampler;

        public Spline(final CubicSpline<DensityFunctions.Spline.Coordinate> spline) {
            this.spline = spline;
            this.sampler = CubicSpline.asSampler(spline);
        }

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            return this.sampler.apply(new DensityFunctions.Spline.Point(context));
        }

        @Override
        public double minValue() {
            return this.spline.minValue();
        }

        @Override
        public double maxValue() {
            return this.spline.maxValue();
        }

        @Override
        public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            contextProvider.fillAllDirectly(output, this);
        }

        @Override
        public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.Spline(this.spline.mapCoordinates(c -> c.mapChildren(visitor)));
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }

        public CubicSpline<DensityFunctions.Spline.Coordinate> spline() {
            return this.spline;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj == this ? true : obj
                                    instanceof
                                    DensityFunctions.Spline splineFunction && this.spline.equals(splineFunction.spline);
        }

        @Override
        public int hashCode() {
            return this.spline.hashCode();
        }

        @Override
        public String toString() {
            return this.spline.toString();
        }

        public record Coordinate(DensityFunction function)
                implements BoundedFloatFunction<DensityFunctions.Spline.Point> {
            public static final Codec<
                    DensityFunctions.Spline.Coordinate> CODEC = DensityFunction.CODEC
                    .xmap(DensityFunctions.Spline.Coordinate
                    ::new, DensityFunctions.Spline.Coordinate::function);

            public float apply(final DensityFunctions.Spline.Point point) {
                return (float) this.function.compute(point.context());
            }

            @Override
            public float minValue() {
                return (float) this.function.minValue();
            }

            @Override
            public float maxValue() {
                return (float) this.function.maxValue();
            }

            public DensityFunctions.Spline.Coordinate mapChildren(final DensityFunction.Visitor visitor) {
                return new DensityFunctions.Spline.Coordinate(visitor.apply(this.function));
            }
        }

        public record Point(DensityFunction.FunctionContext context) {}
    }

    private interface TransformerWithContext extends DensityFunction {
        DensityFunction input();

        @Override
        default double compute(final DensityFunction.FunctionContext context) {
            return this.transform(context, this.input().compute(context));
        }

        @Override
        default void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
            this.input().fillArray(output, contextProvider);

            for (int i = 0; i < output.length; i++) {
                output[i] = this.transform(contextProvider.forIndex(i), output[i]);
            }
        }

        double transform(DensityFunction.FunctionContext contextSupplier, final double input);
    }

    public interface TwoArgumentSimpleFunction extends DensityFunction {
        Logger LOGGER = LogUtils.getLogger();

        static DensityFunctions.TwoArgumentSimpleFunction create(
                final DensityFunctions.TwoArgumentSimpleFunction.Type type, final DensityFunction argument1, final DensityFunction argument2) {
            double min1 = argument1.minValue();
            double min2 = argument2.minValue();
            double max1 = argument1.maxValue();
            double max2 = argument2.maxValue();
            if (type == DensityFunctions.TwoArgumentSimpleFunction.Type.MIN || type == DensityFunctions.TwoArgumentSimpleFunction.Type.MAX) {
                boolean firstAlwaysBiggerThanSecond = min1 >= max2;
                boolean secondAlwaysBiggerThanFirst = min2 >= max1;
                if (firstAlwaysBiggerThanSecond || secondAlwaysBiggerThanFirst) {
                    LOGGER.warn("Creating a {} function between two non-overlapping inputs: {} and {}", type, argument1, argument2);
                }
            }
            double minValue = switch (type) {
                case ADD -> min1 + min2;
                case MUL ->
                        min1 > 0.0 && min2 > 0.0 ? min1 * min2 : (max1 < 0.0 && max2 < 0.0 ? max1 * max2 : Math.min(min1 * max2, max1 * min2));
                case MIN -> Math.min(min1, min2);
                case MAX -> Math.max(min1, min2);
            };

            double maxValue = switch (type) {
                case ADD -> max1 + max2;
                case MUL ->
                        min1 > 0.0 && min2 > 0.0 ? max1 * max2 : (max1 < 0.0 && max2 < 0.0 ? min1 * min2 : Math.max(min1 * min2, max1 * max2));
                case MIN -> Math.min(max1, max2);
                case MAX -> Math.max(max1, max2);
            };
            if (type == DensityFunctions.TwoArgumentSimpleFunction.Type.MUL || type == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD) {
                if (argument1 instanceof DensityFunctions.Constant constant) {
                    return new DensityFunctions.MulOrAdd(
                    type == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL,
                    argument2,
                    minValue,
                    maxValue,
                    constant.value
                    );
                }

                if (argument2 instanceof DensityFunctions.Constant constant) {
                    return new DensityFunctions.MulOrAdd(
                    type == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL,
                    argument1,
                    minValue,
                    maxValue,
                    constant.value
                    );
                }
            }

            return new DensityFunctions.Ap2(type, argument1, argument2, minValue, maxValue);
        }

        DensityFunctions.TwoArgumentSimpleFunction.Type type();

        DensityFunction argument1();

        DensityFunction argument2();

        @Override
        default KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return this.type().codec;
        }

        enum Type implements StringRepresentable {
            ADD("add"),
            MUL("mul"),
            MIN("min"),
            MAX("max");

            private final KeyDispatchDataCodec<
                    DensityFunctions.TwoArgumentSimpleFunction> codec = DensityFunctions.doubleFunctionArgumentCodec(
                    (argument1, argument2) -> DensityFunctions.TwoArgumentSimpleFunction.create(this, argument1, argument2),
                    DensityFunctions.TwoArgumentSimpleFunction::argument1,
                    DensityFunctions.TwoArgumentSimpleFunction::argument2
            );
            private final String name;

            Type(final String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }
    }

    private record YClampedGradient(int fromY, int toY, double fromValue, double toValue)
            implements DensityFunction.SimpleFunction {
        private static final MapCodec<
                DensityFunctions.YClampedGradient> DATA_CODEC = RecordCodecBuilder.mapCodec(
                i -> i.group(
                        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("from_y").forGetter(DensityFunctions.YClampedGradient
                                ::fromY),
                        Codec.intRange(DimensionType.MIN_Y, DimensionType.MAX_Y).fieldOf("to_y").forGetter(DensityFunctions.YClampedGradient
                                ::toY),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("from_value").forGetter(DensityFunctions.YClampedGradient
                                ::fromValue),
                        DensityFunctions.NOISE_VALUE_CODEC.fieldOf("to_value").forGetter(DensityFunctions.YClampedGradient
                                ::toValue)
                )
                        .apply(i, DensityFunctions.YClampedGradient::new)
        );
        public static final KeyDispatchDataCodec<
                DensityFunctions.YClampedGradient> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

        @Override
        public double compute(final DensityFunction.FunctionContext context) {
            // 🔧 MCRe：YClampedGradient 控制 Y 轴 base stone 海拔梯度——独立开关 enabledYClampedGradientOffset
            // 默认关闭（保持原版海拔梯度，避免 Y 轴边境之地消失）；用户主动开启后才能扩展 Y 轴探索范围
            final double y = WorldReposition.isYClampedGradientOffsetEnabled()
                    ? WorldReposition.reposition(context.blockY(), Direction.Axis.Y)
                    : context.blockY();
            return Mth.clampedMap(y, this.fromY, this.toY, this.fromValue, this.toValue);
        }

        @Override
        public double minValue() {
            return Math.min(this.fromValue, this.toValue);
        }

        @Override
        public double maxValue() {
            return Math.max(this.fromValue, this.toValue);
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            return CODEC;
        }
    }
}