package net.minecraft.util;

import java.util.Locale;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

public class StaticCache2D<T> {
   private final int minX;
   private final int minZ;
   private final int sizeX;
   private final int sizeZ;
   private final @Nullable Object[] cache;

   public static <T> StaticCache2D<T> create(final int centerX, final int centerZ, final int range, final StaticCache2D.Initializer<T> initializer) {
      int minX = centerX - range;
      int minZ = centerZ - range;
      int size = 2 * range + 1;
      return new StaticCache2D<>(minX, minZ, size, size, initializer);
   }

   private StaticCache2D(final int minX, final int minZ, final int sizeX, final int sizeZ, final StaticCache2D.Initializer<T> initializer) {
      this.minX = minX;
      this.minZ = minZ;
      this.sizeX = sizeX;
      this.sizeZ = sizeZ;
      this.cache = new Object[this.sizeX * this.sizeZ];

      for (int x = minX; x < minX + sizeX; x++) {
         for (int z = minZ; z < minZ + sizeZ; z++) {
            this.cache[this.getIndex(x, z)] = initializer.get(x, z);
         }
      }
   }

   public <U> StaticCache2D<U> map(final StaticCache2D.MappingFunction<T, U> function) {
      return new StaticCache2D(this.minX, this.minZ, this.sizeX, this.sizeZ, (x, z) -> (T)function.apply(this.get(x, z), x, z));
   }

   public void forEach(final Consumer<T> consumer) {
      for (Object o : this.cache) {
         consumer.accept((T)o);
      }
   }

   public T get(final int x, final int z) {
      if (!this.contains(x, z)) {
         throw new IllegalArgumentException("Requested out of range value (" + x + "," + z + ") from " + this);
      } else {
         return (T)this.cache[this.getIndex(x, z)];
      }
   }

   public boolean contains(final int x, final int z) {
      int deltaX = x - this.minX;
      int deltaZ = z - this.minZ;
      return deltaX >= 0 && deltaX < this.sizeX && deltaZ >= 0 && deltaZ < this.sizeZ;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "StaticCache2D[%d, %d, %d, %d]", this.minX, this.minZ, this.minX + this.sizeX, this.minZ + this.sizeZ);
   }

   private int getIndex(final int x, final int z) {
      int deltaX = x - this.minX;
      int deltaZ = z - this.minZ;
      return deltaX * this.sizeZ + deltaZ;
   }

   @FunctionalInterface
   public interface Initializer<T> {
      T get(int x, int z);
   }

   @FunctionalInterface
   public interface MappingFunction<T, U> {
      U apply(T value, int x, int z);
   }
}
