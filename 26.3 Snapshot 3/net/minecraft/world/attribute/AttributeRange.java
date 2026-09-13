package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public interface AttributeRange<Value> {
   AttributeRange<Float> UNIT_FLOAT = ofFloat(0.0F, 1.0F);
   AttributeRange<Float> NON_NEGATIVE_FLOAT = ofFloat(0.0F, Float.POSITIVE_INFINITY);

   static <Value> AttributeRange<Value> any() {
      return new AttributeRange<Value>() {
         @Override
         public DataResult<Value> validate(final Value value) {
            return DataResult.success(value);
         }

         @Override
         public Value sanitize(final Value value) {
            return value;
         }
      };
   }

   static AttributeRange<Float> ofFloat(final float minValue, final float maxValue) {
      return new AttributeRange<Float>() {
         public DataResult<Float> validate(final Float value) {
            return value >= minValue && value <= maxValue
               ? DataResult.success(value)
               : DataResult.error(() -> value + " is not in range [" + minValue + "; " + maxValue + "]");
         }

         public Float sanitize(final Float value) {
            return value >= minValue && value <= maxValue ? value : Mth.clamp(value, minValue, maxValue);
         }
      };
   }

   DataResult<Value> validate(Value value);

   Value sanitize(Value value);
}
