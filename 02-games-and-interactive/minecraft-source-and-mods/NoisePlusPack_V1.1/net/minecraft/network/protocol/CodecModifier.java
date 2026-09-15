package net.minecraft.network.protocol;

import net.minecraft.network.codec.StreamCodec;

@FunctionalInterface
public interface CodecModifier<B, V, C> {
   StreamCodec<? super B, V> apply(StreamCodec<? super B, V> var1, C var2);
}
