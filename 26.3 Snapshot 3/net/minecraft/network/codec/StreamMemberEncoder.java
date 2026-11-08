package net.minecraft.network.codec;

@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
   void encode(T value, O output);
}
