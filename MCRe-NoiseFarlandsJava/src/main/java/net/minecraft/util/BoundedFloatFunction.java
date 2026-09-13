package net.minecraft.util;

import java.util.function.Function;

public interface BoundedFloatFunction<C> {
    BoundedFloatFunction<Float> IDENTITY = new BoundedFloatFunction<Float>() {
        public float apply(final Float value) {
            return value;
        }

        @Override
        public float minValue() {
            return Float.NEGATIVE_INFINITY;
        }

        @Override
        public float maxValue() {
            return Float.POSITIVE_INFINITY;
        }
    };

    float apply(final C c);

    float minValue();

    float maxValue();

    static <C> BoundedFloatFunction<C> constant(final float value) {
        return new BoundedFloatFunction<C>() {
            @Override
            public float apply(final C c) {
                return value;
            }

            @Override
            public float minValue() {
                return value;
            }

            @Override
            public float maxValue() {
                return value;
            }
        };
    }

    default <C2> BoundedFloatFunction<C2> comap(final Function<C2, C> function) {
        final BoundedFloatFunction<C> outer = this;
        return new BoundedFloatFunction<C2>() {
            @Override
            public float apply(final C2 c2) {
                return outer.apply(function.apply(c2));
            }

            @Override
            public float minValue() {
                return outer.minValue();
            }

            @Override
            public float maxValue() {
                return outer.maxValue();
            }
        };
    }
}