package net.minecraft.util;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

public final class Interval {
   public static final Interval NaI = new Interval(Double.NaN, Double.NaN);
   public static final Interval INFINITE = new Interval(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
   private static final Interval NEGATIVE_ONE_TO_ONE = new Interval(-1.0, 1.0);
   private static final Interval ZERO_TO_ONE = new Interval(0.0, 1.0);
   private final double min;
   private final double max;

   private Interval(final double min, final double max) {
      this.min = min;
      this.max = max;
   }

   public static Interval of(final double min, final double max) {
      if (max < min) {
         throw new IllegalArgumentException("max (" + max + ") < min (" + min + ")");
      }

      if (Double.isNaN(min) || Double.isNaN(max)) {
         throw new IllegalArgumentException("Bounds cannot include NaN [" + min + "; " + max + "]: use Interval.NaI explicitly");
      }

      if (min == Double.NEGATIVE_INFINITY && max == Double.POSITIVE_INFINITY) {
         return INFINITE;
      }

      if (max == 1.0) {
         if (min == 0.0) {
            return ZERO_TO_ONE;
         }

         if (min == -1.0) {
            return NEGATIVE_ONE_TO_ONE;
         }
      }

      return new Interval(min, max);
   }

   public static Interval ofSymmetric(final double range) {
      return of(-range, range);
   }

   public static Interval ofExact(final double value) {
      return of(value, value);
   }

   public static Interval encapsulating(final List<Interval> intervals) {
      if (intervals.isEmpty()) {
         throw new IllegalArgumentException("At least one interval required");
      }

      double min = Double.POSITIVE_INFINITY;
      double max = Double.NEGATIVE_INFINITY;

      for (Interval interval : intervals) {
         if (!interval.isNaI()) {
            min = Math.min(interval.min, min);
            max = Math.max(interval.max, max);
         }
      }

      return max < min ? NaI : of(min, max);
   }

   public static Interval encapsulating(final Interval... intervals) {
      return encapsulating(List.of(intervals));
   }

   public static Interval encapsulating(final double first, final double second) {
      if (Double.isNaN(first) && Double.isNaN(second)) {
         return NaI;
      } else if (Double.isNaN(first)) {
         return ofExact(second);
      } else {
         return Double.isNaN(second) ? ofExact(first) : of(Math.min(first, second), Math.max(first, second));
      }
   }

   public static Interval add(final Interval left, final Interval right) {
      double min = left.min + right.min;
      double max = left.max + right.max;
      return !Double.isNaN(min) && !Double.isNaN(max) ? of(min, max) : NaI;
   }

   public static Interval sub(final Interval left, final Interval right) {
      double min = left.min - right.max;
      double max = left.max - right.min;
      return !Double.isNaN(min) && !Double.isNaN(max) ? of(min, max) : NaI;
   }

   public static Interval mul(final Interval left, final Interval right) {
      if (!left.isNaI() && !right.isNaI()) {
         double minMin = mulBound(left.min, right.min);
         double minMax = mulBound(left.min, right.max);
         double maxMin = mulBound(left.max, right.min);
         double maxMax = mulBound(left.max, right.max);
         return of(Math.min(Math.min(minMin, minMax), Math.min(maxMin, maxMax)), Math.max(Math.max(minMin, minMax), Math.max(maxMin, maxMax)));
      } else {
         return NaI;
      }
   }

   private static double mulBound(final double left, final double right) {
      return left != 0.0 && right != 0.0 ? left * right : 0.0;
   }

   public static Interval inverse(final Interval input) {
      if (!input.isNaI() && (input.min != 0.0 || input.max != 0.0)) {
         if (!input.contains(0.0)) {
            return of(1.0 / input.max, 1.0 / input.min);
         } else if (input.max == 0.0) {
            return of(Double.NEGATIVE_INFINITY, 1.0 / input.min);
         } else {
            return input.min == 0.0 ? of(1.0 / input.max, Double.POSITIVE_INFINITY) : INFINITE;
         }
      } else {
         return NaI;
      }
   }

   public static Interval div(final Interval left, final Interval right) {
      return mul(left, inverse(right));
   }

   public static Interval min(final Interval left, final Interval right) {
      return !left.isNaI() && !right.isNaI() ? of(Math.min(left.min, right.min), Math.min(left.max, right.max)) : NaI;
   }

   public static Interval max(final Interval left, final Interval right) {
      return !left.isNaI() && !right.isNaI() ? of(Math.max(left.min, right.min), Math.max(left.max, right.max)) : NaI;
   }

   public static Interval clamp(final Interval input, final double min, final double max) {
      if (min > max) {
         throw new IllegalArgumentException("min (" + min + ") > max (" + max + ")");
      } else if (input.isNaI()) {
         return NaI;
      } else if (input.min >= max) {
         return of(max, max);
      } else {
         return input.max <= min ? of(min, min) : of(Math.max(input.min, min), Math.min(input.max, max));
      }
   }

   public static Interval abs(final Interval input) {
      if (input.isNaI()) {
         return NaI;
      }

      double max = Math.max(Math.abs(input.min), Math.abs(input.max));
      return input.contains(0.0) ? of(0.0, max) : of(Math.min(Math.abs(input.min), Math.abs(input.max)), max);
   }

   public static Interval square(final Interval input) {
      if (input.isNaI()) {
         return NaI;
      }

      double max = Math.max(Mth.square(input.min), Mth.square(input.max));
      return input.contains(0.0) ? of(0.0, max) : of(Math.min(Mth.square(input.min), Mth.square(input.max)), max);
   }

   public static Interval mapMonotonic(final Interval input, final DoubleUnaryOperator monotonicOp) {
      if (input.isNaI()) {
         return NaI;
      } else {
         double mappedMin = monotonicOp.applyAsDouble(input.min);
         double mappedMax = monotonicOp.applyAsDouble(input.max);
         if (!Double.isNaN(mappedMin) && !Double.isNaN(mappedMax)) {
            return of(Math.min(mappedMin, mappedMax), Math.max(mappedMin, mappedMax));
         } else {
            throw new IllegalStateException("Monotonic operator should not produce NaN");
         }
      }
   }

   public static Interval lerp(final Interval alpha, final Interval first, final Interval second) {
      return !alpha.isNaI() && !first.isNaI() && !second.isNaI()
         ? encapsulating(
            lerp(alpha, first.min, second.min), lerp(alpha, first.max, second.min), lerp(alpha, first.min, second.max), lerp(alpha, first.max, second.max)
         )
         : NaI;
   }

   public static Interval lerp(final Interval alpha, final double first, final double second) {
      if (alpha.isNaI() || Double.isNaN(first) || Double.isNaN(second)) {
         return NaI;
      } else {
         return Double.isFinite(first) && Double.isFinite(second) ? lerpFiniteBounds(alpha, first, second) : lerpInfiniteBounds(alpha, first, second);
      }
   }

   private static Interval lerpFiniteBounds(final Interval alpha, final double first, final double second) {
      return encapsulating(lerpFiniteBound(alpha.min, first, second), lerpFiniteBound(alpha.max, first, second));
   }

   private static double lerpFiniteBound(final double alpha, final double first, final double second) {
      return first + mulBound(alpha, second - first);
   }

   private static Interval lerpInfiniteBounds(final Interval alpha, final double first, final double second) {
      if (first == second) {
         return ofExact(first);
      }

      double newMin = lerpInfiniteBound(alpha.min, first, second);
      double newMax = lerpInfiniteBound(alpha.max, first, second);
      return !Double.isNaN(newMin) && !Double.isNaN(newMax) ? encapsulating(newMin, newMax) : NaI;
   }

   private static double lerpInfiniteBound(final double alpha, final double first, final double second) {
      double firstPart = mulBound(1.0 - alpha, first);
      double secondPart = mulBound(alpha, second);
      if (!Double.isInfinite(firstPart) || !Double.isInfinite(secondPart)) {
         return firstPart + secondPart;
      } else if (alpha <= 0.0) {
         return second > first ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
      } else if (alpha >= 1.0) {
         return second > first ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
      } else {
         return Double.NaN;
      }
   }

   public boolean contains(final double value) {
      return value >= this.min && value <= this.max;
   }

   public boolean intersects(final Interval other) {
      return this.min <= other.max && this.max >= other.min;
   }

   public boolean isNaI() {
      return this == NaI;
   }

   public double min() {
      return this.min;
   }

   public double max() {
      return this.max;
   }

   @Override
   public boolean equals(final Object obj) {
      return obj == this ? true : obj instanceof Interval interval && this.min == interval.min && this.max == interval.max;
   }

   @Override
   public int hashCode() {
      int hash = Double.hashCode(this.min);
      return hash * 31 + Double.hashCode(this.max);
   }

   @Override
   public String toString() {
      return this.isNaI() ? "[NaN]" : "[" + this.min + "; " + this.max + "]";
   }
}
