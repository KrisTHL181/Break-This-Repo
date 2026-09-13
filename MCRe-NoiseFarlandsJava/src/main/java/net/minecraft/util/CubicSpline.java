package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public sealed interface CubicSpline<I> permits CubicSpline.Multipoint, CubicSpline.Constant {
    CubicSpline<I> mapCoordinates(UnaryOperator<I> mapper);

    float minValue();

    float maxValue();

    @VisibleForDebug
    String parityString();

    static <C, I extends BoundedFloatFunction<C>> float sample(final CubicSpline<I> spline, final C coordinate) {
        return switch (spline) {
            case CubicSpline.Multipoint<I> multipoint -> CubicSpline.Multipoint.sample(multipoint, coordinate);
            case CubicSpline.Constant<I> constant -> constant.value();
            default -> throw new MatchException(null, null);
        };
    }

    static <C, I extends BoundedFloatFunction<C>> BoundedFloatFunction<C> asSampler(final CubicSpline<I> spline) {
        return switch (spline) {
            case CubicSpline.Multipoint<I> multipoint -> new BoundedFloatFunction<C>() {
                @Override
                public float apply(final C c) {
                    return CubicSpline.Multipoint.sample(multipoint, c);
                }

                @Override
                public float minValue() {
                    return multipoint.minValue();
                }

                @Override
                public float maxValue() {
                    return multipoint.maxValue();
                }
            };
            case CubicSpline.Constant<I> constant -> BoundedFloatFunction.constant(constant.value());
            default -> throw new MatchException(null, null);
        };
    }

    static <I extends BoundedFloatFunction<?>> Codec<CubicSpline<I>> codec(final Codec<I> coordinateCodec) {
        return Codec.recursive(
            "CubicSpline",
            subSplineCodec -> Codec.either(Codec.FLOAT, CubicSpline.Multipoint.codec(coordinateCodec, subSplineCodec))
                .xmap(
                    e -> e.map(CubicSpline.Constant::new, m -> m),
                    spline -> {
                        return switch (spline) {
                            case CubicSpline.Constant(float value) -> Either.left(value);
                            case CubicSpline.Multipoint<I> multipoint -> Either.right(multipoint);
                            default -> throw new MatchException(null, null);
                        };
                    }
                )
        );
    }

    static <I> CubicSpline<I> constant(final float value) {
        return new CubicSpline.Constant<>(value);
    }

    static <I extends BoundedFloatFunction<?>> CubicSpline.Builder<I> builder(final I coordinate) {
        return new CubicSpline.Builder<>(coordinate);
    }

    static <I extends BoundedFloatFunction<?>> CubicSpline.Builder<I> builder(final I coordinate, final Float2FloatFunction valueTransformer) {
        return new CubicSpline.Builder<>(coordinate, valueTransformer);
    }

    final class Builder<I extends BoundedFloatFunction<?>> {
        private final I coordinate;
        private final Float2FloatFunction valueTransformer;
        private final FloatList locations = new FloatArrayList();
        private final List<CubicSpline<I>> values = Lists.newArrayList();
        private final FloatList derivatives = new FloatArrayList();

        private Builder(final I coordinate) {
            this(coordinate, Float2FloatFunction.identity());
        }

        private Builder(final I coordinate, final Float2FloatFunction valueTransformer) {
            this.coordinate = coordinate;
            this.valueTransformer = valueTransformer;
        }

        public CubicSpline.Builder<I> addPoint(final float location, final float value) {
            return this.addPoint(location, new CubicSpline.Constant<>(this.valueTransformer.apply(value)), 0.0F);
        }

        public CubicSpline.Builder<I> addPoint(final float location, final float value, final float derivative) {
            return this.addPoint(location, new CubicSpline.Constant<>(this.valueTransformer.apply(value)), derivative);
        }

        public CubicSpline.Builder<I> addPoint(final float location, final CubicSpline<I> sampler) {
            return this.addPoint(location, sampler, 0.0F);
        }

        private CubicSpline.Builder<I> addPoint(final float location, final CubicSpline<I> sampler, final float derivative) {
            if (!this.locations.isEmpty() && location <= this.locations.getFloat(this.locations.size() - 1)) {
                throw new IllegalArgumentException("Please register points in ascending order");
            }

            this.locations.add(location);
            this.values.add(sampler);
            this.derivatives.add(derivative);
            return this;
        }

        public CubicSpline<I> build() {
            if (this.locations.isEmpty()) {
                throw new IllegalStateException("No elements added");
            } else {
                return new CubicSpline.Multipoint(this.coordinate, this.locations.toFloatArray(), List.copyOf(this.values), this.derivatives.toFloatArray());
            }
        }
    }

    @VisibleForDebug
    record Constant<I>(float value) implements CubicSpline<I> {
        @Override
        public String parityString() {
            return String.format(Locale.ROOT, "k=%.3f", this.value);
        }

        @Override
        public float minValue() {
            return this.value;
        }

        @Override
        public float maxValue() {
            return this.value;
        }

        @Override
        public CubicSpline<I> mapCoordinates(final UnaryOperator<I> mapper) {
            return this;
        }
    }

    @VisibleForDebug
    record Multipoint<I extends BoundedFloatFunction<?>>(
        I coordinate, float[] locations, List<CubicSpline<I>> values, float[] derivatives, float minValue, float maxValue
    ) implements CubicSpline<I> {
        public Multipoint {
            validateSizes(locations, values, derivatives);
        }

        public Multipoint(final I coordinate, final float[] locations, final List<CubicSpline<I>> values, final float[] derivatives) {
            int lastIndex = locations.length - 1;
            float minValue = Float.POSITIVE_INFINITY;
            float maxValue = Float.NEGATIVE_INFINITY;
            float minInput = coordinate.minValue();
            float maxInput = coordinate.maxValue();
            if (minInput < locations[0]) {
                float edge1 = linearExtend(minInput, locations, values.get(0).minValue(), derivatives, 0);
                float edge2 = linearExtend(minInput, locations, values.get(0).maxValue(), derivatives, 0);
                minValue = Math.min(minValue, Math.min(edge1, edge2));
                maxValue = Math.max(maxValue, Math.max(edge1, edge2));
            }

            if (maxInput > locations[lastIndex]) {
                float edge1 = linearExtend(maxInput, locations, values.get(lastIndex).minValue(), derivatives, lastIndex);
                float edge2 = linearExtend(maxInput, locations, values.get(lastIndex).maxValue(), derivatives, lastIndex);
                minValue = Math.min(minValue, Math.min(edge1, edge2));
                maxValue = Math.max(maxValue, Math.max(edge1, edge2));
            }

            for (CubicSpline<I> value : values) {
                minValue = Math.min(minValue, value.minValue());
                maxValue = Math.max(maxValue, value.maxValue());
            }

            for (int i = 0; i < lastIndex; i++) {
                float x1 = locations[i];
                float x2 = locations[i + 1];
                float xDiff = x2 - x1;
                CubicSpline<I> v1 = values.get(i);
                CubicSpline<I> v2 = values.get(i + 1);
                float min1 = v1.minValue();
                float max1 = v1.maxValue();
                float min2 = v2.minValue();
                float max2 = v2.maxValue();
                float d1 = derivatives[i];
                float d2 = derivatives[i + 1];
                if (d1 != 0.0F || d2 != 0.0F) {
                    float p1 = d1 * xDiff;
                    float p2 = d2 * xDiff;
                    float minLerp1 = Math.min(min1, min2);
                    float maxLerp1 = Math.max(max1, max2);
                    float minA = p1 - max2 + min1;
                    float maxA = p1 - min2 + max1;
                    float minB = -p2 + min2 - max1;
                    float maxB = -p2 + max2 - min1;
                    float minLerp2 = Math.min(minA, minB);
                    float maxLerp2 = Math.max(maxA, maxB);
                    minValue = Math.min(minValue, minLerp1 + 0.25F * minLerp2);
                    maxValue = Math.max(maxValue, maxLerp1 + 0.25F * maxLerp2);
                }
            }

            this(coordinate, locations, values, derivatives, minValue, maxValue);
        }

        private static float linearExtend(final float input, final float[] locations, final float value, final float[] derivatives, final int index) {
            float derivative = derivatives[index];
            return derivative == 0.0F ? value : value + derivative * (input - locations[index]);
        }

        private static <I> void validateSizes(final float[] locations, final List<CubicSpline<I>> values, final float[] derivatives) {
            if (locations.length != values.size() || locations.length != derivatives.length) {
                throw new IllegalArgumentException("All lengths must be equal, got: " + locations.length + " " + values.size() + " " + derivatives.length);
            }

            if (locations.length == 0) {
                throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
            }
        }

        public static <C, I extends BoundedFloatFunction<C>> float sample(final CubicSpline.Multipoint<I> sampler, final C c) {
            return sample(sampler.coordinate, sampler.derivatives, sampler.locations, sampler.values, c);
        }

        private static <C, I extends BoundedFloatFunction<C>> float sample(
            final I coordinate, final float[] derivatives, final float[] locations, final List<CubicSpline<I>> values, final C c
        ) {
            float input = coordinate.apply(c);
            int start = findIntervalStart(locations, input);
            int lastIndex = locations.length - 1;
            if (start < 0) {
                return linearExtend(input, locations, CubicSpline.sample(values.getFirst(), c), derivatives, 0);
            }

            if (start == lastIndex) {
                return linearExtend(input, locations, CubicSpline.sample(values.get(lastIndex), c), derivatives, lastIndex);
            }

            float x1 = locations[start];
            float x2 = locations[start + 1];
            float t = (input - x1) / (x2 - x1);
            CubicSpline<I> f1 = values.get(start);
            CubicSpline<I> f2 = values.get(start + 1);
            float d1 = derivatives[start];
            float d2 = derivatives[start + 1];
            float y1 = CubicSpline.sample(f1, c);
            float y2 = CubicSpline.sample(f2, c);
            float a = d1 * (x2 - x1) - (y2 - y1);
            float b = -d2 * (x2 - x1) + (y2 - y1);
            return Mth.lerp(t, y1, y2) + t * (1.0F - t) * Mth.lerp(t, a, b);
        }

        private static int findIntervalStart(final float[] locations, final float input) {
            return Mth.binarySearch(0, locations.length, i -> input < locations[i]) - 1;
        }

        @VisibleForTesting
        @Override
        public String parityString() {
            return "Spline{coordinate="
                + this.coordinate
                + ", locations="
                + toString(this.locations)
                + ", derivatives="
                + toString(this.derivatives)
                + ", values="
                + this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]"))
                + "}";
        }

        private static String toString(final float[] arr) {
            return "["
                + IntStream.range(0, arr.length)
                    .mapToDouble(i -> arr[i])
                    .mapToObj(f -> String.format(Locale.ROOT, "%.3f", f))
                    .collect(Collectors.joining(", "))
                + "]";
        }

        @Override
        public CubicSpline<I> mapCoordinates(final UnaryOperator<I> mapper) {
            return new CubicSpline.Multipoint(
                (I)((BoundedFloatFunction)mapper.apply(this.coordinate)),
                this.locations,
                this.values.stream().map(v -> v.mapCoordinates(mapper)).toList(),
                this.derivatives
            );
        }

        public static <I extends BoundedFloatFunction<?>> Codec<CubicSpline.Multipoint<I>> codec(
            final Codec<I> coordinateCodec, final Codec<CubicSpline<I>> subSplineCodec
        ) {
            return RecordCodecBuilder.create(
                i -> i.group(
                        coordinateCodec.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate),
                        ExtraCodecs.<CubicSpline.Multipoint.Point<I>>nonEmptyList(CubicSpline.Multipoint.Point.codec(subSplineCodec).listOf())
                            .fieldOf("points")
                            .forGetter(CubicSpline.Multipoint::packToPoints)
                    )
                    .apply(i, CubicSpline.Multipoint::createFromPoints)
            );
        }

        private List<CubicSpline.Multipoint.Point<I>> packToPoints() {
            int pointCount = this.locations.length;
            List<CubicSpline.Multipoint.Point<I>> list = new ArrayList<>(pointCount);

            for (int p = 0; p < pointCount; p++) {
                list.add(new CubicSpline.Multipoint.Point(this.locations[p], this.values.get(p), this.derivatives[p]));
            }

            return list;
        }

        private static <I extends BoundedFloatFunction<?>> CubicSpline.Multipoint<I> createFromPoints(
            final I coordinate, final List<CubicSpline.Multipoint.Point<I>> points
        ) {
            int pointCount = points.size();
            float[] locations = new float[pointCount];
            ImmutableList.Builder<CubicSpline<I>> values = ImmutableList.builderWithExpectedSize(pointCount);
            float[] derivatives = new float[pointCount];

            for (int p = 0; p < pointCount; p++) {
                CubicSpline.Multipoint.Point<I> point = points.get(p);
                locations[p] = point.location();
                values.add(point.value());
                derivatives[p] = point.derivative();
            }

            return new CubicSpline.Multipoint(coordinate, locations, values.build(), derivatives);
        }

        private record Point<I extends BoundedFloatFunction<?>>(float location, CubicSpline<I> value, float derivative) {
            public static <I extends BoundedFloatFunction<?>> Codec<CubicSpline.Multipoint.Point<I>> codec(final Codec<CubicSpline<I>> subSplineCodec) {
                return RecordCodecBuilder.create(
                    i -> i.group(
                            Codec.FLOAT.fieldOf("location").forGetter(CubicSpline.Multipoint.Point::location),
                            subSplineCodec.fieldOf("value").forGetter(CubicSpline.Multipoint.Point::value),
                            Codec.FLOAT.fieldOf("derivative").forGetter(CubicSpline.Multipoint.Point::derivative)
                        )
                        .apply(i, CubicSpline.Multipoint.Point::new)
                );
            }
        }
    }
}
