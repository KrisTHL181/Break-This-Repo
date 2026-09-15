package net.minecraft.core;

public interface HolderOwner<T> {
    default boolean canSerializeIn(final HolderOwner<T> context) {
        return context == this;
    }
}