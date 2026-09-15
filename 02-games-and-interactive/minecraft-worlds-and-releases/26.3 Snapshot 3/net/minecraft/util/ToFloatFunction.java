package net.minecraft.util;

@FunctionalInterface
public interface ToFloatFunction<T> {
   float applyAsFloat(T value);
}
