package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Keyframe<T>(int ticks, T value) {
   public static <T> Codec<Keyframe<T>> codec(Codec<T> p_452766_) {
      return RecordCodecBuilder.create(
         p_460507_ -> p_460507_.group(
               ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks").forGetter(Keyframe::ticks), p_452766_.fieldOf("value").forGetter(Keyframe::value)
            )
            .apply(p_460507_, Keyframe::new)
      );
   }
}
