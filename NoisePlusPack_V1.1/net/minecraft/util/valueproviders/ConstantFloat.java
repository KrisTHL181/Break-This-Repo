package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;

public class ConstantFloat extends FloatProvider {
   public static final ConstantFloat ZERO = new ConstantFloat(0.0F);
   public static final MapCodec<ConstantFloat> CODEC = Codec.FLOAT.fieldOf("value").xmap(ConstantFloat::of, ConstantFloat::getValue);
   private final float value;

   public static ConstantFloat of(float p_146459_) {
      return p_146459_ == 0.0F ? ZERO : new ConstantFloat(p_146459_);
   }

   private ConstantFloat(float p_146456_) {
      this.value = p_146456_;
   }

   public float getValue() {
      return this.value;
   }

   @Override
   public float sample(RandomSource p_216852_) {
      return this.value;
   }

   @Override
   public float getMinValue() {
      return this.value;
   }

   @Override
   public float getMaxValue() {
      return this.value;
   }

   @Override
   public FloatProviderType<?> getType() {
      return FloatProviderType.CONSTANT;
   }

   @Override
   public String toString() {
      return Float.toString(this.value);
   }
}
