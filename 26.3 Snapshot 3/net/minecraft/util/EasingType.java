package net.minecraft.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public interface EasingType {
   ExtraCodecs.LateBoundIdMapper<String, EasingType> SIMPLE_REGISTRY = new ExtraCodecs.LateBoundIdMapper<>();
   Codec<EasingType> CODEC = Codec.either(SIMPLE_REGISTRY.codec(Codec.STRING), EasingType.CubicBezier.CODEC)
      .xmap(Either::unwrap, easing -> easing instanceof EasingType.CubicBezier bezier ? Either.right(bezier) : Either.left(easing));
   EasingType CONSTANT = registerSimple("constant", x -> 0.0F);
   EasingType LINEAR = registerSimple("linear", x -> x);
   EasingType IN_BACK = registerSimple("in_back", Ease::inBack);
   EasingType IN_BOUNCE = registerSimple("in_bounce", Ease::inBounce);
   EasingType IN_CIRC = registerSimple("in_circ", Ease::inCirc);
   EasingType IN_CUBIC = registerSimple("in_cubic", Ease::inCubic);
   EasingType IN_ELASTIC = registerSimple("in_elastic", Ease::inElastic);
   EasingType IN_EXPO = registerSimple("in_expo", Ease::inExpo);
   EasingType IN_QUAD = registerSimple("in_quad", Ease::inQuad);
   EasingType IN_QUART = registerSimple("in_quart", Ease::inQuart);
   EasingType IN_QUINT = registerSimple("in_quint", Ease::inQuint);
   EasingType IN_SINE = registerSimple("in_sine", Ease::inSine);
   EasingType IN_OUT_BACK = registerSimple("in_out_back", Ease::inOutBack);
   EasingType IN_OUT_BOUNCE = registerSimple("in_out_bounce", Ease::inOutBounce);
   EasingType IN_OUT_CIRC = registerSimple("in_out_circ", Ease::inOutCirc);
   EasingType IN_OUT_CUBIC = registerSimple("in_out_cubic", Ease::inOutCubic);
   EasingType IN_OUT_ELASTIC = registerSimple("in_out_elastic", Ease::inOutElastic);
   EasingType IN_OUT_EXPO = registerSimple("in_out_expo", Ease::inOutExpo);
   EasingType IN_OUT_QUAD = registerSimple("in_out_quad", Ease::inOutQuad);
   EasingType IN_OUT_QUART = registerSimple("in_out_quart", Ease::inOutQuart);
   EasingType IN_OUT_QUINT = registerSimple("in_out_quint", Ease::inOutQuint);
   EasingType IN_OUT_SINE = registerSimple("in_out_sine", Ease::inOutSine);
   EasingType OUT_BACK = registerSimple("out_back", Ease::outBack);
   EasingType OUT_BOUNCE = registerSimple("out_bounce", Ease::outBounce);
   EasingType OUT_CIRC = registerSimple("out_circ", Ease::outCirc);
   EasingType OUT_CUBIC = registerSimple("out_cubic", Ease::outCubic);
   EasingType OUT_ELASTIC = registerSimple("out_elastic", Ease::outElastic);
   EasingType OUT_EXPO = registerSimple("out_expo", Ease::outExpo);
   EasingType OUT_QUAD = registerSimple("out_quad", Ease::outQuad);
   EasingType OUT_QUART = registerSimple("out_quart", Ease::outQuart);
   EasingType OUT_QUINT = registerSimple("out_quint", Ease::outQuint);
   EasingType OUT_SINE = registerSimple("out_sine", Ease::outSine);

   static EasingType registerSimple(final String id, final EasingType easing) {
      SIMPLE_REGISTRY.put(id, easing);
      return easing;
   }

   static EasingType cubicBezier(final float x1, final float y1, final float x2, final float y2) {
      return new EasingType.CubicBezier(new EasingType.CubicBezierControls(x1, y1, x2, y2));
   }

   static EasingType symmetricCubicBezier(final float x1, final float y1) {
      return cubicBezier(x1, y1, 1.0F - x1, 1.0F - y1);
   }

   float apply(float x);

   final class CubicBezier implements EasingType {
      public static final Codec<EasingType.CubicBezier> CODEC = RecordCodecBuilder.create(
         i -> i.group(EasingType.CubicBezierControls.CODEC.fieldOf("cubic_bezier").forGetter(b -> b.controls)).apply(i, EasingType.CubicBezier::new)
      );
      private static final int NEWTON_RAPHSON_ITERATIONS = 4;
      private static final float MAX_STEP = 0.25F;
      private final EasingType.CubicBezierControls controls;
      private final EasingType.CubicBezier.CubicCurve xCurve;
      private final EasingType.CubicBezier.CubicCurve yCurve;

      public CubicBezier(final EasingType.CubicBezierControls controls) {
         this.controls = controls;
         this.xCurve = curveFromControls(controls.x1, controls.x2);
         this.yCurve = curveFromControls(controls.y1, controls.y2);
      }

      private static EasingType.CubicBezier.CubicCurve curveFromControls(final float v1, final float v2) {
         return new EasingType.CubicBezier.CubicCurve(3.0F * v1 - 3.0F * v2 + 1.0F, -6.0F * v1 + 3.0F * v2, 3.0F * v1);
      }

      @Override
      public float apply(final float x) {
         return this.yCurve.sample(this.solveT(x));
      }

      private float solveT(final float x) {
         float t = x;

         for (int i = 0; i < 4; i++) {
            float error = this.xCurve.sample(t) - x;
            if (Math.abs(error) < 1.0E-5F) {
               return t;
            }

            float gradient = this.xCurve.sampleGradient(t);
            if (gradient < 1.0E-5F) {
               break;
            }

            t -= Mth.clamp(error / gradient, -0.25F, 0.25F);
         }

         return this.solveTBisect(x, t);
      }

      private float solveTBisect(final float x, final float initialT) {
         float t0 = 0.0F;
         float t1 = 1.0F;

         float t;
         for (t = initialT; t0 < t1; t = (t1 + t0) / 2.0F) {
            float error = this.xCurve.sample(t) - x;
            if (Math.abs(error) < 1.0E-5F) {
               return t;
            }

            if (error < 0.0F) {
               t0 = t;
            } else {
               t1 = t;
            }
         }

         return t;
      }

      @Override
      public boolean equals(final Object obj) {
         return obj instanceof EasingType.CubicBezier bezier && this.controls.equals(bezier.controls);
      }

      @Override
      public int hashCode() {
         return this.controls.hashCode();
      }

      @Override
      public String toString() {
         return "CubicBezier(" + this.controls.x1 + ", " + this.controls.y1 + ", " + this.controls.x2 + ", " + this.controls.y2 + ")";
      }

      private record CubicCurve(float a, float b, float c) {
         public float sample(final float t) {
            return ((this.a * t + this.b) * t + this.c) * t;
         }

         public float sampleGradient(final float t) {
            return (3.0F * this.a * t + 2.0F * this.b) * t + this.c;
         }
      }
   }

   record CubicBezierControls(float x1, float y1, float x2, float y2) {
      public static final Codec<EasingType.CubicBezierControls> CODEC = Codec.FLOAT
         .listOf(4, 4)
         .xmap(
            floats -> new EasingType.CubicBezierControls((Float)floats.get(0), (Float)floats.get(1), (Float)floats.get(2), (Float)floats.get(3)),
            controls -> List.of(controls.x1, controls.y1, controls.x2, controls.y2)
         )
         .validate(EasingType.CubicBezierControls::validate);

      private DataResult<EasingType.CubicBezierControls> validate() {
         if (this.x1 < 0.0F || this.x1 > 1.0F) {
            return DataResult.error(() -> "x1 must be in range [0; 1]");
         } else {
            return !(this.x2 < 0.0F) && !(this.x2 > 1.0F) ? DataResult.success(this) : DataResult.error(() -> "x2 must be in range [0; 1]");
         }
      }
   }
}
