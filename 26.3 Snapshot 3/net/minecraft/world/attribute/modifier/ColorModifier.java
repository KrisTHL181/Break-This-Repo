package net.minecraft.world.attribute.modifier;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.LerpFunction;

public interface ColorModifier<Argument> extends AttributeModifier<Integer, Argument> {
   ColorModifier<Integer> ALPHA_BLEND = new ColorModifier<Integer>() {
      public Integer apply(final Integer subject, final Integer argument) {
         return ARGB.alphaBlend(subject, argument);
      }

      @Override
      public Codec<Integer> argumentCodec(final EnvironmentAttribute<Integer> type) {
         return ExtraCodecs.STRING_ARGB_COLOR;
      }

      @Override
      public LerpFunction<Integer> argumentKeyframeLerp(final EnvironmentAttribute<Integer> type) {
         return LerpFunction.ofColor();
      }
   };
   ColorModifier<Integer> ADD = ARGB::addRgb;
   ColorModifier<Integer> SUBTRACT = ARGB::subtractRgb;
   ColorModifier<Integer> MULTIPLY_RGB = ARGB::multiply;
   ColorModifier<Integer> MULTIPLY_ARGB = ARGB::multiply;
   ColorModifier<ColorModifier.BlendToGray> BLEND_TO_GRAY = new ColorModifier<ColorModifier.BlendToGray>() {
      public Integer apply(final Integer subject, final ColorModifier.BlendToGray argument) {
         int multipliedGreyscale = ARGB.scaleRGB(ARGB.greyscale(subject), argument.brightness);
         return ARGB.srgbLerp(argument.factor, subject, multipliedGreyscale);
      }

      @Override
      public Codec<ColorModifier.BlendToGray> argumentCodec(final EnvironmentAttribute<Integer> type) {
         return ColorModifier.BlendToGray.CODEC;
      }

      @Override
      public LerpFunction<ColorModifier.BlendToGray> argumentKeyframeLerp(final EnvironmentAttribute<Integer> type) {
         return (alpha, from, to) -> new ColorModifier.BlendToGray(Mth.lerp(alpha, from.brightness, to.brightness), Mth.lerp(alpha, from.factor, to.factor));
      }
   };

   @FunctionalInterface
   interface ArgbModifier extends ColorModifier<Integer> {
      @Override
      default Codec<Integer> argumentCodec(final EnvironmentAttribute<Integer> type) {
         return Codec.either(ExtraCodecs.STRING_ARGB_COLOR, ExtraCodecs.RGB_COLOR_CODEC)
            .xmap(Either::unwrap, color -> ARGB.alpha(color) == 255 ? Either.right(color) : Either.left(color));
      }

      @Override
      default LerpFunction<Integer> argumentKeyframeLerp(final EnvironmentAttribute<Integer> type) {
         return LerpFunction.ofColor();
      }
   }

   record BlendToGray(float brightness, float factor) {
      public static final Codec<ColorModifier.BlendToGray> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               Codec.floatRange(0.0F, 1.0F).fieldOf("brightness").forGetter(ColorModifier.BlendToGray::brightness),
               Codec.floatRange(0.0F, 1.0F).fieldOf("factor").forGetter(ColorModifier.BlendToGray::factor)
            )
            .apply(i, ColorModifier.BlendToGray::new)
      );
   }

   @FunctionalInterface
   interface RgbModifier extends ColorModifier<Integer> {
      @Override
      default Codec<Integer> argumentCodec(final EnvironmentAttribute<Integer> type) {
         return ExtraCodecs.STRING_RGB_COLOR;
      }

      @Override
      default LerpFunction<Integer> argumentKeyframeLerp(final EnvironmentAttribute<Integer> type) {
         return LerpFunction.ofColor();
      }
   }
}
