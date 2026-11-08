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
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.BoundedFloatFunction;
import net.minecraft.util.CubicSpline;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Interval;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.slf4j.Logger;

public final class DensityFunctions {
   private static final Codec<DensityFunction> CODEC = BuiltInRegistries.DENSITY_FUNCTION_TYPE
      .byNameCodec()
      .dispatch(function -> function.codec().codec(), Function.identity());
   static final double MAX_REASONABLE_NOISE_VALUE = 1000000.0;
   private static final Codec<Double> NOISE_VALUE_CODEC = Codec.doubleRange(-1000000.0, 1000000.0);
   public static final Codec<DensityFunction> DIRECT_CODEC = Codec.either(NOISE_VALUE_CODEC, CODEC)
      .xmap(
         either -> (DensityFunction)either.map(DensityFunctions::constant, Function.identity()),
         function -> function instanceof DensityFunctions.Constant constant ? Either.left(constant.value()) : Either.right(function)
      );

   public static MapCodec<? extends DensityFunction> bootstrap(final Registry<MapCodec<? extends DensityFunction>> registry) {
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
      final Registry<MapCodec<? extends DensityFunction>> registry, final String name, final KeyDispatchDataCodec<? extends DensityFunction> codec
   ) {
      return Registry.register(registry, name, codec.codec());
   }

   private static <A, O> KeyDispatchDataCodec<O> singleArgumentCodec(
      final Codec<A> argumentCodec, final Function<A, O> constructor, final Function<O, A> getter
   ) {
      return KeyDispatchDataCodec.of(argumentCodec.fieldOf("argument").xmap(constructor, getter));
   }

   private static <O> KeyDispatchDataCodec<O> singleFunctionArgumentCodec(
      final Function<DensityFunction, O> constructor, final Function<O, DensityFunction> getter
   ) {
      return singleArgumentCodec(DensityFunction.CODEC, constructor, getter);
   }

   // ===== 修改：group 改为实例方法调用 i.group() =====
   private static <O> KeyDispatchDataCodec<O> doubleFunctionArgumentCodec(
      final BiFunction<DensityFunction, DensityFunction, O> constructor,
      final Function<O, DensityFunction> firstArgumentGetter,
      final Function<O, DensityFunction> secondArgumentGetter
   ) {
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

   private DensityFunctions() {
   }

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
      final double maxTarget
   ) {
      return mapFromUnitTo(new DensityFunctions.Noise(new DensityFunction.NoiseHolder(noiseData), xzScale, yScale), minTarget, maxTarget);
   }

   public static DensityFunction mappedNoise(
      final Holder<NormalNoise.NoiseParameters> noiseData, final double yScale, final double minTarget, final double maxTarget
   ) {
      return mappedNoise(noiseData, 1.0, yScale, minTarget, maxTarget);
   }

   public static DensityFunction mappedNoise(final Holder<NormalNoise.NoiseParameters> noiseData, final double minTarget, final double maxTarget) {
      return mappedNoise(noiseData, 1.0, 1.0, minTarget, maxTarget);
   }

   public static DensityFunction shiftedNoise2d(
      final DensityFunction shiftX, final DensityFunction shiftZ, final double xzScale, final Holder<NormalNoise.NoiseParameters> noiseData
   ) {
      return new DensityFunctions.ShiftedNoise(shiftX, zero(), shiftZ, xzScale, 0.0, new DensityFunction.NoiseHolder(noiseData));
   }

   public static DensityFunction noise(final Holder<NormalNoise.NoiseParameters> noiseData) {
      return noise(noiseData, 1.0, 1.0);
   }

   public static DensityFunction noise(final Holder<NormalNoise.NoiseParameters> noiseData, final double xzScale, final double yScale) {
      return new DensityFunctions.Noise(new DensityFunction.NoiseHolder(noiseData), xzScale, yScale);
   }

   public static DensityFunction noise(final Holder<NormalNoise.NoiseParameters> noiseData, final double yScale) {
      return noise(noiseData, 1.0, yScale);
   }

   public static DensityFunction rangeChoice(
      final DensityFunction input,
      final double minInclusive,
      final double maxExclusive,
      final DensityFunction whenInRange,
      final DensityFunction whenOutOfRange
   ) {
      return new DensityFunctions.RangeChoice(input, minInclusive, maxExclusive, whenInRange, whenOutOfRange);
   }

   public static DensityFunction intervalSelect(final DensityFunction input, final DoubleList thresholds, final List<DensityFunction> functions) {
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

   public static DensityFunction spline(final CubicSpline<DensityFunctions.Spline.Coordinate> spline) {
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
      return new DensityFunctions.Mapped(type, function);
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
      DensityFunctions.TwoArgumentSimpleFunction.Type type, DensityFunction argument1, DensityFunction argument2, double minValue2, double maxValue2
   ) implements DensityFunctions.TwoArgumentSimpleFunction {
      public Ap2(final DensityFunctions.TwoArgumentSimpleFunction.Type type, final DensityFunction argument1, final DensityFunction argument2) {
         Interval range2 = argument2.range();
         this(type, argument1, argument2, range2.min(), range2.max());
         if ((type == DensityFunctions.TwoArgumentSimpleFunction.Type.MIN || type == DensityFunctions.TwoArgumentSimpleFunction.Type.MAX)
            && !argument1.range().intersects(range2)) {
            LOGGER.warn("Creating a {} function between two non-overlapping inputs: {} and {}", new Object[]{type, argument1, argument2});
         }
      }

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         double v1 = this.argument1.compute(context);

         return switch (this.type) {
            case ADD -> v1 + this.argument2.compute(context);
            case MUL -> v1 == 0.0 ? 0.0 : v1 * this.argument2.compute(context);
            case MIN -> v1 < this.minValue2 ? v1 : Math.min(v1, this.argument2.compute(context));
            case MAX -> v1 > this.maxValue2 ? v1 : Math.max(v1, this.argument2.compute(context));
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
                  output[i] = v == 0.0 ? 0.0 : v * this.argument2.compute(contextProvider.forIndex(i));
               }
               break;
            case MIN:
               for (int i = 0; i < output.length; i++) {
                  double v = output[i];
                  output[i] = v < this.minValue2 ? v : Math.min(v, this.argument2.compute(contextProvider.forIndex(i)));
               }
               break;
            case MAX:
               for (int i = 0; i < output.length; i++) {
                  double v = output[i];
                  output[i] = v > this.maxValue2 ? v : Math.max(v, this.argument2.compute(contextProvider.forIndex(i)));
               }
         }
      }

      @Override
      public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
         return DensityFunctions.TwoArgumentSimpleFunction.create(this.type, visitor.apply(this.argument1), visitor.apply(this.argument2));
      }

      @Override
      public Interval range() {
         Interval range1 = this.argument1.range();
         Interval range2 = this.argument2.range();

         return switch (this.type) {
            case ADD -> Interval.add(range1, range2);
            case MUL -> Interval.mul(range1, range2);
            case MIN -> Interval.min(range1, range2);
            case MAX -> Interval.max(range1, range2);
         };
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
   }

   public interface BeardifierOrMarker extends DensityFunction.SimpleFunction {
      KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(DensityFunctions.BeardifierMarker.INSTANCE));

      @Override
      default Interval range() {
         return Beardifier.RANGE;
      }

      @Override
      default KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   enum BlendAlpha implements DensityFunction.SimpleFunction {
      INSTANCE;

      public static final KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return 1.0;
      }

      @Override
      public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
         Arrays.fill(output, 1.0);
      }

      @Override
      public Interval range() {
         return Interval.of(0.0, 1.0);
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   enum BlendOffset implements DensityFunction.SimpleFunction {
      INSTANCE;

      public static final KeyDispatchDataCodec<DensityFunction> CODEC = KeyDispatchDataCodec.of(MapCodec.unit(INSTANCE));

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return 0.0;
      }

      @Override
      public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
         Arrays.fill(output, 0.0);
      }

      @Override
      public Interval range() {
         return Interval.INFINITE;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   // ===== 修改：group 改为 i.group，validate 已为 lambda =====
   protected record Clamp(DensityFunction input, double min, double max) implements DensityFunctions.PureTransformer {
      private static final MapCodec<DensityFunctions.Clamp> DATA_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  DensityFunction.CODEC.fieldOf("input").forGetter(DensityFunctions.Clamp::input),
                  DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min").forGetter(DensityFunctions.Clamp::min),
                  DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max").forGetter(DensityFunctions.Clamp::max)
               )
               .apply(i, DensityFunctions.Clamp::new)
         )
         .validate((DensityFunctions.Clamp clamp) -> DensityFunctions.Clamp.validate(clamp));
      public static final KeyDispatchDataCodec<DensityFunctions.Clamp> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

      private static DataResult<DensityFunctions.Clamp> validate(final DensityFunctions.Clamp clamp) {
         return clamp.max < clamp.min
            ? DataResult.error(() -> "min (" + clamp.min + ") must be less than or equal to max (" + clamp.max + ")")
            : DataResult.success(clamp);
      }

      @Override
      public double transform(final double input) {
         return Mth.clamp(input, this.min, this.max);
      }

      @Override
      public DensityFunction mapChildren(final DensityFunction.Visitor visitor) {
         return new DensityFunctions.Clamp(visitor.apply(this.input), this.min, this.max);
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }

      @Override
      public Interval range() {
         return Interval.clamp(this.input.range(), this.min, this.max);
      }
   }

   private record Constant(double value) implements DensityFunction.SimpleFunction {
      private static final KeyDispatchDataCodec<DensityFunctions.Constant> CODEC = DensityFunctions.singleArgumentCodec(
         DensityFunctions.NOISE_VALUE_CODEC, DensityFunctions.Constant::new, DensityFunctions.Constant::value
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
      public Interval range() {
         return Interval.ofExact(this.value);
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   protected static final class EndIslandDensityFunction implements DensityFunction.SimpleFunction {
      public static final KeyDispatchDataCodec<DensityFunctions.EndIslandDensityFunction> CODEC = KeyDispatchDataCodec.of(
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
         int chunkX = sectionX / 2;
         int chunkZ = sectionZ / 2;
         int subSectionX = sectionX % 2;
         int subSectionZ = sectionZ % 2;
         float doffs = 100.0F - Mth.sqrt(sectionX * sectionX + sectionZ * sectionZ) * 8.0F;
         doffs = Mth.clamp(doffs, -100.0F, 80.0F);

         for (int xo = -12; xo <= 12; xo++) {
            for (int zo = -12; zo <= 12; zo++) {
               long totalChunkX = chunkX + xo;
               long totalChunkZ = chunkZ + zo;
               if (totalChunkX * totalChunkX + totalChunkZ * totalChunkZ > 4096L && islandNoise.getValue(totalChunkX, totalChunkZ) < -0.9F) {
                  float islandSize = (Mth.abs((float)totalChunkX) * 3439.0F + Mth.abs((float)totalChunkZ) * 147.0F) % 13.0F + 9.0F;
                  float xd = subSectionX - xo * 2;
                  float zd = subSectionZ - zo * 2;
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
         return (getHeightValue(this.islandNoise, context.blockX() / 8, context.blockZ() / 8) - 8.0) / 128.0;
      }

      @Override
      public Interval range() {
         return Interval.of(-0.84375, 0.5625);
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   // ===== 修改：group 改为 i.group =====
   private record FindTopSurface(DensityFunction density, DensityFunction upperBound, int lowerBound, int cellHeight) implements DensityFunction {
      private static final MapCodec<DensityFunctions.FindTopSurface> DATA_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               DensityFunction.CODEC.fieldOf("density").forGetter(DensityFunctions.FindTopSurface::density),
               DensityFunction.CODEC.fieldOf("upper_bound").forGetter(DensityFunctions.FindTopSurface::upperBound),
               Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("lower_bound").forGetter(DensityFunctions.FindTopSurface::lowerBound),
               ExtraCodecs.POSITIVE_INT.fieldOf("cell_height").forGetter(DensityFunctions.FindTopSurface::cellHeight)
            )
            .apply(i, DensityFunctions.FindTopSurface::new)
      );
      public static final KeyDispatchDataCodec<DensityFunctions.FindTopSurface> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

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
      public Interval range() {
         return Interval.of(this.lowerBound, Math.max(this.lowerBound, this.upperBound.range().max()));
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
      public Interval range() {
         return this.function.isBound() ? this.function.value().range() : Interval.INFINITE;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         throw new UnsupportedOperationException("Calling .codec() on HolderHolder");
      }
   }

   // ===== 修改：group 改为 i.group，validate 改为显式 lambda =====
   private record IntervalSelect(DensityFunction input, DoubleList thresholds, List<DensityFunction> functions) implements DensityFunction {
      private static final Codec<DoubleList> THRESHOLDS_CODEC = DensityFunctions.NOISE_VALUE_CODEC.listOf().xmap(DoubleArrayList::new, Function.identity());
      public static final MapCodec<DensityFunctions.IntervalSelect> DATA_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  DensityFunction.CODEC.fieldOf("input").forGetter(DensityFunctions.IntervalSelect::input),
                  THRESHOLDS_CODEC.fieldOf("thresholds").forGetter(DensityFunctions.IntervalSelect::thresholds),
                  DensityFunction.CODEC.listOf(2, Integer.MAX_VALUE).fieldOf("functions").forGetter(DensityFunctions.IntervalSelect::functions)
               )
               .apply(i, DensityFunctions.IntervalSelect::new)
         )
         .validate((DensityFunctions.IntervalSelect intervalSelect) -> intervalSelect.validate());
      public static final KeyDispatchDataCodec<DensityFunctions.IntervalSelect> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

      private DataResult<DensityFunctions.IntervalSelect> validate() {
         if (this.thresholds.size() != this.functions.size() - 1) {
            return DataResult.error(
               () -> "Expected " + (this.functions.size() - 1) + " thresholds for " + this.functions.size() + " functions, but got " + this.thresholds.size()
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
         return new DensityFunctions.IntervalSelect(visitor.apply(this.input), this.thresholds, List.copyOf(Lists.transform(this.functions, visitor::apply)));
      }

      @Override
      public Interval range() {
         return Interval.encapsulating(Lists.transform(this.functions, DensityFunction::range));
      }

      @Override
      public KeyDispatchDataCodec<DensityFunctions.IntervalSelect> codec() {
         return CODEC;
      }
   }

   protected record Mapped(DensityFunctions.Mapped.Type type, DensityFunction input) implements DensityFunctions.PureTransformer {
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
         return new DensityFunctions.Mapped(this.type, visitor.apply(this.input));
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return this.type.codec;
      }

      @Override
      public Interval range() {
         Interval input = this.input.range();

         return switch (this.type) {
            case ABS -> Interval.abs(input);
            case SQUARE -> Interval.square(input);
            case CUBE, HALF_NEGATIVE, QUARTER_NEGATIVE, SQUEEZE -> Interval.mapMonotonic(input, value -> transform(this.type, value));
            case INVERT -> Interval.inverse(input);
         };
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
         private final KeyDispatchDataCodec<DensityFunctions.Mapped> codec = DensityFunctions.singleFunctionArgumentCodec(
            input -> new DensityFunctions.Mapped(this, input), DensityFunctions.Mapped::input
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

   record Marker(DensityFunctions.Marker.Type type, DensityFunction wrapped) implements DensityFunctions.MarkerOrMarked {
      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return this.wrapped.compute(context);
      }

      @Override
      public void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
         this.wrapped.fillArray(output, contextProvider);
      }

      @Override
      public Interval range() {
         return switch (this.type) {
            case Interpolated, FlatCache, Cache2D, CacheOnce, CacheAllInCell -> this.wrapped.range();
            case BlendDensity -> Interval.INFINITE;
         };
      }

      public enum Type implements StringRepresentable {
         Interpolated("interpolated"),
         FlatCache("flat_cache"),
         Cache2D("cache_2d"),
         CacheOnce("cache_once"),
         CacheAllInCell("cache_all_in_cell"),
         BlendDensity("blend_density");

         private final String name;
         private final KeyDispatchDataCodec<DensityFunctions.MarkerOrMarked> codec = DensityFunctions.singleFunctionArgumentCodec(
            input -> new DensityFunctions.Marker(this, input), DensityFunctions.MarkerOrMarked::wrapped
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

   private record MulOrAdd(DensityFunctions.MulOrAdd.Type specificType, DensityFunction input, double argument)
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
         return new DensityFunctions.MulOrAdd(this.specificType, visitor.apply(this.input), this.argument);
      }

      @Override
      public Interval range() {
         return switch (this.specificType) {
            case MUL -> Interval.mul(this.input.range(), Interval.ofExact(this.argument));
            case ADD -> Interval.add(this.input.range(), Interval.ofExact(this.argument));
         };
      }

      public enum Type {
         MUL,
         ADD;
      }
   }

   // ===== 修改：group 改为 i.group =====
   protected record Noise(DensityFunction.NoiseHolder noise, @Deprecated double xzScale, double yScale) implements DensityFunction {
      public static final MapCodec<DensityFunctions.Noise> DATA_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.Noise::noise),
               Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.Noise::xzScale),
               Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.Noise::yScale)
            )
            .apply(i, DensityFunctions.Noise::new)
      );
      public static final KeyDispatchDataCodec<DensityFunctions.Noise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return this.noise.getValue(context.blockX() * this.xzScale, context.blockY() * this.yScale, context.blockZ() * this.xzScale);
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
      public Interval range() {
         return Interval.ofSymmetric(this.noise.maxValue());
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

   // ===== 修改：group 改为 i.group =====
   private record RangeChoice(DensityFunction input, double minInclusive, double maxExclusive, DensityFunction whenInRange, DensityFunction whenOutOfRange)
      implements DensityFunction {
      public static final MapCodec<DensityFunctions.RangeChoice> DATA_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               DensityFunction.CODEC.fieldOf("input").forGetter(DensityFunctions.RangeChoice::input),
               DensityFunctions.NOISE_VALUE_CODEC.fieldOf("min_inclusive").forGetter(DensityFunctions.RangeChoice::minInclusive),
               DensityFunctions.NOISE_VALUE_CODEC.fieldOf("max_exclusive").forGetter(DensityFunctions.RangeChoice::maxExclusive),
               DensityFunction.CODEC.fieldOf("when_in_range").forGetter(DensityFunctions.RangeChoice::whenInRange),
               DensityFunction.CODEC.fieldOf("when_out_of_range").forGetter(DensityFunctions.RangeChoice::whenOutOfRange)
            )
            .apply(i, DensityFunctions.RangeChoice::new)
      );
      public static final KeyDispatchDataCodec<DensityFunctions.RangeChoice> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

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
      public Interval range() {
         return Interval.encapsulating(this.whenInRange.range(), this.whenOutOfRange.range());
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   protected record Shift(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
      private static final KeyDispatchDataCodec<DensityFunctions.Shift> CODEC = DensityFunctions.singleArgumentCodec(
         DensityFunction.NoiseHolder.CODEC, DensityFunctions.Shift::new, DensityFunctions.Shift::offsetNoise
      );

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return this.compute(context.blockX(), context.blockY(), context.blockZ());
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

   protected record ShiftA(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
      private static final KeyDispatchDataCodec<DensityFunctions.ShiftA> CODEC = DensityFunctions.singleArgumentCodec(
         DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftA::new, DensityFunctions.ShiftA::offsetNoise
      );

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return this.compute(context.blockX(), 0.0, context.blockZ());
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

   protected record ShiftB(DensityFunction.NoiseHolder offsetNoise) implements DensityFunctions.ShiftNoise {
      private static final KeyDispatchDataCodec<DensityFunctions.ShiftB> CODEC = DensityFunctions.singleArgumentCodec(
         DensityFunction.NoiseHolder.CODEC, DensityFunctions.ShiftB::new, DensityFunctions.ShiftB::offsetNoise
      );

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return this.compute(context.blockZ(), context.blockX(), 0.0);
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
      default Interval range() {
         return Interval.ofSymmetric(this.offsetNoise().maxValue() * 4.0);
      }

      default double compute(final double localX, final double localY, final double localZ) {
         return this.offsetNoise().getValue(localX * 0.25, localY * 0.25, localZ * 0.25) * 4.0;
      }

      @Override
      default void fillArray(final double[] output, final DensityFunction.ContextProvider contextProvider) {
         contextProvider.fillAllDirectly(output, this);
      }
   }

   // ===== 修改：group 改为 i.group =====
   protected record ShiftedNoise(
      DensityFunction shiftX, DensityFunction shiftY, DensityFunction shiftZ, double xzScale, double yScale, DensityFunction.NoiseHolder noise
   ) implements DensityFunction {
      private static final MapCodec<DensityFunctions.ShiftedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               DensityFunction.CODEC.fieldOf("shift_x").forGetter(DensityFunctions.ShiftedNoise::shiftX),
               DensityFunction.CODEC.fieldOf("shift_y").forGetter(DensityFunctions.ShiftedNoise::shiftY),
               DensityFunction.CODEC.fieldOf("shift_z").forGetter(DensityFunctions.ShiftedNoise::shiftZ),
               Codec.DOUBLE.fieldOf("xz_scale").forGetter(DensityFunctions.ShiftedNoise::xzScale),
               Codec.DOUBLE.fieldOf("y_scale").forGetter(DensityFunctions.ShiftedNoise::yScale),
               DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(DensityFunctions.ShiftedNoise::noise)
            )
            .apply(i, DensityFunctions.ShiftedNoise::new)
      );
      public static final KeyDispatchDataCodec<DensityFunctions.ShiftedNoise> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         double x = context.blockX() * this.xzScale + this.shiftX.compute(context);
         double y = context.blockY() * this.yScale + this.shiftY.compute(context);
         double z = context.blockZ() * this.xzScale + this.shiftZ.compute(context);
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
      public Interval range() {
         return Interval.ofSymmetric(this.noise.maxValue());
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }

   public static final class Spline implements DensityFunction {
      private static final Codec<CubicSpline<DensityFunctions.Spline.Coordinate>> SPLINE_CODEC = CubicSpline.codec(DensityFunctions.Spline.Coordinate.CODEC);
      private static final MapCodec<DensityFunctions.Spline> DATA_CODEC = SPLINE_CODEC.fieldOf("spline")
         .xmap(DensityFunctions.Spline::new, DensityFunctions.Spline::spline);
      public static final KeyDispatchDataCodec<DensityFunctions.Spline> CODEC = DensityFunctions.makeCodec(DATA_CODEC);
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
      public Interval range() {
         return this.spline.range();
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
         return obj == this ? true : obj instanceof DensityFunctions.Spline splineFunction && this.spline.equals(splineFunction.spline);
      }

      @Override
      public int hashCode() {
         return this.spline.hashCode();
      }

      @Override
      public String toString() {
         return this.spline.toString();
      }

      public record Coordinate(DensityFunction function) implements BoundedFloatFunction<DensityFunctions.Spline.Point> {
         public static final Codec<DensityFunctions.Spline.Coordinate> CODEC = DensityFunction.CODEC
            .xmap(DensityFunctions.Spline.Coordinate::new, DensityFunctions.Spline.Coordinate::function);

         public float apply(final DensityFunctions.Spline.Point point) {
            return (float)this.function.compute(point.context());
         }

         @Override
         public Interval range() {
            return this.function.range();
         }

         public DensityFunctions.Spline.Coordinate mapChildren(final DensityFunction.Visitor visitor) {
            return new DensityFunctions.Spline.Coordinate(visitor.apply(this.function));
         }
      }

      public record Point(DensityFunction.FunctionContext context) {
      }
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
         final DensityFunctions.TwoArgumentSimpleFunction.Type type, final DensityFunction argument1, final DensityFunction argument2
      ) {
         if (type == DensityFunctions.TwoArgumentSimpleFunction.Type.MUL || type == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD) {
            if (argument1 instanceof DensityFunctions.Constant constant) {
               return new DensityFunctions.MulOrAdd(
                  type == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL,
                  argument2,
                  constant.value
               );
            }

            if (argument2 instanceof DensityFunctions.Constant constant) {
               return new DensityFunctions.MulOrAdd(
                  type == DensityFunctions.TwoArgumentSimpleFunction.Type.ADD ? DensityFunctions.MulOrAdd.Type.ADD : DensityFunctions.MulOrAdd.Type.MUL,
                  argument1,
                  constant.value
               );
            }
         }

         return new DensityFunctions.Ap2(type, argument1, argument2);
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

         private final KeyDispatchDataCodec<DensityFunctions.TwoArgumentSimpleFunction> codec = DensityFunctions.doubleFunctionArgumentCodec(
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

   // ===== 修改：group 改为 i.group =====
   private record YClampedGradient(int fromY, int toY, double fromValue, double toValue) implements DensityFunction.SimpleFunction {
      private static final MapCodec<DensityFunctions.YClampedGradient> DATA_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("from_y").forGetter(DensityFunctions.YClampedGradient::fromY),
               Codec.intRange(DimensionType.MIN_Y * 2, DimensionType.MAX_Y * 2).fieldOf("to_y").forGetter(DensityFunctions.YClampedGradient::toY),
               DensityFunctions.NOISE_VALUE_CODEC.fieldOf("from_value").forGetter(DensityFunctions.YClampedGradient::fromValue),
               DensityFunctions.NOISE_VALUE_CODEC.fieldOf("to_value").forGetter(DensityFunctions.YClampedGradient::toValue)
            )
            .apply(i, DensityFunctions.YClampedGradient::new)
      );
      public static final KeyDispatchDataCodec<DensityFunctions.YClampedGradient> CODEC = DensityFunctions.makeCodec(DATA_CODEC);

      @Override
      public double compute(final DensityFunction.FunctionContext context) {
         return Mth.clampedMap(context.blockY(), this.fromY, this.toY, this.fromValue, this.toValue);
      }

      @Override
      public Interval range() {
         return Interval.encapsulating(this.fromValue, this.toValue);
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return CODEC;
      }
   }
}