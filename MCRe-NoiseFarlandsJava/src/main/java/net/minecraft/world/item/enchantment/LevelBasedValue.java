package net.minecraft.world.item.enchantment;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;

public interface LevelBasedValue {
    Codec<LevelBasedValue> DISPATCH_CODEC = BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE.byNameCodec().dispatch(LevelBasedValue::codec, c -> c);
    Codec<LevelBasedValue> CODEC = Codec.either(LevelBasedValue.Constant.CODEC, DISPATCH_CODEC)
        .xmap(
            either -> either.map(l -> (LevelBasedValue)l, r -> (LevelBasedValue)r),
            levelBasedValue -> levelBasedValue instanceof LevelBasedValue.Constant constant ? Either.left(constant) : Either.right(levelBasedValue)
        );

    static MapCodec<? extends LevelBasedValue> bootstrap(final Registry<MapCodec<? extends LevelBasedValue>> registry) {
        Registry.register(registry, "clamped", LevelBasedValue.Clamped.CODEC);
        Registry.register(registry, "fraction", LevelBasedValue.Fraction.CODEC);
        Registry.register(registry, "levels_squared", LevelBasedValue.LevelsSquared.CODEC);
        Registry.register(registry, "linear", LevelBasedValue.Linear.CODEC);
        Registry.register(registry, "exponent", LevelBasedValue.Exponent.CODEC);
        return Registry.register(registry, "lookup", LevelBasedValue.Lookup.CODEC);
    }

    static LevelBasedValue.Constant constant(final float value) {
        return new LevelBasedValue.Constant(value);
    }

    static LevelBasedValue.Linear perLevel(final float base, final float perLevelAboveFirst) {
        return new LevelBasedValue.Linear(base, perLevelAboveFirst);
    }

    static LevelBasedValue.Linear perLevel(final float perLevel) {
        return perLevel(perLevel, perLevel);
    }

    static LevelBasedValue.Lookup lookup(final List<Float> values, final LevelBasedValue fallback) {
        return new LevelBasedValue.Lookup(values, fallback);
    }

    float calculate(int level);

    MapCodec<? extends LevelBasedValue> codec();

    record Clamped(LevelBasedValue value, float min, float max) implements LevelBasedValue {
        public static final MapCodec<LevelBasedValue.Clamped> CODEC = RecordCodecBuilder.<LevelBasedValue.Clamped>mapCodec(
                i -> i.group(
                        LevelBasedValue.CODEC.fieldOf("value").forGetter(LevelBasedValue.Clamped::value),
                        Codec.FLOAT.fieldOf("min").forGetter(LevelBasedValue.Clamped::min),
                        Codec.FLOAT.fieldOf("max").forGetter(LevelBasedValue.Clamped::max)
                    )
                    .apply(i, LevelBasedValue.Clamped::new)
            )
            .validate(u -> u.max <= u.min ? DataResult.error(() -> "Max must be larger than min, min: " + u.min + ", max: " + u.max) : DataResult.success(u));

        @Override
        public float calculate(final int level) {
            return Mth.clamp(this.value.calculate(level), this.min, this.max);
        }

        @Override
        public MapCodec<LevelBasedValue.Clamped> codec() {
            return CODEC;
        }
    }

    record Constant(float value) implements LevelBasedValue {
        public static final Codec<LevelBasedValue.Constant> CODEC = Codec.FLOAT.xmap(LevelBasedValue.Constant::new, LevelBasedValue.Constant::value);
        public static final MapCodec<LevelBasedValue.Constant> TYPED_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.FLOAT.fieldOf("value").forGetter(LevelBasedValue.Constant::value)).apply(i, LevelBasedValue.Constant::new)
        );

        @Override
        public float calculate(final int level) {
            return this.value;
        }

        @Override
        public MapCodec<LevelBasedValue.Constant> codec() {
            return TYPED_CODEC;
        }
    }

    record Exponent(LevelBasedValue base, LevelBasedValue power) implements LevelBasedValue {
        public static final MapCodec<LevelBasedValue.Exponent> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    LevelBasedValue.CODEC.fieldOf("base").forGetter(LevelBasedValue.Exponent::base),
                    LevelBasedValue.CODEC.fieldOf("power").forGetter(LevelBasedValue.Exponent::power)
                )
                .apply(i, LevelBasedValue.Exponent::new)
        );

        @Override
        public float calculate(final int level) {
            return (float)Math.pow(this.base.calculate(level), this.power.calculate(level));
        }

        @Override
        public MapCodec<LevelBasedValue.Exponent> codec() {
            return CODEC;
        }
    }

    record Fraction(LevelBasedValue numerator, LevelBasedValue denominator) implements LevelBasedValue {
        public static final MapCodec<LevelBasedValue.Fraction> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    LevelBasedValue.CODEC.fieldOf("numerator").forGetter(LevelBasedValue.Fraction::numerator),
                    LevelBasedValue.CODEC.fieldOf("denominator").forGetter(LevelBasedValue.Fraction::denominator)
                )
                .apply(i, LevelBasedValue.Fraction::new)
        );

        @Override
        public float calculate(final int level) {
            float denominator = this.denominator.calculate(level);
            return denominator == 0.0F ? 0.0F : this.numerator.calculate(level) / denominator;
        }

        @Override
        public MapCodec<LevelBasedValue.Fraction> codec() {
            return CODEC;
        }
    }

    record LevelsSquared(float added) implements LevelBasedValue {
        public static final MapCodec<LevelBasedValue.LevelsSquared> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(Codec.FLOAT.fieldOf("added").forGetter(LevelBasedValue.LevelsSquared::added)).apply(i, LevelBasedValue.LevelsSquared::new)
        );

        @Override
        public float calculate(final int level) {
            return Mth.square(level) + this.added;
        }

        @Override
        public MapCodec<LevelBasedValue.LevelsSquared> codec() {
            return CODEC;
        }
    }

    record Linear(float base, float perLevelAboveFirst) implements LevelBasedValue {
        public static final MapCodec<LevelBasedValue.Linear> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.FLOAT.fieldOf("base").forGetter(LevelBasedValue.Linear::base),
                    Codec.FLOAT.fieldOf("per_level_above_first").forGetter(LevelBasedValue.Linear::perLevelAboveFirst)
                )
                .apply(i, LevelBasedValue.Linear::new)
        );

        @Override
        public float calculate(final int level) {
            return this.base + this.perLevelAboveFirst * (level - 1);
        }

        @Override
        public MapCodec<LevelBasedValue.Linear> codec() {
            return CODEC;
        }
    }

    record Lookup(List<Float> values, LevelBasedValue fallback) implements LevelBasedValue {
        public static final MapCodec<LevelBasedValue.Lookup> CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.FLOAT.listOf().fieldOf("values").forGetter(LevelBasedValue.Lookup::values),
                    LevelBasedValue.CODEC.fieldOf("fallback").forGetter(LevelBasedValue.Lookup::fallback)
                )
                .apply(i, LevelBasedValue.Lookup::new)
        );

        @Override
        public float calculate(final int level) {
            return level <= this.values.size() ? this.values.get(level - 1) : this.fallback.calculate(level);
        }

        @Override
        public MapCodec<LevelBasedValue.Lookup> codec() {
            return CODEC;
        }
    }
}