package net.minecraft.core;

import org.jspecify.annotations.Nullable;

public interface IdMap<T> extends Iterable<T> {
   int DEFAULT = -1;

   int getId(T var1);

   @Nullable T byId(int var1);

   default T byIdOrThrow(int p_200958_) {
      T t = this.byId(p_200958_);
      if (t == null) {
         throw new IllegalArgumentException("No value with id " + p_200958_);
      } else {
         return t;
      }
   }

   default int getIdOrThrow(T p_329088_) {
      int i = this.getId(p_329088_);
      if (i == -1) {
         throw new IllegalArgumentException("Can't find id for '" + p_329088_ + "' in map " + this);
      } else {
         return i;
      }
   }

   int size();
}
