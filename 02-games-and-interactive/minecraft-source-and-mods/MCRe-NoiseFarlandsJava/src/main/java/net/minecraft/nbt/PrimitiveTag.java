package net.minecraft.nbt;

public sealed interface PrimitiveTag extends Tag permits NumericTag, StringTag {
    @Override
    default Tag copy() {
        return this;
    }
}