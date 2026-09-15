package net.minecraft.core;

public interface HolderOwner<T> {
   default boolean canSerializeIn(HolderOwner<T> p_255875_) {
      return p_255875_ == this;
   }
}
