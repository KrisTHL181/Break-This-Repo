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
   Codec<LevelBasedValue> DISPATCH_CODEC = BuiltInRegistries.ENCHANTMENT_LEVEL_BASED_VALUE_TYPE
      .byNameCodec()
      .dispatch(LevelBasedValue::codec, p_344739_ -> p_344739_);
   Codec<LevelBasedValue> CODEC = Codec.either(LevelBasedValue.Constant.CODEC, DISPATCH_CODEC)
      .xmap(
         p_342800_ -> (LevelBasedValue)p_342800_.map(p_345300_ -> p_345300_, p_342996_ -> p_342996_),
         p_343036_ -> p_343036_ instanceof LevelBasedValue.Constant levelbasedvalue$constant ? Either.left(levelbasedvalue$constant) : Either.right(p_343036_)
      );

   static MapCodec<? extends LevelBasedValue> bootstrap(Registry<MapCodec<? extends LevelBasedValue>> p_342464_) {
      Registry.register(p_342464_, "clamped", LevelBasedValue.Clamped.CODEC);
      Registry.register(p_342464_, "fraction", LevelBasedValue.Fraction.CODEC);
      Registry.register(p_342464_, "levels_squared", LevelBasedValue.LevelsSquared.CODEC);
      Registry.register(p_342464_, "linear", LevelBasedValue.Linear.CODEC);
      Registry.register(p_342464_, "exponent", LevelBasedValue.Exponent.CODEC);
      return Registry.register(p_342464_, "lookup", LevelBasedValue.Lookup.CODEC);
   }

   static LevelBasedValue.Constant constant(float p_343866_) {
      return new LevelBasedValue.Constant(p_343866_);
   }

   static LevelBasedValue.Linear perLevel(float p_343120_, float p_345457_) {
      return new LevelBasedValue.Linear(p_343120_, p_345457_);
   }

   static LevelBasedValue.Linear perLevel(float p_343073_) {
      return perLevel(p_343073_, p_343073_);
   }

   static LevelBasedValue.Lookup lookup(List<Float> p_342101_, LevelBasedValue p_345072_) {
      return new LevelBasedValue.Lookup(p_342101_, p_345072_);
   }

   float calculate(int var1);

   MapCodec<? extends LevelBasedValue> codec();

   record Clamped(LevelBasedValue value, float min, float max) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Clamped> CODEC = RecordCodecBuilder.mapCodec(
            p_343138_ -> p_343138_.group(
                  LevelBasedValue.CODEC.fieldOf("value").forGetter(LevelBasedValue.Clamped::value),
                  Codec.FLOAT.fieldOf("min").forGetter(LevelBasedValue.Clamped::min),
                  Codec.FLOAT.fieldOf("max").forGetter(LevelBasedValue.Clamped::max)
               )
               .apply(p_343138_, LevelBasedValue.Clamped::new)
         )
         .validate(
            p_345252_ -> p_345252_.max <= p_345252_.min
               ? DataResult.error(() -> "Max must be larger than min, min: " + p_345252_.min + ", max: " + p_345252_.max)
               : DataResult.success(p_345252_)
         );

      @Override
      public float calculate(int p_342880_) {
         return Mth.clamp(this.value.calculate(p_342880_), this.min, this.max);
      }

      @Override
      public MapCodec<LevelBasedValue.Clamped> codec() {
         return CODEC;
      }
   }

   record Constant(float value) implements LevelBasedValue {
      public static final Codec<LevelBasedValue.Constant> CODEC = Codec.FLOAT.xmap(LevelBasedValue.Constant::new, LevelBasedValue.Constant::value);
      public static final MapCodec<LevelBasedValue.Constant> TYPED_CODEC = RecordCodecBuilder.mapCodec(
         p_345310_ -> p_345310_.group(Codec.FLOAT.fieldOf("value").forGetter(LevelBasedValue.Constant::value)).apply(p_345310_, LevelBasedValue.Constant::new)
      );

      @Override
      public float calculate(int p_342950_) {
         return this.value;
      }

      @Override
      public MapCodec<LevelBasedValue.Constant> codec() {
         return TYPED_CODEC;
      }
   }

   record Exponent(LevelBasedValue base, LevelBasedValue power) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Exponent> CODEC = RecordCodecBuilder.mapCodec(
         p_460161_ -> p_460161_.group(
               LevelBasedValue.CODEC.fieldOf("base").forGetter(LevelBasedValue.Exponent::base),
               LevelBasedValue.CODEC.fieldOf("power").forGetter(LevelBasedValue.Exponent::power)
            )
            .apply(p_460161_, LevelBasedValue.Exponent::new)
      );

      @Override
      public float calculate(int p_458183_) {
         return (float)Math.pow(this.base.calculate(p_458183_), this.power.calculate(p_458183_));
      }

      @Override
      public MapCodec<LevelBasedValue.Exponent> codec() {
         return CODEC;
      }
   }

   record Fraction(LevelBasedValue numerator, LevelBasedValue denominator) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Fraction> CODEC = RecordCodecBuilder.mapCodec(
         p_342531_ -> p_342531_.group(
               LevelBasedValue.CODEC.fieldOf("numerator").forGetter(LevelBasedValue.Fraction::numerator),
               LevelBasedValue.CODEC.fieldOf("denominator").forGetter(LevelBasedValue.Fraction::denominator)
            )
            .apply(p_342531_, LevelBasedValue.Fraction::new)
      );

      @Override
      public float calculate(int p_344335_) {
         float f = this.denominator.calculate(p_344335_);
         return f == 0.0F ? 0.0F : this.numerator.calculate(p_344335_) / f;
      }

      @Override
      public MapCodec<LevelBasedValue.Fraction> codec() {
         return CODEC;
      }
   }

   record LevelsSquared(float added) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.LevelsSquared> CODEC = RecordCodecBuilder.mapCodec(
         p_342272_ -> p_342272_.group(Codec.FLOAT.fieldOf("added").forGetter(LevelBasedValue.LevelsSquared::added))
            .apply(p_342272_, LevelBasedValue.LevelsSquared::new)
      );

      @Override
      public float calculate(int p_345311_) {
         return Mth.square(p_345311_) + this.added;
      }

      @Override
      public MapCodec<LevelBasedValue.LevelsSquared> codec() {
         return CODEC;
      }
   }

   record Linear(float base, float perLevelAboveFirst) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Linear> CODEC = RecordCodecBuilder.mapCodec(
         p_345355_ -> p_345355_.group(
               Codec.FLOAT.fieldOf("base").forGetter(LevelBasedValue.Linear::base),
               Codec.FLOAT.fieldOf("per_level_above_first").forGetter(LevelBasedValue.Linear::perLevelAboveFirst)
            )
            .apply(p_345355_, LevelBasedValue.Linear::new)
      );

      @Override
      public float calculate(int p_343508_) {
         return this.base + this.perLevelAboveFirst * (p_343508_ - 1);
      }

      @Override
      public MapCodec<LevelBasedValue.Linear> codec() {
         return CODEC;
      }
   }

   record Lookup(List<Float> values, LevelBasedValue fallback) implements LevelBasedValue {
      public static final MapCodec<LevelBasedValue.Lookup> CODEC = RecordCodecBuilder.mapCodec(
         p_342915_ -> p_342915_.group(
               Codec.FLOAT.listOf().fieldOf("values").forGetter(LevelBasedValue.Lookup::values),
               LevelBasedValue.CODEC.fieldOf("fallback").forGetter(LevelBasedValue.Lookup::fallback)
            )
            .apply(p_342915_, LevelBasedValue.Lookup::new)
      );

      @Override
      public float calculate(int p_342461_) {
         return p_342461_ <= this.values.size() ? this.values.get(p_342461_ - 1) : this.fallback.calculate(p_342461_);
      }

      @Override
      public MapCodec<LevelBasedValue.Lookup> codec() {
         return CODEC;
      }
   }
}
