package net.minecraft.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.mutable.MutableObject;

public interface CubicSpline<C, I extends BoundedFloatFunction<C>> extends BoundedFloatFunction<C> {
   @VisibleForDebug
   String parityString();

   CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> var1);

   static <C, I extends BoundedFloatFunction<C>> Codec<CubicSpline<C, I>> codec(Codec<I> p_184263_) {
      MutableObject<Codec<CubicSpline<C, I>>> mutableobject = new MutableObject();

      record Point<C, I extends BoundedFloatFunction<C>>(float location, CubicSpline<C, I> value, float derivative) {
      }

      Codec<Point<C, I>> codec = RecordCodecBuilder.create(
         p_449239_ -> p_449239_.group(
               Codec.FLOAT.fieldOf("location").forGetter(Point::location),
               Codec.lazyInitialized(mutableobject).fieldOf("value").forGetter(Point::value),
               Codec.FLOAT.fieldOf("derivative").forGetter(Point::derivative)
            )
            .apply(p_449239_, (p_184242_, p_184243_, p_184244_) -> new Point(p_184242_, p_184243_, p_184244_))
      );
      Codec<CubicSpline.Multipoint<C, I>> codec1 = RecordCodecBuilder.create(
         p_184267_ -> p_184267_.group(
               p_184263_.fieldOf("coordinate").forGetter(CubicSpline.Multipoint::coordinate),
               ExtraCodecs.nonEmptyList(codec.listOf())
                  .fieldOf("points")
                  .forGetter(
                     p_184272_ -> IntStream.range(0, p_184272_.locations.length)
                        .mapToObj(
                           p_184249_ -> new Point(
                              p_184272_.locations()[p_184249_], (CubicSpline<C, I>)p_184272_.values().get(p_184249_), p_184272_.derivatives()[p_184249_]
                           )
                        )
                        .toList()
                  )
            )
            .apply(p_184267_, (p_426224_, p_184259_) -> {
               float[] afloat = new float[p_184259_.size()];
               com.google.common.collect.ImmutableList.Builder<CubicSpline<C, I>> builder = ImmutableList.builder();
               float[] afloat1 = new float[p_184259_.size()];

               for (int i = 0; i < p_184259_.size(); i++) {
                  Point<C, I> point = (Point<C, I>)p_184259_.get(i);
                  afloat[i] = point.location();
                  builder.add(point.value());
                  afloat1[i] = point.derivative();
               }

               return CubicSpline.Multipoint.create((I)p_426224_, afloat, builder.build(), afloat1);
            })
      );
      mutableobject.setValue(
         Codec.either(Codec.FLOAT, codec1)
            .xmap(
               p_184261_ -> (CubicSpline)p_184261_.map(CubicSpline.Constant::new, p_184246_ -> p_184246_),
               p_184251_ -> p_184251_ instanceof CubicSpline.Constant<C, I> constant
                  ? Either.left(constant.value())
                  : Either.right((CubicSpline.Multipoint)p_184251_)
            )
      );
      return (Codec<CubicSpline<C, I>>)mutableobject.get();
   }

   static <C, I extends BoundedFloatFunction<C>> CubicSpline<C, I> constant(float p_184240_) {
      return new CubicSpline.Constant<>(p_184240_);
   }

   static <C, I extends BoundedFloatFunction<C>> CubicSpline.Builder<C, I> builder(I p_423759_) {
      return new CubicSpline.Builder<>(p_423759_);
   }

   static <C, I extends BoundedFloatFunction<C>> CubicSpline.Builder<C, I> builder(I p_427148_, BoundedFloatFunction<Float> p_429627_) {
      return new CubicSpline.Builder<>(p_427148_, p_429627_);
   }

   final class Builder<C, I extends BoundedFloatFunction<C>> {
      private final I coordinate;
      private final BoundedFloatFunction<Float> valueTransformer;
      private final FloatList locations = new FloatArrayList();
      private final List<CubicSpline<C, I>> values = Lists.newArrayList();
      private final FloatList derivatives = new FloatArrayList();

      protected Builder(I p_423417_) {
         this(p_423417_, BoundedFloatFunction.IDENTITY);
      }

      protected Builder(I p_425868_, BoundedFloatFunction<Float> p_426365_) {
         this.coordinate = p_425868_;
         this.valueTransformer = p_426365_;
      }

      public CubicSpline.Builder<C, I> addPoint(float p_216115_, float p_216116_) {
         return this.addPoint(p_216115_, new CubicSpline.Constant<>(this.valueTransformer.apply(p_216116_)), 0.0F);
      }

      public CubicSpline.Builder<C, I> addPoint(float p_184299_, float p_184300_, float p_184301_) {
         return this.addPoint(p_184299_, new CubicSpline.Constant<>(this.valueTransformer.apply(p_184300_)), p_184301_);
      }

      public CubicSpline.Builder<C, I> addPoint(float p_216118_, CubicSpline<C, I> p_216119_) {
         return this.addPoint(p_216118_, p_216119_, 0.0F);
      }

      private CubicSpline.Builder<C, I> addPoint(float p_184303_, CubicSpline<C, I> p_184304_, float p_184305_) {
         if (!this.locations.isEmpty() && p_184303_ <= this.locations.getFloat(this.locations.size() - 1)) {
            throw new IllegalArgumentException("Please register points in ascending order");
         }

         this.locations.add(p_184303_);
         this.values.add(p_184304_);
         this.derivatives.add(p_184305_);
         return this;
      }

      public CubicSpline<C, I> build() {
         if (this.locations.isEmpty()) {
            throw new IllegalStateException("No elements added");
         } else {
            return CubicSpline.Multipoint.create(
               this.coordinate, this.locations.toFloatArray(), ImmutableList.copyOf(this.values), this.derivatives.toFloatArray()
            );
         }
      }
   }

   @VisibleForDebug
   record Constant<C, I extends BoundedFloatFunction<C>>(float value) implements CubicSpline<C, I> {
      @Override
      public float apply(C p_184313_) {
         return this.value;
      }

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
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> p_211581_) {
         return this;
      }
   }

   interface CoordinateVisitor<I> {
      I visit(I var1);
   }

   @VisibleForDebug
   record Multipoint<C, I extends BoundedFloatFunction<C>>(
      I coordinate, float[] locations, List<CubicSpline<C, I>> values, float[] derivatives, float minValue, float maxValue
   ) implements CubicSpline<C, I> {
      public Multipoint {
         validateSizes(locations, values, derivatives);
      }

      static <C, I extends BoundedFloatFunction<C>> CubicSpline.Multipoint<C, I> create(
         I p_431146_, float[] p_216145_, List<CubicSpline<C, I>> p_216146_, float[] p_216147_
      ) {
         validateSizes(p_216145_, p_216146_, p_216147_);
         int i = p_216145_.length - 1;
         float f = Float.POSITIVE_INFINITY;
         float f1 = Float.NEGATIVE_INFINITY;
         float f2 = p_431146_.minValue();
         float f3 = p_431146_.maxValue();
         if (f2 < p_216145_[0]) {
            float f4 = linearExtend(f2, p_216145_, p_216146_.get(0).minValue(), p_216147_, 0);
            float f5 = linearExtend(f2, p_216145_, p_216146_.get(0).maxValue(), p_216147_, 0);
            f = Math.min(f, Math.min(f4, f5));
            f1 = Math.max(f1, Math.max(f4, f5));
         }

         if (f3 > p_216145_[i]) {
            float f24 = linearExtend(f3, p_216145_, p_216146_.get(i).minValue(), p_216147_, i);
            float f25 = linearExtend(f3, p_216145_, p_216146_.get(i).maxValue(), p_216147_, i);
            f = Math.min(f, Math.min(f24, f25));
            f1 = Math.max(f1, Math.max(f24, f25));
         }

         for (CubicSpline<C, I> cubicspline2 : p_216146_) {
            f = Math.min(f, cubicspline2.minValue());
            f1 = Math.max(f1, cubicspline2.maxValue());
         }

         for (int j = 0; j < i; j++) {
            float f26 = p_216145_[j];
            float f6 = p_216145_[j + 1];
            float f7 = f6 - f26;
            CubicSpline<C, I> cubicspline = p_216146_.get(j);
            CubicSpline<C, I> cubicspline1 = p_216146_.get(j + 1);
            float f8 = cubicspline.minValue();
            float f9 = cubicspline.maxValue();
            float f10 = cubicspline1.minValue();
            float f11 = cubicspline1.maxValue();
            float f12 = p_216147_[j];
            float f13 = p_216147_[j + 1];
            if (f12 != 0.0F || f13 != 0.0F) {
               float f14 = f12 * f7;
               float f15 = f13 * f7;
               float f16 = Math.min(f8, f10);
               float f17 = Math.max(f9, f11);
               float f18 = f14 - f11 + f8;
               float f19 = f14 - f10 + f9;
               float f20 = -f15 + f10 - f9;
               float f21 = -f15 + f11 - f8;
               float f22 = Math.min(f18, f20);
               float f23 = Math.max(f19, f21);
               f = Math.min(f, f16 + 0.25F * f22);
               f1 = Math.max(f1, f17 + 0.25F * f23);
            }
         }

         return new CubicSpline.Multipoint<>(p_431146_, p_216145_, p_216146_, p_216147_, f, f1);
      }

      private static float linearExtend(float p_216134_, float[] p_216135_, float p_216136_, float[] p_216137_, int p_216138_) {
         float f = p_216137_[p_216138_];
         return f == 0.0F ? p_216136_ : p_216136_ + f * (p_216134_ - p_216135_[p_216138_]);
      }

      private static <C, I extends BoundedFloatFunction<C>> void validateSizes(float[] p_216152_, List<CubicSpline<C, I>> p_216153_, float[] p_216154_) {
         if (p_216152_.length != p_216153_.size() || p_216152_.length != p_216154_.length) {
            throw new IllegalArgumentException("All lengths must be equal, got: " + p_216152_.length + " " + p_216153_.size() + " " + p_216154_.length);
         }

         if (p_216152_.length == 0) {
            throw new IllegalArgumentException("Cannot create a multipoint spline with no points");
         }
      }

      @Override
      public float apply(C p_184340_) {
         float f = this.coordinate.apply(p_184340_);
         int i = findIntervalStart(this.locations, f);
         int j = this.locations.length - 1;
         if (i < 0) {
            return linearExtend(f, this.locations, this.values.get(0).apply(p_184340_), this.derivatives, 0);
         }

         if (i == j) {
            return linearExtend(f, this.locations, this.values.get(j).apply(p_184340_), this.derivatives, j);
         }

         float f1 = this.locations[i];
         float f2 = this.locations[i + 1];
         float f3 = (f - f1) / (f2 - f1);
         BoundedFloatFunction<C> boundedfloatfunction = (BoundedFloatFunction<C>)this.values.get(i);
         BoundedFloatFunction<C> boundedfloatfunction1 = (BoundedFloatFunction<C>)this.values.get(i + 1);
         float f4 = this.derivatives[i];
         float f5 = this.derivatives[i + 1];
         float f6 = boundedfloatfunction.apply(p_184340_);
         float f7 = boundedfloatfunction1.apply(p_184340_);
         float f8 = f4 * (f2 - f1) - (f7 - f6);
         float f9 = -f5 * (f2 - f1) + (f7 - f6);
         return Mth.lerp(f3, f6, f7) + f3 * (1.0F - f3) * Mth.lerp(f3, f8, f9);
      }

      private static int findIntervalStart(float[] p_216149_, float p_216150_) {
         return Mth.binarySearch(0, p_216149_.length, p_216142_ -> p_216150_ < p_216149_[p_216142_]) - 1;
      }

      @VisibleForTesting
      @Override
      public String parityString() {
         return "Spline{coordinate="
            + this.coordinate
            + ", locations="
            + this.toString(this.locations)
            + ", derivatives="
            + this.toString(this.derivatives)
            + ", values="
            + this.values.stream().map(CubicSpline::parityString).collect(Collectors.joining(", ", "[", "]"))
            + "}";
      }

      private String toString(float[] p_184335_) {
         return "["
            + IntStream.range(0, p_184335_.length)
               .mapToDouble(p_184338_ -> p_184335_[p_184338_])
               .mapToObj(p_184330_ -> String.format(Locale.ROOT, "%.3f", p_184330_))
               .collect(Collectors.joining(", "))
            + "]";
      }

      @Override
      public CubicSpline<C, I> mapAll(CubicSpline.CoordinateVisitor<I> p_211585_) {
         return create(
            p_211585_.visit(this.coordinate), this.locations, this.values().stream().map(p_211588_ -> p_211588_.mapAll(p_211585_)).toList(), this.derivatives
         );
      }
   }
}
