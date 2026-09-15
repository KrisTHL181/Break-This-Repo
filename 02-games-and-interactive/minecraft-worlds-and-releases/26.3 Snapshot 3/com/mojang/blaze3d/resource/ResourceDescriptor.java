package com.mojang.blaze3d.resource;

public interface ResourceDescriptor<T> {
   T allocate();

   default void prepare(final T resource) {
   }

   void free(T resource);

   default boolean canUsePhysicalResource(final ResourceDescriptor<?> other) {
      return this.equals(other);
   }
}
