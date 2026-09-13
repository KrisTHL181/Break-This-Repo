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
      public Integer apply(Integer p_459139_, Integer p_460205_) {
         return ARGB.alphaBlend(p_459139_, p_460205_);
      }

      @Override
      public Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> p_452562_) {
         return ExtraCodecs.STRING_ARGB_COLOR;
      }

      @Override
      public LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_459071_) {
         return LerpFunction.ofColor();
      }
   };
   ColorModifier<Integer> ADD = ARGB::addRgb;
   ColorModifier<Integer> SUBTRACT = ARGB::subtractRgb;
   ColorModifier<Integer> MULTIPLY_RGB = ARGB::multiply;
   ColorModifier<Integer> MULTIPLY_ARGB = ARGB::multiply;
   ColorModifier<ColorModifier.BlendToGray> BLEND_TO_GRAY = new ColorModifier<ColorModifier.BlendToGray>() {
      public Integer apply(Integer p_452366_, ColorModifier.BlendToGray p_456372_) {
         int i = ARGB.scaleRGB(ARGB.greyscale(p_452366_), p_456372_.brightness);
         return ARGB.srgbLerp(p_456372_.factor, p_452366_, i);
      }

      @Override
      public Codec<ColorModifier.BlendToGray> argumentCodec(EnvironmentAttribute<Integer> p_460999_) {
         return ColorModifier.BlendToGray.CODEC;
      }

      @Override
      public LerpFunction<ColorModifier.BlendToGray> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_452182_) {
         return (p_453000_, p_459865_, p_456638_) -> new ColorModifier.BlendToGray(
            Mth.lerp(p_453000_, p_459865_.brightness, p_456638_.brightness), Mth.lerp(p_453000_, p_459865_.factor, p_456638_.factor)
         );
      }
   };

   @FunctionalInterface
   interface ArgbModifier extends ColorModifier<Integer> {
      @Override
      default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> p_454098_) {
         return Codec.either(ExtraCodecs.STRING_ARGB_COLOR, ExtraCodecs.RGB_COLOR_CODEC)
            .xmap(Either::unwrap, p_450479_ -> ARGB.alpha(p_450479_) == 255 ? Either.right(p_450479_) : Either.left(p_450479_));
      }

      @Override
      default LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_458286_) {
         return LerpFunction.ofColor();
      }
   }

   record BlendToGray(float brightness, float factor) {
      public static final Codec<ColorModifier.BlendToGray> CODEC = RecordCodecBuilder.create(
         p_459470_ -> p_459470_.group(
               Codec.floatRange(0.0F, 1.0F).fieldOf("brightness").forGetter(ColorModifier.BlendToGray::brightness),
               Codec.floatRange(0.0F, 1.0F).fieldOf("factor").forGetter(ColorModifier.BlendToGray::factor)
            )
            .apply(p_459470_, ColorModifier.BlendToGray::new)
      );
   }

   @FunctionalInterface
   interface RgbModifier extends ColorModifier<Integer> {
      @Override
      default Codec<Integer> argumentCodec(EnvironmentAttribute<Integer> p_451408_) {
         return ExtraCodecs.STRING_RGB_COLOR;
      }

      @Override
      default LerpFunction<Integer> argumentKeyframeLerp(EnvironmentAttribute<Integer> p_459370_) {
         return LerpFunction.ofColor();
      }
   }
}
