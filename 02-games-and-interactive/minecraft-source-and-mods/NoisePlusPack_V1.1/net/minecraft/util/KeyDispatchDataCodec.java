package net.minecraft.util;

import com.mojang.serialization.MapCodec;

public record KeyDispatchDataCodec<A>(MapCodec<A> codec) {
   public static <A> KeyDispatchDataCodec<A> of(MapCodec<A> p_216239_) {
      return new KeyDispatchDataCodec<>(p_216239_);
   }
}
