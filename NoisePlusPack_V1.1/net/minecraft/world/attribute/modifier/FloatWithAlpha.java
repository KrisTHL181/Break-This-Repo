package net.minecraft.world.attribute.modifier;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FloatWithAlpha(float value, float alpha) {
   private static final Codec<FloatWithAlpha> FULL_CODEC = RecordCodecBuilder.create(
      p_459701_ -> p_459701_.group(
            Codec.FLOAT.fieldOf("value").forGetter(FloatWithAlpha::value),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("alpha", 1.0F).forGetter(FloatWithAlpha::alpha)
         )
         .apply(p_459701_, FloatWithAlpha::new)
   );
   public static final Codec<FloatWithAlpha> CODEC = Codec.either(Codec.FLOAT, FULL_CODEC)
      .xmap(
         p_460777_ -> (FloatWithAlpha)p_460777_.map(FloatWithAlpha::new, p_453757_ -> p_453757_),
         p_454760_ -> p_454760_.alpha() == 1.0F ? Either.left(p_454760_.value()) : Either.right(p_454760_)
      );

   public FloatWithAlpha(float p_451397_) {
      this(p_451397_, 1.0F);
   }
}
