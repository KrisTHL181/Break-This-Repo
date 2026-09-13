package net.minecraft.util.valueproviders;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class IntProviders {
    private static final Codec<Either<Integer, IntProvider>> CONSTANT_OR_DISPATCH_CODEC = Codec.either(
        Codec.INT, BuiltInRegistries.INT_PROVIDER_TYPE.byNameCodec().dispatch(IntProvider::codec, t -> t)
    );
    public static final Codec<IntProvider> CODEC = CONSTANT_OR_DISPATCH_CODEC.xmap(
        either -> either.map(ConstantInt::of, f -> (IntProvider)f),
        f -> f instanceof ConstantInt constantInt ? Either.left(constantInt.value()) : Either.right(f)
    );
    public static final Codec<IntProvider> NON_NEGATIVE_CODEC = codec(0, Integer.MAX_VALUE);
    public static final Codec<IntProvider> POSITIVE_CODEC = codec(1, Integer.MAX_VALUE);

    public static Codec<IntProvider> codec(final int minValue, final int maxValue) {
        return validateCodec(minValue, maxValue, CODEC);
    }

    public static <T extends IntProvider> Codec<T> validateCodec(final int minValue, final int maxValue, final Codec<T> codec) {
        return codec.validate(value -> validate(minValue, maxValue, value));
    }

    private static <T extends IntProvider> DataResult<T> validate(final int minValue, final int maxValue, final T value) {
        if (value.minInclusive() < minValue) {
            return DataResult.error(() -> "Value provider too low: " + minValue + " [" + value.minInclusive() + "-" + value.maxInclusive() + "]");
        } else {
            return value.maxInclusive() > maxValue
                ? DataResult.error(() -> "Value provider too high: " + maxValue + " [" + value.minInclusive() + "-" + value.maxInclusive() + "]")
                : DataResult.success(value);
        }
    }

    public static MapCodec<? extends IntProvider> bootstrap(final Registry<MapCodec<? extends IntProvider>> registry) {
        Registry.register(registry, "constant", ConstantInt.MAP_CODEC);
        Registry.register(registry, "uniform", UniformInt.MAP_CODEC);
        Registry.register(registry, "biased_to_bottom", BiasedToBottomInt.MAP_CODEC);
        Registry.register(registry, "clamped", ClampedInt.MAP_CODEC);
        Registry.register(registry, "weighted_list", WeightedListInt.MAP_CODEC);
        Registry.register(registry, "clamped_normal", ClampedNormalInt.MAP_CODEC);
        return Registry.register(registry, "trapezoid", TrapezoidInt.MAP_CODEC);
    }
}