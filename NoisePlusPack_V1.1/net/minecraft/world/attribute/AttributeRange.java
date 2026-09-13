package net.minecraft.world.attribute;

import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public interface AttributeRange<Value> {
   AttributeRange<Float> UNIT_FLOAT = ofFloat(0.0F, 1.0F);
   AttributeRange<Float> NON_NEGATIVE_FLOAT = ofFloat(0.0F, Float.POSITIVE_INFINITY);

   static <Value> AttributeRange<Value> any() {
      return new AttributeRange<Value>() {
         @Override
         public DataResult<Value> validate(Value p_456412_) {
            return DataResult.success(p_456412_);
         }

         @Override
         public Value sanitize(Value p_455294_) {
            return p_455294_;
         }
      };
   }

   static AttributeRange<Float> ofFloat(final float p_457262_, final float p_457736_) {
      return new AttributeRange<Float>() {
         public DataResult<Float> validate(Float p_458009_) {
            return p_458009_ >= p_457262_ && p_458009_ <= p_457736_
               ? DataResult.success(p_458009_)
               : DataResult.error(() -> p_458009_ + " is not in range [" + p_457262_ + "; " + p_457736_ + "]");
         }

         public Float sanitize(Float p_457184_) {
            return p_457184_ >= p_457262_ && p_457184_ <= p_457736_ ? p_457184_ : Mth.clamp(p_457184_, p_457262_, p_457736_);
         }
      };
   }

   DataResult<Value> validate(Value var1);

   Value sanitize(Value var1);
}
