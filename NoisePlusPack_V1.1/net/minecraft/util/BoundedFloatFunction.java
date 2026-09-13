package net.minecraft.util;

import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import java.util.function.Function;

public interface BoundedFloatFunction<C> {
   BoundedFloatFunction<Float> IDENTITY = createUnlimited(p_424716_ -> p_424716_);

   float apply(C var1);

   float minValue();

   float maxValue();

   static BoundedFloatFunction<Float> createUnlimited(final Float2FloatFunction p_425005_) {
      return new BoundedFloatFunction<Float>() {
         public float apply(Float p_426917_) {
            return (Float)p_425005_.apply(p_426917_);
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
   }

   default <C2> BoundedFloatFunction<C2> comap(final Function<C2, C> p_425486_) {
      final BoundedFloatFunction<C> boundedfloatfunction = this;
      return new BoundedFloatFunction<C2>() {
         @Override
         public float apply(C2 p_431624_) {
            return boundedfloatfunction.apply(p_425486_.apply(p_431624_));
         }

         @Override
         public float minValue() {
            return boundedfloatfunction.minValue();
         }

         @Override
         public float maxValue() {
            return boundedfloatfunction.maxValue();
         }
      };
   }
}
