package net.minecraft.server.dialog.input;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public record NumberRangeInput(int width, Component label, String labelFormat, NumberRangeInput.RangeInfo rangeInfo) implements InputControl {
   // ===== 修改：RecordCodecBuilder.<NumberRangeInput>group → i.group =====
   public static final MapCodec<NumberRangeInput> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Dialog.WIDTH_CODEC.optionalFieldOf("width", 200).forGetter(NumberRangeInput::width),
            ComponentSerialization.CODEC.fieldOf("label").forGetter(NumberRangeInput::label),
            Codec.STRING.optionalFieldOf("label_format", "options.generic_value").forGetter(NumberRangeInput::labelFormat),
            NumberRangeInput.RangeInfo.MAP_CODEC.forGetter(NumberRangeInput::rangeInfo)
         )
         .apply(i, NumberRangeInput::new)
   );

   @Override
   public MapCodec<NumberRangeInput> mapCodec() {
      return MAP_CODEC;
   }

   public Component computeLabel(final String value) {
      return Component.translatable(this.labelFormat, this.label, value);
   }

   public record RangeInfo(float start, float end, Optional<Float> initial, Optional<Float> step) {
      // ===== 修改：RecordCodecBuilder.<NumberRangeInput.RangeInfo>group → i.group =====
      public static final MapCodec<NumberRangeInput.RangeInfo> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                  Codec.FLOAT.fieldOf("start").forGetter(NumberRangeInput.RangeInfo::start),
                  Codec.FLOAT.fieldOf("end").forGetter(NumberRangeInput.RangeInfo::end),
                  Codec.FLOAT.optionalFieldOf("initial").forGetter(NumberRangeInput.RangeInfo::initial),
                  ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("step").forGetter(NumberRangeInput.RangeInfo::step)
               )
               .apply(i, NumberRangeInput.RangeInfo::new)
         )
         .validate((NumberRangeInput.RangeInfo range) -> {
            if (range.initial.isPresent()) {
               double initial = range.initial.get().floatValue();
               double min = Math.min(range.start, range.end);
               double max = Math.max(range.start, range.end);
               if (initial < min || initial > max) {
                  return DataResult.error(() -> "Initial value " + initial + " is outside of range [" + min + ", " + max + "]");
               }
            }

            return DataResult.success(range);
         });

      public float computeScaledValue(final float sliderValue) {
         float valueInRange = Mth.lerp(sliderValue, this.start, this.end);
         if (this.step.isEmpty()) {
            return valueInRange;
         }

         float step = this.step.get();
         float initialValue = this.initialScaledValue();
         float deltaToInitial = valueInRange - initialValue;
         int stepsOutsideInitial = Math.round(deltaToInitial / step);
         float result = initialValue + stepsOutsideInitial * step;
         if (!this.isOutOfRange(result)) {
            return result;
         }

         int oneStepLess = stepsOutsideInitial - Mth.sign(stepsOutsideInitial);
         return initialValue + oneStepLess * step;
      }

      private boolean isOutOfRange(final float scaledValue) {
         float sliderPos = this.scaledValueToSlider(scaledValue);
         return sliderPos < 0.0 || sliderPos > 1.0;
      }

      private float initialScaledValue() {
         return this.initial.isPresent() ? this.initial.get() : (this.start + this.end) / 2.0F;
      }

      public float initialSliderValue() {
         float value = this.initialScaledValue();
         return this.scaledValueToSlider(value);
      }

      private float scaledValueToSlider(final float value) {
         return this.start == this.end ? 0.5F : Mth.inverseLerp(value, this.start, this.end);
      }
   }
}